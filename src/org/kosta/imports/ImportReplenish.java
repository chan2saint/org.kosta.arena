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
 * Contributor: Carlos Ruiz - globalqss                                       *
 *****************************************************************************/
package org.kosta.imports;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.exceptions.DBException;
import org.adempiere.model.ImportValidator;
import org.adempiere.process.ImportProcess;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MContactInterest;
import org.compiere.model.MLocation;
import org.compiere.model.MReplenish;
import org.compiere.model.MUser;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.Query;
import org.compiere.model.X_I_BPartner;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.kosta.model.I_I_Replenish;
import org.kosta.model.X_I_Replenish;

/**
 *	Import BPartners from I_Replenish
 *
 * 	@author 	Jorg Janke
 * 	@version 	$Id: ImportBPartner.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 * 
 * @author Teo Sarca, www.arhipac.ro
 * 			<li>FR [ 2788074 ] ImportBPartner: add IsValidateOnly option
 * 				https://sourceforge.net/tracker/?func=detail&aid=2788074&group_id=176962&atid=879335
 * 			<li>FR [ 2788278 ] Data Import Validator - migrate core processes
 * 				https://sourceforge.net/tracker/?func=detail&aid=2788278&group_id=176962&atid=879335
 */
public class ImportReplenish extends SvrProcess
implements ImportProcess
{
	/**	Client to be imported to		*/
	private int				m_AD_Client_ID = 0;
	/**	Delete old Imported				*/
	private boolean			m_deleteOldImported = false;
	/**	Only validate, don't import		*/
	private boolean			p_IsValidateOnly = false;

	/** Effective						*/
	private Timestamp		m_DateValue = null;

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("AD_Client_ID"))
				m_AD_Client_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("DeleteOldImported"))
				m_deleteOldImported = "Y".equals(para[i].getParameter());
			else if (name.equals("IsValidateOnly"))
				p_IsValidateOnly = para[i].getParameterAsBoolean();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		if (m_DateValue == null)
			m_DateValue = new Timestamp (System.currentTimeMillis());
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception
	 */
	protected String doIt() throws java.lang.Exception
	{
		StringBuilder sql = null;
		int no = 0;
		String clientCheck = getWhereClause();

		//	****	Prepare	****

		//	Delete Old Imported
		if (m_deleteOldImported)
		{
			sql = new StringBuilder ("DELETE I_Replenish ")
					.append("WHERE I_IsImported='Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			if (log.isLoggable(Level.FINE)) log.fine("Delete Old Impored =" + no);
		}

		//	Set Client, Org, IsActive, Created/Updated
		sql = new StringBuilder ("UPDATE I_Replenish ")
				.append("SET AD_Client_ID = COALESCE (AD_Client_ID, ").append(m_AD_Client_ID).append("),")
						.append(" AD_Org_ID = COALESCE (AD_Org_ID, 0),")
						.append(" IsActive = COALESCE (IsActive, 'Y'),")
						.append(" Created = COALESCE (Created, SysDate),")
						.append(" CreatedBy = COALESCE (CreatedBy, 0),")
						.append(" Updated = COALESCE (Updated, SysDate),")
						.append(" UpdatedBy = COALESCE (UpdatedBy, 0),")
						.append(" I_ErrorMsg = ' ',")
						.append(" I_IsImported = 'N' ")
						.append("WHERE I_IsImported<>'Y' OR I_IsImported IS NULL");
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Reset=" + no);

		// update
		// product by value
		sql = new StringBuilder ("UPDATE I_Replenish i ")
				.append("SET m_product_id =(SELECT m_product_id FROM m_product p")
				.append(" WHERE i.productvalue = p.value AND p.AD_Client_ID=i.AD_Client_ID) ")
				.append(" WHERE m_product_id is null ")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Product =" + no);
		
		// product by name
		sql = new StringBuilder ("UPDATE I_Replenish i ")
				.append("SET m_product_id =(SELECT m_product_id FROM m_product p")
				.append(" WHERE i.productName = p.name AND p.AD_Client_ID=i.AD_Client_ID) ")
				.append(" WHERE m_product_id is null ")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Product =" + no);
		// product
		sql = new StringBuilder ("UPDATE I_Replenish ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product, ' ")
				.append(" WHERE m_product_id IS NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.CONFIG)) log.config("Invalid Product =" + no);
		
		// warehouse by value
		sql = new StringBuilder ("UPDATE I_Replenish i ")
				.append("SET M_Warehouse_ID =(SELECT M_Warehouse_ID FROM m_warehouse p")
				.append(" WHERE i.warehousevalue = p.value AND p.AD_Client_ID=i.AD_Client_ID) ")
				.append(" WHERE M_Warehouse_ID is null ")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Warehouse =" + no);
		
		// warehouse by name
		sql = new StringBuilder ("UPDATE I_Replenish i ")
				.append("SET M_Warehouse_ID =(SELECT M_Warehouse_ID FROM m_warehouse p")
				.append(" WHERE i.warehouseName = p.name AND p.AD_Client_ID=i.AD_Client_ID) ")
				.append(" WHERE M_Warehouse_ID is null ")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.FINE)) log.fine("Warehouse =" + no);
		
		// warehouse
		sql = new StringBuilder ("UPDATE I_Replenish ")
				.append("SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Warehouse, ' ")
				.append(" WHERE M_Warehouse_ID IS NULL")
				.append(" AND I_IsImported<>'Y'").append(clientCheck);
		no = DB.executeUpdateEx(sql.toString(), get_TrxName());
		if (log.isLoggable(Level.CONFIG)) log.config("Invalid Warehouse =" + no);
		// end update
				
		commitEx();		
		
		if (p_IsValidateOnly)
		{
			return "Validated";
		}
		//	-------------------------------------------------------------------
		int noInsert = 0;
		int noUpdate = 0;

		sql = new StringBuilder ("SELECT * FROM I_Replenish ")
				.append("WHERE I_IsImported='N'").append(clientCheck);
		
		sql.append(" ORDER BY I_Replenish_ID");
		PreparedStatement pstmt =  null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			rs = pstmt.executeQuery();
			
			while ( rs.next()){
				X_I_Replenish iReplenish = new X_I_Replenish(getCtx(), rs, get_TrxName());
				
				String whereClause = " M_Product_ID = ? and M_Warehouse_ID = ? ";
				MReplenish replenish = new Query(
						getCtx(), MReplenish.Table_Name, whereClause, get_TrxName())
					.setParameters(iReplenish.getM_Product_ID() , iReplenish.getM_Warehouse_ID())
					.first();
				
				// create Replenish
				if ( replenish == null ){ 
					MReplenish NewReplenish = new MReplenish(getCtx(), 0, get_TrxName());
					
					NewReplenish.setM_Product_ID(iReplenish.getM_Product_ID());
					NewReplenish.setM_Warehouse_ID(iReplenish.getM_Warehouse_ID());
					NewReplenish.setReplenishType("1");
					NewReplenish.setLevel_Min(iReplenish.getLevel_Min());
					NewReplenish.setLevel_Max(iReplenish.getLevel_Max());
					NewReplenish.setQtyBatchSize(Env.ONE);
					
					NewReplenish.saveEx();
					noInsert++;
				}
				// end create 
				else {
					replenish.setM_Product_ID(iReplenish.getM_Product_ID());
					replenish.setM_Warehouse_ID(iReplenish.getM_Warehouse_ID());
					replenish.setReplenishType("1");
					replenish.setLevel_Min(iReplenish.getLevel_Min());
					replenish.setLevel_Max(iReplenish.getLevel_Max());
					replenish.setQtyBatchSize(Env.ONE);
					
					replenish.saveEx();
					noUpdate++;
				}
				// update import replenish
				iReplenish.setI_IsImported(true);
				iReplenish.setProcessed(true);
				iReplenish.setProcessing(false);
				iReplenish.saveEx();
				
				commitEx();				
			}
			DB.close(rs, pstmt);
		}
		catch (SQLException e)
		{
			rollback();
			//log.log(Level.SEVERE, "", e);
			throw new DBException(e, sql.toString());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
			//	Set Error to indicator to not imported
			sql = new StringBuilder ("UPDATE I_Replenish ")
					.append("SET I_IsImported='N', Updated=SysDate ")
					.append("WHERE I_IsImported<>'Y'").append(clientCheck);
			no = DB.executeUpdateEx(sql.toString(), get_TrxName());
			addLog (0, null, new BigDecimal (no), "@Errors@");
			addLog (0, null, new BigDecimal (noInsert), "@M_Replenish_UU@: @Inserted@");
			addLog (0, null, new BigDecimal (noUpdate), "@M_Replenish_UU@: @Updated@");
		}
		return "";
		
	}

	//@Override
	public String getWhereClause()
	{
		StringBuilder msgreturn = new StringBuilder(" AND AD_Client_ID=").append(m_AD_Client_ID);
		return msgreturn.toString();
	}

	@Override
	public String getImportTableName() {
		// TODO Auto-generated method stub
		return X_I_Replenish.Table_Name;
	}
	
}	//	ImportReplenish
