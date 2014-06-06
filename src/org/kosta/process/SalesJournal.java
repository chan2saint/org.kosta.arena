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
package org.kosta.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;

import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MElementValue;
import org.compiere.model.MPeriod;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Language;


/**
 *	Sales Journal
 *	
 *  @author Surya Sentosa
 *
 *  @author Copy java from TrialBalance.java
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see http://sourceforge.net/tracker2/?func=detail&atid=879335&aid=2520591&group_id=176962 
 *  @version $Id:  $
 */
public class SalesJournal extends SvrProcess
{
	/** AcctSchame Parameter			*/
	private int					p_C_AcctSchema_ID = 0;
	/**	Period Parameter				*/
	private int					p_C_Period_ID = 0;
	private Timestamp			p_DateAcct_From = null;
	private Timestamp			p_DateAcct_To = null;
	/**	Org Parameter					*/
	private int					p_AD_Org_ID = 0;
	/**	Account Parameter				*/
	private int					p_Account_ID = 0;
	private String				p_AccountValue_From = null;
	private String				p_AccountValue_To = null;
	/**	BPartner Parameter				*/
	private int					p_C_BPartner_ID = 0;
	/**	Product Parameter				*/
	private int					p_M_Product_ID = 0;
	/**	Project Parameter				*/
	private int					p_C_Project_ID = 0;
	/**	Activity Parameter				*/
	private int					p_C_Activity_ID = 0;
	/**	SalesRegion Parameter			*/
	private int					p_C_SalesRegion_ID = 0;
	/**	Campaign Parameter				*/
	private int					p_C_Campaign_ID = 0;
	/** Posting Type					*/
	private String				p_PostingType = "A";
	/** Hierarchy						*/
	private int					p_PA_Hierarchy_ID = 0;
	
	private int					p_AD_OrgTrx_ID = 0;
	private int					p_C_LocFrom_ID = 0;
	private int					p_C_LocTo_ID = 0;
	private int					p_User1_ID = 0;
	private int					p_User2_ID = 0;
	
	
	/**	Parameter Where Clause			*/
	private StringBuffer		m_parameterWhere = new StringBuffer();
	/**	Account							*/ 
	private MElementValue 		m_acct = null;
	
	/**	Start Time						*/
	private long 				m_start = System.currentTimeMillis();
	/**	Insert Statement				*/
	private static String		s_insert = "INSERT INTO T_SalesJournal "
		+ "(AD_PInstance_ID, Fact_Acct_ID,"
		+ " AD_Client_ID, AD_Org_ID, Created,CreatedBy, Updated,UpdatedBy,"
		+ " C_AcctSchema_ID, Account_ID, AccountValue, DateTrx, DateAcct, C_Period_ID,"
		+ " AD_Table_ID, Record_ID, Line_ID,"
		+ " GL_Category_ID, GL_Budget_ID, C_Tax_ID, M_Locator_ID, PostingType,"
		+ " C_Currency_ID, AmtSourceDr, AmtSourceCr, AmtSourceBalance,"
		+ " AmtAcctDr, AmtAcctCr, AmtAcctBalance, C_UOM_ID, Qty,"
		+ " M_Product_ID, C_BPartner_ID, AD_OrgTrx_ID, C_LocFrom_ID,C_LocTo_ID,"
		+ " C_SalesRegion_ID, C_Project_ID, C_Campaign_ID, C_Activity_ID,"
		+ " User1_ID, User2_ID, A_Asset_ID, Description)";
	private static String		s_account = "(select c_elementvalue_id from c_elementvalue "
			+ " where name in ('Cash Register ( IDR )', 'Cash In Transit - AMEX', 'Cash In Transit - BCA', 'Cash In Transit - Mandiri' "
			+ " , 'Others Discount', 'Advance From Customer ( Cash )', 'Other Receivable', 'Compliment Guest', 'Discount Sign - Compliment Owner', "
			+ " 'Discount Sign - Compliment Staff', 'Compliment Voucher', 'Voucher for Sale', 'Misc Other Expense', 'Sales Kitchen', "
			+ " 'Sales Bar', 'Sales Delika', 'Sales Tatsuya', 'Sales Other', 'Other Revenue', 'A/R SITE')) ";
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		StringBuffer sb = new StringBuffer ("AD_PInstance_ID=")
			.append(getAD_PInstance_ID());
		//	Parameter
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null && para[i].getParameter_To() == null)
				;
			else if (name.equals("C_AcctSchema_ID"))
				p_C_AcctSchema_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("DateAcct"))
			{
				p_DateAcct_From = (Timestamp)para[i].getParameter();
				p_DateAcct_To = (Timestamp)para[i].getParameter_To();
			}
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("PostingType"))
				p_PostingType = (String)para[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		//	Mandatory C_AcctSchema_ID
		m_parameterWhere.append("C_AcctSchema_ID=").append(p_C_AcctSchema_ID);
		
		if (p_AD_Org_ID != 0)
			m_parameterWhere.append(" AND AD_ORG_ID=").append(p_AD_Org_ID);
		
		m_parameterWhere.append(" AND PostingType='").append(p_PostingType).append("'");
		//
		setDateAcct();
		sb.append(" - DateAcct ").append(p_DateAcct_From).append("-").append(p_DateAcct_To);
		sb.append(" - Where=").append(m_parameterWhere);
		if (log.isLoggable(Level.FINE)) log.fine(sb.toString());
	}	//	prepare

	/**
	 * 	Set Start/End Date of Report - if not defined current Month
	 */
	private void setDateAcct()
	{
		//	Date defined
		if (p_DateAcct_From != null)
		{
			if (p_DateAcct_To == null)
				p_DateAcct_To = new Timestamp (System.currentTimeMillis());
			return;
		}
		//	Get Date from Period
		if (p_C_Period_ID == 0)
		{
		   GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
		   cal.setTimeInMillis(System.currentTimeMillis());
		   cal.set(Calendar.HOUR_OF_DAY, 0);
		   cal.set(Calendar.MINUTE, 0);
		   cal.set(Calendar.SECOND, 0);
		   cal.set(Calendar.MILLISECOND, 0);
		   cal.set(Calendar.DAY_OF_MONTH, 1);		//	set to first of month
		   p_DateAcct_From = new Timestamp (cal.getTimeInMillis());
		   cal.add(Calendar.MONTH, 1);
		   cal.add(Calendar.DAY_OF_YEAR, -1);		//	last of month
		   p_DateAcct_To = new Timestamp (cal.getTimeInMillis());
		   return;
		}

		String sql = "SELECT StartDate, EndDate FROM C_Period WHERE C_Period_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, p_C_Period_ID);
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				p_DateAcct_From = rs.getTimestamp(1);
				p_DateAcct_To = rs.getTimestamp(2);
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}	//	setDateAcct

	
	/**************************************************************************
	 *  Perform process.
	 *  @return Message to be translated
	 */
	protected String doIt()
	{
		//createBalanceLine();
		createDetailLines();

	//	int AD_PrintFormat_ID = 134;
	//	getProcessInfo().setTransientObject (MPrintFormat.get (getCtx(), AD_PrintFormat_ID, false));

		if (log.isLoggable(Level.FINE)) log.fine((System.currentTimeMillis() - m_start) + " ms");
		return "";
	}	//	doIt


	private void createDetailLines()
	{
		StringBuilder sql = new StringBuilder (s_insert);
		//	(AD_PInstance_ID, Fact_Acct_ID,
		sql.append(" SELECT ").append(getAD_PInstance_ID()).append(",null,");
		//	AD_Client_ID, AD_Org_ID, Created,CreatedBy, Updated,UpdatedBy,
		sql.append(getAD_Client_ID()).append(",AD_Org_ID,null,null, null,null,");
		//	C_AcctSchema_ID, Account_ID, DateTrx, AccountValue, DateAcct, C_Period_ID,
		sql.append("C_AcctSchema_ID, Account_ID, null, null, null, null,");
		//	AD_Table_ID, Record_ID, Line_ID,
		sql.append("null, null, null,");
		//	GL_Category_ID, GL_Budget_ID, C_Tax_ID, M_Locator_ID, PostingType,
		sql.append("null, null, null, null, 'A',");
		//	C_Currency_ID, AmtSourceDr, AmtSourceCr, AmtSourceBalance, --add by Surya, aggregate function
		sql.append("null, sum(AmtSourceDr),sum(AmtSourceCr), sum(AmtSourceDr-AmtSourceCr),");
		//	AmtAcctDr, AmtAcctCr, AmtAcctBalance, C_UOM_ID, Qty,
		sql.append(" sum(AmtAcctDr),sum(AmtAcctCr), sum(AmtAcctDr-AmtAcctCr), null,0,");
		//	M_Product_ID, C_BPartner_ID, AD_OrgTrx_ID, C_LocFrom_ID,C_LocTo_ID,
		sql.append ("null, null, null, null,null,");
		//	C_SalesRegion_ID, C_Project_ID, C_Campaign_ID, C_Activity_ID,
		sql.append ("null, null, null, null,");
		//	User1_ID, User2_ID, A_Asset_ID, Description)
		sql.append ("null, null, null, null");
		//
		sql.append(" FROM Fact_Acct WHERE AD_Client_ID=").append(getAD_Client_ID())
			.append (" AND ").append(m_parameterWhere)
			.append(" AND DateAcct >= ").append(DB.TO_DATE(p_DateAcct_From, true))
			.append(" AND TRUNC(DateAcct) <= ").append(DB.TO_DATE(p_DateAcct_To, true))
			.append(" AND ACCOUNT_ID in " + s_account)
			.append(" GROUP BY C_AcctSchema_ID, AD_Org_ID, Account_ID");
		//
		int no = DB.executeUpdate(sql.toString(), get_TrxName());
		if (no == 0)
			if (log.isLoggable(Level.FINE)) log.fine(sql.toString());
		if (log.isLoggable(Level.FINE)) log.fine("#" + no + " (Account_ID=" + p_Account_ID + ")");
		
		//	Update AccountValue
		String sql2 = "UPDATE T_SalesJournal tb SET AccountValue = "
			+ "(SELECT Value FROM C_ElementValue ev WHERE ev.C_ElementValue_ID=tb.Account_ID), DateAcct='" + p_DateAcct_From + "' "
			+ "WHERE tb.Account_ID IS NOT NULL AND tb.AD_PInstance_ID = " + getAD_PInstance_ID();
		no = DB.executeUpdate(sql2, get_TrxName());
		if (no > 0)
			if (log.isLoggable(Level.FINE)) log.fine("Set AccountValue #" + no);
		
	}	//	createDetailLines

}	//	TrialBalance
