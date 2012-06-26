
package edu.wustl.catissuecore.action;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import edu.wustl.catissuecore.actionForm.AdvanceSearchForm;
import edu.wustl.catissuecore.querysuite.QueryShoppingCart;
import edu.wustl.catissuecore.util.global.AppUtility;
import edu.wustl.catissuecore.util.global.Constants;
import edu.wustl.common.beans.NameValueBean;
import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.query.util.global.AQConstants;

/**
 * @author santhoshkumar_c
 */
public class ViewSpecimenList extends QueryShoppingCartAction
{

	/**
	 * Overrides the executeSecureAction method of SecureAction class.
	 * @param mapping
	 *            object of ActionMapping
	 * @param form
	 *            object of ActionForm
	 * @param request
	 *            object of HttpServletRequest
	 * @param response
	 *            object of HttpServletResponse
	 * @throws Exception
	 *             generic exception
	 * @return ActionForward : ActionForward
	 */

	@Override
	protected ActionForward executeAction(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		// AdvanceSearchForm searchForm = (AdvanceSearchForm) form;
		final HttpSession session = request.getSession();
		String target = null;

		final QueryShoppingCart cart = (QueryShoppingCart) session
				.getAttribute(Constants.QUERY_SHOPPING_CART);
		AdvanceSearchForm searchForm = (AdvanceSearchForm)form;
		AppUtility.setDefaultPrinterTypeLocation(searchForm);
		request.setAttribute(Constants.EVENT_PARAMETERS_LIST, Constants.EVENT_PARAMETERS);
		this.setCartView(request, cart);
		target = new String(Constants.VIEW);
		session.removeAttribute(AQConstants.HYPERLINK_COLUMN_MAP);
		final String eventArray[] = Constants.EVENT_PARAMETERS;
		final String newEvenetArray[] = new String[2];
		int count = 0;
		for (final String eventName : eventArray)
		{
			if (("Transfer").equals(eventName) || ("Disposal").equals(eventName))
			{
				newEvenetArray[count] = eventName;
				count++;
			}
		}
		getLabels(request);
		request.setAttribute("eventArray", newEvenetArray);
		request.setAttribute("advanceSearchForm", searchForm);
		return mapping.findForward(target);
	}

	private void getLabels(HttpServletRequest request) throws ApplicationException 
	{
		String sql = "select * from tag where user_id = ?";
		
		SessionDataBean sessionData = (SessionDataBean)request.getSession().getAttribute(Constants.SESSION_DATA);
		
		ColumnValueBean bean = new ColumnValueBean(sessionData.getUserId());
		List<ColumnValueBean> valueBeanList = new ArrayList<ColumnValueBean>();
		valueBeanList.add(bean);
		List list = AppUtility.executeSQLQuery(sql, valueBeanList);
		List<NameValueBean> labelList = new ArrayList<NameValueBean>();
		if(list !=null && list.size() > 0)
		{
			for (Object object : list) 
			{
				List result = (List)object;
				NameValueBean valueBean  = new NameValueBean(result.get(1), result.get(0));
				labelList.add(valueBean);
			}
			
			
		}
		request.setAttribute("dropDownList", labelList);
	}
}
