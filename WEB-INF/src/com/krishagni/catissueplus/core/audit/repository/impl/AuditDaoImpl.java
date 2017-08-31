package com.krishagni.catissueplus.core.audit.repository.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;

import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.repository.AuditDao;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class AuditDaoImpl extends AbstractDao<UserApiCallLog> implements AuditDao {

	@Override
	@SuppressWarnings("unchecked")
	public AuditDetail getAuditDetail(String auditTable, Long objectId) {
		Pair<Date, UserSummary> createInfo = getRevisionInfo(getLatestRevisionInfo(auditTable, objectId, 0));
		Pair<Date, UserSummary> updateInfo = getRevisionInfo(getLatestRevisionInfo(auditTable, objectId, 1));

		AuditDetail result = new AuditDetail();
		result.setCreatedOn(createInfo.first());
		result.setCreatedBy(createInfo.second());
		result.setLastUpdatedOn(updateInfo.first());
		result.setLastUpdatedBy(updateInfo.second());
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Date getLatestApiCallTime(Long userId, String token) {
		List<Date> result = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_LATEST_API_CALL_TIME)
				.setLong("userId", userId)
				.setString("authToken", token)
				.list();

		return result.isEmpty() ? null : result.get(0);
	}

	@SuppressWarnings("unchecked")
	private Object[] getLatestRevisionInfo(String auditTable, Long objectId, int revType) {
		List<Object[]> rows = getCurrentSession().createSQLQuery(String.format(GET_REV_INFO, auditTable))
			.addScalar("rev", LongType.INSTANCE)
			.addScalar("revTime", TimestampType.INSTANCE)
			.addScalar("userId", LongType.INSTANCE)
			.addScalar("firstName", StringType.INSTANCE)
			.addScalar("lastName", StringType.INSTANCE)
			.addScalar("emailAddr", StringType.INSTANCE)
			.setParameter("objectId", objectId)
			.setParameter("revType", revType)
			.setMaxResults(1)
			.list();
		return CollectionUtils.isEmpty(rows) ? null : rows.iterator().next();
	}

	private Pair<Date, UserSummary> getRevisionInfo(Object[] row) {
		if (row == null) {
			return Pair.make(null, null);
		}

		int idx = 1;
		Date revTime = (Date)row[idx++];

		UserSummary user = new UserSummary();
		user.setId((Long)row[idx++]);
		user.setFirstName((String)row[idx++]);
		user.setLastName((String)row[idx++]);
		user.setEmailAddress((String)row[idx++]);
		return Pair.make(revTime, user);
	}

	private static final String FQN = UserApiCallLog.class.getName();

	private static final String GET_REV_INFO =
		"select " +
		"  r.rev as rev, r.revtstmp as revTime, r.user_id as userId, " +
		"  u.first_name as firstName, u.last_name as lastName, u.email_address as emailAddr " +
		"from " +
		"  os_revisions r " +
		"  inner join %s a on a.rev = r.rev " +
		"  inner join catissue_user u on u.identifier = r.user_id " +
		"where " +
		"  a.identifier = :objectId " +
		"  and a.revtype = :revType " +
		"order by " +
		"  r.revtstmp desc";

	private static final String GET_LATEST_API_CALL_TIME = FQN + ".getLatestApiCallTime";
}
