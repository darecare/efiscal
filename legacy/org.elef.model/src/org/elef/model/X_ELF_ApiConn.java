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

/** Generated Model for ELF_ApiConn
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ELF_ApiConn")
public class X_ELF_ApiConn extends PO implements I_ELF_ApiConn, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250822L;

    /** Standard Constructor */
    public X_ELF_ApiConn (Properties ctx, int ELF_ApiConn_ID, String trxName)
    {
      super (ctx, ELF_ApiConn_ID, trxName);
      /** if (ELF_ApiConn_ID == 0)
        {
			setElf_ApiConn_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ELF_ApiConn (Properties ctx, int ELF_ApiConn_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_ApiConn_ID, trxName, virtualColumns);
      /** if (ELF_ApiConn_ID == 0)
        {
			setElf_ApiConn_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ELF_ApiConn (Properties ctx, String ELF_ApiConn_UU, String trxName)
    {
      super (ctx, ELF_ApiConn_UU, trxName);
      /** if (ELF_ApiConn_UU == null)
        {
			setElf_ApiConn_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ELF_ApiConn (Properties ctx, String ELF_ApiConn_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_ApiConn_UU, trxName, virtualColumns);
      /** if (ELF_ApiConn_UU == null)
        {
			setElf_ApiConn_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ELF_ApiConn (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ELF_ApiConn[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** BasicAuth = BS */
	public static final String ELF_APIAUTHTYPE_BasicAuth = "BS";
	/** SessionHandle = SH */
	public static final String ELF_APIAUTHTYPE_SessionHandle = "SH";
	/** mTLS = mTLS */
	public static final String ELF_APIAUTHTYPE_MTLS = "mTLS";
	/** Set Elf_ApiAuthType.
		@param Elf_ApiAuthType Elf_ApiAuthType
	*/
	public void setElf_ApiAuthType (String Elf_ApiAuthType)
	{

		set_Value (COLUMNNAME_Elf_ApiAuthType, Elf_ApiAuthType);
	}

	/** Get Elf_ApiAuthType.
		@return Elf_ApiAuthType	  */
	public String getElf_ApiAuthType()
	{
		return (String)get_Value(COLUMNNAME_Elf_ApiAuthType);
	}

	/** Set API Configuration.
		@param Elf_ApiConn_ID API Configuration
	*/
	public void setElf_ApiConn_ID (int Elf_ApiConn_ID)
	{
		if (Elf_ApiConn_ID < 1)
			set_ValueNoCheck (COLUMNNAME_Elf_ApiConn_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_Elf_ApiConn_ID, Integer.valueOf(Elf_ApiConn_ID));
	}

	/** Get API Configuration.
		@return API Configuration	  */
	public int getElf_ApiConn_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Elf_ApiConn_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Elf_ApiConnn_UU.
		@param Elf_ApiConn_UU Elf_ApiConnn_UU
	*/
	public void setElf_ApiConn_UU (String Elf_ApiConn_UU)
	{
		set_Value (COLUMNNAME_Elf_ApiConn_UU, Elf_ApiConn_UU);
	}

	/** Get Elf_ApiConnn_UU.
		@return Elf_ApiConnn_UU	  */
	public String getElf_ApiConn_UU()
	{
		return (String)get_Value(COLUMNNAME_Elf_ApiConn_UU);
	}

	/** Set Elf_ApiKey.
		@param Elf_ApiKey Elf_ApiKey
	*/
	public void setElf_ApiKey (String Elf_ApiKey)
	{
		set_Value (COLUMNNAME_Elf_ApiKey, Elf_ApiKey);
	}

	/** Get Elf_ApiKey.
		@return Elf_ApiKey	  */
	public String getElf_ApiKey()
	{
		return (String)get_Value(COLUMNNAME_Elf_ApiKey);
	}

	/** Set Elf_ApiPassword.
		@param Elf_ApiPassword Elf_ApiPassword
	*/
	public void setElf_ApiPassword (String Elf_ApiPassword)
	{
		set_Value (COLUMNNAME_Elf_ApiPassword, Elf_ApiPassword);
	}

	/** Get Elf_ApiPassword.
		@return Elf_ApiPassword	  */
	public String getElf_ApiPassword()
	{
		return (String)get_Value(COLUMNNAME_Elf_ApiPassword);
	}

	/** Set Elf_ApiSecret.
		@param Elf_ApiSecret Elf_ApiSecret
	*/
	public void setElf_ApiSecret (String Elf_ApiSecret)
	{
		set_Value (COLUMNNAME_Elf_ApiSecret, Elf_ApiSecret);
	}

	/** Get Elf_ApiSecret.
		@return Elf_ApiSecret	  */
	public String getElf_ApiSecret()
	{
		return (String)get_Value(COLUMNNAME_Elf_ApiSecret);
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

	/** Set Elf_ApiURL.
		@param Elf_ApiURL Elf_ApiURL
	*/
	public void setElf_ApiURL (String Elf_ApiURL)
	{
		set_Value (COLUMNNAME_Elf_ApiURL, Elf_ApiURL);
	}

	/** Get Elf_ApiURL.
		@return Elf_ApiURL	  */
	public String getElf_ApiURL()
	{
		return (String)get_Value(COLUMNNAME_Elf_ApiURL);
	}

	/** Set Elf_ApiUserName.
		@param Elf_ApiUserName Elf_ApiUserName
	*/
	public void setElf_ApiUserName (String Elf_ApiUserName)
	{
		set_Value (COLUMNNAME_Elf_ApiUserName, Elf_ApiUserName);
	}

	/** Get Elf_ApiUserName.
		@return Elf_ApiUserName	  */
	public String getElf_ApiUserName()
	{
		return (String)get_Value(COLUMNNAME_Elf_ApiUserName);
	}

	/** Set Elf_CERT.
		@param Elf_CERT Elf_CERT
	*/
	public void setElf_CERT (String Elf_CERT)
	{
		set_Value (COLUMNNAME_Elf_CERT, Elf_CERT);
	}

	/** Get Elf_CERT.
		@return Elf_CERT	  */
	public String getElf_CERT()
	{
		return (String)get_Value(COLUMNNAME_Elf_CERT);
	}

	/** Set Elf_ExtName.
		@param Elf_ExtName Elf_ExtName
	*/
	public void setElf_ExtName (String Elf_ExtName)
	{
		set_Value (COLUMNNAME_Elf_ExtName, Elf_ExtName);
	}

	/** Get Elf_ExtName.
		@return Elf_ExtName	  */
	public String getElf_ExtName()
	{
		return (String)get_Value(COLUMNNAME_Elf_ExtName);
	}

	/** Set Elf_PAC.
		@param Elf_PAC Elf_PAC
	*/
	public void setElf_PAC (String Elf_PAC)
	{
		set_Value (COLUMNNAME_Elf_PAC, Elf_PAC);
	}

	/** Get Elf_PAC.
		@return Elf_PAC	  */
	public String getElf_PAC()
	{
		return (String)get_Value(COLUMNNAME_Elf_PAC);
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

	/** Set eFiscal_esirno.
		@param eFiscal_esirno eFiscal_esirno
	*/
	public void seteFiscal_esirno (String eFiscal_esirno)
	{
		set_Value (COLUMNNAME_eFiscal_esirno, eFiscal_esirno);
	}

	/** Get eFiscal_esirno.
		@return eFiscal_esirno	  */
	public String geteFiscal_esirno()
	{
		return (String)get_Value(COLUMNNAME_eFiscal_esirno);
	}
}