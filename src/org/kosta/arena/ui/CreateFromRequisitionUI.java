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
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;

import org.compiere.apps.IStatusBar;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridTab;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRMA;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.Query;
import org.compiere.model.X_M_MovementLine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.grid.CreateFrom;
import org.kosta.model.MUMReplenish;
import org.kosta.model.MUMReplenishLine;

public abstract class CreateFromRequisitionUI extends CreateFrom
{
	/**	Logger			*/
	protected CLogger log = CLogger.getCLogger(getClass());

	/** Loaded Inventory Move            */
	protected MUMReplenish MUMReplenish = null;

	public CreateFromRequisitionUI(GridTab gridTab) {
		super(gridTab);
		if (log.isLoggable(Level.INFO)) log.info(gridTab.toString());
	}
	
	protected void configureMiniTable (IMiniTable miniTable)
	{
		miniTable.setColumnClass(0, Boolean.class, false);     //  Selection
		miniTable.setColumnClass(1, String.class, true);  //  Replenish No
		miniTable.setColumnClass(2, String.class, true);  //  org
		miniTable.setColumnClass(3, String.class, true);  //  warehouse
		miniTable.setColumnClass(4, String.class, true);  //  date promise
		miniTable.setColumnClass(5, String.class, true);       //  product category
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
		int m_requisition_id = ((Integer) getGridTab().getValue("M_Requisition_ID")).intValue();
		MRequisition requisition = new MRequisition(Env.getCtx(), m_requisition_id, trxName);
				
		// Lines
		for (int i = 0; i < miniTable.getRowCount(); i++)
		{
			if (((Boolean)miniTable.getValueAt(i, 0)).booleanValue()) {
			    KeyNamePair pp = (KeyNamePair) miniTable.getValueAt(i, 2); // ad_org
				int Ad_Org_ID = pp.getKey();
				
				pp = (KeyNamePair) miniTable.getValueAt(i, 1); // document no
				int UM_Replenish_ID = pp.getKey();
				
				pp = (KeyNamePair) miniTable.getValueAt(i, 3); // m_warehouse
				int M_Warehouse_ID = pp.getKey();
				
				String datePromised = (String)miniTable.getValueAt(i, 4); // date promised
				
				pp = (KeyNamePair) miniTable.getValueAt(i, 5); // m product category
				int M_Product_Category_ID = pp.getKey();
			    
				MUMReplenish replenish = new MUMReplenish(Env.getCtx(), UM_Replenish_ID, trxName);
								
				MUMReplenishLine[] repLines = replenish.getLines();
				
				//int lengthReplenish = replenish.getLines().length;
				if ( repLines.length > 0){
					for (int j = 0; j < repLines.length; j++) {
						// create requisition line from replenish ID
						// added by Surya to group product in requisition 
						MRequisitionLine reqLine = null;
						BigDecimal tempQty = Env.ZERO;
						reqLine = new Query(Env.getCtx(), MRequisitionLine.Table_Name, MRequisitionLine.COLUMNNAME_M_Requisition_ID + "=? AND " + MRequisitionLine.COLUMNNAME_M_Product_ID + "=? ", trxName)
									.setParameters(new Object[]{requisition.get_ID(), repLines[j].getM_Product_ID()})
									.first();
						
						if (reqLine == null) reqLine = new MRequisitionLine(requisition);
						else tempQty = tempQty.add(reqLine.getQty());
											
						reqLine.setM_Product_ID(repLines[j].getM_Product_ID());
						reqLine.setC_UOM_ID(repLines[j].getC_UOM_ID());
						reqLine.setQty(repLines[j].getQtyEntered().add(tempQty));
						reqLine.setDescription(repLines[j].get_ValueAsString("Description"));	
						reqLine.set_ValueNoCheck("QtyDelivered", Env.ZERO);
						reqLine.saveEx();
						//added by Surya, add purchase qty 
						repLines[j].setQtyPurchased(repLines[j].getQtyEntered());
						repLines[j].saveEx();
					}
				}
				replenish.setM_Requisition_ID(requisition.get_ID());
				replenish.saveEx();				
			}   //   if selected
		}   //  for all rows

		requisition.set_ValueNoCheck("createfrom", "Y");
		requisition.saveEx();		
		return true;	
	}

	/**
	 *  get data from replenish 
	 */
	protected ArrayList<KeyNamePair> loadReplenish ()
	{
		ArrayList<KeyNamePair> list = new ArrayList<KeyNamePair>();

		String sqlStmt = "select r.um_replenish_id , r.documentno || ' - ' ||  ( select value from m_warehouse where m_warehouse_id = r.m_warehouse_id) || ' - ' || (select value from m_product_category where m_product_category_id = r.m_product_category_id) as detail from um_replenish r " +
				" where r.ad_client_id = ? " +
				" and r.docstatus = 'CO' " +
				" and r.um_replenish_id in ( select um_replenish_id from um_replenishline rl where r.um_replenish_id = rl.um_replenish_id )";
		
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
	 *  get data from replenish 
	 */
	protected ArrayList<KeyNamePair> loadProductCategory()
	{
		ArrayList<KeyNamePair> list = new ArrayList<KeyNamePair>();

		String sqlStmt = "select m_product_category_id, name from m_product_category " +
				" where ad_client_id = ? " +
				" and isactive = 'Y' " +
				" order by name";
		
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
	 *  Load Data - replenish
	 *  @param um_replenish_id Replenish
	 */
	protected Vector<Vector<Object>> getMovementData (int um_replenish_id)
	{
		MUMReplenish = new MUMReplenish(Env.getCtx(), um_replenish_id, null); 				
		
	    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
	    	    
	    String sqlStmt = "select r.um_replenish_id, rl.qtyentered, rl.m_product_id, p.name , rl.c_uom_id,  coalesce(c.UOMSymbol, c.name) as uom," +
	    		" coalesce(p.m_locator_id,1000000) as m_locator_id, ( select value from m_locator where m_locator_id = coalesce(p.m_locator_id,1000000)  ) locatorname, " +
	    		" r.m_warehouse_id, ( select value from m_warehouse where m_warehouse_id = r.m_warehouse_id ) locatortoName " +
	    		" from um_replenish r " +
	    		" join um_replenishline rl on r.um_replenish_id = rl.um_replenish_id " +
	    		" join c_uom c on c.c_uom_id = rl.c_uom_id " +
	    		" join m_product p on p.m_product_id = rl.m_product_id " +
	    		" where r.ad_client_id = ? " +
	    		" and r.docstatus = 'CO'" +
	    		" and r.um_replenish_id = ?";

	    		PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try
	    {
	        pstmt = DB.prepareStatement(sqlStmt.toString(), null);
	        pstmt.setInt(1, Env.getContextAsInt(Env.getCtx(), "AD_Client_ID"));	        
	        pstmt.setInt(2, um_replenish_id);
	        
	        rs = pstmt.executeQuery();

	        while (rs.next())
            {
	            Vector<Object> line = new Vector<Object>(5);
	            line.add(new Boolean(false));   // 0-Selection
	            line.add(rs.getBigDecimal("qtyEntered"));  // 1-Qty Move
	            KeyNamePair pp = new KeyNamePair(rs.getInt("m_product_id"), rs.getString("name"));
	            line.add(pp); //  2-product
	            pp = new KeyNamePair(rs.getInt("c_uom_id"), rs.getString("uom"));
				line.add(pp); 	// 3 UOM
	            
				line.add(rs.getInt("UM_Replenish_id"));	// 4-movement id form
				pp = new KeyNamePair(rs.getInt("m_locator_id"), rs.getString("LocatorName"));
				line.add(pp);	// 5 Locator
				pp = new KeyNamePair(rs.getInt("m_warehouse_id"), rs.getString("LocatorToName"));
				line.add(pp);	// 6 Locator To
				
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
	}   //  LoadReplenish
	
	/**
	 *  Load Data - replenish line
	 *  @param um_replenish_id Replenish
	 */
	protected Vector<Vector<Object>> getReplenishDataDate (java.sql.Timestamp datePromised, Integer m_product_category_id)
	{
	    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
	    	    
	    String sqlStmt = "select r.um_replenish_id, documentno, r.ad_org_id, org.name as organization , r.m_warehouse_id , w.name as warehouse, " +
	    		"  r.datepromised, " +
	    		"  ( select max(m_product_category_id) from m_product_category pc where pc.m_product_category_id in " +
	    		" ( select m_product_category_id from um_replenishline rl " +
	    		" join m_product p on p.m_product_id = rl.m_product_id " +
	    		" where um_replenish_id = r.um_replenish_id " +
	    		" )) as m_product_category_id, " +
	    		" ( select name from m_product_category pc where pc.m_product_category_id = r.m_product_category_id ) as productcategory " +
	    		" from um_replenish r " +
	    		" join ad_org org on org.ad_org_id = r.ad_org_id " +
	    		" join m_warehouse w on w.m_warehouse_id = r.m_warehouse_id " +
	    		" where r.ad_client_id = ? " +
	    		" and r.docstatus = 'CO' " +
	    		" and datepromised = ? " +
	    		" and m_requisition_id is null ";
	    if ( m_product_category_id != 0) // search by product category
	    	sqlStmt += "  and ( select max(m_product_category_id) from m_product_category pc where pc.m_product_category_id in " +
	    			" ( select m_product_category_id from um_replenishline rl " +
	    			" join m_product p on p.m_product_id = rl.m_product_id " +
	    			" where um_replenish_id = r.um_replenish_id ) " +
	    			" ) = ? ";	

	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try
	    {
	        pstmt = DB.prepareStatement(sqlStmt.toString(), null);
	        pstmt.setInt(1, Env.getContextAsInt(Env.getCtx(), "AD_Client_ID"));	        
	        pstmt.setTimestamp(2, datePromised);
	        if ( m_product_category_id != 0)
	        	pstmt.setInt(3, m_product_category_id);
	        
	        rs = pstmt.executeQuery();

	        while (rs.next())
            {
	            Vector<Object> line = new Vector<Object>(5);
	            line.add(new Boolean(false));   // 0-Selection
	            KeyNamePair pp = new KeyNamePair(rs.getInt("um_replenish_id"), rs.getString("documentno"));
	            line.add(pp); //  1- document no
	            pp = new KeyNamePair(rs.getInt("ad_org_id"), rs.getString("organization"));
				line.add(pp); 	// 2 - Org
				pp = new KeyNamePair(rs.getInt("m_warehouse_id"), rs.getString("warehouse"));
				line.add(pp); 	// 3 - warehouse
	            
				line.add(rs.getString("datepromised"));	// 4-Date promise 
				pp = new KeyNamePair(rs.getInt("m_product_category_id"), rs.getString("productCategory"));
				line.add(pp);	// 5 product categ
				
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
	}   //  Load Replenish ID line

	protected Vector<String> getOISColumnNames()
	{
		//  Header Info
	    Vector<String> columnNames = new Vector<String>(5);
	    columnNames.add(Msg.getMsg(Env.getCtx(), "Select"));
	    columnNames.add(Msg.getElement(Env.getCtx(), "documentno", false));
	    columnNames.add(Msg.translate(Env.getCtx(), "Ad_Org_ID"));
	    columnNames.add(Msg.translate(Env.getCtx(), "M_Warehouse_ID"));
	    columnNames.add(Msg.translate(Env.getCtx(), "DatePromised"));
	    columnNames.add(Msg.translate(Env.getCtx(), "M_Product_Category_ID"));
	    
	    return columnNames;
	}
}
