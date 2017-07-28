package com.krishagni.catissueplus.core.common.errors;

public enum ConfigPrintRuleErrorCode implements ErrorCode{
	RULES_REQUIRED,

	CMD_FILES_DIR_REQUIRED,

	LABEL_TOKEN_NOT_FOUND,

	IP_RANGE_INVALID;

	@Override
	public String code() {
		return "CONFIG_PRINT_RULE_" + this.name();
	}
}
