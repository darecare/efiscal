package org.elef.bs.orders;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MOrder;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.elef.model.MELF_ApiConn;
import org.elef.model.MELF_ApiField;
import org.elef.model.MELF_ApiTemplate;
import org.elef.orders.utils.OrderUtils;
import org.elef.products.utils.BSUtils;
import org.json.JSONObject;

public class BSSendOrders extends SvrProcess {
	private String SesionHandle = null;
	private String p_year;
	private String p_companyID;
	private String p_itemType;
	private String p_Template;

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("Year"))
            {
                p_year = para[i].getParameterAsString();
            }
            else if (name.equals("CompanyID"))
            {
                p_companyID = para[i].getParameterAsString();
            }
			else if (name.equals("itemType"))
            {
				p_itemType = para[i].getParameterAsString();
            }
			else if (name.equals("Template"))
            {
				p_Template = para[i].getParameterAsString();
            }
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub
		int elf_apiconn_id = MELF_ApiConn.GetID(get_TrxName(), Env.getCtx(), "BS");
		MELF_ApiConn ElfApiConn = new MELF_ApiConn(getCtx(), elf_apiconn_id, get_TrxName());
		
		//get list of unsent orders
		List<MOrder> orders = OrderUtils.getListOfUnSentOrders(get_TrxName(), 0);
		if (orders == null || orders.isEmpty())
			return "No Orders for send...";
		
		String log = sendOrders(ElfApiConn, orders);
		
		return log;
	}
	
	//sendMeta method
	private String sendOrders(MELF_ApiConn ElfApiConn, List<MOrder> orders) {
		// TODO Auto-generated method stub
		int sent = 0;
		String log = "";
		for (MOrder order : orders) {
			if(p_itemType == "") {
				String elf_bPartner_externalID = OrderUtils.getBPartnerExternalID(get_TrxName(), order);
				if (elf_bPartner_externalID == null)
					try {
						elf_bPartner_externalID = sendNewCustomer(ElfApiConn, order);
						MBPartner mbpartner = (MBPartner) order.getC_BPartner();
						mbpartner.set_ValueOfColumn("elf_externalid", elf_bPartner_externalID);
						mbpartner.saveEx();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			else {
				// send order
				try {
					sendOrder(ElfApiConn, order);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log = log + "Error sending order: " + order.getDocumentNo() + " " + e.getMessage();
				}
			}
			sent++;
			//order.set_ValueOfColumn("elf_issent", "Y");
			//order.saveEx();
		}
		
		return log;
	}

	private String sendNewCustomer(MELF_ApiConn ElfApiConn, MOrder order) throws Exception{
		// TODO Auto-generated method stub
		String l_ExternalID = null;
		int elf_apitemplate_id = MELF_ApiTemplate.GetID(get_TrxName(), Env.getCtx(), "BS_Send_Customer", ElfApiConn.get_ID());
		MELF_ApiTemplate ElfApiTemplate = new MELF_ApiTemplate(getCtx(), elf_apitemplate_id, get_TrxName());
		
		JSONObject CustomerRequestBody = prepareCustomerRequestBody(order, ElfApiTemplate, elf_apitemplate_id);
		System.out.println(CustomerRequestBody.toString());
		//send request
		if(SesionHandle == null)
			SesionHandle = BSUtils.getSessionHandle(ElfApiConn, ElfApiTemplate ,p_companyID, p_year);
		
		String xml = BSUtils.prepareXMLRequest(SesionHandle, "itCustomer", CustomerRequestBody.toString(),"AddItem");
		HttpResponse<String> response = null	;
		try {
			response = BSUtils.sendRequest(xml);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (response != null) {
			System.out.println(response.body());
			l_ExternalID = BSUtils.parseItemsResponse(response,"AddItem");
		}
		return l_ExternalID;
		
	}
	
	private JSONObject prepareCustomerRequestBody(MOrder order, MELF_ApiTemplate ElfApiTemplate, int elf_apitemplate_id) {
		// TODO Auto-generated method stub
        JSONObject requestJSONBody = new JSONObject();
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(order.getC_BPartner_ID());
		params.add(Env.getAD_Client_ID(Env.getCtx()));
        List<Object> list = DB.getSQLValueObjectsEx(get_TrxName(),
        		"select bp.name, cl.address1, cl.city,cl.postal, au.email, au.phone from c_bpartner bp\n"
        		+ "inner join c_bpartner_location bpl on bpl.c_bpartner_id = bp.c_bpartner_id\n"
        		+ "inner join c_location cl on cl.c_location_id = bpl.c_location_id\n"
        		+ "inner join ad_user au on au.c_bpartner_id = bp.c_bpartner_id\n"
        		+ "where bp.c_bpartner_id = ? and bp.ad_client_id = ?", order.getC_BPartner_ID(), Env.getAD_Client_ID(Env.getCtx()));
        
        List<MELF_ApiField> apiFields = ElfApiTemplate.getApiFields("N", "N", "Y");
                for (MELF_ApiField apiField : apiFields) {
                	
                	if(apiField.isElf_setDefault())
                		requestJSONBody.put(apiField.getElf_ExtName(), apiField.getElf_DefaultValue() != null ?  apiField.getElf_DefaultValue():JSONObject.NULL );
                	else
                		requestJSONBody.put(apiField.getElf_ExtName(), list.get(apiField.getElf_listPosition()).toString());
                }
//        
//        requestJSONBody.put(MELFApiField.getExtName(get_TrxName(), getCtx(), "Name",elf_apitemplate_id), list.get(0).toString());
//        requestJSONBody.put(MELFApiField.getExtName(get_TrxName(), getCtx(), "ContactPerson",elf_apitemplate_id), list.get(0).toString());
//        requestJSONBody.put(MELFApiField.getExtName(get_TrxName(), getCtx(), "Address",elf_apitemplate_id), list.get(1));
//        requestJSONBody.put(MELFApiField.getExtName(get_TrxName(), getCtx(), "City",elf_apitemplate_id), list.get(2));
//        requestJSONBody.put(MELFApiField.getExtName(get_TrxName(), getCtx(), "Postal",elf_apitemplate_id), list.get(3));
//        requestJSONBody.put(MELFApiField.getExtName(get_TrxName(), getCtx(), "Email",elf_apitemplate_id), list.get(4));
//        requestJSONBody.put(MELFApiField.getExtName(get_TrxName(), getCtx(), "Phone",elf_apitemplate_id), list.get(5));
             
        return requestJSONBody;
	}
	
	private String sendOrder(MELF_ApiConn ElfApiConn, MOrder order) throws Exception {
		String l_jsonResponse = null;
		int elf_apitemplate_id = MELF_ApiTemplate.GetID(get_TrxName(), Env.getCtx(), p_Template, ElfApiConn.get_ID());
		MELF_ApiTemplate ElfApiTemplate = new MELF_ApiTemplate(getCtx(), elf_apitemplate_id, get_TrxName());
		JSONObject CustomerRequestBody = new JSONObject();;
				prepareOrderHeaderRequestBody(CustomerRequestBody, order, ElfApiTemplate);
				prepareOrderLinesRequestBody(CustomerRequestBody, order, ElfApiTemplate);
				if(SesionHandle == null)
					SesionHandle = BSUtils.getSessionHandle(ElfApiConn, ElfApiTemplate ,p_companyID, p_year);
				
				String xml = BSUtils.prepareXMLRequest(SesionHandle, p_itemType, CustomerRequestBody.toString(),"AddItem");
				HttpResponse<String> response = null	;
				try {
					response = BSUtils.sendRequest(xml);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (response != null) {
					System.out.println(response.body());
					l_jsonResponse = BSUtils.parseItemsResponse(response,"AddItem");
					JSONObject jsonObject = new JSONObject(l_jsonResponse);
					ElfApiTemplate.setJsonResponse(jsonObject, order);
				}
		return null;
	}

	private JSONObject prepareOrderRequestBody(MOrder order, MELF_ApiTemplate elfApiTemplate) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void prepareOrderHeaderRequestBody(JSONObject RequestBody, MOrder order, MELF_ApiTemplate elfApiTemplate) {
		StringBuilder sql = getHeaderSQL();
		List<Object> list = DB.getSQLValueObjectsEx(get_TrxName(), sql.toString(), order.get_ID(), order.getC_BPartner_ID(), Env.getAD_Client_ID(Env.getCtx()));
		elfApiTemplate.getJsonRequestBody(RequestBody, list, "master");
		//I want to check content of RequestBody here
		System.out.println(RequestBody.toString());
	}
	
	private void prepareOrderLinesRequestBody(JSONObject RequestBody, MOrder order, MELF_ApiTemplate elfApiTemplate) {
		StringBuilder sql = getLinesSQL();
		List<List<Object>> list = DB.getSQLArrayObjectsEx(get_TrxName(), sql.toString(), order.get_ID(),
				Env.getAD_Client_ID(Env.getCtx()));
		elfApiTemplate.getJsonRequestBody(RequestBody, list, "detail");
		// I want to check content of RequestBody here
		System.out.println(RequestBody.toString());
	}
		
	//function to build sql based on the paramente itemtype
	private StringBuilder getHeaderSQL() {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ");
		if(p_itemType.equals("stWeb_POS_Orders"))
			sql.append("co.documentno, bp.name, au.email, au.phone, to_char(co.created, 'YYYY-MM-DD\"T\"HH24:MI:SS') as time, date(now()) as date ");
		else if (p_itemType.equals("stWebOrdersWithPrice"))
			sql.append(
					"co.documentno, bp.elf_externalid, date(co.dateordered) as date ");
		sql.append("FROM c_order co ");
		sql.append("inner join c_bpartner bp on co.c_bpartner_id = bp.c_bpartner_id ");
		sql.append("inner join c_bpartner_location bpl on bpl.c_bpartner_id = bp.c_bpartner_id ");
		sql.append("inner join c_location cl on cl.c_location_id = bpl.c_location_id ");
		sql.append("inner join ad_user au on au.c_bpartner_id = bp.c_bpartner_id ");
		sql.append("where co.c_order_id = ? and bp.c_bpartner_id = ? and bp.ad_client_id = ?");
		return sql;
	}
	
	private StringBuilder getLinesSQL() {
		StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        	sql.append("col.line, mp.elf_externalid, col.qtyordered, col.priceentered ");
        sql.append("FROM c_order co ");
        sql.append("inner join c_orderline col on col.c_order_id = co.c_order_id ");
        sql.append("inner join m_product mp on mp.m_product_id = col.m_product_id ");
        sql.append("where co.c_order_id = ? and co.ad_client_id = ?");
        return sql;
    }

}
