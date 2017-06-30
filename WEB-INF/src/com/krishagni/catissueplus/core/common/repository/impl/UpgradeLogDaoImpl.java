package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.UpgradeLog;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.UpgradeLogDao;

public class UpgradeLogDaoImpl extends AbstractDao<UpgradeLog> implements UpgradeLogDao {

	@Override
	@SuppressWarnings("unchecked")
	public UpgradeLog getLatestVersion() {
		List<UpgradeLog> log = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_UPGRADE_LOGS)
			.setMaxResults(1)
			.list();
		
		return log.isEmpty() ? null : log.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<UpgradeLog> getUpgradeLogs() {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_UPGRADE_LOGS)
			.list();
	}
	
	private static final String FQN = UpgradeLog.class.getName();
	
	private static final String GET_UPGRADE_LOGS = FQN + ".getUpgradeLogs";
}