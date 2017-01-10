package com.krishagni.catissueplus.core.common;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class DataModelNamingStrategy extends PhysicalNamingStrategyStandardImpl implements PhysicalNamingStrategy {

	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		if (name.getText().equals("ImportJob")) {
			return new Identifier("OS_BULK_IMPORT_JOBS", name.isQuoted());
		} else if (name.getText().equals("ImportJob_params")) {
			return new Identifier("OS_BULK_IMPORT_JOB_PARAMS", name.isQuoted());
		} else {
			return name;
		}
	}

	public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment context) {
		if (name.getText().equals("ImportJobsSeq")) {
			return new Identifier("OS_BULK_IMPORT_JOBS_SEQ", name.isQuoted());
		} else {
			return name;
		}
	}
}
