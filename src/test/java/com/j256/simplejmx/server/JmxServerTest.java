package com.j256.simplejmx.server;

import static org.junit.Assert.fail;

import javax.management.JMException;

import org.junit.Test;

public class JmxServerTest {

	private static final int DEFAULT_PORT = 5256;

	@Test
	public void testJmxServer() throws Exception {
		JmxServer jmxServer = new JmxServer(DEFAULT_PORT);
		try {
			jmxServer.start();
		} finally {
			jmxServer.stop();
		}
	}

	@Test
	public void testJmxServerStartStopStart() throws Exception {
		JmxServer jmxServer = new JmxServer(DEFAULT_PORT);
		try {
			jmxServer.start();
			jmxServer.stop();
			jmxServer.start();
		} finally {
			jmxServer.stop();
		}
	}

	@Test
	public void testJmxServerInt() throws Exception {
		JmxServer jmxServer = new JmxServer();
		try {
			jmxServer.setPort(DEFAULT_PORT);
			jmxServer.start();
		} finally {
			jmxServer.stop();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testJmxServerStartNoPort() throws Exception {
		JmxServer jmxServer = new JmxServer();
		try {
			jmxServer.start();
			fail("Should not have gotten here");
		} finally {
			jmxServer.stop();
		}
	}

	@Test
	public void testRegister() throws Exception {
		JmxServer jmxServer = new JmxServer(DEFAULT_PORT);
		TestObject obj = new TestObject();
		try {
			jmxServer.start();
			jmxServer.register(obj);
			jmxServer.unregister(obj);
		} finally {
			jmxServer.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testDoubleRegister() throws Exception {
		JmxServer jmxServer = new JmxServer(DEFAULT_PORT);
		TestObject obj = new TestObject();
		try {
			jmxServer.start();
			jmxServer.register(obj);
			jmxServer.register(obj);
		} finally {
			jmxServer.stop();
		}
	}

	@JmxResource(description = "Test object", domainName = "j256", objectName = "testObject")
	protected static class TestObject {

		private int foo = 1;

		@JmxAttribute(description = "A value")
		public int getFoo() {
			return foo;
		}

		@JmxAttribute(description = "A value")
		public void setFoo(int foo) {
			this.foo = foo;
		}
	}
}
