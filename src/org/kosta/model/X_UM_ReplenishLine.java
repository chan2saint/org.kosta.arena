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
package org.kosta.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for UM_ReplenishLine
 *  @author iDempiere (generated) 
 *  @version Release 1.0c - $Id$ */
public class X_UM_ReplenishLine extends PO implements I_UM_ReplenishLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20140409L;

    /** Standard Constructor */
    public X_UM_ReplenishLine (Properties ctx, int UM_ReplenishLine_ID, String trxName)
    {
      super (ctx, UM_ReplenishLine_ID, trxName);
      /** if (UM_ReplenishLine_ID == 0)
        {
        } */
    }

    /** Load Constructor */
    public X_UM_ReplenishLine (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_UM_ReplenishLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_UOM getC_UOM() throws RuntimeException
    {
		return (org.compiere.model.I_C_UOM)MTable.get(getCtx(), org.compiere.model.I_C_UOM.Table_Name)
			.getPO(getC_UOM_ID(), get_TrxName());	}

	/** Set UOM.
		@param C_UOM_ID 
		Unit of Measure
	  */
	public void setC_UOM_ID (int C_UOM_ID)
	{
		if (C_UOM_ID < 1) 
			set_Value (COLUMNNAME_C_UOM_ID, null);
		else 
			set_Value (COLUMNNAME_C_UOM_ID, Integer.valueOf(C_UOM_ID));
	}

	/** Get UOM.
		@return Unit of Measure
	  */
	public int getC_UOM_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_UOM_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Maximum Level.
		@param Level_Max 
		Maximum Inventory level for this product
	  */
	public void setLevel_Max (BigDecimal Level_Max)
	{
		set_Value (COLUMNNAME_Level_Max, Level_Max);
	}

	/** Get Maximum Level.
		@return Maximum Inventory level for this product
	  */
	public BigDecimal getLevel_Max () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Level_Max);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Minimum Level.
		@param Level_Min 
		Minimum Inventory level for this product
	  */
	public void setLevel_Min (BigDecimal Level_Min)
	{
		set_Value (COLUMNNAME_Level_Min, Level_Min);
	}

	/** Get Minimum Level.
		@return Minimum Inventory level for this product
	  */
	public BigDecimal getLevel_Min () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Level_Min);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Line.
		@param LineNo 
		Line No
	  */
	public void setLineNo (int LineNo)
	{
		set_Value (COLUMNNAME_LineNo, Integer.valueOf(LineNo));
	}

	/** Get Line.
		@return Line No
	  */
	public int getLineNo () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LineNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_M_AttributeSetInstance getM_AttributeSetInstance() throws RuntimeException
    {
		return (I_M_AttributeSetInstance)MTable.get(getCtx(), I_M_AttributeSetInstance.Table_Name)
			.getPO(getM_AttributeSetInstance_ID(), get_TrxName());	}

	/** Set Attribute Set Instance.
		@param M_AttributeSetInstance_ID 
		Product Attribute Set Instance
	  */
	public void setM_AttributeSetInstance_ID (int M_AttributeSetInstance_ID)
	{
		if (M_AttributeSetInstance_ID < 0) 
			set_ValueNoCheck (COLUMNNAME_M_AttributeSetInstance_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_AttributeSetInstance_ID, Integer.valueOf(M_AttributeSetInstance_ID));
	}

	/** Get Attribute Set Instance.
		@return Product Attribute Set Instance
	  */
	public int getM_AttributeSetInstance_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_AttributeSetInstance_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
    {
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_Name)
			.getPO(getM_Product_ID(), get_TrxName());	}

	/** Set Product.
		@param M_Product_ID 
		Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Delivered Quantity.
		@param QtyDelivered 
		Delivered Quantity
	  */
	public void setQtyDelivered (BigDecimal QtyDelivered)
	{
		set_ValueNoCheck (COLUMNNAME_QtyDelivered, QtyDelivered);
	}

	/** Get Delivered Quantity.
		@return Delivered Quantity
	  */
	public BigDecimal getQtyDelivered () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyDelivered);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Quantity.
		@param QtyEntered 
		The Quantity Entered is based on the selected UoM
	  */
	public void setQtyEntered (BigDecimal QtyEntered)
	{
		set_ValueNoCheck (COLUMNNAME_QtyEntered, QtyEntered);
	}

	/** Get Quantity.
		@return The Quantity Entered is based on the selected UoM
	  */
	public BigDecimal getQtyEntered () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyEntered);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set On Hand Quantity.
		@param QtyOnHand 
		On Hand Quantity
	  */
	public void setQtyOnHand (BigDecimal QtyOnHand)
	{
		set_ValueNoCheck (COLUMNNAME_QtyOnHand, QtyOnHand);
	}

	/** Get On Hand Quantity.
		@return On Hand Quantity
	  */
	public BigDecimal getQtyOnHand () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyOnHand);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Ordered Quantity.
		@param QtyOrdered 
		Ordered Quantity
	  */
	public void setQtyOrdered (BigDecimal QtyOrdered)
	{
		set_ValueNoCheck (COLUMNNAME_QtyOrdered, QtyOrdered);
	}

	/** Get Ordered Quantity.
		@return Ordered Quantity
	  */
	public BigDecimal getQtyOrdered () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyOrdered);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set QtyPurchased.
		@param QtyPurchased QtyPurchased	  */
	public void setQtyPurchased (BigDecimal QtyPurchased)
	{
		set_Value (COLUMNNAME_QtyPurchased, QtyPurchased);
	}

	/** Get QtyPurchased.
		@return QtyPurchased	  */
	public BigDecimal getQtyPurchased () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyPurchased);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_UM_Replenish getUM_Replenish() throws RuntimeException
    {
		return (I_UM_Replenish)MTable.get(getCtx(), I_UM_Replenish.Table_Name)
			.getPO(getUM_Replenish_ID(), get_TrxName());	}

	/** Set UM_Replenish_ID.
		@param UM_Replenish_ID UM_Replenish_ID	  */
	public void setUM_Replenish_ID (int UM_Replenish_ID)
	{
		if (UM_Replenish_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_UM_Replenish_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_UM_Replenish_ID, Integer.valueOf(UM_Replenish_ID));
	}

	/** Get UM_Replenish_ID.
		@return UM_Replenish_ID	  */
	public int getUM_Replenish_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_UM_Replenish_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set UM_ReplenishLine_ID.
		@param UM_ReplenishLine_ID UM_ReplenishLine_ID	  */
	public void setUM_ReplenishLine_ID (int UM_ReplenishLine_ID)
	{
		if (UM_ReplenishLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_UM_ReplenishLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_UM_ReplenishLine_ID, Integer.valueOf(UM_ReplenishLine_ID));
	}

	/** Get UM_ReplenishLine_ID.
		@return UM_ReplenishLine_ID	  */
	public int getUM_ReplenishLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_UM_ReplenishLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set UM_ReplenishLine_UU.
		@param UM_ReplenishLine_UU UM_ReplenishLine_UU	  */
	public void setUM_ReplenishLine_UU (String UM_ReplenishLine_UU)
	{
		set_ValueNoCheck (COLUMNNAME_UM_ReplenishLine_UU, UM_ReplenishLine_UU);
	}

	/** Get UM_ReplenishLine_UU.
		@return UM_ReplenishLine_UU	  */
	public String getUM_ReplenishLine_UU () 
	{
		return (String)get_Value(COLUMNNAME_UM_ReplenishLine_UU);
	}
}