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
 *****************************************************************************/
package org.kosta.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.adempiere.base.Core;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.BPartnerNoBillToAddressException;
import org.adempiere.exceptions.BPartnerNoShipToAddressException;
import org.adempiere.exceptions.FillMandatoryException;
import org.adempiere.model.ITaxProvider;
import org.adempiere.process.SalesOrderRateInquiryProcess;
import org.compiere.model.I_M_RequisitionLine;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportEngine;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ServerProcessCtl;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;

public class MUMReplenish extends X_UM_Replenish implements DocAction
{
	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;
	
	public MUMReplenish(Properties ctx, int kst_replenish_ID, String trxName) {
		super(ctx, kst_replenish_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MUMReplenish(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	protected boolean beforeSave(boolean newRecord)
	{		
		if( getDatePromised().before(getDateReplenish())) // checking data promise
		{
			log.saveError("Error", Msg.getMsg(getCtx(), "\nTanggal Promised harus melebihin tanggal Requestion"));
			return false;
		}
		else 
		{
			if ( getHour(getDatePromised()) == 9 || getHour(getDatePromised()) == 13)
				;
			else
			{
				log.saveError("Error", Msg.getMsg(getCtx(), "\nDate Promised harus di jam 9.00 AM dan jam 1.00 PM"));
				return false;
			}
		}
		
		return true;
	}
	
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		
		return true;
	}
	
	private Integer getHour(java.sql.Timestamp date)
	{
		String tgl 			= date.toString();
		String[] listTgl 	= tgl.split(" ");
		String hour 		= listTgl[1].substring(0,2);
		
		return Integer.parseInt(hour);		
	}
	
	/**************************************************************************
	 * 	Process document
	 *	@param processAction document action
	 *	@return true if performed
	 */
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt

	@Override
	public boolean unlockIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean invalidateIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String prepareIt() {
		// TODO Auto-generated method stub
		return DOCSTATUS_InProgress;
	}

	@Override
	public boolean approveIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rejectIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String completeIt() {
		// TODO Auto-generated method stub
		if ( getDocStatus().equals(DOCSTATUS_Drafted) || getDocStatus().equals(DOCSTATUS_InProgress)) 
		{
			int qReplenish = new Query(getCtx(), MUMReplenishLine.Table_Name, "UM_Replenish_ID = ?",
					get_TrxName())
					.setParameters(getUM_Replenish_ID())
					.count();
			log.warning("Lines "+qReplenish);
			
			if ( qReplenish == 0)
				throw new AdempiereException("\nCreate Line Form / Create New Data");
			
			setProcessed(true);
			setDocAction(DOCACTION_None);
			setDocStatus(DOCSTATUS_Completed);
			
			return DOCSTATUS_Completed;
		}
		
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}

	@Override
	public boolean voidIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean closeIt() {
		// TODO Auto-generated method stub
		
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}

	@Override
	public boolean reverseCorrectIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reverseAccrualIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reActivateIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSummary() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDocumentInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File createPDF() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProcessMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getDoc_User_ID() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getC_Currency_ID() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BigDecimal getApprovalAmt() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public MUMReplenishLine[] m_lines = null;
	
	public MUMReplenishLine[] getLines()
	{
		if (m_lines != null) {
			m_lines = null; // reset m_lines to null
		}
		
		//red1 - FR: [ 2214883 ] Remove SQL code and Replace for Query  
 	 	final String whereClause = I_UM_ReplenishLine.COLUMNNAME_UM_Replenish_ID+"=?";
	 	List <MUMReplenishLine> list = new Query(getCtx(), MUMReplenishLine.Table_Name, whereClause, get_TrxName())
			.setParameters(get_ID())
			.setOrderBy(I_UM_ReplenishLine.COLUMNNAME_LineNo)
			.list();
	 	//  red1 - end -

		m_lines = new MUMReplenishLine[list.size ()];
		list.toArray (m_lines);
		return m_lines;
	}	//	getLines
	
}
