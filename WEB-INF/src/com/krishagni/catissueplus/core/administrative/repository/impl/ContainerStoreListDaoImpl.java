package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList;
import com.krishagni.catissueplus.core.administrative.repository.ContainerStoreListDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ContainerStoreListDaoImpl extends AbstractDao<ContainerStoreList> implements ContainerStoreListDao {

	@Override
	public Class<?> getType() {
		return ContainerStoreList.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ContainerStoreList> getPendingStoreLists(int startAt, int maxLists) {
		return getCurrentSession().getNamedQuery(GET_PENDING_LISTS)
			.setFirstResult(startAt <= 0 ? 0 : startAt)
			.setMaxResults(maxLists <= 0 ? 100 : maxLists)
			.list();
	}

	private static final String FQN = ContainerStoreList.class.getName();

	private static final String GET_PENDING_LISTS = FQN + ".getPendingLists";
}
