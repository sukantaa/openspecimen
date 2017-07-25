package com.krishagni.catissueplus.core.common.domain.factory;

import com.krishagni.catissueplus.core.common.domain.ConfigPrintRule;
import com.krishagni.catissueplus.core.common.events.ConfigPrintRuleDetail;

public interface ConfigPrintRuleFactory {
	ConfigPrintRule createConfigPrintRule(ConfigPrintRuleDetail detail);
}