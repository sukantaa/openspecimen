package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.ListQueryAuditLogsCriteria;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogDetail;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogSummary;
import com.krishagni.catissueplus.core.de.services.QueryService;

@Controller
@RequestMapping("/query-audit-logs")
public class QueryAuditLogsController {

	@Autowired
	private QueryService querySvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<QueryAuditLogSummary> getAuditLogs(
		@RequestParam(value = "queryId", required = false)
		Long queryId,
			
		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,
			
		@RequestParam(value = "maxResults", required = false, defaultValue = "25")
		int maxResults) {
		
		ListQueryAuditLogsCriteria crit = new ListQueryAuditLogsCriteria()
			.queryId(queryId)
			.startAt(startAt)
			.maxResults(maxResults);
		return response(querySvc.getAuditLogs(request(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getAuditLogsCount(
		@RequestParam(value = "queryId", required = false)
		Long queryId) {

		ListQueryAuditLogsCriteria crit = new ListQueryAuditLogsCriteria().queryId(queryId);
		Long count = response(querySvc.getAuditLogsCount(request(crit)));
		return Collections.singletonMap("count", count);
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public QueryAuditLogDetail getAuditLog(@PathVariable Long id) {
		return response(querySvc.getAuditLog(request(id)));
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}	
}
