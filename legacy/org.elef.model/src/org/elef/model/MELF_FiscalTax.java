package org.elef.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MELF_FiscalTax extends X_ELF_FiscalTax {

	private static final long serialVersionUID = 9108133848293242583L;

	public MELF_FiscalTax(Properties ctx, int ELF_FiscalTax_ID, String trxName) {
		super(ctx, ELF_FiscalTax_ID, trxName);
		// TODO Auto-generated constructor stub
	}
	
	public MELF_FiscalTax(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

}
