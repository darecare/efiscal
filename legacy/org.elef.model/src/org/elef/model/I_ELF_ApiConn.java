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

/** Generated Interface for ELF_ApiConn
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ELF_ApiConn 
{

    /** TableName=ELF_ApiConn */
    public static final String Table_Name = "ELF_ApiConn";

    /** AD_Table_ID=1000004 */
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

    /** Column name Elf_ApiAuthType */
    public static final String COLUMNNAME_Elf_ApiAuthType = "Elf_ApiAuthType";

	/** Set Elf_ApiAuthType	  */
	public void setElf_ApiAuthType (String Elf_ApiAuthType);

	/** Get Elf_ApiAuthType	  */
	public String getElf_ApiAuthType();

    /** Column name Elf_ApiConn_ID */
    public static final String COLUMNNAME_Elf_ApiConn_ID = "Elf_ApiConn_ID";

	/** Set API Configuration	  */
	public void setElf_ApiConn_ID (int Elf_ApiConn_ID);

	/** Get API Configuration	  */
	public int getElf_ApiConn_ID();

    /** Column name Elf_ApiConn_UU */
    public static final String COLUMNNAME_Elf_ApiConn_UU = "Elf_ApiConn_UU";

	/** Set Elf_ApiConnn_UU	  */
	public void setElf_ApiConn_UU (String Elf_ApiConn_UU);

	/** Get Elf_ApiConnn_UU	  */
	public String getElf_ApiConn_UU();

    /** Column name Elf_ApiKey */
    public static final String COLUMNNAME_Elf_ApiKey = "Elf_ApiKey";

	/** Set Elf_ApiKey	  */
	public void setElf_ApiKey (String Elf_ApiKey);

	/** Get Elf_ApiKey	  */
	public String getElf_ApiKey();

    /** Column name Elf_ApiPassword */
    public static final String COLUMNNAME_Elf_ApiPassword = "Elf_ApiPassword";

	/** Set Elf_ApiPassword	  */
	public void setElf_ApiPassword (String Elf_ApiPassword);

	/** Get Elf_ApiPassword	  */
	public String getElf_ApiPassword();

    /** Column name Elf_ApiSecret */
    public static final String COLUMNNAME_Elf_ApiSecret = "Elf_ApiSecret";

	/** Set Elf_ApiSecret	  */
	public void setElf_ApiSecret (String Elf_ApiSecret);

	/** Get Elf_ApiSecret	  */
	public String getElf_ApiSecret();

    /** Column name Elf_ApiSystem */
    public static final String COLUMNNAME_Elf_ApiSystem = "Elf_ApiSystem";

	/** Set Elf_ApiSystem	  */
	public void setElf_ApiSystem (String Elf_ApiSystem);

	/** Get Elf_ApiSystem	  */
	public String getElf_ApiSystem();

    /** Column name Elf_ApiURL */
    public static final String COLUMNNAME_Elf_ApiURL = "Elf_ApiURL";

	/** Set Elf_ApiURL	  */
	public void setElf_ApiURL (String Elf_ApiURL);

	/** Get Elf_ApiURL	  */
	public String getElf_ApiURL();

    /** Column name Elf_ApiUserName */
    public static final String COLUMNNAME_Elf_ApiUserName = "Elf_ApiUserName";

	/** Set Elf_ApiUserName	  */
	public void setElf_ApiUserName (String Elf_ApiUserName);

	/** Get Elf_ApiUserName	  */
	public String getElf_ApiUserName();

    /** Column name Elf_CERT */
    public static final String COLUMNNAME_Elf_CERT = "Elf_CERT";

	/** Set Elf_CERT	  */
	public void setElf_CERT (String Elf_CERT);

	/** Get Elf_CERT	  */
	public String getElf_CERT();

    /** Column name Elf_ExtName */
    public static final String COLUMNNAME_Elf_ExtName = "Elf_ExtName";

	/** Set Elf_ExtName	  */
	public void setElf_ExtName (String Elf_ExtName);

	/** Get Elf_ExtName	  */
	public String getElf_ExtName();

    /** Column name Elf_PAC */
    public static final String COLUMNNAME_Elf_PAC = "Elf_PAC";

	/** Set Elf_PAC	  */
	public void setElf_PAC (String Elf_PAC);

	/** Get Elf_PAC	  */
	public String getElf_PAC();

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

    /** Column name eFiscal_esirno */
    public static final String COLUMNNAME_eFiscal_esirno = "eFiscal_esirno";

	/** Set eFiscal_esirno	  */
	public void seteFiscal_esirno (String eFiscal_esirno);

	/** Get eFiscal_esirno	  */
	public String geteFiscal_esirno();
}
