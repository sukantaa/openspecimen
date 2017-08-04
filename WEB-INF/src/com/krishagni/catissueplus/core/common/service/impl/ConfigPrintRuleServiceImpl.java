package com.krishagni.catissueplus.core.common.service.impl;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.ConfigPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.ConfigPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ConfigPrintRuleDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.repository.ConfigPrintRuleListCriteria;
import com.krishagni.catissueplus.core.common.service.ConfigPrintRuleService;

public class ConfigPrintRuleServiceImpl implements ConfigPrintRuleService {
	private DaoFactory daoFactory;

	private ConfigPrintRuleFactory configPrintRuleFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setConfigPrintRuleFactory(ConfigPrintRuleFactory configPrintRuleFactory) {
		this.configPrintRuleFactory = configPrintRuleFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ConfigPrintRuleDetail>> getConfigPrintRules(RequestEvent<ConfigPrintRuleListCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			List<ConfigPrintRule> rules = daoFactory.getConfigPrintRuleDao().getConfigPrintRules(req.getPayload());

			return ResponseEvent.response(ConfigPrintRuleDetail.from(rules));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ConfigPrintRuleDetail> createConfigPrintRule(RequestEvent<ConfigPrintRuleDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			ConfigPrintRuleDetail detail = req.getPayload();

			ConfigPrintRule rule = configPrintRuleFactory.createConfigPrintRule(detail);
			daoFactory.getConfigPrintRuleDao().saveOrUpdate(rule);

			return ResponseEvent.response(ConfigPrintRuleDetail.from(rule));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
}