package com.krishagni.catissueplus.core.common;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class DataModelNamingStrategy extends PhysicalNamingStrategyStandardImpl implements PhysicalNamingStrategy {
	private Map<String, String> tableNames = new HashMap<String, String>() {
		{
			put("ImportJob", "OS_BULK_IMPORT_JOBS");
			put("ImportJob_params", "OS_BULK_IMPORT_JOB_PARAMS");
			put("AuthDomain", "OS_AUTH_DOMAINS");
			put("AuthProvider", "OS_AUTH_PROVIDERS");
			put("AuthProvider_props", "OS_AUTH_PROVIDER_PROPS");
			put("UserApiCallLog", "OS_USER_API_CALLS_LOG");
			put("LoginAuditLog", "OS_LOGIN_AUDIT_LOGS");
			put("AuthToken", "OS_AUTH_TOKENS");
			put("ForgotPasswordToken", "OS_FORGOT_PASSWORD_TOKENS");
		}
	};

	private Map<String, String> sequenceNames = new HashMap<String, String>() {
		{
			put("ImportJobsSeq", "OS_BULK_IMPORT_JOBS_SEQ");
			put("AuthDomainsSeq", "OS_AUTH_DOMAINS_SEQ");
			put("AuthProvidersSeq", "OS_AUTH_PROVIDERS_SEQ");
			put("UserApiCallLogsSeq", "OS_USER_API_CALLS_LOG_SEQ");
			put("LoginAuditLogsSeq", "OS_LOGIN_AUDIT_LOGS_SEQ");
		}
	};

	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return toId(tableNames, name);
	}

	public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment context) {
		return toId(sequenceNames, name);
	}

	private Identifier toId(Map<String, String> namesMap, Identifier name) {
		String mappedName = namesMap.get(name.getText());
		if (mappedName == null) {
			return name;
		} else {
			return new Identifier(mappedName, name.isQuoted());
		}
	}
}
