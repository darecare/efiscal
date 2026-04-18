package org.elef.efiscal;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MMailText;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MTax;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.elef.model.MELF_ApiConn;
import org.elef.model.MELF_ApiTemplate;
import org.elef.model.MELF_FiscalBill;
import org.elef.model.MELF_FiscalBillConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onerp.statement.email.utils.FiscalCustomEmailUtils;

public class FiscalBillService {
    private Properties ctx;
    private String trxName;
    private int AD_PInstance_ID;
    private String errorMessage;

    public FiscalBillService(Properties ctx, String trxName, int AD_PInstance_ID) {
        this.ctx = ctx;
        this.trxName = trxName;
        this.AD_PInstance_ID = AD_PInstance_ID;
    }
    
    public MELF_FiscalBill processFiscalBill (MOrder order, int p_invoiceType, int p_transactionType, boolean p_IsSendMail) throws Exception{
        // Orchestration logic here
        // - check for existing bills
        // - handle advance scenarios
        // - build request, call API, save data
        // - send email if needed
    	if(checkIfFiscalBillExists(order, p_invoiceType, p_transactionType)) {
			throw new AdempiereException("Fiscal bill already exists for Order: " + order.getDocumentNo() + " with Invoice Type: " + p_invoiceType + " and Transaction Type: " + p_transactionType);
		}
    	int elf_apiconn_id = MELF_ApiConn.GetID(trxName, Env.getCtx(), "EF");
		MELF_ApiConn ElfApiConn = new MELF_ApiConn(ctx, elf_apiconn_id, trxName);
		MELF_ApiTemplate ElfApiTemplate = MELF_ApiTemplate.getTemplate(trxName, ctx, "Post_FiscalBill", ElfApiConn.get_ID());
		SSLContext sslContext = eFiscalUtils.getSSLContext(ElfApiConn.getElf_CERT(), ElfApiConn.getElf_ApiPassword());
		
		// Prepare request body dynamically based on invoiceType and transactionType
		String requestBody = buildFiscalBillRequestBody(order, ElfApiConn, ElfApiTemplate, p_invoiceType, p_transactionType);
		HttpRequest request = ElfApiConn.getHttpRequest(ElfApiTemplate.getElf_UrlExtend(), requestBody, ElfApiTemplate);
		try {
			HttpResponse<String> response = ElfApiConn.sendRequest(request, sslContext);
			MELF_FiscalBill mfb = null;
			if (response != null && response.statusCode() == 200) {
				String responseBody = response.body();
				// TODO: Parse response and store in MELF_FiscalBill and MELF_FiscalTax
				JSONObject responseJSON = new JSONObject(responseBody);
				mfb = eFiscalUtils.saveFiscalBill(responseJSON, p_invoiceType, p_transactionType, order);
				if( mfb != null) {
					if (p_IsSendMail) {
						MELF_FiscalBillConfig mfc = new MELF_FiscalBillConfig(ctx, 1000001, trxName);
						sendMail(mfb, order, mfc);
					}
				}
				}
				else {
					throw new AdempiereException("Error processing fiscal bill: " );
				}
			return mfb;
			}
		 catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }
    
    private String buildFiscalBillRequestBody(MOrder order, MELF_ApiConn ElfApiConn, MELF_ApiTemplate ElfApiTemplate, 
			int p_invoiceType, int p_transactionType) {
		Map<String, Object> apiFields = setHeaderFields(order,ElfApiConn, p_invoiceType, p_transactionType);
		setReferentFields(apiFields, order, p_invoiceType, p_transactionType);
		setPayment(apiFields, order);
		String buyerId = getBuyerId(order);
		if (buyerId != null && !buyerId.isEmpty()) {
			apiFields.put("buyerId", buyerId);
		}
		
		if(p_invoiceType == 0)
			setLineItems(apiFields, order);
		else if(p_invoiceType == 4) // Advance
			setAdvanceLineItems(apiFields, order);
		else
			throw new AdempiereException("Unsupported invoice type: " + p_invoiceType);
	

		JSONObject requestJSONBody = new org.json.JSONObject(apiFields);
		return requestJSONBody.toString();
	}
    
    
	
	private Map<String, Object> setHeaderFields(MOrder order, MELF_ApiConn ElfApiConn, int p_invoiceType, int p_transactionType) {
		Map<String, Object> result = new HashMap<>();
		result.put("invoiceType", p_invoiceType);
		result.put("transactionType", p_transactionType);
		result.put("dateAndTimeOfIssue", ZonedDateTime.now(ZoneId.of("Europe/Belgrade")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
 		result.put("invoiceNumber",ElfApiConn.geteFiscal_esirno());
		return result;	
	}
	
	private String getBuyerId(MOrder order) {
		MBPartner bp = (MBPartner) order.getC_BPartner();
		String buyerid = null;
		if (bp != null ) {
			if(bp.getTaxID() != null && !bp.getTaxID().isEmpty()) {
				buyerid = bp.get_ValueAsString("eFiscal_CustomerType") + ":" + bp.getTaxID();
			} 
		}
		return buyerid;
	}
	
	private void setReferentFields(Map<String, Object> result, MOrder order, int p_invoiceType, int p_transactionType) {
		MELF_FiscalBill Ref_fb = null;
		FiscalBillRepository fbr = new FiscalBillRepository(ctx, trxName);
		if(p_invoiceType == 0 && p_transactionType == 1) {
			Ref_fb = fbr.getFiscalBill(order.getC_Order_ID(), p_invoiceType, 0);
		}
		if(Ref_fb != null) {
			// Set referent fields based on the reference fiscal bill
			result.put("referentDocumentNumber", Ref_fb.geteFiscal_sdc_invoiceno());
			result.put("referentDocumentDT", Ref_fb.geteFiscal_sdcdatetime());
		} 
	
	}
	
	private void setLineItems(Map<String, Object> result, MOrder order) {
		MOrderLine[] orderLines = order.getLines();
		// here we need to put JSonArray of items and add each order line as an item
		JSONArray itemsArray = new JSONArray();
			for (MOrderLine orderLine : orderLines) {
				Map<String, Object> lineFields = new HashMap<>();
				lineFields.put("name", orderLine.getProduct().getName());
				lineFields.put("quantity", orderLine.getQtyOrdered());
				lineFields.put("unitPrice", orderLine.getPriceActual());
				lineFields.put("totalAmount", orderLine.getLineNetAmt());
				// add labels for tax  it is in the orderLine
				JSONArray labels = new JSONArray();
				labels.put(getTaxLabel(orderLine));
				lineFields.put("labels", labels);
				// Add more fields as necessary
				itemsArray.put(lineFields);			
		}
		if(itemsArray != null && itemsArray.length() > 0) {
			result.put("items", itemsArray);
		} else {
			throw new IllegalArgumentException("No order lines found for the order.");
		}
	}
	
	//this method creates fiscall bill lines for type advance fiscal bill
	private void setAdvanceLineItems(Map<String, Object> result, MOrder order) {
		JSONArray items = new JSONArray();
		
		StringBuilder sql = null;
		sql = new StringBuilder("select ct.c_tax_id, ct.efiscal_taxlabel,ct.rate,ct.efiscal_taxprefix, sum(col.lineNetAmt) as lineTotalAmt "
				+ "from c_orderline col "
				+ "inner join c_tax ct on ct.c_tax_id = col.c_tax_id "
				+ "where C_Order_ID=? "
				+ "group by ct.c_tax_id, ct.efiscal_taxlabel, ct.rate, ct.efiscal_taxprefix");
		
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), trxName);
			int index = 1;
			pstmt.setInt(index, order.getC_Order_ID());
		}
		catch (Exception e)
		{
			throw new AdempiereException(e);
		}
		
		ResultSet rs = null;
		
		try
		{
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{	
				JSONObject item = new JSONObject();
				BigDecimal l_unitprice;
				BigDecimal l_totalamount;
				
//				if(PayAmt.compareTo(SOTotalAmt) == -1) {
//					l_unitprice = rs.getBigDecimal("lineTotalAmt").divide(SOTotalAmt , 2 , RoundingMode.HALF_EVEN).multiply(PayAmt);
//					l_totalamount = rs.getBigDecimal("lineTotalAmt").divide(SOTotalAmt , 2 , RoundingMode.HALF_EVEN).multiply(PayAmt);
//				}
//				else
//				{
				l_unitprice = rs.getBigDecimal("lineTotalAmt");
				l_totalamount = rs.getBigDecimal("lineTotalAmt");
//				}
				
				item.put("name", rs.getString("efiscal_taxprefix") + ":" + " Avans");

				
				item.put("quantity", 1);
				JSONArray labels = new JSONArray();
				labels.put(rs.getString("efiscal_taxlabel")); 
				item.put("labels",labels);

				item.put("unitPrice", l_unitprice);
				item.put("totalAmount", l_totalamount);
				items.put(item);	
			}
				if(items != null && items.length() > 0) {
					result.put("items", items);
				} else {
					throw new IllegalArgumentException("No order lines found for the order.");
				}	
			}
		catch (Exception e)
			{
				throw new AdempiereException(e);
			}
		finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			} 	
	}
	
	private void setPayment(Map<String, Object> result, MOrder order) {
		JSONArray paymentArray = new JSONArray();
		Map<String, Object> paymentFields = new HashMap<>();
		int paymentType;
		
		switch(order.getPaymentRule()) {
		case "B":
			paymentType = 1;
			break;
		case "K":
			paymentType = 2;
			break;
		case "S":
			paymentType = 4;
			break;
		default:
			paymentType = 1;
	}
		paymentFields.put("amount", order.getGrandTotal());
		paymentFields.put("paymentType", paymentType); // or other payment type based on your logic
		paymentArray.put(paymentFields);
		result.put("payment", paymentArray);
	}
	
	private String getTaxLabel(MOrderLine orderLine) {
		MTax tax = (MTax) orderLine.getC_Tax();
		if (tax != null) {
			return tax.get_ValueAsString("eFiscal_TaxLabel");
		} else {
			//throw new IllegalArgumentException("Tax not found for order line: " + orderLine.getLine());
			throw new IllegalArgumentException("Tax not found for order line: " + orderLine.getLine());
		}
	}
	
	private void sendMail(MELF_FiscalBill mfb, MOrder so, MELF_FiscalBillConfig mfc) {
		String l_email = null;
		List<String> msgToList = new ArrayList<String>();
		List<String> msgBccList = new ArrayList<String>();
		try {
		if(mfc.getEMail_Test() != null && mfc.isTest())
        {
            msgToList.add(mfc.getEMail_Test());
        }
        else
        {
            msgToList.add(so.getBill_User().getEMail());
        }

		if(mfc.getEMail_Bcc() != null)
        {
			String[] emailsBcc = mfc.getEMail_Bcc().split(",");
			msgBccList = Arrays.asList(emailsBcc);
        }
		File report = null;
		if (mfc.getAD_PrintFormat_ID() > 0)
		{
			report = FiscalCustomEmailUtils.generateReports(trxName, mfc.getAD_PrintFormat_ID(),
					AD_PInstance_ID, so.get_ID());

			if (report == null)
				throw new AdempiereException("Not able to generate report.");
		}
		MMailText mailText = new MMailText(Env.getCtx(), mfc.getR_MailText_ID(), null);
		mailText.setLanguage(Env.getContext(Env.getCtx(), "#AD_Language"));

		String message = mailText.getMailText(true);
		String subject = mailText.getMailHeader();

		subject = Env.parseVariable(subject, so, so.get_TrxName(), true);
		message = Env.parseVariable(message, so, so.get_TrxName(), true);
		
		List<File> files = new ArrayList<File>();
		if (report != null)
			files.add(report);
		String sentStatus = FiscalCustomEmailUtils.sendEmail(Env.getCtx(), files, msgToList, msgBccList, mfc.getEMail_From(),
				mailText.isHtml(), subject, message);
		//addLog("***Email - " + so.getBill_User().getEMail() + " Mail Sent Status: " + sentStatus);
		
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new AdempiereException("Error generating report: " + e.getMessage());
		}
	}
	
	private boolean checkIfFiscalBillExists(MOrder order, int p_invoiceType, int p_transactionType) {
		FiscalBillRepository fbr = new FiscalBillRepository(ctx, trxName);
		MELF_FiscalBill existingBill = fbr.getFiscalBill(order.getC_Order_ID(), p_invoiceType, p_transactionType);
		return existingBill != null;
	}
}
