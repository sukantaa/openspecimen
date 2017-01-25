package com.krishagni.catissueplus.core.auth.services;

import java.util.Map;

import com.krishagni.auth.domain.AuthToken;
import com.krishagni.auth.events.LoginDetail;
import com.krishagni.auth.events.TokenDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;

public interface UserAuthServiceWrapper {
	ResponseEvent<Map<String, Object>> authenticateUser(RequestEvent<LoginDetail> req);
	
	ResponseEvent<AuthToken> validateToken(RequestEvent<TokenDetail> req);
	
	ResponseEvent<UserSummary> getCurrentLoggedInUser();
	
	ResponseEvent<String> removeToken(RequestEvent<String> req);
}