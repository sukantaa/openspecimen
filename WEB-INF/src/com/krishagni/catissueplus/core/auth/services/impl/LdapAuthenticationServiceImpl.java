package com.krishagni.catissueplus.core.auth.services.impl;

import java.util.Map;

import com.krishagni.auth.domain.Authenticator;
import com.krishagni.auth.services.impl.LdapAuthenticator;

public class LdapAuthenticationServiceImpl implements Authenticator {
	private LdapAuthenticator ldapAuthenticator;

	public LdapAuthenticationServiceImpl() {

	}

	public LdapAuthenticationServiceImpl(Map<String, String> props) {
		ldapAuthenticator = new LdapAuthenticator(props);
	}

	@Override
	public void authenticate(String username, String password) {
		ldapAuthenticator.authenticate(username, password);
	}
}
