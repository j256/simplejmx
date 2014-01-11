package com.j256.simplejmx.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.management.JMException;

import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;
import com.j256.simplejmx.server.JmxServer;

public class BaseJmxSelfNamingTest {

	private static final String DOMAIN_NAME = "foo.bar";
	private static final String JMX_RESOURCE_BEAN_NAME = "beanName";

	private static final String JMX_SELF_NAMING_BEAN_NAME = "nameOfBean";

	@Test
	public void testOverrideOne() throws Exception {
		int port = 8000;
		JmxServer server = new JmxServer(port);
		try {
			server.start();
			OurJmxObject jmxObject = new OurJmxObject();
			server.register(jmxObject);

			JmxClient client = new JmxClient(port);
			try {
				client.getAttribute(ObjectNameUtil.makeObjectName(DOMAIN_NAME, JMX_RESOURCE_BEAN_NAME), "foo");
				fail("should have thrown");
			} catch (JMException e) {
				// expected
			}

			long value =
					(Long) client.getAttribute(ObjectNameUtil.makeObjectName(DOMAIN_NAME, JMX_SELF_NAMING_BEAN_NAME),
							"foo");
			assertEquals(jmxObject.foo, value);
		} finally {
			server.stop();
		}
	}

	@Test
	public void testOverrideNone() throws Exception {
		int port = 8000;
		JmxServer server = new JmxServer(port);
		try {
			server.start();
			OurJmxObjectNoOverride jmxObject = new OurJmxObjectNoOverride();
			server.register(jmxObject);

			JmxClient client = new JmxClient(port);
			long value =
					(Long) client.getAttribute(ObjectNameUtil.makeObjectName(DOMAIN_NAME, JMX_RESOURCE_BEAN_NAME),
							"foo");
			assertEquals(jmxObject.foo, value);
		} finally {
			server.stop();
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = JMX_RESOURCE_BEAN_NAME)
	private static class OurJmxObject extends BaseJmxSelfNaming implements JmxSelfNaming {

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
