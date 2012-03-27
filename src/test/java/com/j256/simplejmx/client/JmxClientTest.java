package com.j256.simplejmx.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.j256.simplejmx.common.JmxAttribute;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.ObjectNameUtil;
import com.j256.simplejmx.server.JmxServer;

public class JmxClientTest {

	private static final int JMX_PORT = 8000;
	private static final String JMX_DOMAIN = "foo.com";

	private static JmxServer server;
	private static String objectNameName;
	private static ObjectName objectName;
	private static JmxClient client;
	private static JmxClient closedClient;

	@BeforeClass
	public static void beforeClass() throws Exception {
		server = new JmxServer(JMX_PORT);
		server.start();
		JmxClientTestObject obj = new JmxClientTestObject();
		server.register(obj);
		objectNameName = JmxClientTestObject.class.getSimpleName();
		objectName = ObjectNameUtil.makeObjectName(JMX_DOMAIN, objectNameName);
		client = new JmxClient(JMX_PORT);
		closedClient = new JmxClient(JMX_PORT);
		closedClient.closeThrow();
	}

	@AfterClass
	public static void afterClass() {
		if (client != null) {
			client.close();
			client = null;
		}
		if (server != null) {
			server.stop();
			server = null;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullUrl() throws Exception {
		new JmxClient(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidUrl() throws Exception {
		new JmxClient("invalid url");
	}

	@Test
	public void testHostPort() throws Exception {
		JmxClient client = new JmxClient("localhost", JMX_PORT);
		try {
			client.getAttribute(objectName, "x");
		} finally {
			client.closeThrow();
		}
	}

	@Test
	public void testGetBeanDomains() throws Exception {
		String[] domains = client.getBeanDomains();
		boolean found = false;
		for (String domain : domains) {
			if (JMX_DOMAIN.equals(domain)) {
				found = true;
			}
		}
		assertTrue(found);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetBeanDomainsClosed() throws Exception {
		closedClient.getBeanDomains();
	}

	@Test
	public void testGetBeanNames() throws Exception {
		Set<ObjectName> names = client.getBeanNames();
		boolean found = false;
		for (ObjectName name : names) {
			if (name.equals(objectName)) {
				found = true;
			}
		}
		assertTrue(found);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetBeanNamesClosed() throws Exception {
		closedClient.getBeanNames();
	}

	@Test
	public void testGetAttributesInfoStringString() throws Exception {
		MBeanAttributeInfo[] infos = client.getAttributesInfo(JMX_DOMAIN, objectNameName);
		assertEquals(2, infos.length);
		assertEquals("null", infos[0].getName());
		assertEquals(String.class.getName(), infos[0].getType());
		assertEquals("x", infos[1].getName());
		assertEquals(int.class.getName(), infos[1].getType());
	}

	@Test
	public void testGetAttributesInfo() throws Exception {
		MBeanAttributeInfo[] infos = client.getAttributesInfo(objectName);
		assertEquals(2, infos.length);
		assertEquals("null", infos[0].getName());
		assertEquals(String.class.getName(), infos[0].getType());
		assertEquals("x", infos[1].getName());
		assertEquals(int.class.getName(), infos[1].getType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAttributesInfoClosed() throws Exception {
		closedClient.getAttributesInfo(objectName);
	}

	@Test
	public void testGetAttributeInfo() throws Exception {
		MBeanAttributeInfo info = client.getAttributeInfo(objectName, "x");
		assertNotNull(info);
		assertEquals(int.class.getName(), info.getType());
	}

	@Test
	public void testGetAttributeInfoUnknown() throws Exception {
		JmxClient client = new JmxClient(JMX_PORT);
		try {
			assertNull(client.getAttributeInfo(objectName, "not-known"));
		} finally {
			client.closeThrow();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAttributeInfoClosed() throws Exception {
		closedClient.getAttributeInfo(objectName, "x");
	}

	@Test
	public void testGetOperationsInfo() throws Exception {
		MBeanOperationInfo[] infos = client.getOperationsInfo(objectName);
		assertEquals(3, infos.length);
		assertEquals("times", infos[0].getName());
		assertEquals("doThrow", infos[1].getName());
		assertEquals("returnNull", infos[2].getName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetOperationsInfoClosed() throws Exception {
		closedClient.getOperationsInfo(objectName);
	}

	@Test
	public void testGetOperationInfo() throws Exception {
		MBeanOperationInfo info = client.getOperationInfo(objectName, "times");
		assertNotNull(info);
		MBeanParameterInfo[] params = info.getSignature();
		assertEquals(2, params.length);
		assertEquals(int.class.getName(), params[0].getType());
		assertEquals(long.class.getName(), params[1].getType());
		assertEquals(String.class.getName(), info.getReturnType());
	}

	@Test
	public void testGetOperationInfoUnknown() throws Exception {
		assertNull(client.getOperationInfo(objectName, "unknownoperation"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetOperationInfoClosed() throws Exception {
		closedClient.getOperationInfo(objectName, "times");
	}

	@Test
	public void testGetAttributeStringString() throws Exception {
		int val = 13123;
		client.setAttribute(objectName, "x", val);
		Object result = client.getAttribute(JMX_DOMAIN, objectNameName, "x");
		assertEquals(val, result);
	}

	@Test
	public void testGetAttribute() throws Exception {
		int val = 13123;
		client.setAttribute(objectName, "x", val);
		Object result = client.getAttribute(objectName, "x");
		assertEquals(val, result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAttributeClosed() throws Exception {
		closedClient.getAttribute(objectName, "x");
	}

	@Test
	public void testGetAttributeAsStringStringString() throws Exception {
		int val = 13123;
		client.setAttribute(objectName, "x", val);
		String result = client.getAttributeString(JMX_DOMAIN, objectNameName, "x");
		assertEquals(Integer.toString(val), result);
	}

	@Test
	public void testGetAttributeAsString() throws Exception {
		int val = 13123;
		client.setAttribute(objectName, "x", val);
		String result = client.getAttributeString(objectName, "x");
		assertEquals(Integer.toString(val), result);
	}

	@Test
	public void testGetAttributeAsStringNull() throws Exception {
		String result = client.getAttributeString(objectName, "null");
		assertNull(result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAttributeAsStringClosed() throws Exception {
		closedClient.getAttributeString(objectName, "x");
	}

	@Test
	public void testSetAttributeStringString() throws Exception {
		int val = 2131231231;
		client.setAttribute(JMX_DOMAIN, objectNameName, "x", val);
		Object result = client.getAttribute(objectName, "x");
		assertEquals(val, result);
	}

	@Test
	public void testSetAttributeStringStringToString() throws Exception {
		int val = 2131231231;
		client.setAttribute(JMX_DOMAIN, objectNameName, "x", Integer.toString(val));
		Object result = client.getAttribute(objectName, "x");
		assertEquals(val, result);
	}

	@Test
	public void testSetAttributeToString() throws Exception {
		int val = 2131231231;
		client.setAttribute(objectName, "x", Integer.toString(val));
		Object result = client.getAttribute(objectName, "x");
		assertEquals(val, result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetAttributeClosed() throws Exception {
		closedClient.setAttribute(objectName, "x", 1);
	}

	@JmxResource(domainName = JMX_DOMAIN)
	protected static class JmxClientTestObject {
		int x;
		@JmxAttribute
		public void setX(int x) {
			this.x = x;
		}
		@JmxAttribute
		public int getX() {
			return x;
		}
		@JmxAttribute
		public String getNull() {
			return null;
		}
		@JmxOperation
		public String times(int x1, long x2) {
			return Long.valueOf(x1 * x2).toString();
		}
		@JmxOperation
		public void doThrow() {
			throw new RuntimeException("throw away!");
		}
		@JmxOperation
		public Object returnNull() {
			return null;
		}
	}
}
