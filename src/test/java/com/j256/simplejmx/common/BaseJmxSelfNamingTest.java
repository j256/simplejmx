package com.j256.simplejmx.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.InetAddress;

import javax.management.JMException;

import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;
import com.j256.simplejmx.server.JmxServer;

public class BaseJmxSelfNamingTest {

	private static final String DOMAIN_NAME = "foo.bar";
	private static final String JMX_RESOURCE_BEAN_NAME = "beanName";
	private static final String JMX_RESOURCE_NOT_FOUND_BEAN_NAME = "beanName23";
	private static final String JMX_SELF_NAMING_BEAN_NAME = "nameOfBean";

	@Test
	public void testOverrideOne() throws Exception {
		int port = 8000;
		InetAddress address = InetAddress.getByName("127.0.0.1");
		JmxServer server = new JmxServer(address, port);
		JmxClient client = null;
		try {
			server.start();
			OurJmxSelfNaming jmxObject = new OurJmxSelfNaming();
			server.register(jmxObject);

			client = new JmxClient(address, port);
			try {
				// wrong bean name here
				client.getAttribute(ObjectNameUtil.makeObjectName(DOMAIN_NAME, JMX_RESOURCE_NOT_FOUND_BEAN_NAME),
						"foo");
				fail("should have not found bean name " + JMX_RESOURCE_NOT_FOUND_BEAN_NAME);
			} catch (JMException e) {
				// expected
			}

			long value = (Long) client
					.getAttribute(ObjectNameUtil.makeObjectName(DOMAIN_NAME, JMX_SELF_NAMING_BEAN_NAME), "foo");
			assertEquals(jmxObject.foo, value);
		} finally {
			IoUtils.closeQuietly(client);
			IoUtils.closeQuietly(server);
		}
	}

	@Test
	public void testOverrideNone() throws Exception {
		int port = 8000;
		InetAddress address = InetAddress.getByName("127.0.0.1");
		JmxServer server = new JmxServer(address, port);
		JmxClient client = null;
		try {
			server.start();
			OurJmxObjectNoOverride jmxObject = new OurJmxObjectNoOverride();
			server.register(jmxObject);

			client = new JmxClient(address, port);
			long value = (Long) client.getAttribute(ObjectNameUtil.makeObjectName(DOMAIN_NAME, JMX_RESOURCE_BEAN_NAME),
					"foo");
			assertEquals(jmxObject.foo, value);
		} finally {
			IoUtils.closeQuietly(client);
			IoUtils.closeQuietly(server);
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = JMX_RESOURCE_NOT_FOUND_BEAN_NAME)
	private static class OurJmxSelfNaming extends BaseJmxSelfNaming implements JmxSelfNaming {

		@JmxAttributeField
		public long foo = 10;

		@Override
		public String getJmxBeanName() {
			return JMX_SELF_NAMING_BEAN_NAME;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = JMX_RESOURCE_BEAN_NAME)
	private static class OurJmxObjectNoOverride extends BaseJmxSelfNaming implements JmxSelfNaming {

		@JmxAttributeField
		public long foo = 10;
	}
}
