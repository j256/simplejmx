package com.j256.simplejmx.server;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;
import com.j256.simplejmx.common.IoUtils;

public class JmxUsernamePasswordAuthenticatorTest {

	private static final int DEFAULT_PORT = 5256;

	@Test
	public void testAuthentication() throws Exception {
		InetAddress serverAddress = InetAddress.getByName("127.0.0.1");
		JmxServer server = new JmxServer(serverAddress, DEFAULT_PORT);
		JmxClient client = null;
		try {
			JmxUsernamePasswordAuthenticator authenticator = new JmxUsernamePasswordAuthenticator();
			Map<String, String> authMap = new HashMap<String, String>();
			String username = "hello";
			String password = "there";
			authMap.put(username, password);
			authenticator.setAuthMap(authMap);
			server.setAuthenticator(authenticator);
			server.start();
			Thread.sleep(10000000);
			// client = new JmxClient(serverAddress, DEFAULT_PORT, username, password);
		} finally {
			IoUtils.closeQuietly(client);
			IoUtils.closeQuietly(server);
		}
	}
}
