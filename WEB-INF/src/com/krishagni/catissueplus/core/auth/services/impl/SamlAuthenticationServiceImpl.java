package com.krishagni.catissueplus.core.auth.services.impl;

import java.util.Map;

import com.krishagni.auth.domain.Authenticator;
import com.krishagni.auth.services.impl.SamlAuthenticator;

public class SamlAuthenticationServiceImpl implements Authenticator {
	private SamlAuthenticator samlAuthenticator;

	public SamlAuthenticationServiceImpl() {

	}

	public SamlAuthenticationServiceImpl(Map<String, String> props) {
		samlAuthenticator = new SamlAuthenticator(props);
	}

	@Override
	public void authenticate(String username, String password) {
		samlAuthenticator.authenticate(username, password);
	}
}
