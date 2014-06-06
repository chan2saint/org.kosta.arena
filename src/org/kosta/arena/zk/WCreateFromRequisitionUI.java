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

package org.kosta.arena.zk;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WDatetimeEditor;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WLocatorEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WStringEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.compiere.apps.IStatusBar;
import org.compiere.grid.CreateFromShipment;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridTab;
import org.compiere.model.I_M_RequisitionLine;
import org.compiere.model.MLocatorLookup;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MProduct;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.Query;

import static org.compiere.model.SystemIDs.*;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.kosta.arena.ui.CreateFromRequisitionUI;
import org.kosta.model.MUMReplenishLine;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Space;
import org.zkoss.zul.Vlayout;
import org.adempiere.webui.apps.form.WCreateFromWindow;

public class WCreateFromRequisitionUI extends CreateFromRequisitionUI implements EventListener<Event>, ValueChangeListener
{
	private WCreateFromWindow window;
	
	public WCreateFromRequisitionUI(GridTab tab) 
	{
		super(tab);
		log.info(getGridTab().toString());
		
		window = new WCreateFromWindow(this, getGridTab().getWindowNo());
		
		p_WindowNo = getGridTab().getWindowNo();

		try
		{
			if (!dynInit())
				return;
			zkInit();
			setInitOK(true);
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "", e);
			setInitOK(false);
			throw new AdempiereException(e.getMessage());
		}
		AEnv.showWindow(window);
	}
	
	private void initMovementDetails()
	{
	    productCatField.removeActionListener(this);
	    productCatField.removeAllItems();
	    //  None
	    KeyNamePair pp = new KeyNamePair(0,"");
	    productCatField.addItem(pp);
	    
	    ArrayList<KeyNamePair> list = loadProductCategory();
		for(KeyNamePair knp : list)
			productCatField.addItem(knp);
		
	    productCatField.setSelectedIndex(0);
	    productCatField.addActionListener(this);
	}
	
	private void initReplenishDetails()
	{
	    //datePromiseField.removeValuechangeListener(this);
	    
	}
	
	/** Window No               */
	private int p_WindowNo;

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(getClass());
		
    /** Label for the movement selection */
    protected Label productCatLabel = new Label();
    /** Combo box for selecting Movement */
    protected Listbox productCatField = ListboxFactory.newDropdownListbox();
	
	protected Label datePromised = new Label();
	protected WDatetimeEditor datePromiseField = new WDatetimeEditor("DatePromised", false, false, true, "DatePromised");
    
	/**
	 *  Dynamic Init
	 *  @throws Exception if Lookups cannot be initialized
	 *  @return true if initialized
	 */
	public boolean dynInit() throws Exception
	{
		log.config("");
		
		super.dynInit();
		
		window.setTitle(getTitle());
		
		datePromiseField.addValueChangeListener(this);
		
		initMovementDetails();

		return true;
	}   //  dynInit
	
	protected void zkInit() throws Exception
	{
		productCatLabel.setText(Msg.translate(Env.getCtx(), "M_Product_Category_ID"));
		datePromised.setText(Msg.translate(Env.getCtx(), "DatePromised"));
		
    	boolean isRMAWindow = ((getGridTab().getAD_Window_ID() == WINDOW_RETURNTOVENDOR) || (getGridTab().getAD_Window_ID() == WINDOW_CUSTOMERRETURN)); 

		//upcLabel.setText(Msg.getElement(Env.getCtx(), "UPC", false));

		Vlayout vlayout = new Vlayout();
		vlayout.setVflex("1");
		vlayout.setWidth("100%");
    	Panel parameterPanel = window.getParameterPanel();
		parameterPanel.appendChild(vlayout);
		
		Grid parameterStdLayout = GridFactory.newGridLayout();
    	vlayout.appendChild(parameterStdLayout);
		
		Rows rows = (Rows) parameterStdLayout.newRows();
		Row row = rows.newRow();		
		
		row.appendChild(datePromised);
		row.appendChild(datePromiseField.getComponent());
		datePromised.setHflex("1");	
		datePromiseField.getComponent().setHflex("1");
		
		row.appendChild(productCatLabel.rightAlign());
		row.appendChild(productCatField);
    	productCatField.setHflex("1");
	}

	private boolean 	m_actionActive = false;
	
	/**
	 *  Action Listener
	 *  @param e event
	 * @throws Exception 
	 */
	public void onEvent(Event e) throws Exception
	{
		if (m_actionActive)
			return;
		m_actionActive = true;
		
		// movement
        if (e.getTarget().equals(productCatField))
        {
            KeyNamePair pp = productCatField.getSelectedItem().toKeyNamePair();
            if (pp == null || pp.getKey() == 0)
                ;
            else
            {
                int M_movement_id = pp.getKey();
                
                loadReplenish((Timestamp)datePromiseField.getValue()	 , M_movement_id);
                //loadMovement(M_movement_id);
                //loadProductCategory();
            }
        }
		
		m_actionActive = false;
	}
	
	/**
	 *  Load Data - Requisition
	 *  @param M_movement_id Requisition
	 */
	protected void loadMovement (int M_movement_id)
	{
		loadTableOIS(getMovementData(M_movement_id));
	}
	
	protected void loadReplenish (Timestamp datePromised, Integer m_product_category_id)
	{
		loadTableOIS(getReplenishDataDate(datePromised, m_product_category_id));
	}
	
	/**
	 * Checks the UPC value and checks if the UPC matches any of the products in the
	 * list.
	 */
	private void checkProductUsingUPC()
	{
		/*
		String upc = upcField.getDisplay();
		//DefaultTableModel model = (DefaultTableModel) dialog.getMiniTable().getModel();
		ListModelTable model = (ListModelTable) window.getWListbox().getModel();
		
		// Lookup UPC
		List<MProduct> products = MProduct.getByUPC(Env.getCtx(), upc, null);
		for (MProduct product : products)
		{
			int row = findProductRow(product.get_ID());
			if (row >= 0)
			{
				BigDecimal qty = (BigDecimal)model.getValueAt(row, 1);
				model.setValueAt(qty, row, 1);
				model.setValueAt(Boolean.TRUE, row, 0);
				model.updateComponent(row, row);
			}
		}
		upcField.setValue("");
		*/
	}

	/**
	 * Finds the row where a given product is. If the product is not found
	 * in the table -1 is returned.
	 * @param M_Product_ID
	 * @return  Row of the product or -1 if non existing.
	 * 
	 */
	private int findProductRow(int M_Product_ID)
	{
		//DefaultTableModel model = (DefaultTableModel)dialog.getMiniTable().getModel();
		ListModelTable model = (ListModelTable) window.getWListbox().getModel();
		KeyNamePair kp;
		for (int i=0; i<model.getRowCount(); i++) {
			kp = (KeyNamePair)model.getValueAt(i, 4);
			if (kp.getKey()==M_Product_ID) {
				return(i);
			}
		}
		return(-1);
	}
		
	/**
	 *  Change Listener
	 *  @param e event
	 */
	public void valueChange (ValueChangeEvent e)
	{
		if (log.isLoggable(Level.CONFIG)) log.config(e.getPropertyName() + "=" + e.getNewValue());

		if (e.getPropertyName().equals("DatePromised"))
		{
			Timestamp DatePromised = (Timestamp)datePromiseField.getValue();
			loadReplenish(DatePromised, 0);
		}
		window.tableChanged(null);
	}   //  vetoableChange
	
	
	/**
	 * Load RMA that are candidates for shipment
	 * @param C_BPartner_ID BPartner
	 */
	private void initBPRMADetails(int C_BPartner_ID)
	{
	    productCatField.removeActionListener(this);
	    productCatField.removeAllItems();
	    //  None
	    KeyNamePair pp = new KeyNamePair(0,"");
	    productCatField.addItem(pp);
		
	    productCatField.setSelectedIndex(0);
	    productCatField.addActionListener(this);
	}
	
	/**
	 *  Load Order/Invoice/Shipment data into Table
	 *  @param data data
	 */
	protected void loadTableOIS (Vector<?> data)
	{
		window.getWListbox().clear();
		
		//  Remove previous listeners
		window.getWListbox().getModel().removeTableModelListener(window);
		//  Set Model
		ListModelTable model = new ListModelTable(data);
		model.addTableModelListener(window);
		window.getWListbox().setData(model, getOISColumnNames());
		
		configureMiniTable(window.getWListbox());
	}   //  loadOrder
	
	public void showWindow()
	{
		window.setVisible(true);
	}
	
	public void closeWindow()
	{
		window.dispose();
	}

	@Override
	public Object getWindow() {
		return window;
	}

}
