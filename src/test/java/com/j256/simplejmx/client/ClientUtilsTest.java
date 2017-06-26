package com.j256.simplejmx.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ClientUtilsTest {

	@Test
	public void testStringToParam() {
		assertEquals(true, ClientUtils.stringToParam("true", "boolean"));
		assertEquals(true, ClientUtils.stringToParam("true", "java.lang.Boolean"));
		assertEquals('i', ClientUtils.stringToParam("i", "char"));
		assertEquals('i', ClientUtils.stringToParam("i", "java.lang.Character"));
		assertEquals('\0', ClientUtils.stringToParam("", "char"));
		assertEquals((byte) 1, ClientUtils.stringToParam("1", "byte"));
		assertEquals((byte) 2, ClientUtils.stringToParam("2", "java.lang.Byte"));
		assertEquals((short) 1, ClientUtils.stringToParam("1", "short"));
		assertEquals((short) 2, ClientUtils.stringToParam("2", "java.lang.Short"));
		assertEquals((int) 1, ClientUtils.stringToParam("1", "int"));
		assertEquals((int) 2, ClientUtils.stringToParam("2", "java.lang.Integer"));
		assertEquals((long) 1, ClientUtils.stringToParam("1", "long"));
		assertEquals((long) 2, ClientUtils.stringToParam("2", "java.lang.Long"));
		assertEquals((float) 1, ClientUtils.stringToParam("1", "float"));
		assertEquals((float) 2, ClientUtils.stringToParam("2", "java.lang.Float"));
		assertEquals((double) 1, ClientUtils.stringToParam("1", "double"));
		assertEquals((double) 2, ClientUtils.stringToParam("2", "java.lang.Double"));
		assertEquals("2", ClientUtils.stringToParam("2", "java.lang.String"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPrivateConstructor() {
		ClientUtils.stringToParam("2", PrivateConstructor.class.getName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoStringConstructor() {
		ClientUtils.stringToParam("2", NoStringConstructor.class.getName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorThrows() {
		ClientUtils.stringToParam("2", ConstructorThrows.class.getName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnknownClass() {
		ClientUtils.stringToParam("2", "unknown-class");
	}

	@Test
	public void testStringConstructor() {
		String str = "fpoewjfpewfjw";
		StringConstructor obj = (StringConstructor) ClientUtils.stringToParam(str, StringConstructor.class.getName());
		assertEquals(str, obj.val);
	}

	@Test
	public void testValueToString() {
		assertEquals("null", ClientUtils.valueToString(null));
		assertEquals("3", ClientUtils.valueToString(3));

		assertEquals("[true]", ClientUtils.valueToString(new boolean[] { true }));
		assertEquals("[i]", ClientUtils.valueToString(new char[] { 'i' }));
		assertEquals("[1]", ClientUtils.valueToString(new byte[] { 1 }));
		assertEquals("[2]", ClientUtils.valueToString(new short[] { 2 }));
		assertEquals("[3]", ClientUtils.valueToString(new int[] { 3 }));
		assertEquals("[4]", ClientUtils.valueToString(new long[] { 4L }));
		assertEquals("[5.0]", ClientUtils.valueToString(new float[] { 5 }));
		assertEquals("[6.0]", ClientUtils.valueToString(new double[] { 6 }));
		String str = "fpowjf";
		assertEquals("[" + str + "]", ClientUtils.valueToString(new String[] { str }));
	}

	@Test
	public void testDisplayType() {
		assertNull(ClientUtils.displayType(null, null));
		assertEquals("array of unknown", ClientUtils.displayType("[J", null));
		assertEquals("array of boolean", ClientUtils.displayType("[J", new boolean[0]));
		assertEquals("array of byte", ClientUtils.displayType("[J", new byte[0]));
		assertEquals("array of char", ClientUtils.displayType("[J", new char[0]));
		assertEquals("array of short", ClientUtils.displayType("[J", new short[0]));
		assertEquals("array of int", ClientUtils.displayType("[J", new int[0]));
		assertEquals("array of long", ClientUtils.displayType("[J", new long[0]));
		assertEquals("array of float", ClientUtils.displayType("[J", new float[0]));
		assertEquals("array of double", ClientUtils.displayType("[J", new double[0]));
		assertEquals("array of unknown", ClientUtils.displayType("[J", new Object[0]));
		assertEquals("String", ClientUtils.displayType("[Ljava.lang.String]", new String[0]));
		assertEquals("String", ClientUtils.displayType("java.lang.String", new String[0]));
		assertEquals("String", ClientUtils.displayType("[Ljavax.management.openmbean.String]", new String[0]));
	}

	@Test
	public void testCoverage() {
		new ClientUtils();
	}

	public static class PrivateConstructor {
		private PrivateConstructor() {
		}
	}

	public static class NoStringConstructor {
		public NoStringConstructor() {
		}
	}

	public static class ConstructorThrows {
		public ConstructorThrows(String str) {
			throw new RuntimeException("expected");
		}
	}

	public static class StringConstructor {
		String val;

		public StringConstructor(String val) {
			this.val = val;
		}
	}
}
