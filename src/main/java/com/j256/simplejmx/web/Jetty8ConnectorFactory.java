package com.j256.simplejmx.web;

import java.net.InetAddress;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * Configuration of our Jetty {@link Connector} if using Jetty version 8.
 * 
 * @author graywatson
 */
public class Jetty8ConnectorFactory implements JettyConnectorFactory {

	private static final int WEB_SERVER_MIN_THREADS = 1;
	private static final int WEB_SERVER_MAX_THREADS = 5;

	@Override
	public Connector buildConnector(Server server, InetAddress inetAddress, int port) {
		// create the NIO connector
		SelectChannelConnector connector = new SelectChannelConnector();
		if (inetAddress != null) {
			connector.setHost(inetAddress.getHostAddress());
		}
		connector.setPort(port);

		// turn on statistics
		connector.setStatsOn(true);
		// set whether or not to reuse the addresses
		connector.setReuseAddress(true);

		// configure the thread pool for accepting connections
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMinThreads(WEB_SERVER_MIN_THREADS);
		threadPool.setMaxThreads(WEB_SERVER_MAX_THREADS);
		threadPool.setName("simplejmx-web-server");
		connector.setThreadPool(threadPool);

		return connector;
	}
}
