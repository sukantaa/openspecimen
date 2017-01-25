
package com.krishagni.catissueplus.rest.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.auth.events.AuthDomainDetail;
import com.krishagni.auth.events.AuthDomainSummary;
import com.krishagni.auth.events.ListAuthDomainCriteria;
import com.krishagni.catissueplus.core.auth.services.AuthDomainWrapperService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/auth-domains")
public class AuthDomainController {

	@Autowired
	private AuthDomainWrapperService authDomainSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<AuthDomainSummary> getAuthDomains(
		@RequestParam(value = "maxResults", required = false, defaultValue = "1000")
		int maxResults) {

		return response(authDomainSvc.getDomains(request(new ListAuthDomainCriteria().maxResults(maxResults))));
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public AuthDomainDetail registerDomain(@RequestBody AuthDomainDetail domainDetail) {
		return response(authDomainSvc.registerDomain(request(domainDetail)));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public AuthDomainDetail updaterDomain(@PathVariable Long id, @RequestBody AuthDomainDetail domainDetail) {
		domainDetail.setId(id);
		return response(authDomainSvc.updateDomain(request(domainDetail)));
	}

	<T> RequestEvent<T> request(T input) {
		return new RequestEvent<>(input);
	}

	<R> R response(ResponseEvent<R> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}