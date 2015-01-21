package com.krishagni.catissueplus.rest.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.administrative.events.BoxScannerDetail;
import com.krishagni.catissueplus.core.administrative.events.GetAllBoxScannerEvent;
import com.krishagni.catissueplus.core.administrative.events.ReqAllBoxScannersEvent;
import com.krishagni.catissueplus.core.administrative.events.ScanStorageContainerDetails;
import com.krishagni.catissueplus.core.administrative.events.ScanStorageContainerDetailsEvents;
import com.krishagni.catissueplus.core.administrative.services.BoxScanService;

@Controller
@RequestMapping("/container-scanners")
public class ContainerScanController {

	@Autowired
	private BoxScanService scannerSvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<BoxScannerDetail> getScannerList() {
		ReqAllBoxScannersEvent req = new ReqAllBoxScannersEvent();
		GetAllBoxScannerEvent resp = scannerSvc.getAllBoxScanners(req);
		if (!resp.isSuccess()) {
			resp.raiseException();
		}
		return resp.getScannerDetails();
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/validateScanData")
	 @ResponseStatus(HttpStatus.OK)
	 @ResponseBody
	 public ScanStorageContainerDetails validateScan(@RequestBody ScanStorageContainerDetails ScanStorageContainerDetails) {
	      ScanStorageContainerDetailsEvents resp = scannerSvc.validateAndPopulateScanContainerData(ScanStorageContainerDetails);
	      if (!resp.isSuccess()) {
	        resp.raiseException();
	       }      
	       return resp.getContainerDetails();
	 }
	   
	    @RequestMapping(method = RequestMethod.GET, value = "/scanContainer")
	 @ResponseStatus(HttpStatus.OK)
	 @ResponseBody
	 public ScanStorageContainerDetails scanContainerData(@RequestParam(value = "ipAddress", required = true, defaultValue = "") String ipAddress,
			 @RequestParam(value = "selCont", required = false, defaultValue = "") String selContName) {
	   ScanStorageContainerDetailsEvents resp = scannerSvc.getScanContainerData(ipAddress,selContName);
	   if (!resp.isSuccess()) {
	     resp.raiseException();
	    }   
	    return resp.getContainerDetails();
	 }
}
