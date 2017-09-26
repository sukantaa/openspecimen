package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.administrative.domain.SpecimenRequest;
import com.krishagni.catissueplus.core.administrative.events.SpecimenRequestSummary;
import com.krishagni.catissueplus.core.administrative.repository.SpecimenRequestDao;
import com.krishagni.catissueplus.core.administrative.repository.SpecimenRequestListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class SpecimenRequestDaoImpl extends AbstractDao<SpecimenRequest> implements SpecimenRequestDao {
	public Class<SpecimenRequest> getType() {
		return SpecimenRequest.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SpecimenRequestSummary> getSpecimenRequests(SpecimenRequestListCriteria listCrit) {
		Criteria query = addSummaryFields(getListQuery(listCrit));
		return ((List<Object[]>)query.list()).stream().map(this::getRequest).collect(Collectors.toList());
	}

	private Criteria getListQuery(SpecimenRequestListCriteria listCrit) {
		Criteria query = getCurrentSession().createCriteria(SpecimenRequest.class)
			.createAlias("dp", "dp", JoinType.LEFT_OUTER_JOIN)
			.setFirstResult(listCrit.startAt())
			.setMaxResults(listCrit.maxResults())
			.addOrder(Order.desc("id"));

		return addCatalogCond(query, listCrit);
	}

	private Criteria addCatalogCond(Criteria query, SpecimenRequestListCriteria listCrit) {
		if (listCrit.catalogId() == null) {
			return query;
		}

		query.add(Restrictions.eq("catalogId", listCrit.catalogId()));
		return query;
	}

	private Criteria addSummaryFields(Criteria query) {
		query.setProjection(Projections.projectionList()
			.add(Projections.property("id"))
			.add(Projections.property("catalogId"))
			.add(Projections.property("requestorEmailId"))
			.add(Projections.property("irbId"))
			.add(Projections.property("dp.id"))
			.add(Projections.property("dp.shortTitle"))
			.add(Projections.property("dateOfRequest"))
			.add(Projections.property("activityStatus")));
		return query;
	}

	private SpecimenRequestSummary getRequest(Object[] row) {
		int idx = 0;
		SpecimenRequestSummary req = new SpecimenRequestSummary();
		req.setId((Long)row[idx++]);
		req.setCatalogId((Long)row[idx++]);
		req.setRequestorEmailId((String)row[idx++]);
		req.setIrbId((String)row[idx++]);
		req.setDpId((Long)row[idx++]);
		req.setDpShortTitle((String)row[idx++]);
		req.setDateOfRequest((Date)row[idx++]);
		req.setActivityStatus((String)row[idx++]);
		return req;
	}
}