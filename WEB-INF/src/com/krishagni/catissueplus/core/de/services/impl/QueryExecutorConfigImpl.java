package com.krishagni.catissueplus.core.de.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.query.services.QueryExecutorConfig;

import edu.common.dynamicextensions.query.Query;
import edu.common.dynamicextensions.query.QueryResultScreener;
import edu.common.dynamicextensions.query.ResultColumn;

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
	public QueryResultScreener getScreener(Query query) {
		if (query.isPhiResult(true) && !AuthUtil.isAdmin()) {
			return new QueryResultScreenerImpl(AuthUtil.getCurrentUser(), false);
		}

		return null;
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
}
