package com.krishagni.catissueplus.core.administrative.repository;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface ContainerStoreListDao extends Dao<ContainerStoreList> {
	List<ContainerStoreList> getPendingStoreLists(int startAt, int maxLists);
}
