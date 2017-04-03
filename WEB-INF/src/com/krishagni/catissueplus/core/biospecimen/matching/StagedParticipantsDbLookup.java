package com.krishagni.catissueplus.core.biospecimen.matching;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;

public class StagedParticipantsDbLookup implements ParticipantLookupLogic {
	private ParticipantLookupLogic osDbLookup;

	private DaoFactory daoFactory;

	public void setOsDbLookup(LocalDbParticipantLookupImpl osDbLookup) {
		this.osDbLookup = osDbLookup;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public List<MatchedParticipant> getMatchingParticipants(ParticipantDetail detail) {
		StagedParticipant stagingParticipant = null;

		//
		// If eMPI is entered then get the participant from staging DB by eMPI
		//
		if (StringUtils.isNotBlank(detail.getEmpi())) {
			stagingParticipant = daoFactory.getStagedParticipantDao().getByEmpi(detail.getEmpi());
		}

		//
		// If no matching found in staging DB then get matching from local DB and return
		//
		if (stagingParticipant == null) {
			return osDbLookup.getMatchingParticipants(detail);
		}

		//
		// Matching found in staging DB, now check if patient exist in OS with same eMPI
		//
		Participant osParticipant = daoFactory.getParticipantDao().getByEmpi(detail.getEmpi());

		//
		// If found in OS then return else populate the PV mapping for the staging participant and return
		//
		ParticipantDetail result;
		if (osParticipant != null) {
			result = ParticipantDetail.from(osParticipant, false);
		} else {
			result = createStagedParticipantDetail(stagingParticipant);
		}

		return Collections.singletonList(new MatchedParticipant(result, Collections.singletonList("empi")));
	}

	//Populating the participant details from staging participant
	private ParticipantDetail createStagedParticipantDetail(StagedParticipant participant) {
		StagedParticipantDetail detail = StagedParticipantDetail.from(participant);
		detail.setUpdatedTime(null);
		return detail;
	}
}
