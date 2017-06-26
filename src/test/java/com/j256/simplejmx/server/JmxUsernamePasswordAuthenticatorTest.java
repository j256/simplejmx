package com.j256.simplejmx.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;

import org.junit.Test;

public class JmxUsernamePasswordAuthenticatorTest {

	@Test
	public void testStuff() {
		JmxUsernamePasswordAuthenticator auth = new JmxUsernamePasswordAuthenticator();
		Map<String, String> authMap = new HashMap<String, String>();
		String user = "me";
		String pwd = "_secret";
		authMap.put(user, pwd);
		auth.setAuthMap(authMap);

		String[] strs = new String[] { user, pwd };
		Subject subject = auth.authenticate(strs);
		assertNotNull(subject);
		assertTrue(subject.isReadOnly());
		assertNotNull(subject.getPrincipals());
		assertEquals(1, subject.getPrincipals().size());
	}

	@Test(expected = SecurityException.class)
	public void testWrongType() {
		JmxUsernamePasswordAuthenticator auth = new JmxUsernamePasswordAuthenticator();
		auth.authenticate(new Object());
	}

	@Test(expected = SecurityException.class)
	public void testWrongNumStrings() {
		JmxUsernamePasswordAuthenticator auth = new JmxUsernamePasswordAuthenticator();
		auth.authenticate(new String[0]);
	}

	@Test(expected = SecurityException.class)
	public void testUnknownUser() {
		JmxUsernamePasswordAuthenticator auth = new JmxUsernamePasswordAuthenticator();
		Map<String, String> authMap = new HashMap<String, String>();
		String user = "me";
		String pwd = "_secret";
		authMap.put(user, pwd);
		auth.setAuthMap(authMap);
		auth.authenticate(new String[] { user, "badpasword" });
	}

	@Test(expected = SecurityException.class)
	public void testBadPassword() {
		JmxUsernamePasswordAuthenticator auth = new JmxUsernamePasswordAuthenticator();
		auth.authenticate(new String[] { "unknownUser", "badpasword" });
	}
}
