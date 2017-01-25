
package com.krishagni.catissueplus.core.auth.services.impl;

import java.util.List;

import com.krishagni.auth.events.AuthDomainDetail;
import com.krishagni.auth.events.AuthDomainSummary;
import com.krishagni.auth.events.ListAuthDomainCriteria;
import com.krishagni.auth.services.AuthDomainService;
import com.krishagni.catissueplus.core.auth.services.AuthDomainWrapperService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.commons.errors.AppException;

public class AuthDomainWrapperServiceImpl implements AuthDomainWrapperService {

	private AuthDomainService authDomainService;

	public void setAuthDomainService(AuthDomainService authDomainService) {
		this.authDomainService = authDomainService;
	}

	@Override
	@PlusTransactional	
	public ResponseEvent<List<AuthDomainSummary>> getDomains(RequestEvent<ListAuthDomainCriteria> req) {
		try {
			return ResponseEvent.response(authDomainService.getDomains(req.getPayload()));
		} catch (AppException ae) {
			return ResponseEvent.serverError(ae);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<AuthDomainDetail> registerDomain(RequestEvent<AuthDomainDetail> req) {
		try {
			return ResponseEvent.response(authDomainService.registerDomain(req.getPayload()));
		} catch (AppException ae) {
			return ResponseEvent.serverError(ae);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<AuthDomainDetail> updateDomain(RequestEvent<AuthDomainDetail> req) {
		try {
			return ResponseEvent.response(authDomainService.updateDomain(req.getPayload()));
		} catch (AppException ae) {
			return ResponseEvent.serverError(ae);
		}
	}
}
