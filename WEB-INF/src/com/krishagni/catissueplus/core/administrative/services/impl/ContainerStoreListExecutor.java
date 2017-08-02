package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;

@Configurable
public class ContainerStoreListExecutor implements ScheduledTask {
	private final static int MAX_PENDING_LISTS_TO_FETCH = 15;

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun jobRun)
	throws Exception {
		boolean hasPendingLists = true;
		while (hasPendingLists) {
			List<ContainerStoreList> storeLists = getPendingStoreLists();
			storeLists.forEach(ContainerStoreList::process);
			hasPendingLists = (storeLists.size() >= MAX_PENDING_LISTS_TO_FETCH);
		}
	}

	@PlusTransactional
	private List<ContainerStoreList> getPendingStoreLists() {
		List<ContainerStoreList> storeLists = daoFactory.getContainerStoreListDao().getPendingStoreLists(0, MAX_PENDING_LISTS_TO_FETCH);
		for (ContainerStoreList list : storeLists) {
			//
			// Touch the auto freezer provider so that its data is already loaded while we are in transaction.
			//
			list.getContainer().getAutoFreezerProvider().getImplClass();
		}

		return storeLists;
	}
}
