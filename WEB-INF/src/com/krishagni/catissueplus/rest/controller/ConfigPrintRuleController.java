package com.krishagni.catissueplus.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.ConfigPrintRuleDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigPrintRuleService;

@Controller
@RequestMapping("/print-rules")
public class ConfigPrintRuleController {
	@Autowired
	private ConfigPrintRuleService configPrintRuleSvc;

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ConfigPrintRuleDetail createConfigPrintRule(@RequestBody ConfigPrintRuleDetail detail) {
		RequestEvent<ConfigPrintRuleDetail> req = new RequestEvent<ConfigPrintRuleDetail>(detail);
		ResponseEvent<ConfigPrintRuleDetail> resp = configPrintRuleSvc.createConfigPrintRule(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}
}
