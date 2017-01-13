package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenKit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenKitFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.impl.SpecimenKitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenKitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenKitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenKitListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenKitService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public class SpecimenKitServiceImpl implements SpecimenKitService {

	private DaoFactory daoFactory;

	private SpecimenKitFactory specimenKitFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setSpecimenKitFactory(SpecimenKitFactory specimenKitFactory) {
		this.specimenKitFactory = specimenKitFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenKitSummary>> getSpecimenKits(RequestEvent<SpecimenKitListCriteria> req) {
		try {
			return ResponseEvent.response(daoFactory.getSpecimenKitDao().getSpecimenKits(req.getPayload()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenKitDetail> getSpecimenKit(RequestEvent<Long> req) {
		try {
			return ResponseEvent.response(SpecimenKitDetail.from(getKit(req.getPayload())));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenKitDetail> createSpecimenKit(RequestEvent<SpecimenKitDetail> req) {
		try {
			SpecimenKit kit = specimenKitFactory.createSpecimenKit(req.getPayload());
			daoFactory.getSpecimenKitDao().saveOrUpdate(kit);
			return ResponseEvent.response(SpecimenKitDetail.from(kit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenKitDetail> updateSpecimenKit(RequestEvent<SpecimenKitDetail> req) {
		try {
			SpecimenKitDetail detail = req.getPayload();
			SpecimenKit existing = getKit(detail.getId());

			SpecimenKit kit = specimenKitFactory.createSpecimenKit(detail);
			existing.update(kit);

			daoFactory.getSpecimenKitDao().saveOrUpdate(existing);
			return ResponseEvent.response(SpecimenKitDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public SpecimenKit createSpecimenKit(SpecimenKitDetail kitDetail, List<Specimen> specimens) {
		SpecimenKit kit = specimenKitFactory.createSpecimenKit(kitDetail, specimens);
		daoFactory.getSpecimenKitDao().saveOrUpdate(kit);
		return kit;
	}

	private SpecimenKit getKit(Long kitId) {
		SpecimenKit kit = daoFactory.getSpecimenKitDao().getById(kitId);

		if (kit == null) {
			throw OpenSpecimenException.userError(SpecimenKitErrorCode.NOT_FOUND, kitId);
		}

		return kit;
	}
}
