
package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;

public interface SpecimenFactory {
	Specimen createSpecimen(SpecimenDetail specimenDetail, Specimen parent);
	
	Specimen createSpecimen(Specimen existing, SpecimenDetail specimenDetail, Specimen parent);
}
