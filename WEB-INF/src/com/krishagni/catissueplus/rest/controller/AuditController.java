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
import com.krishagni.catissueplus.core.audit.events.RevisionDetail;
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

		AuditQueryCriteria criteria = new AuditQueryCriteria();
		criteria.setObjectName(objectName);
		criteria.setObjectId(objectId);

		ResponseEvent<List<AuditDetail>> resp = auditService.getAuditDetail(new RequestEvent<>(Collections.singletonList(criteria)));
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

	@RequestMapping(method = RequestMethod.GET, value="/revisions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<RevisionDetail> getRevisions(
		@RequestParam("objectName")
		String objectName,

		@RequestParam("objectId")
		Long objectId) {

		AuditQueryCriteria criteria = new AuditQueryCriteria();
		criteria.setObjectName(objectName);
		criteria.setObjectId(objectId);

		ResponseEvent<List<RevisionDetail>> resp = auditService.getRevisions(new RequestEvent<>(Collections.singletonList(criteria)));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value="/revisions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<RevisionDetail> getRevisions(@RequestBody List<AuditQueryCriteria> criteria) {
		ResponseEvent<List<RevisionDetail>> resp = auditService.getRevisions(new RequestEvent<>(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
