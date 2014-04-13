package org.eclipse.jetty.server;

/**
 * Fake server connector class to get this to compile without Jetty 9 dependency.
 * 
 * @author graywatson
 */
public class ServerConnector extends AbstractConnector {

	public ServerConnector(Server server) {
		// noop
	}

	public void open() {
		// noop
	}

	public void close() {
		// noop
	}

	public int getLocalPort() {
		return 0;
	}

	public Object getConnection() {
		return null;
	}

	@Override
	protected void accept(int acceptorID) {
		// noop
	}
}
