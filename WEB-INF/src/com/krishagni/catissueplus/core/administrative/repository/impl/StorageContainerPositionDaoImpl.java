package com.krishagni.catissueplus.core.administrative.repository.impl;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerPositionDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class StorageContainerPositionDaoImpl extends AbstractDao<StorageContainerPosition> implements StorageContainerPositionDao {
	@Override
	public Class<StorageContainerPosition> getType() {
		return StorageContainerPosition.class;
	}
}
