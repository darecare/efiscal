package org.elef.efiscal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.elef.model.MELF_FiscalBill;


public class PostFiscalBill extends SvrProcess {

	private int p_c_order_id = 0;
	private int p_invoiceType = 0; // 0-Normal, 1-Proforma, 2-Copy, 3-Training, 4-Advance
	private int p_transactionType = 0; // 0-Sale, 1-Refund
	private boolean p_IsSendMail	= false;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("invoiceType"))
				p_invoiceType = para[i].getParameterAsInt();
			else if (name.equals("transactionType"))
				p_transactionType = para[i].getParameterAsInt();
			else if (name.equals("IsSendMail"))
				p_IsSendMail = "Y".equals(para[i].getParameter());
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
		if (getTable_ID() == MOrder.Table_ID && getRecord_ID() > 0) {
			p_c_order_id = getRecord_ID();
		}
	}

	@Override
	protected String doIt() throws Exception {
		MOrder order = null;
		if(p_c_order_id > 0)
			order = new MOrder(getCtx(), p_c_order_id, get_TrxName());
		
		List<Integer> selectedOrderIDs = new ArrayList<>();
		String sql = "SELECT t_selection_id FROM T_Selection WHERE AD_PInstance_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
		    pstmt = DB.prepareStatement(sql, get_TrxName());
		    pstmt.setInt(1, getAD_PInstance_ID());
		    rs = pstmt.executeQuery();
		    while (rs.next()) {
		        selectedOrderIDs.add(rs.getInt(1));
		    }
		} finally {
		    DB.close(rs, pstmt);
		}
		if(order == null && selectedOrderIDs.size() > 0) {
			order = new MOrder(getCtx(), selectedOrderIDs.get(0), get_TrxName());
		} else if(order == null) {
			throw new AdempiereException("No order found to process.");
		}
		FiscalBillService service = new FiscalBillService(getCtx(), get_TrxName(),getAD_PInstance_ID());
        MELF_FiscalBill bill = service.processFiscalBill(
            order, p_invoiceType, p_transactionType, p_IsSendMail);
        statusUpdate("Processed");
        order.setProcessMessage("tttt");
        order.saveEx();
		return "RPOBA";
	}

	
	private ResultSet getResultSet() {

		return null; // Placeholder
	}
	
}
