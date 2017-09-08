package com.krishagni.catissueplus.core.de.services.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.TemplateService;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.importer.domain.ObjectSchema;
import com.krishagni.catissueplus.core.importer.domain.ObjectSchema.Record;

import edu.common.dynamicextensions.domain.nui.Container;

public class CustomFieldsSchemaBuilder extends ExtensionSchemaBuilder {

	private TemplateService templateService;

	private String schemaResource;

	private DaoFactory daoFactory;

	public void setTemplateService(TemplateService templateService) {
		this.templateService = templateService;
	}

	public void setSchemaResource(String schemaResource) {
		this.schemaResource = schemaResource;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ObjectSchema getObjectSchema(Map<String, String> params) {
		if (params == null) {
			params = Collections.emptyMap();
		}

		String cpIdStr = params.get("cpId");
		if (StringUtils.isBlank(cpIdStr)) {
			cpIdStr = "-1";
		}

		Long cpId = null;
		try {
			cpId = Long.parseLong(cpIdStr);
		} catch (Exception e) {
			//
			// TODO: Is this right idea to default to -1?
			//
			cpId = -1L;
		}

		CollectionProtocol cp = null;
		if (cpId != -1L && daoFactory != null) {
			cp = daoFactory.getCollectionProtocolDao().getById(cpId);
		}

		return addCustomFields(getSchema(cp), cpId);
	}

	private ObjectSchema getSchema(CollectionProtocol cp) {
		InputStream in = null;
		try {
			in = preprocessSchema(schemaResource, cp);
			return ObjectSchema.parseSchema(in);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private InputStream preprocessSchema(String schemaResource, CollectionProtocol cp) {
		Map<String, Object> params = new HashMap<>();
		params.put("cp", cp);
		String template = templateService.render(schemaResource, params);
		return new ByteArrayInputStream( template.getBytes() );
	}

	private ObjectSchema addCustomFields(ObjectSchema schema, Long cpId) {
		for (Record record : getCustomFieldRecords(schema.getRecord())) {
			addCustomFields(record, cpId);
		}

		return schema;
	}

	private void addCustomFields(Record record, Long cpId) {
		String entityType = record.getEntityType();
		if (StringUtils.isBlank(entityType)) {
			throw OpenSpecimenException.userError(FormErrorCode.ENTITY_TYPE_REQUIRED);
		}

		Map<String, Object> formInfo = DeObject.getFormInfo(cpId, entityType);
		if (formInfo == null) {
			return;
		}

		String formName = (String)formInfo.get("formName");
		Container form = DeObject.getForm(formName);

		Record customFields = getFormRecord(form, false);
		customFields.setAttribute("attrsMap");

		record.setCaption(form.getCaption());
		record.setSubRecords(Collections.singletonList(customFields));
	}

	private List<Record> getCustomFieldRecords(Record record) {
		List<Record> customFieldRecs = new ArrayList<Record>();
		for (Record subRecord : record.getSubRecords()) {
			if (subRecord.getType() != null && subRecord.getType().equals("customFields")) {
				customFieldRecs.add(subRecord);
			}

			customFieldRecs.addAll(getCustomFieldRecords(subRecord));
		}

		return customFieldRecs;
	}


}
