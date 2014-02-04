package com.j256.simplejmx.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import com.j256.simplejmx.common.JmxAttributeFieldInfo;
import com.j256.simplejmx.common.JmxAttributeMethodInfo;
import com.j256.simplejmx.common.JmxOperationInfo;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxResourceInfo;
import com.j256.simplejmx.common.JmxSelfNaming;
import com.j256.simplejmx.common.ObjectNameUtil;

/**
 * JMX server which allows classes to publish and un-publish themselves as JMX beans.
 * 
 * @author graywatson
 */
public class JmxServer {

	private final String RMI_SERVER_HOST_NAME_PROPERTY = "java.rmi.server.hostname";

	private Registry rmiRegistry;
	private InetAddress inetAddress;
	private int serverPort;
	private int registryPort;
	private JMXConnectorServer connector;
	private MBeanServer mbeanServer;
	private int registeredCount;
	private RMIServerSocketFactory serverSocketFactory;
	private boolean serverHostNamePropertySet = false;

	/**
	 * Create a JMX server that will be set with the port using setters. Used with spring. You must at least specify the
	 * port number with {@link #setPort(int)}.
	 */
	public JmxServer() {
		// for spring
	}

	/**
	 * Create a JMX server running on a particular registry-port.
	 * 
	 * @param registryPort
	 *            The "RMI registry port" that you specify in jconsole to connect to the server. See
	 *            {@link #setRegistryPort(int)}.
	 */
	public JmxServer(int registryPort) {
		this.registryPort = registryPort;
	}

	/**
	 * Create a JMX server running on a particular address and registry-port.
	 * 
	 * @param inetAddress
	 *            Address to bind to.  If you use on the non-address constructors, it will bind to all interfaces.
	 * @param registryPort
	 *            The "RMI registry port" that you specify in jconsole to connect to the server. See
	 *            {@link #setRegistryPort(int)}.
	 */
	public JmxServer(InetAddress inetAddress, int registryPort) {
		this.inetAddress = inetAddress;
		this.registryPort = registryPort;
	}

	/**
	 * Create a JMX server running on a particular registry and server port pair.
	 * 
	 * @param registryPort
	 *            The "RMI registry port" that you specify in jconsole to connect to the server. See
	 *            {@link #setRegistryPort(int)}.
	 * @param serverPort
	 *            The RMI server port that jconsole uses to transfer data to/from the server. See
	 *            {@link #setServerPort(int)}. The same port as the registry-port can be used.
	 */
	public JmxServer(int registryPort, int serverPort) {
		this.registryPort = registryPort;
		this.serverPort = serverPort;
	}

	/**
	 * Create a JMX server running on a particular registry and server port pair.
	 * 
	 * @param inetAddress
	 *            Address to bind to.  If you use on the non-address constructors, it will bind to all interfaces.
	 * @param registryPort
	 *            The "RMI registry port" that you specify in jconsole to connect to the server. See
	 *            {@link #setRegistryPort(int)}.
	 * @param serverPort
	 *            The RMI server port that jconsole uses to transfer data to/from the server. See
	 *            {@link #setServerPort(int)}. The same port as the registry-port can be used.
	 */
	public JmxServer(InetAddress inetAddress, int registryPort, int serverPort) {
		this.inetAddress = inetAddress;
		this.registryPort = registryPort;
		this.serverPort = serverPort;
	}

	/**
	 * Create a JmxServer wrapper around an existing MBeanServer. You may want to use this with
	 * {@link ManagementFactory#getPlatformMBeanServer()} to use the JVM platform's default server.
	 */
	public JmxServer(MBeanServer mbeanServer) {
		this.mbeanServer = mbeanServer;
	}

	/**
	 * Start our JMX service. The port must have already been called either in the {@link #JmxServer(int)} constructor
	 * or the {@link #setRegistryPort(int)} method before this is called.
	 * 
	 * @throws IllegalStateException
	 *             If the registry port has not already been set.
	 */
	public synchronized void start() throws JMException {
		if (mbeanServer != null) {
			// no-op
			return;
		}
		if (registryPort == 0) {
			throw new IllegalStateException("registry-port must be already set when JmxServer is initialized");
		}
		startRmiRegistry();
		startJmxService();
	}

	/**
	 * Same as {@link #stopThrow()} but this ignores any exceptions.
	 */
	public synchronized void stop() {
		try {
			stopThrow();
		} catch (JMException e) {
			// ignored
		}
	}

	/**
	 * Stop the JMX server by closing the connector and unpublishing it from the RMI registry. This throws a JMException
	 * on any issues.
	 */
	public synchronized void stopThrow() throws JMException {
		if (connector != null) {
			try {
				connector.stop();
			} catch (IOException e) {
				throw createJmException("Could not stop our Jmx connector server", e);
			} finally {
				connector = null;
			}
		}
		if (rmiRegistry != null) {
			try {
				UnicastRemoteObject.unexportObject(rmiRegistry, true);
			} catch (NoSuchObjectException e) {
				throw createJmException("Could not unexport our RMI registry", e);
			} finally {
				rmiRegistry = null;
			}
		}
		if (serverHostNamePropertySet) {
			System.clearProperty(RMI_SERVER_HOST_NAME_PROPERTY);
			serverHostNamePropertySet = false;
		}
	}

	/**
	 * Register the object parameter for exposure with JMX. The object passed in must have a {@link JmxResource}
	 * annotation or must implement {@link JmxSelfNaming}.
	 */
	public synchronized void register(Object obj) throws JMException {
		ObjectName objectName = ObjectNameUtil.makeObjectName(obj);
		ReflectionMbean mbean;
		try {
			mbean = new ReflectionMbean(obj, getObjectDescription(obj));
		} catch (Exception e) {
			throw createJmException("Could not build MBean object for: " + obj, e);
		}
		doRegister(objectName, mbean);
	}

	/**
	 * Register the object parameter for exposure with JMX with user defined field-attribute, method-attribute, and
	 * operation information.
	 * 
	 * @param obj
	 *            Object that we are registering.
	 * @param objectName
	 *            Name of the object most likely generated by one of the methods from the {@link ObjectNameUtil} class.
	 * @param attributeFieldInfos
	 *            Array of attribute information for fields that are exposed through reflection. Can be null if none.
	 * @param attributeMethodInfos
	 *            Array of attribute information for fields that are exposed through get/set/is methods.
	 * @param operationInfos
	 *            Array of operation information for methods.
	 */
	public synchronized void register(Object obj, ObjectName objectName, JmxAttributeFieldInfo[] attributeFieldInfos,
			JmxAttributeMethodInfo[] attributeMethodInfos, JmxOperationInfo[] operationInfos) throws JMException {
		String description = getObjectDescription(obj);
		register(obj, objectName, description, attributeFieldInfos, attributeMethodInfos, operationInfos);
	}

	/**
	 * Register the object parameter for exposure with JMX with user defined field-attribute, method-attribute, and
	 * operation information.
	 * 
	 * @param obj
	 *            Object that we are registering.
	 * @param objectName
	 *            Name of the object most likely generated by one of the methods from the {@link ObjectNameUtil} class.
	 * @param description
	 *            Optional description of the object for jconsole debug output. Default is "Information about class".
	 * @param attributeFieldInfos
	 *            Array of attribute information for fields that are exposed through reflection. Can be null if none.
	 * @param attributeMethodInfos
	 *            Array of attribute information for fields that are exposed through get/set/is methods.
	 * @param operationInfos
	 *            Array of operation information for methods.
	 */
	public synchronized void register(Object obj, ObjectName objectName, String description,
			JmxAttributeFieldInfo[] attributeFieldInfos, JmxAttributeMethodInfo[] attributeMethodInfos,
			JmxOperationInfo[] operationInfos) throws JMException {
		ReflectionMbean mbean;
		try {
			mbean = new ReflectionMbean(obj, description, attributeFieldInfos, attributeMethodInfos, operationInfos);
		} catch (Exception e) {
			throw createJmException("Could not build MBean object for: " + obj, e);
		}
		doRegister(objectName, mbean);
	}

	/**
	 * Register the object parameter for exposure with JMX with user defined field-attribute, method-attribute, and
	 * operation information.
	 * 
	 * @param obj
	 *            Object that we are registering.
	 * @param resourceInfo
	 *            Resource information used to build the object-name.
	 * @param attributeFieldInfos
	 *            Array of attribute information for fields that are exposed through reflection. Can be null if none.
	 * @param attributeMethodInfos
	 *            Array of attribute information for fields that are exposed through get/set/is methods.
	 * @param operationInfos
	 *            Array of operation information for methods.
	 */
	public synchronized void register(Object obj, JmxResourceInfo resourceInfo,
			JmxAttributeFieldInfo[] attributeFieldInfos, JmxAttributeMethodInfo[] attributeMethodInfos,
			JmxOperationInfo[] operationInfos) throws JMException {
		ObjectName objectName = ObjectNameUtil.makeObjectName(resourceInfo);
		ReflectionMbean mbean;
		try {
			mbean =
					new ReflectionMbean(obj, resourceInfo.getJmxDescription(), attributeFieldInfos,
							attributeMethodInfos, operationInfos);
		} catch (Exception e) {
			throw createJmException("Could not build MBean object for: " + obj, e);
		}
		doRegister(objectName, mbean);
	}

	/**
	 * Same as {@link #unregisterThrow(Object)} except this ignores exceptions.
	 */
	public void unregister(Object obj) {
		try {
			unregisterThrow(obj);
		} catch (Exception e) {
			// ignored
		}
	}

	/**
	 * Same as {@link #unregisterThrow(ObjectName)} except this ignores exceptions.
	 */
	public void unregister(ObjectName objName) {
		try {
			unregisterThrow(objName);
		} catch (Exception e) {
			// ignored
		}
	}

	/**
	 * Un-register the object parameter from JMX but this throws exceptions. Use the {@link #unregister(Object)} if you
	 * want it to be silent.
	 */
	public synchronized void unregisterThrow(Object obj) throws JMException {
		ObjectName objectName = ObjectNameUtil.makeObjectName(obj);
		mbeanServer.unregisterMBean(objectName);
		registeredCount--;
	}

	/**
	 * Un-register the object name from JMX but this throws exceptions. Use the {@link #unregister(Object)} if you want
	 * it to be silent.
	 */
	public synchronized void unregisterThrow(ObjectName objName) throws JMException {
		mbeanServer.unregisterMBean(objName);
	}

	/**
	 * Not required. Default is to bind to local interfaces.
	 */
	public void setInetAddress(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
	}

	/**
	 * This is actually calls {@link #setRegistryPort(int)}.
	 */
	public void setPort(int port) {
		setRegistryPort(port);
	}

	/**
	 * @see JmxServer#setRegistryPort(int)
	 */
	public int getRegistryPort() {
		return registryPort;
	}

	/**
	 * Set our port number to listen for JMX connections. This is the "RMI registry port" but it is the port that you
	 * specify in jconsole to connect to the server. This must be set either here or in the {@link #JmxServer(int)}
	 * constructor before {@link #start()} is called.
	 */
	public void setRegistryPort(int registryPort) {
		this.registryPort = registryPort;
	}

	/**
	 * @see JmxServer#setServerPort(int)
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * Chances are you should be using {@link #setPort(int)} or {@link #setRegistryPort(int)} unless you know what you
	 * are doing. This sets what JMX calls the "RMI server port". By default this does not have to be set and the
	 * registry-port will be used. Both the registry and the server can be the same port. When you specify a port number
	 * in jconsole this is not the port that should be specified -- see the registry port.
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * Optional server socket factory that can will be used to generate our registry and server ports. Unfortunately if
	 * this is specified then the registry and server ports have to be different.
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * Number of registered objects.
	 */
	public int getRegisteredCount() {
		return registeredCount;
	}

	private String getObjectDescription(Object obj) {
		Class<? extends Object> clazz = obj.getClass();
		JmxResource jmxResource = clazz.getAnnotation(JmxResource.class);
		if (jmxResource == null || jmxResource.description() == null || jmxResource.description().length() == 0) {
			return null;
		} else {
			return jmxResource.description();
		}
	}

	private void doRegister(ObjectName objectName, ReflectionMbean mbean) throws JMException {
		try {
			mbeanServer.registerMBean(mbean, objectName);
			registeredCount++;
		} catch (Exception e) {
			throw createJmException("Registering JMX object " + objectName + " failed", e);
		}
	}

	private void startRmiRegistry() throws JMException {
		if (rmiRegistry == null) {
			try {
				if (inetAddress == null) {
					rmiRegistry = LocateRegistry.createRegistry(registryPort);
				} else {
					if (serverSocketFactory == null) {
						serverSocketFactory = new LocalSocketFactory(inetAddress);
					}
					if (System.getProperty(RMI_SERVER_HOST_NAME_PROPERTY) == null) {
						/*
						 * We have to do this because JMX tries to connect back the server that we just set and it won't
						 * be able to locate it if we set our own address to anything but the InetAddress.getLocalHost()
						 * address.
						 */
						System.setProperty(RMI_SERVER_HOST_NAME_PROPERTY, inetAddress.getHostAddress());
						serverHostNamePropertySet = true;
					}
					rmiRegistry =
							LocateRegistry.createRegistry(registryPort, RMISocketFactory.getDefaultSocketFactory(),
									serverSocketFactory);
				}
			} catch (IOException e) {
				throw createJmException("Unable to create RMI registry on port " + registryPort, e);
			}
		}
	}

	private void startJmxService() throws JMException {
		if (connector != null) {
			return;
		}
		JMXServiceURL url = null;
		if (serverPort == 0) {
			if (inetAddress == null) {
				/*
				 * If we aren't specifying an address then we can use the registry-port for both the registry call _and_
				 * the RMI calls seems to work fine. There must be RMI multiplexing underneath the covers of the JMX
				 * handler. Did not know that. Thanks to EJB@SO.
				 */
				serverPort = registryPort;
			} else {
				/*
				 * Unfortunately, it doesn't seem like we can use the same port if we are using a socket factory. When
				 * this is debugged it calls createServerSocket(...) two times instead of sharing the same socket
				 * without the socket-factory.
				 */
				serverPort = registryPort + 1;
			}
		}
		String serverHost = "localhost";
		String registryHost = "";
		if (inetAddress != null) {
			String hostAddr = inetAddress.getHostAddress();
			serverHost = hostAddr;
			registryHost = hostAddr;
		}
		String urlString =
				"service:jmx:rmi://" + serverHost + ":" + serverPort + "/jndi/rmi://" + registryHost + ":"
						+ registryPort + "/jmxrmi";
		try {
			url = new JMXServiceURL(urlString);
		} catch (MalformedURLException e) {
			throw createJmException("Malformed service url created " + urlString, e);
		}

		Map<String, Object> envMap = null;
		if (serverSocketFactory != null) {
			envMap = new HashMap<String, Object>();
			envMap.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, serverSocketFactory);
		}

		try {
			connector =
					JMXConnectorServerFactory.newJMXConnectorServer(url, envMap,
							ManagementFactory.getPlatformMBeanServer());
		} catch (IOException e) {
			throw createJmException("Could not make our Jmx connector server on URL: " + url, e);
		}
		try {
			connector.start();
		} catch (IOException e) {
			connector = null;
			throw createJmException("Could not start our Jmx connector server on URL: " + url, e);
		}
		mbeanServer = connector.getMBeanServer();
	}

	private JMException createJmException(String message, Exception e) {
		JMException jmException = new JMException(message);
		jmException.initCause(e);
		return jmException;
	}

	/**
	 * Socket factory which allows us to set a particular local address.
	 */
	private static class LocalSocketFactory implements RMIServerSocketFactory {

		private final InetAddress inetAddress;

		public LocalSocketFactory(InetAddress inetAddress) {
			this.inetAddress = inetAddress;
		}

		public ServerSocket createServerSocket(int port) throws IOException {
			return new ServerSocket(port, 0, inetAddress);
		}

		@Override
		public int hashCode() {
			return (this.inetAddress == null ? 0 : this.inetAddress.hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			LocalSocketFactory other = (LocalSocketFactory) obj;
			if (this.inetAddress == null) {
				return (other.inetAddress == null);
			} else {
				return this.inetAddress.equals(other.inetAddress);
			}
		}
	}
}
