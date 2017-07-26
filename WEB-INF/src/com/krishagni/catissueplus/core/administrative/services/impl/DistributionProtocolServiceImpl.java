
package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.function.Predicate;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.DpDistributionSite;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpConsentTier;
import com.krishagni.catissueplus.core.administrative.domain.DpRequirement;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.DpRequirementErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.DpRequirementFactory;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStat;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStatListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionProtocolDetail;
import com.krishagni.catissueplus.core.administrative.events.DpConsentTierDetail;
import com.krishagni.catissueplus.core.administrative.events.DpRequirementDetail;
import com.krishagni.catissueplus.core.administrative.events.DprStat;
import com.krishagni.catissueplus.core.administrative.repository.DpListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.DpRequirementDao;
import com.krishagni.catissueplus.core.administrative.repository.UserListCriteria;
import com.krishagni.catissueplus.core.administrative.services.DistributionProtocolService;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.ConsentStatement;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ConsentStatementErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ObjectStateParamsResolver;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class DistributionProtocolServiceImpl implements DistributionProtocolService, ObjectStateParamsResolver {
	
	private static final Map<String, String> attrDisplayKeys = new HashMap<String, String>() {
		{
			put("specimenType", "dist_specimen_type");
			put("anatomicSite", "dist_anatomic_site");
			put("pathologyStatus", "dist_pathology_status");
		}
	};
	
	private DaoFactory daoFactory;

	private DistributionProtocolFactory distributionProtocolFactory;
	
	private DpRequirementFactory dprFactory;

	private FormService formSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDistributionProtocolFactory(DistributionProtocolFactory distributionProtocolFactory) {
		this.distributionProtocolFactory = distributionProtocolFactory;
	}
	
	public void setDprFactory(DpRequirementFactory dprFactory) {
		this.dprFactory = dprFactory;
	}

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	private DpRequirementDao getDprDao() {
		return daoFactory.getDistributionProtocolRequirementDao();
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionProtocolDetail>> getDistributionProtocols(RequestEvent<DpListCriteria> req) {
		try {
			DpListCriteria crit = addDpListCriteria(req.getPayload());
			if (crit == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<DistributionProtocol> dps = daoFactory.getDistributionProtocolDao().getDistributionProtocols(crit);
			List<DistributionProtocolDetail> result = DistributionProtocolDetail.from(dps);
			
			if (crit.includeStat()) {
				addDpStats(result);
			}
						
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Long> getDistributionProtocolsCount(RequestEvent<DpListCriteria> req) {
		try {
			DpListCriteria crit = addDpListCriteria(req.getPayload());
			if (crit == null) {
				return ResponseEvent.response(0L);
			}

			return ResponseEvent.response(daoFactory.getDistributionProtocolDao().getDistributionProtocolsCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<DistributionProtocolDetail> getDistributionProtocol(RequestEvent<Long> req) {
		try {
			Long protocolId = req.getPayload();
			DistributionProtocol existing = getDistributionProtocol(protocolId);
			AccessCtrlMgr.getInstance().ensureReadDpRights(existing);
			return ResponseEvent.response(DistributionProtocolDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DistributionProtocolDetail> createDistributionProtocol(RequestEvent<DistributionProtocolDetail> req) {
		try {
			DistributionProtocol dp = distributionProtocolFactory.createDistributionProtocol(req.getPayload());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);
			ensureUniqueConstraints(dp, null);
			ensurePiCoordNotSame(dp);
			
			daoFactory.getDistributionProtocolDao().saveOrUpdate(dp);
			dp.addOrUpdateExtension();
			notifyDpRoleUpdated(dp);
			return ResponseEvent.response(DistributionProtocolDetail.from(dp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DistributionProtocolDetail> updateDistributionProtocol(RequestEvent<DistributionProtocolDetail> req) {
		try {
			DistributionProtocolDetail reqDetail = req.getPayload();
			DistributionProtocol existing = getDistributionProtocol(reqDetail.getId(), reqDetail.getShortTitle(), reqDetail.getTitle());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(existing);

			DistributionProtocol dp = distributionProtocolFactory.createDistributionProtocol(reqDetail);
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);
			ensureUniqueConstraints(dp, existing);
			ensurePiCoordNotSame(dp);
			
			existing.update(dp);
			daoFactory.getDistributionProtocolDao().saveOrUpdate(existing);
			existing.addOrUpdateExtension();
			return ResponseEvent.response(DistributionProtocolDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception ex) {
			return ResponseEvent.serverError(ex);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req) {
		try {
			DistributionProtocol existing = getDistributionProtocol(req.getPayload());
			return ResponseEvent.response(existing.getDependentEntities());
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionProtocolDetail>> deleteDistributionProtocols(RequestEvent<BulkDeleteEntityOp> req) {
		//
		// TODO: Force delete is not implemented
		//
		try {
			Set<Long> dpIds = req.getPayload().getIds();

			List<DistributionProtocol> dps = daoFactory.getDistributionProtocolDao().getByIds(dpIds);
			if (dpIds.size() != dps.size()) {
				dps.forEach(dp -> dpIds.remove(dp.getId()));
				throw OpenSpecimenException.userError(DistributionProtocolErrorCode.NOT_FOUND, dpIds, dpIds.size());
			}

			dps.forEach(AccessCtrlMgr.getInstance()::ensureDeleteDpRights);
			dps.forEach(DistributionProtocol::delete);
			return ResponseEvent.response(DistributionProtocolDetail.from(dps));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<DistributionProtocolDetail> updateActivityStatus(RequestEvent<DistributionProtocolDetail> req) {
		try {
			Long dpId = req.getPayload().getId();
			String status = req.getPayload().getActivityStatus();
			if (StringUtils.isBlank(status) || !Status.isValidActivityStatus(status)) {
				return ResponseEvent.userError(ActivityStatusErrorCode.INVALID);
			}
			
			DistributionProtocol existing = getDistributionProtocol(dpId);

			if (existing.getActivityStatus().equals(status)) {
				return ResponseEvent.response(DistributionProtocolDetail.from(existing));
			}
			
			if (status.equals(Status.ACTIVITY_STATUS_DISABLED.getStatus())) {
				AccessCtrlMgr.getInstance().ensureDeleteDpRights(existing);
				existing.delete();
			} else {
				AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(existing);
				existing.setActivityStatus(status);
			}

			daoFactory.getDistributionProtocolDao().saveOrUpdate(existing);
			return ResponseEvent.response(DistributionProtocolDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
		
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionOrderStat>> getOrderStats(
			RequestEvent<DistributionOrderStatListCriteria> req) {
		try {
			DistributionOrderStatListCriteria crit = req.getPayload();
			List<DistributionOrderStat> stats = getOrderStats(crit);
			
			return ResponseEvent.response(stats);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<File> exportOrderStats(RequestEvent<DistributionOrderStatListCriteria> req) {
		File tempFile = null;
		CsvWriter csvWriter = null;
		try {
			DistributionOrderStatListCriteria crit = req.getPayload();
			List<DistributionOrderStat> orderStats = getOrderStats(crit);
			
			tempFile = File.createTempFile("dp-order-stats", null);
			csvWriter = CsvFileWriter.createCsvFileWriter(tempFile);
			
			if (crit.dpId() != null && !orderStats.isEmpty()) {
				DistributionOrderStat orderStat = orderStats.get(0);
				csvWriter.writeNext(new String[] {
					MessageUtil.getInstance().getMessage("dist_dp_title"),
					orderStat.getDistributionProtocol().getTitle()
				});
			}
			
			csvWriter.writeNext(new String[] {
				MessageUtil.getInstance().getMessage("dist_exported_by"),
				AuthUtil.getCurrentUser().formattedName()
			});
			csvWriter.writeNext(new String[] {
				MessageUtil.getInstance().getMessage("dist_exported_on"),
				Utility.getDateString(Calendar.getInstance().getTime())
			});
			csvWriter.writeNext(new String[1]);
			
			String[] headers = getOrderStatsReportHeaders(crit);
			csvWriter.writeNext(headers);
			for (DistributionOrderStat stat: orderStats) {
				csvWriter.writeNext(getOrderStatsReportData(stat, crit));
			}
			
			return ResponseEvent.response(tempFile);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(csvWriter);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> getExtensionForm() {
		return ResponseEvent.response(formSvc.getExtensionInfo(-1L, DistributionProtocol.EXTN));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DpRequirementDetail>> getRequirements(RequestEvent<Long> req) {
		try {
			Long dpId = req.getPayload();
			DistributionProtocol dp = getDistributionProtocol(dpId);
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);
			
			List<DpRequirementDetail> reqDetails = DpRequirementDetail.from(dp.getRequirements());
			Map<Long, DprStat> distributionStat = getDprDao().getDistributionStatByDp(dpId);
			for (DpRequirementDetail reqDetail : reqDetails) {
				DprStat stat = distributionStat.get(reqDetail.getId());
				if (stat != null) {
					reqDetail.setDistributedCnt(stat.getDistributedCnt());
					reqDetail.setDistributedQty(stat.getDistributedQty());
				} else {
					reqDetail.setDistributedCnt(new Long(0));
					reqDetail.setDistributedQty(BigDecimal.ZERO);
				}
			}
			
			return ResponseEvent.response(reqDetails);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<DpRequirementDetail> getRequirement(RequestEvent<Long> req) {
		try {
			DpRequirement existing = getDprDao().getById(req.getPayload());
			if (existing == null) {
				return ResponseEvent.userError(DpRequirementErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(existing.getDistributionProtocol());
			return ResponseEvent.response(DpRequirementDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpRequirementDetail> createRequirement(RequestEvent<DpRequirementDetail> req) {
		try {
			DpRequirement dpr = dprFactory.createDistributionProtocolRequirement(req.getPayload());	
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dpr.getDistributionProtocol());

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureSpecimenPropertyPresent(dpr, ose);
			ensureUniqueReqConstraints(null, dpr, ose);
			ose.checkAndThrow();

			getDprDao().saveOrUpdate(dpr);
			return ResponseEvent.response(DpRequirementDetail.from(dpr));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpRequirementDetail> updateRequirement(RequestEvent<DpRequirementDetail> req) {
		try {
			Long dpReqId = req.getPayload().getId();
			DpRequirement existing = getDprDao().getById(dpReqId);
			if (existing == null) {
				return ResponseEvent.userError(DpRequirementErrorCode.NOT_FOUND);
			}

			DpRequirement newDpr = dprFactory.createDistributionProtocolRequirement(req.getPayload());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(newDpr.getDistributionProtocol());
			
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureSpecimenPropertyPresent(newDpr, ose);
			ensureUniqueReqConstraints(existing, newDpr, ose);
			ose.checkAndThrow();

			existing.update(newDpr);
			getDprDao().saveOrUpdate(existing);
			return ResponseEvent.response(DpRequirementDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpRequirementDetail> deleteRequirement(RequestEvent<Long> req) {
		try {
			DpRequirement existing = getDprDao().getById(req.getPayload());
			if (existing == null) {
				return ResponseEvent.userError(DpRequirementErrorCode.NOT_FOUND);
			}

			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(existing.getDistributionProtocol());
			existing.delete();
			getDprDao().saveOrUpdate(existing);
			return ResponseEvent.response(DpRequirementDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public String getObjectName() {
		return "distributionProtocol";
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolve(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getDistributionProtocolDao().getDpIds(key, value);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<DpConsentTierDetail>> getConsentTiers(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			DistributionProtocol dp = getDistributionProtocol(crit.getId(), crit.getName(), null);
			
			AccessCtrlMgr.getInstance().ensureReadDpRights(dp);
			return ResponseEvent.response(DpConsentTierDetail.from(dp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpConsentTierDetail> createConsentTier(RequestEvent<DpConsentTierDetail> req) {
		try {
			DpConsentTierDetail detail = req.getPayload();

			DistributionProtocol dp = getDistributionProtocol(detail.getDpId(), detail.getDpShortTitle(), detail.getDpTitle());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);

			ensureUniqueConsentStatement(detail, dp);
			ConsentStatement stmt = getStatement(detail.getStatementId(), detail.getStatementCode(), detail.getStatement());

			DpConsentTier tier = dp.addConsentTier(getConsentTierObj(detail.getId(), stmt));
			daoFactory.getDistributionProtocolDao().saveOrUpdate(dp, true);
			return ResponseEvent.response(DpConsentTierDetail.from(tier));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DpConsentTierDetail> updateConsentTier(RequestEvent<DpConsentTierDetail> req) {
		try {
			DpConsentTierDetail detail = req.getPayload();

			DistributionProtocol dp = getDistributionProtocol(detail.getDpId(), detail.getDpShortTitle(), detail.getDpTitle());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);

			ensureUniqueConsentStatement(detail, dp);
			ConsentStatement stmt = getStatement(detail.getStatementId(), detail.getStatementCode(), detail.getStatement());

			DpConsentTier tier = dp.updateConsentTier(getConsentTierObj(detail.getId(), stmt));
			return ResponseEvent.response(DpConsentTierDetail.from(tier));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<DpConsentTierDetail> deleteConsentTier(RequestEvent<DpConsentTierDetail> req) {
		try {
			DpConsentTierDetail detail = req.getPayload();
			DistributionProtocol dp = getDistributionProtocol(detail.getDpId(), detail.getDpShortTitle(), detail.getDpTitle());
			AccessCtrlMgr.getInstance().ensureCreateUpdateDpRights(dp);

			DpConsentTier tier = dp.removeConsentTier(detail.getId());
			return ResponseEvent.response(DpConsentTierDetail.from(tier));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private DpListCriteria addDpListCriteria(DpListCriteria crit) {
		Set<Long> siteIds = AccessCtrlMgr.getInstance().getReadAccessDistributionProtocolSites();
		if (siteIds != null && CollectionUtils.isEmpty(siteIds)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (StringUtils.isNotBlank(crit.cpShortTitle())) {
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(crit.cpShortTitle());
			if (cp == null) {
				return null;
			}

			Set<Long> cpSiteIds = Utility.collect(cp.getSites(), "site.id", true);
			if (siteIds != null) {
				siteIds = new HashSet<>(CollectionUtils.intersection(siteIds, cpSiteIds));
				if (siteIds.isEmpty()) {
					return null;
				}
			} else {
				siteIds = cpSiteIds;
			}
		}

		if (siteIds != null) {
			crit.siteIds(siteIds);
		}
		
		return crit;
	}

	private void ensureSpecimenPropertyPresent(DpRequirement dpr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(dpr.getSpecimenType()) && StringUtils.isBlank(dpr.getAnatomicSite()) &&
			CollectionUtils.isEmpty(dpr.getPathologyStatuses()) && StringUtils.isBlank(dpr.getClinicalDiagnosis())) {
			ose.addError(DpRequirementErrorCode.SPEC_PROPERTY_REQUIRED);
		}
	}

	private void ensureUniqueReqConstraints(DpRequirement oldDpr, DpRequirement newDpr, OpenSpecimenException ose) {
		if (oldDpr != null && oldDpr.equalsSpecimenGroup(newDpr)) {
			return;
		}
		
		DistributionProtocol dp = newDpr.getDistributionProtocol();
		if (dp.hasRequirement(newDpr.getSpecimenType(), newDpr.getAnatomicSite(), newDpr.getPathologyStatuses(),
			newDpr.getClinicalDiagnosis())) {
			ose.addError(DpRequirementErrorCode.ALREADY_EXISTS);
		}
	}
	
	private void ensurePiCoordNotSame(DistributionProtocol dp) {
		if (!dp.getCoordinators().contains(dp.getPrincipalInvestigator())) {
			return;
		}

		throw OpenSpecimenException.userError(
			DistributionProtocolErrorCode.PI_COORD_CANNOT_BE_SAME,
			dp.getPrincipalInvestigator().formattedName());
	}
	
	private void addDpStats(List<DistributionProtocolDetail> dps) {
		if (CollectionUtils.isEmpty(dps)) {
			return;
		}
		
		Map<Long, DistributionProtocolDetail> dpMap = new HashMap<Long, DistributionProtocolDetail>();
		for (DistributionProtocolDetail dp : dps) {
			dpMap.put(dp.getId(), dp);
		}
				
		Map<Long, Integer> countMap = daoFactory.getDistributionProtocolDao().getSpecimensCountByDpIds(dpMap.keySet());		
		for (Map.Entry<Long, Integer> count : countMap.entrySet()) {
			dpMap.get(count.getKey()).setDistributedSpecimensCount(count.getValue());
		}		
	}
	
	private void ensureUniqueConstraints(DistributionProtocol newDp, DistributionProtocol existingDp) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		
		if (!isUniqueTitle(newDp, existingDp)) {
			ose.addError(DistributionProtocolErrorCode.DUP_TITLE, newDp.getTitle());
		}
		
		if (!isUniqueShortTitle(newDp, existingDp)) {
			ose.addError(DistributionProtocolErrorCode.DUP_SHORT_TITLE, newDp.getShortTitle());
		}
		
		ose.checkAndThrow();
	}
	
	private boolean isUniqueTitle(DistributionProtocol newDp, DistributionProtocol existingDp) {
		if (existingDp != null && newDp.getTitle().equals(existingDp.getTitle())) {
			return true;
		}
		
		DistributionProtocol existing = daoFactory.getDistributionProtocolDao().getDistributionProtocol(newDp.getTitle());
		if (existing != null) {
			return false;
		}
		
		return true;
	}

	private boolean isUniqueShortTitle(DistributionProtocol newDp, DistributionProtocol existingDp) {
		if (existingDp != null && newDp.getShortTitle().equals(existingDp.getShortTitle())) {
			return true;
		}
		
		DistributionProtocol existing = daoFactory.getDistributionProtocolDao().getByShortTitle(newDp.getShortTitle());
		if (existing != null) {
			return false;
		}
		
		return true;
	}

	private List<DistributionOrderStat> getOrderStats(DistributionOrderStatListCriteria crit) {
		if (crit.dpId() != null) {
			DistributionProtocol dp = getDistributionProtocol(crit.dpId());
			AccessCtrlMgr.getInstance().ensureReadDpRights(dp);
		} else {
			Set<Long> siteIds = AccessCtrlMgr.getInstance().getCreateUpdateAccessDistributionOrderSiteIds();
			if (siteIds != null && CollectionUtils.isEmpty(siteIds)) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
			
			if (siteIds != null) {
				crit.siteIds(siteIds);
			}
		}
		
		return daoFactory.getDistributionProtocolDao().getOrderStats(crit);
	}
	
	private String[] getOrderStatsReportHeaders(DistributionOrderStatListCriteria crit) {
		List<String> headers = new ArrayList<String>();
		if (crit.dpId() == null) {
			headers.add(MessageUtil.getInstance().getMessage("dist_dp_title"));
		}
		
		headers.add(MessageUtil.getInstance().getMessage("dist_order_name"));
		headers.add(MessageUtil.getInstance().getMessage("dist_distribution_date"));
		for (String attr: crit.groupByAttrs()) {
			headers.add(MessageUtil.getInstance().getMessage(attrDisplayKeys.get(attr)));
		}
		
		headers.add(MessageUtil.getInstance().getMessage("dist_specimen_distributed"));
		return headers.toArray(new String[0]);
	}
	
	private String [] getOrderStatsReportData(DistributionOrderStat stat, DistributionOrderStatListCriteria crit) {
		List<String> data = new ArrayList<String>();
		if (crit.dpId() == null) {
			data.add(stat.getDistributionProtocol().getShortTitle());
		}
		
		data.add(stat.getName());
		data.add(Utility.getDateString(stat.getExecutionDate()));
		for (String attr: crit.groupByAttrs()) {
			data.add(stat.getGroupByAttrVals().get(attr).toString());
		}
		
		data.add(stat.getDistributedSpecimenCount().toString());
		
		return data.toArray(new String[0]);
	}

	private DistributionProtocol getDistributionProtocol(Long id) {
		return getDistributionProtocol(id, null, null);
	}
	
	private DistributionProtocol getDistributionProtocol(Long id, String dpShortTitle, String dpTitle) {
		DistributionProtocol dp = null;
		Object key = null;

		if (id != null) {
			dp = daoFactory.getDistributionProtocolDao().getById(id);
			key = id;
		} else if (StringUtils.isNotBlank(dpTitle)) {
			dp = daoFactory.getDistributionProtocolDao().getDistributionProtocol(dpTitle);
			key = dpTitle;
		} else if (StringUtils.isNotBlank(dpShortTitle)) {
			dp = daoFactory.getDistributionProtocolDao().getByShortTitle(dpShortTitle);
			key = dpShortTitle;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.DP_REQUIRED);
		} else if (dp == null) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.NOT_FOUND, key, 1);
		}
		
		return dp;
	}

	private ConsentStatement getStatement(Long id, String code, String statement) {
		ConsentStatement stmt = null;
		Object key = null;

		if (id != null) {
			key = id;
			stmt = daoFactory.getConsentStatementDao().getById(id);
		} else if (StringUtils.isNotBlank(code)) {
			key = code;
			stmt = daoFactory.getConsentStatementDao().getByCode(code);
		} else if (StringUtils.isNotBlank(statement)) {
			key = statement;
			stmt = daoFactory.getConsentStatementDao().getByStatement(statement);
		}

		if (key == null) {
			throw OpenSpecimenException.userError(ConsentStatementErrorCode.CODE_REQUIRED);
		} else if (stmt == null) {
			throw OpenSpecimenException.userError(ConsentStatementErrorCode.NOT_FOUND, key);
		}

		return stmt;
	}

	private DpConsentTier getConsentTierObj(Long id, ConsentStatement stmt) {
		DpConsentTier tier = new DpConsentTier();
		tier.setId(id);
		tier.setStatement(stmt);
		return tier;
	}

	private void ensureUniqueConsentStatement(DpConsentTierDetail consentTierDetail, DistributionProtocol dp) {
		Predicate<DpConsentTier> findFn;
		if (consentTierDetail.getStatementId() != null) {
			findFn = (t) -> t.getStatement().getId().equals(consentTierDetail.getStatementId());
		} else if (StringUtils.isNotBlank(consentTierDetail.getStatementCode())) {
			findFn = (t) -> t.getStatement().getCode().equals(consentTierDetail.getStatementCode());
		} else if (StringUtils.isNotBlank(consentTierDetail.getStatement())) {
			findFn = (t) -> t.getStatement().getStatement().equals(consentTierDetail.getStatement());
		} else {
			throw OpenSpecimenException.userError(ConsentStatementErrorCode.CODE_REQUIRED);
		}

		DpConsentTier tier = dp.getConsentTiers().stream().filter(findFn).findFirst().orElse(null);
		if (tier != null && !tier.getId().equals(consentTierDetail.getId())) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.DUP_CONSENT, tier.getStatement().getCode(), dp.getShortTitle());
		}
	}

	private void notifyDpRoleUpdated(DistributionProtocol dp) {
		Map<String, Object> props = new HashMap<>();
		props.put("dp", dp);
		props.put("instituteSitesMap", DpDistributionSite.getInstituteSitesMap(dp.getDistributingSites()));

		notifyDpRoleUpdated(Collections.singletonList(dp.getPrincipalInvestigator()), dp, props, null, N_USER, N_DP_PI);
		notifyDpRoleUpdated(dp.getCoordinators(), dp, props, null, N_USER, N_DP_COORD);

		notifyDpRoleUpdated(getInstituteAdmins(dp.getInstitute()), dp, props, dp.getInstitute().getName(), N_INST_ADMIN, N_DP_RECV_SITE);
		if (dp.getDefReceivingSite() != null) {
			notifyDpRoleUpdated(dp.getDefReceivingSite().getCoordinators(), dp, props, dp.getDefReceivingSite().getName(), N_SITE_ADMIN, N_DP_RECV_SITE);
		}

		for (DpDistributionSite distSite : dp.getDistributingSites()) {
			if (distSite.getSite() != null) {
				notifyDpRoleUpdated(distSite.getSite().getCoordinators(), dp, props, distSite.getSite().getName(), 1, 1);
			}
		}
	}

	private void notifyDpRoleUpdated(
		Collection<User> notifyUsers,
		DistributionProtocol dp,
		Map<String, Object> props,
		String instSiteName,
		int userOrSiteOrInst, // 0: user, 1: site, 2: institute
		int roleChoice) {     // for users -  1: PI / 2: Coordinator, for others - 1: distributing / 2: receiving

		if (CollectionUtils.isEmpty(notifyUsers)) {
			return;
		}

		String notifMessage = getNotifMsg(dp.getShortTitle(), instSiteName, userOrSiteOrInst, roleChoice);
		props.put("emailText", notifMessage);
		for (User rcpt : notifyUsers) {
			props.put("rcpt", rcpt);
			EmailUtil.getInstance().sendEmail(ROLE_UPDATED_EMAIL_TMPL, new String[] {rcpt.getEmailAddress()}, null, props);
		}

		Notification notif = new Notification();
		notif.setEntityType(DistributionProtocol.getEntityName());
		notif.setEntityId(dp.getId());
		notif.setOperation("UPDATE");
		notif.setCreatedBy(AuthUtil.getCurrentUser());
		notif.setCreationTime(Calendar.getInstance().getTime());
		notif.setMessage(notifMessage);
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("dp-overview", notifyUsers));
	}

	private String getNotifMsg(String shortTitle, String instSiteName, int siteOrInst, int roleChoice) {
		String msgKey;
		Object[] params;
		if (siteOrInst == 0) {
			msgKey = "dp_user_notif_role";
			params = new Object[] { roleChoice, shortTitle};
		} else {
			msgKey = "dp_site_inst_notif";
			params = new Object[] {siteOrInst, instSiteName, roleChoice, shortTitle};
		}

		return MessageUtil.getInstance().getMessage(msgKey, params);
	}

	private List<User> getInstituteAdmins(Institute institute) {
		return daoFactory.getUserDao().getUsers(
			new UserListCriteria()
				.instituteName(institute.getName())
				.type("INSTITUTE")
				.activityStatus("Active")
		);
	}

	private static final String ROLE_UPDATED_EMAIL_TMPL = "users_dp_role_updated";

	private static final int N_USER = 0;

	private static final int N_SITE_ADMIN = 1;

	private static final int N_INST_ADMIN = 2;

	private static final int N_DP_PI = 1;

	private static final int N_DP_COORD = 2;

	private static final int N_DP_DIST_SITE = 1;

	private static final int N_DP_RECV_SITE = 2;
}
