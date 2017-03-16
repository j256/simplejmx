package com.j256.simplejmx.web;

import java.net.InetAddress;

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
	 * 
	 * @param server
	 *            Server we are setting up the connector for.
	 * @param inetAddress
	 *            Optional address to establish the connector on. null if default.
	 * @param port
	 *            Port the server will be listening on.
	 * 
	 */
	public Connector buildConnector(Server server, InetAddress inetAddress, int port);
}
