/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 * Contributor(s): Chris Farley - northernbrewer                              *
 *****************************************************************************/
package org.kosta.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrg;
import org.compiere.model.MProduct;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.model.X_T_Replenish;
import org.compiere.util.AdempiereSystemError;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.ReplenishInterface;
import org.eevolution.model.MDDOrder;
import org.eevolution.model.MDDOrderLine;
import org.kosta.model.MUMReplenish;
import org.kosta.model.X_UM_Replenish;
import org.kosta.model.X_UM_ReplenishLine;
import org.compiere.model.*;
import org.compiere.process.*;

/**
 *	Replenishment Report change to window
 *	
 *  generate replenish line per warehouse in kst replenish
 */
public class CreateFromReplenish extends SvrProcess
{
	/** Warehouse				*/
	private int		p_M_Warehouse_ID = 0;
	/**	Optional BPartner		*/
	private int		p_C_BPartner_ID = 0;
	/** Create (POO)Purchse Order or (POR)Requisition or (MMM)Movements */
	private String	p_ReplenishmentCreate = null;
	/** Document Type			*/
	private int		p_C_DocType_ID = 0;
	
	private Integer p_M_Product_Category_ID = 0;
	/** Return Info				*/
	private StringBuffer	m_info = new StringBuffer();
	
	private Integer kst_replenish_id = null;
	private X_UM_Replenish replenish = null;
	private Integer tempTable = null;
	private Integer pinstance = null;
	private Integer prosesTable = null;
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("M_Warehouse_ID"))
				p_M_Warehouse_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("ReplenishmentCreate"))
				p_ReplenishmentCreate = (String)para[i].getParameter();
			else if (name.equals("C_DocType_ID"))
				p_C_DocType_ID = para[i].getParameterAsInt();
			else if (name.equals("M_Product_Category_ID"))
				p_M_Product_Category_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
		kst_replenish_id = getRecord_ID();
		//tempTable = getTable_ID();
		pinstance = getAD_PInstance_ID();
		//prosesTable = getAD_User_ID();
		
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message 
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception // generate replenish line per warehouse in kst replenish
	{
		StringBuilder msglog = new StringBuilder("M_Warehouse_ID=").append(p_M_Warehouse_ID) 
				.append(", C_BPartner_ID=").append(p_C_BPartner_ID) 
				.append(" - ReplenishmentCreate=").append(p_ReplenishmentCreate)
				.append(", C_DocType_ID=").append(p_C_DocType_ID);
		if (log.isLoggable(Level.INFO)) log.info(msglog.toString());
		if (p_ReplenishmentCreate != null && p_C_DocType_ID == 0)
			throw new AdempiereUserError("@FillMandatory@ @C_DocType_ID@");
		
		replenish = new X_UM_Replenish(getCtx(), kst_replenish_id, get_TrxName());
		//p_M_Warehouse_ID = replenish.getM_Warehouse_ID();
		
		MWarehouse wh = MWarehouse.get(getCtx(), p_M_Warehouse_ID);
		if (wh.get_ID() == 0)  
			throw new AdempiereSystemError("@FillMandatory@ @M_Warehouse_ID@");
		//
		prepareTable();
		fillTable(wh);
		//
		if (p_ReplenishmentCreate == null)
			return "OK";
		//
		MDocType dt = MDocType.get(getCtx(), p_C_DocType_ID);
		if (!dt.getDocBaseType().equals(p_ReplenishmentCreate))
			throw new AdempiereSystemError("@C_DocType_ID@=" + dt.getName() + " <> " + p_ReplenishmentCreate);
		//
		if (p_ReplenishmentCreate.equals("POO"))
			createPO();
		else if (p_ReplenishmentCreate.equals("POR"))
			createRequisition();
		else if (p_ReplenishmentCreate.equals("MMM"))
			createMovements();
		else if (p_ReplenishmentCreate.equals("DOO"))
			createDO();
		return m_info.toString();
	}	//	doIt

	/**
	 * 	Prepare/Check Replenishment Table
	 */
	private void prepareTable()
	{
		//	Level_Max must be >= Level_Max
		StringBuilder sql = new StringBuilder("UPDATE M_Replenish")
							.append(" SET Level_Max = Level_Min ")
							.append("WHERE Level_Max < Level_Min");
		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Corrected Max_Level=" + no);
		
		//	Minimum Order should be 1
		sql = new StringBuilder("UPDATE M_Product_PO")
			.append(" SET Order_Min = 1 ")
			.append("WHERE Order_Min IS NULL OR Order_Min < 1");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Corrected Order Min=" + no);
		
		//	Pack should be 1
		sql = new StringBuilder("UPDATE M_Product_PO")
			.append(" SET Order_Pack = 1 ")
			.append("WHERE Order_Pack IS NULL OR Order_Pack < 1");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Corrected Order Pack=" + no);

		//	Set Current Vendor where only one vendor
		
		sql = new StringBuilder("UPDATE M_Product_PO p")
			.append(" SET IsCurrentVendor='Y' ")
			.append("WHERE IsCurrentVendor<>'Y'")
			.append(" AND EXISTS (SELECT pp.M_Product_ID FROM M_Product_PO pp ")
				.append("WHERE p.M_Product_ID=pp.M_Product_ID ")
				.append("GROUP BY pp.M_Product_ID ")
				.append("HAVING COUNT(*) = 1)");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Corrected CurrentVendor(Y)=" + no);

		//	More then one current vendor
		sql = new StringBuilder("UPDATE M_Product_PO p")
			.append(" SET IsCurrentVendor='N' ")
			.append("WHERE IsCurrentVendor = 'Y'")
			.append(" AND EXISTS (SELECT pp.M_Product_ID FROM M_Product_PO pp ")
				.append("WHERE p.M_Product_ID=pp.M_Product_ID AND pp.IsCurrentVendor='Y' ")
				.append("GROUP BY pp.M_Product_ID ")
				.append("HAVING COUNT(*) > 1)");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Corrected CurrentVendor(N)=" + no);
		
		//	Just to be sure
		sql = new StringBuilder("DELETE T_Replenish WHERE AD_PInstance_ID=").append(getAD_PInstance_ID());
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Delete Existing Temp=" + no);
	}	//	prepareTable

	/**
	 * 	Fill Table
	 * 	@param wh warehouse
	 */
	private void fillTable (MWarehouse wh) throws Exception
	{
		StringBuilder sql = new StringBuilder("INSERT INTO T_Replenish ");
						sql.append("(AD_PInstance_ID, M_Warehouse_ID, M_Product_ID, AD_Client_ID, AD_Org_ID,");
						sql.append(" ReplenishType, Level_Min, Level_Max,");
						sql.append(" C_BPartner_ID, Order_Min, Order_Pack, QtyToOrder, ReplenishmentCreate) ");
						sql.append("SELECT ").append(getAD_PInstance_ID()); 
						sql.append(", r.M_Warehouse_ID, r.M_Product_ID, r.AD_Client_ID, r.AD_Org_ID,");
						sql.append(" r.ReplenishType, r.Level_Min, r.Level_Max,");
						sql.append(" po.C_BPartner_ID, po.Order_Min, po.Order_Pack, 0, ");
						
		if (p_ReplenishmentCreate == null)
			sql.append("null");
		else
			sql.append("'").append(p_ReplenishmentCreate).append("'");
		sql.append(" FROM M_Replenish r");
			sql.append(" INNER JOIN M_Product_PO po ON (r.M_Product_ID=po.M_Product_ID) ");
			sql.append(" join m_product p on (r.m_product_id = p.m_product_id) ");
			sql.append("WHERE po.IsCurrentVendor='Y'");	//	Only Current Vendor
			sql.append(" AND r.ReplenishType<>'0'");
			sql.append(" AND po.IsActive='Y' AND r.IsActive='Y'");
			sql.append(" AND r.M_Warehouse_ID=").append(p_M_Warehouse_ID);
			sql.append(" and p.m_product_category_id = ").append(p_M_Product_Category_ID);
		if (p_C_BPartner_ID != 0)
			sql.append(" AND po.C_BPartner_ID=").append(p_C_BPartner_ID);
		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINEST)) log.finest(sql.toString());
		if (log.isLoggable(Level.FINE)) log.fine("Insert (1) #" + no);
		
		if (p_C_BPartner_ID == 0)
		{
			sql = new StringBuilder("INSERT INTO T_Replenish ");
				sql.append("(AD_PInstance_ID, M_Warehouse_ID, M_Product_ID, AD_Client_ID, AD_Org_ID,");
				sql.append(" ReplenishType, Level_Min, Level_Max,");
				sql.append(" C_BPartner_ID, Order_Min, Order_Pack, QtyToOrder, ReplenishmentCreate) ");
				sql.append("SELECT ").append(getAD_PInstance_ID());
				sql.append(", r.M_Warehouse_ID, r.M_Product_ID, r.AD_Client_ID, r.AD_Org_ID,");
				sql.append(" r.ReplenishType, r.Level_Min, r.Level_Max,");
			    sql.append(" 0, 1, 1, 0, ");
			if (p_ReplenishmentCreate == null)
				sql.append("null");
			else
				sql.append("'").append(p_ReplenishmentCreate).append("'");
				
				sql.append(" FROM M_Replenish r ");
				sql.append(" INNER JOIN M_Product mp ON mp.M_Product_ID= r.M_Product_ID");
				sql.append(" WHERE r.ReplenishType<>'0' AND r.IsActive='Y'");
				sql.append(" AND mp.M_Product_Category_ID= ").append(p_M_Product_Category_ID);
				sql.append(" AND r.M_Warehouse_ID=").append(p_M_Warehouse_ID);
				sql.append(" AND NOT EXISTS (SELECT * FROM T_Replenish t ");
					sql.append("WHERE r.M_Product_ID=t.M_Product_ID");
					sql.append(" AND AD_PInstance_ID=").append(getAD_PInstance_ID()).append(")");
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Insert (BP) #" + no);
		}
		sql = new StringBuilder("UPDATE T_Replenish t SET ");
			sql.append("QtyOnHand = (SELECT COALESCE(SUM(QtyOnHand),0) FROM M_StorageOnHand s, M_Locator l WHERE t.M_Product_ID=s.M_Product_ID");
			sql.append(" AND l.M_Locator_ID=s.M_Locator_ID AND l.M_Warehouse_ID=t.M_Warehouse_ID),");
			sql.append("QtyReserved = (SELECT COALESCE(SUM(Qty),0) FROM M_StorageReservation s WHERE t.M_Product_ID=s.M_Product_ID");
			sql.append(" AND t.M_Warehouse_ID=s.M_Warehouse_ID),");
			sql.append("QtyOrdered = (SELECT COALESCE(SUM(Qty),0) FROM M_StorageReservation s WHERE t.M_Product_ID=s.M_Product_ID");
			sql.append(" AND t.M_Warehouse_ID=s.M_Warehouse_ID)");
		if (p_C_DocType_ID != 0)
			sql.append(", C_DocType_ID=").append(p_C_DocType_ID);
		sql.append(" WHERE AD_PInstance_ID=").append(getAD_PInstance_ID());
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Update #" + no);

		//	Delete inactive products and replenishments
		sql = new StringBuilder("DELETE T_Replenish r ");
			sql.append("WHERE (EXISTS (SELECT * FROM M_Product p ");
				sql.append("WHERE p.M_Product_ID=r.M_Product_ID AND p.IsActive='N')");
			sql.append(" OR EXISTS (SELECT * FROM M_Replenish rr ");
				sql.append(" WHERE rr.M_Product_ID=r.M_Product_ID AND rr.IsActive='N'");
				sql.append(" AND rr.M_Warehouse_ID=").append(p_M_Warehouse_ID).append(" ))");
			sql.append(" AND AD_PInstance_ID=").append(getAD_PInstance_ID());
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Delete Inactive=" + no);
	 
		//	Ensure Data consistency
		sql = new StringBuilder("UPDATE T_Replenish SET QtyOnHand = 0 WHERE QtyOnHand IS NULL");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		sql = new StringBuilder("UPDATE T_Replenish SET QtyReserved = 0 WHERE QtyReserved IS NULL");
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		sql = new StringBuilder("UPDATE T_Replenish SET QtyOrdered = 0 WHERE QtyOrdered IS NULL");
		no = DB.executeUpdate(sql.toString(), get_TrxName());

		//	Set Minimum / Maximum Maintain Level
		//	X_M_Replenish.REPLENISHTYPE_ReorderBelowMinimumLevel
		sql = new StringBuilder("UPDATE T_Replenish");
			sql.append(" SET QtyToOrder = CASE WHEN QtyOnHand - QtyReserved + QtyOrdered <= Level_Min ");
			sql.append(" THEN Level_Max - QtyOnHand + QtyReserved - QtyOrdered ");
			sql.append(" ELSE 0 END ");
			sql.append("WHERE ReplenishType='1'"); 
			sql.append(" AND AD_PInstance_ID=").append(getAD_PInstance_ID());
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Update Type-1=" + no);
		//
		//	X_M_Replenish.REPLENISHTYPE_MaintainMaximumLevel
		sql = new StringBuilder("UPDATE T_Replenish");
			sql.append(" SET QtyToOrder = Level_Max - QtyOnHand + QtyReserved - QtyOrdered ");
			sql.append("WHERE ReplenishType='2'" );
			sql.append(" AND AD_PInstance_ID=").append(getAD_PInstance_ID());
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Update Type-2=" + no);
	

		//	Minimum Order Quantity
		sql = new StringBuilder("UPDATE T_Replenish");
			sql.append(" SET QtyToOrder = Order_Min ");
			sql.append("WHERE QtyToOrder < Order_Min");
			sql.append(" AND QtyToOrder > 0" );
			sql.append(" AND AD_PInstance_ID=").append(getAD_PInstance_ID());
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set MinOrderQty=" + no);

		//	Even dividable by Pack
		sql = new StringBuilder("UPDATE T_Replenish");
			sql.append(" SET QtyToOrder = QtyToOrder - MOD(QtyToOrder, Order_Pack) + Order_Pack ");
			sql.append("WHERE MOD(QtyToOrder, Order_Pack) <> 0");
			sql.append(" AND QtyToOrder > 0");
			sql.append(" AND AD_PInstance_ID=").append(getAD_PInstance_ID());
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set OrderPackQty=" + no);
		
		//	Source from other warehouse
		if (wh.getM_WarehouseSource_ID() != 0)
		{
			sql = new StringBuilder("UPDATE T_Replenish");
				sql.append(" SET M_WarehouseSource_ID=").append(wh.getM_WarehouseSource_ID()); 
				sql.append(" WHERE AD_PInstance_ID=").append(getAD_PInstance_ID());
			no = DB.executeUpdate(sql.toString(), get_TrxName());
			if (no != 0)
				if (log.isLoggable(Level.FINE)) log.fine("Set Source Warehouse=" + no);
		}
		//	Check Source Warehouse
		sql = new StringBuilder("UPDATE T_Replenish");
			sql.append(" SET M_WarehouseSource_ID = NULL "); 
			sql.append("WHERE M_Warehouse_ID=M_WarehouseSource_ID");
			sql.append(" AND AD_PInstance_ID=").append(getAD_PInstance_ID());
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set same Source Warehouse=" + no);
		
		//	Custom Replenishment
		String className = wh.getReplenishmentClass();
		if (className != null && className.length() > 0)
		{	
			//	Get Replenishment Class
			ReplenishInterface custom = null;
			try
			{
				Class<?> clazz = Class.forName(className);
				custom = (ReplenishInterface)clazz.newInstance();
			}
			catch (Exception e)
			{
				throw new AdempiereUserError("No custom Replenishment class "
						+ className + " - " + e.toString());
			}

			X_T_Replenish[] replenishs = getReplenish("ReplenishType='9'");
			for (int i = 0; i < replenishs.length; i++)
			{
				X_T_Replenish replenish = replenishs[i];
				if (replenish.getReplenishType().equals(X_T_Replenish.REPLENISHTYPE_Custom))
				{
					BigDecimal qto = null;
					try
					{
						qto = custom.getQtyToOrder(wh, replenish);
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, custom.toString(), e);
					}
					if (qto == null)
						qto = Env.ZERO;
					replenish.setQtyToOrder(qto);
					replenish.saveEx();
				}
			}
		}
		//	Delete rows where nothing to order
		sql = new StringBuilder("DELETE T_Replenish ");
			sql.append("WHERE QtyToOrder < 1");
		    sql.append(" AND AD_PInstance_ID=").append(getAD_PInstance_ID());
		no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no != 0)
			if (log.isLoggable(Level.FINE)) log.fine("Delete No QtyToOrder=" + no);
				
		createReplenisLines(); // call here
	}	//	fillTable
	
	/**
	 * 	Create Replenish Line 
	 *  add by ah
	 */
	private void createReplenisLines()
	{
		if ( !replenish.isCreateFrom() )
		{
			X_T_Replenish[] arrayReplenis = listReplenish(pinstance); // get replenish products count
			//Integer qtyOrdered = new Query(getCtx(), MUMReplenish.Table_Name, "", get_TrxName()).count();
			
			if ( arrayReplenis.length != 0){ // process replenish not found product
				for (int j = 0; j < arrayReplenis.length; j++) {
					Integer qtyEntered = 0;
					Integer qtyPurchased = 0;
					
					String sql = "select coalesce( sum(qtyentered), 0 ) as qtyentered, coalesce(sum(qtypurchased),0 ) as qtypurchased, m_product_id from um_replenish a " +
							"join UM_replenishLine b on a.um_replenish_id = b.um_replenish_id " +
							"where docstatus = 'CO' " +							
							" and m_product_id = ? " +
							" and a.m_warehouse_id = ? " +
							" and a.ad_client_id = ? " +
							" group by m_product_id";
					PreparedStatement pstmt = null;
					ResultSet rs = null; 
					
					try
					{
						pstmt = DB.prepareStatement (sql, null);
						pstmt.setInt (1, arrayReplenis[j].getM_Product_ID());
						pstmt.setInt (2, arrayReplenis[j].getM_Warehouse_ID());
						pstmt.setInt (3, arrayReplenis[j].getAD_Client_ID());
						
						rs = pstmt.executeQuery (); // validate stock in kst_replenish request ( complete )
						if (rs.next ()) {
							if (arrayReplenis[j].getM_Product_ID() == rs.getInt("m_product_id")){
								qtyEntered = rs.getInt("qtyEntered");
								qtyPurchased = rs.getInt("qtyPurchased");
							}
						}
					
						X_UM_ReplenishLine replenishLine = new X_UM_ReplenishLine(getCtx(), 0, get_TrxName());
						replenishLine.setUM_Replenish_ID(replenish.getUM_Replenish_ID());
						
						// set QtyOrder history						
						Integer qtyOrdered = qtyEntered - qtyPurchased;
						if ( qtyEntered == 0)
							replenishLine.setQtyOrdered(Env.ZERO);
						else replenishLine.setQtyOrdered(BigDecimal.valueOf(qtyOrdered));
						
						if (arrayReplenis[j].getQtyToOrder().intValue() <= arrayReplenis[j].getLevel_Max().intValue() ){
							if ( arrayReplenis[j].getQtyToOrder().signum() == 0)
								continue; // skip if order m_product_id with qty = 0
							
							BigDecimal result = arrayReplenis[j].getQtyToOrder().subtract(BigDecimal.valueOf( qtyOrdered));
														
							if ( result.signum() > 0 ){
								replenishLine.setQtyEntered(result);
							}
							else {
								replenishLine.setQtyEntered(Env.ZERO);
							}							
						}
						else replenishLine.setQtyEntered(Env.ZERO);
						
						replenishLine.setLineNo((j+1)*10);
						replenishLine.setQtyDelivered(Env.ZERO);
						replenishLine.setQtyPurchased(Env.ZERO);
						replenishLine.setLevel_Max(arrayReplenis[j].getLevel_Max());
						replenishLine.setLevel_Min(arrayReplenis[j].getLevel_Min());
						replenishLine.setQtyOnHand(arrayReplenis[j].getQtyOnHand());				
						replenishLine.setM_Product_ID(arrayReplenis[j].getM_Product_ID());
						
						X_M_Product product = new X_M_Product(getCtx(), arrayReplenis[j].getM_Product_ID(), get_TrxName());				
						replenishLine.setC_UOM_ID(product.getC_UOM_ID());
						
						replenishLine.saveEx();	 // create replenish line from m_replenish
					}
					catch (Exception e)
					{
						throw new AdempiereException("\nError di Replenishment Line");
					}
					finally
					{
						DB.close(rs, pstmt);
						rs = null;
						pstmt = null;
					}
				}	
			}
			else throw new AdempiereException("\nProcess not found product to create Replenishment Line");
			
			replenish.setCreateFrom("Y");
			replenish.saveEx();
		} // end if isCreateFrom
		else throw new AdempiereException("Create Line From already process");
	}
	
	/**
	 * 	Create Replenish Line 
	 * 	@param wh warehouse, m_product_category_id
	 *  add by aries
	 *  old method
	 */
	private void createReplenisLine() // method not use
	{
		if ( !replenish.isCreateFrom() )
		{		
			Integer qtyOrdereds = null;
			X_T_Replenish[] arrayReplenis = listReplenish(pinstance); // get replenish products count
			
			if ( arrayReplenis.length != 0){ // process replenish not found product
				//log.warning("Length "+arrayReplenis.length );
				for (int i = 0; i < arrayReplenis.length; i++) {	
					
					String sql = "select coalesce( sum(qtyordered), 0 ) as qtyordered, coalesce(sum(qtypurchased),0 ) as qtypurchased, m_product_id from um_replenish a " +
							"join UM_replenishLine b on a.um_replenish_id = b.um_replenish_id " +
							"where docstatus = 'CO' " +							
							" and m_product_id = ? " +
							" and a.m_warehouse_id = ? " +
							" and a.ad_client_id = ? " +
							" group by m_product_id";
					PreparedStatement pstmt = null;
					ResultSet rs = null; 
					
					try
					{
						pstmt = DB.prepareStatement (sql, null);
						pstmt.setInt (1, arrayReplenis[i].getM_Product_ID());
						pstmt.setInt (2, arrayReplenis[i].getM_Warehouse_ID());
						pstmt.setInt (3, arrayReplenis[i].getAD_Client_ID());
						
						rs = pstmt.executeQuery (); // validate stock in kst_replenish request ( complete )
						if (rs.next ()) {
							if (arrayReplenis[i].getM_Product_ID() == rs.getInt("m_product_id"))
								qtyOrdereds = rs.getInt("qtyordered");
						}
						else qtyOrdereds = 0;						
						
						X_UM_ReplenishLine replenishLine = new X_UM_ReplenishLine(getCtx(), 0, get_TrxName());
						replenishLine.setUM_Replenish_ID(replenish.getUM_Replenish_ID());
						
						replenishLine.setQtyDelivered(Env.ZERO);
						if ( qtyOrdereds == 0){ // checking qtyOrdered from um_replenish that complete
							replenishLine.setQtyEntered(arrayReplenis[i].getQtyToOrder());
							replenishLine.setQtyOrdered(arrayReplenis[i].getQtyToOrder());
						}
						else {
							BigDecimal qtyOrdered = BigDecimal.valueOf(qtyOrdereds);
							if ( qtyOrdered == null) qtyOrdered = Env.ZERO; // make sure
							
							BigDecimal result = arrayReplenis[i].getQtyToOrder().subtract(qtyOrdered); // selisih
							
							if ( result.signum() > 0){
								replenishLine.setQtyEntered(result);
							}
							else { 
								replenishLine.setQtyEntered(Env.ZERO);
							}
							
							//replenishLine.setQtyEntered(qtyOrdered.add(replenishLine.getQtyEntered()));
						}
						
						//replenishLine.setQtyEntered(BigDecimal.valueOf(qtyToOrdered));
						replenishLine.setLineNo((i+1)*10);
						replenishLine.setLevel_Max(arrayReplenis[i].getLevel_Max());
						replenishLine.setLevel_Min(arrayReplenis[i].getLevel_Min());
						replenishLine.setQtyOnHand(arrayReplenis[i].getQtyOnHand());
						replenishLine.setM_Product_ID(arrayReplenis[i].getM_Product_ID());
						//replenishLine.setM_Product_Category_ID(replenish.getM_Product_Category_ID());
						
						X_M_Product product = new X_M_Product(getCtx(), arrayReplenis[i].getM_Product_ID(), get_TrxName());				
						replenishLine.setC_UOM_ID(product.getC_UOM_ID());
						
						replenishLine.saveEx();	 // create replenish line from m_replenish						
					}
					catch (Exception e)
					{
						throw new AdempiereException("\nError di Replenishment Line");
					}
					finally
					{
						DB.close(rs, pstmt);
						rs = null;
						pstmt = null;
					}
				}
			}
			else throw new AdempiereException("\nProcess not found product to create Requestion Line");
			
			replenish.setCreateFrom("Y");
			replenish.saveEx();
		}	
		else throw new AdempiereException("Replenish Line Sudah Ada");
	}

	/**
	 * 	Create PO's
	 */
	private void createPO()
	{
		int noOrders = 0;
		StringBuilder info = new StringBuilder();
		//
		MOrder order = null;
		MWarehouse wh = null;
		X_T_Replenish[] replenishs = getReplenish("M_WarehouseSource_ID IS NULL");
		for (int i = 0; i < replenishs.length; i++)
		{
			X_T_Replenish replenish = replenishs[i];
			if (wh == null || wh.getM_Warehouse_ID() != replenish.getM_Warehouse_ID())
				wh = MWarehouse.get(getCtx(), replenish.getM_Warehouse_ID());
			//
			if (order == null 
				|| order.getC_BPartner_ID() != replenish.getC_BPartner_ID()
				|| order.getM_Warehouse_ID() != replenish.getM_Warehouse_ID())
			{
				order = new MOrder(getCtx(), 0, get_TrxName());
				order.setIsSOTrx(false);
				order.setC_DocTypeTarget_ID(p_C_DocType_ID);
				MBPartner bp = new MBPartner(getCtx(), replenish.getC_BPartner_ID(), get_TrxName());
				order.setBPartner(bp);
				order.setSalesRep_ID(getAD_User_ID());
				order.setDescription(Msg.getMsg(getCtx(), "Replenishment"));
				//	Set Org/WH
				order.setAD_Org_ID(wh.getAD_Org_ID());
				order.setM_Warehouse_ID(wh.getM_Warehouse_ID());
				if (!order.save())
					return;
				if (log.isLoggable(Level.FINE)) log.fine(order.toString());
				noOrders++;
				info.append(" - "); 
				info.append(order.getDocumentNo());
			}
			MOrderLine line = new MOrderLine (order);
			line.setM_Product_ID(replenish.getM_Product_ID());
			line.setQty(replenish.getQtyToOrder());
			line.setPrice();
			line.saveEx();
		}
		m_info = new StringBuffer("#").append(noOrders).append(info.toString());
		if (log.isLoggable(Level.INFO)) log.info(m_info.toString());
	}	//	createPO
	
	/**
	 * 	Create Requisition
	 */
	private void createRequisition()
	{
		int noReqs = 0;
		StringBuilder info = new StringBuilder();
		//
		MRequisition requisition = null;
		MWarehouse wh = null;
		X_T_Replenish[] replenishs = getReplenish("M_WarehouseSource_ID IS NULL");
		for (int i = 0; i < replenishs.length; i++)
		{
			X_T_Replenish replenish = replenishs[i];
			if (wh == null || wh.getM_Warehouse_ID() != replenish.getM_Warehouse_ID())
				wh = MWarehouse.get(getCtx(), replenish.getM_Warehouse_ID());
			//
			if (requisition == null
				|| requisition.getM_Warehouse_ID() != replenish.getM_Warehouse_ID())
			{
				requisition = new MRequisition (getCtx(), 0, get_TrxName());
				requisition.setAD_User_ID (getAD_User_ID());
				requisition.setC_DocType_ID(p_C_DocType_ID);
				requisition.setDescription(Msg.getMsg(getCtx(), "Replenishment"));
				//	Set Org/WH
				requisition.setAD_Org_ID(wh.getAD_Org_ID());
				requisition.setM_Warehouse_ID(wh.getM_Warehouse_ID());
				if (!requisition.save())
					return;
				if (log.isLoggable(Level.FINE)) log.fine(requisition.toString());
				noReqs++;
				info.append(" - "); 
				info.append(requisition.getDocumentNo());
			}
			//
			MRequisitionLine line = new MRequisitionLine(requisition);
			line.setM_Product_ID(replenish.getM_Product_ID());
			line.setC_BPartner_ID(replenish.getC_BPartner_ID());
			line.setQty(replenish.getQtyToOrder());
			line.setPrice();
			line.saveEx();
		}
		m_info = new StringBuffer("#").append(noReqs).append(info.toString());
		if (log.isLoggable(Level.INFO)) log.info(m_info.toString());
	}	//	createRequisition

	/**
	 * 	Create Inventory Movements
	 */
	private void createMovements()
	{
		int noMoves = 0;
		StringBuilder info = new StringBuilder();
		//
		MClient client = null;
		MMovement move = null;
		int M_Warehouse_ID = 0;
		int M_WarehouseSource_ID = 0;
		MWarehouse whSource = null;
		MWarehouse wh = null;
		X_T_Replenish[] replenishs = getReplenish("M_WarehouseSource_ID IS NOT NULL");
		for (int i = 0; i < replenishs.length; i++)
		{
			X_T_Replenish replenish = replenishs[i];
			if (whSource == null || whSource.getM_WarehouseSource_ID() != replenish.getM_WarehouseSource_ID())
				whSource = MWarehouse.get(getCtx(), replenish.getM_WarehouseSource_ID());
			if (wh == null || wh.getM_Warehouse_ID() != replenish.getM_Warehouse_ID())
				wh = MWarehouse.get(getCtx(), replenish.getM_Warehouse_ID());
			if (client == null || client.getAD_Client_ID() != whSource.getAD_Client_ID())
				client = MClient.get(getCtx(), whSource.getAD_Client_ID());
			//
			if (move == null
				|| M_WarehouseSource_ID != replenish.getM_WarehouseSource_ID()
				|| M_Warehouse_ID != replenish.getM_Warehouse_ID())
			{
				M_WarehouseSource_ID = replenish.getM_WarehouseSource_ID();
				M_Warehouse_ID = replenish.getM_Warehouse_ID();
				
				move = new MMovement (getCtx(), 0, get_TrxName());
				move.setC_DocType_ID(p_C_DocType_ID);
				move.setDescription(Msg.getMsg(getCtx(), "Replenishment")
					+ ": " + whSource.getName() + "->" + wh.getName());
				//	Set Org
				move.setAD_Org_ID(whSource.getAD_Org_ID());
				if (!move.save())
					return;
				if (log.isLoggable(Level.FINE)) log.fine(move.toString());
				noMoves++;
				info.append(" - ").append(move.getDocumentNo());
			}
			//	To
			int M_LocatorTo_ID = wh.getDefaultLocator().getM_Locator_ID();
			//	From: Look-up Storage
			MProduct product = MProduct.get(getCtx(), replenish.getM_Product_ID());
			String MMPolicy = product.getMMPolicy();
			MStorageOnHand[] storages = MStorageOnHand.getWarehouse(getCtx(),
				whSource.getM_Warehouse_ID(), replenish.getM_Product_ID(), 0, null, 
				MClient.MMPOLICY_FiFo.equals(MMPolicy), false, 0, get_TrxName());
			//
			BigDecimal target = replenish.getQtyToOrder();
			for (int j = 0; j < storages.length; j++)
			{
				MStorageOnHand storage = storages[j];
				if (storage.getQtyOnHand().signum() <= 0)
					continue;
				BigDecimal moveQty = target;
				if (storage.getQtyOnHand().compareTo(moveQty) < 0)
					moveQty = storage.getQtyOnHand();
				//
				MMovementLine line = new MMovementLine(move);
				line.setM_Product_ID(replenish.getM_Product_ID());
				line.setMovementQty(moveQty);
				if (replenish.getQtyToOrder().compareTo(moveQty) != 0)
					line.setDescription("Total: " + replenish.getQtyToOrder());
				line.setM_Locator_ID(storage.getM_Locator_ID());		//	from
				line.setM_AttributeSetInstance_ID(storage.getM_AttributeSetInstance_ID());
				line.setM_LocatorTo_ID(M_LocatorTo_ID);					//	to
				line.setM_AttributeSetInstanceTo_ID(storage.getM_AttributeSetInstance_ID());
				line.saveEx();
				//
				target = target.subtract(moveQty);
				if (target.signum() == 0)
					break;
			}
		}
		if (replenishs.length == 0)
		{
			m_info = new StringBuffer("No Source Warehouse");
			log.warning(m_info.toString());
		}
		else
		{
			m_info = new StringBuffer("#") .append(noMoves).append(info);
			if (log.isLoggable(Level.INFO)) log.info(m_info.toString());
		}
	}	//	Create Inventory Movements
	
	/**
	 * 	Create Distribution Order
	 */
	private void createDO() throws Exception
	{
		int noMoves = 0;
		StringBuilder info = new StringBuilder();
		//
		MClient client = null;
		MDDOrder order = null;
		int M_Warehouse_ID = 0;
		int M_WarehouseSource_ID = 0;
		MWarehouse whSource = null;
		MWarehouse wh = null;
		X_T_Replenish[] replenishs = getReplenishDO("M_WarehouseSource_ID IS NOT NULL");
		for (X_T_Replenish replenish:replenishs)
		{
			if (whSource == null || whSource.getM_WarehouseSource_ID() != replenish.getM_WarehouseSource_ID())
				whSource = MWarehouse.get(getCtx(), replenish.getM_WarehouseSource_ID());
			if (wh == null || wh.getM_Warehouse_ID() != replenish.getM_Warehouse_ID())
				wh = MWarehouse.get(getCtx(), replenish.getM_Warehouse_ID());
			if (client == null || client.getAD_Client_ID() != whSource.getAD_Client_ID())
				client = MClient.get(getCtx(), whSource.getAD_Client_ID());
			//
			if (order == null
				|| M_WarehouseSource_ID != replenish.getM_WarehouseSource_ID()
				|| M_Warehouse_ID != replenish.getM_Warehouse_ID())
			{
				M_WarehouseSource_ID = replenish.getM_WarehouseSource_ID();
				M_Warehouse_ID = replenish.getM_Warehouse_ID();
				
				order = new MDDOrder (getCtx(), 0, get_TrxName());				
				order.setC_DocType_ID(p_C_DocType_ID);
				StringBuffer msgsd = new StringBuffer(Msg.getMsg(getCtx(), "Replenishment"))
						.append(": ").append(whSource.getName()).append("->").append(wh.getName());
				order.setDescription(msgsd.toString());
				//	Set Org
				order.setAD_Org_ID(whSource.getAD_Org_ID());
				// Set Org Trx
				MOrg orgTrx = MOrg.get(getCtx(), wh.getAD_Org_ID());
				order.setAD_OrgTrx_ID(orgTrx.getAD_Org_ID());
				int C_BPartner_ID = orgTrx.getLinkedC_BPartner_ID(get_TrxName()); 
				if (C_BPartner_ID==0)
					throw new AdempiereUserError(Msg.translate(getCtx(), "C_BPartner_ID")+ " @FillMandatory@ ");
				MBPartner bp = new MBPartner(getCtx(),C_BPartner_ID,get_TrxName());
				// Set BPartner Link to Org
				order.setBPartner(bp);
				order.setDateOrdered(new Timestamp(System.currentTimeMillis()));
				//order.setDatePromised(DatePromised);
				order.setDeliveryRule(MDDOrder.DELIVERYRULE_Availability);
				order.setDeliveryViaRule(MDDOrder.DELIVERYVIARULE_Delivery);
				order.setPriorityRule(MDDOrder.PRIORITYRULE_Medium);
				order.setIsInDispute(false);
				order.setIsApproved(false);
				order.setIsDropShip(false);
				order.setIsDelivered(false);
				order.setIsInTransit(false);
				order.setIsPrinted(false);
				order.setIsSelected(false);
				order.setIsSOTrx(false);
				// Warehouse in Transit
				MWarehouse[] whsInTransit  = MWarehouse.getForOrg(getCtx(), whSource.getAD_Org_ID());
				for (MWarehouse whInTransit:whsInTransit)
				{
					if(whInTransit.isInTransit())	
					order.setM_Warehouse_ID(whInTransit.getM_Warehouse_ID());
				}
				if (order.getM_Warehouse_ID()==0)
					throw new AdempiereUserError("Warehouse inTransit is @FillMandatory@ ");
				
				if (!order.save())
					return;
				if (log.isLoggable(Level.FINE)) log.fine(order.toString());
				noMoves++;
				info.append(" - ").append(order.getDocumentNo());
			}
		
			//	To
			int M_LocatorTo_ID = wh.getDefaultLocator().getM_Locator_ID();
			int M_Locator_ID = whSource.getDefaultLocator().getM_Locator_ID();
			if(M_LocatorTo_ID == 0 || M_Locator_ID==0)
			throw new AdempiereUserError(Msg.translate(getCtx(), "M_Locator_ID")+" @FillMandatory@ ");
			
			//	From: Look-up Storage
			/*MProduct product = MProduct.get(getCtx(), replenish.getM_Product_ID());
			MProductCategory pc = MProductCategory.get(getCtx(), product.getM_Product_Category_ID());
			String MMPolicy = pc.getMMPolicy();
			if (MMPolicy == null || MMPolicy.length() == 0)
				MMPolicy = client.getMMPolicy();
			//
			MStorage[] storages = MStorage.getWarehouse(getCtx(), 
				whSource.getM_Warehouse_ID(), replenish.getM_Product_ID(), 0, 0,
				true, null, 
				MClient.MMPOLICY_FiFo.equals(MMPolicy), get_TrxName());
			
			
			BigDecimal target = replenish.getQtyToOrder();
			for (int j = 0; j < storages.length; j++)
			{
				MStorage storage = storages[j];
				if (storage.getQtyOnHand().signum() <= 0)
					continue;
				BigDecimal moveQty = target;
				if (storage.getQtyOnHand().compareTo(moveQty) < 0)
					moveQty = storage.getQtyOnHand();
				//
				MDDOrderLine line = new MDDOrderLine(order);
				line.setM_Product_ID(replenish.getM_Product_ID());
				line.setQtyEntered(moveQty);
				if (replenish.getQtyToOrder().compareTo(moveQty) != 0)
					line.setDescription("Total: " + replenish.getQtyToOrder());
				line.setM_Locator_ID(storage.getM_Locator_ID());		//	from
				line.setM_AttributeSetInstance_ID(storage.getM_AttributeSetInstance_ID());
				line.setM_LocatorTo_ID(M_LocatorTo_ID);					//	to
				line.setM_AttributeSetInstanceTo_ID(storage.getM_AttributeSetInstance_ID());
				line.setIsInvoiced(false);
				line.saveEx();
				//
				target = target.subtract(moveQty);
				if (target.signum() == 0)
					break;
			}*/
			
			MDDOrderLine line = new MDDOrderLine(order);
			line.setM_Product_ID(replenish.getM_Product_ID());
			line.setQty(replenish.getQtyToOrder());
			if (replenish.getQtyToOrder().compareTo(replenish.getQtyToOrder()) != 0)
				line.setDescription("Total: " + replenish.getQtyToOrder());
			line.setM_Locator_ID(M_Locator_ID);		//	from
			line.setM_AttributeSetInstance_ID(0);
			line.setM_LocatorTo_ID(M_LocatorTo_ID);					//	to
			line.setM_AttributeSetInstanceTo_ID(0);
			line.setIsInvoiced(false);
			line.saveEx();
			
		}
		if (replenishs.length == 0)
		{
			m_info = new StringBuffer("No Source Warehouse");
			log.warning(m_info.toString());
		}
		else
		{
			m_info = new StringBuffer("#").append(noMoves).append(info);
			if (log.isLoggable(Level.INFO)) log.info(m_info.toString());
		}
	}	//	create Distribution Order

	/**
	 * 	Get Replenish Records
	 *	@return replenish
	 */
	private X_T_Replenish[] getReplenish (String where)
	{
		StringBuilder sql = new StringBuilder("SELECT * FROM T_Replenish ");
						sql.append("WHERE AD_PInstance_ID=? AND C_BPartner_ID > 0 ");
		if (where != null && where.length() > 0)
			sql.append(" AND ").append(where);
		sql.append(" ORDER BY M_Warehouse_ID, M_WarehouseSource_ID, C_BPartner_ID");
		ArrayList<X_T_Replenish> list = new ArrayList<X_T_Replenish>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			pstmt.setInt (1, getAD_PInstance_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new X_T_Replenish (getCtx(), rs, get_TrxName()));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		X_T_Replenish[] retValue = new X_T_Replenish[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getReplenish
	
	private X_T_Replenish[] listReplenish (Integer pinstance)
	{
		StringBuilder sql = new StringBuilder("SELECT * FROM T_Replenish ");
						sql.append("WHERE AD_PInstance_ID=? ");
		//if (where != null && where.length() > 0)
			//sql.append(" AND ").append(where);
		//sql.append(" ORDER BY M_Warehouse_ID, M_WarehouseSource_ID");
		ArrayList<X_T_Replenish> list = new ArrayList<X_T_Replenish>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			pstmt.setInt (1, pinstance);
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new X_T_Replenish (getCtx(), rs, get_TrxName()));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		X_T_Replenish[] retValue = new X_T_Replenish[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getReplenish
	
	/**
	 * 	Get Replenish Records
	 *	@return replenish
	 */
	private X_T_Replenish[] getReplenishDO (String where)
	{
		StringBuilder sql = new StringBuilder("SELECT * FROM T_Replenish ");
								sql.append("WHERE AD_PInstance_ID=? ");
		if (where != null && where.length() > 0)
			sql.append(" AND ").append(where);
		sql.append(" ORDER BY M_Warehouse_ID, M_WarehouseSource_ID, C_BPartner_ID");
		ArrayList<X_T_Replenish> list = new ArrayList<X_T_Replenish>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			pstmt.setInt (1, getAD_PInstance_ID());
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add (new X_T_Replenish (getCtx(), rs, get_TrxName()));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		X_T_Replenish[] retValue = new X_T_Replenish[list.size ()];
		list.toArray (retValue);
		return retValue;
	}	//	getReplenish
	
}	//	Replenish
