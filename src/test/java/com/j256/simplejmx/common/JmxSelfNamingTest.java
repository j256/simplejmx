package com.j256.simplejmx.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.InetAddress;

import javax.management.JMException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;
import com.j256.simplejmx.server.JmxServer;

public class JmxSelfNamingTest {

	private static final String JMX_RESOURCE_DOMAIN_NAME = "foo.bar";
	private static final String JMX_RESOURCE_BEAN_NAME = "beanName";

	private static final String JMX_SELF_NAMING_DOMAIN_NAME = "baz.bing";
	private static final String JMX_SELF_NAMING_BEAN_NAME = "nameOfBean";
	private static final JmxFolderName[] JMX_SELF_NAMING_FOLDERS = new JmxFolderName[] { new JmxFolderName("zing") };

	@Test
	public void testGetAll() throws Exception {
		int port = 8000;
		InetAddress address = InetAddress.getByName("127.0.0.1");
		JmxServer server = new JmxServer(address, port);
		JmxClient client = null;
		try {
			server.start();
			OurJmxObject jmxObject = new OurJmxObject();
			server.register(jmxObject);

			client = new JmxClient(address, port);
			try {
				client.getAttribute(ObjectNameUtil.makeObjectName(JMX_RESOURCE_DOMAIN_NAME, JMX_RESOURCE_BEAN_NAME,
						new String[] { "foo", "bar" }), "foo");
				fail("should have thrown");
			} catch (JMException e) {
				// expected
			}

			long value = (Long) client.getAttribute(ObjectNameUtil.makeObjectName(JMX_SELF_NAMING_DOMAIN_NAME,
					JMX_SELF_NAMING_BEAN_NAME, new String[] { "zing" }), "foo");
			assertEquals(jmxObject.foo, value);
		} finally {
			IOUtils.closeQuietly(client);
			server.stop();
		}
	}

	@JmxResource(domainName = JMX_RESOURCE_DOMAIN_NAME, beanName = JMX_RESOURCE_BEAN_NAME,
			folderNames = { "foo", "bar" })
	private static class OurJmxObject implements JmxSelfNaming {

		@JmxAttributeField
		public long foo = 10;

		@Override
		public String getJmxDomainName() {
			return JMX_SELF_NAMING_DOMAIN_NAME;
		}

		@Override
		public String getJmxBeanName() {
			return JMX_SELF_NAMING_BEAN_NAME;
		}

		@Override
		public JmxFolderName[] getJmxFolderNames() {
			return JMX_SELF_NAMING_FOLDERS;
		}
	}
}
