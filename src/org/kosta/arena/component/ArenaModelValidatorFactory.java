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

import java.math.BigDecimal;
import java.util.Collection;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.base.event.LoginEventData;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.I_M_Forecast;
import org.compiere.model.I_M_ForecastLine;
import org.compiere.model.I_M_InOut;
import org.compiere.model.I_M_Movement;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_M_Requisition;
import org.compiere.model.I_M_RequisitionLine;
import org.compiere.model.MForecastLine;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRMALine;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_M_Forecast;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.osgi.service.event.Event;

/**
 * @author hengsin
 *
 */
public class ArenaModelValidatorFactory extends AbstractEventHandler{
	private static CLogger log = CLogger.getCLogger(ArenaModelValidatorFactory.class);
	private String trxName = "";
	private PO po = null;
	/**
	 * default constructor
	 */
	public ArenaModelValidatorFactory() {
	}
	
	@Override
	protected void initialize() {
		registerEvent(IEventTopics.AFTER_LOGIN);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MInvoice.Table_Name);
				
		log.info("ARENA MODEL VALIDATOR IS NOW INITIALIZED");
	}
	@Override
	protected void doHandleEvent(Event event) {
		String type = event.getTopic();
		if (type.equals(IEventTopics.AFTER_LOGIN)) {
			LoginEventData eventData = getEventData(event);
			log.fine(" topic="+event.getTopic()+" AD_Client_ID="+eventData.getAD_Client_ID()
					+" AD_Org_ID="+eventData.getAD_Org_ID()+" AD_Role_ID="+eventData.getAD_Role_ID()
					+" AD_User_ID="+eventData.getAD_User_ID());
		}
		else 
		{
			setPo(getPO(event));
			setTrxName(po.get_TrxName());
			log.info(" topic="+event.getTopic()+" po="+po);
		}
		if (po instanceof MInvoice && type == IEventTopics.DOC_BEFORE_COMPLETE){
			logEvent(event, po, type);
			MInvoice inv = (MInvoice) po;
			MInvoiceLine invLines[] = inv.getLines();
			for (MInvoiceLine invLine : invLines)
			{
				if (invLine.getC_OrderLine_ID() != 0)
				{
					MOrderLine oLine = new MOrderLine(Env.getCtx(), invLine.getC_OrderLine_ID(), trxName);
					invLine.set_ValueNoCheck("UM_Modifier_ID", oLine.get_Value("UM_Modifier_ID"));
					invLine.saveEx();	
				}	
			}
		}
	}

	
	//red1 factored log message handling
	private void logEvent (Event event, PO po, String msg) {
		log.info("Arena >> ModelValidator // "+event.getTopic()+" po="+po+" MESSAGE ="+msg);		
	}

	private void setPo(PO eventPO) {
		 po = eventPO;
	}

	private void setTrxName(String get_TrxName) {
		trxName = get_TrxName;		
	}
	

}
