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

/** Generated Interface for ELF_FiscalBill
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ELF_FiscalBill 
{

    /** TableName=ELF_FiscalBill */
    public static final String Table_Name = "ELF_FiscalBill";

    /** AD_Table_ID=1000007 */
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

    /** Column name C_Invoice_ID */
    public static final String COLUMNNAME_C_Invoice_ID = "C_Invoice_ID";

	/** Set Invoice.
	  * Invoice Identifier
	  */
	public void setC_Invoice_ID (int C_Invoice_ID);

	/** Get Invoice.
	  * Invoice Identifier
	  */
	public int getC_Invoice_ID();

	public org.compiere.model.I_C_Invoice getC_Invoice() throws RuntimeException;

    /** Column name C_Order_ID */
    public static final String COLUMNNAME_C_Order_ID = "C_Order_ID";

	/** Set Order.
	  * Order
	  */
	public void setC_Order_ID (int C_Order_ID);

	/** Get Order.
	  * Order
	  */
	public int getC_Order_ID();

	public org.compiere.model.I_C_Order getC_Order() throws RuntimeException;

    /** Column name C_Payment_ID */
    public static final String COLUMNNAME_C_Payment_ID = "C_Payment_ID";

	/** Set Payment.
	  * Payment identifier
	  */
	public void setC_Payment_ID (int C_Payment_ID);

	/** Get Payment.
	  * Payment identifier
	  */
	public int getC_Payment_ID();

	public org.compiere.model.I_C_Payment getC_Payment() throws RuntimeException;

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

    /** Column name ELF_FIscalBill_ID */
    public static final String COLUMNNAME_ELF_FIscalBill_ID = "ELF_FIscalBill_ID";

	/** Set ELF_FIscalBill	  */
	public void setELF_FIscalBill_ID (int ELF_FIscalBill_ID);

	/** Get ELF_FIscalBill	  */
	public int getELF_FIscalBill_ID();

    /** Column name ELF_FIscalBill_UU */
    public static final String COLUMNNAME_ELF_FIscalBill_UU = "ELF_FIscalBill_UU";

	/** Set ELF_FIscalBill_UU	  */
	public void setELF_FIscalBill_UU (String ELF_FIscalBill_UU);

	/** Get ELF_FIscalBill_UU	  */
	public String getELF_FIscalBill_UU();

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

    /** Column name eFiscal_Name */
    public static final String COLUMNNAME_eFiscal_Name = "eFiscal_Name";

	/** Set eFiscal_Name	  */
	public void seteFiscal_Name (String eFiscal_Name);

	/** Get eFiscal_Name	  */
	public String geteFiscal_Name();

    /** Column name eFiscal_address */
    public static final String COLUMNNAME_eFiscal_address = "eFiscal_address";

	/** Set eFiscal_address	  */
	public void seteFiscal_address (String eFiscal_address);

	/** Get eFiscal_address	  */
	public String geteFiscal_address();

    /** Column name eFiscal_businessname */
    public static final String COLUMNNAME_eFiscal_businessname = "eFiscal_businessname";

	/** Set eFiscal_businessname	  */
	public void seteFiscal_businessname (String eFiscal_businessname);

	/** Get eFiscal_businessname	  */
	public String geteFiscal_businessname();

    /** Column name eFiscal_code */
    public static final String COLUMNNAME_eFiscal_code = "eFiscal_code";

	/** Set eFiscal_code	  */
	public void seteFiscal_code (String eFiscal_code);

	/** Get eFiscal_code	  */
	public String geteFiscal_code();

    /** Column name eFiscal_encryptedinternaldata */
    public static final String COLUMNNAME_eFiscal_encryptedinternaldata = "eFiscal_encryptedinternaldata";

	/** Set eFiscal_encryptedinternaldata	  */
	public void seteFiscal_encryptedinternaldata (String eFiscal_encryptedinternaldata);

	/** Get eFiscal_encryptedinternaldata	  */
	public String geteFiscal_encryptedinternaldata();

    /** Column name eFiscal_invoicecounter */
    public static final String COLUMNNAME_eFiscal_invoicecounter = "eFiscal_invoicecounter";

	/** Set eFiscal_invoicecounter	  */
	public void seteFiscal_invoicecounter (String eFiscal_invoicecounter);

	/** Get eFiscal_invoicecounter	  */
	public String geteFiscal_invoicecounter();

    /** Column name eFiscal_invoicecounterext */
    public static final String COLUMNNAME_eFiscal_invoicecounterext = "eFiscal_invoicecounterext";

	/** Set eFiscal_invoicecounterext	  */
	public void seteFiscal_invoicecounterext (String eFiscal_invoicecounterext);

	/** Get eFiscal_invoicecounterext	  */
	public String geteFiscal_invoicecounterext();

    /** Column name eFiscal_invoicetype */
    public static final String COLUMNNAME_eFiscal_invoicetype = "eFiscal_invoicetype";

	/** Set eFiscal_invoicetype	  */
	public void seteFiscal_invoicetype (int eFiscal_invoicetype);

	/** Get eFiscal_invoicetype	  */
	public int geteFiscal_invoicetype();

    /** Column name eFiscal_link */
    public static final String COLUMNNAME_eFiscal_link = "eFiscal_link";

	/** Set eFiscal_link	  */
	public void seteFiscal_link (String eFiscal_link);

	/** Get eFiscal_link	  */
	public String geteFiscal_link();

    /** Column name eFiscal_messages */
    public static final String COLUMNNAME_eFiscal_messages = "eFiscal_messages";

	/** Set eFiscal_messages	  */
	public void seteFiscal_messages (String eFiscal_messages);

	/** Get eFiscal_messages	  */
	public String geteFiscal_messages();

    /** Column name eFiscal_mrc */
    public static final String COLUMNNAME_eFiscal_mrc = "eFiscal_mrc";

	/** Set eFiscal_mrc	  */
	public void seteFiscal_mrc (String eFiscal_mrc);

	/** Get eFiscal_mrc	  */
	public String geteFiscal_mrc();

    /** Column name eFiscal_qr */
    public static final String COLUMNNAME_eFiscal_qr = "eFiscal_qr";

	/** Set eFiscal_qr	  */
	public void seteFiscal_qr (String eFiscal_qr);

	/** Get eFiscal_qr	  */
	public String geteFiscal_qr();

    /** Column name eFiscal_requestedby */
    public static final String COLUMNNAME_eFiscal_requestedby = "eFiscal_requestedby";

	/** Set eFiscal_requestedby	  */
	public void seteFiscal_requestedby (String eFiscal_requestedby);

	/** Get eFiscal_requestedby	  */
	public String geteFiscal_requestedby();

    /** Column name eFiscal_sdc_invoiceno */
    public static final String COLUMNNAME_eFiscal_sdc_invoiceno = "eFiscal_sdc_invoiceno";

	/** Set eFiscal_sdc_invoiceno	  */
	public void seteFiscal_sdc_invoiceno (String eFiscal_sdc_invoiceno);

	/** Get eFiscal_sdc_invoiceno	  */
	public String geteFiscal_sdc_invoiceno();

    /** Column name eFiscal_sdcdatetime */
    public static final String COLUMNNAME_eFiscal_sdcdatetime = "eFiscal_sdcdatetime";

	/** Set eFiscal_sdcdatetime	  */
	public void seteFiscal_sdcdatetime (String eFiscal_sdcdatetime);

	/** Get eFiscal_sdcdatetime	  */
	public String geteFiscal_sdcdatetime();

    /** Column name eFiscal_signature */
    public static final String COLUMNNAME_eFiscal_signature = "eFiscal_signature";

	/** Set eFiscal_signature	  */
	public void seteFiscal_signature (String eFiscal_signature);

	/** Get eFiscal_signature	  */
	public String geteFiscal_signature();

    /** Column name eFiscal_signedby */
    public static final String COLUMNNAME_eFiscal_signedby = "eFiscal_signedby";

	/** Set eFiscal_signedby	  */
	public void seteFiscal_signedby (String eFiscal_signedby);

	/** Get eFiscal_signedby	  */
	public String geteFiscal_signedby();

    /** Column name eFiscal_taxgrouprevision */
    public static final String COLUMNNAME_eFiscal_taxgrouprevision = "eFiscal_taxgrouprevision";

	/** Set eFiscal_taxgrouprevision	  */
	public void seteFiscal_taxgrouprevision (int eFiscal_taxgrouprevision);

	/** Get eFiscal_taxgrouprevision	  */
	public int geteFiscal_taxgrouprevision();

    /** Column name eFiscal_tin */
    public static final String COLUMNNAME_eFiscal_tin = "eFiscal_tin";

	/** Set eFiscal_tin	  */
	public void seteFiscal_tin (String eFiscal_tin);

	/** Get eFiscal_tin	  */
	public String geteFiscal_tin();

    /** Column name eFiscal_totalamount */
    public static final String COLUMNNAME_eFiscal_totalamount = "eFiscal_totalamount";

	/** Set eFiscal_totalamount	  */
	public void seteFiscal_totalamount (BigDecimal eFiscal_totalamount);

	/** Get eFiscal_totalamount	  */
	public BigDecimal geteFiscal_totalamount();

    /** Column name eFiscal_totalcounter */
    public static final String COLUMNNAME_eFiscal_totalcounter = "eFiscal_totalcounter";

	/** Set eFiscal_totalcounter	  */
	public void seteFiscal_totalcounter (BigDecimal eFiscal_totalcounter);

	/** Get eFiscal_totalcounter	  */
	public BigDecimal geteFiscal_totalcounter();

    /** Column name eFiscal_transactiontype */
    public static final String COLUMNNAME_eFiscal_transactiontype = "eFiscal_transactiontype";

	/** Set eFiscal_transactiontype	  */
	public void seteFiscal_transactiontype (int eFiscal_transactiontype);

	/** Get eFiscal_transactiontype	  */
	public int geteFiscal_transactiontype();

    /** Column name eFiscal_transactiontypecounter */
    public static final String COLUMNNAME_eFiscal_transactiontypecounter = "eFiscal_transactiontypecounter";

	/** Set eFiscal_transactiontypecounter	  */
	public void seteFiscal_transactiontypecounter (int eFiscal_transactiontypecounter);

	/** Get eFiscal_transactiontypecounter	  */
	public int geteFiscal_transactiontypecounter();
}
