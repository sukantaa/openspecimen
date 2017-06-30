package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.domain.UserNotification;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.UserNotificationDao;
import com.krishagni.catissueplus.core.common.repository.UserNotifsListCriteria;

public class UserNotificationDaoImpl extends AbstractDao<UserNotification> implements UserNotificationDao {
	@Override
	public Class<?> getType() {
		return UserNotification.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<UserNotification> getUserNotifications(UserNotifsListCriteria crit) {
		return getUserNotificationsListCriteria(crit)
			.createAlias("un.notification", "n")
			.addOrder(Order.desc("n.creationTime"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();
	}

	@Override
	public Long getUnreadNotificationsCount(UserNotifsListCriteria crit) {
		Number count = (Number) getUserNotificationsListCriteria(crit)
			.add(Restrictions.eq("un.status", UserNotification.Status.UNREAD))
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	public int markUserNotificationsAsRead(Long userId, Date notifsBefore) {
		return getCurrentSession().getNamedQuery(MARK_AS_READ)
			.setParameter("userId", userId)
			.setParameter("notifsBefore", notifsBefore)
			.executeUpdate();
	}

	@Override
	public void saveOrUpdate(Notification notification) {
		getCurrentSession().saveOrUpdate(notification);
	}

	private Criteria getUserNotificationsListCriteria(UserNotifsListCriteria crit) {
		Criteria query = sessionFactory.getCurrentSession()
			.createCriteria(UserNotification.class, "un");

		addIdsCondition(query, crit);
		return addUserCondition(query, crit);
	}

	private Criteria addIdsCondition(Criteria query, UserNotifsListCriteria crit) {
		if (CollectionUtils.isEmpty(crit.ids())) {
			return query;
		}

		return query.add(Restrictions.in("un.id", crit.ids()));
	}

	private Criteria addUserCondition(Criteria query, UserNotifsListCriteria crit) {
		Long userId = crit.userId();
		if (userId == null) {
			return query;
		}

		return query.createAlias("un.user", "user").add(Restrictions.eq("user.id", userId));
	}

	private static final String FQN = UserNotification.class.getName();

	private static final String MARK_AS_READ = FQN + ".markNotifsAsRead";
}