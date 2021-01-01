package com.j256.simplejmx.web;

import java.net.InetAddress;

import org.junit.Ignore;
import org.junit.Test;

import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.server.JmxServer;

@Ignore("Just for integration testing")
public class JmxWebIntergrationTest {

	private static final int WEB_SERVER_PORT = 8080;
	private static final String WEB_SERVER_NAME = "127.0.0.1";
	private static final String DOMAIN_NAME = "j256.com";
	private static final String OBJECT_NAME = "TestBean";
	private static final int OP_PARAM_THROWS = 1414124;

	private static JmxWebServer webServer;
	private static JmxServer jmxServer;
	private static final TestBean testBean = new TestBean();

	@Test
	public void testStuff() throws Exception {
		jmxServer = new JmxServer(9113);
		jmxServer.start();
		jmxServer.register(testBean);
		webServer = new JmxWebServer(InetAddress.getByName(WEB_SERVER_NAME), WEB_SERVER_PORT);
		webServer.start();
		System.out.println("Web server running on port: " + WEB_SERVER_PORT);
		Thread.sleep(3600000);
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
