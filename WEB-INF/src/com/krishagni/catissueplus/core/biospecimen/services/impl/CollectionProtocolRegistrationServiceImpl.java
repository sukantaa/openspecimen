
package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.AnonymizeEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.ConsentResponses;
import com.krishagni.catissueplus.core.biospecimen.domain.ConsentTierResponse;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CollectionProtocolRegistrationFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ConsentResponsesFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.BulkRegistrationsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpEntityDeleteCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipantsList;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantRegistrationsList;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.events.RegistrationQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSpecimensQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.CprListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.Anonymizer;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.services.ParticipantService;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenKitService;
import com.krishagni.catissueplus.core.biospecimen.services.VisitService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.impl.ConfigurationServiceImpl;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class CollectionProtocolRegistrationServiceImpl implements CollectionProtocolRegistrationService, ObjectAccessor, InitializingBean {
	private Log logger = LogFactory.getLog(CollectionProtocolRegistrationServiceImpl.class);

	private DaoFactory daoFactory;

	private CollectionProtocolRegistrationFactory cprFactory;
	
	private ConsentResponsesFactory consentResponsesFactory;
	
	private ParticipantService participantService;

	private VisitService visitSvc;
	
	private ConfigurationServiceImpl cfgSvc;

	private LabelGenerator labelGenerator;

	private Anonymizer<CollectionProtocolRegistration> anonymizer;

	private SpecimenKitService specimenKitSvc;

	private ExportService exportSvc;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setCprFactory(CollectionProtocolRegistrationFactory cprFactory) {
		this.cprFactory = cprFactory;
	}
	
	public void setConsentResponsesFactory(ConsentResponsesFactory consentResponsesFactory) {
		this.consentResponsesFactory = consentResponsesFactory;
	}

	public void setParticipantService(ParticipantService participantService) {
		this.participantService = participantService;
	}

	public void setVisitSvc(VisitService visitSvc) {
		this.visitSvc = visitSvc;
	}

	public void setCfgSvc(ConfigurationServiceImpl cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setLabelGenerator(LabelGenerator labelGenerator) {
		this.labelGenerator = labelGenerator;
	}

	public void setAnonymizer(Anonymizer<CollectionProtocolRegistration> anonymizer) {
		this.anonymizer = anonymizer;
	}

	public void setSpecimenKitSvc(SpecimenKitService specimenKitSvc) {
		this.specimenKitSvc = specimenKitSvc;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolRegistrationDetail> getRegistration(RequestEvent<RegistrationQueryCriteria> req) {				
		try {			
			RegistrationQueryCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getCprId(), crit.getCpId(), crit.getPpid());
			boolean allowPhiAccess = AccessCtrlMgr.getInstance().ensureReadCprRights(cpr);

			List<CollectionProtocolRegistration> otherCprs = getOtherCprs(cpr);
			return ResponseEvent.response(CollectionProtocolRegistrationDetail.from(cpr, !allowPhiAccess, otherCprs));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolRegistrationDetail> createRegistration(RequestEvent<CollectionProtocolRegistrationDetail> req) {
		try {
			return ResponseEvent.response(saveOrUpdateRegistration(req.getPayload(), null, true));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolRegistrationDetail> updateRegistration(RequestEvent<CollectionProtocolRegistrationDetail> req) {
		try {
			CollectionProtocolRegistrationDetail detail = req.getPayload();
			CollectionProtocolRegistration existing = getCpr(detail.getId(), detail.getCpId(), detail.getCpShortTitle(), detail.getPpid());
			return ResponseEvent.response(saveOrUpdateRegistration(detail, existing, true));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolRegistrationDetail>> bulkRegistration(RequestEvent<BulkRegistrationsDetail> req) {
		try {
			BulkRegistrationsDetail detail = req.getPayload();
			String collectionSite = detail.getCollectionSite();
			if (StringUtils.isBlank(collectionSite)) {
				throw OpenSpecimenException.userError(CprErrorCode.COLLECTION_SITE_REQUIRED);
			}

			Date regDate = Calendar.getInstance().getTime();
			List<CollectionProtocolRegistration> cprs = new ArrayList<>();
			List<CollectionProtocolRegistrationDetail> result = new ArrayList<>();
			List<CollectionProtocolEvent> events = getCpes(detail);
			for (int i = 0; i < detail.getRegCount(); i++) {
				CollectionProtocolRegistration cpr = registerParticipant(detail, regDate, collectionSite, events, i == 0);
				cprs.add(cpr);
				result.add(CollectionProtocolRegistrationDetail.from(cpr, false));
			}

			if (detail.getKitDetail() != null && !cprs.isEmpty()) {
				List<Specimen> spmns = cprs.stream().flatMap(cpr -> cpr.getOrderedVisits().stream())
					.flatMap(visit -> visit.getSpecimens().stream())
					.filter(Specimen::isPrePrintEnabled)
					.collect(Collectors.toList());
				detail.getKitDetail().setCpId(result.get(0).getCpId());
				detail.getKitDetail().setReceivingSite(collectionSite);
				specimenKitSvc.createSpecimenKit(detail.getKitDetail(), spmns);
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception ex) {
			return ResponseEvent.serverError(ex);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolRegistrationDetail> anonymize(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			RegistrationQueryCriteria detail = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(detail.getCprId(), detail.getCpId(), null, detail.getPpid());
			raiseErrorIfSpecimenCentric(cpr);
			AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);
			anonymizer.anonymize(cpr);

			AnonymizeEvent event = new AnonymizeEvent();
			event.setCpr(cpr);
			event.setAnonymizedBy(AuthUtil.getCurrentUser());
			event.setAnonymizeTime(Calendar.getInstance().getTime());
			daoFactory.getAnonymizeEventDao().saveOrUpdate(event);

			return ResponseEvent.response(CollectionProtocolRegistrationDetail.from(cpr, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			RegistrationQueryCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getCprId(), crit.getCpId(), crit.getPpid());
			AccessCtrlMgr.getInstance().ensureReadCprRights(cpr);
			return ResponseEvent.response(cpr.getDependentEntities());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolRegistrationDetail> deleteRegistration(RequestEvent<CpEntityDeleteCriteria> req) {
		try {
			CpEntityDeleteCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getId(), null, crit.getCpShortTitle(), crit.getName());
			raiseErrorIfSpecimenCentric(cpr);
			AccessCtrlMgr.getInstance().ensureDeleteCprRights(cpr);
			cpr.delete(!crit.isForceDelete());
			return ResponseEvent.response(CollectionProtocolRegistrationDetail.from(cpr, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}	
	
	@Override
	@PlusTransactional
	public ResponseEvent<File> getConsentForm(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			Long cprId = req.getPayload().getCprId();
			CollectionProtocolRegistration existing = daoFactory.getCprDao().getById(cprId);
			if (existing == null) {
				return ResponseEvent.userError(CprErrorCode.NOT_FOUND);
			}
			
			boolean hasPhiAccess = AccessCtrlMgr.getInstance().ensureReadCprRights(existing);
			if (!hasPhiAccess) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}
			
			String fileName = existing.getSignedConsentDocumentName();
			if (StringUtils.isBlank(fileName)) {
				return ResponseEvent.userError(CprErrorCode.CONSENT_FORM_NOT_FOUND);
			}
			
			File file = new File(ConfigParams.getConsentsDirPath() + fileName);
			if (!file.exists()) {
				return ResponseEvent.userError(CprErrorCode.CONSENT_FORM_NOT_FOUND);
			}
			
			return ResponseEvent.response(file);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<String> uploadConsentForm(RequestEvent<FileDetail> req) {
		OutputStream outputStream = null;
		try {
			FileDetail detail = req.getPayload();

			Long cprId = detail.getId();
			String cpShortTitle = null, ppid = null;
			if (detail.getObjectProps() != null) {
				cpShortTitle = (String) detail.getObjectProps().get("cpShortTitle");
				ppid         = (String) detail.getObjectProps().get("ppid");
			}

			CollectionProtocolRegistration existing = getCpr(cprId, null, cpShortTitle, ppid);
			raiseErrorIfSpecimenCentric(existing);
			AccessCtrlMgr.getInstance().ensureUpdateCprRights(existing);
			
			if (existing.getCollectionProtocol().isConsentsWaived()) {
				return ResponseEvent.userError(CpErrorCode.CONSENTS_WAIVED, existing.getCollectionProtocol().getShortTitle());
			}
			
			String newFileName = UUID.randomUUID() + "_" + detail.getFilename();
			File newFile = new File(ConfigParams.getConsentsDirPath() + newFileName);
			
			outputStream = new FileOutputStream(newFile);
			IOUtils.copy(detail.getFileIn(), outputStream);
			
			String oldFileName = existing.getSignedConsentDocumentName();
			if (StringUtils.isNotBlank(oldFileName)) {
				File oldFile = new File(ConfigParams.getConsentsDirPath() + oldFileName);
				if (oldFile.exists()) {
					oldFile.delete();
				}
 			}
			existing.setSignedConsentDocumentName(newFileName);
			return ResponseEvent.response(detail.getFilename());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> deleteConsentForm(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			Long cprId = req.getPayload().getCprId();
			CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(cprId);
			if (cpr == null) {
				return ResponseEvent.userError(CprErrorCode.NOT_FOUND);
			}

			raiseErrorIfSpecimenCentric(cpr);
			AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);

			String fileName = cpr.getSignedConsentDocumentName();
			if (StringUtils.isBlank(fileName)) {
				return ResponseEvent.userError(CprErrorCode.CONSENT_FORM_NOT_FOUND);
			}
			
			File file = new File(ConfigParams.getConsentsDirPath() + fileName);
			if (!file.exists()) {
				return ResponseEvent.userError(CprErrorCode.CONSENT_FORM_NOT_FOUND);
			}
			
			boolean isFileDeleted = file.delete();
			if (isFileDeleted) {
				cpr.setSignedConsentDocumentName(null);
			} 

			return ResponseEvent.response(isFileDeleted);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ConsentDetail> getConsents(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			RegistrationQueryCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getCprId(), crit.getCpId(), crit.getPpid());
			boolean hasPhiAccess = AccessCtrlMgr.getInstance().ensureReadCprRights(cpr);
			return ResponseEvent.response(ConsentDetail.fromCpr(cpr, !hasPhiAccess));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ConsentDetail> saveConsents(RequestEvent<ConsentDetail> req) {
		try {
			ConsentDetail consentDetail = req.getPayload();

			CollectionProtocolRegistration existing = getCpr(consentDetail.getCprId(),
				consentDetail.getCpId(), consentDetail.getCpShortTitle(), consentDetail.getPpid());
			raiseErrorIfSpecimenCentric(existing);
			boolean hasPhiAccess = AccessCtrlMgr.getInstance().ensureUpdateCprRights(existing);
			
			ConsentResponses consentResponses = consentResponsesFactory.createConsentResponses(existing, consentDetail);
			existing.updateConsents(consentResponses);
			return ResponseEvent.response(ConsentDetail.fromCpr(existing, !hasPhiAccess));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<VisitSummary>> getVisits(RequestEvent<VisitsListCriteria> req) {
		try {
			CollectionProtocolRegistration cpr = getCpr(req.getPayload().cprId(), null, null);
			AccessCtrlMgr.getInstance().ensureReadVisitRights(cpr, false);
			return ResponseEvent.response(daoFactory.getVisitsDao().getVisits(req.getPayload()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<VisitDetail> getLatestVisit(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			RegistrationQueryCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getCprId(), crit.getCpId(), crit.getPpid());
			AccessCtrlMgr.getInstance().ensureReadVisitRights(cpr, false);

			Visit visit = daoFactory.getVisitsDao().getLatestVisit(cpr.getId());
			return ResponseEvent.response(visit != null ? VisitDetail.from(visit) : null);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenDetail>> getSpecimens(RequestEvent<VisitSpecimensQueryCriteria> req) {
		VisitSpecimensQueryCriteria crit = req.getPayload();
		
		try {
			CollectionProtocolRegistration cpr = getCpr(req.getPayload().getCprId(), null, null);
			AccessCtrlMgr.getInstance().ensureReadSpecimenRights(cpr, false);

			List<SpecimenDetail> specimens = Collections.emptyList();			
			if (crit.getVisitId() != null) {
				specimens = getSpecimensByVisit(crit.getCprId(), crit.getVisitId());
				checkDistributedSpecimens(specimens);
			} else if (crit.getEventId() != null) {
				specimens = getAnticipatedSpecimens(crit.getCprId(), crit.getEventId());
			}

			return ResponseEvent.response(specimens);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ParticipantRegistrationsList> createRegistrations(RequestEvent<ParticipantRegistrationsList> req) {
		try {
			return ResponseEvent.response(saveOrUpdateRegistrations(req.getPayload(), false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ParticipantRegistrationsList> updateRegistrations(RequestEvent<ParticipantRegistrationsList> req) {
		try {
			return ResponseEvent.response(saveOrUpdateRegistrations(req.getPayload(), true));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	public String getObjectName() {
		return CollectionProtocolRegistration.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getCprDao().getCprIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "CATISSUE_COLL_PROT_REG_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		AccessCtrlMgr.getInstance().ensureReadCprRights(id);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		exportSvc.registerObjectsGenerator("cpr", this::getCprsGenerator);
		exportSvc.registerObjectsGenerator("consent", this::getConsentsGenerator);
	}

	private CollectionProtocolRegistrationDetail saveOrUpdateRegistration(
		CollectionProtocolRegistrationDetail input,
		CollectionProtocolRegistration existing,
		boolean saveParticipant) {

		CollectionProtocolRegistration cpr = saveOrUpdateRegistration(input, existing, null, null, saveParticipant);
		return CollectionProtocolRegistrationDetail.from(cpr, false);
	}

	private CollectionProtocolRegistration saveOrUpdateRegistration(
		CollectionProtocolRegistrationDetail input,
		CollectionProtocolRegistration existing,
		List<CollectionProtocolEvent> cpes,
		String collectionSite,
		boolean saveParticipant) {

		CollectionProtocolRegistration cpr = cprFactory.createCpr(existing, input);
		raiseErrorIfSpecimenCentric(cpr);

		if (existing == null) {
			AccessCtrlMgr.getInstance().ensureCreateCprRights(cpr);
		} else {
			AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);
		}

		return saveOrUpdateRegistration(cpr, existing, cpes, collectionSite, saveParticipant);
	}

	private CollectionProtocolRegistration saveOrUpdateRegistration(
		CollectionProtocolRegistration cpr,
		CollectionProtocolRegistration existing,
		List<CollectionProtocolEvent> cpes,
		String collectionSite,
		boolean saveParticipant) {

		raiseErrorIfSpecimenCentric(cpr);

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureValidAndUniquePpid(existing, cpr, ose);
		ensureUniqueBarcode(existing, cpr, ose);
		if (existing == null) {
			ensureUniqueParticipantReg(cpr, ose);
		}
		
		ose.checkAndThrow();
		
		if (saveParticipant && cpr.isActive()) {
			saveParticipant(existing, cpr);
		}

		if (existing != null) {
			existing.update(cpr);
			cpr = existing;
		}
		
		cpr.setPpidIfEmpty();
		daoFactory.getCprDao().saveOrUpdate(cpr);

		if (existing == null && cpr.isSpecimenLabelPrePrintOnRegEnabled()) {
			addVisits(cpr, cpes == null ? cpr.getCollectionProtocol().getOrderedCpeList() : cpes, collectionSite);
		}

		return cpr;
	}

	private ParticipantRegistrationsList saveOrUpdateRegistrations(ParticipantRegistrationsList input, boolean update) {
		ParticipantDetail inputParticipant = input.getParticipant();
		if (inputParticipant == null) {
			inputParticipant = new ParticipantDetail();
		}

		if (update) {
			Participant existing = getParticipant(input);
			if (Status.isDisabledStatus(inputParticipant.getActivityStatus())) {
				return deleteParticipant(existing, inputParticipant.isForceDelete());
			}

			AccessCtrlMgr.getInstance().ensureUpdateParticipantRights(existing);
			inputParticipant.setId(existing.getId());
		}

		//
		// Step 1: Save/update participant
		//
		ParticipantDetail participantDetail = participantService.saveOrUpdateParticipant(inputParticipant);
		Participant participant = daoFactory.getParticipantDao().getById(participantDetail.getId());

		ParticipantDetail p = new ParticipantDetail();
		p.setId(participantDetail.getId());

		//
		// Step 2: Build a map of participant registrations
		//
		Map<String, CollectionProtocolRegistration> cprMap = participant.getCprs().stream()
			.collect(Collectors.toMap(reg -> reg.getCpShortTitle() + "_" + reg.getPpid(), reg -> reg));

		//
		// Step 3: Run through each registration
		//
		List<CollectionProtocolRegistrationDetail> registrations = new ArrayList<>();
		for (CollectionProtocolRegistrationDetail cprDetail : input.getRegistrations()) {
			cprDetail.setParticipant(p);

			CollectionProtocolRegistration existing = null;
			if (StringUtils.isNotBlank(cprDetail.getCpShortTitle()) && StringUtils.isNotBlank(cprDetail.getPpid())) {
				existing = cprMap.get(cprDetail.getCpShortTitle() + "_" + cprDetail.getPpid());
			}

			cprDetail = saveOrUpdateRegistration(cprDetail, existing, false);

			cprDetail.setParticipant(null);
			registrations.add(cprDetail);
		}

		ParticipantRegistrationsList result = new ParticipantRegistrationsList();
		result.setParticipant(participantDetail);
		result.setRegistrations(registrations);
		return result;
	}

	private void saveParticipant(CollectionProtocolRegistration existing, CollectionProtocolRegistration cpr) {		
		Participant existingParticipant = null;
		Participant participant = cpr.getParticipant();
		
		if (existing == null) {
			// new registration
			if (participant.getId() != null) { 
				// existing participant
				existingParticipant = daoFactory.getParticipantDao().getById(participant.getId());
			} else {
				// new participant
			}
		} else { 
			// existing reg, therefore it has to be existing participant
			existingParticipant = mergeParticipant(existing, cpr);
		}
		
		if (existingParticipant != null) {
			participantService.updateParticipant(existingParticipant, participant);
			cpr.setParticipant(existingParticipant);
		} else {
			participantService.createParticipant(participant);
			cpr.setParticipant(participant);
		}
	}
	
	private Participant mergeParticipant(CollectionProtocolRegistration existingCpr, CollectionProtocolRegistration inputCpr) {
		if (existingCpr.getParticipant().equals(inputCpr.getParticipant())) {
			//
			// No change in participant between registration updates. Do nothing
			//
			return existingCpr.getParticipant();
		}
		
		//
		// we’ve been asked to use participant in inputCpr for updating existingCpr
		//
		// Two broad cases:
		// Case #1: Participants are registered to non-overlapping protocols. 
		// e.g. {cp1, cp4} and {cp2, cp3}
		// In this case, we need to only point registration to appropriate participant
		//
		// Case #2: Participants are registered to overlapping protocols. 
		// e.g. {cp1, cp2, cp4} and {cp2, cp3, cp4}
		// In this case, we need to transfer visits from source participant’s overlapping CPRs 
		// to target participant’s corresponding CPRs
		//
		//
		
		Participant srcParticipant = existingCpr.getParticipant();
		Participant tgtParticipant = null;
		if (inputCpr.getParticipant().getId() != null && inputCpr.getParticipant().getId() != -1L) {
			tgtParticipant = daoFactory.getParticipantDao().getById(inputCpr.getParticipant().getId());
		} else {
			tgtParticipant = inputCpr.getParticipant();
		}

		Set<CollectionProtocolRegistration> tgtCprs = tgtParticipant.getCprs();
		for (CollectionProtocolRegistration srcCpr : srcParticipant.getCprs()) {
			CollectionProtocolRegistration mergedCpr = mergeCpr(srcCpr, tgtCprs);
			if (mergedCpr == null) {
				// case 1
				srcCpr.setParticipant(tgtParticipant);
			} else if (mergedCpr.getCollectionProtocol().equals(existingCpr.getCollectionProtocol())){
				// case 2
				// marking input CPR should not be used
				inputCpr.setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
			}
		}
		
		srcParticipant.getCprs().clear();
		if (srcParticipant.isActive()) {
			srcParticipant.delete();
		}

		if (tgtParticipant.getId() == null) {
			//
			// participant might be sourced from external repository
			//
			daoFactory.getParticipantDao().saveOrUpdate(tgtParticipant);
		}

		return tgtParticipant;
	}
	
	private CollectionProtocolRegistration mergeCpr(CollectionProtocolRegistration srcCpr, Set<CollectionProtocolRegistration> tgtCprs) {
		try {
			AccessCtrlMgr.getInstance().ensureUpdateCprRights(srcCpr);
		} catch (OpenSpecimenException ose) {
			throw OpenSpecimenException.userError(CprErrorCode.CANNOT_MERGE_PARTICIPANT, srcCpr.getCollectionProtocol().getShortTitle());
		}
		
		for (CollectionProtocolRegistration tgtCpr : tgtCprs) {
			if (srcCpr.getCollectionProtocol().equals(tgtCpr.getCollectionProtocol())) {
				tgtCpr.addVisits(srcCpr.getVisits());
				srcCpr.getVisits().clear();
				srcCpr.delete();
				return tgtCpr;
			}
		}
		
		return null;
	}

	private Participant getParticipant(ParticipantRegistrationsList input) {
		for (CollectionProtocolRegistrationDetail cprDetail : input.getRegistrations()) {
			String cpShortTitle = cprDetail.getCpShortTitle();
			String ppid         = cprDetail.getPpid();

			if (StringUtils.isNotBlank(cpShortTitle) && StringUtils.isNotBlank(ppid)) {
				CollectionProtocolRegistration cpr = daoFactory.getCprDao().getCprByCpShortTitleAndPpid(cpShortTitle, ppid);
				if (cpr != null) {
					return cpr.getParticipant();
				}
			}
		}

		return daoFactory.getParticipantDao().getById(lookupParticipant(input.getParticipant()).getId());
	}

	private ParticipantDetail lookupParticipant(ParticipantDetail detail) {
		RequestEvent<List<ParticipantDetail>> req = new RequestEvent<>(Collections.singletonList(detail));
		ResponseEvent<List<MatchedParticipantsList>> resp = participantService.getMatchingParticipants(req);
		resp.throwErrorIfUnsuccessful();

		List<MatchedParticipant> result = resp.getPayload().get(0).getMatches();
		if (result.isEmpty()) {
			throw OpenSpecimenException.userError(ParticipantErrorCode.NOT_FOUND);
		}

		return result.iterator().next().getParticipant();
	}

	//
	// Deletes all registrations of participant
	//
	private ParticipantRegistrationsList deleteParticipant(Participant participant, boolean forceDelete) {
		List<CollectionProtocolRegistrationDetail> registrations = new ArrayList<>();
		for (CollectionProtocolRegistration cpr : participant.getCprs()) {
			AccessCtrlMgr.getInstance().ensureDeleteCprRights(cpr);
			cpr.delete(!forceDelete);
			registrations.add(CollectionProtocolRegistrationDetail.from(cpr, true));
		}

		ParticipantRegistrationsList result = new ParticipantRegistrationsList();
		result.setParticipant(ParticipantDetail.from(participant, true));
		result.setRegistrations(registrations);
		return result;
	}

	//
	// Checks whether same participant is registered for same protocol already
	//
	private void ensureUniqueParticipantReg(CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		if (cpr.getParticipant() == null || cpr.getParticipant().getId() == null) {
			return ;
		}
		
		Long participantId = cpr.getParticipant().getId();
		Long cpId = cpr.getCollectionProtocol().getId();
		
		if (daoFactory.getCprDao().getCprByParticipantId(cpId, participantId) != null) {
			ose.addError(CprErrorCode.DUP_REGISTRATION);
		}
	}

	private void ensureValidAndUniquePpid(CollectionProtocolRegistration existing, CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		if (existing != null && existing.getPpid().equals(cpr.getPpid())) {
			return;
		}
		
		CollectionProtocol cp = cpr.getCollectionProtocol();

		String ppid = cpr.getPpid();
		if (StringUtils.isBlank(ppid)) {
			boolean ppidReq = cp.isManualPpidEnabled() || StringUtils.isBlank(cp.getPpidFormat());
			if (ppidReq) {
				ose.addError(CprErrorCode.PPID_REQUIRED);
			}
			
			return;
		}
		
		
		if (StringUtils.isNotBlank(cp.getPpidFormat())) {
			//
			// PPID format is specified
			//
			
			if (!cp.isManualPpidEnabled()) {
				ose.addError(CprErrorCode.MANUAL_PPID_NOT_ALLOWED);
				return;
			}
			
			if (!labelGenerator.validate(cp.getPpidFormat(), cpr, ppid)) {
				ose.addError(CprErrorCode.INVALID_PPID, ppid);
				return;
			}
		}
		
		if (daoFactory.getCprDao().getCprByPpid(cp.getId(), ppid) != null) {
			ose.addError(CprErrorCode.DUP_PPID, ppid);
		}
	}

	private void ensureUniqueBarcode(CollectionProtocolRegistration existing, CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		if (existing != null && 
			StringUtils.isNotBlank(existing.getBarcode()) && 
			existing.getBarcode().equals(cpr.getBarcode())) { // barcode has not changed
			return;
		}
		
		if (StringUtils.isBlank(cpr.getBarcode())) {
			return;
		}
		
		if (daoFactory.getCprDao().getCprByBarcode(cpr.getBarcode()) != null) {
			ose.addError(CprErrorCode.DUP_BARCODE);
		}
	}
		
	private List<SpecimenDetail> getSpecimensByVisit(Long cprId, Long visitId) {
		Visit visit = daoFactory.getVisitsDao().getById(visitId);
		if (visit == null) {
			throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND);
		}
		
		Set<SpecimenRequirement> anticipatedSpecimens = visit.isUnplanned() ? Collections.EMPTY_SET : visit.getCpEvent().getTopLevelAnticipatedSpecimens();
		Set<Specimen> specimens = visit.getTopLevelSpecimens();

		return SpecimenDetail.getSpecimens(anticipatedSpecimens, specimens);
	}

	private List<SpecimenDetail> getAnticipatedSpecimens(Long cprId, Long eventId) {
		CollectionProtocolEvent cpe = daoFactory.getCollectionProtocolDao().getCpe(eventId);
		if (cpe == null) {
			throw OpenSpecimenException.userError(CpeErrorCode.NOT_FOUND, eventId, 1);
		}
		
		Set<SpecimenRequirement> anticipatedSpecimens = cpe.getTopLevelAnticipatedSpecimens();
		return SpecimenDetail.getSpecimens(anticipatedSpecimens, Collections.<Specimen>emptySet());		
	}
	
	private CollectionProtocolRegistration getCpr(Long cprId, Long cpId, String ppid) {
		return getCpr(cprId, cpId, null, ppid);
	}

	private CollectionProtocolRegistration getCpr(Long cprId, Long cpId, String cpShortTitle, String ppid) {
		CollectionProtocolRegistration cpr = null;
		if (cprId != null) {
			cpr = daoFactory.getCprDao().getById(cprId);
		} else if (cpId != null && StringUtils.isNotBlank(ppid)) {
			cpr = daoFactory.getCprDao().getCprByPpid(cpId, ppid);
		} else if (StringUtils.isNotBlank(cpShortTitle) && StringUtils.isNotBlank(ppid)) {
			cpr = daoFactory.getCprDao().getCprByCpShortTitleAndPpid(cpShortTitle, ppid);
		}
		
		if (cpr == null) {
			throw OpenSpecimenException.userError(CprErrorCode.NOT_FOUND);
		}
		
		return cpr;
	}

	private void checkDistributedSpecimens(List<SpecimenDetail> specimens) {
		List<Long> specimenIds = getSpecimenIds(specimens);
		if (CollectionUtils.isEmpty(specimenIds)) {
			return;
		}
		
		Map<Long, String> distStatus = daoFactory.getSpecimenDao().getDistributionStatus(specimenIds);
		setDistributedStatus(specimens, distStatus);
	}

	private void setDistributedStatus(List<SpecimenDetail> specimens, Map<Long, String> distStatus) {
		for (SpecimenDetail detail : specimens) {
			detail.setDistributionStatus(distStatus.get(detail.getId()));
			if (CollectionUtils.isNotEmpty(detail.getChildren())) {
				setDistributedStatus(detail.getChildren(), distStatus);
			}
		}
	}

	private List<Long> getSpecimenIds(List<SpecimenDetail> specimens) {
		List<Long> ids = new ArrayList<Long>();
		for (SpecimenDetail detail : specimens) {
			if (detail.getId() != null) {
				ids.add(detail.getId());
			}

			if (CollectionUtils.isNotEmpty(detail.getChildren())) {
				ids.addAll(getSpecimenIds(detail.getChildren()));
			}
		}

		return ids;
	}

	private List<CollectionProtocolRegistration> getOtherCprs(CollectionProtocolRegistration cpr) {
		Participant participant = cpr.getParticipant();
		if (participant.getCprs().size() < 2) {
			return Collections.emptyList();
		}

		return AccessCtrlMgr.getInstance().getAccessibleCprs(
			participant.getCprs().stream()
				.filter(otherCpr -> !otherCpr.equals(cpr))
				.collect(Collectors.toList())
		);
	}

	private CollectionProtocolRegistration registerParticipant(
		BulkRegistrationsDetail bulkRegDetail,
		Date regDate,
		String mrnSite,
		List<CollectionProtocolEvent> events,
		boolean checkPermission) {

		CollectionProtocolRegistrationDetail cprDetail = new CollectionProtocolRegistrationDetail();
		cprDetail.setRegistrationDate(regDate);
		cprDetail.setCpId(bulkRegDetail.getCpId());
		cprDetail.setCpTitle(bulkRegDetail.getCpTitle());
		cprDetail.setCpShortTitle(bulkRegDetail.getCpShortTitle());
		cprDetail.setParticipant(getParticipantDetail(mrnSite));

		if (checkPermission) {
			return saveOrUpdateRegistration(cprDetail, null, events, mrnSite, true);
		} else {
			return saveOrUpdateRegistration(cprFactory.createCpr(cprDetail), null, events, mrnSite, true);
		}
	}

	private ParticipantDetail getParticipantDetail(String mrnSite) {
		if (StringUtils.isBlank(mrnSite)) {
			return null;
		}

		ParticipantDetail participant = new ParticipantDetail();
		PmiDetail pmi = new PmiDetail();
		pmi.setSiteName(mrnSite);
		participant.setPmi(pmi);
		return participant;
	}

	private void addVisits(CollectionProtocolRegistration cpr, List<CollectionProtocolEvent> cpes, String collectionSite) {
		if (CollectionUtils.isEmpty(cpes)) {
			return;
		}

		boolean checkPermission = true;
		for (CollectionProtocolEvent cpe : cpes) {
			VisitDetail visitDetail = new VisitDetail();
			visitDetail.setCprId(cpr.getId());
			visitDetail.setEventId(cpe.getId());
			visitDetail.setSite(collectionSite);
			visitDetail.setClinicalDiagnoses(Collections.singleton(cpe.getClinicalDiagnosis()));
			visitDetail.setClinicalStatus(cpe.getClinicalStatus());
			visitDetail.setStatus(Visit.VISIT_STATUS_PENDING);

			cpr.addVisit(visitSvc.addVisit(visitDetail, checkPermission));
			checkPermission = false;
		}
	}

	@SuppressWarnings("unchecked")
	private List<CollectionProtocolEvent> getCpes(BulkRegistrationsDetail detail) {
		List<CollectionProtocolEventDetail> cpeDetails = detail.getEvents();

		if (CollectionUtils.isEmpty(cpeDetails)) {
			return Collections.emptyList();
		}

		List<CollectionProtocolEvent> result = null;
		Collection<Object> notFoundEvents    = null;

		List<Long> eventIds = cpeDetails.stream().filter(cpe -> cpe.getId() != null)
			.map(CollectionProtocolEventDetail::getId)
			.collect(Collectors.toList());

		if (CollectionUtils.isNotEmpty(eventIds)) {
			if (eventIds.size() != cpeDetails.size()) {
				throw OpenSpecimenException.userError(CpeErrorCode.IDS_OR_LABELS_REQUIRED, 1);
			}

			result = daoFactory.getCollectionProtocolDao().getCpes(eventIds);
			if (result.size() != eventIds.size()) {
				notFoundEvents = CollectionUtils.subtract(
					eventIds,
					result.stream().map(CollectionProtocolEvent::getId).collect(Collectors.toList()));
			}
		} else {
			List<String> eventLabels = cpeDetails.stream().filter(cpe -> StringUtils.isNotBlank(cpe.getEventLabel()))
				.map(CollectionProtocolEventDetail::getEventLabel)
				.collect(Collectors.toList());

			if (eventLabels.size() != cpeDetails.size()) {
				throw OpenSpecimenException.userError(CpeErrorCode.IDS_OR_LABELS_REQUIRED, 2);
			}

			result = daoFactory.getCollectionProtocolDao().getCpesByShortTitleAndEventLabels(
				detail.getCpShortTitle(), eventLabels);
			if (result.size() != eventIds.size()) {
				notFoundEvents = CollectionUtils.subtract(
					eventLabels,
					result.stream().map(CollectionProtocolEvent::getEventLabel).collect(Collectors.toList()));
			}
		}

		if (CollectionUtils.isNotEmpty(notFoundEvents)) {
			throw OpenSpecimenException.userError(CpeErrorCode.NOT_FOUND, notFoundEvents, notFoundEvents.size());
		}

		return result;
	}

	private void raiseErrorIfSpecimenCentric(CollectionProtocolRegistration cpr) {
		if (cpr.getCollectionProtocol().isSpecimenCentric()) {
			throw OpenSpecimenException.userError(CpErrorCode.OP_NOT_ALLOWED_SC, cpr.getCollectionProtocol().getShortTitle());
		}
	}

	private Long getCpId(Map<String, String> params) {
		Long cpId = null;
		String cpIdStr = params.get("cpId");
		if (StringUtils.isNotBlank(cpIdStr)) {
			try {
				cpId = Long.parseLong(cpIdStr);
				if (cpId == -1L) {
					cpId = null;
				}
			} catch (Exception e) {
				logger.error("Invalid CP ID: " + cpIdStr, e);
			}
		}

		return cpId;
	}

	private abstract class AbstractCprsGenerator implements Function<ExportJob, List<? extends Object>> {
		private boolean endOfCprs;

		private CprListCriteria crit;

		private int startAt;

		private boolean paramsInited;

		List<CollectionProtocolRegistration> nextCprs(ExportJob job) {
			initParams(job);

			if (endOfCprs) {
				return Collections.emptyList();
			}

			List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getCprs(crit.startAt(startAt));
			startAt += cprs.size();
			if (CollectionUtils.isNotEmpty(crit.ppids()) || cprs.size() < 100) {
				endOfCprs = true;
			}

			return cprs;
		}

		//
		// -1: error, 0: full access, 1: access without PHI
		//
		int canRead(CollectionProtocolRegistration cpr) {
			try {
				return AccessCtrlMgr.getInstance().ensureReadCprRights(cpr) ? 0 : 1;
			} catch (OpenSpecimenException ose) {
				if (!ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
					logger.error("Error checking participant record access", ose);
				}

				return -1;
			}
		}

		private void initParams(ExportJob job) {
			if (paramsInited) {
				return;
			}

			Map<String, String> params = job.getParams();
			if (params == null) {
				params = Collections.emptyMap();
			}

			Long cpId = getCpId(params);
			AccessCtrlMgr.ParticipantReadAccess access = AccessCtrlMgr.getInstance().getParticipantReadAccess(cpId);
			if (!access.admin && access.noAccessibleSites()) {
				endOfCprs = true;
			} else if (!AccessCtrlMgr.getInstance().hasCprEximRights(cpId)) {
				endOfCprs = true;
			} else {
				crit = new CprListCriteria()
					.cpId(cpId)
					.ppids(Utility.csvToStringList(params.get("ppids")))
					.siteCps(access.siteCps)
					.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());

				if (CollectionUtils.isNotEmpty(crit.ppids())) {
					crit.limitItems(false);
				} else {
					crit.limitItems(true).maxResults(100);
				}
			}

			paramsInited = true;
		}
	}

	private Function<ExportJob, List<? extends Object>> getCprsGenerator() {
		return new AbstractCprsGenerator() {
			@Override
			public List<? extends Object> apply(ExportJob job) {
				List<CollectionProtocolRegistrationDetail> records = new ArrayList<>();
				for (CollectionProtocolRegistration cpr : nextCprs(job)) {
					int accessType = canRead(cpr);
					if (accessType >= 0) {
						records.add(CollectionProtocolRegistrationDetail.from(cpr, accessType == 1));
					}
				}

				return records;
			}
		};
	}

	private Function<ExportJob, List<? extends Object>> getConsentsGenerator() {
		return new AbstractCprsGenerator() {
			@Override
			public List<? extends Object> apply(ExportJob job) {
				List<ConsentDetail> records = new ArrayList<>();

				boolean endOfWork = false;
				while (!endOfWork) {
					List<CollectionProtocolRegistration> cprs = nextCprs(job);
					if (cprs.isEmpty()) {
						break;
					}

					for (CollectionProtocolRegistration cpr : cprs) {
						int accessType = canRead(cpr);
						if (accessType < 0 || !hasConsent(cpr, accessType == 0)) {
							continue;
						}

						ConsentDetail detail = toConsentDetail(cpr, accessType == 0);
						records.add(detail);

						boolean firstResp = true;
						for (ConsentTierResponse resp : getSortedResponses(cpr)) {
							if (!firstResp) {
								detail = copyOf(detail);
								records.add(detail);
							}

							detail.setStatement(resp.getStatement());
							detail.setResponse(resp.getResponse());
							firstResp = false;
						}
					}

					endOfWork = !records.isEmpty();
				}

				return records;
			}

			private boolean hasConsent(CollectionProtocolRegistration cpr, boolean hasPhi) {
				return cpr.getConsentSignDate() != null ||
					cpr.getConsentWitness() != null ||
					StringUtils.isNotBlank(cpr.getConsentComments()) ||
					(hasPhi && StringUtils.isNotBlank(cpr.getSignedConsentDocumentName())) ||
					CollectionUtils.isNotEmpty(cpr.getConsentResponses());
			}

			private ConsentDetail toConsentDetail(CollectionProtocolRegistration cpr, boolean hasPhi) {
				ConsentDetail detail = new ConsentDetail();
				detail.setCpShortTitle(cpr.getCpShortTitle());
				detail.setPpid(cpr.getPpid());
				detail.setConsentSignatureDate(cpr.getConsentSignDate());
				detail.setComments(cpr.getConsentComments());

				if (cpr.getConsentWitness() != null) {
					detail.setWitness(UserSummary.from(cpr.getConsentWitness()));
				}

				if (cpr.getSignedConsentDocumentName() != null && hasPhi) {
					detail.setConsentDocumentName(cpr.getSignedConsentDocumentName());
					detail.setDocumentFile(new File(ConfigParams.getConsentsDirPath(), cpr.getSignedConsentDocumentName()));
				}

				return detail;
			}

			private Collection<ConsentTierResponse> getSortedResponses(CollectionProtocolRegistration cpr) {
				return cpr.getConsentResponses().stream()
					.filter(r1 -> StringUtils.isNotBlank(r1.getResponse()))
					.sorted((r1, r2) -> r1.getConsentTier().getId().compareTo(r2.getConsentTier().getId()))
					.collect(Collectors.toList());
			}

			private ConsentDetail copyOf(ConsentDetail input) {
				try {
					ConsentDetail output = new ConsentDetail();
					BeanUtilsBean.getInstance().copyProperties(output, input);
					output.setConsentDocumentName(null);
					output.setDocumentFile(null);
					return output;
				} catch (Exception e) {
					throw new RuntimeException("Error cloning consent detail object", e);
				}
			}
		};
	}
}
