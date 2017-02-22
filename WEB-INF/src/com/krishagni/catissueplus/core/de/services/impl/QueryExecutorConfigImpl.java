package com.krishagni.catissueplus.core.de.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;
import com.krishagni.query.events.ExecuteQueryOp;
import com.krishagni.query.services.QueryExecutorConfig;

import edu.common.dynamicextensions.query.Query;
import edu.common.dynamicextensions.query.QueryResultScreener;
import edu.common.dynamicextensions.query.ResultColumn;
import edu.common.dynamicextensions.query.WideRowMode;

public class QueryExecutorConfigImpl implements QueryExecutorConfig {
	@Override
	public int getMaxConcurrentQueries() {
		return ConfigUtil.getInstance().getIntSetting(CFG_MOD, MAX_CONCURRENT_QUERIES, DEF_MAX_CONCURRENT_QUERIES);
	}

	@Override
	public String getDateFormat() {
		return ConfigUtil.getInstance().getDeDateFmt();
	}

	@Override
	public String getTimeFormat() {
		return ConfigUtil.getInstance().getTimeFmt();
	}

	@Override
	public String getPathsConfig() {
		return QUERY_PATH_CFG;
	}

	@Override
	public void addRestrictions(ExecuteQueryOp op) {
		String rootForm = cprForm;
		if (StringUtils.isNotBlank(op.getDrivingForm())) {
			rootForm = op.getDrivingForm();
		}

		Query query = Query.createQuery()
			.wideRowMode(WideRowMode.valueOf(op.getWideRowMode()))
			.ic(true)
			.dateFormat(ConfigUtil.getInstance().getDeDateFmt())
			.timeFormat(ConfigUtil.getInstance().getTimeFmt());
		query.compile(rootForm, op.getAql());

		String aql = op.getAql();
		if (query.isPhiResult(true) && !AuthUtil.isAdmin()) {
			if (query.isAggregateQuery() || StringUtils.isNotBlank(query.getResultProcessorName())) {
				throw OpenSpecimenException.userError(SavedQueryErrorCode.PHI_NOT_ALLOWED_IN_AGR);
			}

			aql = getAqlWithCpIdInSelect(AuthUtil.getCurrentUser(), "Count".equals(op.getRunType()), aql);
		}

		Map<String, Object> appData = op.getAppData();
		Long cpId = -1L;
		if (appData != null && appData.get("cpId") != null) {
			cpId = ((Number)appData.get("cpId")).longValue();
		}

		op.setDrivingForm(rootForm);
		op.setAql(aql);
		op.setRestriction(getRestriction(AuthUtil.getCurrentUser(), cpId));
	}

	@Override
	public QueryResultScreener getScreener(Query query) {
		if (query.isPhiResult(true) && !AuthUtil.isAdmin()) {
			return new QueryResultScreenerImpl(AuthUtil.getCurrentUser(), false);
		}

		return null;
	}

	private String getRestriction(User user, Long cpId) {
		if (user.isAdmin()) {
			if (cpId != null && cpId != -1) {
				return cpForm + ".id = " + cpId;
			}
		} else {
			Set<Long> cpIds = AccessCtrlMgr.getInstance().getReadableCpIds();
			if (cpIds == null || cpIds.isEmpty()) {
				throw new IllegalAccessError("User does not have access to any CP");
			}

			if (cpId != null && cpId != -1) {
				if (cpIds.contains(cpId)) {
					return cpForm + ".id = " + cpId;
				}

				throw new IllegalAccessError("Access to cp is not permitted: " + cpId);
			} else {
				List<String> restrictions = new ArrayList<String>();
				List<Long> cpIdList = new ArrayList<Long>(cpIds);

				int startIdx = 0, numCpIds = cpIdList.size();
				int chunkSize = 999;
				while (startIdx < numCpIds) {
					int endIdx = startIdx + chunkSize;
					if (endIdx > numCpIds) {
						endIdx = numCpIds;
					}

					restrictions.add(getCpIdRestriction(cpIdList.subList(startIdx, endIdx)));
					startIdx = endIdx;
				}

				return "(" + StringUtils.join(restrictions, " or ") + ")";
			}
		}

		return null;
	}

	private String getCpIdRestriction(List<Long> cpIds) {
		return new StringBuilder(cpForm)
				.append(".id in (")
				.append(StringUtils.join(cpIds, ", "))
				.append(")")
				.toString();
	}

	private String getAqlWithCpIdInSelect(User user, boolean isCount, String aql) {
		if (user.isAdmin() || isCount) {
			return aql;
		} else {
			aql = aql.trim();
			Matcher matcher = SELECT_PATTERN.matcher(aql);
			if (matcher.matches()) {
				String select = matcher.group(1);
				return select + " " + cpForm + ".id, " + aql.substring(select.length());
			} else {
				String afterSelect = aql.trim().substring("select".length());
				return "select " + cpForm + ".id, " + afterSelect;
			}
		}
	}

	private class QueryResultScreenerImpl implements QueryResultScreener {
		private User user;

		private boolean countQuery;

		private String mask;

		private Map<Long, AccessCtrlMgr.ParticipantReadAccess> phiAccessMap = new HashMap<Long, AccessCtrlMgr.ParticipantReadAccess>();

		private static final String MASK_MARKER = "##########";

		public QueryResultScreenerImpl(User user, boolean countQuery) {
			this(user, countQuery, null);
		}

		public QueryResultScreenerImpl(User user, boolean countQuery, String mask) {
			this.user = user;
			this.countQuery = countQuery;
			this.mask = (mask == null) ? MASK_MARKER : mask;
		}

		@Override
		public List<ResultColumn> getScreenedResultColumns(List<ResultColumn> preScreenedResultCols) {
			if (user.isAdmin() || this.countQuery) {
				return preScreenedResultCols;
			}

			List<ResultColumn> result = new ArrayList<ResultColumn>(preScreenedResultCols);
			result.remove(0);
			return result;
		}

		@Override
		public Object[] getScreenedRowData(List<ResultColumn> preScreenedResultCols, Object[] rowData) {
			if (user.isAdmin() || this.countQuery || rowData.length == 0) {
				return rowData;
			}

			Long cpId = ((Number)rowData[0]).longValue();
			Object[] screenedData = ArrayUtils.remove(rowData, 0);

			AccessCtrlMgr.ParticipantReadAccess access = phiAccessMap.get(cpId);
			if (access == null) {
				access = AccessCtrlMgr.getInstance().getParticipantReadAccess(cpId);
				phiAccessMap.put(cpId, access);
			}

			if (access.phiAccess) {
				return screenedData;
			}

			int i = 0;
			boolean first = true;
			for (ResultColumn col : preScreenedResultCols) {
				if (first) {
					first = false;
					continue;
				}

				if (col.isSimpleExpr() && col.isPhi()) {
					screenedData[i] = mask;
				}

				++i;
			}

			return screenedData;
		}
	}

	private static String CFG_MOD = "query";

	private static String MAX_CONCURRENT_QUERIES = "max_concurrent_queries";

	private static int DEF_MAX_CONCURRENT_QUERIES = 10;

	private static final String QUERY_PATH_CFG = "/com/krishagni/catissueplus/core/de/query/paths.xml";

	private static final String cpForm = "CollectionProtocol";

	private static final String cprForm = "Participant";

	private static final Pattern SELECT_PATTERN = Pattern.compile("^(select\\s+distinct|select)\\s+.*$");
}
