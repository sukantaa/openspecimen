package com.krishagni.catissueplus.core.common;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

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

	public DataModelNamingStrategy() {
		loadPluginMappings("classpath*:tabNamesMap.properties", tableNames);
		loadPluginMappings("classpath*:seqNamesMap.properties", sequenceNames);
	}

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

	private void loadPluginMappings(String pattern, Map<String, String> mapping) {
		try {
			ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
			Resource[] resources = resourcePatternResolver.getResources(pattern);
			for (Resource resource : resources) {
				InputStream in = null;
				try {
					in = resource.getURL().openStream();

					Properties props = new Properties();
					props.load(in);
					for (String key : props.stringPropertyNames()) {
						mapping.put(key, props.getProperty(key));
					}
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading mappings from: " + pattern, e);
		}
	}
}
