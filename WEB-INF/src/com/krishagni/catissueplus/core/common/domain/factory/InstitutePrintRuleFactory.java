package com.krishagni.catissueplus.core.common.domain.factory;

import com.krishagni.catissueplus.core.common.domain.InstitutePrintRule;
import com.krishagni.catissueplus.core.common.events.InstitutePrintRuleDetail;

public interface InstitutePrintRuleFactory {
	InstitutePrintRule createInstitutePrintRule(InstitutePrintRuleDetail detail);
}