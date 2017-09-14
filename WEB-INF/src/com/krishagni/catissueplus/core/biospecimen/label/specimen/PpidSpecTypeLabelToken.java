package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;

public class PpidSpecTypeLabelToken extends AbstractUniqueIdToken<Specimen> {

	@Autowired
	private DaoFactory daoFactory;

	public PpidSpecTypeLabelToken() {
		this.name = "PPI_SPEC_TYPE_UID";
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		String ppid = specimen.getVisit().getRegistration().getPpid();
		String key = ppid + "_" + specimen.getSpecimenType();
		Long uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(name, key);
		return uniqueId == 1L ? -1 : uniqueId;
	}
}
