package com.krishagni.catissueplus.core.common.domain;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

public class UserNotification extends BaseEntity {
	public enum Status {
		READ,
		UNREAD
	};

	private User user;
	
	private Notification notification;
	
	private Status status;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Notification getNotification() {
		return notification;
	}

	public void setNotification(Notification notification) {
		this.notification = notification;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
