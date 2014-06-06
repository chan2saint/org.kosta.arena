package org.kosta.arena.component;

import java.io.ObjectInputStream.GetField;
import java.math.BigDecimal;

import org.compiere.grid.ICreateFrom;
import org.compiere.grid.ICreateFromFactory;
import org.compiere.model.GridTab;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_M_InOut;
import org.compiere.model.I_M_Inventory;
import org.compiere.model.I_M_Movement;
import org.compiere.model.I_M_Requisition;
import org.kosta.arena.zk.WCreateFromInventoryUI;
import org.kosta.arena.zk.WCreateFromOrderUI;
import org.kosta.arena.zk.WCreateFromRequisitionUI;
import org.kosta.arena.zk.WCreateFromShipmentUI;

public class ArenaCreateFormFactory implements ICreateFromFactory {

	@Override
	public ICreateFrom create(GridTab mTab) {
		String tableName = mTab.getTableName();
		if(tableName.equals(I_C_Order.Table_Name))
			return new WCreateFromOrderUI(mTab);	
		else if(tableName.equals(I_M_Requisition.Table_Name)){
			return new WCreateFromRequisitionUI(mTab);
		}
		else if(tableName.equals(I_M_Inventory.Table_Name)){
			return new WCreateFromInventoryUI(mTab);
		}
		else if(tableName.equals(I_M_InOut.Table_Name)){
			return new WCreateFromShipmentUI(mTab);
		}
		return null;
	}

}
