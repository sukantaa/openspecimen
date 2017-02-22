
package com.krishagni.catissueplus.core.auth.services.impl;

import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;

import com.krishagni.auth.domain.AuthDomain;
import com.krishagni.auth.domain.AuthToken;
import com.krishagni.auth.events.LoginDetail;
import com.krishagni.auth.events.TokenDetail;
import com.krishagni.auth.services.impl.UserAuthenticationServiceImpl;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.auth.services.UserAuthServiceWrapper;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.commons.domain.IUser;
import com.krishagni.commons.errors.AppException;

public class UserAuthServiceWrapperImpl extends UserAuthenticationServiceImpl implements UserAuthServiceWrapper {
	private static final String DEFAULT_AUTH_DOMAIN = "openspecimen";

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> authenticateUser(RequestEvent<LoginDetail> req) {
		return Utility.invokeFn(super::authenticateUser, req, true);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<AuthToken> validateToken(RequestEvent<TokenDetail> req) {
		return Utility.invokeFn(super::validateToken, req);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<UserSummary> getCurrentLoggedInUser() {
		try {
			User user = (User)super.getLoggedInUser();
			return ResponseEvent.response(UserSummary.from(user));
		} catch (AppException ae) {
			return ResponseEvent.fromAppException(ae);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<String> removeToken(RequestEvent<String> req) {
		try {
			super.removeToken(req.getPayload());
			return ResponseEvent.response("Success");
		} catch (AppException ae) {
			return ResponseEvent.fromAppException(ae);
		}
	}
	
	@Scheduled(cron="0 0 12 ? * *")
	@PlusTransactional
	public void deleteInactiveAuthTokens() {
		super.purgeInactiveTokens();
	}
	
	public String generateToken(User user, LoginDetail loginDetail) {
		return super.generateToken(user, loginDetail);
	}

	@Override
	public AuthDomain getAuthDomain(IUser user) {
		return ((User) user).getAuthDomain();
	}


	@Override
	public IUser getUser(String username, String domain) {
		return daoFactory.getUserDao().getUser(username, domain);
	}

	@Override
	public IUser getUserByEmailAddress(String emailAddress) {
		return daoFactory.getUserDao().getUserByEmailAddress(emailAddress);
	}

	@Override
	public String getDefaultAuthDomain() {
		return DEFAULT_AUTH_DOMAIN;
	}
}
