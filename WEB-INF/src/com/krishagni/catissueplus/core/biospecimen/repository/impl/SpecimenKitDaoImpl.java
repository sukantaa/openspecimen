package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenKit;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenKitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenKitDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenKitListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class SpecimenKitDaoImpl extends AbstractDao<SpecimenKit> implements SpecimenKitDao {

	@Override
	public Class<SpecimenKit> getType() {
		return SpecimenKit.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SpecimenKitSummary> getSpecimenKits(SpecimenKitListCriteria listCrit) {
		Criteria query = getCurrentSession().createCriteria(SpecimenKit.class)
			.setFirstResult(listCrit.startAt())
			.setMaxResults(listCrit.maxResults());

		addCpRestriction(query, listCrit);

		List<SpecimenKitSummary> kits =  getSpecimenKitSummary(query.list());
		if (kits.isEmpty() || !listCrit.includeStat()) {
			return kits;
		}

		Map<Long, SpecimenKitSummary> kitsMap = kits.stream().collect(Collectors.toMap(kit -> kit.getId(), kit -> kit));
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_PARTICIPANT_N_SPECIMEN_CNT)
			.setParameterList("kitIds", kitsMap.keySet())
			.list();

		for (Object[] row :rows) {
			Long kitId = (Long)row[0];
			SpecimenKitSummary kit = kitsMap.get(kitId);
			kit.setParticipantCount((Long) row[1]);
			kit.setSpecimenCount((Long) row[2]);
		}

		return kits;
	}

	private void addCpRestriction(Criteria query, SpecimenKitListCriteria listCrit) {
		if (listCrit.cpId() == null && StringUtils.isBlank(listCrit.cpShortTitle()) && StringUtils.isBlank(listCrit.cpTitle())) {
			return;
		}

		query.createAlias("collectionProtocol", "cp");

		if (listCrit.cpId() != null) {
			query.add(Restrictions.eq("cp.id", listCrit.cpId()));
		} else if (StringUtils.isNotBlank(listCrit.cpShortTitle())) {
			query.add(Restrictions.eq("cp.shortTitle", listCrit.cpShortTitle()));
		} else if (StringUtils.isNotBlank(listCrit.cpTitle())) {
			query.add(Restrictions.eq("cp.title", listCrit.cpTitle()));
		}
	}

	private List<SpecimenKitSummary> getSpecimenKitSummary(List<SpecimenKit> kits) {
		return kits.stream().map(SpecimenKitSummary::from).collect(Collectors.toList());
	}

	private static final String FQN = SpecimenKit.class.getName();

	private static final String GET_PARTICIPANT_N_SPECIMEN_CNT = FQN + ".getParticipantAndSpecimenCount";
}
