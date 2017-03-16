package com.j256.simplejmx.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JmxWebHandlerTest {

	private static final int WEB_SERVER_PORT = 8080;
	private static JmxWebServer webServer;

	@BeforeClass
	public static void beforeClass() throws Exception {
		webServer = new JmxWebServer(InetAddress.getByName("localhost"), WEB_SERVER_PORT);
		webServer.start();
		System.err.println("Web server started");
		Thread.sleep(2000);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (webServer != null) {
			webServer.stop();
			System.err.println("Web server stopped");
		}
	}

	@Test(timeout = 10000)
	public void testSimple() throws Exception {
		WebClient webClient = new WebClient();
		HtmlPage page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT);
		assertTrue(page.asText().contains("JMX Domains"));
		System.err.println("Got first page");

		String domain = "java.lang";
		HtmlAnchor anchor = page.getAnchorByText(domain);
		assertNotNull(anchor);
		page = anchor.click();
		assertTrue(page.asText().contains("Beans in domain " + domain));

		anchor = page.getAnchorByName("text");
		TextPage textPage = anchor.click();
		String bean = "type=Memory";
		assertTrue(textPage.getContent().contains(domain + ":" + bean));

		anchor = page.getAnchorByText(bean);
		page = anchor.click();
		assertTrue(page.asText().contains("Information about object " + domain + ":" + bean));

		anchor = page.getAnchorByName("text");
		textPage = anchor.click();
		assertTrue(textPage.getContent().contains("Verbose"));

		HtmlForm form = page.getFormByName("Verbose");
		assertNotNull(form);
		HtmlInput input = form.getInputByName("val");
		assertEquals("false", input.getValueAttribute());
		assertNotNull(input);
		input.setValueAttribute("true");
		HtmlElement button = (HtmlElement) page.createElement("button");
		button.setAttribute("type", "submit");
		// append the button to the form to simulate
		form.appendChild(button);
		// submit the form
		page = button.click();
		assertTrue(page.asText().contains("Information about object " + domain + ":" + bean));

		form = page.getFormByName("Verbose");
		assertNotNull(form);
		input = form.getInputByName("val");
		assertEquals("true", input.getValueAttribute());

		String operation = "gc";
		form = page.getFormByName(operation);
		assertNotNull(form);
		input = form.getInputByValue(operation);
		page = input.click();

		assertTrue(page.asText().contains("Invoking Operation " + operation));
		assertTrue(page.asText().contains(operation + " method successfully invoked."));

		anchor = page.getAnchorByName("text");
		assertNotNull(anchor);
		textPage = anchor.click();
		assertEquals(operation + " method successfully invoked.\n", textPage.getContent());
	}

	@Test(timeout = 10000)
	public void testOtherStuff() throws Exception {
		WebClient webClient = new WebClient();
		HtmlPage page = webClient.getPage("http://" + InetAddress.getLocalHost() + ":" + WEB_SERVER_PORT + "/s");
		assertTrue(page.asText().contains("All Beans"));
		System.err.println("Got first page");

		String beanName = "java.lang:type=Memory";
		HtmlAnchor anchor = page.getAnchorByText(beanName);
		page = anchor.click();
		assertTrue(page.asText().contains("Information about object " + beanName));

		try {
			webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/not-found/");
			fail("should have thrown");
		} catch (FailingHttpStatusCodeException fhsce) {
			// ignored
		}

		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/b/:::::");
		assertTrue(page.asText(), page.asText().contains("Invalid object name"));

		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/b/hello:name=there");
		assertTrue(page.asText(), page.asText().contains("Investigating object threw exception"));

		beanName = "java.lang:type=Runtime";
		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/b/" + beanName);
		assertTrue(page.asText(), page.asText().contains("Information about object " + beanName));

		anchor = page.getAnchorByName("text");
		TextPage textPage = anchor.click();
		assertTrue(textPage.getContent().contains("ClassPath="));

		beanName = "java.lang:type=Threading";
		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/b/" + beanName);
		assertTrue(page.asText().contains("Information about object " + beanName));

		anchor = page.getAnchorByName("text");
		textPage = anchor.click();
		assertTrue(textPage.getContent().contains("dumpAllThreads boolean boolean"));

		/* assign errors */

		beanName = "java.lang:type=Memory";
		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/a/" + beanName);
		assertTrue(page.asText().contains("Invalid number of parameters"));

		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/a/" + beanName + "/Verbose");
		assertTrue(page.asText().contains("No value parameter specified"));

		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/a/bad-name/Verbose?val=foo");
		assertTrue(page.asText().contains("Invalid object name"));

		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/a/not:name=found/Verbose?val=foo");
		assertTrue(page.asText().contains("Could not get mbean info"));

		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/a/" + beanName + "/notFound?val=foo");
		assertTrue(page.asText().contains("Cannot find attribute"));

		page = webClient.getPage(
				"http://localhost:" + WEB_SERVER_PORT + "/a/" + beanName + "/ObjectPendingFinalizationCount?val=foo");
		assertTrue(page.asText().contains("Could not set attribute"));

		/* invoke errors */

		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/o/");
		assertTrue(page.asText().contains("Invalid number of parameters"));

		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/o/bad-name/gc/");
		assertTrue(page.asText(), page.asText().contains("Invalid object name"));

		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/o/not:name=found/gc/");
		assertTrue(page.asText(), page.asText().contains("Could not get mbean info"));

		page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT + "/o/" + beanName + "/not-found/");
		assertTrue(page.asText(), page.asText().contains("Cannot find operation"));
	}
}
