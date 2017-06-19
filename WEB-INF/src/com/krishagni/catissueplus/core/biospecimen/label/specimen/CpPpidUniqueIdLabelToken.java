package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;

public class CpPpidUniqueIdLabelToken extends AbstractSpecimenLabelToken {

	@Autowired
	private DaoFactory daoFactory;

	public CpPpidUniqueIdLabelToken() {
		this.name = "CP_PPI_UID";
	}

	@Override
	public String getLabel(Specimen specimen) {
		String ppid = specimen.getVisit().getRegistration().getPpid();
		String cpId = specimen.getCollectionProtocol().getId().toString();
		String key = cpId + "_" + ppid;
		Long uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(name, key);
		return uniqueId.toString();
	}
	
	@Override
	public int validate(Object object, String input, int startIdx, String ... args) {
		return super.validateNumber(input, startIdx);
	}
}
