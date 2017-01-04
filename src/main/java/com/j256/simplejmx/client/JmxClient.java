package com.j256.simplejmx.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
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
public class JmxClient implements Closeable {

	private JMXConnector jmxConnector;
	private JMXServiceURL serviceUrl;
	private MBeanServerConnection mbeanConn;

	private final static Map<String, String> primitiveObjectMap = new HashMap<String, String>();

	static {
		primitiveObjectMap.put(boolean.class.getName(), Boolean.class.getName());
		primitiveObjectMap.put(byte.class.getName(), Byte.class.getName());
		primitiveObjectMap.put(char.class.getName(), Character.class.getName());
		primitiveObjectMap.put(short.class.getName(), Short.class.getName());
		primitiveObjectMap.put(int.class.getName(), Integer.class.getName());
		primitiveObjectMap.put(long.class.getName(), Long.class.getName());
		primitiveObjectMap.put(float.class.getName(), Float.class.getName());
		primitiveObjectMap.put(double.class.getName(), Double.class.getName());
		// NOTE: don't need void/Void
	}

	/**
	 * <p>
	 * Connect the client to a JMX server using the full JMX URL format. The URL should look something like:
	 * </p>
	 * 
	 * <pre>
	 * service:jmx:rmi:///jndi/rmi://hostName:portNumber/jmxrmi
	 * </pre>
	 */
	public JmxClient(String jmxUrl) throws JMException {
		this(jmxUrl, null, null);
	}

	/**
	 * <p>
	 * Connect the client to a JMX server using the full JMX URL format with username/password credentials. The URL
	 * should look something like:
	 * </p>
	 * 
	 * <pre>
	 * service:jmx:rmi:///jndi/rmi://hostName:portNumber/jmxrmi
	 * </pre>
	 */
	public JmxClient(String jmxUrl, String userName, String password) throws JMException {
		if (jmxUrl == null) {
			throw new IllegalArgumentException("Jmx URL cannot be null");
		}

		HashMap<String, Object> map = null;
		if (userName != null || password != null) {
			map = new HashMap<String, Object>();
			String[] credentials = new String[] { userName, password };
			map.put("jmx.remote.credentials", credentials);
		}
		try {
			this.serviceUrl = new JMXServiceURL(jmxUrl);
		} catch (MalformedURLException e) {
			throw createJmException("JmxServiceUrl was malformed: " + jmxUrl, e);
		}

		try {
			jmxConnector = JMXConnectorFactory.connect(serviceUrl, map);
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
	 * Connect the client to a host and port combination, with a username and password.
	 */
	public JmxClient(String hostName, int port, String username, String password) throws JMException {
		this(generalJmxUrlForHostNamePort(hostName, port), username, password);
	}


	/**
	 * Connect the client to an address and port combination.
	 */
	public JmxClient(InetAddress address, int port) throws JMException {
		this(address.getHostAddress(), port);
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
	@Override
	public void close() {
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
	public void closeThrow() throws JMException {
		try {
			if (jmxConnector != null) {
				jmxConnector.close();
				jmxConnector = null;
			}
			// NOTE: doesn't seem to be close method on MBeanServerConnection
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
	 * Return a set of the various bean ObjectName objects associated with the Jmx server.
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
	 * Return a set of the various bean ObjectName objects associated with the Jmx server.
	 */
	public Set<ObjectName> getBeanNames(String domain) throws JMException {
		checkClientConnected();
		try {
			return mbeanConn.queryNames(ObjectName.getInstance(domain + ":*"), null);
		} catch (IOException e) {
			throw createJmException("Problems querying for jmx bean names: " + e, e);
		}
	}

	/**
	 * Return an array of the attributes associated with the bean name.
	 */
	public MBeanAttributeInfo[] getAttributesInfo(String domainName, String beanName) throws JMException {
		return getAttributesInfo(ObjectNameUtil.makeObjectName(domainName, beanName));
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
	public MBeanOperationInfo[] getOperationsInfo(String domainName, String beanName) throws JMException {
		return getOperationsInfo(ObjectNameUtil.makeObjectName(domainName, beanName));
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
	public Object getAttribute(String domain, String beanName, String attributeName) throws Exception {
		return getAttribute(ObjectNameUtil.makeObjectName(domain, beanName), attributeName);
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
	public String getAttributeString(String domain, String beanName, String attributeName) throws Exception {
		return getAttributeString(ObjectNameUtil.makeObjectName(domain, beanName), attributeName);
	}

	/**
	 * Return the value of a JMX attribute as a String or null if attribute has a null value.
	 */
	public String getAttributeString(ObjectName name, String attributeName) throws Exception {
		Object bean = getAttribute(name, attributeName);
		if (bean == null) {
			return null;
		} else {
			return ClientUtils.valueToString(bean);
		}
	}

	/**
	 * Get multiple attributes at once from the server.
	 */
	public List<Attribute> getAttributes(ObjectName name, String[] attributes) throws Exception {
		checkClientConnected();
		return mbeanConn.getAttributes(name, attributes).asList();
	}

	/**
	 * Get multiple attributes at once from the server.
	 */
	public List<Attribute> getAttributes(String domain, String beanName, String[] attributes) throws Exception {
		checkClientConnected();
		return getAttributes(ObjectNameUtil.makeObjectName(domain, beanName), attributes);
	}

	/**
	 * Set the JMX attribute to a particular value string.
	 */
	public void setAttribute(String domainName, String beanName, String attrName, String value) throws Exception {
		setAttribute(ObjectNameUtil.makeObjectName(domainName, beanName), attrName, value);
	}

	/**
	 * Set the JMX attribute to a particular value string.
	 */
	public void setAttribute(ObjectName name, String attrName, String value) throws Exception {
		MBeanAttributeInfo info = getAttrInfo(name, attrName);
		if (info == null) {
			throw new IllegalArgumentException("Cannot find attribute named '" + attrName + "'");
		} else {
			setAttribute(name, attrName, ClientUtils.stringToParam(value, info.getType()));
		}
	}

	/**
	 * Set the JMX attribute to a particular value string.
	 */
	public void setAttribute(String domainName, String beanName, String attrName, Object value) throws Exception {
		setAttribute(ObjectNameUtil.makeObjectName(domainName, beanName), attrName, value);
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
	 * Set a multiple attributes at once on the server.
	 */
	public void setAttributes(ObjectName name, List<Attribute> attributes) throws Exception {
		checkClientConnected();
		mbeanConn.setAttributes(name, new AttributeList(attributes));
	}

	/**
	 * Set a multiple attributes at once on the server.
	 */
	public void setAttributes(String domainName, String beanName, List<Attribute> attributes) throws Exception {
		setAttributes(ObjectNameUtil.makeObjectName(domainName, beanName), attributes);
	}

	/**
	 * Invoke a JMX method with a domain/object-name as an array of parameter strings.
	 * 
	 * @return The value returned by the method or null if none.
	 */
	public Object invokeOperation(String domain, String beanName, String operName, String... paramStrings)
			throws Exception {
		return invokeOperation(ObjectNameUtil.makeObjectName(domain, beanName), operName, paramStrings);
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
			for (int i = 0; i < paramStrings.length; i++) {
				paramObjs[i] = ClientUtils.stringToParam(paramStrings[i], paramTypes[i]);
			}
		}
		return invokeOperation(name, operName, paramTypes, paramObjs);
	}

	/**
	 * Invoke a JMX method as an array of parameter strings.
	 * 
	 * @return The value returned by the method as a string or null if none.
	 */
	public String invokeOperationToString(ObjectName name, String operName, String... paramStrings) throws Exception {
		return ClientUtils.valueToString(invokeOperation(name, operName, paramStrings));
	}

	/**
	 * Invoke a JMX method as an array of objects.
	 * 
	 * @return The value returned by the method or null if none.
	 */
	public Object invokeOperation(String domain, String beanName, String operName, Object... params) throws Exception {
		return invokeOperation(ObjectNameUtil.makeObjectName(domain, beanName), operName, params);
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
		MBeanOperationInfo[] operations;
		try {
			operations = mbeanConn.getMBeanInfo(objectName).getOperations();
		} catch (Exception e) {
			throw createJmException("Cannot get attribute info from " + objectName, e);
		}
		String[] paramTypes = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			paramTypes[i] = params[i].getClass().getName();
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
			if (paramTypes.length == signatureTypes.length) {
				boolean found = true;
				for (int i = 0; i < paramTypes.length; i++) {
					if (!isClassNameEquivalent(paramTypes[i], signatureTypes[i])) {
						found = false;
						break;
					}
				}
				if (found) {
					return signatureTypes;
				}
			}
			first = signatureTypes;
			nameC++;
		}

		if (first == null) {
			throw new IllegalArgumentException("Cannot find operation named '" + operName + "'");
		} else if (nameC > 1) {
			throw new IllegalArgumentException(
					"Cannot find operation named '" + operName + "' with matching argument types");
		} else {
			// return the first one we found that matches the name
			return first;
		}
	}

	private boolean isClassNameEquivalent(String className1, String className2) {
		return getWrapperClass(className1).equals(getWrapperClass(className2));
	}

	private String getWrapperClass(String className) {
		String wrapperClassName = primitiveObjectMap.get(className);
		if (wrapperClassName == null) {
			return className;
		} else {
			return wrapperClassName;
		}
	}

	private void checkClientConnected() {
		if (mbeanConn == null) {
			throw new IllegalArgumentException("JmxClient is not connected");
		}
	}

	private MBeanAttributeInfo getAttrInfo(ObjectName objectName, String attrName) throws JMException {
		MBeanAttributeInfo[] attributes;
		try {
			attributes = mbeanConn.getMBeanInfo(objectName).getAttributes();
		} catch (Exception e) {
			throw createJmException("Cannot get attribute info from " + objectName, e);
		}
		for (MBeanAttributeInfo info : attributes) {
			if (info.getName().equals(attrName)) {
				return info;
			}
		}
		return null;
	}

	private JMException createJmException(String message, Exception e) {
		JMException jmException = new JMException(message);
		jmException.initCause(e);
		return jmException;
	}
}
