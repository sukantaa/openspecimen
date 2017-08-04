package com.krishagni.catissueplus.rest.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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

import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.ConfigPrintRuleDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.repository.ConfigPrintRuleListCriteria;
import com.krishagni.catissueplus.core.common.service.ConfigPrintRuleService;

@Controller
@RequestMapping("/print-rules")
public class ConfigPrintRuleController {
	@Autowired
	private ConfigPrintRuleService configPrintRuleSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ConfigPrintRuleDetail> getConfigPrintRules(
			@RequestParam(value = "objectType", required = false)
			String objectType,

			@RequestParam(value = "cpTitle", required = false)
			String cpTitle,

			@RequestParam(value = "instituteName", required = false)
			String instituteName,

			@RequestParam(value = "userName", required = false)
			String userName,

			@RequestParam(value = "activityStatus", required = false)
			String activityStatus,

			@RequestParam(value = "startAt", required = false, defaultValue = "0")
			int startAt,

			@RequestParam(value = "maxResults", required = false, defaultValue = "100")
			int maxResults) {

		ConfigPrintRuleListCriteria crit = new ConfigPrintRuleListCriteria()
			.objectType(objectType)
			.cpTitle(cpTitle)
			.instituteName(instituteName)
			.userName(userName)
			.activityStatus(activityStatus)
			.startAt(startAt)
			.maxResults(maxResults);

		return response(configPrintRuleSvc.getConfigPrintRules(request(crit)));
	}

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

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<ConfigPrintRuleDetail>  deleteConfigPrintRule(
		@PathVariable
		Long id,

		@RequestParam(value="close", required=false, defaultValue="false")
		boolean close) {

		BulkDeleteEntityOp op = new BulkDeleteEntityOp();
		op.setIds(Collections.singleton(id));
		op.setClose(close);

		return response(configPrintRuleSvc.deleteConfigPrintRules(request(op)));
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<ConfigPrintRuleDetail>  deleteConfigPrintRules(
		@RequestParam(value = "id")
		Long[] ids,

		@RequestParam(value="close", required=false, defaultValue="false")
		boolean close) {

		BulkDeleteEntityOp op = new BulkDeleteEntityOp();
		op.setIds(new HashSet<>(Arrays.asList(ids)));
		op.setClose(close);

		return response(configPrintRuleSvc.deleteConfigPrintRules(request(op)));
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
