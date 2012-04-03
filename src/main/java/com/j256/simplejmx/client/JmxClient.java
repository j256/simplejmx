package com.j256.simplejmx.client;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Set;

import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.j256.simplejmx.common.ObjectNameUtil;

/**
 * JMX client connection implementation which connects to a JMX server and gets JMX information, gets/sets attributes,
 * and invokes operations.
 * 
 * @author graywatson
 */
public class JmxClient {

	private JMXConnector jmxConnector;
	private JMXServiceURL serviceUrl;
	private MBeanServerConnection mbeanConn;
	private MBeanAttributeInfo[] attributes;
	private MBeanOperationInfo[] operations;

	/**
	 * Connect the client to a JMX server using the full JMX URL format. The URL should look something like:
	 * <p>
	 * 
	 * <pre>
	 * service:jmx:rmi:///jndi/rmi://hostName:portNumber/jmxrmi
	 * </pre>
	 * 
	 * </p>
	 */
	public JmxClient(String jmxUrl) throws JMException {
		if (jmxUrl == null) {
			throw new IllegalArgumentException("Jmx URL cannot be null");
		}

		try {
			this.serviceUrl = new JMXServiceURL(jmxUrl);
		} catch (MalformedURLException e) {
			throw createJmException("JmxServiceUrl was malformed: " + jmxUrl, e);
		}

		try {
			jmxConnector = JMXConnectorFactory.connect(serviceUrl, null);
			mbeanConn = jmxConnector.getMBeanServerConnection();
		} catch (IOException e) {
			if (jmxConnector != null) {
				try {
					jmxConnector.close();
				} catch (IOException e1) {
					// ignore, we did our best
				}
				jmxConnector = null;
			}
			throw createJmException("Problems connecting to the server" + e, e);
		}
	}

	/**
	 * Connect the client to the local host at a certain port number.
	 */
	public JmxClient(int localPort) throws JMException {
		this(generalJmxUrlForHostNamePort("", localPort));
	}

	/**
	 * Connect the client to a host and port combination.
	 */
	public JmxClient(String hostName, int port) throws JMException {
		this(generalJmxUrlForHostNamePort(hostName, port));
	}

	/**
	 * Returns a JMX/RMI URL for a host-name and port.
	 */
	public static String generalJmxUrlForHostNamePort(String hostName, int port) {
		return "service:jmx:rmi:///jndi/rmi://" + hostName + ":" + port + "/jmxrmi";
	}

	/**
	 * Close the client connection to the mbean server.If you want a method that throws then use {@link #closeThrow()}.
	 */
	public synchronized void close() {
		try {
			closeThrow();
		} catch (JMException e) {
			// ignored
		}
	}

	/**
	 * Close the client connection to the mbean server. If you want a method that does not throw then use
	 * {@link #close()}.
	 */
	public synchronized void closeThrow() throws JMException {
		try {
			if (jmxConnector != null) {
				jmxConnector.close();
				jmxConnector = null;
			}
			// NOTE: doesn't seem to be close method on mbsc
			mbeanConn = null;
		} catch (IOException e) {
			throw createJmException("Could not close the jmx connector", e);
		}
	}

	/**
	 * Return an array of the bean's domain names.
	 */
	public String[] getBeanDomains() throws JMException {
		checkClientConnected();

		try {
			return mbeanConn.getDomains();
		} catch (IOException e) {
			throw createJmException("Problems getting jmx domains: " + e, e);
		}
	}

	/**
	 * Return a set of the various bean names associated with the Jmx server.
	 */
	public Set<ObjectName> getBeanNames() throws JMException {
		checkClientConnected();

		try {
			return mbeanConn.queryNames(null, null);
		} catch (IOException e) {
			throw createJmException("Problems querying for jmx bean names: " + e, e);
		}
	}

	/**
	 * Return an array of the attributes associated with the bean name.
	 */
	public MBeanAttributeInfo[] getAttributesInfo(String domainName, String name) throws JMException {
		return getAttributesInfo(ObjectNameUtil.makeObjectName(domainName, name));
	}

	/**
	 * Return an array of the attributes associated with the bean name.
	 */
	public MBeanAttributeInfo[] getAttributesInfo(ObjectName name) throws JMException {
		checkClientConnected();

		try {
			return mbeanConn.getMBeanInfo(name).getAttributes();
		} catch (Exception e) {
			throw createJmException("Problems getting bean information from " + name, e);
		}
	}

	/**
	 * Return information for a particular attribute name.
	 */
	public MBeanAttributeInfo getAttributeInfo(ObjectName name, String attrName) throws JMException {
		checkClientConnected();

		return getAttrInfo(name, attrName);
	}

	/**
	 * Return an array of the operations associated with the bean name.
	 */
	public MBeanOperationInfo[] getOperationsInfo(ObjectName name) throws JMException {
		checkClientConnected();

		try {
			return mbeanConn.getMBeanInfo(name).getOperations();
		} catch (Exception e) {
			throw createJmException("Problems getting bean information from " + name, e);
		}
	}

	/**
	 * Return an array of the operations associated with the bean name.
	 */
	public MBeanOperationInfo getOperationInfo(ObjectName name, String oper) throws JMException {
		checkClientConnected();

		MBeanInfo mbeanInfo;
		try {
			mbeanInfo = mbeanConn.getMBeanInfo(name);
		} catch (Exception e) {
			throw createJmException("Problems getting bean information from " + name, e);
		}
		for (MBeanOperationInfo info : mbeanInfo.getOperations()) {
			if (oper.equals(info.getName())) {
				return info;
			}
		}
		return null;
	}

	/**
	 * Return the value of a JMX attribute.
	 */
	public Object getAttribute(String domain, String objectName, String attributeName) throws Exception {
		return getAttribute(ObjectNameUtil.makeObjectName(domain, objectName), attributeName);
	}

	/**
	 * Return the value of a JMX attribute.
	 */
	public Object getAttribute(ObjectName name, String attributeName) throws Exception {
		checkClientConnected();
		return mbeanConn.getAttribute(name, attributeName);
	}

	/**
	 * Return the value of a JMX attribute as a String.
	 */
	public String getAttributeString(String domain, String objectName, String attributeName) throws Exception {
		return getAttributeString(ObjectNameUtil.makeObjectName(domain, objectName), attributeName);
	}

	/**
	 * Return the value of a JMX attribute as a String or null if attribute has a null value.
	 */
	public String getAttributeString(ObjectName name, String attributeName) throws Exception {
		Object bean = getAttribute(name, attributeName);
		if (bean == null) {
			return null;
		} else {
			return bean.toString();
		}
	}

	/**
	 * Set the JMX attribute to a particular value string.
	 */
	public void setAttribute(String domainName, String objectName, String attrName, String value) throws Exception {
		setAttribute(ObjectNameUtil.makeObjectName(domainName, objectName), attrName, value);
	}

	/**
	 * Set the JMX attribute to a particular value string.
	 */
	public void setAttribute(ObjectName name, String attrName, String value) throws Exception {
		MBeanAttributeInfo info = getAttrInfo(name, attrName);
		setAttribute(name, attrName, stringToObject(value, info.getType()));
	}

	/**
	 * Set the JMX attribute to a particular value string.
	 */
	public void setAttribute(String domainName, String objectName, String attrName, Object value) throws Exception {
		setAttribute(ObjectNameUtil.makeObjectName(domainName, objectName), attrName, value);
	}

	/**
	 * Set the JMX attribute to a particular value.
	 */
	public void setAttribute(ObjectName name, String attrName, Object value) throws Exception {
		checkClientConnected();

		Attribute attribute = new Attribute(attrName, value);
		mbeanConn.setAttribute(name, attribute);
	}

	/**
	 * Invoke a JMX method with a domain/object-name as an array of parameter strings.
	 * 
	 * @return The value returned by the method or null if none.
	 */
	public Object invokeOperation(String domain, String objectName, String operName, String... paramStrings)
			throws Exception {
		return invokeOperation(ObjectNameUtil.makeObjectName(domain, objectName), operName, paramStrings);
	}

	/**
	 * Invoke a JMX method as an array of parameter strings.
	 * 
	 * @return The value returned by the method or null if none.
	 */
	public Object invokeOperation(ObjectName name, String operName, String... paramStrings) throws Exception {
		String[] paramTypes = lookupParamTypes(name, operName, paramStrings);
		Object[] paramObjs;
		if (paramStrings.length == 0) {
			paramObjs = null;
		} else {
			paramObjs = new Object[paramStrings.length];
		}
		for (int i = 0; i < paramStrings.length; i++) {
			paramObjs[i] = stringToObject(paramStrings[i], paramTypes[i]);
		}
		return invokeOperation(name, operName, paramTypes, paramObjs);
	}

	/**
	 * Invoke a JMX method as an array of parameter strings.
	 * 
	 * @return The value returned by the method as a string or null if none.
	 */
	public String invokeOperationToString(ObjectName name, String operName, String... paramStrings) throws Exception {
		return invokeOperation(name, operName, paramStrings).toString();
	}

	/**
	 * Invoke a JMX method as an array of objects.
	 * 
	 * @return The value returned by the method or null if none.
	 */
	public Object invokeOperation(String domain, String objectName, String operName, Object... params) throws Exception {
		return invokeOperation(ObjectNameUtil.makeObjectName(domain, objectName), operName, params);
	}

	/**
	 * Invoke a JMX method as an array of objects.
	 * 
	 * @return The value returned by the method or null if none.
	 */
	public Object invokeOperation(ObjectName objectName, String operName, Object... params) throws Exception {
		String[] paramTypes = lookupParamTypes(objectName, operName, params);
		return invokeOperation(objectName, operName, paramTypes, params);
	}

	private Object invokeOperation(ObjectName objectName, String operName, String[] paramTypes, Object[] params)
			throws Exception {
		if (params != null && params.length == 0) {
			params = null;
		}
		return mbeanConn.invoke(objectName, operName, params, paramTypes);
	}

	private String[] lookupParamTypes(ObjectName objectName, String operName, Object[] params) throws JMException {
		checkClientConnected();

		if (operations == null) {
			try {
				operations = mbeanConn.getMBeanInfo(objectName).getOperations();
			} catch (Exception e) {
				throw createJmException("Cannot get attribute info from " + objectName, e);
			}
		}
		String[] paramTypes = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			paramTypes[i] = params[i].getClass().toString();
		}
		int nameC = 0;
		String[] first = null;
		for (MBeanOperationInfo info : operations) {
			if (!info.getName().equals(operName)) {
				continue;
			}
			MBeanParameterInfo[] mbeanParams = info.getSignature();
			if (params.length != mbeanParams.length) {
				continue;
			}
			String[] signatureTypes = new String[mbeanParams.length];
			for (int i = 0; i < params.length; i++) {
				signatureTypes[i] = mbeanParams[i].getType();
			}
			if (Arrays.equals(paramTypes, signatureTypes)) {
				return signatureTypes;
			}
			first = signatureTypes;
			nameC++;
		}

		if (first == null) {
			throw new IllegalArgumentException("Cannot find operation named '" + operName + "'");
		} else if (nameC > 1) {
			throw new IllegalArgumentException("Cannot find operation named '" + operName
					+ "' with matching argument types");
		} else {
			// return the first one we found that matches the name
			return first;
		}
	}

	private void checkClientConnected() {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}
	}

	private MBeanAttributeInfo getAttrInfo(ObjectName objectName, String attrName) throws JMException {
		if (attributes == null) {
			try {
				attributes = mbeanConn.getMBeanInfo(objectName).getAttributes();
			} catch (Exception e) {
				throw createJmException("Cannot get attribute info from " + objectName, e);
			}
		}
		for (MBeanAttributeInfo info : attributes) {
			if (info.getName().equals(attrName)) {
				return info;
			}
		}
		return null;
	}

	private Object stringToObject(String string, String typeString) throws IllegalArgumentException {
		if (typeString.equals("boolean") || typeString.equals("java.lang.Boolean")) {
			return Boolean.parseBoolean(string);
		} else if (typeString.equals("char") || typeString.equals("java.lang.Character")) {
			if (string.length() == 0) {
				// not sure what to do here
				return '\0';
			} else {
				return string.toCharArray()[0];
			}
		} else if (typeString.equals("byte") || typeString.equals("java.lang.Byte")) {
			return Byte.parseByte(string);
		} else if (typeString.equals("short") || typeString.equals("java.lang.Short")) {
			return Short.parseShort(string);
		} else if (typeString.equals("int") || typeString.equals("java.lang.Integer")) {
			return Integer.parseInt(string);
		} else if (typeString.equals("long") || typeString.equals("java.lang.Long")) {
			return Long.parseLong(string);
		} else if (typeString.equals("java.lang.String")) {
			return string;
		} else if (typeString.equals("float") || typeString.equals("java.lang.Float")) {
			return Float.parseFloat(string);
		} else if (typeString.equals("double") || typeString.equals("java.lang.Double")) {
			return Double.parseDouble(string);
		} else {
			Constructor<?> constr = getConstructor(typeString);
			try {
				return constr.newInstance(new Object[] { string });
			} catch (Exception e) {
				throw new IllegalArgumentException("Could not get new instance using string constructor for type "
						+ typeString);
			}
		}
	}

	private <C> Constructor<C> getConstructor(String typeString) throws IllegalArgumentException {
		Class<Object> clazz;
		try {
			@SuppressWarnings("unchecked")
			Class<Object> clazzCast = (Class<Object>) Class.forName(typeString);
			clazz = clazzCast;
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Unknown class for type " + typeString);
		}
		try {
			@SuppressWarnings("unchecked")
			Constructor<C> constructor = (Constructor<C>) clazz.getConstructor(new Class[] { String.class });
			return constructor;
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not find string constructor for " + clazz);
		}
	}

	private JMException createJmException(String message, Exception e) {
		JMException jmException = new JMException(message);
		jmException.initCause(e);
		return jmException;
	}
}
