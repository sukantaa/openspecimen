package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserNotificationDetail;
import com.krishagni.catissueplus.core.common.repository.UserNotifsListCriteria;
import com.krishagni.catissueplus.core.common.service.NotificationService;
import com.krishagni.catissueplus.core.common.util.Utility;

@Controller
@RequestMapping("/user-notifications")
public class UserNotificationsController {

	@Autowired
	private NotificationService notificationSvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserNotificationDetail> getUserNotifications(
		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int start,

		@RequestParam(value = "maxResults", required = false, defaultValue = "10")
		int maxResults) {

		UserNotifsListCriteria crit = new UserNotifsListCriteria()
			.startAt(start)
			.maxResults(maxResults);

		ResponseEvent<List<UserNotificationDetail>> resp = notificationSvc.getUserNotifications(new RequestEvent<>(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getUnreadNotificationsCount() {
		ResponseEvent<Long> resp = notificationSvc.getUnreadNotificationsCount();
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/mark-as-read")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<UserNotificationDetail> readNotifications(@RequestBody List<UserNotificationDetail> notifs) {
		List<Long> ids = Utility.collect(notifs, "id");
		if (CollectionUtils.isEmpty(ids)) {
			return Collections.emptyList();
		}

		RequestEvent<List<Long>> req = new RequestEvent<>(ids);
		ResponseEvent<List<UserNotificationDetail>> resp = notificationSvc.markNotificationsAsRead(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
