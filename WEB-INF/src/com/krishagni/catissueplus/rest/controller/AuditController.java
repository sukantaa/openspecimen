package com.krishagni.catissueplus.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
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

		AuditQueryCriteria crit = new AuditQueryCriteria(objectName, objectId);
		ResponseEvent<AuditDetail> resp = auditService.getAuditDetail(new RequestEvent<>(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

}
