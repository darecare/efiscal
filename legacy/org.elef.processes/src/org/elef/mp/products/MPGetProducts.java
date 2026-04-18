package org.elef.mp.products;

import java.math.BigDecimal;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPO;
import org.compiere.model.MProductPrice;
import org.compiere.model.MSysConfig;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.elef.model.MELF_ApiConn;
import org.elef.model.MELF_ApiTemplate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MPGetProducts extends SvrProcess{
	
	private String 	P_sku;
	private String	P_id;
	private String 	P_ean;
	private String 	P_name;
	private String 	P_category_name;
	private String 	p_vendor_name;
	private String 	P_vendor_code;
	private String 	p_sales_rep;
	private String 	p_invoice_rep;
	private String 	P_supplier;
	private String 	P_ShopProductID;
	private String 	p_web;
	private String 	p_image_url;
	private String 	p_location;
	private int 	p_availability = 0;
	private int 	P_limit = 0;
	private int 	P_max = 0;
	private int 	P_start = 0;
	private boolean P_is_new = false;
	private String P_status = null;
	private BigDecimal P_price_gross;
	private BigDecimal P_old_price_gross;
	private MProduct mproduct;
	private MProductPO mproductPO;
	private HttpRequest request;
	private String messageCheck = "";
	
	private int ad_org_id = 0;
	
//	private final static String API_GETPRODUCT_URL = "API_MP_GETPRODUCT_URL";
//	private final static String API_GETPRODUCT_KEY = "API_MP_GET_KEY";
//	private final static String API_GETPRODUCT_SECRET = "API_MP_GET_SECRET";
	private final static String MP_SALES_PRICELIST = "MP_SALES_PRICELIST";
	
	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("API_Supplier"))
				P_supplier = para[i].getParameterAsString();
			else if (name.equals("API_Limit"))
				P_limit = para[i].getParameterAsInt();
			else if (name.equals("API_Max"))
				P_max = para[i].getParameterAsInt();
			else if (name.equals("API_Start"))
				P_start = para[i].getParameterAsInt();
			else if (name.equals("ShopProductID"))
				P_ShopProductID = para[i].getParameterAsString();
			else if (name.equals("IsNew"))
				P_is_new = "Y".equals(para[i].getParameterAsString());
			else if (name.equals("Status"))
				P_status = para[i].getParameterAsString();
//			else if (name.equals("AD_Client_ID")) {
//				ad_client_id = para[i].getParameterAsInt();
//			}
//			else if (name.equals("AD_Org_ID")) {
//				ad_org_id = para[i].getParameterAsInt();
//			}
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
			
		}
	}

	@Override
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub
		
//		String l_url = MSysConfig.getValue(API_GETPRODUCT_URL, getAD_Client_ID());
//		String l_apikey = MSysConfig.getValue(API_GETPRODUCT_KEY, Env.getAD_Client_ID(getCtx()),Env.getAD_Org_ID(getCtx()));
//		String l_apisecret = MSysConfig.getValue(API_GETPRODUCT_SECRET, getAD_Client_ID());
			
		int elf_apiconn_id = MELF_ApiConn.GetID(get_TrxName(), Env.getCtx(), "MP");
		MELF_ApiConn ElfApiConn = new MELF_ApiConn(getCtx(), elf_apiconn_id, get_TrxName());
		MELF_ApiTemplate ElfApiTemplate = MELF_ApiTemplate.getTemplate(get_TrxName(), getCtx(), "MP_Get_Products", ElfApiConn.get_ID());
		
		
		int m_pricelist_version_id = getPLVersionID();
		//BigDecimal d = new BigDecimal(P_max);
		//d = d.subtract(BigDecimal.valueOf(P_start)).divide(BigDecimal.valueOf(P_limit), 0 ,RoundingMode.CEILING);
		
		int counter = 0;
		int l_limit = P_limit;
		int l_start = P_start;
		boolean isContinue = true;
		
//		for (int i =1; i <= d.intValue(); i++) {
//			int l_start;
//			if(i>1)
//				l_start = P_start + P_limit * (i - 1);
//			else
//				l_start = P_start;
//			
//			int l_limit = 0;
//			if ( P_max - (P_limit*(i-1)) <  P_limit )
//				{
//					l_limit = P_max - (P_limit*(i-1));
//					}
//			else
//				{
//					l_limit = P_limit;
//					}
			do {
//				if(P_start > 0 && l_start == 0)
//                    l_start = l_start + P_start;
//				else
//					l_start = l_start + counter;
			StringBuilder apicall = prepareAPIcall(ElfApiTemplate.getElf_UrlExtend(), l_limit, l_start);
			
			request = ElfApiConn.getHttpRequest(apicall.toString(), null, ElfApiTemplate);
			
//			HttpClient client = HttpClient.newHttpClient();
//	        client.sendAsync(request, BodyHandlers.ofString())
//	        .thenApply(HttpResponse::body)
//	        .join();

	        HttpResponse<String> response = ElfApiConn.sendRequest(request);
	        
	        if (response.statusCode() == 200) {
	        	counter++;
		        JSONObject obj = new JSONObject(response.body());
		        JSONArray arr = obj.getJSONArray("data");
		        
		        arr.forEach(item -> {
		            JSONObject product = (JSONObject) item;
		            
		            setLValues(product);     
		            
		            int m_product_id = checkExistingProduct();
		            if (m_product_id > 0) {
		            	mproduct = new MProduct(getCtx(),m_product_id,get_TrxName());
		            }
		            else
		            	mproduct = new MProduct(getCtx(),0,null);
		                       
		            setProductData(mproduct);
		            

		            // ubacivanje sifre dobavljaca i dobavljaca na Purchase Tab
		            if(p_vendor_name == null || P_vendor_code == null) {
		            	//System.out.println( "No Vendor found in API data: " + mproduct.getSKU() );
		            }
		            else {
		            	int p_id = getProdPO_id(mproduct.getM_Product_ID(), p_vendor_name, P_vendor_code);
		            }
		            
		            
		            setPrice_PL(m_pricelist_version_id);
		            
		            messageCheck = " ID:" + mproduct.getValue();  
		            
		        });
		        
		        statusUpdate("Obradjuje se: " + counter);
		        try {
		            Thread.sleep(350); // 350ms to stay under 3 calls/sec
		        } catch (InterruptedException e) {
		            Thread.currentThread().interrupt();
		            log.severe("Sleep interrupted: " + e.getMessage());
		        }
	        }
	        else
	        	log.severe("API error: " + response.statusCode());
	        
	        counter = counter + l_limit;
	        l_start += l_limit;
	        if(P_max > 0 && counter >= P_max)
	        	isContinue = false;
			if (P_max == 0)
				isContinue = false;
		}
			while(isContinue);
		StringBuilder msgreturn = new StringBuilder("@Updated@ = ").append(messageCheck);
		return msgreturn.toString();
	}
	
	
	protected int getUoMId() {
		int c_uom_id = DB.getSQLValue(get_TrxName(), "select c_uom_id from c_uom where uomsymbol = 'kom'");
		if(c_uom_id < 0) {
			return 0; 
		}
		return c_uom_id;
	}
	
	protected int getTaxCatId() {
		int c_taxcategory_id = DB.getSQLValue(get_TrxName(), "select c_taxcategory_id from c_taxcategory where name = 'PDV'");
		if(c_taxcategory_id < 0) {
			c_taxcategory_id = DB.getSQLValue(get_TrxName(), "select c_taxcategory_id from c_taxcategory where isdefault = 'Y' and ad_client_id = ?", getAD_Client_ID());
		}
		return c_taxcategory_id;
	}
	
	protected int getProdCatId(String P_category_name) {
		int m_product_category_id = DB.getSQLValue(get_TrxName(), "select m_product_category_id from m_product_category where name like ?", P_category_name);
		if(m_product_category_id < 0) {
			m_product_category_id = DB.getSQLValue(get_TrxName(), "select m_product_category_id from m_product_category where isdefault = 'Y' and ad_client_id = ?", getAD_Client_ID());
		}
		return m_product_category_id;
	}
	
	protected int getProdPO_id(int p_product_id, String p_vendor_name, String p_vendor_Code ) {
		int m_product_id = DB.getSQLValue(get_TrxName(), "select m_product_id from\n"
				+ "m_product_po mppo\n "
				+ "inner join c_bpartner bp on bp.c_bpartner_id = mppo.c_bpartner_id\n "
				+ "where bp.name ilike ? and mppo.iscurrentvendor = 'Y' and mppo.m_product_id = ?", "%" + p_vendor_name + "%", p_product_id );
		
		if(m_product_id < 0) {
			
			int c_bpartner_id = DB.getSQLValue(get_TrxName(), "select c_bpartner_id from c_bpartner where name3 = ?", p_vendor_name);
			
			if(c_bpartner_id > 0) {
				mproductPO = new MProductPO(getCtx(), 0, get_TrxName());
				mproductPO.setC_BPartner_ID(c_bpartner_id);
				mproductPO.setM_Product_ID(p_product_id);
				if (p_vendor_Code != null && p_vendor_Code != "")
					mproductPO.setVendorProductNo(p_vendor_Code);
				else
					mproductPO.setVendorProductNo("N/A");
				
				try {
					mproductPO.saveEx();
				}
				catch (AdempiereException e) {
					log.severe(e.getMessage());
				}
			}
			else
				return -1;
			
			return 0; 
		}
		
		return m_product_id;
	}
	
	protected int getUserID(String p_sales_rep) {
		int ad_user_id = DB.getSQLValue(get_TrxName(), "select ad_user_id from "
				+ "ad_user "
				+ "where Comments ilike ?", "%" + p_sales_rep + "%");
		if(ad_user_id < 0) {
			return 0;
		}
		
		return ad_user_id;
	}
	
	protected int getPLVersionID() {
		String l_salespricelist_id = MSysConfig.getValue(MP_SALES_PRICELIST, getAD_Client_ID());
		int m_pricelist_version_id = DB.getSQLValue(get_TrxName(), "select m_pricelist_version_id from"
				+ " m_pricelist_version "
				+ " where m_pricelist_id = ? ",
				Integer.valueOf(l_salespricelist_id)
				);
		return m_pricelist_version_id;
	}
	
	protected void setPrice_PL(int m_pricelist_version_id) {
		MProductPrice mproductprice;
		int M_ProductPrice_ID = DB.getSQLValue(get_TrxName(), "select m_productprice_id from m_productprice"
				+ " where m_pricelist_version_id = ? "
				+ " and m_product_id = ?",  m_pricelist_version_id, mproduct.getM_Product_ID());
		if (M_ProductPrice_ID < 0) {
			mproductprice = new MProductPrice(getCtx(), m_pricelist_version_id, mproduct.getM_Product_ID(),  get_TrxName());
		}
		else {
			mproductprice = new MProductPrice(getCtx(), M_ProductPrice_ID, get_TrxName());
		}
		mproductprice.setPriceStd(P_price_gross);
		if(P_old_price_gross !=null)
			mproductprice.setPriceList(P_old_price_gross);
		else
			mproductprice.setPriceList(new BigDecimal(0));
		try {
			mproductprice.saveEx();
		}
		catch (AdempiereException e) {
			log.severe(e.getMessage());
		}
	}
	
	protected StringBuilder prepareAPIcall(String l_url, int l_limit, int l_start) {
		StringBuilder apicall = null;
		apicall = new StringBuilder(l_url);
		
		if(l_limit > 0)
			apicall = apicall.append("?limit=" + l_limit);
		if(l_start > 0)
			apicall = apicall.append("&start=" + l_start);
		if( P_is_new )
			apicall = apicall.append("&is_new=true");
		if( P_supplier!= null )
			apicall = apicall.append("&meta_fields[dobavljac]=" + P_supplier);
		if(P_ShopProductID!=null)
			apicall = apicall.append("?id=" + P_ShopProductID);
		if(P_status != null && !P_status.equals(""))
			apicall = apicall.append("&status=" + P_status);
		return apicall;
	}
	
	protected int checkExistingProduct() {
		int m_product_id = DB.getSQLValue(get_TrxName(), "select m_product_id from m_product where value = ?", P_id);
		
		return m_product_id;
	}
	
	protected void setLValues(JSONObject product) {
		P_id = product.get("id").toString();
		if(product.has("sku")&&!product.get("sku").toString().equals("null"))
			P_sku = product.get("sku").toString();
		if(product.has("ean")&&!product.get("ean").toString().equals("null"))
			P_ean =  product.get("ean").toString();
		//String ss = product.optString("ean");
        P_name = product.get("name").toString();
        P_price_gross = product.getBigDecimal("price_gross");
        P_old_price_gross = product.optBigDecimal("old_price_gross", null);
        P_category_name = product.get("category_name").toString();
        p_vendor_name = null;
        P_vendor_code = null;
        p_sales_rep = null;
    	if (product.has("image_url"))
    		p_image_url = product.getJSONObject("image_url").getString("large");
    	if (product.has("availability_id"))
    		p_availability = product.getInt("availability_id");
        try {
        	JSONObject meta_fields = null;
        	if(product.has("meta_fields"))
        		meta_fields = product.optJSONObject("meta_fields");
        	
        	if(meta_fields != null) {
	        	if (meta_fields.has("dobavljac")) {
	        		p_vendor_name = meta_fields.getString("dobavljac");
	        		}
	        	if (meta_fields.has("sifra_dobavljaca")) {
	        		P_vendor_code = meta_fields.getString("sifra_dobavljaca");
	        	}
	        	if (meta_fields.has("komercijalista")) {
	        		p_sales_rep = meta_fields.getString("komercijalista");
	        	}
	        	if (meta_fields.has("skladiste"))
	        		p_location = meta_fields.getString("skladiste");
	        	if (meta_fields.has("fakturista"))
	        		p_invoice_rep = meta_fields.getString("fakturista");
	        	if (meta_fields.has("web_kreiranje_naloga"))
	        		p_web = meta_fields.getString("web_kreiranje_naloga");
        	}
        }
        catch (JSONException je ){
        	System.out.println(je.getMessage());
        	// potrebno dodati kod u slucaju da ne pronadje podatke u meta_fields
        }
	}
	
	protected boolean setProductData(MProduct mproduct) {
		//mproduct.setClientOrg(ad_client_id,0);
		if(ad_org_id > 0)
			mproduct.setAD_Org_ID(ad_org_id);
		mproduct.setName(P_name);
        if(P_sku != null)
        	mproduct.setSKU(P_sku);
        if(P_ean != null)
            mproduct.setUPC(P_ean);
//        else 
//        	mproduct.setUPC("");
        mproduct.set_ValueOfColumn("value", P_id);
        mproduct.setC_UOM_ID(getUoMId());
        mproduct.setProductType("I");
        mproduct.set_ValueOfColumn( "c_taxcategory_id", getTaxCatId());
        if(p_location != null) {
        	mproduct.set_ValueOfColumn("m_product_loc", p_location);
            if (!p_location.equals("VP Lager roba")) {
            	mproduct.setIsDropShip(true);
            }
        }
        if(p_image_url != null)
        	mproduct.set_ValueOfColumn("ImageURL", p_image_url);

        if(p_web != null)
        	mproduct.set_ValueOfColumn("p_web", p_web);
        if(p_availability != 0)
        	mproduct.set_ValueOfColumn("Availability", p_availability);
        
        int l_category_id = getProdCatId(P_category_name);
        if (P_category_name != null && !P_category_name.equals("") && l_category_id > 0 )
        	mproduct.set_ValueOfColumn("m_product_category_id", l_category_id);
        else {
        	mproduct.set_ValueOfColumn("m_product_category_id", 1001014);
        	//fiksirana kategorija na N/A;
        }
        
        if(p_sales_rep != null)
        	mproduct.setSalesRep_ID(getUserID(p_sales_rep));
        if(p_invoice_rep != null)
        	mproduct.set_ValueOfColumn("InvoiceRep_ID", getUserID(p_invoice_rep)); 
        if(p_vendor_name != null)
        	mproduct.set_ValueOfColumn("p_vendor_name", p_vendor_name);

        try {
        	mproduct.saveEx();
        	//log.info("ID saved:" + mproduct.getValue());
        	//System.out.println("ID saved:" + mproduct.getValue());
        	return true;
        }
        catch (AdempiereException e){
        	log.severe("ProductID: " + mproduct.getValue() + " " + e.getMessage());
        	return false;
        }
	}

}
