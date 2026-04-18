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

/** Generated Model for ELF_ApiField
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ELF_ApiField")
public class X_ELF_ApiField extends PO implements I_ELF_ApiField, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241127L;

    /** Standard Constructor */
    public X_ELF_ApiField (Properties ctx, int ELF_ApiField_ID, String trxName)
    {
      super (ctx, ELF_ApiField_ID, trxName);
      /** if (ELF_ApiField_ID == 0)
        {
			setELF_ApiField_ID (0);
			setElf_IsBase (false);
// N
			setElf_IsRequest (false);
// N
			setElf_IsResponse (false);
// N
			setElf_isArray (false);
// N
			setElf_setDefault (false);
// N
			setIsMandatory (false);
// N
			setIsParent (false);
// N
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_ELF_ApiField (Properties ctx, int ELF_ApiField_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_ApiField_ID, trxName, virtualColumns);
      /** if (ELF_ApiField_ID == 0)
        {
			setELF_ApiField_ID (0);
			setElf_IsBase (false);
// N
			setElf_IsRequest (false);
// N
			setElf_IsResponse (false);
// N
			setElf_isArray (false);
// N
			setElf_setDefault (false);
// N
			setIsMandatory (false);
// N
			setIsParent (false);
// N
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_ELF_ApiField (Properties ctx, String ELF_ApiField_UU, String trxName)
    {
      super (ctx, ELF_ApiField_UU, trxName);
      /** if (ELF_ApiField_UU == null)
        {
			setELF_ApiField_ID (0);
			setElf_IsBase (false);
// N
			setElf_IsRequest (false);
// N
			setElf_IsResponse (false);
// N
			setElf_isArray (false);
// N
			setElf_setDefault (false);
// N
			setIsMandatory (false);
// N
			setIsParent (false);
// N
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_ELF_ApiField (Properties ctx, String ELF_ApiField_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ELF_ApiField_UU, trxName, virtualColumns);
      /** if (ELF_ApiField_UU == null)
        {
			setELF_ApiField_ID (0);
			setElf_IsBase (false);
// N
			setElf_IsRequest (false);
// N
			setElf_IsResponse (false);
// N
			setElf_isArray (false);
// N
			setElf_setDefault (false);
// N
			setIsMandatory (false);
// N
			setIsParent (false);
// N
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_ELF_ApiField (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ELF_ApiField[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set ELF_ApiField.
		@param ELF_ApiField_ID ELF_ApiField
	*/
	public void setELF_ApiField_ID (int ELF_ApiField_ID)
	{
		if (ELF_ApiField_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ELF_ApiField_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ELF_ApiField_ID, Integer.valueOf(ELF_ApiField_ID));
	}

	/** Get ELF_ApiField.
		@return ELF_ApiField	  */
	public int getELF_ApiField_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ELF_ApiField_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ELF_ApiField_UU.
		@param ELF_ApiField_UU ELF_ApiField_UU
	*/
	public void setELF_ApiField_UU (String ELF_ApiField_UU)
	{
		set_Value (COLUMNNAME_ELF_ApiField_UU, ELF_ApiField_UU);
	}

	/** Get ELF_ApiField_UU.
		@return ELF_ApiField_UU	  */
	public String getELF_ApiField_UU()
	{
		return (String)get_Value(COLUMNNAME_ELF_ApiField_UU);
	}

	public I_ELF_ApiTemplate getELF_ApiTemplate() throws RuntimeException
	{
		return (I_ELF_ApiTemplate)MTable.get(getCtx(), I_ELF_ApiTemplate.Table_ID)
			.getPO(getELF_ApiTemplate_ID(), get_TrxName());
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

	public I_ELF_ApiField getELF_ParentApiField() throws RuntimeException
	{
		return (I_ELF_ApiField)MTable.get(getCtx(), I_ELF_ApiField.Table_ID)
			.getPO(getELF_ParentApiField_ID(), get_TrxName());
	}

	/** Set ELF_ParentApiField.
		@param ELF_ParentApiField_ID ELF_ParentApiField
	*/
	public void setELF_ParentApiField_ID (int ELF_ParentApiField_ID)
	{
		if (ELF_ParentApiField_ID < 1)
			set_Value (COLUMNNAME_ELF_ParentApiField_ID, null);
		else
			set_Value (COLUMNNAME_ELF_ParentApiField_ID, Integer.valueOf(ELF_ParentApiField_ID));
	}

	/** Get ELF_ParentApiField.
		@return ELF_ParentApiField	  */
	public int getELF_ParentApiField_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ELF_ParentApiField_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Elf_DefaultValue.
		@param Elf_DefaultValue Elf_DefaultValue
	*/
	public void setElf_DefaultValue (String Elf_DefaultValue)
	{
		set_Value (COLUMNNAME_Elf_DefaultValue, Elf_DefaultValue);
	}

	/** Get Elf_DefaultValue.
		@return Elf_DefaultValue	  */
	public String getElf_DefaultValue()
	{
		return (String)get_Value(COLUMNNAME_Elf_DefaultValue);
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

	/** Set Elf_IsBase.
		@param Elf_IsBase Elf_IsBase
	*/
	public void setElf_IsBase (boolean Elf_IsBase)
	{
		set_Value (COLUMNNAME_Elf_IsBase, Boolean.valueOf(Elf_IsBase));
	}

	/** Get Elf_IsBase.
		@return Elf_IsBase	  */
	public boolean isElf_IsBase()
	{
		Object oo = get_Value(COLUMNNAME_Elf_IsBase);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Elf_IsRequest.
		@param Elf_IsRequest Elf_IsRequest
	*/
	public void setElf_IsRequest (boolean Elf_IsRequest)
	{
		set_Value (COLUMNNAME_Elf_IsRequest, Boolean.valueOf(Elf_IsRequest));
	}

	/** Get Elf_IsRequest.
		@return Elf_IsRequest	  */
	public boolean isElf_IsRequest()
	{
		Object oo = get_Value(COLUMNNAME_Elf_IsRequest);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Elf_IsResponse.
		@param Elf_IsResponse Elf_IsResponse
	*/
	public void setElf_IsResponse (boolean Elf_IsResponse)
	{
		set_Value (COLUMNNAME_Elf_IsResponse, Boolean.valueOf(Elf_IsResponse));
	}

	/** Get Elf_IsResponse.
		@return Elf_IsResponse	  */
	public boolean isElf_IsResponse()
	{
		Object oo = get_Value(COLUMNNAME_Elf_IsResponse);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Elf_SQLName.
		@param Elf_SQLName Elf_SQLName
	*/
	public void setElf_SQLName (String Elf_SQLName)
	{
		set_Value (COLUMNNAME_Elf_SQLName, Elf_SQLName);
	}

	/** Get Elf_SQLName.
		@return Elf_SQLName	  */
	public String getElf_SQLName()
	{
		return (String)get_Value(COLUMNNAME_Elf_SQLName);
	}

	/** Set Elf_isArray.
		@param Elf_isArray Elf_isArray
	*/
	public void setElf_isArray (boolean Elf_isArray)
	{
		set_Value (COLUMNNAME_Elf_isArray, Boolean.valueOf(Elf_isArray));
	}

	/** Get Elf_isArray.
		@return Elf_isArray	  */
	public boolean isElf_isArray()
	{
		Object oo = get_Value(COLUMNNAME_Elf_isArray);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Elf_listPosition.
		@param Elf_listPosition Elf_listPosition
	*/
	public void setElf_listPosition (int Elf_listPosition)
	{
		set_Value (COLUMNNAME_Elf_listPosition, Integer.valueOf(Elf_listPosition));
	}

	/** Get Elf_listPosition.
		@return Elf_listPosition	  */
	public int getElf_listPosition()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Elf_listPosition);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Elf_setDefault.
		@param Elf_setDefault Elf_setDefault
	*/
	public void setElf_setDefault (boolean Elf_setDefault)
	{
		set_Value (COLUMNNAME_Elf_setDefault, Boolean.valueOf(Elf_setDefault));
	}

	/** Get Elf_setDefault.
		@return Elf_setDefault	  */
	public boolean isElf_setDefault()
	{
		Object oo = get_Value(COLUMNNAME_Elf_setDefault);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Mandatory.
		@param IsMandatory Data entry is required in this column
	*/
	public void setIsMandatory (boolean IsMandatory)
	{
		set_Value (COLUMNNAME_IsMandatory, Boolean.valueOf(IsMandatory));
	}

	/** Get Mandatory.
		@return Data entry is required in this column
	  */
	public boolean isMandatory()
	{
		Object oo = get_Value(COLUMNNAME_IsMandatory);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Parent link column.
		@param IsParent This column is a link to the parent table (e.g. header from lines) - incl. Association key columns
	*/
	public void setIsParent (boolean IsParent)
	{
		set_Value (COLUMNNAME_IsParent, Boolean.valueOf(IsParent));
	}

	/** Get Parent link column.
		@return This column is a link to the parent table (e.g. header from lines) - incl. Association key columns
	  */
	public boolean isParent()
	{
		Object oo = get_Value(COLUMNNAME_IsParent);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
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