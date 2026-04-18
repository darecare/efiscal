package org.elef.efiscal;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.elef.model.MELF_FiscalBill;
import org.elef.model.MELF_FiscalTax;
import org.json.JSONArray;
import org.json.JSONObject;

public class eFiscalUtils {

	public static SSLContext getSSLContext(String l_efiscal_cert, String l_efiscal_pass) throws Exception {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(new FileInputStream(l_efiscal_cert), l_efiscal_pass.toCharArray());
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(keyStore, l_efiscal_pass.toCharArray());
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
		return sslContext;
	}
	
	public static boolean savetaxItems(JSONArray taxItems, MELF_FiscalBill mfb) {
		for (int i = 0; i < taxItems.length(); i++) {
			MELF_FiscalTax mft = new MELF_FiscalTax(Env.getCtx(),0,null);
	        JSONObject taxItem = (JSONObject) taxItems.get(i);
	        System.out.println(taxItem.get("label"));
	        System.out.println(taxItem.get("amount"));
	        
	        mft.setELF_FiscalBill_ID(mfb.getELF_FiscalBill_ID());
	        mft.seteFiscal_TaxLabel(taxItem.getString("label"));
	        mft.setRate(new BigDecimal(taxItem.get("rate").toString()));
	        mft.setAmount(new BigDecimal(taxItem.get("amount").toString()));
	        mft.seteFiscal_CategoryType(taxItem.getInt("categoryType"));		
	        mft.seteFiscal_CategoryName(taxItem.getString("categoryName"));	        
	        mft.saveEx();
		}
		return true;
	}
	
	public static MELF_FiscalBill saveFiscalBill(JSONObject obj, int p_invoiceType,int p_transactionType, PO po) {
		MELF_FiscalBill mfb = new MELF_FiscalBill(Env.getCtx(),0,null);
		JSONArray taxItems = obj.getJSONArray("taxItems");
			//if PO is type MOrder, set the order ID
			if(po.get_TableName().equals("C_Order")) {
				mfb.setC_Order_ID(po.get_ID());
				MOrder so  = new MOrder(Env.getCtx(), po.get_ID(), null);
				mfb.seteFiscal_CustomerName(so.getC_BPartner().getName());
				mfb.seteFiscal_OrderNo(so.getDocumentNo());
			}
			mfb.seteFiscal_link(obj.getString("verificationUrl"));
			mfb.seteFiscal_sdc_invoiceno(obj.getString("invoiceNumber"));
			mfb.seteFiscal_qr(obj.getString("verificationQRCode"));
			mfb.seteFiscal_sdcdatetime(obj.getString("sdcDateTime"));
			mfb.seteFiscal_requestedby( obj.getString("requestedBy"));
			mfb.seteFiscal_invoicecounter(obj.getString("invoiceCounter"));
			mfb.seteFiscal_invoicecounterext(obj.getString("invoiceCounterExtension"));
			
			mfb.seteFiscal_messages(obj.getString("messages"));
			mfb.seteFiscal_signedby(obj.getString("signedBy"));
			mfb.seteFiscal_encryptedinternaldata(obj.getString("encryptedInternalData"));
			mfb.seteFiscal_signature(obj.getString("signature"));
			//mfb.seteFiscal_totalcounter(obj.getInt("totalCounter"));
			mfb.seteFiscal_transactiontypecounter(obj.getInt("transactionTypeCounter"));
			mfb.seteFiscal_totalamount(obj.getBigDecimal("totalAmount"));
			mfb.seteFiscal_taxgrouprevision(obj.getInt("taxGroupRevision"));
			mfb.seteFiscal_businessname(obj.getString("businessName"));
			mfb.seteFiscal_tin(obj.getString("tin"));
			//mfb.seteFiscal_locationName(obj.getString("locationName"));
			mfb.seteFiscal_address(obj.getString("address"));
			mfb.seteFiscal_mrc(obj.getString("mrc"));
			mfb.seteFiscal_invoicetype(p_invoiceType);
			mfb.seteFiscal_transactiontype(p_transactionType);
	        mfb.saveEx();
			if(savetaxItems(taxItems, mfb))
				return mfb;
			else
				throw new AdempiereException("Error saving tax items for fiscal bill");
	}
}
