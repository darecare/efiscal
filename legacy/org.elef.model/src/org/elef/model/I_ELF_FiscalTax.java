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

/** Generated Interface for ELF_FiscalTax
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ELF_FiscalTax 
{

    /** TableName=ELF_FiscalTax */
    public static final String Table_Name = "ELF_FiscalTax";

    /** AD_Table_ID=1000008 */
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

    /** Column name Amount */
    public static final String COLUMNNAME_Amount = "Amount";

	/** Set Amount.
	  * Amount in a defined currency
	  */
	public void setAmount (BigDecimal Amount);

	/** Get Amount.
	  * Amount in a defined currency
	  */
	public BigDecimal getAmount();

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

    /** Column name ELF_FiscalBill_ID */
    public static final String COLUMNNAME_ELF_FiscalBill_ID = "ELF_FiscalBill_ID";

	/** Set ELF_FiscalBill	  */
	public void setELF_FiscalBill_ID (int ELF_FiscalBill_ID);

	/** Get ELF_FiscalBill	  */
	public int getELF_FiscalBill_ID();

	public I_ELF_FiscalBill getELF_FiscalBill() throws RuntimeException;

    /** Column name ELF_FiscalTax_ID */
    public static final String COLUMNNAME_ELF_FiscalTax_ID = "ELF_FiscalTax_ID";

	/** Set ELF_FiscalTax	  */
	public void setELF_FiscalTax_ID (int ELF_FiscalTax_ID);

	/** Get ELF_FiscalTax	  */
	public int getELF_FiscalTax_ID();

    /** Column name ELF_FiscalTax_UU */
    public static final String COLUMNNAME_ELF_FiscalTax_UU = "ELF_FiscalTax_UU";

	/** Set ELF_FiscalTax_UU	  */
	public void setELF_FiscalTax_UU (String ELF_FiscalTax_UU);

	/** Get ELF_FiscalTax_UU	  */
	public String getELF_FiscalTax_UU();

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

    /** Column name Processed */
    public static final String COLUMNNAME_Processed = "Processed";

	/** Set Processed.
	  * The document has been processed
	  */
	public void setProcessed (boolean Processed);

	/** Get Processed.
	  * The document has been processed
	  */
	public boolean isProcessed();

    /** Column name ProcessedOn */
    public static final String COLUMNNAME_ProcessedOn = "ProcessedOn";

	/** Set Processed On.
	  * The date+time (expressed in decimal format) when the document has been processed
	  */
	public void setProcessedOn (BigDecimal ProcessedOn);

	/** Get Processed On.
	  * The date+time (expressed in decimal format) when the document has been processed
	  */
	public BigDecimal getProcessedOn();

    /** Column name Rate */
    public static final String COLUMNNAME_Rate = "Rate";

	/** Set Rate.
	  * Rate or Tax or Exchange
	  */
	public void setRate (BigDecimal Rate);

	/** Get Rate.
	  * Rate or Tax or Exchange
	  */
	public BigDecimal getRate();

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

    /** Column name eFiscal_CategoryName */
    public static final String COLUMNNAME_eFiscal_CategoryName = "eFiscal_CategoryName";

	/** Set eFiscal_CategoryName	  */
	public void seteFiscal_CategoryName (String eFiscal_CategoryName);

	/** Get eFiscal_CategoryName	  */
	public String geteFiscal_CategoryName();

    /** Column name eFiscal_CategoryType */
    public static final String COLUMNNAME_eFiscal_CategoryType = "eFiscal_CategoryType";

	/** Set eFiscal_CategoryType	  */
	public void seteFiscal_CategoryType (int eFiscal_CategoryType);

	/** Get eFiscal_CategoryType	  */
	public int geteFiscal_CategoryType();

    /** Column name eFiscal_TaxLabel */
    public static final String COLUMNNAME_eFiscal_TaxLabel = "eFiscal_TaxLabel";

	/** Set eFiscal_TaxLabel	  */
	public void seteFiscal_TaxLabel (String eFiscal_TaxLabel);

	/** Get eFiscal_TaxLabel	  */
	public String geteFiscal_TaxLabel();
}
