package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenResolver;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;
import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.FileUploadControl;
import edu.common.dynamicextensions.nutility.FileUploadMgr;

public class SpecimenImporter implements ObjectImporter<SpecimenDetail, SpecimenDetail> {

	private DaoFactory daoFactory;
	
	private SpecimenService specimenSvc;

	private SpecimenResolver specimenResolver;

	private FormService formSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setSpecimenSvc(SpecimenService specimenSvc) {
		this.specimenSvc = specimenSvc;
	}

	public void setSpecimenResolver(SpecimenResolver specimenResolver) {
		this.specimenResolver = specimenResolver;
	}

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	@Override
	public ResponseEvent<SpecimenDetail> importObject(RequestEvent<ImportObjectDetail<SpecimenDetail>> req) {
		try {
			ImportObjectDetail<SpecimenDetail> detail = req.getPayload();
			detail.getObject().setForceDelete(true);
			RequestEvent<SpecimenDetail> specReq = new RequestEvent<SpecimenDetail>(detail.getObject());

			CollectionProtocol cp = getCollectionProtocol(detail.getObject(), !detail.isCreate());
			Map<String, Object> extensionInfo = formSvc.getExtensionInfo(cp.getId(), Specimen.EXTN);
			String formName = (String)extensionInfo.get("formName");
			Container form = DeObject.getForm(formName);

			initFileFields(detail.getUploadedFilesDir(), form, detail.getObject().getExtensionDetail().getAttrs());


			if (detail.isCreate()) {
				return specimenSvc.createSpecimen(specReq);
			} else {
				return specimenSvc.updateSpecimen(specReq);
			}
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}

	private CollectionProtocol getCollectionProtocol(SpecimenDetail detail, boolean isUpdateOp) {
		String cpShortTitle = detail.getCpShortTitle();
		String visitName = detail.getVisitName();
		String parentLabel = detail.getParentLabel();
		CollectionProtocol cp = null;

		if (StringUtils.isNotBlank(cpShortTitle)) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(cpShortTitle);
			if (cp == null) {
				throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, cpShortTitle);
			}
		} else if (StringUtils.isNotBlank(visitName)) {
			Visit visit = daoFactory.getVisitsDao().getByName(visitName);
			if (visit == null) {
				throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, visitName);
			}
			cp = visit.getCollectionProtocol();
		} else if (StringUtils.isNotBlank(parentLabel)) {
			Specimen specimen = specimenResolver.getSpecimen(cpShortTitle, parentLabel);
			if (specimen == null) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, parentLabel);
			}
			cp = specimen.getCollectionProtocol();
		} else if (isUpdateOp && (detail.getId() != null || StringUtils.isNotBlank(detail.getLabel())
			|| StringUtils.isNotBlank(detail.getBarcode()))) {
			Specimen specimen = specimenResolver.getSpecimen(detail.getId(), null, detail.getLabel(), detail.getBarcode());
			cp = specimen.getCollectionProtocol();
		}

		return cp;
	}

	private void initFileFields(String filesDir, Container form, List<ExtensionDetail.AttrDetail> attrs) {
		for (Control ctrl : form.getControls()) {
			if (!(ctrl instanceof FileUploadControl)) {
				continue;
			}

			ExtensionDetail.AttrDetail attrDetail = attrs.stream()
				.filter(a -> a.getName().equals(ctrl.getName()))
				.findFirst()
				.get();
			String filename = (String)attrDetail.getValue();
			if (StringUtils.isNotBlank(filename)) {
				Map<String, String> fileDetail = uploadFile(filesDir, filename);
				attrDetail.setValue(fileDetail);
			}
		}
	}

	private Map<String, String> uploadFile(String filesDir, String filename) {
		FileInputStream fin = null;
		try {
			File fileToUpload = new File(filesDir + File.separator + filename);
			fin = new FileInputStream(fileToUpload);
			String fileId = FileUploadMgr.getInstance().saveFile(fin);

			Map<String, String> fileDetail = new HashMap<>();
			fileDetail.put("filename", filename);
			fileDetail.put("fileId", fileId);
			fileDetail.put("contentType", Utility.getContentType(fileToUpload));

			return fileDetail;
		} catch (FileNotFoundException fnfe) {
			throw OpenSpecimenException.userError(FormErrorCode.UPLOADED_FILE_NOT_FOUND, filename);
		} finally {
			IOUtils.closeQuietly(fin);
		}
	}
}
