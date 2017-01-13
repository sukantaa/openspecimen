package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenKit;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenKitSummary;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface SpecimenKitDao extends Dao<SpecimenKit> {
    List<SpecimenKitSummary> getSpecimenKits(SpecimenKitListCriteria criteria);
}
