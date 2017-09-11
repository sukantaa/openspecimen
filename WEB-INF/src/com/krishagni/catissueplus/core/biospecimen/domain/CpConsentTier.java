package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.List;

import org.hibernate.envers.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.repository.CollectionProtocolDao;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.util.Status;

@Configurable
@Audited
public class CpConsentTier extends ConsentTier {
	private CollectionProtocol collectionProtocol;

	@Autowired
	private DaoFactory daoFactory;

	public CollectionProtocol getCollectionProtocol() {
		return collectionProtocol;
	}

	public void setCollectionProtocol(CollectionProtocol collectionProtocol) {
		this.collectionProtocol = collectionProtocol;
	}

	public List<DependentEntityDetail> getDependentEntities() {
		CollectionProtocolDao cpDao = daoFactory.getCollectionProtocolDao();
		int responseCount = cpDao.getConsentRespsCount(this.getId());

		return DependentEntityDetail
			.listBuilder()
			.add(ConsentTierResponse.getEntityName(), responseCount)
			.build();
	}

	public CpConsentTier copy() {
		CpConsentTier result = new CpConsentTier();
		result.setStatement(getStatement());
		result.setActivityStatus(getActivityStatus());
		return result;
	}

	public void updateStatus(String activityStatus) {
		//
		// TODO: update the user and time in audit when the status is changed.
		//
		if (getActivityStatus().equals(activityStatus)) {
			return;
		}

		if (Status.ACTIVITY_STATUS_CLOSED.getStatus().equals(activityStatus)) {
			close();
		} else if (Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(activityStatus)) {
			activate();
		}
	}

	private void close() {
		setActivityStatus(Status.ACTIVITY_STATUS_CLOSED.getStatus());
	}

	private void activate() {
		setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
	}
}
