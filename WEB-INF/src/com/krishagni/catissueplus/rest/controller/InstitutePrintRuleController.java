package com.krishagni.catissueplus.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.InstitutePrintRuleDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.InstitutePrintRuleService;

@Controller
@RequestMapping("/print-rules")
public class InstitutePrintRuleController {
	@Autowired
	private InstitutePrintRuleService institutePrintRuleSvc;

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public InstitutePrintRuleDetail createInstitutePrintRule(@RequestBody InstitutePrintRuleDetail detail) {
		RequestEvent<InstitutePrintRuleDetail> req = new RequestEvent<InstitutePrintRuleDetail>(detail);
		ResponseEvent<InstitutePrintRuleDetail> resp = institutePrintRuleSvc.createInstitutePrintRule(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}
}
