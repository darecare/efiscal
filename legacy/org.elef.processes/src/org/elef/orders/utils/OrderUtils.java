package org.elef.orders.utils;

import java.util.List;

import org.compiere.model.MOrder;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class OrderUtils {

	public static List<MOrder> getListOfUnSentOrders(String trx, int p_InvoiceType)
	{
		List<MOrder> morder = new Query(Env.getCtx(), MOrder.Table_Name, "elf_issent = 'N' and ad_client_id = ?", trx)
				.setOnlyActiveRecords(true).setClient_ID()
				.setParameters(Env.getAD_Client_ID(Env.getCtx())).setOrderBy("Created DESC").list();

		return morder;
	}
	//check externalID field on BPartner
	public static String getBPartnerExternalID(String trx, MOrder order) {
		String elf_externalId = DB.getSQLValueString(trx, "select elf_externalid from c_bpartner where c_bpartner_id = ? and ad_client_id = ?", order.getC_BPartner_ID(), Env.getAD_Client_ID(Env.getCtx()));
		return elf_externalId;
	}
}
