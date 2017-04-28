package com.krishagni.catissueplus.core.biospecimen.repository.impl;


import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.Pair;

public class SpecimenDaoHelper {

	private static final SpecimenDaoHelper instance = new SpecimenDaoHelper();

	private SpecimenDaoHelper() {
	}

	public static SpecimenDaoHelper getInstance() {
		return instance;
	}

	public void addSiteCpsCond(Criteria query, SpecimenListCriteria crit) {
		addSiteCpsCond(query, crit, query.getAlias().equals("visit") ? "cpr" : "visit");
	}

	public void addSiteCpsCond(Criteria query, SpecimenListCriteria crit, String startAlias) {
		if (CollectionUtils.isEmpty(crit.siteCps())) {
			return;
		}

		switch (startAlias) {
			case "visit":
				query.createAlias("specimen.visit", "visit");

			case "cpr":
				query.createAlias("visit.registration", "cpr");

			case "cp":
				query.createAlias("cpr.collectionProtocol", "cp");
		}

		query.createAlias("cp.sites", "cpSite")
			.createAlias("cpSite.site", "site")
			.createAlias("cpr.participant", "participant")
			.createAlias("participant.pmis", "pmi", JoinType.LEFT_OUTER_JOIN)
			.createAlias("pmi.site", "mrnSite", JoinType.LEFT_OUTER_JOIN);

		Disjunction cpSitesCond = Restrictions.disjunction();
		for (Pair<Long, Long> siteCp : crit.siteCps()) {
			Long siteId = siteCp.first();
			Long cpId = siteCp.second();

			Junction siteCond = Restrictions.disjunction();
			if (crit.useMrnSites()) {
				//
				// When MRNs exist, site ID should be one of the MRN site
				//
				Junction mrnSite = Restrictions.conjunction()
					.add(Restrictions.isNotEmpty("participant.pmis"))
					.add(Restrictions.eq("mrnSite.id", siteId));

				//
				// When no MRNs exist, site ID should be one of CP site
				//
				Junction cpSite = Restrictions.conjunction()
					.add(Restrictions.isEmpty("participant.pmis"))
					.add(Restrictions.eq("site.id", siteId));

				siteCond.add(mrnSite).add(cpSite);
			} else {
				//
				// Site ID should be either MRN site or CP site
				//
				siteCond
					.add(Restrictions.eq("mrnSite.id", siteId))
					.add(Restrictions.eq("site.id", siteId));
			}

			Junction cond = Restrictions.conjunction().add(siteCond);
			if (cpId != null) {
				cond.add(Restrictions.eq("cp.id", cpId));
			}

			cpSitesCond.add(cond);
		}

		query.add(cpSitesCond);
	}
}
