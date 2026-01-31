package com.j256.simplejmx.web;

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JmxJetty9WebServerTest {

	private static final int WEB_SERVER_PORT = 8081;

	private static final AtomicInteger portCounter = new AtomicInteger(WEB_SERVER_PORT);

	@Test(timeout = 10000)
	public void testBasic() throws Exception {
		int port = portCounter.getAndIncrement();
		JmxJetty9WebServer webServer = new JmxJetty9WebServer(InetAddress.getByName("localhost"), port);
		webServer.start();
		try {
			testServer(port);
		} finally {
			webServer.close();
		}
	}

	@Test(timeout = 10000)
	public void testSpring() throws Exception {
		int port = portCounter.getAndIncrement();
		JmxJetty9WebServer webServer = new JmxJetty9WebServer();
		webServer.setServerAddress(InetAddress.getByName("localhost"));
		webServer.setServerPort(port);
		webServer.start();
		try {
			testServer(port);
		} finally {
			webServer.close();
		}
	}

	@Test(timeout = 10000)
	public void testCoverage() throws Exception {
		int port = portCounter.getAndIncrement();
		JmxJetty9WebServer webServer = new JmxJetty9WebServer(port);
		webServer.start();
		try {
			testServer(port);
		} finally {
			webServer.close();
			webServer.close();
		}
	}

	private void testServer(int port) throws Exception {
		WebClient webClient = new WebClient();
		HtmlPage page = webClient.getPage("http://localhost:" + port);
		assertTrue(page.asNormalizedText().contains("JMX Domains"));
		webClient.close();
	}
}
