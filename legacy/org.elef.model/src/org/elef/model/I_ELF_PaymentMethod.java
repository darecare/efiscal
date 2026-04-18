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

/** Generated Interface for ELF_PaymentMethod
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ELF_PaymentMethod 
{

    /** TableName=ELF_PaymentMethod */
    public static final String Table_Name = "ELF_PaymentMethod";

    /** AD_Table_ID=1000011 */
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

    /** Column name ELF_PaymentMethod_FiscalName */
    public static final String COLUMNNAME_ELF_PaymentMethod_FiscalName = "ELF_PaymentMethod_FiscalName";

	/** Set ELF_PaymentMethod_FiscalName	  */
	public void setELF_PaymentMethod_FiscalName (String ELF_PaymentMethod_FiscalName);

	/** Get ELF_PaymentMethod_FiscalName	  */
	public String getELF_PaymentMethod_FiscalName();

    /** Column name ELF_PaymentMethod_FiscalValue */
    public static final String COLUMNNAME_ELF_PaymentMethod_FiscalValue = "ELF_PaymentMethod_FiscalValue";

	/** Set ELF_PaymentMethod_FiscalValue	  */
	public void setELF_PaymentMethod_FiscalValue (int ELF_PaymentMethod_FiscalValue);

	/** Get ELF_PaymentMethod_FiscalValue	  */
	public int getELF_PaymentMethod_FiscalValue();

    /** Column name ELF_PaymentMethod_ID */
    public static final String COLUMNNAME_ELF_PaymentMethod_ID = "ELF_PaymentMethod_ID";

	/** Set ELF_PaymentMethod	  */
	public void setELF_PaymentMethod_ID (int ELF_PaymentMethod_ID);

	/** Get ELF_PaymentMethod	  */
	public int getELF_PaymentMethod_ID();

    /** Column name ELF_PaymentMethod_UU */
    public static final String COLUMNNAME_ELF_PaymentMethod_UU = "ELF_PaymentMethod_UU";

	/** Set ELF_PaymentMethod_UU	  */
	public void setELF_PaymentMethod_UU (String ELF_PaymentMethod_UU);

	/** Get ELF_PaymentMethod_UU	  */
	public String getELF_PaymentMethod_UU();

    /** Column name Elf_ApiSystem */
    public static final String COLUMNNAME_Elf_ApiSystem = "Elf_ApiSystem";

	/** Set Elf_ApiSystem	  */
	public void setElf_ApiSystem (String Elf_ApiSystem);

	/** Get Elf_ApiSystem	  */
	public String getElf_ApiSystem();

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
