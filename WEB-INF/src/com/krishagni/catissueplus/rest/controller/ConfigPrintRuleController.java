package com.krishagni.catissueplus.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
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
		return response(configPrintRuleSvc.createConfigPrintRule(request(detail)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public ConfigPrintRuleDetail updateConfigPrintRule(@PathVariable Long id, @RequestBody ConfigPrintRuleDetail detail) {
		detail.setId(id);
		return response(configPrintRuleSvc.updateConfigPrintRule(request(detail)));
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
