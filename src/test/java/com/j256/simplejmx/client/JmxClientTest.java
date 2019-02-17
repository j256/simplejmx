package com.j256.simplejmx.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.j256.simplejmx.common.IoUtils;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.ObjectNameUtil;
import com.j256.simplejmx.server.JmxServer;

public class JmxClientTest {

	private static final int JMX_PORT = 8000;
	private static final String JMX_DOMAIN = "foo.com";

	private static JmxServer server;
	private static String beanName;
	private static ObjectName objectName;
	private static String anotherBeanName;
	private static ObjectName anotherObjectName;
	private static JmxClient client;
	private static JmxClient closedClient;
	private static JmxClientTestObject testObject;

	@BeforeClass
	public static void beforeClass() throws Exception {
		InetAddress address = InetAddress.getByName("localhost");
		server = new JmxServer(address, JMX_PORT);
		server.start();
		testObject = new JmxClientTestObject();
		server.register(testObject);
		beanName = JmxClientTestObject.class.getSimpleName();
		objectName = ObjectNameUtil.makeObjectName(JMX_DOMAIN, beanName);

		JmxClientTestAnotherObject anotherObj = new JmxClientTestAnotherObject();
		server.register(anotherObj);
		anotherBeanName = JmxClientTestAnotherObject.class.getSimpleName();
		anotherObjectName = ObjectNameUtil.makeObjectName(JMX_DOMAIN, anotherBeanName);

		client = new JmxClient(address, JMX_PORT);
		closedClient = new JmxClient(address, JMX_PORT);
		closedClient.closeThrow();
		closedClient.closeThrow();
	}

	@AfterClass
	public static void afterClass() {
		IoUtils.closeQuietly(client);
		client = null;
		IoUtils.closeQuietly(server);
		server = null;
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullUrl() throws Exception {
		new JmxClient(null).close();
	}

	@Test(expected = JMException.class)
	public void testInvalidUrl() throws Exception {
		new JmxClient("invalid url").close();
	}

	@Test
	public void testHostPort() throws Exception {
		@SuppressWarnings("resource")
		JmxClient client = new JmxClient("localhost", JMX_PORT);
		try {
			client.getAttribute(objectName, "x");
		} finally {
			client.closeThrow();
		}
	}

	@Test
	public void testHostPortEnvironment() throws Exception {
		@SuppressWarnings("resource")
		JmxClient client = new JmxClient("localhost", JMX_PORT,
				Collections.<String, Object> singletonMap("jmx.remote.x.client.connection.check.period", 5000L));
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
		MBeanAttributeInfo[] infos = client.getAttributesInfo(JMX_DOMAIN, beanName);
		assertEquals(2, infos.length);
		assertEquals("null", infos[0].getName());
		assertEquals(String.class.getName(), infos[0].getType());
		assertEquals("x", infos[1].getName());
		assertEquals(int.class.getName(), infos[1].getType());
		int val = 349813481;
		testObject.x = val;
		Object value = client.getAttribute(JMX_DOMAIN, beanName, "x");
		assertTrue(value instanceof Integer);
		assertEquals(val, (int) value);
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
		assertNull(client.getAttributeInfo(objectName, "not-known"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetAttributeUnknown() throws Exception {
		client.setAttribute(objectName, "not-known", "1");
	}

	@Test(expected = AttributeNotFoundException.class)
	public void testSetAttributeUnknownObj() throws Exception {
		client.setAttribute(objectName, "not-known", 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAttributeInfoClosed() throws Exception {
		closedClient.getAttributeInfo(objectName, "x");
	}

	@Test
	public void testGetOperationsInfo() throws Exception {
		MBeanOperationInfo[] infos = client.getOperationsInfo(objectName);
		assertEquals(13, infos.length);
		Set<String> expectedNames =
				new HashSet<String>(Arrays.asList("times", "shortToString", "byteToString", "charToString", "doThrow"));

		// orders seem to change
		for (MBeanOperationInfo info : infos) {
			expectedNames.remove(info.getName());
		}

		for (String expectedName : expectedNames) {
			fail("should have matched " + expectedName);
		}
	}

	@Test
	public void testGetOperationsInfoStringString() throws Exception {
		MBeanOperationInfo[] infos = client.getOperationsInfo(JMX_DOMAIN, beanName);
		assertEquals(13, infos.length);
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
		assertEquals(short.class.getName(), params[0].getType());
		assertEquals(int.class.getName(), params[1].getType());
		assertEquals(long.class.getName(), info.getReturnType());
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
		Object result = client.getAttribute(JMX_DOMAIN, beanName, "x");
		assertEquals(val, result);
	}

	@Test
	public void testGetAttribute() throws Exception {
		int val = 13123;
		client.setAttribute(objectName, "x", val);
		Object result = client.getAttribute(objectName, "x");
		assertEquals(val, result);
	}

	@Test
	public void testGetAttributes() throws Exception {
		int val = 13123;
		client.setAttribute(objectName, "x", val);
		List<Attribute> results = client.getAttributes(objectName, new String[] { "x" });
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(new Attribute("x", val), results.get(0));
	}

	@Test
	public void testGetAttributesStringString() throws Exception {
		int val = 213132;
		client.setAttribute(objectName, "x", val);
		List<Attribute> results = client.getAttributes(JMX_DOMAIN, beanName, new String[] { "x" });
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(new Attribute("x", val), results.get(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetAttributeClosed() throws Exception {
		closedClient.getAttribute(objectName, "x");
	}

	@Test
	public void testGetAttributeAsStringStringString() throws Exception {
		int val = 13123;
		client.setAttribute(objectName, "x", val);
		String result = client.getAttributeString(JMX_DOMAIN, beanName, "x");
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
		client.setAttribute(JMX_DOMAIN, beanName, "x", val);
		Object result = client.getAttribute(objectName, "x");
		assertEquals(val, result);
	}

	@Test
	public void testSetAttributeStringStringToString() throws Exception {
		int val = 2131231231;
		client.setAttribute(JMX_DOMAIN, beanName, "x", Integer.toString(val));
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

	@Test
	public void testSetAttributes() throws Exception {
		int val = 217731231;
		client.setAttributes(objectName, Collections.singletonList(new Attribute("x", val)));
		Object result = client.getAttribute(objectName, "x");
		assertEquals(val, result);
	}

	@Test
	public void testSetAttributesStringString() throws Exception {
		int val = 218131231;
		client.setAttributes(JMX_DOMAIN, beanName, Collections.singletonList(new Attribute("x", val)));
		Object result = client.getAttribute(objectName, "x");
		assertEquals(val, result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetAttributeClosed() throws Exception {
		closedClient.setAttribute(objectName, "x", 1);
	}

	@Test
	public void testInvokeOperationStringString() throws Exception {
		short val1 = 231;
		int val2 = 524;
		Object result = client.invokeOperation(JMX_DOMAIN, beanName, "times", val1, val2);
		long times = val1 * val2;
		assertEquals(times, result);
	}

	@Test
	public void testInvokeOperationStringStringString() throws Exception {
		short val1 = 231;
		int val2 = 524;
		Object result =
				client.invokeOperation(JMX_DOMAIN, beanName, "times", Short.toString(val1), Integer.toString(val2));
		long times = val1 * val2;
		assertEquals(times, result);
	}

	@Test
	public void testInvokeOperation() throws Exception {
		short val1 = 231;
		int val2 = 524;
		Object result = client.invokeOperation(objectName, "times", val1, val2);
		long times = val1 * val2;
		assertEquals(times, result);
	}

	@Test
	public void testInvokeOperationStrings() throws Exception {
		short val1 = 231;
		int val2 = 524;
		Object result = client.invokeOperation(objectName, "times", Short.toString(val1), Integer.toString(val2));
		long times = val1 * val2;
		assertEquals(times, result);
	}

	@Test
	public void testInvokeOperationStringsNoArgs() throws Exception {
		assertNull(client.invokeOperation(objectName, "returnNull", new String[0]));
	}

	@Test
	public void testInvokeOperationNoArgs() throws Exception {
		assertNull(client.invokeOperation(objectName, "returnNull", new Object[0]));
	}

	@Test
	public void testInvokeOperationStringStringNoArgs() throws Exception {
		assertNull(client.invokeOperation(JMX_DOMAIN, beanName, "returnNull", new String[0]));
	}

	@Test
	public void testInvokeOperationArgs() throws Exception {
		assertNull(client.invokeOperation(objectName, "returnNull", 1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvokeOperationClosed() throws Exception {
		closedClient.invokeOperation(objectName, "returnNull", new Object[0]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvokeOperationUnknown() throws Exception {
		client.invokeOperation(objectName, "unknown-operation");
	}

	@Test
	public void testInvokeOperationToString() throws Exception {
		short val1 = 21231;
		int val2 = 524;
		String result =
				client.invokeOperationToString(objectName, "times", Short.toString(val1), Integer.toString(val2));
		int times = val1 * val2;
		assertEquals(Long.toString(times), result);
	}

	@Test
	public void testBoolToString() throws Exception {
		testThingtoString("booleanToString", true);
	}

	@Test
	public void testCharToString() throws Exception {
		testThingtoString("charToString", '\0');
	}

	@Test
	public void testCharToStringNoArg() throws Exception {
		testThingtoString("charToString", "\0", "");
	}

	@Test
	public void testByteToString() throws Exception {
		testThingtoString("byteToString", (byte) 10);
	}

	@Test
	public void testShortToString() throws Exception {
		testThingtoString("shortToString", (short) 1042);
	}

	@Test
	public void testIntToString() throws Exception {
		testThingtoString("intToString", (int) 123423320);
	}

	@Test
	public void testLongToString() throws Exception {
		testThingtoString("longToString", (long) 10824424242L);
	}

	@Test
	public void testFloatToString() throws Exception {
		testThingtoString("floatToString", (float) 12.56);
	}

	@Test
	public void testDoubleToString() throws Exception {
		testThingtoString("doubleToString", (double) 1313.4122);
	}

	@Test
	public void testInvokeOperationStringsObject() throws Exception {
		testThingtoString("dateToString", new Date());
	}

	@Test
	public void testSystem() throws Exception {
		client.getAttribute(ObjectName.getInstance("java.lang:type=OperatingSystem"), "AvailableProcessors");
	}

	@Test
	public void testGetBeansForDomain() throws Exception {
		String domainName = "java.lang";
		Set<ObjectName> beans = client.getBeanNames(domainName);
		boolean found = false;
		String beanName = domainName + ":type=OperatingSystem";
		for (ObjectName objectName : beans) {
			String nameString = objectName.getCanonicalName();
			assertTrue(nameString.startsWith(domainName + ":"));
			if (beanName.equals(nameString)) {
				found = true;
			}
		}
		assertTrue("Found bean " + beanName, found);
	}

	@Test
	public void testAnotherObjectAttribute() throws Exception {
		int val = 273907923;
		client.setAttribute(anotherObjectName, "y", Integer.toString(val));
		assertEquals(val, client.getAttribute(anotherObjectName, "y"));
	}

	@Test
	public void testAnotherObjectOperation() throws Exception {
		short val1 = 23;
		int val2 = 245;
		Object result = client.invokeOperation(anotherObjectName, "timesTwo", val1, val2);
		long times = val1 * val2;
		assertEquals(times, result);
	}

	@Test
	public void testCoverage() {
		try {
			new JmxClient(JMX_PORT).close();
		} catch (JMException jme) {
			// ignored
		}
	}

	@Test
	public void testUserNamePass() throws Exception {
		new JmxClient("localhost", JMX_PORT, "user", "pass").close();
	}

	@Test
	public void testUserNamePassNull() throws Exception {
		new JmxClient("localhost", JMX_PORT, null, null).close();
	}

	@Test
	public void testServiceUrl() throws Exception {
		new JmxClient("service:jmx:rmi:///jndi/rmi://localhost:" + JMX_PORT + "/jmxrmi").close();
	}

	@Test
	public void testUserNamePassServiceUrl() throws Exception {
		new JmxClient("service:jmx:rmi:///jndi/rmi://localhost:" + JMX_PORT + "/jmxrmi", null, null).close();
	}

	@Test
	public void testUserNamePassNull2() throws Exception {
		new JmxClient("service:jmx:rmi:///jndi/rmi://localhost:" + JMX_PORT + "/jmxrmi", null, null,
				Collections.<String, Object> emptyMap()).close();
	}

	/* ======================================================================= */

	private void testThingtoString(String methodName, Object arg) throws Exception {
		String argString = arg.toString();
		testThingtoString(methodName, argString, argString);
	}

	private void testThingtoString(String methodName, String expected, String argString) throws Exception {
		assertEquals(expected, client.invokeOperationToString(objectName, methodName, argString));
	}

	@JmxResource(domainName = JMX_DOMAIN)
	protected static class JmxClientTestObject {
		volatile int x;

		@JmxAttributeMethod
		public void setX(int x) {
			this.x = x;
		}

		@JmxAttributeMethod
		public int getX() {
			return x;
		}

		@JmxAttributeMethod
		public String getNull() {
			return null;
		}

		@JmxOperation
		public long times(short x1, int x2) {
			return x1 * x2;
		}

		@JmxOperation
		public void doThrow() {
			throw new RuntimeException("throw away!");
		}

		@JmxOperation
		public Object returnNull() {
			return null;
		}

		@JmxOperation
		public Object returnNull(int anotherArg) {
			return null;
		}

		@JmxOperation
		public String dateToString(Date date) {
			return date.toString();
		}

		@JmxOperation
		public String booleanToString(boolean booleanVal) {
			return Boolean.toString(booleanVal);
		}

		@JmxOperation
		public String charToString(char charVal) {
			return Character.toString(charVal);
		}

		@JmxOperation
		public String byteToString(byte byteVal) {
			return Byte.toString(byteVal);
		}

		@JmxOperation
		public String shortToString(short shortVal) {
			return Short.toString(shortVal);
		}

		@JmxOperation
		public String intToString(int intVal) {
			return Integer.toString(intVal);
		}

		@JmxOperation
		public String longToString(long longVal) {
			return Long.toString(longVal);
		}

		@JmxOperation
		public String floatToString(float floatVal) {
			return Float.toString(floatVal);
		}

		@JmxOperation
		public String doubleToString(double doubleVal) {
			return Double.toString(doubleVal);
		}
	}

	@JmxResource(domainName = JMX_DOMAIN)
	protected static class JmxClientTestAnotherObject {
		int y;

		@JmxAttributeMethod
		public int getY() {
			return y;
		}

		@JmxAttributeMethod
		public void setY(int y) {
			this.y = y;
		}

		@JmxOperation
		public long timesTwo(short y1, int y2) {
			return y1 * y2;
		}
	}
}
