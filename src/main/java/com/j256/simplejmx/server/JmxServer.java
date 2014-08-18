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
	private String serviceUrl;

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
	 *            Address to bind to. If you use on the non-address constructors, it will bind to all interfaces.
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
	 *            Address to bind to. If you use on the non-address constructors, it will bind to all interfaces.
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
	 * Create a JmxServer that uses an existing MBeanServer. You may want to use this with
	 * {@link ManagementFactory#getPlatformMBeanServer()} to use the JVM platform's default server.
	 * 
	 * <p>
	 * <b>NOTE:</b> You can also use the {@link #JmxServer(boolean)} or {@link #setUsePlatformMBeanServer(boolean)} with
	 * true to set the internal MBeanServer to the platform one already defined.
	 * </p>
	 */
	public JmxServer(MBeanServer mbeanServer) {
		this.mbeanServer = mbeanServer;
	}

	/**
	 * If you pass in true, this will create a JmxServer that uses the existing JVM platform's MBeanServer. This calls
	 * through to the {@link ManagementFactory#getPlatformMBeanServer()} which will create one if it doesn't already
	 * exist.
	 */
	public JmxServer(boolean usePlatformMBeanServer) {
		if (usePlatformMBeanServer) {
			this.mbeanServer = ManagementFactory.getPlatformMBeanServer();
		}
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
			// if we've already assigned a mbean-server then there's nothing to start
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
	public synchronized ObjectName register(Object obj) throws JMException {
		if (mbeanServer == null) {
			throw new JMException("JmxServer has not be started");
		}
		ObjectName objectName = ObjectNameUtil.makeObjectName(obj);
		ReflectionMbean mbean;
		try {
			mbean = new ReflectionMbean(obj, getObjectDescription(obj));
		} catch (Exception e) {
			throw createJmException("Could not build MBean object for: " + obj, e);
		}
		doRegister(objectName, mbean);
		return objectName;
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
	 * Register the object parameter for exposure with JMX that is wrapped using the PublishAllBeanWrapper.
	 */
	public synchronized ObjectName register(PublishAllBeanWrapper wrapper) throws JMException {
		ReflectionMbean mbean;
		try {
			mbean = new ReflectionMbean(wrapper);
		} catch (Exception e) {
			throw createJmException("Could not build mbean object for publish-all bean: " + wrapper.getTarget(), e);
		}
		ObjectName objectName = ObjectNameUtil.makeObjectName(wrapper.getJmxResourceInfo());
		doRegister(objectName, mbean);
		return objectName;
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
	 * @return Resulting object name.
	 */
	public synchronized ObjectName register(Object obj, JmxResourceInfo resourceInfo,
			JmxAttributeFieldInfo[] attributeFieldInfos, JmxAttributeMethodInfo[] attributeMethodInfos,
			JmxOperationInfo[] operationInfos) throws JMException {
		ObjectName objectName = ObjectNameUtil.makeObjectName(resourceInfo);
		register(obj, objectName, resourceInfo.getJmxDescription(), attributeFieldInfos, attributeMethodInfos,
				operationInfos);
		return objectName;
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
		if (mbeanServer == null) {
			throw new JMException("JmxServer has not be started");
		}
		ReflectionMbean mbean;
		try {
			mbean =
					new ReflectionMbean(obj, description, attributeFieldInfos, attributeMethodInfos, operationInfos,
							false);
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
		if (obj instanceof PublishAllBeanWrapper) {
			unregisterThrow(ObjectNameUtil.makeObjectName(((PublishAllBeanWrapper)obj).getJmxResourceInfo()));
		} else {
			unregisterThrow(ObjectNameUtil.makeObjectName(obj));
		}
	}

	/**
	 * Un-register the object name from JMX but this throws exceptions. Use the {@link #unregister(Object)} if you want
	 * it to be silent.
	 */
	public synchronized void unregisterThrow(ObjectName objName) throws JMException {
		if (mbeanServer == null) {
			throw new JMException("JmxServer has not be started");
		}
		mbeanServer.unregisterMBean(objName);
		registeredCount--;
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
	 * registry-port will be used also as the server-port. Both the registry and the server can be the same port. When
	 * you specify a port number in jconsole this is not the port that should be specified -- see the registry port.
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * Optional server socket factory that can will be used to generate our registry and server ports. This is not
	 * necessary if you are specifying addresses or ports.
	 */
	public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * Optional service URL which is used to specify the connection endpoints. You should not use this if you are
	 * setting the address or the ports directly. The format is something like:
	 * 
	 * <p>
	 * 
	 * <pre>
	 * service:jmx:rmi://your-server-name:server-port/jndi/rmi://registry-host:registry-port/jmxrmi
	 * </pre>
	 * 
	 * </p>
	 * 
	 * <p>
	 * <tt>your-server-name</tt> could be an IP of an interface or just localhost. <tt>registry-host</tt> can also be an
	 * interface IP or blank for localhost.
	 * </p>
	 */
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * Set this to true (default is false) to have the JmxServer use the MBean server defined by the JVM as opposed to
	 * making one itself.
	 */
	public void setUsePlatformMBeanServer(boolean usePlatformMBeanServer) {
		if (usePlatformMBeanServer) {
			mbeanServer = ManagementFactory.getPlatformMBeanServer();
		}
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
		if (rmiRegistry != null) {
			return;
		}
		try {
			if (inetAddress == null) {
				rmiRegistry = LocateRegistry.createRegistry(registryPort);
			} else {
				if (serverSocketFactory == null) {
					serverSocketFactory = new LocalSocketFactory(inetAddress);
				}
				if (System.getProperty(RMI_SERVER_HOST_NAME_PROPERTY) == null) {
					/*
					 * We have to do this because JMX tries to connect back the server that we just set and it won't be
					 * able to locate it if we set our own address to anything but the InetAddress.getLocalHost()
					 * address.
					 */
					System.setProperty(RMI_SERVER_HOST_NAME_PROPERTY, inetAddress.getHostAddress());
					serverHostNamePropertySet = true;
				}
				/*
				 * NOTE: the client factory being null is a critical part of this for some reason. If we specify a
				 * client socket factory then the registry and the RMI server can't be on the same port. Thanks to EJB.
				 * 
				 * I also tried to inject a client socket factory both here and below in the connector environment but I
				 * could not get it to work.
				 */
				rmiRegistry = LocateRegistry.createRegistry(registryPort, null, serverSocketFactory);
			}
		} catch (IOException e) {
			throw createJmException("Unable to create RMI registry on port " + registryPort, e);
		}
	}

	private void startJmxService() throws JMException {
		if (connector != null) {
			return;
		}
		if (serverPort == 0) {
			/*
			 * If we aren't specifying an address then we can use the registry-port for both the registry call _and_ the
			 * RMI calls. There is RMI port multiplexing underneath the covers of the JMX handler. Did not know that.
			 * Thanks to EJB.
			 */
			serverPort = registryPort;
		}
		String serverHost = "localhost";
		String registryHost = "";
		if (inetAddress != null) {
			String hostAddr = inetAddress.getHostAddress();
			serverHost = hostAddr;
			registryHost = hostAddr;
		}
		if (serviceUrl == null) {
			serviceUrl =
					"service:jmx:rmi://" + serverHost + ":" + serverPort + "/jndi/rmi://" + registryHost + ":"
							+ registryPort + "/jmxrmi";
		}
		JMXServiceURL url;
		try {
			url = new JMXServiceURL(serviceUrl);
		} catch (MalformedURLException e) {
			throw createJmException("Malformed service url created " + serviceUrl, e);
		}

		Map<String, Object> envMap = null;
		if (serverSocketFactory != null) {
			envMap = new HashMap<String, Object>();
			envMap.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, serverSocketFactory);
		}
		/*
		 * NOTE: I tried to inject a client socket factory with RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE
		 * but I could not get it to work. It seemed to require the client to have the LocalSocketFactory class in the
		 * classpath.
		 */

		try {
			mbeanServer = ManagementFactory.getPlatformMBeanServer();
			connector = JMXConnectorServerFactory.newJMXConnectorServer(url, envMap, mbeanServer);
		} catch (IOException e) {
			throw createJmException("Could not make our Jmx connector server on URL: " + url, e);
		}
		try {
			connector.start();
		} catch (IOException e) {
			connector = null;
			throw createJmException("Could not start our Jmx connector server on URL: " + url, e);
		}
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
