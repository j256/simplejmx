package com.j256.simplejmx.client;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * JMX client connection implementation.
 * 
 * @author graywatson
 */
public class JmxClient {

	private JMXConnector jmxConnector;
	private JMXServiceURL serviceUrl;
	private MBeanServerConnection mbeanConn;
	private MBeanAttributeInfo[] attributes;
	private MBeanOperationInfo[] operations;

	public JmxClient(String url) throws IllegalArgumentException {
		if (url == null) {
			throw new NullPointerException("Jmx URL cannot be null");
		}

		try {
			this.serviceUrl = new JMXServiceURL(url);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("JmxServiceUrl was malformed: " + url, e);
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
			throw new IllegalArgumentException("Problems connecting to the server" + e, e);
		}
	}

	public JmxClient(int localPort) throws IllegalArgumentException {
		this(hostPortToUrl("", localPort));
	}

	public JmxClient(String host, int port) throws IllegalArgumentException {
		this(hostPortToUrl(host, port));
	}

	public static String hostPortToUrl(String host, int port) {
		return "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
	}

	/**
	 * Close the client connection to the mbean server.
	 */
	public synchronized void close() {
		try {
			if (jmxConnector != null) {
				jmxConnector.close();
				jmxConnector = null;
			}
			// NOTE: doesn't seem to be close method on mbsc
			mbeanConn = null;
		} catch (IOException e) {
		}
	}

	/**
	 * @return Array of the bean's domain names.
	 */
	public String[] getBeanDomains() throws IllegalArgumentException {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}

		try {
			return mbeanConn.getDomains();
		} catch (IOException e) {
			throw new IllegalArgumentException("Problems getting jmx domains: " + e, e);
		}
	}

	/**
	 * @return Set of the various bean names associated with the Jmx server.
	 */
	public Set<ObjectName> getBeanNames() throws IllegalArgumentException {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}

		try {
			return mbeanConn.queryNames(null, null);
		} catch (IOException e) {
			throw new IllegalArgumentException("Problems querying for jmx bean names: " + e, e);
		}
	}

	/**
	 * @return Array of the attributes associated with the bean name.
	 */
	public MBeanAttributeInfo[] getAttributesInfo(ObjectName name) throws IllegalArgumentException {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}

		try {
			return mbeanConn.getMBeanInfo(name).getAttributes();
		} catch (Exception e) {
			throw new IllegalArgumentException("Problems getting bean information from " + name, e);
		}
	}

	/**
	 * @return Info for a particular attribute name.
	 */
	public MBeanAttributeInfo getAttributeInfo(ObjectName name, String attrName) throws IllegalArgumentException {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}

		MBeanInfo mbeanInfo;
		try {
			mbeanInfo = mbeanConn.getMBeanInfo(name);
		} catch (Exception e) {
			throw new IllegalArgumentException("Problems getting bean information from " + name, e);
		}
		for (MBeanAttributeInfo info : mbeanInfo.getAttributes()) {
			if (info.getName().equals(attrName)) {
				return info;
			}
		}
		return null;
	}

	/**
	 * @return Array of the operations associated with the bean name.
	 */
	public MBeanOperationInfo[] getOperationsInfo(ObjectName name) throws IllegalArgumentException {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}

		try {
			return mbeanConn.getMBeanInfo(name).getOperations();
		} catch (Exception e) {
			throw new IllegalArgumentException("Problems getting bean information from " + name, e);
		}
	}

	/**
	 * @return Array of the operations associated with the bean name.
	 */
	public MBeanOperationInfo getOperationInfo(ObjectName name, String oper) throws IllegalArgumentException {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}

		MBeanInfo mbeanInfo;
		try {
			mbeanInfo = mbeanConn.getMBeanInfo(name);
		} catch (Exception e) {
			throw new IllegalArgumentException("Problems getting bean information from " + name, e);
		}
		for (MBeanOperationInfo info : mbeanInfo.getOperations()) {
			if (oper.equals(info.getName())) {
				return info;
			}
		}
		return null;
	}

	/**
	 * @return The value of a JMX attribute.
	 */
	public Object getAttribute(ObjectName name, String attributeName) throws IllegalArgumentException {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}

		try {
			return mbeanConn.getAttribute(name, attributeName);
		} catch (Exception e) {
			throw new IllegalArgumentException("Problems getting " + attributeName + " from " + name, e);
		}
	}

	/**
	 * @return The value of a JMX attribute.
	 */
	public String getAttributeString(ObjectName name, String attributeName) throws IllegalArgumentException {
		Object bean = getAttribute(name, attributeName);
		if (bean == null) {
			return "(null)";
		} else {
			return bean.toString();
		}
	}

	/**
	 * Set the JMX attribute to a particular value string.
	 */
	public void setAttribute(ObjectName name, String attrName, String value) throws IllegalArgumentException {
		MBeanAttributeInfo info = getAttrInfo(name, attrName);
		setAttribute(name, attrName, stringToObject(value, info.getType()));
	}

	/**
	 * Set the JMX attribute to a particular value.
	 */
	public void setAttribute(ObjectName name, String attrName, Object value) throws IllegalArgumentException {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}

		Attribute attribute = new Attribute(attrName, value);
		try {
			mbeanConn.setAttribute(name, attribute);
		} catch (Exception e) {
			throw new IllegalArgumentException("Problems setting " + attribute + " from " + name, e);
		}
	}

	/**
	 * Invoke a JMX method.
	 * 
	 * @return The value returned by the method or null if none.
	 */
	public Object invokeOperation(ObjectName name, String operName, String[] paramStrings)
			throws IllegalArgumentException {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}

		MBeanOperationInfo info = getOperInfo(name, operName);
		if (info == null) {
			throw new IllegalArgumentException("Cannot get operation info from " + name + " for operation " + operName);
		}
		MBeanParameterInfo[] paraminfos = info.getSignature();
		if ((paraminfos == null && paramStrings.length != 0)
				|| (paraminfos != null && paramStrings.length != paraminfos.length)) {
			throw new IllegalArgumentException("Passed param count does not match signature count");
		}
		String[] signature = new String[paramStrings.length];
		Object[] paramObjs;
		if (paramStrings.length == 0) {
			paramObjs = null;
		} else {
			paramObjs = new Object[paramStrings.length];
		}
		for (int i = 0; i < paramStrings.length; i++) {
			MBeanParameterInfo paraminfo = paraminfos[i];
			paramObjs[i] = stringToObject(paramStrings[i], paraminfo.getType());
			signature[i] = paraminfo.getType();
		}
		try {
			return mbeanConn.invoke(name, operName, paramObjs, signature);
		} catch (Exception e) {
			throw new IllegalArgumentException("Problems invoking " + operName + " on " + name, e);
		}
	}

	/**
	 * Invoke a JMX method.
	 * 
	 * @return The value returned by the method or null if none.
	 */
	public String invokeOperationToString(ObjectName name, String operName, String[] paramStrings)
			throws IllegalArgumentException {
		return invokeOperation(name, operName, paramStrings).toString();
	}

	private MBeanOperationInfo getOperInfo(ObjectName name, String attrName) throws IllegalArgumentException {
		if (operations == null) {
			try {
				operations = mbeanConn.getMBeanInfo(name).getOperations();
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot get attribute info from " + name, e);
			}
		}
		for (MBeanOperationInfo info : operations) {
			if (info.getName().equals(attrName)) {
				return info;
			}
		}
		return null;
	}

	private MBeanAttributeInfo getAttrInfo(ObjectName name, String attrName) throws IllegalArgumentException {
		if (attributes == null) {
			try {
				attributes = mbeanConn.getMBeanInfo(name).getAttributes();
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot get attribute info from " + name, e);
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
			Class<Object> clazz = getClassFromString(typeString);
			Constructor<?> constr = getConstructor(typeString, clazz);
			try {
				return constr.newInstance(new Object[] { string });
			} catch (Exception e) {
				throw new IllegalArgumentException("Could not get new instance using string constructor for type "
						+ typeString);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Class<Object> getClassFromString(String typeString) throws IllegalArgumentException {
		try {
			return (Class<Object>) Class.forName(typeString);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Unknown class for type " + typeString);
		}
	}

	@SuppressWarnings("unchecked")
	private <C> Constructor<C> getConstructor(String typeString, Class<C> clazz) throws IllegalArgumentException {
		try {
			return (Constructor<C>) Class.forName(typeString).getConstructor(new Class[] { clazz });
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not find string constructor for type " + typeString);
		}
	}
}
