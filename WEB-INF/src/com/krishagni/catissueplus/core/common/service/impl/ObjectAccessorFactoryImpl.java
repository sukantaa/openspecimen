package com.krishagni.catissueplus.core.common.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.ObjectAccessorFactory;

public class ObjectAccessorFactoryImpl implements ObjectAccessorFactory {
	private Map<String, ObjectAccessor> resolvers = new HashMap<>();

	@Override
	public ObjectAccessor getAccessor(String objectName) {
		return resolvers.get(objectName);
	}

	public void setResolvers(List<ObjectAccessor> resolvers) {
		resolvers.forEach(resolver -> this.resolvers.put(resolver.getObjectName(), resolver));
	}
}
