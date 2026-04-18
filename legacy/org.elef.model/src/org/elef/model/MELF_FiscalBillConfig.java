package org.elef.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MELF_FiscalBillConfig extends X_ELF_FiscalBillConfig {

	private static final long serialVersionUID = 1L;

	/**
	 * Standard Constructor
	 * 
	 * @param ctx
	 * @param ELF_FiscalBillConfig_ID
	 * @param trxName
	 */
	public MELF_FiscalBillConfig(Properties ctx, int ELF_FiscalBillConfig_ID, String trxName) {
		super(ctx, ELF_FiscalBillConfig_ID, trxName);
	}

	/**
	 * Load Constructor
	 * 
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MELF_FiscalBillConfig(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
