package org.elef.mp.orders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MLocation;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.MPriceList;
import org.compiere.model.MProcess;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoLog;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.ServerProcessCtl;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.elef.model.MELF_ApiConn;
import org.elef.model.MELF_ApiTemplate;
import org.elef.products.utils.ProductUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class MPGetOrders extends SvrProcess{
	
	private String 	P_orderid;
	private MOrder  morder;
	private MProduct mshipmentproduct;
	private int 	so_created = 0;
	private int 	P_limit = 0;
	private int 	p_shipmentproduct_id = 0;
	private boolean p_includediscount = false;
	private String  p_shipping_status = null;
	private String  p_shipping_substatus = null;
	
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("OrderID"))
				P_orderid = para[i].getParameterAsString();
			else if (name.equals("API_Limit"))
				P_limit = para[i].getParameterAsInt();
			else if (name.equals("ShipmentProduct_ID"))
				p_shipmentproduct_id = para[i].getParameterAsInt();
			else if (name.equals("IncludeDiscount"))
				p_includediscount = para[i].getParameterAsBoolean();
			else if (name.equals("ShippingStatus"))
				p_shipping_status = para[i].getParameterAsString();
			else if (name.equals("ShippingSubstatus"))
				p_shipping_substatus = para[i].getParameterAsString();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
	}

	@Override
	protected String doIt() throws Exception {
		
		int elf_apiconn_id = MELF_ApiConn.GetID(get_TrxName(), Env.getCtx(), "MP");
		MELF_ApiConn ElfApiConn = new MELF_ApiConn(getCtx(), elf_apiconn_id, get_TrxName());
		MELF_ApiTemplate ElfApiTemplate = MELF_ApiTemplate.getTemplate(get_TrxName(), getCtx(), "MP_Get_Orders", ElfApiConn.get_ID());
		
		int l_start = 0;
		int l_limit = P_limit;
        
		StringBuilder apicall = prepareAPIcall(ElfApiTemplate.getElf_UrlExtend(), l_limit, l_start);
		
		HttpRequest request = ElfApiConn.getHttpRequest(apicall.toString(), null, ElfApiTemplate);

        HttpResponse<String> response = ElfApiConn.sendRequest(request);
        
        JSONObject obj = new JSONObject(response.body());
        JSONArray arr = obj.getJSONArray("data");
        arr.forEach(item -> {
            JSONObject order= (JSONObject) item;

            int l_order_id = getOrderId(order.get("id").toString());
			if (l_order_id > 0) {
				addLog(0, null, null, "Porudzbina vec postoji: " + order.get("id").toString(), 0, 0);
				
				return;
			}
            
            addOrder(order);
           
        });
		
		StringBuilder msgreturn = new StringBuilder("@Kreirana@ = ").append(so_created).append("\n");
		ProcessInfo pi = getProcessInfo();
		List<ProcessInfoLog> logs = pi.getLogList();
		for (ProcessInfoLog log : logs) {
			msgreturn.append(log.getP_Msg()).append("\n");
		}
		return msgreturn.toString();
	}
	
    public static byte[] generateSHA512Hash(String input) throws NoSuchAlgorithmException {
        // Create MessageDigest instance for SHA-512
        MessageDigest md = MessageDigest.getInstance("SHA-512");

        // Convert the input string to bytes and hash it
        return md.digest(input.getBytes());
    }
    public static int UnsignedToBytes(byte b) 
    {
        return b & 0xFF;
    }
	// procedura koja pronalazi ID proizvoda i vraca nazad, da bih mogao da upisem stavke, mozda iskoristiti istu proceduru 
	// i za ostale elemente koji mi trebaju
	protected int GetID(String p_value) {
		
		StringBuilder sql = null;
		sql = new StringBuilder("SELECT * FROM M_Product")
				.append(" WHERE m_product.value = ? ");
		
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			int index = 1;
			pstmt.setString(index, p_value);
		}
		catch (Exception e)
		{
			throw new AdempiereException(e);
		}
		
		ResultSet rs = null;
		
		try
		{
			rs = pstmt.executeQuery ();
			if (rs.next ())
			{	
				return rs.getInt("m_product_id");
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
		
		return 0;
	}
	
	// get BPartner_ID if not found create new BP
	protected int getBPId(String p_email, String p_phone, JSONObject p_order) {

		int c_bpartner_id = DB.getSQLValue(get_TrxName(), "select c_bpartner_id from ad_user " + "where email like ? and ad_client_id = ?", p_email, Env.getAD_Client_ID(getCtx()));
		if (c_bpartner_id < 0) {
			//log.severe("BP with email : " + email + " does not exist on iDempiere");
			c_bpartner_id = DB.getSQLValue(get_TrxName(), "select c_bpartner_id from ad_user " + "where phone like ? and ad_client_id = ?", p_phone, Env.getAD_Client_ID(getCtx()));
			//if (c_bpartner_id < 0)
			//c_bpartner_id = createBP(orderSf);
		}
		
		if (c_bpartner_id < 0) {
			return createBP(p_email, p_phone, p_order);
		}
		else {
			return c_bpartner_id;
		}
	}
	
	// vraca tax_id, samo po vrednosti, dodati kasnije i uslov za kompaniju npr.
	protected int getTaxId(int p_taxvalue) {
		int c_tax_id = DB.getSQLValue(get_TrxName(), "select c_tax_id from c_tax where rate = ? and ad_client_id = ?", p_taxvalue, Env.getAD_Client_ID(getCtx()));
		if(c_tax_id < 0) {
			return 0; 
		}
		return c_tax_id;
	}
	
	protected int getOrderId(String p_documentno) {
		int l_order_id = DB.getSQLValue(get_TrxName(), "select c_order_id from c_order where documentno = ?", p_documentno);
		return l_order_id;
	}
	
	protected int getPriceListId() {
			int l_price_list_id = DB.getSQLValue(get_TrxName(),
					"select m_pricelist_id from m_pricelist " + "where isdefault = 'Y' and issopricelist = 'Y' and ad_client_id = ?",
					Env.getAD_Client_ID(getCtx()));
		return l_price_list_id;
	}
	
	protected int createBP(String p_email, String p_phone,JSONObject p_order) {
		String l_name = p_order.get("shipping_name").toString();	
		int l_BPGroup_id = DB.getSQLValue(get_TrxName(), "select c_bp_group_id from c_bp_group " + "where value = 'FL'");
		if (l_BPGroup_id <= 0) {
            l_BPGroup_id = DB.getSQLValue(get_TrxName(), "select c_bp_group_id from c_bp_group " + "where isdefault = 'Y' and ad_client_id = ?", Env.getAD_Client_ID(getCtx()));
        }
		
		MBPartner l_BP = new MBPartner(Env.getCtx(), 0, null);
		//businessPartner.setValue((String) defaultAddress.get("name"));
		l_BP.setName(l_name);
		l_BP.setC_BP_Group_ID(l_BPGroup_id);
		l_BP.setIsCustomer(true);
		l_BP.setIsProspect(false);
		l_BP.setIsVendor(false);
		l_BP.saveEx();
		
		int l_c_location_id = createLocation(p_order);
		int l_C_BPartner_Location_ID = createBPLocation( l_BP.get_ID(), l_c_location_id );
		createUser(l_BP, p_email, p_phone, l_C_BPartner_Location_ID);
		
		return l_BP.get_ID();
	}
	
	protected int createLocation(JSONObject p_order) {
		String l_address = p_order.get("shipping_address").toString();	
		String l_city = p_order.get("shipping_city").toString();
		String l_postal_code= p_order.get("shipping_postal_code").toString();
		int l_country_id = DB.getSQLValue(get_TrxName(), "select c_country_id from c_country " + "where countrycode = ?", p_order.get("shipping_country_code").toString());
		
		MLocation location = new MLocation(getCtx(), l_country_id, 0, l_city, null);
		location.setAddress1(l_address);
		location.setPostal(l_postal_code);
		location.saveEx();
		return location.get_ID();
	}

	private int createBPLocation(int C_BPartner_ID, int C_Location_ID) {
		MBPartnerLocation l_BPlocation = new MBPartnerLocation(getCtx(), 0, null);
		l_BPlocation.setC_BPartner_ID(C_BPartner_ID);
		l_BPlocation.setC_Location_ID(C_Location_ID);
		l_BPlocation.setIsBillTo(true);
		l_BPlocation.setIsShipTo(true);
		l_BPlocation.saveEx();
		return l_BPlocation.getC_BPartner_Location_ID();
	}	

	private void createUser(MBPartner businessPartner, String email, String phone, int C_BPartner_Location_ID) {
		MUser user = new MUser(getCtx(), 0, null);
		user.setAD_Org_ID(0);
		user.setC_BPartner_ID(businessPartner.getC_BPartner_ID());
		user.setC_BPartner_Location_ID(C_BPartner_Location_ID);
		user.setName(businessPartner.getName());
		if(email != null && email.length() > 0 && !email.equals("null"))
			user.setEMail(email);
		else
			user.setEMail("darko_z@yahoo.com");
		user.setPhone(phone);
		user.saveEx();
	}
	
	protected StringBuilder prepareAPIcall(String l_url, int l_limit, int l_start) {
		StringBuilder apicall = null;
		apicall = new StringBuilder(l_url);
		apicall = apicall.append("?include=line_items");
//		if(p_created_after_s!= null)
//			apicall = apicall.append("&created_after="+p_created_after_s.substring(0, 19));
//		if(p_shipstatus != null)
//			apicall = apicall.append("&shipping_status="+p_shipstatus);
		if(P_orderid != null)
			apicall = apicall.append("&id=" + P_orderid );	
		if(l_start > 0)
			apicall = apicall.append("&start=" + l_start );	
		if(l_limit > 0)
			apicall = apicall.append("&limit=" + l_limit );	
		if(p_shipping_status != null)
			apicall = apicall.append("&shipping_status=" + p_shipping_status );
		if(p_shipping_substatus != null)
			apicall = apicall.append("&shipping_substatus_id=" + p_shipping_substatus );
		return apicall;
	}
	
	private void addOrder(JSONObject order) {
		BigDecimal l_freightAmt = null;
        String l_email = order.get("customer_email").toString();
        String l_phone = order.get("shipping_phone").toString();
        String l_paymenttype = order.getString("payment_method_name");
        String l_shipstatus = order.getString("shipping_status");
        String l_payment_method = order.getString("payment_method_code");
        if (order.get("shipping_amount").toString() != "null")
        	l_freightAmt = new BigDecimal(order.get("shipping_amount").toString());
        
        int l_warehouse_id = DB.getSQLValue(get_TrxName(), "select m_warehouse_id from m_warehouse " + "where value = 'VP'");
		if (l_warehouse_id <= 0) {
			l_warehouse_id = DB.getSQLValue(get_TrxName(),
					"select m_warehouse_id from m_warehouse " + "where value = 'Standard' and ad_client_id = ?", Env.getAD_Client_ID(getCtx()));
		}

        morder = new MOrder(getCtx(), 0, get_TrxName());
        String s_date = order.get("date_created").toString().substring(0,10)
        		+ " " 
        		+ order.get("date_created").toString().substring(11,19);
        Timestamp ts = Timestamp.valueOf(s_date);
        morder.setDateOrdered(ts);
        morder.setDateAcct(ts);
        morder.setC_DocTypeTarget_ID(MOrder.DocSubTypeSO_Standard);
        morder.setIsSOTrx(true);
        morder.setDocumentNo(order.get("id").toString());
        morder.setC_BPartner_ID(getBPId(l_email, l_phone, order));
        morder.setM_Warehouse_ID(l_warehouse_id);
        if( l_freightAmt != null) {
        	morder.setFreightCostRule("F");
        	morder.setFreightAmt(l_freightAmt);
        }
        morder.setM_PriceList_ID(getPriceListId());
        morder.set_ValueOfColumn("elf_paymenttype", l_paymenttype);
        morder.set_ValueOfColumn("elf_orderstatus", l_shipstatus);
        
        MPriceList mpl = new MPriceList(getCtx(), morder.getM_PriceList_ID(), get_TrxName());
        boolean l_isTaxIncluded = mpl.isTaxIncluded();
        
        switch(l_payment_method) {
        	case "cash_delivery":
        		morder.setPaymentRule("B");
        		break;
        	case "wire":	
        		morder.setPaymentRule("S");
        		break;
        	case "intesa":	
        		morder.setPaymentRule("C");
        		break;
        	case "raiffeisen_credit":	
        		morder.setPaymentRule("S");
        		break;
        	default:
        		morder.setPaymentRule("B");
        }
        
        try {
        	morder.saveEx();
        	addLog(0, null, null, "Web porudzbina: " + morder.getDocumentNo(), morder.get_Table_ID(),morder.get_ID());
        }
        catch(AdempiereException ae) {
        	addLog(0, null, null, "Web porudzbina nije uspesno kreirana: " + morder.getDocumentNo() + ae.getLocalizedMessage(), morder.get_Table_ID(), morder.get_ID());
        }
        so_created++;
        // kreiranje stavki porudzbine
        addShipmentLine(order, l_isTaxIncluded);
        addOrderLines(order, l_isTaxIncluded); 
	
	}
	
	private void addShipmentLine(JSONObject order, boolean l_isTaxIncluded) {
		// create new order line for shipment
		BigDecimal shippingAmount = new BigDecimal(order.get("shipping_amount").toString());
		if (shippingAmount != null && shippingAmount.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal shippingTax = new BigDecimal(order.get("shipping_tax_amount").toString());
			BigDecimal shippingNet = shippingAmount.subtract(shippingTax);
			MOrderLine ol = new MOrderLine(morder);
			if(p_shipmentproduct_id > 0) {
				ol.setM_Product_ID(p_shipmentproduct_id);
				mshipmentproduct = new MProduct(getCtx(), p_shipmentproduct_id, get_TrxName());
			}
			else {
				if(order.has("shipping_method_name")) {
					int m_product_id = getShipmentProductId(order.get("shipping_method_name").toString());
					mshipmentproduct = new MProduct(getCtx(), m_product_id, get_TrxName());
					if(m_product_id > 0)
						ol.setM_Product_ID(getShipmentProductId(order.get("shipping_method_name").toString()));
					else //throw error
						throw new AdempiereException("Nije pronadjen proizvod za trosak isporuke, kreirajte proizvod sa nazivom: " + order.get("shipping_method_name").toString());
				}
			}
			ol.setQty(new BigDecimal(1));
			if(l_isTaxIncluded) {
				ol.setPriceActual(shippingAmount);
				ol.setPrice(shippingAmount);	
			}
			else {
				ol.setPriceActual(shippingNet);
				ol.setPrice(shippingNet);
			}
			
			if(shippingTax.compareTo(BigDecimal.ZERO) > 0)
				ol.setC_Tax_ID(getTaxId(20));
			else
				ol.setC_Tax_ID(getTaxId(0));
			ol.saveEx();
		}
	}
	
	private void addOrderLines(JSONObject order, boolean l_isTaxIncluded) {
		JSONArray order_lines= order.getJSONArray("line_items");
        List<JSONObject> discountLine = null;
        if(p_includediscount)
        	discountLine = getDiscountLines(order_lines);
        for (int i = 0; i < order_lines.length(); i++) {
        			JSONObject order_line= order_lines.getJSONObject(i);
        			if (isDiscountLine(order_line))
        					continue;
        			String 	p_prod_status = null;
        			String 	p_prod_status_id = null;
        		
        			MOrderLine ol = new MOrderLine(morder);
        			int m_product_id = GetID(order_line.get("product_id").toString());
        			if(m_product_id <= 0) {
						// pozvati proces za kreiranje proizvoda
						callMPGetProductsProcess(ProductUtils.getProductProcessID(), order_line.get("product_id").toString());
						m_product_id = GetID(order_line.get("product_id").toString());
						if(m_product_id <= 0) {
							addBufferLog(0, null, null, "Proizvod nije pronadjen: " + order_line.get("product_id").toString(), ol.get_Table_ID(), ol.get_ID());
							continue;
						}
					}
        			MProduct mp = new MProduct(getCtx(), GetID(order_line.get("product_id").toString()), get_TrxName());
        			BigDecimal l_discount = BigDecimal.ZERO;
        			if (discountLine != null)
						l_discount = getDiscountAmount(discountLine, mp.getValue(),l_isTaxIncluded);
        			
        			BigDecimal p_qty = new BigDecimal(order_line.get("quantity").toString());
        			BigDecimal p_price;
        			if(l_isTaxIncluded)
        				p_price = new BigDecimal(order_line.get("unit_price_gross").toString());
        			else
        				p_price = new BigDecimal(order_line.get("unit_price_net").toString());
        			p_price = p_price.subtract(l_discount.divide(p_qty));
        			if (order_line.has("status")) {
        				p_prod_status = order_line.getJSONObject("status").getString("name");
        				p_prod_status_id = order_line.getJSONObject("status").getString("id");
        			}
        			int l_taxvalue = (int) order_line.get("product_tax_percent");
        			
        			ol.setM_Product_ID(mp.get_ID());
        			ol.setQty(p_qty);
        			ol.setPriceActual(p_price);
        			ol.setPrice(p_price);  			
        			ol.setC_Tax_ID(getTaxId(l_taxvalue));
        			if (p_prod_status != null)
        				ol.set_ValueOfColumn("PO_Status", p_prod_status_id);
           			try {
        				ol.saveEx();
        			}
        			catch(AdempiereException ae) {
        				addBufferLog(0, null, null, "Stavka nije kreirana: " + order_line.get("product_id").toString() + ae.getLocalizedMessage(), ol.get_Table_ID(), ol.get_ID());
        			}
        			
        		};
        	if(order.has("wallet_amount")) {
        		applyWalletAmountToOrderLines(order.getBigDecimal("wallet_amount"), morder.getLines());
			}
	}
	
	private  int getShipmentProductId(String p_shipping_method_name) {
		int m_product_id = DB.getSQLValue(get_TrxName(), "select m_product_id from m_product where name = ?", p_shipping_method_name);
		return m_product_id;
	}
	
	private List<JSONObject> getDiscountLines(JSONArray order_lines) {
	    List<String> discountTypes = Arrays.asList("promo_cart", "promo_product", "discount", "coupon");
	    List<JSONObject> discountLines = new ArrayList<>();
	    for (int i = 0; i < order_lines.length(); i++) {
	        JSONObject order_line = order_lines.getJSONObject(i);
	        if (order_line.has("item_type") && discountTypes.contains(order_line.getString("item_type"))) {
	            discountLines.add(order_line);
	        }
	    }
	    return discountLines;
	}
	
	private boolean isDiscountLine(JSONObject order_line) {
		List<String> discountTypes = Arrays.asList("promo_cart", "promo_product", "discount", "coupon");
		return discountTypes.contains(order_line.getString("item_type"));
	}
	
	private BigDecimal getDiscountAmount(List<JSONObject> discountLines, String product_id, boolean l_isTaxIncluded) {
	    BigDecimal totalDiscountAmount = BigDecimal.ZERO;
	    BigDecimal discountLineAmount = BigDecimal.ZERO;
	    for (JSONObject discountLine : discountLines) {
	        JSONArray appliedDiscounts = discountLine.getJSONArray("applied_discounts");
	        discountLineAmount = BigDecimal.ZERO;
	        for (int i = 0; i < appliedDiscounts.length(); i++) {
	            JSONObject appliedDiscount = appliedDiscounts.getJSONObject(i);
	            if (product_id.equals(Integer.toString(appliedDiscount.getInt("product_id")))) {
	            	discountLineAmount = discountLineAmount.add(appliedDiscount.getBigDecimal("amount"));
	            }
	        }
		    if(l_isTaxIncluded) {
		    	int l_taxvalue = 20; // defaultamount
		    	if(discountLine.has("product_tax_percent"))
		    		l_taxvalue = discountLine.getInt("product_tax_percent");
		    	BigDecimal taxMultiplier = BigDecimal.valueOf(1 + (l_taxvalue / 100.0));
		    	discountLineAmount = discountLineAmount.multiply(taxMultiplier).setScale(2, RoundingMode.HALF_UP);
		    }
		    totalDiscountAmount = totalDiscountAmount.add(discountLineAmount);
	    }

	    return totalDiscountAmount;
	}
	
	public void applyWalletAmountToOrderLines(BigDecimal walletAmount, MOrderLine[] orderLines) {
	    if (walletAmount == null || walletAmount.compareTo(BigDecimal.ZERO) == 0) return;
	    BigDecimal absWalletAmount = walletAmount.abs();

	    // Calculate total value of all lines
	    BigDecimal totalValue = BigDecimal.ZERO;
	    for (int i = 0; i < orderLines.length; i++) {
	    	if(mshipmentproduct != null && mshipmentproduct.get_ID() == orderLines[i].getM_Product_ID()) // ne racunaj trosak isporuke
	    		continue;
	    	else
	    		totalValue = totalValue.add(orderLines[i].getPriceActual().multiply(orderLines[i].getQtyEntered()));
	    }

	    BigDecimal remainingWallet = absWalletAmount;

	    for (int i = 0; i < orderLines.length; i++) {
	        MOrderLine line = orderLines[i];
	        if(mshipmentproduct != null &&  mshipmentproduct.get_ID() == line.getM_Product_ID()) // ne racunaj trosak isporuke
	        	continue;
	        BigDecimal lineValue = line.getPriceActual().multiply(line.getQtyEntered());
	        BigDecimal discount;
	        if (remainingWallet.compareTo(BigDecimal.ZERO) > 0) {
	            if (lineValue.compareTo(remainingWallet) >= 0) {
	                discount = remainingWallet;
	                remainingWallet = BigDecimal.ZERO;
	            } else {
	                discount = lineValue.subtract(line.getQtyEntered().multiply(new BigDecimal(1)));  //minus 1 per each qty
	                if(discount.compareTo(BigDecimal.ZERO) < 0)
	                	continue;
	                remainingWallet = remainingWallet.subtract(lineValue);
	            }
	        } else {
	            discount = BigDecimal.ZERO;
	        }

	        BigDecimal unitDiscount = discount.divide(line.getQtyEntered(), 2, RoundingMode.HALF_UP);
	        BigDecimal newUnitPrice = line.getPriceActual().subtract(unitDiscount);
	        line.setPriceActual(newUnitPrice.max(BigDecimal.ZERO));
	        line.setPrice(newUnitPrice.max(BigDecimal.ZERO));
	        line.saveEx();
	    }
	}
	
	public void callMPGetProductsProcess(int adProcessID, String m_product_id) {
		try {
			MProcess process = (MProcess) MTable.get(Env.getCtx(), MProcess.Table_ID).getPO(adProcessID, null);
			MPInstance instance = new MPInstance(Env.getCtx(), adProcessID, MProcess.Table_ID, 0, null);
			instance.saveEx();

			int seqNo = 10;
			// parameters
			MPInstancePara parameter = new MPInstancePara(instance, seqNo);
			parameter.setParameter("ShopProductID", m_product_id);
			parameter.save();

			ProcessInfo pInfo = new ProcessInfo(process.getName(), process.get_ID());
			pInfo.setIsBatch(true);
			pInfo.setAD_PInstance_ID(instance.get_ID());
			pInfo.setTable_ID(MProduct.Table_ID);

			ServerProcessCtl.process(pInfo, null);

		} catch (Exception e) {
			throw new AdempiereException(e);
		}
	}

}
