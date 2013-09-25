/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-core/LICENSE.txt for details.
 */

package gov.nih.nci.caxchange.service.globus;

import gov.nih.nci.caxchange.service.CaXchangeConsumerServiceImpl;

import java.rmi.RemoteException;

/** 
 * DO NOT EDIT:  This class is autogenerated!
 *
 * This class implements each method in the portType of the service.  Each method call represented
 * in the port type will be then mapped into the unwrapped implementation which the user provides
 * in the CaXchangeConsumerServiceImpl class.  This class handles the boxing and unboxing of each method call
 * so that it can be correctly mapped in the unboxed interface that the developer has designed and 
 * has implemented.  Authorization callbacks are automatically made for each method based
 * on each methods authorization requirements.
 * 
 * @created by Introduce Toolkit version 1.4
 * 
 */
public class CaXchangeConsumerServiceProviderImpl{
	
	CaXchangeConsumerServiceImpl impl;
	
	public CaXchangeConsumerServiceProviderImpl() throws RemoteException {
		impl = new CaXchangeConsumerServiceImpl();
	}
	

    public gov.nih.nci.caxchange.stubs.ProcessResponse process(gov.nih.nci.caxchange.stubs.ProcessRequest params) throws RemoteException {
    gov.nih.nci.caxchange.stubs.ProcessResponse boxedResult = new gov.nih.nci.caxchange.stubs.ProcessResponse();
    boxedResult.setCaXchangeConsumerResponse(impl.process(params.getCaXchangeRequestMessage().getCaXchangeRequestMessage()));
    return boxedResult;
  }

}
