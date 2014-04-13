package com.j256.simplejmx.web;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

/**
 * Server factory interface to hide the details of Jetty 8 versus 9.
 * 
 * @author graywatson
 */
public interface JettyConnectorFactory {

	/**
	 * Build and configure a connector for our web server.
	 */
	public Connector buildConnector(Server server, int serverPort);
}
