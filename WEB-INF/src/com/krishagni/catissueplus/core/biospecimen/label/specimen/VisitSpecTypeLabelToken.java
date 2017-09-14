package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class VisitSpecTypeLabelToken extends AbstractUniqueIdToken<Specimen> {

	@Autowired
	private DaoFactory daoFactory;

	public VisitSpecTypeLabelToken() {
		this.name = "VISIT_SP_TYPE_UID";
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		String visitName = specimen.getVisit().getName();
		String key = visitName + "_" + specimen.getSpecimenType();
		Long uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(name, key);
		return uniqueId == 1L ? -1 : uniqueId;
	}
}
