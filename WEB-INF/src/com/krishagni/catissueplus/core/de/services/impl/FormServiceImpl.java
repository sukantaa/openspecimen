package com.krishagni.catissueplus.core.de.services.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.multipart.MultipartFile;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.SystemFormUpdatePreventer;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.events.AddRecordEntryOp;
import com.krishagni.catissueplus.core.de.events.EntityFormRecords;
import com.krishagni.catissueplus.core.de.events.FileDetail;
import com.krishagni.catissueplus.core.de.events.FormContextDetail;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormDataDetail;
import com.krishagni.catissueplus.core.de.events.FormFieldSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordCriteria;
import com.krishagni.catissueplus.core.de.events.FormRecordSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordsList;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.events.GetEntityFormRecordsOp;
import com.krishagni.catissueplus.core.de.events.GetFileDetailOp;
import com.krishagni.catissueplus.core.de.events.GetFormFieldPvsOp;
import com.krishagni.catissueplus.core.de.events.GetFormRecordsListOp;
import com.krishagni.catissueplus.core.de.events.ListEntityFormsOp;
import com.krishagni.catissueplus.core.de.events.ListFormFields;
import com.krishagni.catissueplus.core.de.events.ObjectCpDetail;
import com.krishagni.catissueplus.core.de.events.RemoveFormContextOp;
import com.krishagni.catissueplus.core.de.repository.FormDao;
import com.krishagni.catissueplus.core.de.services.FormContextProcessor;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.DataType;
import edu.common.dynamicextensions.domain.nui.FileUploadControl;
import edu.common.dynamicextensions.domain.nui.Label;
import edu.common.dynamicextensions.domain.nui.LookupControl;
import edu.common.dynamicextensions.domain.nui.PageBreak;
import edu.common.dynamicextensions.domain.nui.PermissibleValue;
import edu.common.dynamicextensions.domain.nui.SelectControl;
import edu.common.dynamicextensions.domain.nui.SubFormControl;
import edu.common.dynamicextensions.domain.nui.ValidationErrors;
import edu.common.dynamicextensions.napi.ControlValue;
import edu.common.dynamicextensions.napi.FileControlValue;
import edu.common.dynamicextensions.napi.FormData;
import edu.common.dynamicextensions.napi.FormDataManager;
import edu.common.dynamicextensions.napi.FormEventsNotifier;
import edu.common.dynamicextensions.napi.impl.FormDataManagerImpl;
import edu.common.dynamicextensions.nutility.FileUploadMgr;
import krishagni.catissueplus.beans.FormContextBean;
import krishagni.catissueplus.beans.FormRecordEntryBean;
import krishagni.catissueplus.beans.FormRecordEntryBean.Status;

public class FormServiceImpl implements FormService, InitializingBean {
	private static Log logger = LogFactory.getLog(FormServiceImpl.class);

	private static final String CP_FORM = "CollectionProtocol";

	private static final String PARTICIPANT_FORM = "Participant";

	private static final String COMMON_PARTICIPANT = "CommonParticipant";
	
	private static final String SCG_FORM = "SpecimenCollectionGroup";
	
	private static final String SPECIMEN_FORM = "Specimen";
	
	private static final String SPECIMEN_EVENT_FORM = "SpecimenEvent";
	
	private static Set<String> staticExtendedForms = new HashSet<>();
	
	private static Map<String, String> customFieldEntities = new HashMap<>();

	static {
		staticExtendedForms.add(PARTICIPANT_FORM);
		staticExtendedForms.add(SCG_FORM);
		staticExtendedForms.add(SPECIMEN_FORM);

		customFieldEntities.put(CP_FORM, CollectionProtocol.EXTN);
		customFieldEntities.put(PARTICIPANT_FORM, Participant.EXTN);
		customFieldEntities.put(SCG_FORM, Visit.EXTN);
		customFieldEntities.put(SPECIMEN_FORM, Specimen.EXTN);
	}
	
	private FormDao formDao;

	private DaoFactory daoFactory;

	private ExportService exportSvc;
	
	private Map<String, List<FormContextProcessor>> ctxtProcs = new HashMap<>();

	public void setFormDao(FormDao formDao) {
		this.formDao = formDao;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	public void setCtxtProcs(Map<String, List<FormContextProcessor>> ctxtProcs) {
		this.ctxtProcs = ctxtProcs;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		FormEventsNotifier.getInstance().addListener(new SystemFormUpdatePreventer(formDao));
		exportSvc.registerObjectsGenerator("extensions", this::getFormRecordsGenerator);
	}

	@Override
    @PlusTransactional
	public ResponseEvent<List<FormSummary>> getForms(RequestEvent<FormListCriteria> req) {
		FormListCriteria crit = req.getPayload();

		String entityType = crit.getFormType();
		if (StringUtils.isBlank(entityType) || entityType.equals("DataEntry")) {
			crit = addFormsListCriteria(crit);
			if (crit == null) {
				return ResponseEvent.response(Collections.emptyList());
			}

			return ResponseEvent.response(formDao.getAllFormsSummary(crit));
		} else if (entityType.equalsIgnoreCase("Query")) {
			return ResponseEvent.response(formDao.getQueryForms());
		} else {
			return ResponseEvent.response(formDao.getFormsByEntityType(entityType));
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getFormsCount(RequestEvent<FormListCriteria> req) {
		FormListCriteria crit = addFormsListCriteria(req.getPayload());
		if (crit == null) {
			return ResponseEvent.response(0L);
		}

		return ResponseEvent.response(formDao.getAllFormsCount(crit));
	}

    @Override
    @PlusTransactional
	public ResponseEvent<Container> getFormDefinition(RequestEvent<Long> req) {
		Container form = getContainer(req.getPayload(), null);
		return ResponseEvent.response(form);
	}
    
	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> deleteForms(RequestEvent<BulkDeleteEntityOp> req) {
		try {
			AccessCtrlMgr.getInstance().ensureFormUpdateRights();

			Set<Long> formIds = req.getPayload().getIds();
			List<Form> forms = formDao.getFormsByIds(formIds);
			if (formIds.size() != forms.size()) {
				forms.forEach(form -> formIds.remove(form.getId()));
				throw OpenSpecimenException.userError(FormErrorCode.NOT_FOUND, formIds, formIds.size());
			}

			formIds.forEach(Container::softDeleteContainer);
			formDao.deleteFormContexts(formIds);

			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
    
    @Override
    @PlusTransactional
	public ResponseEvent<List<FormFieldSummary>> getFormFields(RequestEvent<ListFormFields> req) {
    	ListFormFields op = req.getPayload();
		Container form = getContainer(op.getFormId(), null);
		
		List<FormFieldSummary> fields = getFormFields(form);
		Long cpId = op.getCpId();
		if (!op.isExtendedFields()) {
			return ResponseEvent.response(fields);
		}

		if (cpId == null || cpId < 0) {
			cpId = -1L;
		}
		
		String formName = form.getName();
		String entityType = customFieldEntities.get(formName);
		if (StringUtils.isNotBlank(entityType)) {
			Map<String, Object> extnInfo = getExtensionInfo(cpId, entityType);
			if (extnInfo == null && cpId != -1L) {
				extnInfo = getExtensionInfo(-1L, entityType);
			}

			if (extnInfo != null) {
				Long extnFormId = (Long)extnInfo.get("formId");
				fields.add(getExtensionField("customFields", "Custom Fields", Arrays.asList(extnFormId)));
			}
		}
		
		if (!staticExtendedForms.contains(formName)) {
			return ResponseEvent.response(fields);
		}
		
		List<Long> extendedFormIds = formDao.getFormIds(cpId, formName);
		if (formName.equals(PARTICIPANT_FORM)) {
			extendedFormIds.addAll(0, formDao.getFormIds(-1L, COMMON_PARTICIPANT));
		} else if (formName.equals(SPECIMEN_FORM)) {
			extendedFormIds.addAll(formDao.getFormIds(cpId, SPECIMEN_EVENT_FORM));
		}

		FormFieldSummary field = getExtensionField("extensions", "Extensions", extendedFormIds);
		fields.add(field);
		return ResponseEvent.response(fields);
	}
    
	@Override
	@PlusTransactional
	public ResponseEvent<List<FormContextDetail>> getFormContexts(RequestEvent<Long> req) {
		return ResponseEvent.response(formDao.getFormContexts(req.getPayload()));
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<FormContextDetail>> addFormContexts(RequestEvent<List<FormContextDetail>> req) { // TODO: check form is deleted
		try {
			AccessCtrlMgr.getInstance().ensureFormUpdateRights();

			Set<Long> allowedCpIds = new HashSet<Long>();
			List<FormContextDetail> formCtxts = req.getPayload();
			for (FormContextDetail formCtxtDetail : formCtxts) {
				Long formId = formCtxtDetail.getFormId();
				Long cpId = formCtxtDetail.getCollectionProtocol().getId();

				if (cpId == -1 && !AuthUtil.isAdmin()) {
					throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
				}
				
				if (!allowedCpIds.contains(cpId)) {
					if (cpId != -1) {
						AccessCtrlMgr.getInstance().ensureUpdateCpRights(cpId);
					}
					
					allowedCpIds.add(cpId);
				}

				String entity = formCtxtDetail.getLevel();

				FormContextBean formCtxt = formDao.getFormContext(formId, cpId, entity);
				if (formCtxt == null) {
					formCtxt = new FormContextBean();
					formCtxt.setContainerId(formId);
					formCtxt.setCpId(entity == SPECIMEN_EVENT_FORM ? -1 : cpId);
					formCtxt.setEntityType(entity);
					formCtxt.setMultiRecord(formCtxtDetail.isMultiRecord());
				}
				formCtxt.setSortOrder(formCtxtDetail.getSortOrder());

				notifyContextSaved(formCtxt);

				formDao.saveOrUpdate(formCtxt);
				formCtxtDetail.setFormCtxtId(formCtxt.getIdentifier());
			}
			
			return ResponseEvent.response(formCtxts);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<FormCtxtSummary>> getEntityForms(RequestEvent<ListEntityFormsOp> req) {
		try {
			ListEntityFormsOp opDetail = req.getPayload();
			
			List<FormCtxtSummary> forms = null;
			
			Long entityId = opDetail.getEntityId();
			switch (opDetail.getEntityType()) {
			    case COLLECTION_PROTOCOL_REGISTRATION:
			    	AccessCtrlMgr.getInstance().ensureReadCprRights(entityId);
					forms = formDao.getParticipantForms(entityId);
					forms.addAll(formDao.getCprForms(entityId));
			    	break;
			    	
			    case SPECIMEN:
			    	AccessCtrlMgr.getInstance().ensureReadSpecimenRights(entityId);
			    	forms = formDao.getSpecimenForms(opDetail.getEntityId());
			    	break;
			    	
			    case SPECIMEN_COLLECTION_GROUP:
			    	AccessCtrlMgr.getInstance().ensureReadVisitRights(entityId);
			    	forms = formDao.getScgForms(opDetail.getEntityId());
			    	break;

			    case SPECIMEN_EVENT :
			    	AccessCtrlMgr.getInstance().ensureReadSpecimenRights(entityId);
			    	forms = formDao.getSpecimenEventForms(opDetail.getEntityId());
			    	break;
			}
			
			return ResponseEvent.response(forms);			
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<EntityFormRecords> getEntityFormRecords(RequestEvent<GetEntityFormRecordsOp> req) {
		GetEntityFormRecordsOp opDetail = req.getPayload();
		
		FormSummary form = formDao.getFormByContext(opDetail.getFormCtxtId());
		List<FormRecordSummary> formRecs = formDao.getFormRecords(opDetail.getFormCtxtId(), opDetail.getEntityId());
		
		EntityFormRecords result = new EntityFormRecords();
		result.setFormId(form.getFormId());
		result.setFormCaption(form.getCaption());
		result.setFormCtxtId(opDetail.getFormCtxtId());
		result.setRecords(formRecs);
		
		return ResponseEvent.response(result);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<FormDataDetail> getFormData(RequestEvent<FormRecordCriteria> req) {
		try {
			FormRecordCriteria crit = req.getPayload();
			Container form = getContainer(crit.getFormId(), null);
			FormData record = getRecord(form, crit.getRecordId());
			return ResponseEvent.response(FormDataDetail.ok(crit.getFormId(), crit.getRecordId(), record));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<FormDataDetail>> getLatestRecords(RequestEvent<FormRecordCriteria> req) {
		try {
			FormRecordCriteria crit = req.getPayload();
			Container form = getContainer(crit.getFormId(), null);
			Map<Long, Pair<Long, Long>> recordIds = formDao.getLatestRecordIds(
				crit.getFormId(), crit.getEntityType(), crit.getObjectIds());

			List<FormDataDetail> records = new ArrayList<>();
			recordIds.forEach((objectId, recordId) -> {
				FormData record = getRecord(form, objectId, recordId.first(), crit.getEntityType(), recordId.second());
				records.add(FormDataDetail.ok(form.getId(), record.getRecordId(), record));
			});

			return ResponseEvent.response(records);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<FormDataDetail> saveFormData(RequestEvent<FormDataDetail> req) {
		FormDataDetail detail = req.getPayload();
		
		try {
			User user = AuthUtil.getCurrentUser();
			FormData formData = saveOrUpdateFormData(user, detail.getRecordId(), detail.getFormData(), detail.isPartial());
			return ResponseEvent.response(FormDataDetail.ok(formData.getContainer().getId(), formData.getRecordId(), formData));
		} catch (ValidationErrors ve) {
			return ResponseEvent.userError(FormErrorCode.INVALID_DATA, ve.getMessage());
		} catch(IllegalArgumentException ex) {
			return ResponseEvent.userError(FormErrorCode.INVALID_DATA, ex.getMessage());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<FormData>> saveBulkFormData(RequestEvent<List<FormData>> req) {
		try{ 
			User user = AuthUtil.getCurrentUser();
			List<FormData> formDataList = req.getPayload();
			List<FormData> savedFormDataList = new ArrayList<>();
			for (FormData formData : formDataList) {
				FormData savedFormData = saveOrUpdateFormData(user, formData.getRecordId(), formData, false);
				savedFormDataList.add(savedFormData);
			}
			
			return ResponseEvent.response(savedFormDataList);
		} catch (ValidationErrors ve) {
			return ResponseEvent.userError(FormErrorCode.INVALID_DATA, ve.getMessage());
		} catch(IllegalArgumentException ex) {
			return ResponseEvent.userError(FormErrorCode.INVALID_DATA, ex.getMessage());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}	
	}

	@Override
	@PlusTransactional
	public ResponseEvent<FileDetail> getFileDetail(RequestEvent<GetFileDetailOp> req) {
		GetFileDetailOp opDetail = req.getPayload();
		
		FormDataManager formDataMgr = new FormDataManagerImpl(false);
		FileControlValue fcv = formDataMgr.getFileControlValue(opDetail.getFormId(), opDetail.getRecordId(), opDetail.getCtrlName());
		if (fcv == null) {
			return ResponseEvent.userError(FormErrorCode.FILE_NOT_FOUND);
		}
		
		return ResponseEvent.response(FileDetail.from(fcv));
	}
	
	@Override
	public ResponseEvent<FileDetail> uploadFile(RequestEvent<MultipartFile> req) {
		MultipartFile input = req.getPayload();
		
		FileDetail fileDetail = new FileDetail();
		fileDetail.setFilename(input.getOriginalFilename());
		fileDetail.setSize(input.getSize());
		fileDetail.setContentType(input.getContentType());
		
		try {
			InputStream in = input.getInputStream();
			String fileId = FileUploadMgr.getInstance().saveFile(in);
			fileDetail.setFileId(fileId);
			return ResponseEvent.response(fileDetail);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Long> deleteRecord(RequestEvent<FormRecordCriteria> req) {
		try {
			FormRecordCriteria crit = req.getPayload();
			FormRecordEntryBean recEntry = formDao.getRecordEntry(crit.getFormId(), crit.getRecordId());
			if (recEntry == null) {
				return ResponseEvent.userError(FormErrorCode.REC_NOT_FOUND);
			}
			
			FormContextBean formCtxt = formDao.getById(recEntry.getFormCtxtId());
			if (formCtxt.isSysForm()) {
				return ResponseEvent.userError(FormErrorCode.SYS_REC_DEL_NOT_ALLOWED);
			}
			
			String entityType = recEntry.getEntityType();
			Long objectId = recEntry.getObjectId();
			if (entityType.equals("Participant")) {
				AccessCtrlMgr.getInstance().ensureUpdateCprRights(objectId);
			} else if (entityType.equals("SpecimenCollectionGroup")) {
				AccessCtrlMgr.getInstance().ensureCreateOrUpdateVisitRights(objectId, false);
			} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
				AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(objectId, false);
			}
			
			recEntry.setActivityStatus(Status.CLOSED);
			formDao.saveOrUpdateRecordEntry(recEntry);
			return  ResponseEvent.response(crit.getRecordId());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> removeFormContext(RequestEvent<RemoveFormContextOp> req) {
		try {
			AccessCtrlMgr.getInstance().ensureFormUpdateRights();

			RemoveFormContextOp opDetail = req.getPayload();
			FormContextBean formCtx = formDao.getFormContext(
					opDetail.getFormId(), 
					opDetail.getCpId(), 
					opDetail.getEntityType());
			
			if (formCtx == null) {
				return ResponseEvent.userError(FormErrorCode.NO_ASSOCIATION);
			}
			
			if (formCtx.isSysForm()) {
				return ResponseEvent.userError(FormErrorCode.SYS_FORM_DEL_NOT_ALLOWED);
			}
			
			if (formCtx.getCpId() == -1 && !AuthUtil.isAdmin()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}
			
			if (formCtx.getCpId() != -1) {
				AccessCtrlMgr.getInstance().ensureUpdateCpRights(formCtx.getCpId());
			}

			notifyContextRemoved(formCtx);
			switch (opDetail.getRemoveType()) {
				case SOFT_REMOVE:
					formCtx.setDeletedOn(Calendar.getInstance().getTime());
					break;
					
				case HARD_REMOVE:
					formDao.delete(formCtx);
					break;
			}
			
			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<Long> addRecordEntry(RequestEvent<AddRecordEntryOp> req) {
		AddRecordEntryOp opDetail = req.getPayload();
		String entityType = (String) opDetail.getRecIntegrationInfo().get("entityType");

		ObjectCpDetail objCp = formDao.getObjectCpDetail(opDetail.getRecIntegrationInfo());
		Long formCtxtId = formDao.getFormCtxtId(opDetail.getContainerId(), entityType, objCp.getCpId());

		FormRecordEntryBean recordEntry = new FormRecordEntryBean();
		recordEntry.setFormCtxtId(formCtxtId);
		recordEntry.setObjectId(objCp.getObjectId());
		recordEntry.setRecordId(opDetail.getRecordId());
		recordEntry.setUpdatedBy(AuthUtil.getCurrentUser().getId());
		recordEntry.setUpdatedTime(Calendar.getInstance().getTime());
		recordEntry.setActivityStatus(Status.ACTIVE);

		formDao.saveOrUpdateRecordEntry(recordEntry);		
		return ResponseEvent.response(recordEntry.getIdentifier());
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<List<FormRecordsList>> getFormRecords(RequestEvent<GetFormRecordsListOp> req) {
		try {
			GetFormRecordsListOp input = req.getPayload();
			String entityType = input.getEntityType();
			Long objectId = input.getObjectId();

			if (entityType.equals("Participant")) {
				AccessCtrlMgr.getInstance().ensureReadCprRights(objectId);
			} else if (entityType.equals("SpecimenCollectionGroup")) {
				AccessCtrlMgr.getInstance().ensureReadVisitRights(objectId);
			} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
				AccessCtrlMgr.getInstance().ensureReadSpecimenRights(objectId);
			}

			List<FormRecordsList> result = getFormRecords(entityType, input.getFormId(), objectId);
			if (entityType.equals("Participant")) {
				objectId = daoFactory.getCprDao().getById(objectId).getParticipant().getId();
				result.addAll(0, getFormRecords("CommonParticipant", input.getFormId(), objectId));
			}

			return ResponseEvent.response(result);			
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req) {
		try {
			return ResponseEvent.response(formDao.getDependentEntities(req.getPayload()));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	//
	// Internal APIs
	//
	@Override
	public List<FormData> getSummaryRecords(Long formId, List<Long> recordIds) {
		FormDataManager mgr = new FormDataManagerImpl(false);
		return mgr.getSummaryData(formId, recordIds);
	}

	@Override
	public FormData getRecord(Container form, Long recordId) {
		return getRecord(form, null, null, null, recordId);
	}

	@Override
	public ResponseEvent<List<PermissibleValue>> getPvs(RequestEvent<GetFormFieldPvsOp> req) {
		try {
			GetFormFieldPvsOp input = req.getPayload();
			Container form = getContainer(input.getFormId(), input.getFormName());

			String controlName = input.getControlName();
			Control control;
			if (input.isUseUdn()) {
				control = form.getControlByUdn(controlName, "\\.");
			} else {
				control = form.getControl(controlName, "\\.");
			}

			if (!(control instanceof SelectControl)) {
				return ResponseEvent.userError(FormErrorCode.NOT_SELECT_CONTROL, controlName);
			}

			String searchStr = input.getSearchString();
			int maxResults = input.getMaxResults() <= 0 ? 100 : input.getMaxResults();

			SelectControl selectControl = (SelectControl) control;
			List<PermissibleValue> pvs = selectControl.getPvDataSource().getPermissibleValues(searchStr, maxResults);
			return ResponseEvent.response(pvs);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void addFormContextProc(String entity, FormContextProcessor proc) {
		List<FormContextProcessor> procs = ctxtProcs.get(entity);
		if (procs == null) {
			procs = new ArrayList<>();
			ctxtProcs.put(entity, procs);
		}

		boolean exists = false;
		for (FormContextProcessor existing : procs) {
			if (existing == proc) {
				exists = true;
				break;
			}
		}

		if (!exists) {
			procs.add(proc);
		}
	}

	@Override
	@PlusTransactional
	public Map<String, Object> getExtensionInfo(Long cpId, String entityType) {
		return DeObject.getFormInfo(cpId, entityType);
	}

	@Override
	@PlusTransactional
	public List<FormSummary> getEntityForms(Long cpId, String[] entityTypes) {
		return formDao.getFormsByCpAndEntityType(cpId, entityTypes);
	}

	//
	// anonymize. Used by internal code
	//
	@Override
	@PlusTransactional
	public void anonymizeRecord(Container form, Long recordId) {
		FormRecordEntryBean recEntry = formDao.getRecordEntry(form.getId(), recordId);
		if (recEntry == null) {
			throw OpenSpecimenException.userError(FormErrorCode.REC_NOT_FOUND);
		}

		FormDataManager formDataMgr = new FormDataManagerImpl(false);
		formDataMgr.anonymize(null, form, recordId);

		recEntry.setUpdatedBy(AuthUtil.getCurrentUser().getId());
		recEntry.setUpdatedTime(Calendar.getInstance().getTime());
		formDao.saveOrUpdateRecordEntry(recEntry);
	}

	private FormListCriteria addFormsListCriteria(FormListCriteria crit) {
		User currUser = AuthUtil.getCurrentUser();
		if (!currUser.isAdmin() && !currUser.getManageForms()) {
			return null;
		} else if (!currUser.isAdmin() && currUser.getManageForms()) {
			crit.userId(currUser.getId());
			crit.cpIds(AccessCtrlMgr.getInstance().getReadableCpIds());
		}
		
		return crit;
	}

	private FormFieldSummary getExtensionField(String name, String caption, List<Long> extendedFormIds ) {
		FormFieldSummary field = new FormFieldSummary();
		field.setName(name);
		field.setCaption(caption);
		field.setType("SUBFORM");

		List<FormFieldSummary> extensionFields = new ArrayList<FormFieldSummary>();
		for (Long extendedFormId : extendedFormIds) {
			Container form = Container.getContainer(extendedFormId);

			FormFieldSummary extensionField = new FormFieldSummary();
			extensionField.setName(form.getName());
			extensionField.setCaption(form.getCaption());
			extensionField.setType("SUBFORM");
			extensionField.setSubFields(getFormFields(form));
			extensionField.getSubFields().add(0, getRecordIdField(form));
			extensionFields.add(extensionField);
		}

		field.setSubFields(extensionFields);
		return field;
	}

	//
	// The idea was to sort forms in the backend
	// However, at present the workflows is fully UI concept.
	// therefore junked this idea and sorted forms in UI.
	// remove this code after 31/12/2017
	//
//	private List<FormCtxtSummary> sortForms(Long cpId, String entityType, List<FormCtxtSummary> forms) {
//		CpWorkflowConfig cfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(cpId);
//		if (cfg == null) {
//			return forms;
//		}
//
//		Map<String, CpWorkflowConfig.Workflow> workflows = cfg.getWorkflows();
//		if (workflows == null || workflows.isEmpty() || !workflows.containsKey("forms")) {
//			return forms;
//		}
//
//		CpWorkflowConfig.Workflow workflow = workflows.get("forms");
//		if (workflow.getData() == null || workflow.getData().isEmpty() || workflow.getData().get(entityType) == null) {
//			return forms;
//		}
//
//		List<Map<String, Object>> order = (List<Map<String, Object>>)workflow.getData().get(entityType);
//		List<Long> orderedFormIds = order.stream().map(f -> ((Number)f.get("id")).longValue()).collect(Collectors.toList());
//
//		Collections.sort(forms, (f1, f2) -> {
//			int f1Idx = orderedFormIds.indexOf(f1.getFormId());
//			int f2Idx = orderedFormIds.indexOf(f2.getFormId());
//
//			if (f1Idx == -1 && f2Idx == -1) {
//				return 0;
//			} else if (f1Idx == -1) {
//				return 1;
//			} else if (f2Idx == -1) {
//				return -1;
//			} else {
//				return f1Idx - f2Idx;
//			}
//		});
//
//		return forms;
//	}
		
	private FormData saveOrUpdateFormData(User user, Long recordId, FormData formData, boolean isPartial) {
		Map<String, Object> appData = formData.getAppData();
		if (appData.get("formCtxtId") == null || appData.get("objectId") == null) {
			throw new IllegalArgumentException("Invalid form context id or object id ");
		}

		Long objectId = ((Number) appData.get("objectId")).longValue();
		List<Long> formCtxtId = new ArrayList<Long>();
		formCtxtId.add(((Number) appData.get("formCtxtId")).longValue());
		
		List<FormContextBean> formContexts = formDao.getFormContextsById(formCtxtId);
		if (CollectionUtils.isEmpty(formContexts)) {
			throw new IllegalArgumentException("Invalid form context id");
		}
		
		boolean isInsert = (recordId == null);
		FormDataManager formDataMgr = new FormDataManagerImpl(false);
		if (!isInsert && isPartial) {
			FormData existing = formDataMgr.getFormData(formData.getContainer(), formData.getRecordId());
			formData = updateFormData(existing, formData);
		}
		
		formData.validate();
		
		FormContextBean formContext = formContexts.get(0);
		String entityType = formContext.getEntityType();
		Container form = formData.getContainer();
		if (entityType.equals("Participant")) {
			CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(objectId);
			if (cpr == null) {
				throw OpenSpecimenException.userError(CprErrorCode.NOT_FOUND, objectId);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);
		} else if (entityType.equals("CommonParticipant")) {
			CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(objectId);
			if (cpr == null) {
				throw OpenSpecimenException.userError(CprErrorCode.NOT_FOUND, objectId);
			}

			AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);
			objectId = cpr.getParticipant().getId();
		} else if (entityType.equals("SpecimenCollectionGroup")) {
			Visit visit = daoFactory.getVisitsDao().getById(objectId);
			if (visit == null) {
				throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, objectId);
			}

			AccessCtrlMgr.getInstance().ensureCreateOrUpdateVisitRights(visit, form.hasPhiFields());
		} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
			Specimen specimen = daoFactory.getSpecimenDao().getById(objectId);
			if (specimen == null) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, objectId);
			} else if (!specimen.isActive()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.EDIT_NOT_ALLOWED, specimen.getLabel());
			}

			AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(specimen, form.hasPhiFields());
		}

		formData.setRecordId(recordId);
		
		FormRecordEntryBean recordEntry = null;
		
		if (isInsert) {
			if (!formContext.isMultiRecord()) {
				Long noOfRecords = formDao.getRecordsCount(formContext.getIdentifier(), objectId);
				if (noOfRecords >= 1L) {
					throw OpenSpecimenException.userError(FormErrorCode.MULTIPLE_RECS_NOT_ALLOWED);
				}
			}
			
			recordEntry = new FormRecordEntryBean();
			recordEntry.setActivityStatus(Status.ACTIVE);			
		} else {
			recordEntry = formDao.getRecordEntry(formContext.getIdentifier(), objectId, recordId);
			if (recordEntry == null || recordEntry.getActivityStatus() != Status.ACTIVE) {
				throw OpenSpecimenException.userError(FormErrorCode.INVALID_REC_ID, recordId);				
			}
		}

		
		recordId = formDataMgr.saveOrUpdateFormData(null, formData);

		recordEntry.setFormCtxtId(formContext.getIdentifier());
		recordEntry.setObjectId(objectId);
		recordEntry.setRecordId(recordId);
		recordEntry.setUpdatedBy(user.getId());
		recordEntry.setUpdatedTime(Calendar.getInstance().getTime());
		formDao.saveOrUpdateRecordEntry(recordEntry);

		formData.setRecordId(recordId);
		return formData;
	}
	
	private List<FormFieldSummary> getFormFields(Container container) {
        List<FormFieldSummary> fields = new ArrayList<>();

        for (Control control : container.getControls()) {        	
            FormFieldSummary field = new FormFieldSummary();
            field.setName(control.getUserDefinedName());
            field.setCaption(control.getCaption());

            if (control instanceof SubFormControl) {
            	SubFormControl sfCtrl = (SubFormControl)control;
            	if (!sfCtrl.isPathLink()) {
                	field.setType("SUBFORM");
                	field.setSubFields(getFormFields(sfCtrl.getSubContainer()));
                	fields.add(field);            		
            	}
            } else if (!(control instanceof Label || control instanceof PageBreak)) {
            	DataType dataType = getType(control);
            	field.setType(dataType.name());
            	                
            	if (control instanceof SelectControl) {
            		SelectControl selectCtrl = (SelectControl)control;
					List<String> pvs = selectCtrl.getPvDataSource()
						.getPermissibleValues(Calendar.getInstance().getTime(), 100)
						.stream().map(PermissibleValue::getValue)
						.collect(Collectors.toList());
            		field.setPvs(pvs);
            	} else if (control instanceof LookupControl) {
            		LookupControl luCtrl = (LookupControl)control;
            		field.setLookupProps(luCtrl.getPvSourceProps());
            	}
            	
            	fields.add(field);
            }
        }

        return fields;		
	}

	private FormData getRecord(Container form, Long objectId, Long formCtxtId, String entityType, Long recordId) {
		FormDataManager formDataMgr = new FormDataManagerImpl(false);

		FormData formData = formDataMgr.getFormData(form, recordId);
		if (formData == null) {
			throw OpenSpecimenException.userError(FormErrorCode.REC_NOT_FOUND);
		}

		if (objectId == null || entityType == null) {
			FormRecordEntryBean record = formDao.getRecordEntry(formData.getContainer().getId(), formData.getRecordId());
			objectId = record.getObjectId();
			formCtxtId = record.getFormCtxtId();
			entityType = record.getEntityType();
		}

		if (formData.getContainer().hasPhiFields() && !isPhiAccessAllowed(entityType, objectId)) {
			formData.maskPhiFieldValues();
		}

		Map<String, Object> appData = formData.getAppData();
		appData.put("formCtxtId", formCtxtId);
		appData.put("objectId", objectId);
		return formData;
	}

	private boolean isPhiAccessAllowed(String entityType, Long objectId) {
		boolean allowPhiAccess = false;
		if (entityType.equals(PARTICIPANT_FORM) || Participant.EXTN.equals(entityType)) {
			allowPhiAccess = AccessCtrlMgr.getInstance().ensureReadCprRights(objectId);
		} else if (entityType.equals(COMMON_PARTICIPANT)) {
			allowPhiAccess = AccessCtrlMgr.getInstance().ensureReadParticipantRights(objectId);
		} else if (entityType.equals(SCG_FORM) || Visit.EXTN.equals(entityType)) {
			allowPhiAccess = AccessCtrlMgr.getInstance().ensureReadVisitRights(objectId, true);
		} else if (entityType.equals(SPECIMEN_FORM) || entityType.equals(SPECIMEN_EVENT_FORM) || Specimen.EXTN.equals(entityType)) {
			allowPhiAccess = AccessCtrlMgr.getInstance().ensureReadSpecimenRights(objectId, true);
		}

		return allowPhiAccess;
	}

	private FormFieldSummary getRecordIdField(Container form) {
		Control pkCtrl = form.getPrimaryKeyControl();

		FormFieldSummary field = new FormFieldSummary();
		field.setName(pkCtrl.getUserDefinedName());
		field.setCaption(pkCtrl.getCaption());
		field.setType(getType(pkCtrl).name());
		return field;
	}
	
	private DataType getType(Control ctrl) {
		if (ctrl instanceof FileUploadControl) {
			return DataType.STRING;
		} else if (ctrl instanceof LookupControl) {
			return ((LookupControl)ctrl).getValueType();
		} else {
			return ctrl.getDataType();
		}
	}
	
	private FormData updateFormData(FormData existing, FormData formData) {
		existing.setAppData(formData.getAppData());
		for (ControlValue ctrlValue : formData.getFieldValues()) {
			existing.addFieldValue(ctrlValue);
		}
		
		return existing;
	}

	private void notifyContextSaved(FormContextBean formCtxt) {
		notifyContextSaved(formCtxt.getEntityType(), formCtxt);
		notifyContextSaved("*", formCtxt);
	}

	private void notifyContextSaved(String entityType, FormContextBean formCtxt) {
		List<FormContextProcessor> procs = ctxtProcs.get(entityType);
		if (procs != null) {
			procs.forEach(proc -> proc.onSaveOrUpdate(formCtxt));
		}
	}

	private void notifyContextRemoved(FormContextBean formCtxt) {
		notifyContextRemoved(formCtxt.getEntityType(), formCtxt);
		notifyContextRemoved("*", formCtxt);
	}

	private void notifyContextRemoved(String entityType, FormContextBean formCtxt) {
		List<FormContextProcessor> procs = ctxtProcs.get(entityType);
		if (procs != null) {
			procs.forEach(proc -> proc.onRemove(formCtxt));
		}
	}

	private List<FormRecordsList> getFormRecords(String entityType, Long inputFormId, Long objectId) {
		List<FormRecordsList> result = new ArrayList<>();

		FormDataManager formDataMgr = new FormDataManagerImpl(false);
		Map<Long, List<FormRecordSummary>> records = formDao.getFormRecords(objectId, entityType, inputFormId);
		for (Map.Entry<Long, List<FormRecordSummary>> formRecs : records.entrySet()) {
			Long formId = formRecs.getKey();
			Container container = Container.getContainer(formId);

			List<Long> recIds = new ArrayList<>();
			Map<Long, FormRecordSummary> recMap = new HashMap<>();
			for (FormRecordSummary rec : formRecs.getValue()) {
				recMap.put(rec.getRecordId(), rec);
				recIds.add(rec.getRecordId());
			}

			List<FormData> summaryRecs = formDataMgr.getSummaryData(container, recIds);
			for (FormData rec : summaryRecs) {
				recMap.get(rec.getRecordId()).addFieldValues(rec.getFieldValues());
			}

			result.add(FormRecordsList.from(container, recMap.values()));
		}

		return result;
	}

	private Function<ExportJob, List<? extends Object>> getFormRecordsGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean endOfRecords = false;

			private boolean paramsInited = false;

			private Container form;

			private boolean formHasPhi;

			private String entityType;

			private CollectionProtocol cp;

			private Set<Long> cpIds;

			private int startAt;

			private Object lastObj;

			private Long lastObjId;

			private boolean lastObjReadAllowed;

			private boolean lastObjPhiAllowed;

			@Override
			public List<? extends Object> apply(ExportJob job) {
				if (endOfRecords) {
					return Collections.emptyList();
				}

				if (!paramsInited) {
					initParams(job);
				}


				Function<ExportJob, List<Map<String, Object>>> recordsFn = null;
				if (entityType.equals("Participant") || entityType.equals("CommonParticipant")) {
					recordsFn = this::getRegistrationFormRecords;
				} else if (entityType.equals("SpecimenCollectionGroup")) {
					recordsFn = this::getVisitFormRecords;
				} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
					recordsFn = this::getSpmnFormRecords;
				}

				List<Map<String, Object>> records = null;
				if (recordsFn != null) {
					while (!endOfRecords && CollectionUtils.isEmpty(records)) {
						records = recordsFn.apply(job);
					}
				}

				return records;
			}

			private void initParams(ExportJob job) {
				Map<String, String> params = job.getParams();
				if (params == null) {
					params = Collections.emptyMap();
				}

				form = getContainer(null, params.get("formName"));
				formHasPhi = form.hasPhiFields();

				entityType = params.get("entityType");
				if (StringUtils.isBlank(entityType)) {
					throw OpenSpecimenException.userError(FormErrorCode.ENTITY_TYPE_REQUIRED);
				}

				String cpIdStr = params.get("cpId");
				if (StringUtils.isNotBlank(cpIdStr)) {
					try {
						Long cpId = Long.parseLong(cpIdStr);
						if (cpId != -1L) {
							cpIds = Collections.singleton(cpId);
						}
					} catch (Exception e) {
						logger.error("Invalid CP ID: " + cpIdStr, e);
					}
				}

				if (cpIds == null) {
					cpIds = AccessCtrlMgr.getInstance().getReadableCpIds();
				}

				Function<Long, Boolean> hasEximRights = null;
				if (entityType.equals("Participant") || entityType.equals("CommonParticipant")) {
					hasEximRights = AccessCtrlMgr.getInstance()::hasCprEximRights;
				} else if (entityType.equals("SpecimenCollectionGroup")) {
					hasEximRights = AccessCtrlMgr.getInstance()::hasVisitSpecimenEximRights;
				} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
					hasEximRights = AccessCtrlMgr.getInstance()::hasVisitSpecimenEximRights;
				}

				if (hasEximRights == null) {
					endOfRecords = true;
				} else {
					cpIds = cpIds.stream().filter(hasEximRights::apply).collect(Collectors.toSet());
					endOfRecords = cpIds.isEmpty();
				}

				paramsInited = true;
			}

			private List<Map<String, Object>> getRegistrationFormRecords(ExportJob job) {
				String entityType = job.getParams().get("entityType");

				return getRecords(
					job,
					"ppids",
					(ppids) -> {
						if (entityType.equals("Participant")) {
							return formDao.getRegistrationRecords(cpIds, form.getId(), ppids, startAt, 100);
						} else {
							return formDao.getParticipantRecords(cpIds, form.getId(), ppids, startAt, 100);
						}
					},
					"cprId",
					(cprId) -> daoFactory.getCprDao().getById(cprId),
					(cpr) -> {
						if (entityType.equals("Participant")) {
							return AccessCtrlMgr.getInstance().ensureReadCprRights((CollectionProtocolRegistration) cpr);
						} else {
							return AccessCtrlMgr.getInstance().ensureReadParticipantRights(((CollectionProtocolRegistration) cpr).getParticipant().getId());
						}
					},
					(cprFormValueMap) -> {
						CollectionProtocolRegistration cpr = (CollectionProtocolRegistration) cprFormValueMap.first();
						Map<String, Object> valueMap = cprFormValueMap.second();

						Map<String, Object> formData = new HashMap<>();
						formData.put("recordId", valueMap.get("id"));
						formData.put("cpShortTitle", cpr.getCollectionProtocol().getShortTitle());
						formData.put("ppid", cpr.getPpid());
						formData.put("formValueMap", valueMap);
						return formData;
					}
				);
			}

			private List<Map<String, Object>> getVisitFormRecords(ExportJob job) {
				return getRecords(
					job,
					"visitNames",
					(visitNames) -> formDao.getVisitRecords(cpIds, form.getId(), visitNames, startAt, 100),
					"visitId",
					(visitId) -> daoFactory.getVisitsDao().getById(visitId),
					(visit) -> AccessCtrlMgr.getInstance().ensureReadVisitRights((Visit) visit, true),
					(visitFormValueMap) -> {
						Visit visit = (Visit) visitFormValueMap.first();
						Map<String, Object> valueMap = visitFormValueMap.second();

						Map<String, Object> formData = new HashMap<>();
						formData.put("recordId", valueMap.get("id"));
						formData.put("cpShortTitle", visit.getCollectionProtocol().getShortTitle());
						formData.put("visitName", visit.getName());
						formData.put("formValueMap", valueMap);
						return formData;
					}
				);
			}

			private List<Map<String, Object>> getSpmnFormRecords(ExportJob job) {
				return getRecords(
					job,
					"specimenLabels",
					(spmnLabels) -> formDao.getSpecimenRecords(cpIds, form.getId(), entityType, spmnLabels, startAt, 100),
					"spmnId",
					(specimenId) -> daoFactory.getSpecimenDao().getById(specimenId),
					(specimen) -> AccessCtrlMgr.getInstance().ensureReadSpecimenRights((Specimen) specimen, true),
					(spmnFormValueMap) -> {
						Specimen specimen = (Specimen) spmnFormValueMap.first();
						Map<String, Object> valueMap = spmnFormValueMap.second();

						Map<String, Object> formData = new HashMap<>();
						formData.put("recordId", valueMap.get("id"));
						formData.put("cpShortTitle", specimen.getCollectionProtocol().getShortTitle());
						formData.put("specimenLabel", specimen.getLabel());
						formData.put("barcode", specimen.getBarcode());
						formData.put("formValueMap", valueMap);
						return formData;
					}
				);
			}

			private List<Map<String, Object>> getRecords(
				ExportJob job,
				String namesCsvVar,
				Function<List<String>, List<Map<String, Object>>> getFormRecs,
				String objIdVar,
				Function<Long, Object> getObj,
				Function<Object, Boolean> objAccessChecker,
				Function<Pair<Object, Map<String, Object>>, Map<String, Object>> toFormRec) {

				String namesCsv = job.getParams().get(namesCsvVar);
				List<String> names = null;
				if (StringUtils.isNotBlank(namesCsv)) {
					names = Utility.csvToStringList(namesCsv);
					endOfRecords = true;
				}

				List<Map<String, Object>> records = getFormRecs.apply(names);
				startAt += records.size();
				if (records.size() < 100) {
					endOfRecords = true;
				}

				List<Map<String, Object>> result = new ArrayList<>();
				for (Map<String, Object> record : records) {
					Long objId = (Long)record.get(objIdVar);
					if (objId.equals(lastObjId) && !lastObjReadAllowed) {
						continue;
					}

					if (!objId.equals(lastObjId)) {
						try {
							lastObj = getObj.apply(objId);
							lastObjId = objId;
							lastObjPhiAllowed = objAccessChecker.apply(lastObj);
							lastObjReadAllowed = true;
						} catch (OpenSpecimenException ose) {
							if (isAccessDeniedError(ose)) {
								lastObjReadAllowed = lastObjPhiAllowed = false;
								continue;
							}

							throw ose;
						}
					}

					Long recordId = (Long)record.get("recordId");
					result.add(toFormRec.apply(Pair.make(lastObj, getFormData(recordId, !lastObjPhiAllowed))));
				}

				return result;
			}

			private Map<String, Object> getFormData(Long recordId, boolean maskPhi) {
				FormDataManager formDataMgr = new FormDataManagerImpl(false);
				FormData formData = formDataMgr.getFormData(form, recordId);

				if (formHasPhi && maskPhi) {
					formData.maskPhiFieldValues();
				}

				return formData.getFieldNameValueMap(true);
			}

			private boolean isAccessDeniedError(OpenSpecimenException ose) {
				return ose.containsError(RbacErrorCode.ACCESS_DENIED);
			}
		};
	}

	private Container getContainer(Long formId, String formName) {
		Object key = null;
		Container form = null;
		if (formId != null) {
			form = Container.getContainer(formId);
			key = formId;
		} else if (StringUtils.isNotBlank(formName)) {
			form = Container.getContainer(formName);
			key = formName;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(FormErrorCode.NAME_REQUIRED);
		} else if (form == null) {
			throw OpenSpecimenException.userError(FormErrorCode.NOT_FOUND, key, 1);
		}

		return form;
	}
}
