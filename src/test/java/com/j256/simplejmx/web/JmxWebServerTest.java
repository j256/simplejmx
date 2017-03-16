package com.j256.simplejmx.web;

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JmxWebServerTest {

	private static final int WEB_SERVER_PORT = 8081;

	private static final AtomicInteger portCounter = new AtomicInteger(WEB_SERVER_PORT);

	@Test(timeout = 10000)
	public void testBasic() throws Exception {
		int port = portCounter.getAndIncrement();
		JmxWebServer webServer = new JmxWebServer(InetAddress.getByName("localhost"), port);
		webServer.start();
		Thread.sleep(1000);
		try {
			testServer(port);
		} finally {
			webServer.stop();
		}
	}

	@Test(timeout = 10000)
	public void testSpring() throws Exception {
		int port = portCounter.getAndIncrement();
		JmxWebServer webServer = new JmxWebServer();
		webServer.setServerAddress(InetAddress.getByName("localhost"));
		webServer.setServerPort(port);
		webServer.start();
		Thread.sleep(1000);
		try {
			testServer(port);
		} finally {
			webServer.stop();
		}
	}

	private void testServer(int port) throws Exception {
		WebClient webClient = new WebClient();
		HtmlPage page = webClient.getPage("http://localhost:" + port);
		assertTrue(page.asText().contains("JMX Domains"));
	}
}
