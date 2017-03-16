package com.j256.simplejmx.web;

import java.lang.reflect.Constructor;
import java.net.InetAddress;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

/**
 * Configuration of our Jetty {@link Connector} if using Jetty version 9. We have to use reflection here because Jetty 9
 * requires Java 7 which I'm not ready to require.
 * 
 * @author graywatson
 */
public class Jetty9ConnectorFactory implements JettyConnectorFactory {

	@Override
	public Connector buildConnector(Server server, InetAddress inetAddress, int port) {
		try {
			Class<?> clazz = Class.forName("org.eclipse.jetty.server.ServerConnector");
			Constructor<?> constructor = clazz.getConstructor(Server.class);
			Connector connector = (Connector) constructor.newInstance(server);
			if (inetAddress != null) {
				connector.setHost(inetAddress.getHostName());
			}
			connector.setPort(port);
			return connector;
		} catch (Exception e) {
			throw new RuntimeException("could not create ServerConnector with reflection", e);
		}
	}
}
