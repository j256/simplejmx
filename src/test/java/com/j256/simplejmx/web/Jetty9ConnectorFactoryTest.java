package com.j256.simplejmx.web;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

public class Jetty9ConnectorFactoryTest {

	@Test
	public void testStuff() throws UnknownHostException {
		JettyConnectorFactory factory = new Jetty9ConnectorFactory();
		factory.buildConnector(null, null, 0);
		factory.buildConnector(null, InetAddress.getLocalHost(), 0);
	}
}
