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

/** Generated Model for ELF_FiscalTax
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ELF_FiscalTax")
public class X_ELF_FiscalTax extends PO implements I_ELF_FiscalTax, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250803L;

    /** Standard Constructor */
    public X_ELF_FiscalTax (Properties ctx, int ELF_FiscalTax_ID, String trxName)
    {
      super (ctx, ELF_FiscalTax_ID, trxName);
      /** if (ELF_FiscalTax_ID == 0)
        {
			setELF_FiscalTax_ID (0);
			setProcessed (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_ELF_FiscalTax (Properties ctx, int ELF_FiscalTax_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_FiscalTax_ID, trxName, virtualColumns);
      /** if (ELF_FiscalTax_ID == 0)
        {
			setELF_FiscalTax_ID (0);
			setProcessed (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_ELF_FiscalTax (Properties ctx, String ELF_FiscalTax_UU, String trxName)
    {
      super (ctx, ELF_FiscalTax_UU, trxName);
      /** if (ELF_FiscalTax_UU == null)
        {
			setELF_FiscalTax_ID (0);
			setProcessed (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_ELF_FiscalTax (Properties ctx, String ELF_FiscalTax_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_FiscalTax_UU, trxName, virtualColumns);
      /** if (ELF_FiscalTax_UU == null)
        {
			setELF_FiscalTax_ID (0);
			setProcessed (false);
// N
        } */
    }

    /** Load Constructor */
    public X_ELF_FiscalTax (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ELF_FiscalTax[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Amount.
		@param Amount Amount in a defined currency
	*/
	public void setAmount (BigDecimal Amount)
	{
		set_ValueNoCheck (COLUMNNAME_Amount, Amount);
	}

	/** Get Amount.
		@return Amount in a defined currency
	  */
	public BigDecimal getAmount()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Amount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_ELF_FiscalBill getELF_FiscalBill() throws RuntimeException
	{
		return (I_ELF_FiscalBill)MTable.get(getCtx(), I_ELF_FiscalBill.Table_ID)
			.getPO(getELF_FiscalBill_ID(), get_TrxName());
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

	/** Set ELF_FiscalTax.
		@param ELF_FiscalTax_ID ELF_FiscalTax
	*/
	public void setELF_FiscalTax_ID (int ELF_FiscalTax_ID)
	{
		if (ELF_FiscalTax_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ELF_FiscalTax_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ELF_FiscalTax_ID, Integer.valueOf(ELF_FiscalTax_ID));
	}

	/** Get ELF_FiscalTax.
		@return ELF_FiscalTax	  */
	public int getELF_FiscalTax_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ELF_FiscalTax_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ELF_FiscalTax_UU.
		@param ELF_FiscalTax_UU ELF_FiscalTax_UU
	*/
	public void setELF_FiscalTax_UU (String ELF_FiscalTax_UU)
	{
		set_Value (COLUMNNAME_ELF_FiscalTax_UU, ELF_FiscalTax_UU);
	}

	/** Get ELF_FiscalTax_UU.
		@return ELF_FiscalTax_UU	  */
	public String getELF_FiscalTax_UU()
	{
		return (String)get_Value(COLUMNNAME_ELF_FiscalTax_UU);
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

	/** Set Rate.
		@param Rate Rate or Tax or Exchange
	*/
	public void setRate (BigDecimal Rate)
	{
		set_ValueNoCheck (COLUMNNAME_Rate, Rate);
	}

	/** Get Rate.
		@return Rate or Tax or Exchange
	  */
	public BigDecimal getRate()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Rate);
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

	/** Set eFiscal_CategoryName.
		@param eFiscal_CategoryName eFiscal_CategoryName
	*/
	public void seteFiscal_CategoryName (String eFiscal_CategoryName)
	{
		set_Value (COLUMNNAME_eFiscal_CategoryName, eFiscal_CategoryName);
	}

	/** Get eFiscal_CategoryName.
		@return eFiscal_CategoryName	  */
	public String geteFiscal_CategoryName()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_CategoryName);
	}

	/** Set eFiscal_CategoryType.
		@param eFiscal_CategoryType eFiscal_CategoryType
	*/
	public void seteFiscal_CategoryType (int eFiscal_CategoryType)
	{
		set_Value (COLUMNNAME_eFiscal_CategoryType, Integer.valueOf(eFiscal_CategoryType));
	}

	/** Get eFiscal_CategoryType.
		@return eFiscal_CategoryType	  */
	public int geteFiscal_CategoryType()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_eFiscal_CategoryType);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set eFiscal_TaxLabel.
		@param eFiscal_TaxLabel eFiscal_TaxLabel
	*/
	public void seteFiscal_TaxLabel (String eFiscal_TaxLabel)
	{
		set_Value (COLUMNNAME_eFiscal_TaxLabel, eFiscal_TaxLabel);
	}

	/** Get eFiscal_TaxLabel.
		@return eFiscal_TaxLabel	  */
	public String geteFiscal_TaxLabel()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_TaxLabel);
	}
}