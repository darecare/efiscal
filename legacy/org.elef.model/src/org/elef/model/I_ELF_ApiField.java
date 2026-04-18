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
package org.elef.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for ELF_ApiField
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ELF_ApiField 
{

    /** TableName=ELF_ApiField */
    public static final String Table_Name = "ELF_ApiField";

    /** AD_Table_ID=1000006 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name ELF_ApiField_ID */
    public static final String COLUMNNAME_ELF_ApiField_ID = "ELF_ApiField_ID";

	/** Set ELF_ApiField	  */
	public void setELF_ApiField_ID (int ELF_ApiField_ID);

	/** Get ELF_ApiField	  */
	public int getELF_ApiField_ID();

    /** Column name ELF_ApiField_UU */
    public static final String COLUMNNAME_ELF_ApiField_UU = "ELF_ApiField_UU";

	/** Set ELF_ApiField_UU	  */
	public void setELF_ApiField_UU (String ELF_ApiField_UU);

	/** Get ELF_ApiField_UU	  */
	public String getELF_ApiField_UU();

    /** Column name ELF_ApiTemplate_ID */
    public static final String COLUMNNAME_ELF_ApiTemplate_ID = "ELF_ApiTemplate_ID";

	/** Set API Templates	  */
	public void setELF_ApiTemplate_ID (int ELF_ApiTemplate_ID);

	/** Get API Templates	  */
	public int getELF_ApiTemplate_ID();

	public I_ELF_ApiTemplate getELF_ApiTemplate() throws RuntimeException;

    /** Column name ELF_ParentApiField_ID */
    public static final String COLUMNNAME_ELF_ParentApiField_ID = "ELF_ParentApiField_ID";

	/** Set ELF_ParentApiField	  */
	public void setELF_ParentApiField_ID (int ELF_ParentApiField_ID);

	/** Get ELF_ParentApiField	  */
	public int getELF_ParentApiField_ID();

	public I_ELF_ApiField getELF_ParentApiField() throws RuntimeException;

    /** Column name Elf_DefaultValue */
    public static final String COLUMNNAME_Elf_DefaultValue = "Elf_DefaultValue";

	/** Set Elf_DefaultValue	  */
	public void setElf_DefaultValue (String Elf_DefaultValue);

	/** Get Elf_DefaultValue	  */
	public String getElf_DefaultValue();

    /** Column name Elf_ExtName */
    public static final String COLUMNNAME_Elf_ExtName = "Elf_ExtName";

	/** Set Elf_ExtName	  */
	public void setElf_ExtName (String Elf_ExtName);

	/** Get Elf_ExtName	  */
	public String getElf_ExtName();

    /** Column name Elf_IsBase */
    public static final String COLUMNNAME_Elf_IsBase = "Elf_IsBase";

	/** Set Elf_IsBase	  */
	public void setElf_IsBase (boolean Elf_IsBase);

	/** Get Elf_IsBase	  */
	public boolean isElf_IsBase();

    /** Column name Elf_IsRequest */
    public static final String COLUMNNAME_Elf_IsRequest = "Elf_IsRequest";

	/** Set Elf_IsRequest	  */
	public void setElf_IsRequest (boolean Elf_IsRequest);

	/** Get Elf_IsRequest	  */
	public boolean isElf_IsRequest();

    /** Column name Elf_IsResponse */
    public static final String COLUMNNAME_Elf_IsResponse = "Elf_IsResponse";

	/** Set Elf_IsResponse	  */
	public void setElf_IsResponse (boolean Elf_IsResponse);

	/** Get Elf_IsResponse	  */
	public boolean isElf_IsResponse();

    /** Column name Elf_SQLName */
    public static final String COLUMNNAME_Elf_SQLName = "Elf_SQLName";

	/** Set Elf_SQLName	  */
	public void setElf_SQLName (String Elf_SQLName);

	/** Get Elf_SQLName	  */
	public String getElf_SQLName();

    /** Column name Elf_isArray */
    public static final String COLUMNNAME_Elf_isArray = "Elf_isArray";

	/** Set Elf_isArray	  */
	public void setElf_isArray (boolean Elf_isArray);

	/** Get Elf_isArray	  */
	public boolean isElf_isArray();

    /** Column name Elf_listPosition */
    public static final String COLUMNNAME_Elf_listPosition = "Elf_listPosition";

	/** Set Elf_listPosition	  */
	public void setElf_listPosition (int Elf_listPosition);

	/** Get Elf_listPosition	  */
	public int getElf_listPosition();

    /** Column name Elf_setDefault */
    public static final String COLUMNNAME_Elf_setDefault = "Elf_setDefault";

	/** Set Elf_setDefault	  */
	public void setElf_setDefault (boolean Elf_setDefault);

	/** Get Elf_setDefault	  */
	public boolean isElf_setDefault();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsMandatory */
    public static final String COLUMNNAME_IsMandatory = "IsMandatory";

	/** Set Mandatory.
	  * Data entry is required in this column
	  */
	public void setIsMandatory (boolean IsMandatory);

	/** Get Mandatory.
	  * Data entry is required in this column
	  */
	public boolean isMandatory();

    /** Column name IsParent */
    public static final String COLUMNNAME_IsParent = "IsParent";

	/** Set Parent link column.
	  * This column is a link to the parent table (e.g. header from lines) - incl. Association key columns
	  */
	public void setIsParent (boolean IsParent);

	/** Get Parent link column.
	  * This column is a link to the parent table (e.g. header from lines) - incl. Association key columns
	  */
	public boolean isParent();

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name Value */
    public static final String COLUMNNAME_Value = "Value";

	/** Set Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value);

	/** Get Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public String getValue();
}
