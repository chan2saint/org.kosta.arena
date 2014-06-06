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
package org.kosta.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for I_Replenish
 *  @author iDempiere (generated) 
 *  @version Release 1.0c
 */
@SuppressWarnings("all")
public interface I_I_Replenish 
{

    /** TableName=I_Replenish */
    public static final String Table_Name = "I_Replenish";

    /** AD_Table_ID=1000003 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
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

    /** Column name I_ErrorMsg */
    public static final String COLUMNNAME_I_ErrorMsg = "I_ErrorMsg";

	/** Set Import Error Message.
	  * Messages generated from import process
	  */
	public void setI_ErrorMsg (String I_ErrorMsg);

	/** Get Import Error Message.
	  * Messages generated from import process
	  */
	public String getI_ErrorMsg();

    /** Column name I_IsImported */
    public static final String COLUMNNAME_I_IsImported = "I_IsImported";

	/** Set Imported.
	  * Has this import been processed
	  */
	public void setI_IsImported (boolean I_IsImported);

	/** Get Imported.
	  * Has this import been processed
	  */
	public boolean isI_IsImported();

    /** Column name I_Replenish_ID */
    public static final String COLUMNNAME_I_Replenish_ID = "I_Replenish_ID";

	/** Set I_Replenish_ID	  */
	public void setI_Replenish_ID (int I_Replenish_ID);

	/** Get I_Replenish_ID	  */
	public int getI_Replenish_ID();

    /** Column name I_Replenish_UU */
    public static final String COLUMNNAME_I_Replenish_UU = "I_Replenish_UU";

	/** Set I_Replenish_UU	  */
	public void setI_Replenish_UU (String I_Replenish_UU);

	/** Get I_Replenish_UU	  */
	public String getI_Replenish_UU();

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

    /** Column name Level_Max */
    public static final String COLUMNNAME_Level_Max = "Level_Max";

	/** Set Maximum Level.
	  * Maximum Inventory level for this product
	  */
	public void setLevel_Max (BigDecimal Level_Max);

	/** Get Maximum Level.
	  * Maximum Inventory level for this product
	  */
	public BigDecimal getLevel_Max();

    /** Column name Level_Min */
    public static final String COLUMNNAME_Level_Min = "Level_Min";

	/** Set Minimum Level.
	  * Minimum Inventory level for this product
	  */
	public void setLevel_Min (BigDecimal Level_Min);

	/** Get Minimum Level.
	  * Minimum Inventory level for this product
	  */
	public BigDecimal getLevel_Min();

    /** Column name M_Locator_ID */
    public static final String COLUMNNAME_M_Locator_ID = "M_Locator_ID";

	/** Set Locator.
	  * Warehouse Locator
	  */
	public void setM_Locator_ID (int M_Locator_ID);

	/** Get Locator.
	  * Warehouse Locator
	  */
	public int getM_Locator_ID();

	public I_M_Locator getM_Locator() throws RuntimeException;

    /** Column name M_Product_ID */
    public static final String COLUMNNAME_M_Product_ID = "M_Product_ID";

	/** Set Product.
	  * Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID);

	/** Get Product.
	  * Product, Service, Item
	  */
	public int getM_Product_ID();

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException;

    /** Column name M_Warehouse_ID */
    public static final String COLUMNNAME_M_Warehouse_ID = "M_Warehouse_ID";

	/** Set Warehouse.
	  * Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID);

	/** Get Warehouse.
	  * Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID();

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException;

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

    /** Column name Processing */
    public static final String COLUMNNAME_Processing = "Processing";

	/** Set Process Now	  */
	public void setProcessing (boolean Processing);

	/** Get Process Now	  */
	public boolean isProcessing();

    /** Column name ProductName */
    public static final String COLUMNNAME_ProductName = "ProductName";

	/** Set Product Name.
	  * Name of the Product
	  */
	public void setProductName (String ProductName);

	/** Get Product Name.
	  * Name of the Product
	  */
	public String getProductName();

    /** Column name ProductValue */
    public static final String COLUMNNAME_ProductValue = "ProductValue";

	/** Set Product Key.
	  * Key of the Product
	  */
	public void setProductValue (String ProductValue);

	/** Get Product Key.
	  * Key of the Product
	  */
	public String getProductValue();

    /** Column name QtyBatchSize */
    public static final String COLUMNNAME_QtyBatchSize = "QtyBatchSize";

	/** Set Qty Batch Size	  */
	public void setQtyBatchSize (BigDecimal QtyBatchSize);

	/** Get Qty Batch Size	  */
	public BigDecimal getQtyBatchSize();

    /** Column name ReplenishType */
    public static final String COLUMNNAME_ReplenishType = "ReplenishType";

	/** Set Replenish Type.
	  * Method for re-ordering a product
	  */
	public void setReplenishType (String ReplenishType);

	/** Get Replenish Type.
	  * Method for re-ordering a product
	  */
	public String getReplenishType();

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

    /** Column name WarehouseName */
    public static final String COLUMNNAME_WarehouseName = "WarehouseName";

	/** Set Warehouse.
	  * Warehouse Name
	  */
	public void setWarehouseName (String WarehouseName);

	/** Get Warehouse.
	  * Warehouse Name
	  */
	public String getWarehouseName();

    /** Column name WarehouseValue */
    public static final String COLUMNNAME_WarehouseValue = "WarehouseValue";

	/** Set Warehouse Key.
	  * Key of the Warehouse
	  */
	public void setWarehouseValue (String WarehouseValue);

	/** Get Warehouse Key.
	  * Key of the Warehouse
	  */
	public String getWarehouseValue();
}
