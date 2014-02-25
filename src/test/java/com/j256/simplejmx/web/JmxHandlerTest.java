package com.j256.simplejmx.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class JmxHandlerTest {

	private static final int WEB_SERVER_PORT = 8080;
	private static JmxWebServer webServer;

	@BeforeClass
	public static void beforeClass() throws Exception {
		webServer = new JmxWebServer(WEB_SERVER_PORT);
		webServer.start();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		webServer.stop();
	}

	@Test
	public void testSimple() throws Exception {
		WebClient webClient = new WebClient();
		HtmlPage page = webClient.getPage("http://localhost:" + WEB_SERVER_PORT);
		assertTrue(page.asText().contains("JMX Domains"));
		String domain = "java.lang";
		HtmlAnchor anchor = page.getAnchorByText(domain);
		assertNotNull(anchor);
		page = anchor.click();

		assertTrue(page.asText().contains("Beans in domain " + domain));
		String bean = "type=Memory";
		anchor = page.getAnchorByText(bean);
		page = anchor.click();

		assertTrue(page.asText().contains("Information about object " + domain + ":" + bean));
		HtmlForm form = page.getFormByName("Verbose");
		assertNotNull(form);
		HtmlInput input = form.getInputByName("v");
		assertEquals("false", input.getValueAttribute());
		assertNotNull(input);
		input.setValueAttribute("true");

		assertTrue(page.asText().contains("Information about object " + domain + ":" + bean));
		form = page.getFormByName("Verbose");
		assertNotNull(form);
		input = form.getInputByName("v");
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
		TextPage textPage = anchor.click();
		assertEquals(operation + " method successfully invoked.\n", textPage.getContent());
	}
}
