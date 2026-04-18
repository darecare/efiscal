package org.elef.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Env;

public class MELF_ApiField extends X_ELF_ApiField{

	public MELF_ApiField(Properties ctx, int ELF_ApiField_ID, String trxName) {
		super(ctx, ELF_ApiField_ID, trxName);
		// TODO Auto-generated constructor stub
	}


	public MELF_ApiField(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 2547618682377286020L;
	
	public static String getExtName(String trxName, Properties ctx, String l_name,int elf_apitemplate_id) {
		String elf_ExtName = DB.getSQLValueString(trxName, "select elf_extname from elf_apifield where isactive = 'Y' and ad_client_id = ? and name = ? and elf_apitemplate_id = ?", Env.getAD_Client_ID(ctx), l_name, elf_apitemplate_id );	
		return elf_ExtName;
	}

}
