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

/** Generated Model for ELF_FiscalBillConfig
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ELF_FiscalBillConfig")
public class X_ELF_FiscalBillConfig extends PO implements I_ELF_FiscalBillConfig, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250803L;

    /** Standard Constructor */
    public X_ELF_FiscalBillConfig (Properties ctx, int ELF_FiscalBillConfig_ID, String trxName)
    {
      super (ctx, ELF_FiscalBillConfig_ID, trxName);
      /** if (ELF_FiscalBillConfig_ID == 0)
        {
			setIsTest (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_ELF_FiscalBillConfig (Properties ctx, int ELF_FiscalBillConfig_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_FiscalBillConfig_ID, trxName, virtualColumns);
      /** if (ELF_FiscalBillConfig_ID == 0)
        {
			setIsTest (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_ELF_FiscalBillConfig (Properties ctx, String ELF_FiscalBillConfig_UU, String trxName)
    {
      super (ctx, ELF_FiscalBillConfig_UU, trxName);
      /** if (ELF_FiscalBillConfig_UU == null)
        {
			setIsTest (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_ELF_FiscalBillConfig (Properties ctx, String ELF_FiscalBillConfig_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_FiscalBillConfig_UU, trxName, virtualColumns);
      /** if (ELF_FiscalBillConfig_UU == null)
        {
			setIsTest (false);
// N
        } */
    }

    /** Load Constructor */
    public X_ELF_FiscalBillConfig (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ELF_FiscalBillConfig[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_PrintFormat getAD_PrintFormat() throws RuntimeException
	{
		return (org.compiere.model.I_AD_PrintFormat)MTable.get(getCtx(), org.compiere.model.I_AD_PrintFormat.Table_ID)
			.getPO(getAD_PrintFormat_ID(), get_TrxName());
	}

	/** Set Print Format.
		@param AD_PrintFormat_ID Data Print Format
	*/
	public void setAD_PrintFormat_ID (int AD_PrintFormat_ID)
	{
		if (AD_PrintFormat_ID < 1)
			set_Value (COLUMNNAME_AD_PrintFormat_ID, null);
		else
			set_Value (COLUMNNAME_AD_PrintFormat_ID, Integer.valueOf(AD_PrintFormat_ID));
	}

	/** Get Print Format.
		@return Data Print Format
	  */
	public int getAD_PrintFormat_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_PrintFormat_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Fiscal Bill Configuration.
		@param ELF_FiscalBillConfig_ID Fiscal Bill Configuration
	*/
	public void setELF_FiscalBillConfig_ID (int ELF_FiscalBillConfig_ID)
	{
		if (ELF_FiscalBillConfig_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ELF_FiscalBillConfig_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ELF_FiscalBillConfig_ID, Integer.valueOf(ELF_FiscalBillConfig_ID));
	}

	/** Get Fiscal Bill Configuration.
		@return Fiscal Bill Configuration	  */
	public int getELF_FiscalBillConfig_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ELF_FiscalBillConfig_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ELF_FiscalBillConfig_UU.
		@param ELF_FiscalBillConfig_UU ELF_FiscalBillConfig_UU
	*/
	public void setELF_FiscalBillConfig_UU (String ELF_FiscalBillConfig_UU)
	{
		set_Value (COLUMNNAME_ELF_FiscalBillConfig_UU, ELF_FiscalBillConfig_UU);
	}

	/** Get ELF_FiscalBillConfig_UU.
		@return ELF_FiscalBillConfig_UU	  */
	public String getELF_FiscalBillConfig_UU()
	{
		return (String)get_Value(COLUMNNAME_ELF_FiscalBillConfig_UU);
	}

	/** Set EMail_Bcc.
		@param EMail_Bcc EMail_Bcc
	*/
	public void setEMail_Bcc (String EMail_Bcc)
	{
		set_Value (COLUMNNAME_EMail_Bcc, EMail_Bcc);
	}

	/** Get EMail_Bcc.
		@return EMail_Bcc	  */
	public String getEMail_Bcc()
	{
		return (String)get_Value(COLUMNNAME_EMail_Bcc);
	}

	/** Set From EMail.
		@param EMail_From Full EMail address used to send requests - e.g. edi@organization.com
	*/
	public void setEMail_From (String EMail_From)
	{
		set_Value (COLUMNNAME_EMail_From, EMail_From);
	}

	/** Get From EMail.
		@return Full EMail address used to send requests - e.g. edi@organization.com
	  */
	public String getEMail_From()
	{
		return (String)get_Value(COLUMNNAME_EMail_From);
	}

	/** Set EMail_Test.
		@param EMail_Test EMail_Test
	*/
	public void setEMail_Test (String EMail_Test)
	{
		set_Value (COLUMNNAME_EMail_Test, EMail_Test);
	}

	/** Get EMail_Test.
		@return EMail_Test	  */
	public String getEMail_Test()
	{
		return (String)get_Value(COLUMNNAME_EMail_Test);
	}

	/** Set Test.
		@param IsTest Execute in Test Mode
	*/
	public void setIsTest (boolean IsTest)
	{
		set_Value (COLUMNNAME_IsTest, Boolean.valueOf(IsTest));
	}

	/** Get Test.
		@return Execute in Test Mode
	  */
	public boolean isTest()
	{
		Object oo = get_Value(COLUMNNAME_IsTest);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	public org.compiere.model.I_R_MailText getR_MailText() throws RuntimeException
	{
		return (org.compiere.model.I_R_MailText)MTable.get(getCtx(), org.compiere.model.I_R_MailText.Table_ID)
			.getPO(getR_MailText_ID(), get_TrxName());
	}

	/** Set Mail Template.
		@param R_MailText_ID Text templates for mailings
	*/
	public void setR_MailText_ID (int R_MailText_ID)
	{
		if (R_MailText_ID < 1)
			set_Value (COLUMNNAME_R_MailText_ID, null);
		else
			set_Value (COLUMNNAME_R_MailText_ID, Integer.valueOf(R_MailText_ID));
	}

	/** Get Mail Template.
		@return Text templates for mailings
	  */
	public int getR_MailText_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_R_MailText_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}