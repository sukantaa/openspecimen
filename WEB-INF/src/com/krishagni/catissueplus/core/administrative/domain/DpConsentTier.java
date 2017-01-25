package com.krishagni.catissueplus.core.administrative.domain;

import com.krishagni.catissueplus.core.biospecimen.domain.ConsentTier;

public class DpConsentTier extends ConsentTier {
	private DistributionProtocol distributionProtocol;

	public DistributionProtocol getDistributionProtocol() {
		return distributionProtocol;
	}

	public void setDistributionProtocol(DistributionProtocol distributionProtocol) {
		this.distributionProtocol = distributionProtocol;
	}

}
