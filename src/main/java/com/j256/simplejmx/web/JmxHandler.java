package com.j256.simplejmx.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.j256.simplejmx.client.ClientUtils;

/**
 * Simple JMX handler that displays JMX information for a HTTP request.
 * 
 * @author graywatson
 */
public class JmxHandler extends AbstractHandler {

	private static final String COMMAND_LIST_BEANS_IN_DOMAIN = "d";
	private static final String COMMAND_SHOW_BEAN = "b";
	private static final String COMMAND_ASSIGN_ATTRIBUTE = "a";
	private static final String COMMAND_INVOKE_OPERATION = "o";
	private static final String COMMAND_SHOW_ALL_BEANS = "s";
	private static final String PARAM_ATTRIBUTE_VALUE = "v";
	private static final String PARAM_OPERATION_PREFIX = "p";
	private static final String PARAM_TEXT_ONLY = "t";

	private MBeanServer mbeanServer;
	private ObjectNameComparator objectNameComparator = new ObjectNameComparator();

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
		if (mbeanServer == null) {
			mbeanServer = ManagementFactory.getPlatformMBeanServer();
		}
		processRequest(request, response, writer);
		baseRequest.setHandled(true);
		writer.close();
	}

	private void processRequest(HttpServletRequest request, HttpServletResponse response, BufferedWriter writer)
			throws IOException {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null) {
			pathInfo = "";
		} else if (pathInfo.charAt(0) == '/') {
			pathInfo = pathInfo.substring(1);
		}

		boolean textOnly = (request.getParameter(PARAM_TEXT_ONLY) != null);
		if (textOnly) {
			response.setContentType("text/plain");
		} else {
			response.setContentType("text/html");
		}

		int slashIndex = pathInfo.indexOf('/');
		if (slashIndex < 0) {
			if (!textOnly) {
				appendHeader(writer);
			}
			if (pathInfo.equals(COMMAND_SHOW_ALL_BEANS)) {
				listBeansInDomain(writer, textOnly, null);
			} else {
				listDomains(writer, textOnly);
				if (!textOnly) {
					writer.append("<br />\n");
					appendLink(writer, textOnly, '/' + COMMAND_SHOW_ALL_BEANS, "all", "Show all beans.");
					writer.append("  ");
				}
			}
			if (!textOnly) {
				appendFooter(writer);
			}
			return;
		}

		String command = pathInfo.substring(0, slashIndex);
		pathInfo = pathInfo.substring(slashIndex + 1);

		if (command.equals(COMMAND_LIST_BEANS_IN_DOMAIN)) {
			if (!textOnly) {
				appendHeader(writer);
			}
			listBeansInDomain(writer, textOnly, pathInfo);
			if (!textOnly) {
				appendFooter(writer);
			}
		} else if (command.equals(COMMAND_SHOW_BEAN)) {
			if (!textOnly) {
				appendHeader(writer);
			}
			showBean(writer, textOnly, pathInfo);
			if (!textOnly) {
				appendFooter(writer);
			}
		} else if (command.equals(COMMAND_ASSIGN_ATTRIBUTE)) {
			// this may redirect
			assignAttribute(request, response, writer, textOnly, pathInfo);
		} else if (command.equals(COMMAND_INVOKE_OPERATION)) {
			if (!textOnly) {
				appendHeader(writer);
			}
			invokeOperation(request, writer, textOnly, pathInfo);
			if (!textOnly) {
				appendFooter(writer);
			}
		} else {
			if (textOnly) {
				response.sendError(Response.SC_NOT_FOUND);
			} else {
				appendHeader(writer);
				writer.append("Unknown command: " + command + " <br />\n");
				appendBackToRoot(writer);
				appendFooter(writer);
			}
		}
	}

	private void listDomains(BufferedWriter writer, boolean textOnly) throws IOException {
		if (!textOnly) {
			writer.append("<h1> JMX Domains </h1>\n");
		}
		List<String> domainNames = new ArrayList<String>();
		for (String domainName : mbeanServer.getDomains()) {
			domainNames.add(domainName);
		}
		Collections.sort(domainNames);
		for (String domainName : domainNames) {
			appendLink(writer, textOnly, '/' + COMMAND_LIST_BEANS_IN_DOMAIN + '/' + domainName, "beans", domainName);
			appendLine(writer, textOnly, null);
		}
	}

	private void listBeansInDomain(BufferedWriter writer, boolean textOnly, String domainName) throws IOException {
		// TODO: need to show directories here
		if (!textOnly) {
			if (domainName == null) {
				writer.append("<h1> All Beans </h1>\n");
			} else {
				writer.append("<h1> Beans in domain " + domainName + " </h1>\n");
			}
		}
		Set<ObjectInstance> mbeans;
		if (domainName == null) {
			mbeans = mbeanServer.queryMBeans(null, null);
		} else {
			mbeans = mbeanServer.queryMBeans(null, new DomainQueryExp(domainName));
		}
		List<ObjectName> objectNames = new ArrayList<ObjectName>();
		for (ObjectInstance mbean : mbeans) {
			objectNames.add(mbean.getObjectName());
		}
		Collections.sort(objectNames, objectNameComparator);
		for (ObjectName objectName : objectNames) {
			String nameString = objectName.toString();
			if (textOnly) {
				writer.append(nameString + '\n');
			} else {
				String display = nameString;
				int index = nameString.indexOf(':');
				if (index > 0) {
					display = nameString.substring(index + 1, nameString.length());
				}
				appendLink(writer, textOnly, '/' + COMMAND_SHOW_BEAN + '/' + nameString, nameString, display);
				appendLine(writer, textOnly, null);
			}
		}
		if (!textOnly) {
			appendBackToRoot(writer);
		}
	}

	private void showBean(BufferedWriter writer, boolean textOnly, String objectNameString) throws IOException {
		ObjectName objectName;
		try {
			objectName = new ObjectName(objectNameString);
		} catch (MalformedObjectNameException mone) {
			appendLine(writer, textOnly, "Invalid object name: " + objectNameString);
			if (!textOnly) {
				appendBackToRoot(writer);
			}
			return;
		}
		MBeanInfo mbeanInfo;
		try {
			mbeanInfo = mbeanServer.getMBeanInfo(objectName);
		} catch (Exception e) {
			appendLine(writer, textOnly, "Investigating object threw exception: " + e);
			if (!textOnly) {
				appendBackToRoot(writer);;
			}
			return;
		}
		if (!textOnly) {
			writer.append("<h1> Information about object " + objectNameString + " </h1>\n");
			displayClassInfo(writer, mbeanInfo);
		}
		displayAttributes(writer, textOnly, objectName, mbeanInfo);
		displayOperations(writer, textOnly, objectName, mbeanInfo);
		if (!textOnly) {
			appendBackToDomains(writer, objectName);
		}
	}

	private void displayAttributes(BufferedWriter writer, boolean textOnly, ObjectName objectName, MBeanInfo mbeanInfo)
			throws IOException {
		if (!textOnly) {
			writer.append("<table cellpadding='3' cellspacing='1' border='3'>\n");
			writer.append("<tr><th colspan='3'> Attributes: </th></tr>\n");
			writer.append("<tr><th> Name </th><th> Type </th><th> Value </th></tr>\n");
		}
		for (MBeanAttributeInfo attribute : mbeanInfo.getAttributes()) {
			String name = attribute.getName();
			Object value = null;
			if (attribute.isReadable()) {
				try {
					value = mbeanServer.getAttribute(objectName, name);
				} catch (Exception e) {
					value = "error getting value";
				}
			} else {
				value = "not readable";
			}
			String valueString = ClientUtils.valueToString(value);
			if (textOnly) {
				writer.append(name + (attribute.isWritable() ? "*" : "") + "=" + valueString + "\n");
				continue;
			}
			if (attribute.isWritable()) {
				writer.append("<form action=\"/" + COMMAND_ASSIGN_ATTRIBUTE + "/" + objectName + "/" + name
						+ "\" name=\"" + name + "\">\n");
			}
			writer.append("<tr><td> " + name + " </td>");
			writer.append("<td> " + ClientUtils.displayType(attribute.getType(), value) + " </td>");
			if (attribute.isWritable()) {
				writer.append("<td><input name=\"" + PARAM_ATTRIBUTE_VALUE + "\" value=\"" + valueString + "\" ></td>");
			} else {
				writer.append("<td> " + ClientUtils.valueToString(value) + " </td>");
			}
			writer.append("</tr>\n");
			if (attribute.isWritable()) {
				writer.append("</form>\n");
			}
		}
		if (!textOnly) {
			writer.append("</table>\n");
		}
	}

	private void displayOperations(BufferedWriter writer, boolean textOnly, ObjectName objectName, MBeanInfo mbeanInfo)
			throws IOException {
		int maxParams = 1;
		boolean noOperations = true;
		for (MBeanOperationInfo operation : mbeanInfo.getOperations()) {
			if (isGetSet(operation.getName())) {
				continue;
			}
			MBeanParameterInfo[] params = operation.getSignature();
			if (params.length > maxParams) {
				maxParams = params.length;
			}
			noOperations = false;
		}
		if (noOperations) {
			if (!textOnly) {
				writer.append("No operations. <br />\n");
			}
			return;
		}

		if (!textOnly) {
			writer.append("<table cellpadding='3' cellspacing='1' border='3'>\n");
			writer.append("<tr><th colspan='" + (maxParams + 3) + "'> Operations: </th></tr>\n");
			writer.append("<tr><th> Name </th><th> Return </th><th colspan='" + maxParams
					+ "'> Params </th><th> Invoke </th></tr>\n");
		}
		for (MBeanOperationInfo operation : mbeanInfo.getOperations()) {
			String name = operation.getName();
			if (isGetSet(name)) {
				continue;
			}
			if (textOnly) {
				writer.append(name);
			} else {
				writer.append("<form action=\"/" + COMMAND_INVOKE_OPERATION + "/" + objectName + "/" + name
						+ "\" name=\"" + name + "\">\n");
				writer.append("<tr><td> " + name + " </td>");
				writer.append("<td> " + ClientUtils.displayType(operation.getReturnType(), null) + " </td>");
			}
			MBeanParameterInfo[] params = operation.getSignature();
			int paramCount = 0;
			for (MBeanParameterInfo param : params) {
				if (textOnly) {
					writer.append(" " + param.getType());
				} else {
					writer.append("<td> <input name=\"" + PARAM_OPERATION_PREFIX + paramCount++ + "\" value=\""
							+ param.getName() + "\" > (" + param.getType() + ")</td>");
				}
			}
			if (textOnly) {
				writer.append("\n");
			} else {
				for (; paramCount < maxParams; paramCount++) {
					writer.append("<td> &nbsp; </td>");
				}
				writer.append("<td><input type='submit' value='" + name + "' /></td>");
				writer.append("</tr>\n");
				writer.append("</form>\n");
			}
		}
		if (!textOnly) {
			writer.append("</table>\n");
		}
	}

	private void assignAttribute(HttpServletRequest request, HttpServletResponse response, BufferedWriter writer,
			boolean textOnly, String pathInfo) throws IOException {
		String[] parts = pathInfo.split("/");
		if (parts.length != 2) {
			appendLine(writer, textOnly, "Invalid number of parameters to assign command");
			if (!textOnly) {
				appendBackToRoot(writer);
			}
			return;
		}
		ObjectName objectName;
		try {
			objectName = new ObjectName(parts[0]);
		} catch (Exception e) {
			if (!textOnly) {
				appendHeader(writer);
				writer.append("<h1> Setting Bean Attribute </h1>\n");
			}
			appendLine(writer, textOnly, "Invalid object name: " + parts[0] + ": " + e);
			if (!textOnly) {
				appendBackToRoot(writer);
				appendFooter(writer);
			}
			return;
		}
		String attributeName = parts[1];

		MBeanInfo mbeanInfo;
		try {
			mbeanInfo = mbeanServer.getMBeanInfo(objectName);
		} catch (Exception e) {
			if (!textOnly) {
				appendHeader(writer);
				writer.append("<h1> Setting Attribute " + attributeName + "</h1>\n");
			}
			appendLine(writer, textOnly, "Could not get mbean info for: " + objectName + ": " + e);
			if (!textOnly) {
				appendBackToBean(writer, objectName);
				appendFooter(writer);
			}
			return;
		}
		MBeanAttributeInfo info = null;
		for (MBeanAttributeInfo attribute : mbeanInfo.getAttributes()) {
			if (attribute.getName().equals(attributeName)) {
				info = attribute;
				break;
			}
		}
		if (info == null) {
			if (!textOnly) {
				appendHeader(writer);
				writer.append("<h1> Setting Attribute " + attributeName + "</h1>\n");
			}
			appendLine(writer, textOnly, "Cannot find attribute: " + attributeName);
			if (!textOnly) {
				appendBackToBean(writer, objectName);
				appendFooter(writer);
			}
			return;
		}

		String result = request.getParameter(PARAM_ATTRIBUTE_VALUE);
		Object value = ClientUtils.stringToParam(result, info.getType());

		try {
			Attribute attribute = new Attribute(attributeName, value);
			mbeanServer.setAttribute(objectName, attribute);
			if (textOnly) {
				appendLine(writer, textOnly, attributeName + " set to " + value);
			} else {
				// redirect back to the display of the attribute if in html mode
				response.sendRedirect("/" + COMMAND_SHOW_BEAN + "/" + objectName);
			}
		} catch (Exception e) {
			if (!textOnly) {
				appendHeader(writer);
				writer.append("<h1> Setting Attribute " + attributeName + "</h1>\n");
			}
			appendLine(writer, textOnly, "Could not set attribute: " + attributeName + ": " + e);
			if (!textOnly) {
				appendBackToBean(writer, objectName);
				appendFooter(writer);
			}
			return;
		}
	}

	private void invokeOperation(HttpServletRequest request, BufferedWriter writer, boolean textOnly, String pathInfo)
			throws IOException {
		String[] parts = pathInfo.split("/");
		if (parts.length != 2) {
			appendLine(writer, textOnly, "Invalid number of parameters to invoke command");
			if (!textOnly) {
				appendBackToRoot(writer);
			}
			return;
		}
		ObjectName objectName;
		try {
			objectName = new ObjectName(parts[0]);
		} catch (MalformedObjectNameException mone) {
			appendLine(writer, textOnly, "Invalid object name: " + parts[0]);
			if (!textOnly) {
				appendBackToRoot(writer);
			}
			return;
		}
		String operationName = parts[1];

		if (!textOnly) {
			writer.append("<h1> Invoking Operation " + operationName + " </h1>\n");
		}
		MBeanInfo mbeanInfo;
		try {
			mbeanInfo = mbeanServer.getMBeanInfo(objectName);
		} catch (Exception e) {
			appendLine(writer, textOnly, "Could not get mbean info for: " + objectName + ": " + e);
			if (!textOnly) {
				appendBackToBean(writer, objectName);
			}
			return;
		}

		MBeanOperationInfo operation = null;
		for (MBeanOperationInfo info : mbeanInfo.getOperations()) {
			if (info.getName().equals(operationName)) {
				operation = info;
				break;
			}
		}
		if (operation == null) {
			appendLine(writer, textOnly, "Cannot find operation in " + objectName);
			if (!textOnly) {
				appendBackToBean(writer, objectName);
			}
			return;
		}

		MBeanParameterInfo[] paramInfos = operation.getSignature();
		Object[] params = new Object[paramInfos.length];
		String[] paramTypes = new String[paramInfos.length];
		for (int i = 0; i < paramInfos.length; i++) {
			paramTypes[i] = paramInfos[i].getType();
			params[i] = ClientUtils.stringToParam(request.getParameter(PARAM_OPERATION_PREFIX + i), paramTypes[i]);
		}

		Object result;
		try {
			result = mbeanServer.invoke(objectName, operationName, params, paramTypes);
		} catch (Exception e) {
			appendLine(writer, textOnly, "Invoking operation " + operationName + " threw exception: " + e);
			if (!textOnly) {
				appendBackToBean(writer, objectName);
			}
			e.printStackTrace();
			return;
		}

		if (operation.getReturnType().equals("void")) {
			appendLine(writer, textOnly, operationName + " method successfully invoked.");
		} else {
			appendLine(writer, textOnly, operationName + " result is: " + ClientUtils.valueToString(result));
		}
		if (!textOnly) {
			appendBackToBean(writer, objectName);
		}
	}

	private void displayClassInfo(BufferedWriter writer, MBeanInfo mbeanInfo) throws IOException {
		writer.append("ClassName: " + mbeanInfo.getClassName() + "<br />\n");
		if (mbeanInfo.getDescription() != null) {
			writer.append("Description: " + mbeanInfo.getDescription() + "<br />\n");
		}
		writer.append("<br />\n");
	}

	private void appendLine(BufferedWriter writer, boolean textOnly, String line) throws IOException {
		if (line != null) {
			writer.append(line);
		}
		if (!textOnly) {
			writer.append("<br />");
		}
		writer.append("\n");
	}

	private void appendHeader(BufferedWriter writer) throws IOException {
		writer.append("<html><body>\n");
	}

	private void appendFooter(BufferedWriter writer) throws IOException {
		appendLink(writer, false, "?t=1", "text", "Text version");
		writer.append(".  Produced by ");
		appendLink(writer, false, "http://256.com/sources/simplejmx/", "simplejmx", "SimpleJMX");
		appendLine(writer, false, null);
		writer.append("</body></html>\n");
	}

	private void appendBackToBean(BufferedWriter writer, ObjectName objectName) throws IOException {
		writer.append("<br />\n");
		appendLink(writer, false, '/' + COMMAND_SHOW_BEAN + '/' + objectName.toString(), "bean",
				"Back to bean information");
		writer.append("<br />\n");
	}

	private void appendBackToRoot(BufferedWriter writer) throws IOException {
		writer.append("<br />\n");
		appendLink(writer, false, "/", "root", "Back to root");
		writer.append("<br />\n");
	}

	private void appendBackToDomains(BufferedWriter writer, ObjectName objectName) throws IOException {
		writer.append("<br />\n");
		appendLink(writer, false, '/' + COMMAND_LIST_BEANS_IN_DOMAIN + '/' + objectName.getDomain(), "beans",
				"Back to beans in domain");
		writer.append("<br />\n");
	}

	private void appendLink(BufferedWriter writer, boolean textOnly, String url, String name, String text)
			throws IOException {
		if (!textOnly) {
			writer.append("<a href=\"" + url + "\" ");
			if (name != null) {
				writer.append("name=\"" + name + "\" ");
			}
			writer.append(">");
		}
		writer.append(text);
		if (!textOnly) {
			writer.append("</a>");
		}
	}

	private boolean isGetSet(String name) {
		return (name.startsWith("is") || name.startsWith("get") || name.startsWith("set"));
	}

	/**
	 * Limits the objects to a specific domain name.
	 */
	private static class DomainQueryExp implements QueryExp {
		private static final long serialVersionUID = 6041820913324186392L;
		private final String domain;
		public DomainQueryExp(String domain) {
			this.domain = domain;
		}
		public void setMBeanServer(MBeanServer s) {
			// no-op
		}
		public boolean apply(ObjectName name) {
			return domain.equals(name.getDomain());
		}
	}

	/**
	 * Compares two ObjectNames.
	 */
	private static class ObjectNameComparator implements Comparator<ObjectName> {
		public int compare(ObjectName o1, ObjectName o2) {
			return o1.toString().compareTo(o2.toString());
		}
	}
}
