package com.krishagni.catissueplus.core.common.service.impl;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.InstitutePrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.InstitutePrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.InstitutePrintRuleDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.InstitutePrintRuleService;

public class InstitutePrintRuleServiceImpl implements InstitutePrintRuleService {
	private DaoFactory daoFactory;

	private InstitutePrintRuleFactory institutePrintRuleFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setInstitutePrintRuleFactory(InstitutePrintRuleFactory institutePrintRuleFactory) {
		this.institutePrintRuleFactory = institutePrintRuleFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<InstitutePrintRuleDetail> createInstitutePrintRule(RequestEvent<InstitutePrintRuleDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			InstitutePrintRuleDetail detail = req.getPayload();

			InstitutePrintRule rule = institutePrintRuleFactory.createInstitutePrintRule(detail);
			daoFactory.getInstitutePrintRuleDao().saveOrUpdate(rule);

			return ResponseEvent.response(InstitutePrintRuleDetail.from(rule));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
}