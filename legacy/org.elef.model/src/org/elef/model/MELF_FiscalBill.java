package org.elef.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MELF_FiscalBill extends X_ELF_FiscalBill {

	private static final long serialVersionUID = -8944411548953598049L;

	public MELF_FiscalBill(Properties ctx, int ELF_FiscalBill_ID, String trxName) {
		super(ctx, ELF_FiscalBill_ID, trxName);
	}

	public MELF_FiscalBill(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	// Add any additional methods or overrides if necessary

}
