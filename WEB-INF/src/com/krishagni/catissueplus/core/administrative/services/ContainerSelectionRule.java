package com.krishagni.catissueplus.core.administrative.services;

import java.util.Map;

import org.hibernate.criterion.Criterion;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;

public interface ContainerSelectionRule {
	String getName();

	String getSql(String containerTabAlias, Map<String, Object> params);

	Criterion getRestriction(String containerObjAlias, Map<String, Object> params);

	boolean eval(StorageContainer container, Map<String, Object> params);
}
