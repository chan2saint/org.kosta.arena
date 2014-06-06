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
import org.compiere.grid.CreateFrom;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridTab;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MLocator;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRMA;
import org.compiere.model.MRMALine;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MWarehouse;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;

/**
 *  Create PO Orders Transactions from Requisition
 *
 *  @author Zuhri Utama (zuhriutama@gmail.com)
 */
public abstract class CreateFromOrderUI extends CreateFrom 
{
	/**  Loaded Requisition         */
	private MRequisition m_requisition = null;
	
	/**
	 *  Protected Constructor
	 *  @param mTab MTab
	 */
	public CreateFromOrderUI(GridTab mTab)
	{
		super(mTab);
		if (log.isLoggable(Level.INFO)) log.info(mTab.toString());
	}   //  CreateFromOrder

	/**
	 *  Dynamic Init
	 *  @return true if initialized
	 */
	public boolean dynInit() throws Exception
	{
		log.config("");
		setTitle(Msg.getElement(Env.getCtx(), "C_Order_ID", false) + " .. " + Msg.translate(Env.getCtx(), "CreateFrom"));
		
		return true;
	}   //  dynInit
	
	/**
	 *  Load Requisition Field.
	 */
	protected ArrayList<KeyNamePair> loadRequisitionData() {
		ArrayList<KeyNamePair> list = new ArrayList<KeyNamePair>();

		String sqlStmt = "SELECT r.M_Requisition_ID, r.DocumentNo || ' - ' || r.DateRequired from M_Requisition r "
			+ "WHERE r.DocStatus in ('CO', 'CL') "
			+ "AND r.M_Requisition_ID in (SELECT rl.M_Requisition_ID FROM M_RequisitionLine rl "
			+ "WHERE rl.M_Requisition_ID=r.M_Requisition_ID ) " +
			//" and coalesce(rl.qty,0 ) > coalesce( rl.qtydelivered,0 )) " +
			" and r.ad_client_id = ?";

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
	}
		
	/**
	 * Load Requisition details
	 * @param M_Requisition_ID Requisition
	 */
	protected Vector<Vector<Object>> getRequisitionData(int M_Requisition_ID)
	{
		m_requisition = new MRequisition(Env.getCtx(), M_Requisition_ID, null);
			
	    Vector<Vector<Object>> data = new Vector<Vector<Object>>();
	    StringBuilder sqlStmt = new StringBuilder();
	    
	    sqlStmt.append("SELECT rl.M_RequisitionLine_ID, rl.line, coalesce(rl.qty,0 ) - coalesce(rl.qtydelivered,0 ), p.M_Product_ID, p.Name, uom.C_UOM_ID, COALESCE(uom.UOMSymbol,uom.Name) ");
	    sqlStmt.append("FROM M_RequisitionLine rl INNER JOIN M_Product p ON p.M_Product_ID = rl.M_Product_ID ");
	    if (Env.isBaseLanguage(Env.getCtx(), "C_UOM"))
        {
	        sqlStmt.append("LEFT OUTER JOIN C_UOM uom ON (uom.C_UOM_ID=p.C_UOM_ID) ");
        }
	    else
        {
	        sqlStmt.append("LEFT OUTER JOIN C_UOM_Trl uom ON (uom.C_UOM_ID=100 AND uom.AD_Language='");
	        sqlStmt.append(Env.getAD_Language(Env.getCtx())).append("') ");
        }
	    sqlStmt.append("WHERE rl.M_Requisition_ID=? ");
	    sqlStmt.append("AND rl.M_Product_ID IS NOT NULL ");
	    sqlStmt.append(" and coalesce(rl.qty,0 ) > coalesce(rl.qtydelivered,0 ) ");
	         
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try
	    {
	        pstmt = DB.prepareStatement(sqlStmt.toString(), null);
	        pstmt.setInt(1, M_Requisition_ID);
	        rs = pstmt.executeQuery();
	               
	        while (rs.next())
            {
	            Vector<Object> line = new Vector<Object>(5);
	            line.add(new Boolean(false));   // 0-Selection
	            line.add(rs.getBigDecimal(3));  // 1-Qty
	            KeyNamePair pp = new KeyNamePair(rs.getInt(6), rs.getString(7));
	            line.add(pp); // 2-UOM
	            pp = new KeyNamePair(rs.getInt(4), rs.getString(5));
				line.add(pp); // 3-Product
				pp = new KeyNamePair(rs.getInt(1), rs.getString(2));
				line.add(pp);   //4-Requisition
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
	}
	
	/**
	 *  List number of rows selected
	 */
	public void info(IMiniTable miniTable, IStatusBar statusBar)
	{

	}   //  infoInvoice

	protected void configureMiniTable (IMiniTable miniTable)
	{
		miniTable.setColumnClass(0, Boolean.class, false);     //  Selection
		miniTable.setColumnClass(1, BigDecimal.class, false);  //  Qty
		miniTable.setColumnClass(2, String.class, true);       //  UOM
		miniTable.setColumnClass(3, String.class, true);   	   //  Product
		miniTable.setColumnClass(4, String.class, true);       //  Invoice
		
		//  Table UI
		miniTable.autoSize();		
	}

	/**
	 *  Save - Create Invoice Lines
	 *  @return true if saved
	 */
	public boolean save(IMiniTable miniTable, String trxName)
	{		
		// Get Order current
		int C_Order_ID = ((Integer) getGridTab().getValue("C_Order_ID")).intValue();
		MOrder order = new MOrder(Env.getCtx(), C_Order_ID, trxName);
		order.set_ValueNoCheck("createfrom", "Y");
		if (log.isLoggable(Level.CONFIG)) log.config(order.toString());

		// Lines
		for (int i = 0; i < miniTable.getRowCount(); i++)
		{
			if (((Boolean)miniTable.getValueAt(i, 0)).booleanValue()) {
				// variable values
				BigDecimal QtyEntered = (BigDecimal) miniTable.getValueAt(i, 1); // Qty
				KeyNamePair pp = (KeyNamePair) miniTable.getValueAt(i, 2); // UOM
				int C_UOM_ID = pp.getKey();
				pp = (KeyNamePair) miniTable.getValueAt(i, 3); // Product
				int M_Product_ID = pp.getKey();
				int M_RequisitionLine_ID = 0;
				pp = (KeyNamePair) miniTable.getValueAt(i, 4); // Requisition
				// If we have RMA
				if (pp != null)
					M_RequisitionLine_ID = pp.getKey();
				//	Precision of Qty UOM
				int precision = 2;
				if (M_Product_ID != 0)
				{
					MProduct product = MProduct.get(Env.getCtx(), M_Product_ID);
					precision = product.getUOMPrecision();
				}
				QtyEntered = QtyEntered.setScale(precision, BigDecimal.ROUND_HALF_DOWN);
				//
				if (log.isLoggable(Level.FINE)) log.fine("Line QtyEntered=" + QtyEntered
						+ ", Product=" + M_Product_ID 
						+ ", RequisitionLine=" + M_RequisitionLine_ID);

				//	Create new Order Line
				MOrderLine iol = new MOrderLine (order);
				iol.setM_Product_ID(M_Product_ID, C_UOM_ID);	//	Line UOM
				iol.setQty(QtyEntered);							//	Movement/Entered
				
				MRequisitionLine rl = null;				
				
				if (M_RequisitionLine_ID != 0)
				{
					rl = new MRequisitionLine (Env.getCtx(), M_RequisitionLine_ID, trxName);
					rl.setC_OrderLine_ID(iol.get_ID());
					rl.set_ValueNoCheck("QtyDelivered", QtyEntered);
					iol.setDescription(rl.getDescription());	// get description PR
					rl.saveEx();
				}
				
				iol.saveEx();
			}   //   if selected
		}   //  for all rows

		/**
		 *  Update Header
		 */
		if (p_order != null && p_order.getC_Order_ID() != 0)
		{
			order.setC_Order_ID (p_order.getC_Order_ID());
			order.setAD_OrgTrx_ID(p_order.getAD_OrgTrx_ID());
			order.setC_Project_ID(p_order.getC_Project_ID());
			order.setC_Campaign_ID(p_order.getC_Campaign_ID());
			order.setC_Activity_ID(p_order.getC_Activity_ID());
			order.setUser1_ID(p_order.getUser1_ID());
			order.setUser2_ID(p_order.getUser2_ID());

			if ( p_order.isDropShip() )
			{
				order.setM_Warehouse_ID( p_order.getM_Warehouse_ID() );
				order.setIsDropShip(p_order.isDropShip());
				order.setDropShip_BPartner_ID(p_order.getDropShip_BPartner_ID());
				order.setDropShip_Location_ID(p_order.getDropShip_Location_ID());
				order.setDropShip_User_ID(p_order.getDropShip_User_ID());
			}
		}
		order.saveEx();
		return true;		

	}   //  saveOrder

	protected Vector<String> getOISColumnNames()
	{
		//  Header Info
	    Vector<String> columnNames = new Vector<String>(5);
	    columnNames.add(Msg.getMsg(Env.getCtx(), "Select"));
	    columnNames.add(Msg.translate(Env.getCtx(), "Quantity"));
	    columnNames.add(Msg.translate(Env.getCtx(), "C_UOM_ID"));
	    columnNames.add(Msg.translate(Env.getCtx(), "M_Product_ID"));
	    columnNames.add(Msg.getElement(Env.getCtx(), "M_Requisition_ID", false));
	    
	    return columnNames;
	}
}
