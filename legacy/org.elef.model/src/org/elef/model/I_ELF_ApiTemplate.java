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

/** Generated Interface for ELF_ApiTemplate
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ELF_ApiTemplate 
{

    /** TableName=ELF_ApiTemplate */
    public static final String Table_Name = "ELF_ApiTemplate";

    /** AD_Table_ID=1000005 */
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

    /** Column name ELF_ApiTemplate_ID */
    public static final String COLUMNNAME_ELF_ApiTemplate_ID = "ELF_ApiTemplate_ID";

	/** Set API Templates	  */
	public void setELF_ApiTemplate_ID (int ELF_ApiTemplate_ID);

	/** Get API Templates	  */
	public int getELF_ApiTemplate_ID();

    /** Column name ELF_ApiTemplate_UU */
    public static final String COLUMNNAME_ELF_ApiTemplate_UU = "ELF_ApiTemplate_UU";

	/** Set ELF_ApiTemplate_UU	  */
	public void setELF_ApiTemplate_UU (String ELF_ApiTemplate_UU);

	/** Get ELF_ApiTemplate_UU	  */
	public String getELF_ApiTemplate_UU();

    /** Column name Elf_ApiConn_ID */
    public static final String COLUMNNAME_Elf_ApiConn_ID = "Elf_ApiConn_ID";

	/** Set API Configuration	  */
	public void setElf_ApiConn_ID (int Elf_ApiConn_ID);

	/** Get API Configuration	  */
	public int getElf_ApiConn_ID();

	public I_ELF_ApiConn getElf_ApiConn() throws RuntimeException;

    /** Column name Elf_ApiContentType */
    public static final String COLUMNNAME_Elf_ApiContentType = "Elf_ApiContentType";

	/** Set Elf_ApiContentType	  */
	public void setElf_ApiContentType (String Elf_ApiContentType);

	/** Get Elf_ApiContentType	  */
	public String getElf_ApiContentType();

    /** Column name Elf_ApiRequestType */
    public static final String COLUMNNAME_Elf_ApiRequestType = "Elf_ApiRequestType";

	/** Set Elf_ApiRequestType	  */
	public void setElf_ApiRequestType (String Elf_ApiRequestType);

	/** Get Elf_ApiRequestType	  */
	public String getElf_ApiRequestType();

    /** Column name Elf_UrlExtend */
    public static final String COLUMNNAME_Elf_UrlExtend = "Elf_UrlExtend";

	/** Set Elf_UrlExtend	  */
	public void setElf_UrlExtend (String Elf_UrlExtend);

	/** Get Elf_UrlExtend	  */
	public String getElf_UrlExtend();

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
