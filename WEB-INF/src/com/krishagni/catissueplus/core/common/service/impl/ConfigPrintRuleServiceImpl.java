package com.krishagni.catissueplus.core.common.service.impl;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.ConfigPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.ConfigPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.ConfigPrintRuleErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ConfigPrintRuleDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
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

	@Override
	@PlusTransactional
	public ResponseEvent<ConfigPrintRuleDetail> updateConfigPrintRule(RequestEvent<ConfigPrintRuleDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			ConfigPrintRuleDetail detail = req.getPayload();
			ConfigPrintRule existing = null;

			if (detail.getId() != null) {
				existing = daoFactory.getConfigPrintRuleDao().getById(detail.getId());
			}

			if (existing == null) {
				return ResponseEvent.userError(ConfigPrintRuleErrorCode.NOT_FOUND);
			}

			ConfigPrintRule rule = configPrintRuleFactory.createConfigPrintRule(detail);
			existing.update(rule);
			return ResponseEvent.response(ConfigPrintRuleDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

}