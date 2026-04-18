package org.elef.bs.products;

import java.net.http.HttpResponse;
import java.util.logging.Level;

import org.compiere.model.MProduct;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.elef.model.MELF_ApiConn;
import org.elef.model.MELF_ApiTemplate;
import org.elef.products.utils.BSUtils;
import org.elef.products.utils.ProductUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class BSGetProducts extends SvrProcess{
	
	private String SesionHandle = null;
	private String itemType = "itArticle";
	private String JSONRequest;
	private String p_year;
	private String p_companyID;
	

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			
			
			if (para[i].getParameter() == null)
				;
//			else if (name.equals("Date1"))
//			{
//				dateFrom = para[i].getParameterAsTimestamp();
//				dateTo = para[i].getParameter_ToAsTimestamp();
//			}
			else if (name.equals("Year"))
            {
                p_year = para[i].getParameterAsString();
            }
            else if (name.equals("CompanyID"))
            {
                p_companyID = para[i].getParameterAsString();
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

		//System.out.println(SesionHandle);
		
		getBSProducts(ElfApiConn);
		
		
		return SesionHandle;
	}
	
	private int getBSProducts(MELF_ApiConn ElfApiConn) throws Exception{
		MELF_ApiTemplate ElfApiTemplate = MELF_ApiTemplate.getTemplate(get_TrxName(), getCtx(), "BSGetProducts", ElfApiConn.get_ID());
		
		if(SesionHandle == null)
			SesionHandle = BSUtils.getSessionHandle(ElfApiConn, ElfApiTemplate ,p_companyID, p_year);
		
		String items = null;
		JSONRequest = "{\"xsicMobSortOnly\":\"false\"}";
		
		String xml = BSUtils.prepareXMLRequest(SesionHandle, itemType, JSONRequest, "GetItems");
		HttpResponse<String> response = null	;
		try {
			response = BSUtils.sendRequest(xml);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (response != null) {
			System.out.println(response.body());
			items = BSUtils.parseItemsResponse(response,"GetItems");
		}
		System.out.println(items);
		//JSONObject obj = new JSONObject(items);
				JSONArray arr = new JSONArray(items);
		arr.forEach(item -> {
		            JSONObject product = (JSONObject) item;
		            int m_product_id = ProductUtils.checkExistingProduct(product.getString("Barcode"), String.valueOf(product.getInt("ID")));
		            	MProduct mproduct = new MProduct(getCtx(),m_product_id,get_TrxName());
		            	BSUtils.setProductFields(mproduct, ElfApiTemplate.getApiFields("N", "Y", "N"), product);
					if (m_product_id == 0) {
						ProductUtils.setProductDefaults(mproduct);
						mproduct.save();
					}
					else
						mproduct.saveEx();
		        });
		//}
		//catch (Exception e) {
		//	e.printStackTrace();
		//}//
		
		return 0;
	}


}
