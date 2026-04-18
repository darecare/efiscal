package org.elef.model.factory;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.elef.model.MELF_ApiConn;
import org.elef.model.MELF_ApiField;
import org.elef.model.MELF_ApiTemplate;

public class ElfModelFactory implements IModelFactory{

	@Override
	public Class<?> getClass(String tableName) {
		// TODO Auto-generated method stub
		if (tableName.equalsIgnoreCase(MELF_ApiConn.Table_Name))
			return MELF_ApiConn.class;
		if (tableName.equalsIgnoreCase(MELF_ApiTemplate.Table_Name))
			return MELF_ApiTemplate.class;
		if (tableName.equalsIgnoreCase(MELF_ApiField.Table_Name))
			return MELF_ApiField.class;
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		// TODO Auto-generated method stub
		if (tableName.equalsIgnoreCase(MELF_ApiConn.Table_Name))
			return new MELF_ApiConn(Env.getCtx(), Record_ID, trxName);
		if (tableName.equalsIgnoreCase(MELF_ApiTemplate.Table_Name))
			return new MELF_ApiTemplate(Env.getCtx(), Record_ID, trxName);
		if (tableName.equalsIgnoreCase(MELF_ApiField.Table_Name))
			return new MELF_ApiField(Env.getCtx(), Record_ID, trxName);
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		// TODO Auto-generated method stub
		if (tableName.equalsIgnoreCase(MELF_ApiConn.Table_Name))
			return new MELF_ApiConn(Env.getCtx(), rs, trxName);
		if (tableName.equalsIgnoreCase(MELF_ApiTemplate.Table_Name))
			return new MELF_ApiTemplate(Env.getCtx(), rs, trxName);
		if (tableName.equalsIgnoreCase(MELF_ApiField.Table_Name))
			return new MELF_ApiField(Env.getCtx(), rs, trxName);
		return null;
	}

}
