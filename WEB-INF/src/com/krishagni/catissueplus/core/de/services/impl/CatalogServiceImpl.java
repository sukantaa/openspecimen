package com.krishagni.catissueplus.core.de.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.CollectionProtocolCopier;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ObjectCopier.AttributesCopier;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.domain.CpCatalogSetting;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.domain.factory.CpCatalogSettingFactory;
import com.krishagni.catissueplus.core.de.events.CpCatalogSettingDetail;
import com.krishagni.catissueplus.core.de.events.SavedQuerySummary;
import com.krishagni.catissueplus.core.de.services.CatalogService;

public class CatalogServiceImpl implements CatalogService, InitializingBean {
	private static final String CFG_MODULE = "catalog";

	private static final String DEF_QUERY = "default_query";

	private static final String DEF_SPMN_CENTRIC_QUERY = "default_spmn_centric_query";

	private DaoFactory daoFactory;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;
	
	private CpCatalogSettingFactory catalogSettingFactory;
	
	private CollectionProtocolCopier cpCopier;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public void setCatalogSettingFactory(CpCatalogSettingFactory catalogSettingFactory) {
		this.catalogSettingFactory = catalogSettingFactory;
	}

	public void setCpCopier(CollectionProtocolCopier cpCopier) {
		this.cpCopier = cpCopier;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		cpCopier.addAttrCopier(new AttributesCopier<CollectionProtocol>()  {
			@Override
			public void copy(CollectionProtocol source, CollectionProtocol target) {
				target.addOnSaveProc(() -> copyCatalogSettings(source, target));
			}
		});
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SavedQuerySummary> getCpCatalogQuery(RequestEvent<CollectionProtocolSummary> req) {
		try {
			CpCatalogSetting setting = getSetting(req.getPayload());
			if (setting != null) {
				return ResponseEvent.response(SavedQuerySummary.fromSavedQuery(setting.getQuery()));
			}

			Integer queryId = null;
			CollectionProtocol cp = getCp(req.getPayload());
			if (cp.isSpecimenCentric()) {
				queryId = ConfigUtil.getInstance().getIntSetting(CFG_MODULE, DEF_SPMN_CENTRIC_QUERY, null);
			}

			if (queryId == null) {
				queryId = ConfigUtil.getInstance().getIntSetting(CFG_MODULE, DEF_QUERY, null);
			}

			if (queryId == null) {
				return ResponseEvent.response(null);
			}

			SavedQuery query = deDaoFactory.getSavedQueryDao().getQuery(queryId.longValue());
			if (query == null) {
				return ResponseEvent.response(null);
			}

			return ResponseEvent.response(SavedQuerySummary.fromSavedQuery(query));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpCatalogSettingDetail> getCpSetting(RequestEvent<CollectionProtocolSummary> req) {
		try {
			CpCatalogSetting setting = getSetting(req.getPayload());
			if (setting == null) {
				return ResponseEvent.response(null);
			}
			
			AccessCtrlMgr.getInstance().ensureReadCpRights(setting.getCp());
			return ResponseEvent.response(CpCatalogSettingDetail.from(setting));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpCatalogSettingDetail> saveCpSetting(RequestEvent<CpCatalogSettingDetail> req) {
		try {
			CpCatalogSetting setting = catalogSettingFactory.createSetting(req.getPayload());			
			CollectionProtocol cp = setting.getCp();
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
			
			CpCatalogSetting existing = deDaoFactory.getCpCatalogSettingDao().getByCpId(cp.getId());
			if (existing == null) {
				existing = setting;
			} else {
				existing.update(setting);
			}
			
			deDaoFactory.getCpCatalogSettingDao().saveOrUpdate(existing);
			return ResponseEvent.response(CpCatalogSettingDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpCatalogSettingDetail> deleteCpSetting(RequestEvent<CollectionProtocolSummary> req) {
		try {
			CpCatalogSetting setting = getSetting(req.getPayload());
			if (setting == null) {
				return ResponseEvent.response(null);
			}
			
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(setting.getCp());
			setting.setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
			deDaoFactory.getCpCatalogSettingDao().saveOrUpdate(setting);
			return ResponseEvent.response(CpCatalogSettingDetail.from(setting));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	private CpCatalogSetting getSetting(CollectionProtocolSummary cp) {
		CpCatalogSetting setting = null;
		
		if (cp.getId() != null) {
			setting = deDaoFactory.getCpCatalogSettingDao().getByCpId(cp.getId());
		} else if (StringUtils.isNotBlank(cp.getShortTitle())) {
			setting = deDaoFactory.getCpCatalogSettingDao().getByCpShortTitle(cp.getShortTitle());
		}
		
		return setting;
	}
	
	private void copyCatalogSettings(CollectionProtocol source, CollectionProtocol target) {
		CpCatalogSetting existing = deDaoFactory.getCpCatalogSettingDao().getByCpId(source.getId());
		if (existing == null) {
			return;
		}
		
		CpCatalogSetting setting = existing.copyTo(target);
		deDaoFactory.getCpCatalogSettingDao().saveOrUpdate(setting);
	}

	private CollectionProtocol getCp(CollectionProtocolSummary input) {
		CollectionProtocol cp = null;
		Object key = null;

		if (input.getId() != null) {
			key = input.getId();
			cp = daoFactory.getCollectionProtocolDao().getById(input.getId());
		} else if (StringUtils.isNotBlank(input.getShortTitle())) {
			key = input.getShortTitle();
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(input.getShortTitle());
		}

		if (key == null) {
			throw OpenSpecimenException.userError(CpErrorCode.SHORT_TITLE_REQUIRED);
		} else if (cp == null) {
			throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, key);
		}

		return cp;
	}
}