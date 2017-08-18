package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigDao;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigsListCriteria;

public class PrintRuleConfigDaoImpl extends AbstractDao<PrintRuleConfig> implements PrintRuleConfigDao {
	@Override
	public Class<?> getType() {
		return PrintRuleConfig.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PrintRuleConfig> getPrintRules(PrintRuleConfigsListCriteria crit) {
		return getPrintRulesConfigListQuery(crit)
			.addOrder(Order.desc("pr.id"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();
	}

	private Criteria getPrintRulesConfigListQuery(PrintRuleConfigsListCriteria crit) {
		Criteria criteria = getCurrentSession().createCriteria(PrintRuleConfig.class, "pr");
		return addSearchConditions(criteria, crit);
	}

	private Criteria addSearchConditions(Criteria query, PrintRuleConfigsListCriteria crit) {
		addObjectTypeRestriction(query, crit.objectType());
		addUserNameRestriction(query, crit.userName());
		addInstituteRestriction(query, crit.instituteName());
		return query;
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
				.add(Restrictions.ilike("u.lastName",  name, MatchMode.ANYWHERE))
				.add(Restrictions.ilike("u.loginName", name, MatchMode.ANYWHERE))
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
