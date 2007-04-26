/**
 * <p>Title: SpecimenCollectionGroupAction Class>
 * <p>Description:	SpecimenCollectionGroupAction initializes the fields in the 
 * New Specimen Collection Group page.</p>
 * Copyright:    Copyright (c) year
 * Company: Washington University, School of Medicine, St. Louis.
 * @author Ajay Sharma
 * @version 1.00
 */

package edu.wustl.catissuecore.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import edu.wustl.catissuecore.actionForm.SpecimenCollectionGroupForm;
import edu.wustl.catissuecore.bizlogic.BizLogicFactory;
import edu.wustl.catissuecore.bizlogic.SpecimenCollectionGroupBizLogic;
import edu.wustl.catissuecore.bizlogic.UserBizLogic;
import edu.wustl.catissuecore.domain.CollectionEventParameters;
import edu.wustl.catissuecore.domain.CollectionProtocol;
import edu.wustl.catissuecore.domain.CollectionProtocolEvent;
import edu.wustl.catissuecore.domain.CollectionProtocolRegistration;
import edu.wustl.catissuecore.domain.Participant;
import edu.wustl.catissuecore.domain.ParticipantMedicalIdentifier;
import edu.wustl.catissuecore.domain.ReceivedEventParameters;
import edu.wustl.catissuecore.domain.Site;
import edu.wustl.catissuecore.domain.SpecimenCollectionGroup;
import edu.wustl.catissuecore.util.EventsUtil;
import edu.wustl.catissuecore.util.global.Constants;
import edu.wustl.catissuecore.util.global.DefaultValueManager;
import edu.wustl.catissuecore.util.global.Utility;
import edu.wustl.catissuecore.util.global.Variables;
import edu.wustl.common.action.SecureAction;
import edu.wustl.common.beans.NameValueBean;
import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.bizlogic.CDEBizLogic;
import edu.wustl.common.bizlogic.IBizLogic;
import edu.wustl.common.cde.CDE;
import edu.wustl.common.cde.CDEManager;
import edu.wustl.common.util.dbManager.DAOException;
import edu.wustl.common.util.logger.Logger;


/**
 * SpecimenCollectionGroupAction initializes the fields in the 
 * New Specimen Collection Group page.
 * @author ajay_sharma
 */
public class SpecimenCollectionGroupAction  extends SecureAction
{   
	/**
	 * Overrides the execute method of Action class.
	 */
	public ActionForward executeSecureAction(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception
			{
		SpecimenCollectionGroupForm  specimenCollectionGroupForm = (SpecimenCollectionGroupForm)form;
		Logger.out.debug("SCGA : " + specimenCollectionGroupForm.getId() );
		
		//	set the menu selection 
		request.setAttribute(Constants.MENU_SELECTED, "14"  ); 

		//pageOf and operation attributes required for Advance Query Object view.
		String pageOf = request.getParameter(Constants.PAGEOF);

		//Gets the value of the operation parameter.
		String operation = (String)request.getParameter(Constants.OPERATION);

		//Sets the operation attribute to be used in the Edit/View Specimen Collection Group Page in Advance Search Object View. 
		request.setAttribute(Constants.OPERATION,operation);
		if(operation.equalsIgnoreCase(Constants.ADD ) )
		{
			specimenCollectionGroupForm.setId(0);
			Logger.out.debug("SCGA : set to 0 "+ specimenCollectionGroupForm.getId() );
		}

		boolean isOnChange = false; 
		String str = request.getParameter("isOnChange");
		if(str!=null)
		{
			if(str.equals("true"))
				isOnChange = true; 
		}

		// get list of Protocol title.
		SpecimenCollectionGroupBizLogic bizLogic = (SpecimenCollectionGroupBizLogic)BizLogicFactory.getInstance().getBizLogic(Constants.SPECIMEN_COLLECTION_GROUP_FORM_ID);
		//populating protocolist bean.
		String sourceObjectName = CollectionProtocol.class.getName();
		String [] displayNameFields = {"title"};
		String valueField = Constants.SYSTEM_IDENTIFIER;
		List list = bizLogic.getList(sourceObjectName,displayNameFields,valueField, true);
		request.setAttribute(Constants.PROTOCOL_LIST, list);

		//Populating the Site Type bean
		sourceObjectName = Site.class.getName();
		String siteDisplaySiteFields[] = {"name"};
		list = bizLogic.getList(sourceObjectName,siteDisplaySiteFields,valueField, true);
		request.setAttribute(Constants.SITELIST, list);

		//Populating the participants registered to a given protocol
		loadPaticipants(specimenCollectionGroupForm.getCollectionProtocolId() , bizLogic, request);

		//Populating the protocol participants id registered to a given protocol
		loadPaticipantNumberList(specimenCollectionGroupForm.getCollectionProtocolId(),bizLogic,request);

		//Populating the Collection Protocol Events
		loadCollectionProtocolEvent(specimenCollectionGroupForm.getCollectionProtocolId(),bizLogic,request);


		String protocolParticipantId = specimenCollectionGroupForm.getProtocolParticipantIdentifier();
		//Populating the participants Medical Identifier for a given participant
		loadParticipantMedicalIdentifier(specimenCollectionGroupForm.getParticipantId(),bizLogic, request);

		//Load Clinical status for a given study calander event point
		String changeOn = request.getParameter(Constants.CHANGE_ON);
		List calendarEventPointList = null;
		if(changeOn != null && changeOn.equals(Constants.COLLECTION_PROTOCOL_ID))
		{
			calendarEventPointList = new ArrayList(); 
		}
		else
		{
			calendarEventPointList = bizLogic.retrieve(CollectionProtocolEvent.class.getName(),
				Constants.SYSTEM_IDENTIFIER,
				new Long(specimenCollectionGroupForm.getCollectionProtocolEventId()));
		}
		// The values of restrict checkbox and the number of specimen must alos populate in edit mode.
		if((isOnChange || operation.equalsIgnoreCase(Constants.EDIT)))
		{
			//Patch ID: Bug#3184_27
			int numberOfSpecimen = 1;
			if(!calendarEventPointList.isEmpty())
			{
				CollectionProtocolEvent collectionProtocolEvent = (CollectionProtocolEvent)calendarEventPointList.get(0);
				specimenCollectionGroupForm.setClinicalStatus(collectionProtocolEvent.getClinicalStatus());

				/**
				 * Patch ID: Bug#3184_9
				 */
				Collection specimenRequirementCollection = collectionProtocolEvent.getSpecimenRequirementCollection();
				if((specimenRequirementCollection != null) && (!specimenRequirementCollection.isEmpty()))
				{
					//Populate the number of Specimen Requirements.
					numberOfSpecimen = specimenRequirementCollection.size();
					//Set checkbox status depending upon the days of study calendar event point. If it is zero, then unset the restrict
					//checkbox, otherwise set the restrict checkbox
					Double studyCalendarEventPoint = collectionProtocolEvent.getStudyCalendarEventPoint();
					if(studyCalendarEventPoint.doubleValue() == 0)
					{
						specimenCollectionGroupForm.setRestrictSCGCheckbox("false");
					}
					else
					{
						specimenCollectionGroupForm.setRestrictSCGCheckbox("true");
					}
				}
			}
			else if(calendarEventPointList.isEmpty())
			{
				//Set checkbox status
				specimenCollectionGroupForm.setRestrictSCGCheckbox("false");
			}
			//Sets the value for number of specimen field on the specimen collection group page. 
			specimenCollectionGroupForm.setNumberOfSpecimens(numberOfSpecimen);
			request.setAttribute(Constants.NUMBER_OF_SPECIMEN, numberOfSpecimen);
			//Set the number of actual specimen requirements for validation purpose.
			//This value is used in validate method of SpecimenCollectionGroupForm.java.
			request.setAttribute(Constants.NUMBER_OF_SPECIMEN_REQUIREMENTS, numberOfSpecimen + "");
		}

		// populating clinical Diagnosis field 
		CDE cde = CDEManager.getCDEManager().getCDE(Constants.CDE_NAME_CLINICAL_DIAGNOSIS);
		CDEBizLogic cdeBizLogic = (CDEBizLogic)BizLogicFactory.getInstance().getBizLogic(Constants.CDE_FORM_ID);
		List clinicalDiagnosisList = new ArrayList();
		clinicalDiagnosisList.add(new NameValueBean(Constants.SELECT_OPTION,""+Constants.SELECT_OPTION_VALUE));
		cdeBizLogic.getFilteredCDE(cde.getPermissibleValues(),clinicalDiagnosisList);
		request.setAttribute(Constants.CLINICAL_DIAGNOSIS_LIST, clinicalDiagnosisList);


		// populating clinical Status field
		//		NameValueBean undefinedVal = new NameValueBean(Constants.UNDEFINED,Constants.UNDEFINED);
		List clinicalStatusList = CDEManager.getCDEManager().getPermissibleValueList(Constants.CDE_NAME_CLINICAL_STATUS,null);
		request.setAttribute(Constants.CLINICAL_STATUS_LIST, clinicalStatusList);

		//Sets the activityStatusList attribute to be used in the Site Add/Edit Page.
		request.setAttribute(Constants.ACTIVITYSTATUSLIST, Constants.ACTIVITY_STATUS_VALUES);


		Logger.out.debug("CP ID in SCG Action======>"+specimenCollectionGroupForm.getCollectionProtocolId());
		Logger.out.debug("Participant ID in SCG Action=====>"+specimenCollectionGroupForm.getParticipantId()+"  "+specimenCollectionGroupForm.getProtocolParticipantIdentifier());

		// -------called from Collection Protocol Registration start-------------------------------
		if( (request.getAttribute(Constants.SUBMITTED_FOR) !=null) &&(request.getAttribute(Constants.SUBMITTED_FOR).equals("Default")))
		{
			Logger.out.debug("Populating CP and Participant in SCG ====  AddNew operation loop");

			Long cprId =new Long(specimenCollectionGroupForm.getCollectionProtocolRegistrationId());

			if(cprId != null)
			{
				List collectionProtocolRegistrationList = bizLogic.retrieve(CollectionProtocolRegistration.class.getName(),
						Constants.SYSTEM_IDENTIFIER,cprId);
				if(!collectionProtocolRegistrationList.isEmpty())
				{
					Object  obj = collectionProtocolRegistrationList.get(0 ); 
					CollectionProtocolRegistration cpr = (CollectionProtocolRegistration)obj;

					long cpID = cpr.getCollectionProtocol().getId().longValue();
					long pID = cpr.getParticipant().getId().longValue();
					String ppID = cpr.getProtocolParticipantIdentifier();

					Logger.out.debug("cpID : "+ cpID + "   ||  pID : " + pID + "    || ppID : " + ppID );

					specimenCollectionGroupForm.setCollectionProtocolId(cpID);

					//Populating the participants registered to a given protocol
					loadPaticipants(cpID , bizLogic, request);
					loadPaticipantNumberList(specimenCollectionGroupForm.getCollectionProtocolId(),bizLogic,request);

					String firstName = Utility.toString(cpr.getParticipant().getFirstName());;
					String lastName = Utility.toString(cpr.getParticipant().getLastName());
					String birthDate = Utility.toString(cpr.getParticipant().getBirthDate());
					String ssn = Utility.toString(cpr.getParticipant().getSocialSecurityNumber());
					if(firstName.trim().length()>0 || lastName.trim().length()>0 || birthDate.trim().length()>0 || ssn.trim().length()>0)
					{
						specimenCollectionGroupForm.setParticipantId(pID );
						specimenCollectionGroupForm.setCheckedButton(1); 
					}	
					//Populating the protocol participants id registered to a given protocol

					else if(cpr.getProtocolParticipantIdentifier() != null)
					{
						specimenCollectionGroupForm.setProtocolParticipantIdentifier(ppID );
						specimenCollectionGroupForm.setCheckedButton(2); 
					}

					//Populating the Collection Protocol Events
					loadCollectionProtocolEvent(specimenCollectionGroupForm.getCollectionProtocolId(),bizLogic,request);

					//Load Clinical status for a given study calander event point
					calendarEventPointList = bizLogic.retrieve(CollectionProtocolEvent.class.getName(),
							Constants.SYSTEM_IDENTIFIER,
							new Long(specimenCollectionGroupForm.getCollectionProtocolEventId()));
					if(isOnChange && !calendarEventPointList.isEmpty())
					{
						CollectionProtocolEvent collectionProtocolEvent = (CollectionProtocolEvent)calendarEventPointList.get(0);
						specimenCollectionGroupForm.setClinicalStatus(collectionProtocolEvent.getClinicalStatus());
					}
				}
			}
			request.setAttribute(Constants.SUBMITTED_FOR, "Default");
		}


		//*************  ForwardTo implementation *************
		HashMap forwardToHashMap=(HashMap)request.getAttribute("forwardToHashMap");

		if(forwardToHashMap !=null)
		{
			Long collectionProtocolId = (Long)forwardToHashMap.get("collectionProtocolId");
			if(collectionProtocolId == null && request.getParameter("cpId") != null && !request.getParameter("cpId").equals("null"))
			{
				collectionProtocolId = new Long(request.getParameter("cpId"));
			}

			Long participantId=(Long)forwardToHashMap.get("participantId");
			String participantProtocolId = (String) forwardToHashMap.get("participantProtocolId");

			specimenCollectionGroupForm.setCollectionProtocolId(collectionProtocolId.longValue());

			if(participantId != null && participantId.longValue() != 0)
			{    
				//Populating the participants registered to a given protocol
				loadPaticipants(collectionProtocolId.longValue(), bizLogic, request);

				loadPaticipantNumberList(specimenCollectionGroupForm.getCollectionProtocolId(),bizLogic,request);

				specimenCollectionGroupForm.setParticipantId(participantId.longValue());
				specimenCollectionGroupForm.setCheckedButton(1);
				request.setAttribute(Constants.CP_SEARCH_PARTICIPANT_ID,participantId.toString());

 				/**
				 * Name : Deepti Shelar
				 * Reviewer Name : Sachin Lale
				 * Bug id : FutureSCG
				 * Patch Id : FutureSCG_1
				 * Description : setting participantProtocolId to form
				 */
				if(participantProtocolId == null)
				{
					participantProtocolId = getParticipantProtocolIdForCPAndParticipantId(participantId.toString(),collectionProtocolId.toString(),bizLogic);
					if(participantProtocolId != null)
					{
						specimenCollectionGroupForm.setProtocolParticipantIdentifier(participantProtocolId);
						specimenCollectionGroupForm.setCheckedButton(2);
					}
				}
			}
			else if(participantProtocolId != null)
			{
				//Populating the participants registered to a given protocol
				loadPaticipants(collectionProtocolId.longValue(), bizLogic, request);

				loadPaticipantNumberList(specimenCollectionGroupForm.getCollectionProtocolId(),bizLogic,request);
				specimenCollectionGroupForm.setProtocolParticipantIdentifier(participantProtocolId);
				specimenCollectionGroupForm.setCheckedButton(2);
				String cpParticipantId = getParticipantIdForProtocolId(participantProtocolId,bizLogic);
				if(cpParticipantId != null)
				{
					request.setAttribute(Constants.CP_SEARCH_PARTICIPANT_ID,cpParticipantId);
				}
			}
			/**
			 * Patch Id : FutureSCG_3
			 * Description : Setting number of specimens and restricted checkbox
			 */
			Long cpeId = (Long)forwardToHashMap.get("COLLECTION_PROTOCOL_EVENT_ID");
			if(cpeId != null)
			{
				specimenCollectionGroupForm.setCollectionProtocolEventId(cpeId);
				List cpeList = bizLogic.retrieve(CollectionProtocolEvent.class.getName(),Constants.SYSTEM_IDENTIFIER,cpeId);
				if(!cpeList.isEmpty())
				{
					setNumberOfSpecimens(request, specimenCollectionGroupForm, cpeList);
				}
			}
			//Bug 1915:SpecimenCollectionGroup.Study Calendar Event Point not populated when page is loaded through proceedTo
			//Populating the Collection Protocol Events
			loadCollectionProtocolEvent(specimenCollectionGroupForm.getCollectionProtocolId(),bizLogic,request);

			//Load Clinical status for a given study calander event point
			calendarEventPointList = bizLogic.retrieve(CollectionProtocolEvent.class.getName(),
					Constants.SYSTEM_IDENTIFIER,
					new Long(specimenCollectionGroupForm.getCollectionProtocolEventId()));
			if(!calendarEventPointList.isEmpty())
			{
				CollectionProtocolEvent collectionProtocolEvent = (CollectionProtocolEvent)calendarEventPointList.get(0);
				specimenCollectionGroupForm.setClinicalStatus(collectionProtocolEvent.getClinicalStatus());
			}

			Logger.out.debug("CollectionProtocolID found in forwardToHashMap========>>>>>>"+collectionProtocolId);
			Logger.out.debug("ParticipantID found in forwardToHashMap========>>>>>>"+participantId);
			Logger.out.debug("ParticipantProtocolID found in forwardToHashMap========>>>>>>"+participantProtocolId);
		}
		//*************  ForwardTo implementation *************
		//Populate the group name field with default value in the form of 
		//<Collection Protocol Name>_<Participant ID>_<Group Id>
		int groupNumber=bizLogic.getNextGroupNumber();

		//Get the collection protocol title for the collection protocol Id selected
		String collectionProtocolTitle = "";
		list = bizLogic.retrieve(CollectionProtocol.class.getName(),valueField,new Long(specimenCollectionGroupForm.getCollectionProtocolId()));

		if(!list.isEmpty())
		{
			CollectionProtocol collectionProtocol = (CollectionProtocol)list.get(0);
			collectionProtocolTitle=collectionProtocol.getTitle();
		}

		long groupParticipantId = specimenCollectionGroupForm.getParticipantId();
		//check if the reset name link was clicked
		String resetName = request.getParameter(Constants.RESET_NAME);

		//Set the name to default if reset name link was clicked or page is loading for first time 
		//through add link or forward to link 
		if(forwardToHashMap !=null || (specimenCollectionGroupForm.getName()!=null && specimenCollectionGroupForm.getName().equals(""))
				|| (resetName!=null && resetName.equals("Yes")))
		{
			if(!collectionProtocolTitle.equals("")&& (groupParticipantId>0 ||
					(protocolParticipantId!=null && !protocolParticipantId.equals(""))))
			{
				//Poornima:Bug 2833 - Error thrown when adding a specimen collection group
				//Max length of CP is 150 and Max length of SCG is 55, in Oracle the name does not truncate 
				//and it is giving error. So the title is truncated in case it is longer than 30 .
				String maxCollTitle = collectionProtocolTitle;
				if(collectionProtocolTitle.length()>30)
				{
					maxCollTitle = collectionProtocolTitle.substring(0,29);
				}
				//During add operation the id to set in the default name is generated
				if(operation.equals(Constants.ADD))
				{
					//if participant is selected from the list
					if(groupParticipantId>0) 
					{
						specimenCollectionGroupForm.setName(maxCollTitle+"_"+groupParticipantId+"_"+groupNumber);
					}
					//else if participant protocol Id is selected 
					else
					{
						specimenCollectionGroupForm.setName(maxCollTitle+"_"+groupParticipantId+"_"+groupNumber);
					}
				}
				//During edit operation the id to set in the default name using the id
				else if(operation.equals(Constants.EDIT) && (resetName!=null && resetName.equals("Yes")))
				{
					if(groupParticipantId>0) 
					{
						specimenCollectionGroupForm.setName(maxCollTitle+"_"+groupParticipantId+"_"+
								specimenCollectionGroupForm.getId());

					}
					else
					{
						specimenCollectionGroupForm.setName(maxCollTitle+"_"+protocolParticipantId+"_"+
								specimenCollectionGroupForm.getId()); 
					}
				}
			}
		}

		request.setAttribute(Constants.PAGEOF,pageOf);
		Logger.out.debug("page of in Specimen coll grp action:"+request.getParameter(Constants.PAGEOF));
		// -------called from Collection Protocol Registration end -------------------------------
		/**
		 * Name : Ashish Gupta
		 * Reviewer Name : Sachin Lale 
		 * Bug ID: 2741
		 * Patch ID: 2741_11	 
		 * Description: Methods to set default events on SCG page
		 */
		setDefaultEvents(request,specimenCollectionGroupForm,operation);

		request.setAttribute("scgForm", specimenCollectionGroupForm);
		/* Bug ID: 4135
	 	* Patch ID: 4135_2	 
	 	* Description: Setting the ids in collection and received events associated with this scg
		*/
		//When opening in Edit mode, to set the ids of collection event parameters and received event parameters
		if(specimenCollectionGroupForm.getId() != 0)
		{
			setEventsId(specimenCollectionGroupForm,bizLogic);
		}
		
		return mapping.findForward(pageOf);
			}
	/**
	 * Patch Id : FutureSCG_4
	 * Description : method to set Number Of Specimens
	 */
	/**
	 * @param request
	 * @param specimenCollectionGroupForm
	 * @param calendarEventPointList
	 */
	private void setNumberOfSpecimens(HttpServletRequest request, SpecimenCollectionGroupForm specimenCollectionGroupForm, List calendarEventPointList) 
	{
		CollectionProtocolEvent collectionProtocolEvent = (CollectionProtocolEvent)calendarEventPointList.get(0);
		specimenCollectionGroupForm.setClinicalStatus(collectionProtocolEvent.getClinicalStatus());
		Collection specimenRequirementCollection = collectionProtocolEvent.getSpecimenRequirementCollection();
		if((specimenRequirementCollection != null) && (!specimenRequirementCollection.isEmpty()))
		{
			int numberOfSpecimen = specimenRequirementCollection.size();
			specimenCollectionGroupForm.setNumberOfSpecimens(numberOfSpecimen);
			request.setAttribute(Constants.NUMBER_OF_SPECIMEN, numberOfSpecimen);
			specimenCollectionGroupForm.setRestrictSCGCheckbox("true");
		}
	}
	/**
	 * @param specimenCollectionGroupForm
	 * @param bizLogic
	 * @throws DAOException
	 */
	private void setEventsId(SpecimenCollectionGroupForm specimenCollectionGroupForm,SpecimenCollectionGroupBizLogic bizLogic)throws DAOException
	{
		String scgId = ""+specimenCollectionGroupForm.getId();
		List scglist = bizLogic.retrieve(SpecimenCollectionGroup.class.getName(),"id",scgId);
		if(scglist != null && !scglist.isEmpty())
		{
			SpecimenCollectionGroup scg = (SpecimenCollectionGroup)scglist.get(0);
			Collection eventsColl = scg.getSpecimenEventParametersCollection();
			CollectionEventParameters collectionEventParameters = null;
			ReceivedEventParameters receivedEventParameters = null;
			if(eventsColl != null && !eventsColl.isEmpty())
			{
				Iterator iter = eventsColl.iterator();
				while(iter.hasNext())
				{
					Object temp = iter.next();
					if(temp instanceof CollectionEventParameters)
					{
						collectionEventParameters = (CollectionEventParameters)temp;
					}
					else if(temp instanceof ReceivedEventParameters)
					{
						receivedEventParameters = (ReceivedEventParameters)temp;
					}
				}
			}
			//Setting the ids
			specimenCollectionGroupForm.setCollectionEventId(collectionEventParameters.getId().longValue());
			specimenCollectionGroupForm.setReceivedEventId(receivedEventParameters.getId().longValue());
		}
	}
	/**
	 * @param request
	 * @param specimenCollectionGroupForm
	 */
	private void setDefaultEvents(HttpServletRequest request,SpecimenCollectionGroupForm specimenCollectionGroupForm,String operation) throws DAOException
	{
		setDateParameters(specimenCollectionGroupForm);	
		if (specimenCollectionGroupForm.getCollectionEventCollectionProcedure() == null)
		{
			specimenCollectionGroupForm.setCollectionEventCollectionProcedure((String)DefaultValueManager.getDefaultValue(Constants.DEFAULT_COLLECTION_PROCEDURE));
		}			
		if (specimenCollectionGroupForm.getCollectionEventContainer() == null)
		{
			specimenCollectionGroupForm.setCollectionEventContainer((String)DefaultValueManager.getDefaultValue(Constants.DEFAULT_CONTAINER));
		}
		if (specimenCollectionGroupForm.getReceivedEventReceivedQuality() == null)
		{
			specimenCollectionGroupForm.setReceivedEventReceivedQuality((String)DefaultValueManager.getDefaultValue(Constants.DEFAULT_RECEIVED_QUALITY));
		}
		//setting the collector and receiver drop downs
		setUserInForm(request,operation,specimenCollectionGroupForm);
		//Setting the List for drop downs
		setEventsListInRequest(request);
	}
	/**
	 * @param request
	 */
	private void setEventsListInRequest(HttpServletRequest request)
	{
		//setting the procedure
		List procedureList = CDEManager.getCDEManager().getPermissibleValueList(Constants.CDE_NAME_COLLECTION_PROCEDURE, null);
		request.setAttribute(Constants.PROCEDURE_LIST, procedureList);
//		set the container lists
		List containerList = CDEManager.getCDEManager().getPermissibleValueList(Constants.CDE_NAME_CONTAINER, null);
		request.setAttribute(Constants.CONTAINER_LIST, containerList);	

		//setting the quality for received events
		List qualityList = CDEManager.getCDEManager().getPermissibleValueList(Constants.CDE_NAME_RECEIVED_QUALITY, null);
		request.setAttribute(Constants.RECEIVED_QUALITY_LIST, qualityList);

//		Sets the hourList attribute to be used in the Add/Edit FrozenEventParameters Page.
		request.setAttribute(Constants.HOUR_LIST, Constants.HOUR_ARRAY);
		//Sets the minutesList attribute to be used in the Add/Edit FrozenEventParameters Page.
		request.setAttribute(Constants.MINUTES_LIST, Constants.MINUTES_ARRAY);

	}
	/**
	 * @param request
	 * @param operation
	 * @param specimenCollectionGroupForm
	 * @throws DAOException
	 */
	private void setUserInForm(HttpServletRequest request,String operation,SpecimenCollectionGroupForm specimenCollectionGroupForm) throws DAOException
	{
		UserBizLogic userBizLogic = (UserBizLogic) BizLogicFactory.getInstance().getBizLogic(Constants.USER_FORM_ID);
		Collection userCollection = userBizLogic.getUsers(operation);

		request.setAttribute(Constants.USERLIST, userCollection);

		SessionDataBean sessionData = getSessionData(request);
		if (sessionData != null)
		{
			String user = sessionData.getLastName() + ", " + sessionData.getFirstName();
			long collectionEventUserId = EventsUtil.getIdFromCollection(userCollection, user);

			if(specimenCollectionGroupForm.getCollectionEventUserId() == 0)
			{
				specimenCollectionGroupForm.setCollectionEventUserId(collectionEventUserId);
			}
			if(specimenCollectionGroupForm.getReceivedEventUserId() == 0)
			{
				specimenCollectionGroupForm.setReceivedEventUserId(collectionEventUserId);
			}
		}
	}

	/**
	 * @param specimenForm
	 */
	private void setDateParameters(SpecimenCollectionGroupForm specimenForm)
	{
		// set the current Date and Time for the event.
		Calendar cal = Calendar.getInstance();
		//Collection Event fields
		if (specimenForm.getCollectionEventdateOfEvent() == null)
		{
			specimenForm.setCollectionEventdateOfEvent(Utility.parseDateToString(cal.getTime(), Constants.DATE_PATTERN_MM_DD_YYYY));
		}
		if (specimenForm.getCollectionEventTimeInHours() == null)
		{
			specimenForm.setCollectionEventTimeInHours(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
		}
		if (specimenForm.getCollectionEventTimeInMinutes() == null)
		{
			specimenForm.setCollectionEventTimeInMinutes(Integer.toString(cal.get(Calendar.MINUTE)));
		}

		//ReceivedEvent Fields
		if (specimenForm.getReceivedEventDateOfEvent() == null)
		{
			specimenForm.setReceivedEventDateOfEvent(Utility.parseDateToString(cal.getTime(), Constants.DATE_PATTERN_MM_DD_YYYY));
		}
		if (specimenForm.getReceivedEventTimeInHours() == null)
		{
			specimenForm.setReceivedEventTimeInHours(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
		}
		if (specimenForm.getReceivedEventTimeInMinutes() == null)
		{
			specimenForm.setReceivedEventTimeInMinutes(Integer.toString(cal.get(Calendar.MINUTE)));
		}

	}

	private void loadPaticipants(long protocolID, IBizLogic bizLogic, HttpServletRequest request) throws Exception
	{
		//get list of Participant's names
		String sourceObjectName = CollectionProtocolRegistration.class.getName();
		String [] displayParticipantFields = {"participant.id"};
		String valueField = "participant."+Constants.SYSTEM_IDENTIFIER;
		String whereColumnName[] = {"collectionProtocol."+Constants.SYSTEM_IDENTIFIER,"participant.id"};
		String whereColumnCondition[];
		Object[] whereColumnValue; 
		if(Variables.databaseName.equals(Constants.MYSQL_DATABASE))
		{
			whereColumnCondition = new String[]{"=","is not"};
			whereColumnValue=new Object[]{new Long(protocolID),null};
		}
		else
		{
			// for ORACLE
			whereColumnCondition = new String[]{"=",Constants.IS_NOT_NULL};
			whereColumnValue=new Object[]{new Long(protocolID),""};
		}


		String joinCondition = Constants.AND_JOIN_CONDITION;
		String separatorBetweenFields = ", ";

		List list = bizLogic.getList(sourceObjectName, displayParticipantFields, valueField, whereColumnName,
				whereColumnCondition, whereColumnValue, joinCondition, separatorBetweenFields, true);


		//get list of Participant's names
		valueField = Constants.SYSTEM_IDENTIFIER;
		sourceObjectName = Participant.class.getName();
		String[] participantsFields = {"lastName","firstName","birthDate","socialSecurityNumber"};
		String[] whereColumnName2 = {"lastName","firstName","birthDate","socialSecurityNumber"};
		String[] whereColumnCondition2 = {"!=","!=","is not","is not"};
		Object[] whereColumnValue2 = {"","",null,null};
		if(Variables.databaseName.equals(Constants.MYSQL_DATABASE))
		{
			whereColumnCondition2 = new String[]{"!=","!=","is not","is not"};
			whereColumnValue2=new String[]{"","",null,null};
		}
		else
		{
			// for ORACLE
			whereColumnCondition2 = new String[]{Constants.IS_NOT_NULL,Constants.IS_NOT_NULL,Constants.IS_NOT_NULL,Constants.IS_NOT_NULL};
			whereColumnValue2=new String[]{"","","",""};
		}

		String joinCondition2 = Constants.OR_JOIN_CONDITION;
		String separatorBetweenFields2 = ", ";

		List listOfParticipants = bizLogic.getList(sourceObjectName, participantsFields, valueField, whereColumnName2,
				whereColumnCondition2, whereColumnValue2, joinCondition2, separatorBetweenFields, false);

		// removing blank participants from the list of Participants
		list=removeBlankParticipant(list, listOfParticipants);
		//Mandar bug id:1628 :- sort participant dropdown list
		Collections.sort(list );  
		Logger.out.debug("Paticipants List"+list);
		request.setAttribute(Constants.PARTICIPANT_LIST, list);
	}

	private List removeBlankParticipant(List list, List listOfParticipants)
	{
		List listOfActiveParticipant=new ArrayList();

		for(int i=0; i<list.size(); i++)
		{
			NameValueBean nameValueBean =(NameValueBean)list.get(i);

			if(Long.parseLong(nameValueBean.getValue()) == -1)
			{
				listOfActiveParticipant.add(list.get(i));
				continue;
			}

			for(int j=0; j<listOfParticipants.size(); j++)
			{
				if(Long.parseLong(((NameValueBean)listOfParticipants.get(j)).getValue()) == -1)
					continue;

				NameValueBean participantsBean = (NameValueBean)listOfParticipants.get(j);
				if( nameValueBean.getValue().equals(participantsBean.getValue()) )
				{
					listOfActiveParticipant.add(listOfParticipants.get(j));
					break;
				}
			}
		}

		Logger.out.debug("No.Of Active Participants Registered with Protocol~~~~~~~~~~~~~~~~~~~~~~~>"+listOfActiveParticipant.size());

		return listOfActiveParticipant;
	}

	private void loadPaticipantNumberList(long protocolID, IBizLogic bizLogic, HttpServletRequest request) throws Exception
	{
		//get list of Participant's names
		String sourceObjectName = CollectionProtocolRegistration.class.getName();
		String displayParticipantNumberFields[] = {"protocolParticipantIdentifier"};
		String valueField = "protocolParticipantIdentifier";
		String whereColumnName[] = {"collectionProtocol."+Constants.SYSTEM_IDENTIFIER, "protocolParticipantIdentifier"};
		String whereColumnCondition[];// = {"=","!="};
		Object[] whereColumnValue;// = {new Long(protocolID),"null"};
		//		if(Variables.databaseName.equals(Constants.MYSQL_DATABASE))
		//		{
		whereColumnCondition = new String[]{"=","!="};
		whereColumnValue = new Object[]{new Long(protocolID),"null"};
		//		}
		//		else
		//		{
		//			whereColumnCondition = new String[]{"=","!=null"};
		//			whereColumnValue = new Object[]{new Long(protocolID),""};
		//		}

		String joinCondition = Constants.AND_JOIN_CONDITION;
		String separatorBetweenFields = "";

		List list = bizLogic.getList(sourceObjectName, displayParticipantNumberFields, valueField, whereColumnName,
				whereColumnCondition, whereColumnValue, joinCondition, separatorBetweenFields, true);



		Logger.out.debug("Paticipant Number List"+list);
		request.setAttribute(Constants.PROTOCOL_PARTICIPANT_NUMBER_LIST, list);
	}

	private void loadCollectionProtocolEvent(long protocolID, IBizLogic bizLogic, HttpServletRequest request) throws Exception
	{
		String sourceObjectName = CollectionProtocolEvent.class.getName();
		String displayEventFields[] = {"studyCalendarEventPoint","collectionPointLabel"};
		String valueField = "id";
		String whereColumnName[] = {"collectionProtocol."+Constants.SYSTEM_IDENTIFIER};
		String whereColumnCondition[] = {"="};
		Object[] whereColumnValue = {new Long(protocolID)};
		String joinCondition = Constants.AND_JOIN_CONDITION;
		String separatorBetweenFields = ",";

		List list = bizLogic.getList(sourceObjectName, displayEventFields, valueField, whereColumnName,
				whereColumnCondition, whereColumnValue, joinCondition, separatorBetweenFields, false);

		request.setAttribute(Constants.STUDY_CALENDAR_EVENT_POINT_LIST, list);
	}

	private void loadParticipantMedicalIdentifier(long participantID, IBizLogic bizLogic, HttpServletRequest request) throws Exception
	{
		//get list of Participant's names
		String sourceObjectName = ParticipantMedicalIdentifier.class.getName();
		String displayEventFields[] = {"medicalRecordNumber"};
		String valueField = Constants.SYSTEM_IDENTIFIER;
		String whereColumnName[] = {"participant."+Constants.SYSTEM_IDENTIFIER, "medicalRecordNumber"};
		String whereColumnCondition[] = {"=","!="};
		Object[] whereColumnValue = {new Long(participantID),"null"};
		String joinCondition = Constants.AND_JOIN_CONDITION;
		String separatorBetweenFields = "";

		List list = bizLogic.getList(sourceObjectName, displayEventFields, valueField, whereColumnName,
				whereColumnCondition, whereColumnValue, joinCondition, separatorBetweenFields, false);

		request.setAttribute(Constants.PARTICIPANT_MEDICAL_IDNETIFIER_LIST, list);
	}

	private String getParticipantIdForProtocolId(String participantProtocolId,IBizLogic bizLogic) throws Exception
	{
		String sourceObjectName = CollectionProtocolRegistration.class.getName();
		String selectColumnName[] = {"participant.id"};
		String whereColumnName[] = {"protocolParticipantIdentifier"};
		String whereColumnCondition[] = {"="};
		Object[] whereColumnValue = {participantProtocolId};
		List participantList = bizLogic.retrieve(sourceObjectName,selectColumnName,whereColumnName,whereColumnCondition,whereColumnValue,Constants.AND_JOIN_CONDITION);
		if(participantList != null && !participantList.isEmpty())
		{

			String participantId = ((Long) participantList.get(0)).toString();
			return participantId;

		}
		return null;
	}
	/**
	 * 
	 * @param participantId
	 * @param cpId
	 * @param bizLogic
	 * @return
	 * @throws Exception
	 */
	private String getParticipantProtocolIdForCPAndParticipantId(String participantId,String cpId,IBizLogic bizLogic) throws Exception
	{
		String sourceObjectName = CollectionProtocolRegistration.class.getName();
		String selectColumnName[] = {"protocolParticipantIdentifier"};
		String whereColumnName[] = {"participant.id","collectionProtocol.id"};
		String whereColumnCondition[] = {"=","="};
		Object[] whereColumnValue = {participantId,cpId};
		List list = bizLogic.retrieve(sourceObjectName,selectColumnName,whereColumnName,whereColumnCondition,whereColumnValue,Constants.AND_JOIN_CONDITION);
		if(list != null && !list.isEmpty())
		{
			Iterator iter = list.iterator();
			while(iter.hasNext())
			{
				Object id = (Object)iter.next();
				if(id != null)
				{
					return id.toString();
				}
			}
		}
		return null;
	}
}