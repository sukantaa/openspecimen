
package com.krishagni.catissueplus.core.auth.services;

import java.util.List;

import com.krishagni.auth.events.AuthDomainDetail;
import com.krishagni.auth.events.AuthDomainSummary;
import com.krishagni.auth.events.ListAuthDomainCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface AuthDomainWrapperService {
	ResponseEvent<List<AuthDomainSummary>> getDomains(RequestEvent<ListAuthDomainCriteria> req);
	
	ResponseEvent<AuthDomainDetail> registerDomain(RequestEvent<AuthDomainDetail> req);
	
	ResponseEvent<AuthDomainDetail> updateDomain(RequestEvent<AuthDomainDetail> req);
}
