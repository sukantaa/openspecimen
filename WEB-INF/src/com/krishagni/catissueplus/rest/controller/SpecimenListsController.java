package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
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

import com.krishagni.catissueplus.core.biospecimen.events.ShareSpecimenListOp;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateListSpecimensOp;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenListService;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;

@Controller
@RequestMapping("/specimen-lists")
public class SpecimenListsController {
	
	@Autowired
	private HttpServletRequest httpServletRequest;

	@Autowired
	private SpecimenListService specimenListSvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenListSummary> getSpecimenLists(
		@RequestParam(value = "name", required = false)
		String name,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats) {

		SpecimenListsCriteria crit = new SpecimenListsCriteria()
			.query(name)
			.includeStat(includeStats)
			.startAt(startAt < 0 ? 0 : startAt)
			.maxResults(maxResults <=0 ? 100 : maxResults);

		ResponseEvent<List<SpecimenListSummary>> resp = specimenListSvc.getSpecimenLists(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getSpecimenListsCount(@RequestParam(value = "name", required = false) String name) {
		SpecimenListsCriteria crit = new SpecimenListsCriteria().query(name);
		ResponseEvent<Long> resp = specimenListSvc.getSpecimenListsCount(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("count", resp.getPayload());
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListDetail getSpecimenList(@PathVariable("listId") Long listId) {
		EntityQueryCriteria crit = new EntityQueryCriteria(listId);
		ResponseEvent<SpecimenListDetail> resp = specimenListSvc.getSpecimenList(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
		
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListDetail createSpecimenList(@RequestBody SpecimenListDetail details) {
		ResponseEvent<SpecimenListDetail> resp = specimenListSvc.createSpecimenList(getRequest(details));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListDetail updateSpecimenList(@PathVariable Long listId, @RequestBody SpecimenListDetail details) {
		details.setId(listId);

		ResponseEvent<SpecimenListDetail> resp = specimenListSvc.updateSpecimenList(getRequest(details));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PATCH, value="/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListDetail patchSpecimenList(@PathVariable Long listId, @RequestBody SpecimenListDetail details) {
		details.setId(listId);
		ResponseEvent<SpecimenListDetail> resp = specimenListSvc.patchSpecimenList(getRequest(details));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListDetail deleteSpecimenList(@PathVariable Long listId) {
		ResponseEvent<SpecimenListDetail> resp = specimenListSvc.deleteSpecimenList(getRequest(listId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/{listId}/specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenInfo> getListSpecimens(
		@PathVariable("listId")
		Long listId,

		@RequestParam(value = "label", required = false)
		String label,

		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "ppid", required = false)
		String ppid,

		@RequestParam(value = "lineage", required = false)
		String lineage,

		@RequestParam(value = "type", required = false)
		String type,

		@RequestParam(value = "anatomicSite", required = false)
		String anatomicSite,

		@RequestParam(value = "container", required = false)
		String container,

		@RequestParam(value = "available", required = false, defaultValue = "false")
		boolean available,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "50")
		int maxResults,

		@RequestParam(value = "includeListCount", required = false, defaultValue = "false")
		boolean includeListCount) {

		SpecimenListCriteria criteria = new SpecimenListCriteria()
			.specimenListId(listId)
			.labels(StringUtils.isNotBlank(label) ? Collections.singletonList(label) : null)
			.cpId(cpId)
			.ppid(ppid)
			.lineages(StringUtils.isNotBlank(lineage) ? new String[] {lineage} : null)
			.type(type)
			.anatomicSite(anatomicSite)
			.container(container)
			.exactMatch(false)
			.startAt(startAt)
			.maxResults(maxResults)
			.available(available)
			.includeStat(includeListCount)
			.limitItems(true);

		ResponseEvent<List<SpecimenInfo>> resp = specimenListSvc.getListSpecimens(getRequest(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/{listId}/specimens-sorted-by-rel")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenInfo> getListSpecimens(@PathVariable("listId") Long listId) {
		RequestEvent<EntityQueryCriteria> req = new RequestEvent<>(new EntityQueryCriteria(listId));
		ResponseEvent<List<SpecimenInfo>> resp = specimenListSvc.getListSpecimensSortedByRel(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{listId}/specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> updateListSpecimens(
		@PathVariable("listId")
		Long listId,

		@RequestParam(value = "operation", required = false, defaultValue = "ADD")
		String operation,

		@RequestBody
		List<Long> specimenIds) {
		
		UpdateListSpecimensOp opDetail = new UpdateListSpecimensOp();
		opDetail.setListId(listId);
		opDetail.setSpecimens(specimenIds);
		opDetail.setOp(UpdateListSpecimensOp.Operation.valueOf(operation));

		ResponseEvent<Integer> resp = specimenListSvc.updateListSpecimens(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.POST, value="/{listId}/add-child-specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> addChildSpecimens(@PathVariable("listId") Long listId) {
		ResponseEvent<Boolean> resp = specimenListSvc.addChildSpecimens(getRequest(listId));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("status", resp.getPayload().toString());
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/{listId}/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserSummary> shareSpecimenList(
		@PathVariable("listId")
		Long listId,

		@RequestParam(value = "operation", required = false, defaultValue = "UPDATE")
		String operation,

		@RequestBody
		List<Long> userIds) {

		ShareSpecimenListOp opDetail = new ShareSpecimenListOp();
		opDetail.setListId(listId);
		opDetail.setOp(com.krishagni.catissueplus.core.biospecimen.events.ShareSpecimenListOp.Operation.valueOf(operation));
		opDetail.setUserIds(userIds);
		
		ResponseEvent<List<UserSummary>> resp = specimenListSvc.shareSpecimenList(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}/csv-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void exportList(@PathVariable("id") Long listId, HttpServletResponse httpResp) {
		EntityQueryCriteria crit = new EntityQueryCriteria(listId);
		ResponseEvent<ExportedFileDetail> resp = specimenListSvc.exportSpecimenList(getRequest(crit));
		resp.throwErrorIfUnsuccessful();

		ExportedFileDetail fileDetail = resp.getPayload();
		try {
			Utility.sendToClient(httpResp, fileDetail.getName() + ".csv", "application/csv", fileDetail.getFile());
		} finally {
			fileDetail.getFile().delete();
		}
	}
		
	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}
 }
