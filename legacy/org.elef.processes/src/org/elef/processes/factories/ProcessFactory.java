package org.elef.processes.factories;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;
import org.elef.mp.products.MPGetProducts;

public class ProcessFactory implements IProcessFactory{

	@Override
	public ProcessCall newProcessInstance(String className) {
		// TODO Auto-generated method stub
		if(className.equalsIgnoreCase("org.elef.mp.products.mpgetproducts")) {
			return new MPGetProducts();
		}
		if (className.equalsIgnoreCase("org.elef.bs.products.bsgetproducts")) {
			return new org.elef.bs.products.BSGetProducts();
		}
		if (className.equalsIgnoreCase("org.elef.mp.orders.mpgetorders")) {
			return new org.elef.mp.orders.MPGetOrders();
		}
		if (className.equalsIgnoreCase("org.elef.bs.orders.bssendorders")) {
			return new org.elef.bs.orders.BSSendOrders();
		}
		if (className.equalsIgnoreCase("org.elef.efiscal.getstatus")) {
			return new org.elef.efiscal.GetStatus();
		}
		if (className.equalsIgnoreCase("org.elef.efiscal.postfiscalbill")) {
			return new org.elef.efiscal.PostFiscalBill();
		}

		return null;
	}

}
