package com.j256.simplejmx.web;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.EasyMock;
import org.eclipse.jetty.server.Request;
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
import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.server.JmxServer;

public class JmxWebHandlerTest {

	private static final int WEB_SERVER_PORT = 8080;
	private static final String WEB_SERVER_NAME = "127.0.0.1";
	private static final String DOMAIN_NAME = "j256.com";
	private static final String OBJECT_NAME = "TestBean";
	private static final int OP_PARAM_THROWS = 1414124;

	private static JmxWebServer webServer;
	private static JmxServer jmxServer;
	private static final TestBean testBean = new TestBean();

	@BeforeClass
	public static void beforeClass() throws Exception {
		jmxServer = new JmxServer(9113);
		jmxServer.start();
		jmxServer.register(testBean);
		webServer = new JmxWebServer(InetAddress.getByName(WEB_SERVER_NAME), WEB_SERVER_PORT);
		webServer.start();
		Thread.sleep(2000);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		// Thread.sleep(100000);
		if (jmxServer != null) {
			jmxServer.stop();
			jmxServer = null;
		}
		if (webServer != null) {
			webServer.stop();
			webServer = null;
		}
	}

	@Test(timeout = 10000)
	public void testSimple() throws Exception {
		WebClient webClient = new WebClient();
		HtmlPage page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT);
		assertTrue(page.asText().contains("JMX Domains"));

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
		HtmlPage page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT);
		assertTrue(page.asText().contains("Show all beans"));

		HtmlAnchor anchor = page.getAnchorByText("Show all beans.");
		assertNotNull(anchor);
		page = anchor.click();
		assertTrue(page.asText().contains("All Beans"));

		String beanName = "java.lang:type=Memory";
		anchor = page.getAnchorByText(beanName);
		page = anchor.click();
		assertTrue(page.asText().contains("Information about object " + beanName));

		try {
			webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/not-found/");
			fail("should have thrown");
		} catch (FailingHttpStatusCodeException fhsce) {
			// ignored
		}

		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/b/:::::");
		assertTrue(page.asText(), page.asText().contains("Invalid object name"));

		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/b/hello:name=there");
		assertTrue(page.asText(), page.asText().contains("Investigating object threw exception"));

		beanName = "java.lang:type=Runtime";
		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/b/" + beanName);
		assertTrue(page.asText(), page.asText().contains("Information about object " + beanName));

		anchor = page.getAnchorByName("text");
		TextPage textPage = anchor.click();
		assertTrue(textPage.getContent().contains("ClassPath="));

		beanName = "java.lang:type=Threading";
		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/b/" + beanName);
		assertTrue(page.asText().contains("Information about object " + beanName));

		anchor = page.getAnchorByName("text");
		textPage = anchor.click();
		assertTrue(textPage.getContent().contains("dumpAllThreads boolean boolean"));

		/* assign errors */

		beanName = "java.lang:type=Memory";
		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/a/" + beanName);
		assertTrue(page.asText().contains("Invalid number of parameters"));

		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/a/" + beanName + "/Verbose");
		assertTrue(page.asText().contains("No value parameter specified"));

		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/a/bad-name/Verbose?val=foo");
		assertTrue(page.asText().contains("Invalid object name"));

		page = webClient
				.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/a/not:name=found/Verbose?val=foo");
		assertTrue(page.asText().contains("Could not get mbean info"));

		page = webClient
				.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/a/" + beanName + "/notFound?val=foo");
		assertTrue(page.asText().contains("Cannot find attribute"));

		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/a/" + beanName
				+ "/ObjectPendingFinalizationCount?val=foo");
		assertTrue(page.asText().contains("Could not set attribute"));

		textPage = webClient.getPage(
				"http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/a/" + beanName + "/Verbose?val=false&t=true");
		assertTrue(textPage.getContent().contains("Verbose set to false"));

		/* invoke errors */

		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/o/");
		assertTrue(page.asText().contains("Invalid number of parameters"));

		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/o/bad-name/gc/");
		assertTrue(page.asText(), page.asText().contains("Invalid object name"));

		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/o/not:name=found/gc/");
		assertTrue(page.asText(), page.asText().contains("Could not get mbean info"));

		page = webClient
				.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/o/" + beanName + "/not-found/");
		assertTrue(page.asText(), page.asText().contains("Cannot find operation"));

		textPage = webClient
				.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/a/bad-name/Verbose?val=foo&t=true");
		assertTrue(textPage.getContent(), textPage.getContent().contains("Invalid object name"));

		/* special conditions */

		beanName = DOMAIN_NAME + ":name=" + OBJECT_NAME;
		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/b/" + beanName);
		// assertFalse(page.asText(), page.asText().contains("Description"));

		textPage =
				webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/b/" + beanName + "?t=true");
		assertTrue(textPage.getContent().contains("noread=not readable"));

		int val = 11232;
		page = webClient
				.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/o/" + beanName + "/op?p0=" + val);
		assertTrue(page.asText(), page.asText().contains("op result is: " + val));

		page = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/o/" + beanName + "/op?p0=wow");
		assertTrue(page.asText(), page.asText().contains("NumberFormatException"));

		page = webClient.getPage(
				"http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/o/" + beanName + "/op?p0=" + OP_PARAM_THROWS);
		assertTrue(page.asText(), page.asText().contains(OBJECT_NAME + " threw exception"));

		textPage = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/s?t=true");
		assertTrue(textPage.getContent().contains("java.lang:type=Runtime"));

		textPage = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/d/java.lang?t=true");
		assertTrue(textPage.getContent().contains("java.lang:type=Runtime"));

		textPage = webClient.getPage("http://" + WEB_SERVER_NAME + ":" + WEB_SERVER_PORT + "/?t=true");
		assertTrue(textPage.getContent().contains("java.lang"));
	}

	@Test
	public void coverage() throws IOException {
		JmxWebHandler handler = new JmxWebHandler();
		Request request = new Request();
		HttpServletRequest servletRequest = EasyMock.createMock(HttpServletRequest.class);
		HttpServletResponse servletResponse = EasyMock.createMock(HttpServletResponse.class);

		ServletOutputStream outputStream = new ServletOutputStream() {
			@Override
			public void write(int b) {
			}
		};
		expect(servletResponse.getOutputStream()).andReturn(outputStream);
		servletResponse.setContentType("text/html");
		expect(servletRequest.getPathInfo()).andReturn(null);
		expect(servletRequest.getParameter("t")).andReturn(null);

		replay(servletRequest, servletResponse);
		handler.handle(null, request, servletRequest, servletResponse);
		verify(servletRequest, servletResponse);
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	public static class TestBean {
		@JmxAttributeField(isReadible = false)
		int noread;

		@JmxAttributeMethod
		public int getThrows() {
			throw new RuntimeException();
		}

		@JmxOperation
		public String op(int foo) {
			if (foo == OP_PARAM_THROWS) {
				throw new RuntimeException();
			} else {
				return Integer.toString(foo);
			}
		}
	}
}
