package com.krishagni.catissueplus.core.de.repository.impl;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.de.domain.QueryAuditLog;
import com.krishagni.catissueplus.core.de.events.ListQueryAuditLogsCriteria;
import com.krishagni.catissueplus.core.de.repository.QueryAuditLogDao;

public class QueryAuditLogDaoImpl extends AbstractDao<QueryAuditLog> implements QueryAuditLogDao {
	@Override
	public Class<QueryAuditLog> getType() {
		return QueryAuditLog.class;
	}

	@Override
	public Long getLogsCount(ListQueryAuditLogsCriteria crit) {
		Number count = (Number) getLogsQuery(crit)
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<QueryAuditLog> getLogs(ListQueryAuditLogsCriteria crit) {
		return (List<QueryAuditLog>) getLogsQuery(crit)
			.addOrder(Order.desc("id"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();
	}

	private Criteria getLogsQuery(ListQueryAuditLogsCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(QueryAuditLog.class);

		if (crit.queryId() != null) {
			query.createAlias("query", "query")
				.add(Restrictions.eq("query.id", crit.queryId()));
		}

		if (crit.userId() != null || crit.instituteId() != null) {
			query.createAlias("runBy", "rb");

			if (crit.userId() != null) {
				query.add(Restrictions.eq("rb.id", crit.userId()));
			} else {
				query.createAlias("rb.institute", "ri").add(Restrictions.eq("ri.id", crit.instituteId()));
			}
		}

		return query;
	}
}
