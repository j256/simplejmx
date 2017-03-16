package com.j256.simplejmx.server;

import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class NetTest {

	private static final int SERVER_PORT = 8080;
	private static final InetAddress LOCAL_ADDRESS;

	private static Thread thread;
	private static volatile ServerSocket serverSocket;

	static {
		try {
			LOCAL_ADDRESS = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		serverSocket = new ServerSocket();
		thread = new Thread(new OurServer());
		thread.start();
		Thread.sleep(1000);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (serverSocket != null) {
			serverSocket.close();
			serverSocket = null;
		}
		if (thread != null) {
			thread.join(100);
			thread = null;
		}
	}

	@Test
	public void testJmxServer() throws Exception {
		Socket socket = new Socket(LOCAL_ADDRESS, SERVER_PORT);
		byte[] bytes = new byte[100];
		int numBytes = socket.getInputStream().read(bytes);
		assertTrue(numBytes > 0);
		System.out.println("Got " + new String(bytes, 0, numBytes));
	}

	private static class OurServer implements Runnable {
		@Override
		public void run() {
			try {
				doServer();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void doServer() throws Exception {
			serverSocket.bind(new InetSocketAddress(LOCAL_ADDRESS, SERVER_PORT));
			Socket socket = null;
			OutputStream os = null;
			try {
				socket = serverSocket.accept();
				os = socket.getOutputStream();
				os.write(new byte[] { 'h', 'e', 'l', 'l', 'o' });
				os.flush();
			} finally {
				if (os != null) {
					os.close();
				}
				if (socket != null) {
					socket.close();
				}
				serverSocket.close();
			}
		}
	}
}
