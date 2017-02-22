package com.krishagni.catissueplus.core.de.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.repository.UserDao;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.ConfigChangeListener;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.service.TemplateService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.AqlBuilder;
import com.krishagni.catissueplus.core.de.domain.Filter;
import com.krishagni.catissueplus.core.de.domain.QueryAuditLog;
import com.krishagni.catissueplus.core.de.domain.QueryFolder;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.domain.factory.QueryFolderFactory;
import com.krishagni.catissueplus.core.de.events.ExecuteSavedQueryOp;
import com.krishagni.catissueplus.core.de.events.GetFieldValuesOp;
import com.krishagni.catissueplus.core.de.events.ListFolderQueriesCriteria;
import com.krishagni.catissueplus.core.de.events.ListQueryAuditLogsCriteria;
import com.krishagni.catissueplus.core.de.events.ListSavedQueriesCriteria;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogDetail;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogSummary;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogsList;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.events.QueryFolderDetails;
import com.krishagni.catissueplus.core.de.events.QueryFolderSummary;
import com.krishagni.catissueplus.core.de.events.SavedQueriesList;
import com.krishagni.catissueplus.core.de.events.SavedQueryDetail;
import com.krishagni.catissueplus.core.de.events.SavedQuerySummary;
import com.krishagni.catissueplus.core.de.events.ShareQueryFolderOp;
import com.krishagni.catissueplus.core.de.events.UpdateFolderQueriesOp;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;
import com.krishagni.catissueplus.core.de.repository.QueryAuditLogDao;
import com.krishagni.catissueplus.core.de.repository.SavedQueryDao;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;
import com.krishagni.commons.errors.AppException;
import com.krishagni.query.events.ExecuteQueryOp;
import com.krishagni.query.events.FieldDetail;
import com.krishagni.query.events.QueryExecResult;
import com.krishagni.query.services.QueryExecutor;
import com.krishagni.query.services.impl.QueryExecutorImpl;

import edu.common.dynamicextensions.nutility.DeConfiguration;
import edu.common.dynamicextensions.query.Query;
import edu.common.dynamicextensions.query.QueryException;
import edu.common.dynamicextensions.query.QueryParserException;
import edu.common.dynamicextensions.query.QueryResponse;
import edu.common.dynamicextensions.query.WideRowMode;

public class QueryServiceImpl implements QueryService, InitializingBean {
	private static final Log logger = LogFactory.getLog(QueryServiceImpl.class);

	private static final String cprForm = "Participant";

	private static String QUERY_DATA_EXPORTED_EMAIL_TMPL = "query_export_data";
	
	private static String SHARE_QUERY_FOLDER_EMAIL_TMPL = "query_share_query_folder";

	private static String CFG_MOD = "query";

	private static String MAX_RECS_IN_MEM = "max_recs_in_memory";

	private static int DEF_MAX_RECS_IN_MEM = 100;

	private static final int EXPORT_THREAD_POOL_SIZE = getThreadPoolSize();
	
	private static final int ONLINE_EXPORT_TIMEOUT_SECS = 30;

	private static ExecutorService exportThreadPool = Executors.newFixedThreadPool(EXPORT_THREAD_POOL_SIZE);

	private DaoFactory daoFactory;

	private UserDao userDao;
	
	private QueryFolderFactory queryFolderFactory;
	
	private EmailService emailService;
	
	private TemplateService templateService;

	private ConfigurationService cfgService;

	private QueryExecutor queryExecutor = new QueryExecutorImpl();

	private int maxRecsInMemory = DEF_MAX_RECS_IN_MEM;

	private AtomicInteger concurrentQueriesCnt = new AtomicInteger(0);

	static {
		initExportFileCleaner();
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}

	public void setQueryFolderFactory(QueryFolderFactory queryFolderFactory) {
		this.queryFolderFactory = queryFolderFactory;
	}

	public void setEmailService(EmailService emailService) {
		this.emailService = emailService;
	}

	public void setTemplateService(TemplateService templateService) {
		this.templateService = templateService;
	}

	public void setCfgService(ConfigurationService cfgService) {
		this.cfgService = cfgService;
		refreshConfig();
		cfgService.registerChangeListener("query", new ConfigChangeListener() {
			@Override
			public void onConfigChange(String name, String value) {
				refreshConfig();
			}
		});
	}

	public void setQueryExecutor(QueryExecutor queryExecutor) {
		this.queryExecutor = queryExecutor;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		refreshConfig();
		cfgService.registerChangeListener("query", new ConfigChangeListener() {
			@Override
			public void onConfigChange(String name, String value) {
				refreshConfig();
			}
		});
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SavedQueriesList> getSavedQueries(RequestEvent<ListSavedQueriesCriteria> req) {
		try {
			ListSavedQueriesCriteria crit = req.getPayload();
			
			if (crit.startAt() < 0 || crit.maxResults() <= 0) {
				return ResponseEvent.userError(SavedQueryErrorCode.INVALID_PAGINATION_FILTER);
			}

			Long userId = AuthUtil.getCurrentUser().getId();
			List<SavedQuerySummary> queries = daoFactory.getSavedQueryDao().getQueries(
							userId, 
							crit.startAt(),
							crit.maxResults(),
							crit.query());
			
			Long count = null;
			if (crit.countReq()) {
				count = daoFactory.getSavedQueryDao().getQueriesCount(userId, crit.query());
			}
			
			return ResponseEvent.response(SavedQueriesList.create(queries, count));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SavedQueryDetail> getSavedQuery(RequestEvent<Long> req) {
		try {
			SavedQuery savedQuery = daoFactory.getSavedQueryDao().getQuery(req.getPayload());
			if (savedQuery == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.NOT_FOUND, req.getPayload());
			}
			
			return ResponseEvent.response(SavedQueryDetail.fromSavedQuery(savedQuery));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SavedQueryDetail> saveQuery(RequestEvent<SavedQueryDetail> req) {
		try {
			SavedQueryDetail queryDetail = req.getPayload();
			queryDetail.setId(null);

			Query.createQuery()
				.wideRowMode(WideRowMode.DEEP)
				.ic(true)
				.dateFormat(ConfigUtil.getInstance().getDeDateFmt())
				.timeFormat(ConfigUtil.getInstance().getTimeFmt())
				.compile(cprForm, getAql(queryDetail));
			SavedQuery savedQuery = getSavedQuery(queryDetail);
			daoFactory.getSavedQueryDao().saveOrUpdate(savedQuery);
			return ResponseEvent.response(SavedQueryDetail.fromSavedQuery(savedQuery));
		} catch (QueryParserException qpe) {
			return ResponseEvent.userError(SavedQueryErrorCode.MALFORMED, qpe.getMessage());
		} catch (QueryException qe) {
			return ResponseEvent.userError(getErrorCode(qe.getErrorCode()), qe.getMessage());
		} catch (IllegalArgumentException iae) {
			return ResponseEvent.userError(SavedQueryErrorCode.MALFORMED, iae.getMessage());
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}	
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SavedQueryDetail> updateQuery(RequestEvent<SavedQueryDetail> req) {
		try {
			SavedQueryDetail queryDetail = req.getPayload();

			Query.createQuery()
				.wideRowMode(WideRowMode.DEEP)
				.ic(true)
				.dateFormat(ConfigUtil.getInstance().getDeDateFmt())
				.timeFormat(ConfigUtil.getInstance().getTimeFmt())
				.compile(cprForm, getAql(queryDetail));
			SavedQuery savedQuery = getSavedQuery(queryDetail);
			SavedQuery existing = daoFactory.getSavedQueryDao().getQuery(queryDetail.getId());
			if (existing == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.NOT_FOUND, queryDetail.getId());
			}
			
			User user = AuthUtil.getCurrentUser();
			if (!user.isAdmin() && !existing.getCreatedBy().equals(user)) {
				return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED);
			}
			
			existing.update(savedQuery);
			daoFactory.getSavedQueryDao().saveOrUpdate(existing);	
			return ResponseEvent.response(SavedQueryDetail.fromSavedQuery(existing));
		} catch (QueryParserException qpe) {
			return ResponseEvent.userError(SavedQueryErrorCode.MALFORMED, qpe.getMessage());
		} catch (QueryException qe) {
			return ResponseEvent.userError(getErrorCode(qe.getErrorCode()), qe.getMessage());
		} catch (IllegalArgumentException iae) {
			return ResponseEvent.userError(SavedQueryErrorCode.MALFORMED, iae.getMessage());
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> deleteQuery(RequestEvent<Long> req) {
		try {
			Long queryId = req.getPayload();
			SavedQuery query = daoFactory.getSavedQueryDao().getQuery(queryId);
			if (query == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.NOT_FOUND, queryId);
			}

			User user = AuthUtil.getCurrentUser();
			if (!user.isAdmin() && !query.getCreatedBy().equals(user)) {
				return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED);
			}

			query.setDeletedOn(Calendar.getInstance().getTime());
			daoFactory.getSavedQueryDao().saveOrUpdate(query);
			return ResponseEvent.response(queryId);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryExecResult> executeQuery(RequestEvent<ExecuteQueryOp> req) {
		try {
			return ResponseEvent.response(queryExecutor.executeQuery(req.getPayload()));
		} catch (AppException e) {
			return ResponseEvent.error(new OpenSpecimenException(e));
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryExecResult> executeSavedQuery(RequestEvent<ExecuteSavedQueryOp> req) {
		try {
			ExecuteSavedQueryOp input = req.getPayload();
			SavedQuery query = daoFactory.getSavedQueryDao().getQuery(input.getSavedQueryId());
			if (query == null) {
				throw OpenSpecimenException.userError(SavedQueryErrorCode.NOT_FOUND, input.getSavedQueryId());
			}

			query = query.copy();
			for (Filter filter : query.getFilters()) {
				ExecuteSavedQueryOp.Criterion criterion = input.getCriterion(filter.getId());
				if (criterion == null || CollectionUtils.isEmpty(criterion.getValues())) {
					continue;
				}

				switch (criterion.getSearchType()) {
					case EQUALS:
						filter.setEqValues(criterion.getValues());
						break;

					case RANGE:
						filter.setRangeValues(criterion.getValues());
						break;
				}
			}

			ExecuteQueryOp op = new ExecuteQueryOp();
			op.setAppData(Collections.singletonMap("cpId", query.getCpId()));
			op.setDrivingForm(input.getDrivingForm());
			op.setRunType(input.getRunType());
			op.setWideRowMode(input.getWideRowMode());
			op.setQueryId(query.getId());
			op.setAql(query.getAql() + " limit " + input.getStartAt() + ", " + input.getMaxResults());
			return executeQuery(new RequestEvent<>(op));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> exportQueryData(RequestEvent<ExecuteQueryOp> req) {
//		boolean queryCntIncremented = false;
		try {
//			queryCntIncremented = incConcurrentQueriesCnt();
			return ResponseEvent.response(exportQueryData(req.getPayload(), null));
		} catch (QueryParserException qpe) {
			return ResponseEvent.userError(SavedQueryErrorCode.MALFORMED, qpe.getMessage());
		} catch (QueryException qe) {
			return ResponseEvent.userError(getErrorCode(qe.getErrorCode()), qe.getMessage());
		} catch (IllegalArgumentException iae) {
			return ResponseEvent.userError(SavedQueryErrorCode.MALFORMED, iae.getMessage());
		} catch (IllegalAccessError iae) {
			return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED, iae.getMessage());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
//			if (queryCntIncremented) {
//				decConcurrentQueriesCnt();
//			}
		}
	}
	
	@Override
	public ResponseEvent<File> getExportDataFile(RequestEvent<String> req) {
		String fileId = req.getPayload();
		try {
			String path = getExportDataDir() + File.separator + fileId;
			File f = new File(path);
			if (f.exists()) {
				return ResponseEvent.response(f);
			} else {
				return ResponseEvent.userError(SavedQueryErrorCode.EXPORT_DATA_FILE_NOT_FOUND);
			}
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<List<QueryFolderSummary>> getUserFolders(RequestEvent<?> req) {
		try {
			Long userId = AuthUtil.getCurrentUser().getId();			
			List<QueryFolder> folders = daoFactory.getQueryFolderDao().getUserFolders(userId);			
			return ResponseEvent.response(QueryFolderSummary.from(folders));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<QueryFolderDetails> getFolder(RequestEvent<Long> req) {
		try {
			Long folderId = req.getPayload();
			QueryFolder folder = daoFactory.getQueryFolderDao().getQueryFolder(folderId);
			if (folder == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.FOLDER_NOT_FOUND);
			}
			
			User user = AuthUtil.getCurrentUser();
			if (!user.isAdmin() && !folder.canUserAccess(user.getId())) {
				return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED);
			}
			
			return ResponseEvent.response(QueryFolderDetails.from(folder));			
		} catch (Exception e) {
			return ResponseEvent.serverError(e);			
		}
	}	
	
	
	@Override
	@PlusTransactional
	public ResponseEvent<QueryFolderDetails> createFolder(RequestEvent<QueryFolderDetails> req) {
		try {
			QueryFolderDetails folderDetails = req.getPayload();
			
			UserSummary owner = new UserSummary();
			owner.setId(AuthUtil.getCurrentUser().getId());
			folderDetails.setOwner(owner);
			
			QueryFolder queryFolder = queryFolderFactory.createQueryFolder(folderDetails);			
			daoFactory.getQueryFolderDao().saveOrUpdate(queryFolder);
			
			if (!queryFolder.getSharedWith().isEmpty()) {
				sendFolderSharedEmail(queryFolder.getOwner(), queryFolder, queryFolder.getSharedWith());
			}			
			return ResponseEvent.response(QueryFolderDetails.from(queryFolder));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryFolderDetails> updateFolder(RequestEvent<QueryFolderDetails> req) {
		try {
			QueryFolderDetails folderDetails = req.getPayload();
			Long folderId = folderDetails.getId();
			if (folderId == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.FOLDER_NOT_FOUND);
			}
						
			QueryFolder existing = daoFactory.getQueryFolderDao().getQueryFolder(folderId);
			if (existing == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.FOLDER_NOT_FOUND);
			}
			
			User user = AuthUtil.getCurrentUser();
			if (!user.isAdmin() && !existing.getOwner().equals(user)) {
				return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED);
			}
			
			UserSummary owner = new UserSummary();
			owner.setId(existing.getOwner().getId());
			folderDetails.setOwner(owner);
			
			QueryFolder queryFolder = queryFolderFactory.createQueryFolder(folderDetails);
			Set<User> newUsers = new HashSet<User>(queryFolder.getSharedWith());
			newUsers.removeAll(existing.getSharedWith());
			existing.update(queryFolder);
			
			daoFactory.getQueryFolderDao().saveOrUpdate(existing);
			
			if (!newUsers.isEmpty()) {
				sendFolderSharedEmail(user, queryFolder, newUsers);
			}
			return ResponseEvent.response(QueryFolderDetails.from(existing));			
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> deleteFolder(RequestEvent<Long> req) {
		try {
			Long folderId = req.getPayload();
			if (folderId == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.FOLDER_NOT_FOUND);
			}

			QueryFolder existing = daoFactory.getQueryFolderDao().getQueryFolder(folderId);
			if (existing == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.FOLDER_NOT_FOUND);								
			}
			
			User user = AuthUtil.getCurrentUser();			
			if (!user.isAdmin() && !existing.getOwner().equals(user)) {
				return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED);
			}
			
			daoFactory.getQueryFolderDao().deleteFolder(existing);
			return ResponseEvent.response(folderId);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SavedQueriesList> getFolderQueries(RequestEvent<ListFolderQueriesCriteria> req) {
		try {
			ListFolderQueriesCriteria crit = req.getPayload();
			QueryFolder folder = daoFactory.getQueryFolderDao().getQueryFolder(crit.folderId());
			if (folder == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.FOLDER_NOT_FOUND);
			}
			
			User user = AuthUtil.getCurrentUser();
			if (!user.isAdmin() && !folder.canUserAccess(user.getId())) {
				return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED);
			}
			
			List<SavedQuerySummary> queries = daoFactory.getSavedQueryDao().getQueriesByFolderId(
					crit.folderId(),
					crit.startAt(),
					crit.maxResults(),
					crit.query());
			
			Long count = null;
			if (crit.countReq()) {
				count = daoFactory.getSavedQueryDao().getQueriesCountByFolderId(crit.folderId(), crit.query());
			}
			
			return ResponseEvent.response(SavedQueriesList.create(queries, count));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<SavedQuerySummary>> updateFolderQueries(RequestEvent<UpdateFolderQueriesOp> req) {
		try {
			UpdateFolderQueriesOp opDetail = req.getPayload();
			
			Long folderId = opDetail.getFolderId();
			QueryFolder queryFolder = daoFactory.getQueryFolderDao().getQueryFolder(folderId);			
			if (queryFolder == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.FOLDER_NOT_FOUND);
			}
			
			User user = AuthUtil.getCurrentUser();
			if (!user.isAdmin() && !queryFolder.getOwner().equals(user)) {
				return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED);
			}
			
			List<SavedQuery> savedQueries = null;
			List<Long> queryIds = opDetail.getQueries();
			
			if (queryIds == null || queryIds.isEmpty()) {
				savedQueries = new ArrayList<>();
			} else {
				savedQueries = daoFactory.getSavedQueryDao().getQueriesByIds(queryIds);
			}
			
			switch (opDetail.getOp()) {
				case ADD:
					queryFolder.addQueries(savedQueries);
					break;
				
				case UPDATE:
					queryFolder.updateQueries(savedQueries);
					break;
				
				case REMOVE:
					queryFolder.removeQueries(savedQueries);
					break;				
			}
			
			daoFactory.getQueryFolderDao().saveOrUpdate(queryFolder);			

			List<SavedQuerySummary> result = queryFolder.getSavedQueries().stream()
				.map(SavedQuerySummary::fromSavedQuery)
				.collect(Collectors.toList());
			return ResponseEvent.response(result);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<UserSummary>> shareFolder(RequestEvent<ShareQueryFolderOp> req) {
		try {
			ShareQueryFolderOp opDetail = req.getPayload();
			
			Long folderId = opDetail.getFolderId();
			QueryFolder queryFolder = daoFactory.getQueryFolderDao().getQueryFolder(folderId);
			if (queryFolder == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.FOLDER_NOT_FOUND);
			}
			
			User user = AuthUtil.getCurrentUser();
			if (!user.isAdmin() && !queryFolder.getOwner().equals(user)) {
				return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED);
			}
			
			
			List<User> users = null;
			List<Long> userIds = opDetail.getUserIds();
			if (userIds == null || userIds.isEmpty()) {
				users = new ArrayList<>();
			} else {
				users = userDao.getUsersByIds(userIds);
			}
			
			Collection<User> newUsers = null; 
			switch (opDetail.getOp()) {
				case ADD:
					newUsers = queryFolder.addSharedUsers(users);
					break;
					
				case UPDATE:
					newUsers = queryFolder.updateSharedUsers(users);
					break;
					
				case REMOVE:
					queryFolder.removeSharedUsers(users);
					break;					
			}
											
			daoFactory.getQueryFolderDao().saveOrUpdate(queryFolder);			
			List<UserSummary> result = queryFolder.getSharedWith().stream()
				.map(UserSummary::from)
				.collect(Collectors.toList());

			if (newUsers != null && !newUsers.isEmpty()) {
				sendFolderSharedEmail(user, queryFolder, newUsers);
			}
			
			return ResponseEvent.response(result);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
    @Override
    @PlusTransactional
    public ResponseEvent<QueryAuditLogsList> getAuditLogs(RequestEvent<ListQueryAuditLogsCriteria> req){
		try {			
			Long userId = AuthUtil.getCurrentUser().getId();
			
			ListQueryAuditLogsCriteria crit = req.getPayload();
			Long savedQueryId = crit.savedQueryId();
			int startAt = crit.startAt() < 0 ? 0 : crit.startAt();
			int maxRecs = crit.maxResults() < 0 ? 0 : crit.maxResults();

			QueryAuditLogDao logDao = daoFactory.getQueryAuditLogDao();
			List<QueryAuditLogSummary> auditLogs = null;
			Long count = null;
			if (savedQueryId == null || savedQueryId == -1) {
				if (!AuthUtil.isAdmin()) {
					return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED);
				}
				
				switch (crit.type()) {
					case ALL:
						auditLogs = logDao.getAuditLogs(startAt, maxRecs);
						if (crit.countReq()) {
							count = logDao.getAuditLogsCount();
						}
						break;
						
					case LAST_24:
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.DAY_OF_MONTH, -1);
						Date intervalSt = cal.getTime();						
						Date intervalEnd = Calendar.getInstance().getTime();
						auditLogs = logDao.getAuditLogs(intervalSt, intervalEnd, startAt, maxRecs);
						if (crit.countReq()) {
							count = logDao.getAuditLogsCount(intervalSt, intervalEnd);
						}						
						break;
				}
			} else {
				auditLogs = logDao.getAuditLogs(savedQueryId, userId, startAt, maxRecs);
			}
			
			return ResponseEvent.response(QueryAuditLogsList.create(auditLogs, count));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
    }
    
    @Override
    @PlusTransactional
    public ResponseEvent<QueryAuditLogDetail> getAuditLog(RequestEvent<Long> req) {
		try {
			Long auditLogId = req.getPayload();
			QueryAuditLog queryAuditLog = daoFactory.getQueryAuditLogDao().getAuditLog(auditLogId);
			return ResponseEvent.response(QueryAuditLogDetail.from(queryAuditLog));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
    }
    
	@Override
	@PlusTransactional
	public ResponseEvent<String> getQueryDef(RequestEvent<Long> req) {
		try {
			SavedQueryDao queryDao = daoFactory.getSavedQueryDao();
			
			Long queryId = req.getPayload();			
			SavedQuery query = queryDao.getQuery(queryId);			
			if (query == null) {
				return ResponseEvent.userError(SavedQueryErrorCode.NOT_FOUND, queryId);
			}
			
			User user = AuthUtil.getCurrentUser();
			if (!query.getCreatedBy().equals(user) && 
					!queryDao.isQuerySharedWithUser(queryId, user.getId())) {
				return ResponseEvent.userError(SavedQueryErrorCode.OP_NOT_ALLOWED);
			}
			
			String queryDef = query.getQueryDefJson(true);
			return ResponseEvent.response(queryDef);			
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public QueryDataExportResult exportQueryData(final ExecuteQueryOp opDetail, final ExportProcessor processor) {
		OutputStream out = null;

		try {
			final Authentication auth = AuthUtil.getAuth();
			final String filename = getExportFilename(opDetail, processor);
			final OutputStream fout = new FileOutputStream(getExportDataDir() + File.separator + filename);
			out = fout;

			if (processor != null) {
				processor.headers(out);
			}

			Future<Boolean> result = exportThreadPool.submit(new Callable<Boolean>() {
				@Override
				@PlusTransactional
				public Boolean call() throws Exception {
					SecurityContextHolder.getContext().setAuthentication(auth);
					queryExecutor.exportData(opDetail, fout, Utility.getFieldSeparator(), this::sendEmail);
					return true;
				}

				private void sendEmail(Object status) {
					if (status instanceof Exception) {
						return;
					}

					try {
						User user = userDao.getById(AuthUtil.getCurrentUser().getId());
						if (user.isSysUser()) {
							return;
						}

						SavedQuery savedQuery = null;
						Long queryId = opDetail.getQueryId();
						if (queryId != null) {
							savedQuery = daoFactory.getSavedQueryDao().getQuery(queryId);
						}
						sendQueryDataExportedEmail(user, savedQuery, filename);
					} catch (Exception e) {
						logger.error("Error sending email with query exported data", e);
					}
				}
			});

			boolean completed = false;
			try {
				out = null;
				completed = result.get(ONLINE_EXPORT_TIMEOUT_SECS, TimeUnit.SECONDS);
				out = fout;
			} catch (TimeoutException te) {
				completed = false;
			}

			return QueryDataExportResult.create(filename, completed, result);
		} catch (OpenSpecimenException ose) {
			throw ose;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			if (out != null) {
				IOUtils.closeQuietly(out);
			}
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<FieldDetail>> getFacetValues(RequestEvent<GetFieldValuesOp> req) {
		try {
			GetFieldValuesOp op = req.getPayload();
			List<FieldDetail> result = op.getFields().stream()
				.map(facet -> getFieldDetail(op.getCpId(), facet, op.getSearchTerm()))
				.collect(Collectors.toList());
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public String insertCustomQueryForms(String dirName) {
		StringBuilder templates = new StringBuilder();
		try {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
			Resource[] resources = resolver.getResources("classpath:/query-forms/" + dirName + "/*.xml");

			for (Resource resource : resources) {
				String filename = "query-forms/" + dirName + "/" + resource.getFilename();
				templates.append(templateService.render(filename, new HashMap<>()));
			}
		} catch (Exception e) {
			logger.error("Error rendering query forms", e);
		}
		
		return templates.toString();
	}

	private SavedQuery getSavedQuery(SavedQueryDetail detail) {
		SavedQuery savedQuery = new SavedQuery();		
		savedQuery.setTitle(detail.getTitle());
		savedQuery.setCpId(detail.getCpId());
		savedQuery.setSelectList(detail.getSelectList());
		savedQuery.setFilters(detail.getFilters());
		savedQuery.setQueryExpression(detail.getQueryExpression());
		if (detail.getId() == null) {
			savedQuery.setCreatedBy(AuthUtil.getCurrentUser());
		}
		
		savedQuery.setLastUpdatedBy(AuthUtil.getCurrentUser());
		savedQuery.setLastUpdated(Calendar.getInstance().getTime());
		savedQuery.setReporting(detail.getReporting());
		return savedQuery;
	}

	private String getAql(SavedQueryDetail queryDetail) {
		return AqlBuilder.getInstance().getQuery(
				queryDetail.getSelectList(),
				queryDetail.getFilters(),
				queryDetail.getQueryExpression());
	}

	private static int getThreadPoolSize() {
		return 5; // TODO: configurable property
	}
	
	private static String getExportDataDir() {
		String dir = new StringBuilder()
			.append(ConfigUtil.getInstance().getDataDir()).append(File.separator)
			.append("query-exported-data").append(File.separator)
			.toString();
		
		File dirFile = new File(dir);
		if (!dirFile.exists()) {
			if (!dirFile.mkdirs()) {
				throw new RuntimeException("Error couldn't create directory for exporting query data");
			}
		}
		
		return dir;
	}

	private void insertAuditLog(User user, ExecuteQueryOp opDetail, QueryResponse resp) {
		QueryAuditLog auditLog = new QueryAuditLog();
		auditLog.setQueryId(opDetail.getQueryId());
		auditLog.setRunBy(user);
		auditLog.setTimeOfExecution(resp.getTimeOfExecution());
		auditLog.setTimeToFinish(resp.getExecutionTime());
		auditLog.setRunType(opDetail.getRunType());
		auditLog.setSql(resp.getSql());
		daoFactory.getQueryAuditLogDao().saveOrUpdate(auditLog);
	}

	private static void initExportFileCleaner() {
		long currentTime = Calendar.getInstance().getTime().getTime();
		
		Calendar nextDayStart = Calendar.getInstance();
		nextDayStart.set(Calendar.HOUR, 0);
		nextDayStart.set(Calendar.MINUTE, 0);
		nextDayStart.set(Calendar.SECOND, 0);
		nextDayStart.set(Calendar.MILLISECOND, 0);
		nextDayStart.add(Calendar.DATE, 1);
				
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
				new Runnable() {					
					@Override
					public void run() {
						try {
							Calendar cal = Calendar.getInstance();
							cal.add(Calendar.DAY_OF_MONTH, -30);
							long deleteBeforeTime = cal.getTimeInMillis();
							
							File dir = new File(getExportDataDir());							
							for (File file : dir.listFiles()) {								
								if (file.lastModified() <= deleteBeforeTime) {
									file.delete();
								}
							}
						} catch (Exception e) {
							
						}						
					}
				},
				nextDayStart.getTimeInMillis() - currentTime, 
				24 * 60 * 60 * 1000, 
				TimeUnit.MILLISECONDS);						
	}
	
	private void sendQueryDataExportedEmail(User user, SavedQuery query, String filename) {
		String title = query != null ? query.getTitle() : "Unsaved query";
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("user", user);
		props.put("query", query);
		props.put("filename", filename);
		props.put("appUrl", ConfigUtil.getInstance().getAppUrl());
		props.put("$subject", new String[] {title});
		
		emailService.sendEmail(QUERY_DATA_EXPORTED_EMAIL_TMPL, new String[] {user.getEmailAddress()}, props);
	}
	
	private void sendFolderSharedEmail(User user, QueryFolder folder, Collection<User> sharedUsers) {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("user", user);
		props.put("folder", folder);
		props.put("appUrl", ConfigUtil.getInstance().getAppUrl());
		
		for (User sharedWith : sharedUsers) {
			props.put("sharedWith", sharedWith);
			emailService.sendEmail(SHARE_QUERY_FOLDER_EMAIL_TMPL, new String[] {sharedWith.getEmailAddress()}, props);
		}		
	}

	private FieldDetail getFieldDetail(Long cpId, String field, String searchTerm) {
		return queryExecutor.getFieldValues(field, searchTerm, Collections.singletonMap("cpId", cpId));
	}

	private void refreshConfig() {
		maxRecsInMemory = cfgService.getIntSetting(CFG_MOD, MAX_RECS_IN_MEM, DEF_MAX_RECS_IN_MEM);
		DeConfiguration.getInstance().maxCacheElementsInMemory(maxRecsInMemory);
	}

	private String getExportFilename(ExecuteQueryOp op, ExportProcessor proc) {
		String filename = null;
		if (proc != null) {
			filename = proc.filename();
		}

		if (StringUtils.isBlank(filename)) {
			Long queryId = op.getQueryId();
			if (queryId == null) {
				queryId = 0L; // to indicate unsaved query
			}

			filename = "query_" + queryId + "_" + UUID.randomUUID().toString();
		}

		return filename;
	}

	private SavedQueryErrorCode getErrorCode(QueryException.Code error) {
		if (error == QueryException.Code.CYCLES_IN_QUERY) {
			return SavedQueryErrorCode.CYCLES_IN_QUERY;
		}

		return SavedQueryErrorCode.MALFORMED;
	}
}