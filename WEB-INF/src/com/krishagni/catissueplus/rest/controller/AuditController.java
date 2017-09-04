package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.AuditQueryCriteria;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/audit")
public class AuditController {

	@Autowired
	private AuditService auditService;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public AuditDetail getAuditInfo(
		@RequestParam(value = "objectName")
		String objectName,

		@RequestParam(value = "objectId")
		Long objectId) {
		AuditQueryCriteria criteriaObj = new AuditQueryCriteria();
		criteriaObj.setObjectName(objectName);
		criteriaObj.setObjectId(objectId);

		List<AuditQueryCriteria> criteria = Collections.singletonList(criteriaObj);
		ResponseEvent<List<AuditDetail>> resp = auditService.getAuditDetail(new RequestEvent<>(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload().iterator().next();
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<AuditDetail> getAuditInfo(@RequestBody List<AuditQueryCriteria> criteria) {
		ResponseEvent<List<AuditDetail>> resp = auditService.getAuditDetail(new RequestEvent<>(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
