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

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for ELF_PaymentMethod
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ELF_PaymentMethod")
public class X_ELF_PaymentMethod extends PO implements I_ELF_PaymentMethod, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20251023L;

    /** Standard Constructor */
    public X_ELF_PaymentMethod (Properties ctx, int ELF_PaymentMethod_ID, String trxName)
    {
      super (ctx, ELF_PaymentMethod_ID, trxName);
      /** if (ELF_PaymentMethod_ID == 0)
        {
			setELF_PaymentMethod_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_ELF_PaymentMethod (Properties ctx, int ELF_PaymentMethod_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_PaymentMethod_ID, trxName, virtualColumns);
      /** if (ELF_PaymentMethod_ID == 0)
        {
			setELF_PaymentMethod_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_ELF_PaymentMethod (Properties ctx, String ELF_PaymentMethod_UU, String trxName)
    {
      super (ctx, ELF_PaymentMethod_UU, trxName);
      /** if (ELF_PaymentMethod_UU == null)
        {
			setELF_PaymentMethod_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_ELF_PaymentMethod (Properties ctx, String ELF_PaymentMethod_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_PaymentMethod_UU, trxName, virtualColumns);
      /** if (ELF_PaymentMethod_UU == null)
        {
			setELF_PaymentMethod_ID (0);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_ELF_PaymentMethod (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ELF_PaymentMethod[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set ELF_PaymentMethod_FiscalName.
		@param ELF_PaymentMethod_FiscalName ELF_PaymentMethod_FiscalName
	*/
	public void setELF_PaymentMethod_FiscalName (String ELF_PaymentMethod_FiscalName)
	{
		set_Value (COLUMNNAME_ELF_PaymentMethod_FiscalName, ELF_PaymentMethod_FiscalName);
	}

	/** Get ELF_PaymentMethod_FiscalName.
		@return ELF_PaymentMethod_FiscalName	  */
	public String getELF_PaymentMethod_FiscalName()
	{
		return (String)get_Value(COLUMNNAME_ELF_PaymentMethod_FiscalName);
	}

	/** Set ELF_PaymentMethod_FiscalValue.
		@param ELF_PaymentMethod_FiscalValue ELF_PaymentMethod_FiscalValue
	*/
	public void setELF_PaymentMethod_FiscalValue (int ELF_PaymentMethod_FiscalValue)
	{
		set_Value (COLUMNNAME_ELF_PaymentMethod_FiscalValue, Integer.valueOf(ELF_PaymentMethod_FiscalValue));
	}

	/** Get ELF_PaymentMethod_FiscalValue.
		@return ELF_PaymentMethod_FiscalValue	  */
	public int getELF_PaymentMethod_FiscalValue()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ELF_PaymentMethod_FiscalValue);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ELF_PaymentMethod.
		@param ELF_PaymentMethod_ID ELF_PaymentMethod
	*/
	public void setELF_PaymentMethod_ID (int ELF_PaymentMethod_ID)
	{
		if (ELF_PaymentMethod_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ELF_PaymentMethod_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ELF_PaymentMethod_ID, Integer.valueOf(ELF_PaymentMethod_ID));
	}

	/** Get ELF_PaymentMethod.
		@return ELF_PaymentMethod	  */
	public int getELF_PaymentMethod_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ELF_PaymentMethod_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ELF_PaymentMethod_UU.
		@param ELF_PaymentMethod_UU ELF_PaymentMethod_UU
	*/
	public void setELF_PaymentMethod_UU (String ELF_PaymentMethod_UU)
	{
		set_Value (COLUMNNAME_ELF_PaymentMethod_UU, ELF_PaymentMethod_UU);
	}

	/** Get ELF_PaymentMethod_UU.
		@return ELF_PaymentMethod_UU	  */
	public String getELF_PaymentMethod_UU()
	{
		return (String)get_Value(COLUMNNAME_ELF_PaymentMethod_UU);
	}

	/** BiznisSoft = BS */
	public static final String ELF_APISYSTEM_BiznisSoft = "BS";
	/** eFiscal = EF */
	public static final String ELF_APISYSTEM_EFiscal = "EF";
	/** MerchantPro = MP */
	public static final String ELF_APISYSTEM_MerchantPro = "MP";
	/** Set Elf_ApiSystem.
		@param Elf_ApiSystem Elf_ApiSystem
	*/
	public void setElf_ApiSystem (String Elf_ApiSystem)
	{

		set_Value (COLUMNNAME_Elf_ApiSystem, Elf_ApiSystem);
	}

	/** Get Elf_ApiSystem.
		@return Elf_ApiSystem	  */
	public String getElf_ApiSystem()
	{
		return (String)get_Value(COLUMNNAME_Elf_ApiSystem);
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
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
}