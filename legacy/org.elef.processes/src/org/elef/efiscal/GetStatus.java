package org.elef.efiscal;

import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.elef.model.MELF_ApiConn;
import org.elef.model.MELF_ApiTemplate;

public class GetStatus extends SvrProcess{

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub
		int elf_apiconn_id = MELF_ApiConn.GetID(get_TrxName(), Env.getCtx(), "EF");
		MELF_ApiConn ElfApiConn = new MELF_ApiConn(getCtx(), elf_apiconn_id, get_TrxName());
		MELF_ApiTemplate ElfApiTemplate = MELF_ApiTemplate.getTemplate(get_TrxName(), getCtx(), "Get_Status", ElfApiConn.get_ID());
		
		
		return null;
	}

}
