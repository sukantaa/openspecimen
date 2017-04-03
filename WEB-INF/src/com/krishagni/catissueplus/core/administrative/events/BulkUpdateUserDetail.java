package com.krishagni.catissueplus.core.administrative.events;

import java.util.List;

public class BulkUpdateUserDetail {
	private UserDetail detail;

	private List<Long> userIds;

	private List<String> emailAddresses;

	public UserDetail getDetail() {
		return detail;
	}

	public void setDetail(UserDetail detail) {
		this.detail = detail;
	}

	public List<Long> getUserIds() {
		return userIds;
	}

	public void setUserIds(List<Long> userIds) {
		this.userIds = userIds;
	}

	public List<String> getEmailAddresses() {
		return emailAddresses;
	}

	public void setEmailAddresses(List<String> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}
}
