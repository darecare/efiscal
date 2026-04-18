package org.elef.products.utils;

import org.compiere.model.MProduct;
import org.compiere.model.MSysConfig;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class ProductUtils {
	
	private final static String ELF_MPGETPRODUCTS_UU = "ELF_MPGETPRODUCTS_UU";

	public static int checkExistingProduct(String Ean, String SKU) {
		int m_product_id = DB.getSQLValue( null, "select m_product_id from m_product where upc = ? or sku = ?", Ean, SKU);
		if (m_product_id == -1)
			return 0;
		else
			return m_product_id;
	}
	
	public static int getUoMId() {
		int c_uom_id = DB.getSQLValue( null, "select c_uom_id from c_uom where uomsymbol = 'kom'");
		if(c_uom_id < 0) {
			c_uom_id = DB.getSQLValue( null, "select c_uom_id from c_uom where isdefault = 'Y' and ad_client_id = ?\"", Env.getAD_Client_ID(Env.getCtx()));
		}
		return c_uom_id;
	}
	
	public static int getTaxCatId() {
		int c_taxcategory_id = DB.getSQLValue( null, "select c_taxcategory_id from c_taxcategory where name = 'PDV'");
		if(c_taxcategory_id < 0) {
			c_taxcategory_id = DB.getSQLValue( null, "select c_taxcategory_id from c_taxcategory where isdefault = 'Y' and ad_client_id = ?", Env.getAD_Client_ID(Env.getCtx()));
		}
		return c_taxcategory_id;
	}
	
	public static int getProdCatId(String P_category_name) {
		int m_product_category_id = DB.getSQLValue( null, "select m_product_category_id from m_product_category where name like ?", P_category_name);
		if(m_product_category_id < 0) {
			m_product_category_id = DB.getSQLValue( null, "select m_product_category_id from m_product_category where isdefault = 'Y' and ad_client_id = ?", Env.getAD_Client_ID(Env.getCtx()));
		}
		return m_product_category_id;
	}
	
	public static void setProductDefaults(MProduct mproduct) {
		mproduct.setM_Product_Category_ID(getProdCatId(""));
		mproduct.setC_TaxCategory_ID(getTaxCatId());
		mproduct.setC_UOM_ID(getUoMId());
		mproduct.setProductType("I");
	}
	
	public static int getProductProcessID() {
		String sql = "SELECT AD_Process_ID FROM AD_Process WHERE IsActive = 'Y' AND AD_Process_UU = ? ";
		int adProcessID = DB.getSQLValueEx(null, sql, new Object[] { getProductsProcessUUID() });

		if (adProcessID <= 0)
			throw new AdempiereUserError("MPGetProducts Process not found.");

		return adProcessID;
	}

	public static String getProductsProcessUUID() {
		return MSysConfig.getValue(ELF_MPGETPRODUCTS_UU, "0031d8c4-bdaf-415d-ae73-eb9cf0f9c70e",
				Env.getAD_Client_ID(Env.getCtx()));
	}
}
