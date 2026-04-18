package org.elef.efiscal;

import java.util.Properties;

import org.compiere.model.PO;
import org.compiere.model.Query;
import org.elef.model.MELF_FiscalBill;

public class FiscalBillRepository {
    private final Properties ctx;
    private final String trxName;

    public FiscalBillRepository(Properties ctx, String trxName) {
        this.ctx = ctx;
        this.trxName = trxName;
    }
    
    //need method to return fiscal bill for given order id and invoice type and transaction type
    public MELF_FiscalBill getFiscalBill(int orderId, int invoiceType, int transactionType) {
		// Implement logic to retrieve the fiscal bill based on orderId, invoiceType, and transactionType
		// This could involve querying the database or accessing a service
		// For now, returning null as a placeholder
    	PO mf = new Query(ctx, MELF_FiscalBill.Table_Name, "C_Order_ID = ? AND eFiscal_InvoiceType = ? AND eFiscal_TransactionType = ?", trxName)
    			.setClient_ID().setParameters(orderId, invoiceType, transactionType).first();
    	if(mf == null)
    		return null;
		else
			return new MELF_FiscalBill(ctx, mf.get_ID(), trxName);
	}
}
