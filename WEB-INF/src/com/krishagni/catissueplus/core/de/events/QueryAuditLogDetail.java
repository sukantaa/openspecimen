package com.krishagni.catissueplus.core.de.events;

import com.krishagni.catissueplus.core.de.domain.QueryAuditLog;

public class QueryAuditLogDetail extends QueryAuditLogSummary {
	
	private String sql;
	
	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}
	
	public static QueryAuditLogDetail from(QueryAuditLog auditLog){
		QueryAuditLogDetail detail = new QueryAuditLogDetail();
		copyTo(auditLog, detail);
		detail.setSql(auditLog.getSql());
		return detail;
	}
}
