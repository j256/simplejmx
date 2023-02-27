package com.j256.simplejmx.server;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;

/**
 * Username/password authenticator. Thanks much to wodencafe for the basis of the code.
 * 
 * @author wodencafe
 */
public class JmxUsernamePasswordAuthenticator implements JMXAuthenticator {

	private Map<String, String> authMap = Collections.emptyMap();

	@Override
	public Subject authenticate(Object credentials) {
		if (!(credentials instanceof String[])) {
			throw new SecurityException("Was expected credentials String[2] object");
		}
		String[] usernamePassword = (String[]) credentials;
		if (usernamePassword.length != 2) {
			throw new SecurityException("Was expected credentials String[2] object");
		}

		String username = usernamePassword[0];
		String password = usernamePassword[1];

		String expectedPassword = authMap.get(username);
		if (expectedPassword == null || !expectedPassword.equals(password)) {
			throw new SecurityException("Unknown username/password combination");
		}

		Set<Principal> principals = Collections.singleton(new JMXPrincipal(username));
		return new Subject(true /* readOnly */, principals, Collections.emptySet(), Collections.emptySet());
	}

	public void setAuthMap(Map<String, String> authMap) {
		this.authMap = authMap;
	}
}
