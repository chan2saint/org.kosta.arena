/******************************************************************************
 * Copyright (C) 2013 Heng Sin Low                                            *
 * Copyright (C) 2013 Trek Global                 							  *
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
package org.kosta.arena.component;

import java.util.List;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.IColumnCalloutFactory;
import org.adempiere.base.ServiceQuery;
import org.adempiere.base.equinox.EquinoxExtensionLocator;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MLocator;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPayment;
import org.compiere.model.MRMA;
import org.compiere.model.MRMALine;
import org.compiere.model.MRequisitionLine;
import org.kosta.callout.Callout_M_InventoryLine;
import org.kosta.callout.Callout_M_OrderLine;
import org.kosta.callout.Callout_M_RMA;
import org.kosta.callout.Callout_M_RMALine;
import org.kosta.callout.Callout_UM_Replenish;
import org.kosta.callout.Callout_UM_ReplenishLine;
import org.kosta.model.MUMReplenish;
import org.kosta.model.MUMReplenishLine;

/**
 * @author hengsin
 *
 */
public class ArenaCalloutFactory implements IColumnCalloutFactory {

	/**
	 * default constructor
	 */
	public ArenaCalloutFactory() {
	}

	/* (non-Javadoc)
	 * @see org.adempiere.base.IColumnCalloutFactory#getColumnCallouts(java.lang.String, java.lang.String)
	 */
	@Override
	public IColumnCallout[] getColumnCallouts(String tableName,
			String columnName) {
		if ( tableName.equalsIgnoreCase(MUMReplenish.Table_Name)){
			return new IColumnCallout[]{new Callout_UM_Replenish() };
		}
		else if ( tableName.equalsIgnoreCase(MUMReplenishLine.Table_Name)){
			return new IColumnCallout[]{new Callout_UM_ReplenishLine() };
		}
		else if ( tableName.equalsIgnoreCase(MRMA.Table_Name)){
			return new IColumnCallout[]{new Callout_M_RMA() };
		}
		else if ( tableName.equalsIgnoreCase(MRMALine.Table_Name)){
			return new IColumnCallout[]{new Callout_M_RMALine() };
		}
		else if ( tableName.equalsIgnoreCase(MOrderLine.Table_Name)){
			return new IColumnCallout[]{new Callout_M_OrderLine() };
		}
		else if ( tableName.equalsIgnoreCase(MInventoryLine.Table_Name)){
			return new IColumnCallout[]{new Callout_M_InventoryLine() };
		}
		
		return null;
	}

}
