package com.j256.simplejmx.web;

import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * Simple web-server which exposes JMX beans via HTTP.
 * 
 * @author graywatson
 */
public class JmxWebServer {

	private static final int WEB_SERVER_MIN_THREADS = 1;
	private static final int WEB_SERVER_MAX_THREADS = 5;

	private int serverPort;
	private Server server;
	private SelectChannelConnector connector;

	public JmxWebServer() {
		// for spring
	}

	public JmxWebServer(int serverPort) {
		this.serverPort = serverPort;
	}

	public void start() throws Exception {
		server = new Server();

		// create the NIO connector
		connector = new SelectChannelConnector();
		connector.setPort(serverPort);
		configConnector(connector);
		server.addConnector(connector);

		server.setHandler(new JmxHandler());
		server.start();
	}

	public void stop() throws Exception {
		server.stop();
		connector.close();
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	private void configConnector(AbstractConnector conector) {
		// turn on collection of statistics by Jetty
		connector.setStatsOn(true);
		// configure the thread pool for accepting connections
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMinThreads(WEB_SERVER_MIN_THREADS);
		threadPool.setMaxThreads(WEB_SERVER_MAX_THREADS);
		threadPool.setName("web-server");
		connector.setThreadPool(threadPool);
		// set whether or not to reuse the addresses
		connector.setReuseAddress(true);
	}
}
