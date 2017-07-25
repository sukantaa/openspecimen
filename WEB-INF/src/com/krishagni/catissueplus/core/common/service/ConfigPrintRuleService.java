package com.krishagni.catissueplus.core.common.service;

import com.krishagni.catissueplus.core.common.events.ConfigPrintRuleDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface ConfigPrintRuleService {
	ResponseEvent<ConfigPrintRuleDetail> createConfigPrintRule(RequestEvent<ConfigPrintRuleDetail> req);
}