package com.krishagni.catissueplus.core.common.service;

import com.krishagni.catissueplus.core.common.events.InstitutePrintRuleDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface InstitutePrintRuleService {
	ResponseEvent<InstitutePrintRuleDetail> createInstitutePrintRule(RequestEvent<InstitutePrintRuleDetail> req);
}