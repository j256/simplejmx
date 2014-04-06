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
 * Simple JMX handler that displays JMX information for a HTTP request using Jetty. To use this class you need to
 * provide a Jetty version in your dependency list or classpath.
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

	private static boolean charIsMapped[] = new boolean[256];

	static {
		// for <input values
		charIsMapped['\''] = true;
		charIsMapped['\"'] = true;
		// general protection
		charIsMapped['<'] = true;
		charIsMapped['>'] = true;
		charIsMapped['&'] = true;
		// for URLs
		charIsMapped[':'] = true;
		charIsMapped['/'] = true;
		charIsMapped['+'] = true;
		charIsMapped['?'] = true;
	}

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
			appendHeader(writer, textOnly);
			if (pathInfo.equals(COMMAND_SHOW_ALL_BEANS)) {
				listBeansInDomain(writer, textOnly, null);
			} else {
				listDomains(writer, textOnly);
				if (!textOnly) {
					writer.append("<br />\n");
					appendLink(writer, textOnly, '/' + COMMAND_SHOW_ALL_BEANS, "all", null, "Show all beans.");
					writer.append("  ");
				}
			}
			appendFooter(writer, textOnly);
			return;
		}

		String command = pathInfo.substring(0, slashIndex);
		pathInfo = pathInfo.substring(slashIndex + 1);

		if (command.equals(COMMAND_LIST_BEANS_IN_DOMAIN)) {
			appendHeader(writer, textOnly);
			listBeansInDomain(writer, textOnly, pathInfo);
			appendFooter(writer, textOnly);
		} else if (command.equals(COMMAND_SHOW_BEAN)) {
			appendHeader(writer, textOnly);
			showBean(writer, textOnly, pathInfo);
			appendFooter(writer, textOnly);
		} else if (command.equals(COMMAND_ASSIGN_ATTRIBUTE)) {
			// this may redirect
			assignAttribute(request, response, writer, textOnly, pathInfo);
		} else if (command.equals(COMMAND_INVOKE_OPERATION)) {
			appendHeader(writer, textOnly);
			invokeOperation(request, writer, textOnly, pathInfo);
			appendFooter(writer, textOnly);
		} else {
			response.sendError(Response.SC_NOT_FOUND);
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
			appendLink(writer, textOnly, '/' + COMMAND_LIST_BEANS_IN_DOMAIN + '/' + makeHtmlSafe(domainName), "beans",
					null, domainName);
			appendLine(writer, textOnly, null);
		}
	}

	private void listBeansInDomain(BufferedWriter writer, boolean textOnly, String domainName) throws IOException {
		// NOTE: should we show directories here or maybe do cute javascript arrows to hide/show sub-beans?
		if (!textOnly) {
			if (domainName == null) {
				writer.append("<h1> All Beans </h1>\n");
			} else {
				writer.append("<h1> Beans in domain " + makeHtmlSafe(domainName) + " </h1>\n");
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
			String description = null;
			try {
				MBeanInfo mbeanInfo = mbeanServer.getMBeanInfo(objectName);
				description = mbeanInfo.getDescription();
			} catch (Exception e) {
				// ignored
			}
			String nameString = objectName.toString();
			if (textOnly) {
				writer.append(nameString + '\n');
			} else {
				String display = nameString;
				if (domainName != null && display.startsWith(domainName)) {
					// if we are looking at a domain name, remove it from the displayed beans
					display = nameString.substring(domainName.length() + 1, nameString.length());
				}
				appendLink(writer, textOnly, '/' + COMMAND_SHOW_BEAN + '/' + makeHtmlSafe(nameString), nameString,
						description, display);
				appendLine(writer, textOnly, null);
			}
		}
		appendBackToRoot(writer, textOnly);
	}

	private void showBean(BufferedWriter writer, boolean textOnly, String objectNameString) throws IOException {
		ObjectName objectName;
		try {
			objectName = new ObjectName(objectNameString);
		} catch (MalformedObjectNameException mone) {
			appendLine(writer, textOnly, "Invalid object name: " + makeHtmlSafe(objectNameString));
			appendBackToRoot(writer, textOnly);
			return;
		}
		MBeanInfo mbeanInfo;
		try {
			mbeanInfo = mbeanServer.getMBeanInfo(objectName);
		} catch (Exception e) {
			appendLine(writer, textOnly, "Investigating object threw exception: " + e);
			appendBackToRoot(writer, textOnly);;
			return;
		}
		if (!textOnly) {
			writer.append("<h1> Information about object " + makeHtmlSafe(objectNameString) + " </h1>\n");
			displayClassInfo(writer, mbeanInfo);
		}
		displayAttributes(writer, textOnly, objectName, mbeanInfo);
		displayOperations(writer, textOnly, objectName, mbeanInfo);
		appendBackToDomains(writer, textOnly, objectName);
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
			writer.append("<tr><td title='" + makeHtmlSafe(attribute.getDescription()) + "'> " + name + " </td>");
			writer.append("<td> " + ClientUtils.displayType(attribute.getType(), value) + " </td>");
			if (attribute.isWritable()) {
				writer.append("<form action='/" + COMMAND_ASSIGN_ATTRIBUTE + "/" + makeHtmlSafe(objectName.toString())
						+ "/" + makeHtmlSafe(name) + "' name='" + makeHtmlSafe(name) + "'>\n");
				writer.append("<td>");
				writer.append("<input name='" + PARAM_ATTRIBUTE_VALUE + "' value='" + makeHtmlSafe(valueString) + "' >");
				writer.append("</td>");
				writer.append("</form>\n");
			} else {
				writer.append("<td> " + ClientUtils.valueToString(value) + " </td>");
			}
			writer.append("</tr>\n");
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
				writer.append("<tr>\n");
				writer.append("<form action='/" + COMMAND_INVOKE_OPERATION + "/" + makeHtmlSafe(objectName.toString())
						+ "/" + makeHtmlSafe(name) + "' name='" + makeHtmlSafe(name) + "'>\n");
				writer.append("<td title='" + makeHtmlSafe(operation.getDescription()) + "'> " + makeHtmlSafe(name)
						+ " </td>");
				writer.append("<td> " + ClientUtils.displayType(operation.getReturnType(), null) + " </td>");
			}
			MBeanParameterInfo[] params = operation.getSignature();
			int paramCount = 0;
			for (MBeanParameterInfo param : params) {
				if (textOnly) {
					writer.append(" " + param.getType());
				} else {
					writer.append("<td> <input name='" + PARAM_OPERATION_PREFIX + paramCount++ + "' value='"
							+ makeHtmlSafe(param.getName()) + "' > (" + makeHtmlSafe(param.getType()) + ")</td>");
				}
			}
			if (textOnly) {
				writer.append("\n");
			} else {
				for (; paramCount < maxParams; paramCount++) {
					writer.append("<td> &nbsp; </td>");
				}
				writer.append("<td><input type='submit' value='" + makeHtmlSafe(name) + "' title='"
						+ makeHtmlSafe(operation.getDescription()) + "' /></td>");
				writer.append("</form>\n");
				writer.append("</tr>\n");
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
			appendBackToRoot(writer, textOnly);
			return;
		}
		String param = request.getParameter(PARAM_ATTRIBUTE_VALUE);
		if (param == null) {
			appendAssignError(writer, textOnly, "No value parameter specified.");
			return;
		}
		ObjectName objectName;
		try {
			objectName = new ObjectName(parts[0]);
		} catch (Exception e) {
			appendAssignError(writer, textOnly, "Invalid object name: " + makeHtmlSafe(parts[0]) + ": " + e);
			return;
		}
		String attributeName = parts[1];

		MBeanInfo mbeanInfo;
		try {
			mbeanInfo = mbeanServer.getMBeanInfo(objectName);
		} catch (Exception e) {
			appendAssignError(writer, textOnly, "Could not get mbean info for: " + makeHtmlSafe(objectName.toString())
					+ ": " + e);
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
			appendAssignError(writer, textOnly, "Cannot find attribute: " + makeHtmlSafe(attributeName));
			return;
		}

		try {
			Object value = ClientUtils.stringToParam(param, info.getType());
			Attribute attribute = new Attribute(attributeName, value);
			mbeanServer.setAttribute(objectName, attribute);
			if (textOnly) {
				appendLine(writer, textOnly, attributeName + " set to " + value);
			} else {
				// redirect back to the display of the attribute if in html mode
				response.sendRedirect("/" + COMMAND_SHOW_BEAN + "/" + objectName);
			}
		} catch (Exception e) {
			appendAssignError(writer, textOnly, "Could not set attribute: " + makeHtmlSafe(attributeName) + ": " + e);
			return;
		}
	}

	private void appendAssignError(BufferedWriter writer, boolean textOnly, String message) throws IOException {
		if (!textOnly) {
			appendHeader(writer, textOnly);
			writer.append("<h1> Setting Bean Attribute </h1>\n");
		}
		appendLine(writer, textOnly, message);
		appendBackToRoot(writer, textOnly);
		appendFooter(writer, textOnly);
	}

	private void invokeOperation(HttpServletRequest request, BufferedWriter writer, boolean textOnly, String pathInfo)
			throws IOException {
		String[] parts = pathInfo.split("/");
		if (parts.length != 2) {
			appendLine(writer, textOnly, "Invalid number of parameters to invoke command");
			appendBackToRoot(writer, textOnly);
			return;
		}
		ObjectName objectName;
		try {
			objectName = new ObjectName(parts[0]);
		} catch (MalformedObjectNameException mone) {
			appendLine(writer, textOnly, "Invalid object name: " + makeHtmlSafe(parts[0]));
			appendBackToRoot(writer, textOnly);
			return;
		}
		String operationName = parts[1];

		if (!textOnly) {
			writer.append("<h1> Invoking Operation " + makeHtmlSafe(operationName) + " </h1>\n");
		}
		MBeanInfo mbeanInfo;
		try {
			mbeanInfo = mbeanServer.getMBeanInfo(objectName);
		} catch (Exception e) {
			appendLine(writer, textOnly, "Could not get mbean info for: " + makeHtmlSafe(objectName.toString()) + ": "
					+ e);
			appendBackToBean(writer, textOnly, objectName);
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
			appendLine(writer, textOnly, "Cannot find operation in " + makeHtmlSafe(objectName.toString()));
			appendBackToBean(writer, textOnly, objectName);
			return;
		}

		MBeanParameterInfo[] paramInfos = operation.getSignature();
		Object[] params = new Object[paramInfos.length];
		String[] paramTypes = new String[paramInfos.length];
		for (int i = 0; i < paramInfos.length; i++) {
			paramTypes[i] = paramInfos[i].getType();
			try {
				params[i] = ClientUtils.stringToParam(request.getParameter(PARAM_OPERATION_PREFIX + i), paramTypes[i]);
			} catch (IllegalArgumentException iae) {
				appendLine(writer, textOnly, "Converting parameter " + paramInfos[i].getName() + " threw exception: "
						+ iae);
				appendBackToBean(writer, textOnly, objectName);
				return;
			}
		}

		Object result;
		try {
			result = mbeanServer.invoke(objectName, operationName, params, paramTypes);
		} catch (Exception e) {
			appendLine(writer, textOnly, "Invoking operation " + makeHtmlSafe(operationName) + " threw exception: " + e);
			appendBackToBean(writer, textOnly, objectName);
			return;
		}

		if (operation.getReturnType().equals("void")) {
			appendLine(writer, textOnly, makeHtmlSafe(operationName) + " method successfully invoked.");
		} else {
			appendLine(writer, textOnly,
					operationName + " result is: " + makeHtmlSafe(ClientUtils.valueToString(result)));
		}
		appendBackToBean(writer, textOnly, objectName);
	}

	private void displayClassInfo(BufferedWriter writer, MBeanInfo mbeanInfo) throws IOException {
		writer.append("ClassName: " + mbeanInfo.getClassName() + "<br />\n");
		if (mbeanInfo.getDescription() != null) {
			writer.append("Description: " + makeHtmlSafe(mbeanInfo.getDescription()) + "<br />\n");
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

	private void appendHeader(BufferedWriter writer, boolean textOnly) throws IOException {
		if (!textOnly) {
			writer.append("<html><body>\n");
		}
	}

	private void appendFooter(BufferedWriter writer, boolean textOnly) throws IOException {
		if (!textOnly) {
			appendLink(writer, false, "?t=1", "text", null, "Text version");
			writer.append(".  Produced by ");
			appendLink(writer, false, "http://256.com/sources/simplejmx/", "simplejmx", null, "SimpleJMX");
			appendLine(writer, false, null);
			writer.append("</body></html>\n");
		}
	}

	private void appendBackToBean(BufferedWriter writer, boolean textOnly, ObjectName objectName) throws IOException {
		if (!textOnly) {
			writer.append("<br />\n");
			appendLink(writer, false, '/' + COMMAND_SHOW_BEAN + '/' + objectName.toString(), "bean", null,
					"Back to bean information");
			writer.append("<br />\n");
		}
	}

	private void appendBackToRoot(BufferedWriter writer, boolean textOnly) throws IOException {
		if (!textOnly) {
			writer.append("<br />\n");
			appendLink(writer, false, "/", "root", null, "Back to root");
			writer.append("<br />\n");
		}
	}

	private void appendBackToDomains(BufferedWriter writer, boolean textOnly, ObjectName objectName) throws IOException {
		if (!textOnly) {
			writer.append("<br />\n");
			appendLink(writer, false, '/' + COMMAND_LIST_BEANS_IN_DOMAIN + '/' + objectName.getDomain(), "beans", null,
					"Back to beans in domain");
			writer.append("<br />\n");
		}
	}

	private void appendLink(BufferedWriter writer, boolean textOnly, String url, String name, String title, String text)
			throws IOException {
		if (!textOnly) {
			writer.append("<a href='" + url + "' ");
			if (name != null) {
				writer.append("name='" + makeHtmlSafe(name) + "' ");
			}
			if (title != null) {
				writer.append("title='" + makeHtmlSafe(title) + "' ");
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

	private String makeHtmlSafe(String value) {
		if (value == null) {
			return null;
		}

		boolean needsMap = false;
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch < charIsMapped.length && charIsMapped[ch]) {
				needsMap = true;
				break;
			}
		}
		if (!needsMap) {
			return value;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch < charIsMapped.length && charIsMapped[ch]) {
				sb.append("&#");
				sb.append(Integer.toString(ch));
				sb.append(';');
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
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
