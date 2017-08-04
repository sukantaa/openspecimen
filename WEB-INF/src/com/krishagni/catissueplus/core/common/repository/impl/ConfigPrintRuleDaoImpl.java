package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.common.domain.ConfigPrintRule;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.ConfigPrintRuleDao;
import com.krishagni.catissueplus.core.common.repository.ConfigPrintRuleListCriteria;
import com.krishagni.catissueplus.core.common.util.Status;

public class ConfigPrintRuleDaoImpl extends AbstractDao<ConfigPrintRule> implements ConfigPrintRuleDao {
	@Override
	public Class<?> getType() {
		return ConfigPrintRule.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ConfigPrintRule> getConfigPrintRules(ConfigPrintRuleListCriteria crit) {
		return getConfigPrintRuleListCriteria(crit)
				.setFirstResult(crit.startAt())
				.setMaxResults(crit.maxResults())
				.addOrder(Order.desc("pr.updatedBy"))
				.list();
	}

	private Criteria getConfigPrintRuleListCriteria(ConfigPrintRuleListCriteria crit) {
		Criteria criteria = sessionFactory.getCurrentSession()
				.createCriteria(ConfigPrintRule.class, "pr")
				.add(Restrictions.eq("pr.activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()));

		return addSearchConditions(criteria, crit);
	}

	private Criteria addSearchConditions(Criteria criteria, ConfigPrintRuleListCriteria crit) {
		addObjectTypeRestriction(criteria, crit.objectType());
		addUserNameRestriction(criteria, crit.userName());
		addCpRestriction(criteria, crit.cpTitle());
		addInstituteRestriction(criteria, crit.instituteName());
		return criteria;
	}

	private void addObjectTypeRestriction(Criteria criteria, String objectType) {
		if (StringUtils.isBlank(objectType)) {
			return;
		}

		criteria.add(Restrictions.eq("pr.objectType", objectType));
	}

	private void addUserNameRestriction(Criteria criteria, String name) {
		if (StringUtils.isBlank(name)) {
			return;
		}

		criteria.createAlias("pr.updatedBy", "u")
			.add(Restrictions.disjunction()
				.add(Restrictions.ilike("u.firstName", name, MatchMode.ANYWHERE))
				.add(Restrictions.ilike("u.lastName", name, MatchMode.ANYWHERE))
				.add(Restrictions.ilike("u.loginName", name, MatchMode.ANYWHERE))
			);
	}

	private void addCpRestriction(Criteria criteria, String title) {
		if (StringUtils.isBlank(title)) {
			return;
		}

		criteria.createAlias("pr.collectionProtocol", "cp")
			.add(Restrictions.disjunction()
				.add(Restrictions.eq("cp.title", title))
				.add(Restrictions.eq("cp.shortTitle", title))
			);
	}

	private void addInstituteRestriction(Criteria criteria, String instituteName) {
		if (StringUtils.isBlank(instituteName)) {
			return;
		}

		criteria.createAlias("pr.institute", "institute")
			.add(Restrictions.eq("institute.name", instituteName));
	}
}
