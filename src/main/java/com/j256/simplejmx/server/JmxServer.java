package com.j256.simplejmx.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import com.j256.simplejmx.client.JmxClient;

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
	public synchronized void start() throws JMException {
		if (port == 0) {
			throw new IllegalStateException("port must be already set when JmxServer is initialized");
		}
		startRmiRegistry();
		startJmxService();
	}

	/**
	 * Stop the JMX server by closing the connector and unpublishing it from the RMI registry.
	 */
	public synchronized void stop() throws JMException {
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
	}

	/**
	 * Register the object parameter for exposure with JMX.
	 */
	public void register(Object obj) throws JMException {
		ObjectName objectName = extractJmxResourceObjName(obj);
		try {
			mbeanServer.registerMBean(new ReflectionMbean(obj), objectName);
		} catch (Exception e) {
			throw createJmException("Registering JMX object " + objectName + " failed", e);
		}
	}

	/**
	 * Un-register the object parameter from JMX. Use the {@link #unregisterThrow(Object)} if you want to see the
	 * exceptions.
	 */
	public void unregister(Object obj) {
		try {
			unregisterThrow(obj);
		} catch (Exception e) {
			// ignored
		}
	}

	/**
	 * Un-register the object parameter from JMX but this throws exceptions. Use the {@link #unregister(Object)} if you
	 * want it to be silent.
	 */
	public void unregisterThrow(Object obj) throws JMException {
		ObjectName objectName = extractJmxResourceObjName(obj);
		mbeanServer.unregisterMBean(objectName);
	}

	/**
	 * Set our port number to listen for JMX connections. This must be set either here on in the {@link #JmxServer(int)}
	 * constructor before {@link #start()} is called.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Internal method used also by {@link JmxClient} to construct an object name the same in the client and the server.
	 * 
	 * @param domain
	 *            This corresponds to the {@link JmxResource#domainName()} and is the top level folder name for the
	 *            beans.
	 * @param name
	 *            This corresponds to the {@link JmxResource#objectName()} and is the bean name in the lowest folder
	 *            level.
	 * @param fieldValues
	 *            These can be used to setup folders inside of the top folder. Each of the entries in the array are in
	 *            "name=value" format. It is recommended that they are in alphabetic order. I often use ("00",
	 *            "FolderName"). As far as I can tell, the name is ignored by jconsole.
	 */
	public static ObjectName makeObjectName(String domain, String name, JmxNamingFieldValue[] fieldValues) {
		return makeObjectName(domain, name, fieldValues, null);
	}

	/**
	 * Start the RMI registry.
	 */
	private void startRmiRegistry() throws JMException {
		if (rmiRegistry == null) {
			try {
				rmiRegistry = LocateRegistry.createRegistry(port);
			} catch (IOException e) {
				throw createJmException("Unable to create RMI registry on port " + port, e);
			}
		}
	}

	/**
	 * Start our JMX service.
	 */
	private void startJmxService() throws JMException {
		if (connector == null) {
			JMXServiceURL url = null;
			String urlString = "service:jmx:rmi://localhost:" + (port + 1) + "/jndi/rmi://:" + port + "/jmxrmi";
			try {
				url = new JMXServiceURL(urlString);
			} catch (MalformedURLException e) {
				throw createJmException("Malformed service url created " + urlString, e);
			}
			try {
				connector =
						JMXConnectorServerFactory.newJMXConnectorServer(url, new HashMap<String, Object>(),
								ManagementFactory.getPlatformMBeanServer());
			} catch (IOException e) {
				throw createJmException("Could not make our Jmx connector server", e);
			}
			try {
				connector.start();
			} catch (IOException e) {
				connector = null;
				throw createJmException("Could not start our Jmx connector server", e);
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
			return makeObjectName(jmxResource.domainName(), ((JmxSelfNaming) obj).getJmxObjectName(),
					((JmxSelfNaming) obj).getJmxFieldValues(), null);
		} else {
			String objectName = jmxResource.objectName();
			if (objectName == null || objectName.length() == 0) {
				objectName = obj.getClass().getSimpleName();
			}
			return makeObjectName(jmxResource.domainName(), jmxResource.objectName(), null, jmxResource.fieldValues());
		}
	}

	private JMException createJmException(String message, Exception e) {
		JMException jmException = new JMException(message);
		jmException.initCause(e);
		return jmException;
	}

	private static ObjectName makeObjectName(String domain, String name, JmxNamingFieldValue[] fieldValues,
			String[] fieldValueStrings) {
		// j256.backupd:00=clients,name=
		StringBuilder sb = new StringBuilder();
		sb.append(domain);
		sb.append(':');
		boolean first = true;
		if (fieldValues != null) {
			for (JmxNamingFieldValue fieldValue : fieldValues) {
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				sb.append(fieldValue.getField());
				sb.append('=');
				sb.append(fieldValue.getValue());
			}
		}
		if (fieldValueStrings != null) {
			for (String fieldValue : fieldValueStrings) {
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
			throw new IllegalArgumentException("Invalid ObjectName generated: " + sb.toString(), e);
		}
	}
}
