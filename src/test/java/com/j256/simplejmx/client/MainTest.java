package com.j256.simplejmx.client;

import java.io.ByteArrayInputStream;

import javax.management.JMException;

import org.junit.Test;

import com.j256.simplejmx.server.JmxServer;

public class MainTest {

	@Test(expected = IllegalArgumentException.class)
	public void testNoArgs() throws Exception {
		new Main().doMain(new String[0], true);
	}

	@Test
	public void testStatic() throws Exception {
		Main.main(new String[0]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidHostNamePortFormat() throws Exception {
		new Main().doMain(new String[] { "foo" }, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidPortNumber() throws Exception {
		new Main().doMain(new String[] { "localhost:notport" }, true);
	}

	@Test(expected = JMException.class)
	public void testConnectToNoServer() throws Exception {
		new Main().doMain(new String[] { "localhost:18080" }, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooManyArgs() throws Exception {
		new Main().doMain(new String[] { "localhost:18080", "2", "3" }, true);
	}

	@Test
	public void testConnectToServer() throws Exception {
		int port = 8000;
		JmxServer server = new JmxServer(port);
		try {
			server.start();

			StringBuilder sb = new StringBuilder();
			sb.append("quit\n");
			System.setIn(new ByteArrayInputStream(sb.toString().getBytes()));
			new Main().doMain(new String[] { "localhost:" + port }, true);

			// now connect as jmx url
			System.setIn(new ByteArrayInputStream(sb.toString().getBytes()));
			new Main().doMain(new String[] { JmxClient.generalJmxUrlForHostNamePort("localhost", port) }, true);
		} finally {
			server.stop();
		}
	}
}
