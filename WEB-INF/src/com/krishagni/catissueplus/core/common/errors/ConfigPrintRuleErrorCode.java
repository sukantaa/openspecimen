package com.krishagni.catissueplus.core.common.errors;

public enum ConfigPrintRuleErrorCode implements ErrorCode{
	OBJECT_TYPE_REQUIRED,

	RULES_REQUIRED,

	CMD_FILES_DIR_REQUIRED;

	@Override
	public String code() {
		return "CONFIG_PRINT_RULE_" + this.name();
	}
}
