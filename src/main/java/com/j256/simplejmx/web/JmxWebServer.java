package com.j256.simplejmx.web;

import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * Simple web-server which exposes JMX beans via HTTP. To use this class you need to provide a Jetty version in your
 * dependency list or classpath.
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

	/**
	 * Start the internal Jetty web server and configure the {@link JmxWebHandler} to handle the requests.
	 */
	public void start() throws Exception {
		server = new Server();

		// create the NIO connector
		connector = new SelectChannelConnector();
		connector.setPort(serverPort);
		configConnector(connector);
		server.addConnector(connector);

		server.setHandler(new JmxWebHandler());
		server.start();
	}

	/**
	 * Stop the internal Jetty web server and associated classes.
	 */
	public void stop() throws Exception {
		server.stop();
		server = null;
		connector.close();
		connector = null;
	}

	/**
	 * Required port that the Jetty web server will be running on.
	 */
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
