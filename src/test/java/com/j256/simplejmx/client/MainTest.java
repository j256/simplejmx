package com.j256.simplejmx.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;

import javax.management.JMException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.j256.simplejmx.common.IoUtils;
import com.j256.simplejmx.server.JmxServer;

public class MainTest {

	private static PrintStream outSave = System.out;
	private static PrintStream errSave = System.err;

	@BeforeClass
	public static void beforeClass() throws Exception {
		PrintStream ignored = new PrintStream(new File("target/ignored"));
		// we do this to stop spitting out error output
		System.setOut(ignored);
		System.setErr(ignored);
	}

	@AfterClass
	public static void afterClass() {
		System.setOut(outSave);
		System.setErr(errSave);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoArgs() throws Exception {
		System.setErr(new PrintStream(new File("target/ignored")));
		new Main().doMain(new String[0], true);
	}

	@Test
	public void testStatic() throws Exception {
		System.setErr(new PrintStream(new File("target/ignored")));
		System.setOut(new PrintStream(new File("target/ignored")));
		Main.main(new String[0]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidHostNamePortFormat() throws Exception {
		System.setErr(new PrintStream(new File("target/ignored")));
		new Main().doMain(new String[] { "foo" }, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidPortNumber() throws Exception {
		System.setErr(new PrintStream(new File("target/ignored")));
		new Main().doMain(new String[] { "localhost:notport" }, true);
	}

	@Test(expected = JMException.class)
	public void testConnectToNoServer() throws Exception {
		System.setErr(new PrintStream(new File("target/ignored")));
		new Main().doMain(new String[] { "localhost:18080" }, true);
	}

	@Test(expected = JMException.class)
	public void testConnectToNoServerBatchFile() throws Exception {
		System.setErr(new PrintStream(new File("target/ignored")));
		new Main().doMain(new String[] { "localhost:18080", "batch-file" }, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooManyArgs() throws Exception {
		System.setErr(new PrintStream(new File("target/ignored")));
		new Main().doMain(new String[] { "localhost:18080", "2", "3" }, true);
	}

	@Test
	public void testCoverage() throws Exception {
		new Main().doMain(new String[] { "localhost:18080", "2", "3" }, false);
		new Main().doMain(new String[] { "localhost:18080:2" }, false);
		new Main().doMain(new String[] { "localhost:foo" }, false);
		new Main().doMain(new String[] { "--usage" }, false);
		new Main().doMain(new String[] { "--help" }, false);
	}

	@Test
	public void testConnectToServer() throws Exception {
		int port = 8000;
		InetAddress address = InetAddress.getByName("127.0.0.1");
		JmxServer server = new JmxServer(address, port);
		try {
			server.start();

			StringBuilder sb = new StringBuilder();
			sb.append("quit\n");
			System.setIn(new ByteArrayInputStream(sb.toString().getBytes()));
			new Main().doMain(new String[] { address.getHostAddress() + ":" + port }, true);

			// now connect as jmx url
			System.setIn(new ByteArrayInputStream(sb.toString().getBytes()));
			new Main().doMain(new String[] { JmxClient.generalJmxUrlForHostNamePort(address.getHostAddress(), port) },
					true);
		} finally {
			IoUtils.closeQuietly(server);
		}
	}
}
