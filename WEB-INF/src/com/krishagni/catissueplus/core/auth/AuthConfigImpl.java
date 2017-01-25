package com.krishagni.catissueplus.core.auth;

import com.krishagni.auth.AuthConfig;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class AuthConfigImpl implements AuthConfig {
	private static final String MODULE = "auth";
	
	private static final String FAILED_ATTEMPTS   = "allowed_failed_logins";

	private static final String VERIFY_TOKEN_IP   = "verify_token_ip_address";

	private static final String TOKEN_INACTIVE_INTERVAL = "token_inactive_interval";

	private static final String SAML_ENABLE = "saml_enable";

	private static final String OS_AUTH_TOKEN = "osAuthToken";

	private static AuthConfigImpl instance = null;

	private AuthConfigImpl() {

	}
	
	public static AuthConfigImpl getInstance() {
		if (instance == null) {
			instance = new AuthConfigImpl();
		}
		
		return instance;
	}
	
	@Override
	public int getAllowedFailedLoginAttempts() {
		return ConfigUtil.getInstance().getIntSetting(MODULE, FAILED_ATTEMPTS, 5);
	}

	@Override
	public boolean isTokenIpVerified() {
		return ConfigUtil.getInstance().getBoolSetting(MODULE, VERIFY_TOKEN_IP, false);
	}

	@Override
	public int getTokenInactiveIntervalInMinutes() {
		return ConfigUtil.getInstance().getIntSetting(MODULE, TOKEN_INACTIVE_INTERVAL, 60);
	}

	@Override
	public String getAppUrl() {
		return ConfigUtil.getInstance().getAppUrl();
	}

	@Override
	public boolean isSamlEnabled() {
		return ConfigUtil.getInstance().getBoolSetting(MODULE, SAML_ENABLE, false);
	}

	@Override
	public String getCookieName() {
		return OS_AUTH_TOKEN;
	}
}
