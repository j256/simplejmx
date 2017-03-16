package com.j256.simplejmx.web;

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JmxWebServerTest {

	private static final int WEB_SERVER_PORT = 8080;

	@Test(timeout = 10000)
	public void testBasic() throws Exception {
		JmxWebServer webServer = new JmxWebServer(InetAddress.getByName("localhost"), WEB_SERVER_PORT);
		webServer.start();
		Thread.sleep(2000);
		try {
			testServer();
		} finally {
			webServer.stop();
		}
	}

	@Test(timeout = 10000)
	public void testSpring() throws Exception {
		JmxWebServer webServer = new JmxWebServer();
		webServer.setServerAddress(InetAddress.getByName("localhost"));
		webServer.setServerPort(WEB_SERVER_PORT);
		webServer.start();
		Thread.sleep(2000);
		try {
			testServer();
		} finally {
			webServer.stop();
		}
	}

	private void testServer() throws Exception {
		WebClient webClient = new WebClient();
		HtmlPage page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT);
		assertTrue(page.asText().contains("JMX Domains"));
	}
}
