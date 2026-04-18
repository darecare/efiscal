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

/** Generated Model for ELF_ApiTemplate
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ELF_ApiTemplate")
public class X_ELF_ApiTemplate extends PO implements I_ELF_ApiTemplate, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241016L;

    /** Standard Constructor */
    public X_ELF_ApiTemplate (Properties ctx, int ELF_ApiTemplate_ID, String trxName)
    {
      super (ctx, ELF_ApiTemplate_ID, trxName);
      /** if (ELF_ApiTemplate_ID == 0)
        {
			setELF_ApiTemplate_ID (0);
			setElf_ApiConn_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_ELF_ApiTemplate (Properties ctx, int ELF_ApiTemplate_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_ApiTemplate_ID, trxName, virtualColumns);
      /** if (ELF_ApiTemplate_ID == 0)
        {
			setELF_ApiTemplate_ID (0);
			setElf_ApiConn_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_ELF_ApiTemplate (Properties ctx, String ELF_ApiTemplate_UU, String trxName)
    {
      super (ctx, ELF_ApiTemplate_UU, trxName);
      /** if (ELF_ApiTemplate_UU == null)
        {
			setELF_ApiTemplate_ID (0);
			setElf_ApiConn_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_ELF_ApiTemplate (Properties ctx, String ELF_ApiTemplate_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_ApiTemplate_UU, trxName, virtualColumns);
      /** if (ELF_ApiTemplate_UU == null)
        {
			setELF_ApiTemplate_ID (0);
			setElf_ApiConn_ID (0);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_ELF_ApiTemplate (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ELF_ApiTemplate[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set API Templates.
		@param ELF_ApiTemplate_ID API Templates
	*/
	public void setELF_ApiTemplate_ID (int ELF_ApiTemplate_ID)
	{
		if (ELF_ApiTemplate_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ELF_ApiTemplate_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ELF_ApiTemplate_ID, Integer.valueOf(ELF_ApiTemplate_ID));
	}

	/** Get API Templates.
		@return API Templates	  */
	public int getELF_ApiTemplate_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ELF_ApiTemplate_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ELF_ApiTemplate_UU.
		@param ELF_ApiTemplate_UU ELF_ApiTemplate_UU
	*/
	public void setELF_ApiTemplate_UU (String ELF_ApiTemplate_UU)
	{
		set_Value (COLUMNNAME_ELF_ApiTemplate_UU, ELF_ApiTemplate_UU);
	}

	/** Get ELF_ApiTemplate_UU.
		@return ELF_ApiTemplate_UU	  */
	public String getELF_ApiTemplate_UU()
	{
		return (String)get_Value(COLUMNNAME_ELF_ApiTemplate_UU);
	}

	public I_ELF_ApiConn getElf_ApiConn() throws RuntimeException
	{
		return (I_ELF_ApiConn)MTable.get(getCtx(), I_ELF_ApiConn.Table_ID)
			.getPO(getElf_ApiConn_ID(), get_TrxName());
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

	/** application/json = application/json */
	public static final String ELF_APICONTENTTYPE_ApplicationJson = "application/json";
	/** text/plain = text/plain */
	public static final String ELF_APICONTENTTYPE_TextPlain = "text/plain";
	/** Set Elf_ApiContentType.
		@param Elf_ApiContentType Elf_ApiContentType
	*/
	public void setElf_ApiContentType (String Elf_ApiContentType)
	{

		set_Value (COLUMNNAME_Elf_ApiContentType, Elf_ApiContentType);
	}

	/** Get Elf_ApiContentType.
		@return Elf_ApiContentType	  */
	public String getElf_ApiContentType()
	{
		return (String)get_Value(COLUMNNAME_Elf_ApiContentType);
	}

	/** GET = GET */
	public static final String ELF_APIREQUESTTYPE_GET = "GET";
	/** PATCH = PATCH */
	public static final String ELF_APIREQUESTTYPE_PATCH = "PATCH";
	/** POST = POST */
	public static final String ELF_APIREQUESTTYPE_POST = "POST";
	/** Set Elf_ApiRequestType.
		@param Elf_ApiRequestType Elf_ApiRequestType
	*/
	public void setElf_ApiRequestType (String Elf_ApiRequestType)
	{

		set_Value (COLUMNNAME_Elf_ApiRequestType, Elf_ApiRequestType);
	}

	/** Get Elf_ApiRequestType.
		@return Elf_ApiRequestType	  */
	public String getElf_ApiRequestType()
	{
		return (String)get_Value(COLUMNNAME_Elf_ApiRequestType);
	}

	/** Set Elf_UrlExtend.
		@param Elf_UrlExtend Elf_UrlExtend
	*/
	public void setElf_UrlExtend (String Elf_UrlExtend)
	{
		set_Value (COLUMNNAME_Elf_UrlExtend, Elf_UrlExtend);
	}

	/** Get Elf_UrlExtend.
		@return Elf_UrlExtend	  */
	public String getElf_UrlExtend()
	{
		return (String)get_Value(COLUMNNAME_Elf_UrlExtend);
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