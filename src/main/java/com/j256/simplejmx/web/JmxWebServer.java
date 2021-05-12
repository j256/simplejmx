package com.j256.simplejmx.web;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * Simple web-server which exposes JMX beans via HTTP. To use this class you need to provide a Jetty version in your
 * dependency list or classpath.
 * 
 * <p>
 * <b>NOTE:</b> This class tries to support both Jetty version 8 and 9. If the
 * {@code org.eclipse.jetty.server.nio.SelectChannelConnector} class is available it will assume you are using version 8
 * otherwise version 9.
 * </p>
 * 
 * @author graywatson
 */
public class JmxWebServer implements Closeable {

	private static final int DEFAULT_MIN_NUM_THREADS = 0;
	private static final int DEFAULT_MAX_NUM_THREADS = 3;

	private InetAddress serverAddress;
	private int serverPort;
	private int minNumThreads = DEFAULT_MIN_NUM_THREADS;
	private int maxNumThreads = DEFAULT_MAX_NUM_THREADS;
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
		server.setThreadPool(new OurThreadPool(minNumThreads, maxNumThreads));
		SocketConnector connector = new SocketConnector();
		connector.setServer(server);
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
	 * Not requested minimum number of threads. Default is 1.
	 */
	public void setMinNumThreads(int minNumThreads) {
		this.minNumThreads = minNumThreads;
	}

	/**
	 * Not requested maximum number of threads. Default is 1.
	 */
	public void setMaxNumThreads(int maxNumThreads) {
		this.maxNumThreads = maxNumThreads;
	}

	/**
	 * Thread-pool for jetty because they can't roll their own for some reason.
	 */
	private class OurThreadPool implements ThreadPool, ThreadFactory {

		private final ThreadPoolExecutor threadPool;
		private final AtomicInteger threadNum = new AtomicInteger(0);

		public OurThreadPool(int minNumTHreads, int maxNumThreads) {
			threadPool = new ThreadPoolExecutor(minNumThreads, maxNumThreads, 60L, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>(), this);
		}

		@Override
		public boolean dispatch(Runnable job) {
			threadPool.submit(job);
			return true;
		}

		@Override
		public void join() throws InterruptedException {
			threadPool.shutdown();
			threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
		}

		@Override
		public int getThreads() {
			return threadPool.getPoolSize();
		}

		@Override
		public int getIdleThreads() {
			return threadPool.getPoolSize() - threadPool.getActiveCount();
		}

		@Override
		public boolean isLowOnThreads() {
			return false;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setName(JmxWebServer.class.getSimpleName() + '-' + threadNum.incrementAndGet());
			thread.setDaemon(false);
			return thread;
		}
	}
}
