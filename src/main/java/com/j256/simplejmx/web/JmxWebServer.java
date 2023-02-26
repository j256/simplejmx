package com.j256.simplejmx.web;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * Simple web-server which exposes JMX beans via HTTP. To use this class you need to provide a Jetty version 9 in your
 * dependency list or classpath.
 * 
 * @author graywatson
 */
public class JmxWebServer implements Closeable {

	private InetAddress serverAddress;
	private int serverPort;
	private Server server;

	public JmxWebServer() {
		// for spring
	}

	public JmxWebServer(InetAddress inetAddress, int serverPort) {
		this.serverAddress = inetAddress;
		this.serverPort = serverPort;
	}

	public JmxWebServer(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * Start the internal Jetty web server and configure the {@link JmxWebHandler} to handle the requests.
	 */
	public void start() throws Exception {
		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		if (serverAddress != null) {
			connector.setHost(serverAddress.getHostAddress());
		}
		connector.setPort(serverPort);
		server.addConnector(connector);
		server.setHandler(new JmxWebHandler());
		server.start();
	}

	/**
	 * Stop the internal web server and associated classes.
	 */
	public void stop() throws Exception {
		if (server != null) {
			server.stop();
			server = null;
		}
	}

	@Override
	public void close() throws IOException {
		try {
			stop();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * Optional address that the Jetty web server will be running on.
	 */
	public void setServerAddress(InetAddress serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * Required port that the Jetty web server will be running on.
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * Ignored. Used to be needed for Java 8.
	 */
	public void setMinNumThreads(int minNumThreads) {
		// ignored
	}

	/**
	 * Ignored. Used to be needed for Java 8.
	 */
	public void setMaxNumThreads(int maxNumThreads) {
		// ignored
	}
}
