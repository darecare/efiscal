/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.elef.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for ELF_FiscalBill
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ELF_FiscalBill")
public class X_ELF_FiscalBill extends PO implements I_ELF_FiscalBill, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250818L;

    /** Standard Constructor */
    public X_ELF_FiscalBill (Properties ctx, int ELF_FiscalBill_ID, String trxName)
    {
      super (ctx, ELF_FiscalBill_ID, trxName);
      /** if (ELF_FiscalBill_ID == 0)
        {
			setELF_FiscalBill_ID (0);
			setProcessed (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_ELF_FiscalBill (Properties ctx, int ELF_FiscalBill_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_FiscalBill_ID, trxName, virtualColumns);
      /** if (ELF_FiscalBill_ID == 0)
        {
			setELF_FiscalBill_ID (0);
			setProcessed (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_ELF_FiscalBill (Properties ctx, String ELF_FiscalBill_UU, String trxName)
    {
      super (ctx, ELF_FiscalBill_UU, trxName);
      /** if (ELF_FiscalBill_UU == null)
        {
			setELF_FiscalBill_ID (0);
			setProcessed (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_ELF_FiscalBill (Properties ctx, String ELF_FiscalBill_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_FiscalBill_UU, trxName, virtualColumns);
      /** if (ELF_FiscalBill_UU == null)
        {
			setELF_FiscalBill_ID (0);
			setProcessed (false);
// N
        } */
    }

    /** Load Constructor */
    public X_ELF_FiscalBill (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_ELF_FiscalBill[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_Invoice getC_Invoice() throws RuntimeException
	{
		return (org.compiere.model.I_C_Invoice)MTable.get(getCtx(), org.compiere.model.I_C_Invoice.Table_ID)
			.getPO(getC_Invoice_ID(), get_TrxName());
	}

	/** Set Invoice.
		@param C_Invoice_ID Invoice Identifier
	*/
	public void setC_Invoice_ID (int C_Invoice_ID)
	{
		if (C_Invoice_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Invoice_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Invoice_ID, Integer.valueOf(C_Invoice_ID));
	}

	/** Get Invoice.
		@return Invoice Identifier
	  */
	public int getC_Invoice_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Invoice_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Order getC_Order() throws RuntimeException
	{
		return (org.compiere.model.I_C_Order)MTable.get(getCtx(), org.compiere.model.I_C_Order.Table_ID)
			.getPO(getC_Order_ID(), get_TrxName());
	}

	/** Set Order.
		@param C_Order_ID Order
	*/
	public void setC_Order_ID (int C_Order_ID)
	{
		if (C_Order_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Order_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Order_ID, Integer.valueOf(C_Order_ID));
	}

	/** Get Order.
		@return Order
	  */
	public int getC_Order_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Order_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Payment getC_Payment() throws RuntimeException
	{
		return (org.compiere.model.I_C_Payment)MTable.get(getCtx(), org.compiere.model.I_C_Payment.Table_ID)
			.getPO(getC_Payment_ID(), get_TrxName());
	}

	/** Set Payment.
		@param C_Payment_ID Payment identifier
	*/
	public void setC_Payment_ID (int C_Payment_ID)
	{
		if (C_Payment_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Payment_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Payment_ID, Integer.valueOf(C_Payment_ID));
	}

	/** Get Payment.
		@return Payment identifier
	  */
	public int getC_Payment_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Payment_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ELF_FIscalBill_UU.
		@param ELF_FIscalBill_UU ELF_FIscalBill_UU
	*/
	public void setELF_FIscalBill_UU (String ELF_FIscalBill_UU)
	{
		set_Value (COLUMNNAME_ELF_FIscalBill_UU, ELF_FIscalBill_UU);
	}

	/** Get ELF_FIscalBill_UU.
		@return ELF_FIscalBill_UU	  */
	public String getELF_FIscalBill_UU()
	{
		return (String)get_Value(COLUMNNAME_ELF_FIscalBill_UU);
	}

	/** Set ELF_FiscalBill.
		@param ELF_FiscalBill_ID ELF_FiscalBill
	*/
	public void setELF_FiscalBill_ID (int ELF_FiscalBill_ID)
	{
		if (ELF_FiscalBill_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ELF_FiscalBill_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ELF_FiscalBill_ID, Integer.valueOf(ELF_FiscalBill_ID));
	}

	/** Get ELF_FiscalBill.
		@return ELF_FiscalBill	  */
	public int getELF_FiscalBill_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ELF_FiscalBill_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Processed.
		@param Processed The document has been processed
	*/
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Processed.
		@return The document has been processed
	  */
	public boolean isProcessed()
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Processed On.
		@param ProcessedOn The date+time (expressed in decimal format) when the document has been processed
	*/
	public void setProcessedOn (BigDecimal ProcessedOn)
	{
		set_Value (COLUMNNAME_ProcessedOn, ProcessedOn);
	}

	/** Get Processed On.
		@return The date+time (expressed in decimal format) when the document has been processed
	  */
	public BigDecimal getProcessedOn()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ProcessedOn);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Search Key.
		@param Value Search key for the record in the format required - must be unique
	*/
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue()
	{
		return (String)get_Value(COLUMNNAME_Value);
	}

	/** Set eFiscal_CustomerName.
		@param eFiscal_CustomerName eFiscal_CustomerName
	*/
	public void seteFiscal_CustomerName (String eFiscal_CustomerName)
	{
		set_Value (COLUMNNAME_eFiscal_CustomerName, eFiscal_CustomerName);
	}

	/** Get eFiscal_CustomerName.
		@return eFiscal_CustomerName	  */
	public String geteFiscal_CustomerName()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_CustomerName);
	}

	/** Set eFiscal_Name.
		@param eFiscal_Name eFiscal_Name
	*/
	public void seteFiscal_Name (String eFiscal_Name)
	{
		set_Value (COLUMNNAME_eFiscal_Name, eFiscal_Name);
	}

	/** Get eFiscal_Name.
		@return eFiscal_Name	  */
	public String geteFiscal_Name()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_Name);
	}

	/** Set eFiscal_OrderNo.
		@param eFiscal_OrderNo eFiscal_OrderNo
	*/
	public void seteFiscal_OrderNo (String eFiscal_OrderNo)
	{
		set_Value (COLUMNNAME_eFiscal_OrderNo, eFiscal_OrderNo);
	}

	/** Get eFiscal_OrderNo.
		@return eFiscal_OrderNo	  */
	public String geteFiscal_OrderNo()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_OrderNo);
	}

	/** Set eFiscal_address.
		@param eFiscal_address eFiscal_address
	*/
	public void seteFiscal_address (String eFiscal_address)
	{
		set_Value (COLUMNNAME_eFiscal_address, eFiscal_address);
	}

	/** Get eFiscal_address.
		@return eFiscal_address	  */
	public String geteFiscal_address()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_address);
	}

	/** Set eFiscal_businessname.
		@param eFiscal_businessname eFiscal_businessname
	*/
	public void seteFiscal_businessname (String eFiscal_businessname)
	{
		set_Value (COLUMNNAME_eFiscal_businessname, eFiscal_businessname);
	}

	/** Get eFiscal_businessname.
		@return eFiscal_businessname	  */
	public String geteFiscal_businessname()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_businessname);
	}

	/** Set eFiscal_code.
		@param eFiscal_code eFiscal_code
	*/
	public void seteFiscal_code (String eFiscal_code)
	{
		set_Value (COLUMNNAME_eFiscal_code, eFiscal_code);
	}

	/** Get eFiscal_code.
		@return eFiscal_code	  */
	public String geteFiscal_code()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_code);
	}

	/** Set eFiscal_encryptedinternaldata.
		@param eFiscal_encryptedinternaldata eFiscal_encryptedinternaldata
	*/
	public void seteFiscal_encryptedinternaldata (String eFiscal_encryptedinternaldata)
	{
		set_Value (COLUMNNAME_eFiscal_encryptedinternaldata, eFiscal_encryptedinternaldata);
	}

	/** Get eFiscal_encryptedinternaldata.
		@return eFiscal_encryptedinternaldata	  */
	public String geteFiscal_encryptedinternaldata()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_encryptedinternaldata);
	}

	/** Set eFiscal_invoicecounter.
		@param eFiscal_invoicecounter eFiscal_invoicecounter
	*/
	public void seteFiscal_invoicecounter (String eFiscal_invoicecounter)
	{
		set_Value (COLUMNNAME_eFiscal_invoicecounter, eFiscal_invoicecounter);
	}

	/** Get eFiscal_invoicecounter.
		@return eFiscal_invoicecounter	  */
	public String geteFiscal_invoicecounter()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_invoicecounter);
	}

	/** Set eFiscal_invoicecounterext.
		@param eFiscal_invoicecounterext eFiscal_invoicecounterext
	*/
	public void seteFiscal_invoicecounterext (String eFiscal_invoicecounterext)
	{
		set_Value (COLUMNNAME_eFiscal_invoicecounterext, eFiscal_invoicecounterext);
	}

	/** Get eFiscal_invoicecounterext.
		@return eFiscal_invoicecounterext	  */
	public String geteFiscal_invoicecounterext()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_invoicecounterext);
	}

	/** Set eFiscal_invoicetype.
		@param eFiscal_invoicetype eFiscal_invoicetype
	*/
	public void seteFiscal_invoicetype (int eFiscal_invoicetype)
	{
		set_Value (COLUMNNAME_eFiscal_invoicetype, Integer.valueOf(eFiscal_invoicetype));
	}

	/** Get eFiscal_invoicetype.
		@return eFiscal_invoicetype	  */
	public int geteFiscal_invoicetype()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_eFiscal_invoicetype);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set eFiscal_link.
		@param eFiscal_link eFiscal_link
	*/
	public void seteFiscal_link (String eFiscal_link)
	{
		set_Value (COLUMNNAME_eFiscal_link, eFiscal_link);
	}

	/** Get eFiscal_link.
		@return eFiscal_link	  */
	public String geteFiscal_link()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_link);
	}

	/** Set eFiscal_messages.
		@param eFiscal_messages eFiscal_messages
	*/
	public void seteFiscal_messages (String eFiscal_messages)
	{
		set_Value (COLUMNNAME_eFiscal_messages, eFiscal_messages);
	}

	/** Get eFiscal_messages.
		@return eFiscal_messages	  */
	public String geteFiscal_messages()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_messages);
	}

	/** Set eFiscal_mrc.
		@param eFiscal_mrc eFiscal_mrc
	*/
	public void seteFiscal_mrc (String eFiscal_mrc)
	{
		set_Value (COLUMNNAME_eFiscal_mrc, eFiscal_mrc);
	}

	/** Get eFiscal_mrc.
		@return eFiscal_mrc	  */
	public String geteFiscal_mrc()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_mrc);
	}

	/** Set eFiscal_qr.
		@param eFiscal_qr eFiscal_qr
	*/
	public void seteFiscal_qr (String eFiscal_qr)
	{
		set_Value (COLUMNNAME_eFiscal_qr, eFiscal_qr);
	}

	/** Get eFiscal_qr.
		@return eFiscal_qr	  */
	public String geteFiscal_qr()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_qr);
	}

	/** Set eFiscal_requestedby.
		@param eFiscal_requestedby eFiscal_requestedby
	*/
	public void seteFiscal_requestedby (String eFiscal_requestedby)
	{
		set_Value (COLUMNNAME_eFiscal_requestedby, eFiscal_requestedby);
	}

	/** Get eFiscal_requestedby.
		@return eFiscal_requestedby	  */
	public String geteFiscal_requestedby()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_requestedby);
	}

	/** Set eFiscal_sdc_invoiceno.
		@param eFiscal_sdc_invoiceno eFiscal_sdc_invoiceno
	*/
	public void seteFiscal_sdc_invoiceno (String eFiscal_sdc_invoiceno)
	{
		set_Value (COLUMNNAME_eFiscal_sdc_invoiceno, eFiscal_sdc_invoiceno);
	}

	/** Get eFiscal_sdc_invoiceno.
		@return eFiscal_sdc_invoiceno	  */
	public String geteFiscal_sdc_invoiceno()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_sdc_invoiceno);
	}

	/** Set eFiscal_sdcdatetime.
		@param eFiscal_sdcdatetime eFiscal_sdcdatetime
	*/
	public void seteFiscal_sdcdatetime (String eFiscal_sdcdatetime)
	{
		set_Value (COLUMNNAME_eFiscal_sdcdatetime, eFiscal_sdcdatetime);
	}

	/** Get eFiscal_sdcdatetime.
		@return eFiscal_sdcdatetime	  */
	public String geteFiscal_sdcdatetime()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_sdcdatetime);
	}

	/** Set eFiscal_signature.
		@param eFiscal_signature eFiscal_signature
	*/
	public void seteFiscal_signature (String eFiscal_signature)
	{
		set_Value (COLUMNNAME_eFiscal_signature, eFiscal_signature);
	}

	/** Get eFiscal_signature.
		@return eFiscal_signature	  */
	public String geteFiscal_signature()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_signature);
	}

	/** Set eFiscal_signedby.
		@param eFiscal_signedby eFiscal_signedby
	*/
	public void seteFiscal_signedby (String eFiscal_signedby)
	{
		set_Value (COLUMNNAME_eFiscal_signedby, eFiscal_signedby);
	}

	/** Get eFiscal_signedby.
		@return eFiscal_signedby	  */
	public String geteFiscal_signedby()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_signedby);
	}

	/** Set eFiscal_taxgrouprevision.
		@param eFiscal_taxgrouprevision eFiscal_taxgrouprevision
	*/
	public void seteFiscal_taxgrouprevision (int eFiscal_taxgrouprevision)
	{
		set_Value (COLUMNNAME_eFiscal_taxgrouprevision, Integer.valueOf(eFiscal_taxgrouprevision));
	}

	/** Get eFiscal_taxgrouprevision.
		@return eFiscal_taxgrouprevision	  */
	public int geteFiscal_taxgrouprevision()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_eFiscal_taxgrouprevision);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set eFiscal_tin.
		@param eFiscal_tin eFiscal_tin
	*/
	public void seteFiscal_tin (String eFiscal_tin)
	{
		set_Value (COLUMNNAME_eFiscal_tin, eFiscal_tin);
	}

	/** Get eFiscal_tin.
		@return eFiscal_tin	  */
	public String geteFiscal_tin()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_tin);
	}

	/** Set eFiscal_totalamount.
		@param eFiscal_totalamount eFiscal_totalamount
	*/
	public void seteFiscal_totalamount (BigDecimal eFiscal_totalamount)
	{
		set_Value (COLUMNNAME_eFiscal_totalamount, eFiscal_totalamount);
	}

	/** Get eFiscal_totalamount.
		@return eFiscal_totalamount	  */
	public BigDecimal geteFiscal_totalamount()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_eFiscal_totalamount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set eFiscal_totalcounter.
		@param eFiscal_totalcounter eFiscal_totalcounter
	*/
	public void seteFiscal_totalcounter (BigDecimal eFiscal_totalcounter)
	{
		set_Value (COLUMNNAME_eFiscal_totalcounter, eFiscal_totalcounter);
	}

	/** Get eFiscal_totalcounter.
		@return eFiscal_totalcounter	  */
	public BigDecimal geteFiscal_totalcounter()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_eFiscal_totalcounter);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set eFiscal_transactiontype.
		@param eFiscal_transactiontype eFiscal_transactiontype
	*/
	public void seteFiscal_transactiontype (int eFiscal_transactiontype)
	{
		set_Value (COLUMNNAME_eFiscal_transactiontype, Integer.valueOf(eFiscal_transactiontype));
	}

	/** Get eFiscal_transactiontype.
		@return eFiscal_transactiontype	  */
	public int geteFiscal_transactiontype()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_eFiscal_transactiontype);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set eFiscal_transactiontypecounter.
		@param eFiscal_transactiontypecounter eFiscal_transactiontypecounter
	*/
	public void seteFiscal_transactiontypecounter (int eFiscal_transactiontypecounter)
	{
		set_Value (COLUMNNAME_eFiscal_transactiontypecounter, Integer.valueOf(eFiscal_transactiontypecounter));
	}

	/** Get eFiscal_transactiontypecounter.
		@return eFiscal_transactiontypecounter	  */
	public int geteFiscal_transactiontypecounter()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_eFiscal_transactiontypecounter);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}