package com.j256.simplejmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 * JMX server connection which sets up a server (with or without JVM parameters) and allows classes to easily publish
 * themselves via RMI.
 * 
 * @author graywatson
 */
public class JmxServer {

	private Registry rmiRegistry;
	private int port;
	private JMXConnectorServer connector;
	private MBeanServer mbeanServer;

	public JmxServer() {
		// for spring
	}

	public JmxServer(int port) {
		this.port = port;
	}

	/**
	 * Start our JMX service. The port must have already been called either here on in the {@link #JmxServer(int)}
	 * constructor before {@link #start()} is called.
	 */
	public synchronized void start() throws Exception {
		if (port == 0) {
			throw new IllegalStateException("port must be already set when JmxServer is initialized");
		}
		startRmiRegistry();
		startJmxService();
	}

	/**
	 * Close the JMX server.
	 */
	public synchronized void close() throws Exception {
		if (connector != null) {
			try {
				connector.stop();
			} catch (IOException e) {
				throw new Exception("Could not stop our Jmx connector server", e);
			} finally {
				connector = null;
			}
		}
		if (rmiRegistry != null) {
			try {
				UnicastRemoteObject.unexportObject(rmiRegistry, true);
			} catch (NoSuchObjectException e) {
				throw new Exception("Could not unexport our RMI registry", e);
			} finally {
				rmiRegistry = null;
			}
		}
	}

	/**
	 * Register the object parameter for exposure with JMX.
	 */
	public void register(Object obj) {
		ObjectName objectName = extractJmxResourceObjName(obj);
		if (objectName == null) {
			// skip it if it is null
			return;
		}
		try {
			mbeanServer.registerMBean(new ReflectionMbean(obj), objectName);
		} catch (Exception e) {
			throw new IllegalArgumentException("Registering JMX object failed", e);
		}
	}

	/**
	 * Un-register the object parameter from JMX.
	 */
	public void unregister(Object obj) {
		ObjectName objectName = extractJmxResourceObjName(obj);
		try {
			mbeanServer.unregisterMBean(objectName);
		} catch (Exception e) {
			// ignored
		}
	}

	/**
	 * Set our port number to listen for JMX connections. This must be set either here on in the {@link #JmxServer(int)}
	 * constructor before {@link #start()} is called.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Start the RMI registry.
	 */
	private void startRmiRegistry() throws Exception {
		if (rmiRegistry == null) {
			try {
				rmiRegistry = LocateRegistry.createRegistry(port);
			} catch (IOException e) {
				throw new Exception("Unable to create RMI registry on port " + port, e);
			}
		}
	}

	/**
	 * Start our JMX service.
	 */
	private void startJmxService() throws Exception {
		if (connector == null) {
			JMXServiceURL url = null;
			String urlString = "service:jmx:rmi://localhost:" + (port + 1) + "/jndi/rmi://:" + port + "/jmxrmi";
			try {
				url = new JMXServiceURL(urlString);
			} catch (MalformedURLException e) {
				throw new Exception("Malformed service url created " + urlString, e);
			}
			try {
				connector =
						JMXConnectorServerFactory.newJMXConnectorServer(url, new HashMap<String, Object>(),
								ManagementFactory.getPlatformMBeanServer());
			} catch (IOException e) {
				throw new Exception("Could not make our Jmx connector server", e);
			}
			try {
				connector.start();
			} catch (IOException e) {
				connector = null;
				throw new Exception("Could not start our Jmx connector server", e);
			}
			mbeanServer = connector.getMBeanServer();
		}
	}

	private ObjectName extractJmxResourceObjName(Object obj) {
		JmxResource jmxResource = obj.getClass().getAnnotation(JmxResource.class);
		if (jmxResource == null) {
			throw new IllegalArgumentException(
					"Registered class must implement JmxSelfNaming or have JmxResource annotation");
		}
		if (obj instanceof JmxSelfNaming) {
			return extractJmxSelfNamingObjName(jmxResource.domainName(), (JmxSelfNaming) obj);
		} else {
			return makeObjectName(obj, jmxResource.domainName(), jmxResource.objectName(), jmxResource.fieldValues());
		}
	}

	private ObjectName extractJmxSelfNamingObjName(String domain, JmxSelfNaming obj) {
		return makeObjectName(obj, domain, obj.getJmxObjectName(), obj.getJmxFieldValues());
	}

	private ObjectName makeObjectName(Object obj, String domain, String name, String[] fieldValues) {
		// j256.backupd:00=clients,name=
		StringBuilder sb = new StringBuilder();
		sb.append(domain);
		sb.append(':');
		boolean first = true;
		if (fieldValues != null) {
			for (String fieldValue : fieldValues) {
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				sb.append(fieldValue);
			}
		}
		if (!first) {
			sb.append(',');
		}
		sb.append("name=");
		sb.append(name);
		try {
			return new ObjectName(sb.toString());
		} catch (Exception e) {
			throw new IllegalArgumentException("Object " + obj + " generated an invalid name: " + sb.toString(), e);
		}
	}
}
