/******************************************************************************
 * Copyright (C) 2009 Low Heng Sin                                            *
 * Copyright (C) 2009 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.kosta.arena.ui;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;

import org.compiere.apps.IStatusBar;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridTab;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRMA;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.X_M_MovementLine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.grid.CreateFrom;
import org.kosta.model.MUMInventoryLine;
import org.kosta.model.MUMReplenish;
import org.kosta.model.MUMReplenishLine;

public abstract class CreateFromInventoryUI extends CreateFrom
{
	/**	Logger			*/
	protected CLogger log = CLogger.getCLogger(getClass());

	/** Loaded Inventory Move            */
	protected MUMReplenish MUMReplenish = null;

	public CreateFromInventoryUI(GridTab gridTab) {
		super(gridTab);
		if (log.isLoggable(Level.INFO)) log.info(gridTab.toString());
	}
	
	protected void configureMiniTable (IMiniTable miniTable)
	{
		miniTable.setColumnClass(0, Boolean.class, false);     //  Selection
		miniTable.setColumnClass(1, BigDecimal.class, false);       //  qtyentered
		miniTable.setColumnClass(2, String.class, true);  //  Replenish No
		miniTable.setColumnClass(3, String.class, true);  //  org
		miniTable.setColumnClass(4, String.class, true);  //  warehouse
		miniTable.setColumnClass(5, String.class, true);  //  date promise
		miniTable.setColumnClass(6, String.class, true);       //  product category
		miniTable.setColumnClass(7, String.class, true);       //  product 
		
		//  Table UI
		miniTable.autoSize();		
	}

	/**
	 *  Dynamic Init
	 *  @return true if initialized
	 */
	public boolean dynInit() throws Exception
	{
		log.config("");
		setTitle(Msg.getElement(Env.getCtx(), "UM_Replenish_ID", false) + " .. " + Msg.translate(Env.getCtx(), "CreateFrom"));
		
		return true;
	}   //  dynInit

	/**
	 *  List number of rows selected
	 */
	public void info(IMiniTable miniTable, IStatusBar statusBar)
	{

	}   //  infoInvoice

	/**
	 *  Save - Create Invoice Lines
	 *  @return true if saved
	 */
	public boolean save(IMiniTable miniTable, String trxName)
	{
		// update status create from line
		int m_inventory_id = ((Integer) getGridTab().getValue("M_Inventory_ID")).intValue();
		MInventory inventory = new MInventory(Env.getCtx(), m_inventory_id, trxName);		

		// Lines
		for (int i = 0; i < miniTable.getRowCount(); i++)
		{
			if (((Boolean)miniTable.getValueAt(i, 0)).booleanValue()) {
				// variable values
			    
				KeyNamePair pp = (KeyNamePair) miniTable.getValueAt(i, 2); // replenishline id-docNo
				int UM_ReplenishLine_ID = pp.getKey();
				
				BigDecimal qtyEntered = (BigDecimal)miniTable.getValueAt(i, 1); // qtyentered
				
				pp = (KeyNamePair) miniTable.getValueAt(i, 3); // org
				int AD_Org_ID = pp.getKey();
								
				pp = (KeyNamePair) miniTable.getValueAt(i, 4); // warehouse
				int M_Warehouse_ID = pp.getKey();
				
				String datePromised = (String)miniTable.getValueAt(i, 5); // date promised
				
				pp = (KeyNamePair) miniTable.getValueAt(i, 6); // product category
				int M_Product_Category_ID = pp.getKey();
				
				pp = (KeyNamePair) miniTable.getValueAt(i, 7); // product 
				int M_Product_ID = pp.getKey();
				
				MUMReplenishLine repLine = new MUMReplenishLine(Env.getCtx(), UM_ReplenishLine_ID, trxName);
								
				MUMInventoryLine invLine = new MUMInventoryLine(Env.getCtx(), 0, trxName);
				invLine.setM_Inventory_ID(inventory.get_ID());
				invLine.setM_Product_ID(M_Product_ID);
				if ( M_Product_ID != 0){
					MProduct product = new MProduct(Env.getCtx(), M_Product_ID, trxName);
					invLine.set_ValueNoCheck("C_UOM_ID",product.getC_UOM_ID());
				}
				invLine.setQtyInternalUse(qtyEntered);
				// remark by ah 
				// setting manual
				invLine.setM_Locator_ID(1000000);
				invLine.setInventoryType("C");
				invLine.setC_Charge_ID(1000000);
				invLine.set_ValueNoCheck("UM_ReplenishLine_ID", UM_ReplenishLine_ID);
				invLine.setDescription(repLine.get_ValueAsString("Description"));
				invLine.saveEx();		
				
				repLine.setQtyDelivered(qtyEntered);
				repLine.saveEx();
			}   //   if selected
		}   //  for all rows

		inventory.set_ValueNoCheck("createfrom", "Y");
		inventory.saveEx();		
		return true;	
	}

	/**
	 *  get data from inventory move c_doctype_id = Move-Out
	 */
	protected ArrayList<KeyNamePair> loadMovementOut ()
	{
		ArrayList<KeyNamePair> list = new ArrayList<KeyNamePair>();

		String sqlStmt = "select r.um_replenish_id , r.documentno || ' - ' ||  ( select value from m_warehouse where m_warehouse_id = r.m_warehouse_id) || ' - ' || " +
				" (select value from m_product_category where m_product_category_id = r.m_product_category_id) || ' - ' || r.datepromised as detail " +
				" from um_replenish r " +
				" where r.ad_client_id = ? " +
				" and r.docstatus = 'CO' " +
				" and r.um_replenish_id in " +
				" ( select um_replenish_id from um_replenishline rl " +
				" where r.um_replenish_id = rl.um_replenish_id " +
				" and ( rl.qtyentered - rl.qtyDelivered ) > 0)"; // not fix
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sqlStmt, null);
		
			pstmt.setInt(1, Env.getContextAsInt(Env.getCtx(), "AD_Client_ID"));
			
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(new KeyNamePair(rs.getInt(1), rs.getString(2)));
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, sqlStmt.toString(), e);
		} finally{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return list;
	}   //  initBPartnerOIS

	/**
	 *  Load Data - Order
	 *  @param C_Order_ID Order
	 *  @param forInvoice true if for invoice vs. delivery qty
	 */
	protected Vector<Vector<Object>> getMovementData (Integer m_replenish_id)
	{
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
	    
	    String sqlStmt = "select rl.um_replenishline_id, documentno, r.ad_org_id, org.name as organization , r.m_warehouse_id , w.name as warehouse, " +
	    		"  r.datepromised, p.m_product_category_id, pc.name as productCategory, rl.m_product_id, p.name as productname , ( rl.qtyentered - rl.qtyDelivered ) as qtyEntered" +
	    		" from um_replenish r " +
	    		" join um_replenishline rl on r.um_replenish_id = rl.um_replenish_id " +
	    		" join ad_org org on org.ad_org_id = r.ad_org_id " +
	    		" join m_warehouse w on w.m_warehouse_id = r.m_warehouse_id " +
	    		" join m_product p on p.m_product_id = rl.m_product_id " +
	    		" join m_product_category pc on pc.m_product_category_id = p.m_product_category_id " +
	    		" where r.ad_client_id = ? " +
	    		" and r.docstatus = 'CO' " +
	    		" and ( rl.qtyentered - rl.qtyDelivered ) > 0" +
	    		" and r.um_replenish_id = ? ";

	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try
	    {
	        pstmt = DB.prepareStatement(sqlStmt.toString(), null);
	        pstmt.setInt(1, Env.getContextAsInt(Env.getCtx(), "AD_Client_ID"));	        
	        pstmt.setInt(2, m_replenish_id);
	        
	        rs = pstmt.executeQuery();

	        while (rs.next())
            {
	            Vector<Object> line = new Vector<Object>(7);
	            line.add(new Boolean(false));   // 0-Selection
	            line.add(rs.getBigDecimal("qtyEntered"));	// 7 qtyentered
	            
	            KeyNamePair pp = new KeyNamePair(rs.getInt("um_replenishline_id"), rs.getString("documentno"));
	            line.add(pp); //  1- document no
	            pp = new KeyNamePair(rs.getInt("ad_org_id"), rs.getString("organization"));
				line.add(pp); 	// 2 - Org
				pp = new KeyNamePair(rs.getInt("m_warehouse_id"), rs.getString("warehouse"));
				line.add(pp); 	// 3 - warehouse
	            
				line.add(rs.getString("datepromised"));	// 4-Date promise 
				pp = new KeyNamePair(rs.getInt("m_product_category_id"), rs.getString("productCategory"));
				line.add(pp);	// 5 product category
				
				pp = new KeyNamePair(rs.getInt("m_product_id"), rs.getString("productName"));
				line.add(pp);	// 6 product name				
				
	            data.add(line);
            }
	    }
	    catch (Exception ex)
	    {
	        log.log(Level.SEVERE, sqlStmt.toString(), ex);
	    }
	    finally
	    {
	    	DB.close(rs, pstmt);
	    	rs = null; pstmt = null;
	    }

		return data;
	}   //  LoadOrder

	protected Vector<String> getOISColumnNames()
	{
	//  Header Info
	    Vector<String> columnNames = new Vector<String>(7);
	    columnNames.add(Msg.getMsg(Env.getCtx(), "Select"));
	    columnNames.add(Msg.translate(Env.getCtx(), "QtyEntered"));
	    columnNames.add(Msg.getElement(Env.getCtx(), "documentno", false));
	    columnNames.add(Msg.translate(Env.getCtx(), "Ad_Org_ID"));
	    columnNames.add(Msg.translate(Env.getCtx(), "M_Warehouse_ID"));
	    columnNames.add(Msg.translate(Env.getCtx(), "DatePromised"));
	    columnNames.add(Msg.translate(Env.getCtx(), "M_Product_Category_ID"));
	    columnNames.add(Msg.translate(Env.getCtx(), "M_Product_ID"));	    
	    
	    return columnNames;
	}
}
