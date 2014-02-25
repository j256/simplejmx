package com.j256.simplejmx.web;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JmxWebServerTest {

	private static final int WEB_SERVER_PORT = 8080;

	@Test
	public void testBasic() throws Exception {
		JmxWebServer webServer = new JmxWebServer(WEB_SERVER_PORT);
		webServer.start();
		try {
			testServer();
		} finally {
			webServer.stop();
		}
	}

	@Test
	public void testSpring() throws Exception {
		JmxWebServer webServer = new JmxWebServer();
		webServer.setServerPort(WEB_SERVER_PORT);
		webServer.start();
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
