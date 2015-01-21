package com.krishagni.catissueplus.core.administrative.services;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.events.GetAllBoxScannerEvent;
import com.krishagni.catissueplus.core.administrative.events.ReqAllBoxScannersEvent;
import com.krishagni.catissueplus.core.administrative.events.ScanContainerSpecimenDetails;
import com.krishagni.catissueplus.core.administrative.events.ScanStorageContainerDetails;
import com.krishagni.catissueplus.core.administrative.events.ScanStorageContainerDetailsEvents;


public interface BoxScanService {

//	public BoxScannerCreatedEvent save(CreateBiohazardEvent reqEvent);
//
//	public BiohazardUpdatedEvent update(UpdateBiohazardEvent reqEvent);

	public GetAllBoxScannerEvent getAllBoxScanners(ReqAllBoxScannersEvent reqEvent);
	public ScanStorageContainerDetailsEvents validateAndPopulateScanContainerData(ScanStorageContainerDetails details);
	public ScanStorageContainerDetailsEvents getScanContainerData(String scannerName, String selContName);
	public void resolveConflicts(List<ScanContainerSpecimenDetails> conflictedSpecimenList);
}
