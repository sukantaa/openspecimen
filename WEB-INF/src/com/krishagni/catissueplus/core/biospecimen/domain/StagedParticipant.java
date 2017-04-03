package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class StagedParticipant extends Participant {
	
	private Date updatedTime;
	
	private Set<StagedParticipantMedicalIdentifier> pmiList = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Date updatedTime) {
		this.updatedTime = updatedTime;
	}
	
	public Set<StagedParticipantMedicalIdentifier> getPmiList() {
		return pmiList;
	}

	public void setPmiList(Set<StagedParticipantMedicalIdentifier> pmiList) {
		this.pmiList = pmiList;
	}

	public void update(StagedParticipant participant) {
		super.update(participant);
		setUpdatedTime(participant.getUpdatedTime());
		updatePmis(participant);
	}

	private void updatePmis(StagedParticipant participant) {
		for (StagedParticipantMedicalIdentifier pmi : participant.getPmiList()) {
			StagedParticipantMedicalIdentifier existing = getPmiBySite(getPmiList(), pmi.getSite());
			if (existing == null) {
				StagedParticipantMedicalIdentifier newPmi = new StagedParticipantMedicalIdentifier();
				newPmi.setParticipant(this);
				newPmi.setSite(pmi.getSite());
				newPmi.setMedicalRecordNumber(pmi.getMedicalRecordNumber());
				getPmiList().add(newPmi);
			} else {
				existing.setMedicalRecordNumber(pmi.getMedicalRecordNumber());
			}
		}

		Iterator<StagedParticipantMedicalIdentifier> iter = getPmiList().iterator();
		while (iter.hasNext()) {
			StagedParticipantMedicalIdentifier existing = iter.next();
			if (getPmiBySite(participant.getPmiList(), existing.getSite()) == null) {
				iter.remove();
			}
		}
	}

	private StagedParticipantMedicalIdentifier getPmiBySite(Collection<StagedParticipantMedicalIdentifier> pmis, String siteName) {
		StagedParticipantMedicalIdentifier result = null;

		for (StagedParticipantMedicalIdentifier pmi : pmis) {
			if (pmi.getSite().equals(siteName)) {
				result = pmi;
				break;
			}
		}

		return result;
	}
}
