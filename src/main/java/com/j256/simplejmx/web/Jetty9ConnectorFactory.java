package com.j256.simplejmx.web;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * Configuration of our Jetty {@link Connector} if using Jetty version 9.
 * 
 * @author graywatson
 */
public class Jetty9ConnectorFactory implements JettyConnectorFactory {

	public Connector buildConnector(Server server, int serverPort) {
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(serverPort);
		return connector;
	}
}
