package org.kosta.callout;

import java.math.BigDecimal;
import java.math.MathContext;
import java.security.Timestamp;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.IColumnCallout;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.GridTabWrapper;
import org.compiere.model.GridField;
import org.compiere.model.GridTab; 
import org.compiere.model.MProduct;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;


public class Callout_UM_Replenish implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (mField.getColumnName().equals("DatePromised"))  
			return checkMaxDate(ctx, WindowNo, mTab, mField,value);
		return null;
	}
	
	/** Logger					*/
	protected CLogger		log = CLogger.getCLogger(getClass());
	
	/**	Debug Steps			*/
	private boolean steps = false;
	
	public String checkMaxDate (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		java.sql.Timestamp getDatePromised = (java.sql.Timestamp)value;
		if (getDatePromised == null )
			return "";
		
		java.sql.Timestamp tglRequest = (java.sql.Timestamp)mTab.getValue("DateReplenish");
		if ( tglRequest == null ) throw new AdempiereException("\nTanggal DateReplenish harus diisi");
		
		if ( getDatePromised.before(tglRequest))
		{
			throw new AdempiereException("\nTanggal Promised harus melebihin tanggal Replenish");
		}
		else 
		{
			if ( getHour(getDatePromised) == 9 ||  getHour(getDatePromised) == 13 ) 
				;
			else
			{
				throw new AdempiereException("\nTanggal Promised cuma jam 9.00 AM dan jam 1.00 PM");
			}
		}
		
		return "";
	}
	
	private Integer getHour(java.sql.Timestamp date)
	{
		String tgl 			= date.toString();
		String[] listTgl 	= tgl.split(" ");
		String hour 		= listTgl[1].substring(0,2);
		
		return Integer.parseInt(hour);		
	}
	
	public String qtyNetto (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if ( value == null )
			return "";
		
		BigDecimal QtyEntered, QtyNetto;
		QtyNetto = (BigDecimal)mTab.getValue("QtyEnteredNetto");
		QtyEntered = (BigDecimal)mTab.getValue("QtyEntered");
		
		if ( QtyNetto.signum() < 0)
		{
			mTab.setValue("QtyEnteredNetto", QtyEntered);
			throw new AdempiereException("Error Minus");
		}
		else 
			log.warning("Qty Netto Aman");	
		
		return qty(ctx, WindowNo, mTab, mField, value);	
	}
	
	
	public String qtyMove (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if ( value == null)
			return "";
		int M_Product_ID = Env.getContextAsInt(ctx, WindowNo, "M_Product_ID");
		if (steps) log.warning("init - M_Product_ID=" + M_Product_ID + " - " );
		BigDecimal QtyOrdered = Env.ZERO;
		BigDecimal QtyEntered, QtyEnteredNetto;

		QtyEntered = (BigDecimal)mTab.getValue("QtyEntered");
		QtyEnteredNetto = QtyEntered;
		mTab.setValue("QtyEnteredNetto", QtyEnteredNetto);
		log.warning("Qty product Netto Inserted");	
		
		return "";
	}	//	qtyMove
	
	/**
	 *	InOut Line - Quantity.
	 *		- called from C_UOM_ID, QtyEntered, MovementQty
	 *		- enforces qty UOM relationship
	 *	@param ctx context
	 *	@param WindowNo window no
	 *	@param mTab tab model
	 *	@param mField field model
	 *	@param value new value
	 *	@return error message or ""
	 */
	public String qty (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (value == null)
			return "";

		int M_Product_ID = Env.getContextAsInt(ctx, WindowNo, "M_Product_ID");
		//	log.log(Level.WARNING,"qty - init - M_Product_ID=" + M_Product_ID);
		BigDecimal MovementQty, QtyEntered, QtyNetto,DifferentQty;
		
		QtyNetto = (BigDecimal)mTab.getValue("QtyEnteredNetto");
		QtyEntered = (BigDecimal)mTab.getValue("QtyEntered");
		
		if (QtyNetto == null)
			QtyNetto = (BigDecimal)mTab.getValue("QtyEntered");
		
		//DifferentQty = QtyEntered.subtract(QtyNetto);
		
		//	No Product
		if (M_Product_ID == 0)
		{
			QtyEntered = (BigDecimal)mTab.getValue("QtyEntered");
			mTab.setValue("MovementQty", QtyEntered);
		}
		//	UOM Changed - convert from Entered -> Product
		else if (mField.getColumnName().equals("C_UOM_ID"))
		{
			int C_UOM_To_ID = ((Integer)value).intValue();
			QtyEntered = (BigDecimal)mTab.getValue("QtyEntered");
			BigDecimal QtyEntered1 = QtyEntered.setScale(MUOM.getPrecision(ctx, C_UOM_To_ID), BigDecimal.ROUND_HALF_UP);
			if (QtyEntered.compareTo(QtyEntered1) != 0)
			{
				if (log.isLoggable(Level.FINE)) log.fine("Corrected QtyEntered Scale UOM=" + C_UOM_To_ID
					+ "; QtyEntered=" + QtyEntered + "->" + QtyEntered1);
				QtyEntered = QtyEntered1;
				mTab.setValue("QtyEntered", QtyEntered);
			}
			MovementQty = MUOMConversion.convertProductFrom (ctx, M_Product_ID,
				C_UOM_To_ID, QtyEntered);
			if (MovementQty == null)
				MovementQty = QtyEntered;
			boolean conversion = QtyEntered.compareTo(MovementQty) != 0;
			if (log.isLoggable(Level.FINE)) log.fine("UOM=" + C_UOM_To_ID
				+ ", QtyEntered=" + QtyEntered
				+ " -> " + conversion
				+ " MovementQty=" + MovementQty);
			Env.setContext(ctx, WindowNo, "UOMConversion", conversion ? "Y" : "N");
			mTab.setValue("MovementQty", MovementQty);
			mTab.setValue("QtyEnteredNetto", MovementQty);
		}
		//	No UOM defined
		else if (Env.getContextAsInt(ctx, WindowNo, "C_UOM_ID") == 0)
		{
			QtyEntered = (BigDecimal)mTab.getValue("QtyEntered");
			mTab.setValue("MovementQty", QtyEntered);
		}
		//	QtyEntered changed - calculate MovementQty
		else if (mField.getColumnName().equals("QtyEntered"))
		{
			int C_UOM_To_ID = Env.getContextAsInt(ctx, WindowNo, "C_UOM_ID");
			QtyEntered = (BigDecimal)value;
			BigDecimal QtyEntered1 = QtyEntered.setScale(MUOM.getPrecision(ctx, C_UOM_To_ID), BigDecimal.ROUND_HALF_UP);
			if (QtyEntered.compareTo(QtyEntered1) != 0)
			{
				if (log.isLoggable(Level.FINE)) log.fine("Corrected QtyEntered Scale UOM=" + C_UOM_To_ID
					+ "; QtyEntered=" + QtyEntered + "->" + QtyEntered1);
				QtyEntered = QtyEntered1;
				mTab.setValue("QtyEntered", QtyEntered);
			}
			MovementQty = MUOMConversion.convertProductFrom (ctx, M_Product_ID,
				C_UOM_To_ID, QtyEntered);
			if (MovementQty == null)
				MovementQty = QtyEntered;
			boolean conversion = QtyEntered.compareTo(MovementQty) != 0;
			if (log.isLoggable(Level.FINE)) log.fine("UOM=" + C_UOM_To_ID
				+ ", QtyEntered=" + QtyEntered
				+ " -> " + conversion
				+ " MovementQty=" + MovementQty);
			Env.setContext(ctx, WindowNo, "UOMConversion", conversion ? "Y" : "N");
			mTab.setValue("MovementQty", MovementQty);
			mTab.setValue("QtyEnteredNetto", MovementQty);
		}
		//		QtyEnteredNetto changed - calculate MovementQty
		else if (mField.getColumnName().equals("QtyEnteredNetto"))
		{
			int C_UOM_To_ID = Env.getContextAsInt(ctx, WindowNo, "C_UOM_ID");
			QtyEntered = (BigDecimal)value;
			BigDecimal QtyEntered1 = QtyEntered.setScale(MUOM.getPrecision(ctx, C_UOM_To_ID), BigDecimal.ROUND_HALF_UP);
			if (QtyEntered.compareTo(QtyEntered1) != 0)
			{
				if (log.isLoggable(Level.FINE)) log.fine("Corrected QtyEntered Scale UOM=" + C_UOM_To_ID
					+ "; QtyEntered=" + QtyEntered + "->" + QtyEntered1);
				QtyEntered = QtyEntered1;
				mTab.setValue("QtyEntered", QtyEntered);
			}
			
			MovementQty = MUOMConversion.convertProductFrom (ctx, M_Product_ID,
				C_UOM_To_ID, QtyEntered);
			if (MovementQty == null)
				MovementQty = QtyEntered;
			boolean conversion = QtyEntered.compareTo(MovementQty) != 0;
			if (log.isLoggable(Level.FINE)) log.fine("UOM=" + C_UOM_To_ID
				+ ", QtyEntered=" + QtyEntered
				+ " -> " + conversion
				+ " MovementQty=" + MovementQty);
			Env.setContext(ctx, WindowNo, "UOMConversion", conversion ? "Y" : "N");
			
			//mTab.setValue("MovementQty", MovementQty);
		}
		//	MovementQty changed - calculate QtyEntered (should not happen)
		else if (mField.getColumnName().equals("MovementQty"))
		{
			int C_UOM_To_ID = Env.getContextAsInt(ctx, WindowNo, "C_UOM_ID");
			MovementQty = (BigDecimal)value;
			int precision = MProduct.get(ctx, M_Product_ID).getUOMPrecision();
			BigDecimal MovementQty1 = MovementQty.setScale(precision, BigDecimal.ROUND_HALF_UP);
			if (MovementQty.compareTo(MovementQty1) != 0)
			{
				if (log.isLoggable(Level.FINE)) log.fine("Corrected MovementQty "
					+ MovementQty + "->" + MovementQty1);
				MovementQty = MovementQty1;
				mTab.setValue("MovementQty", MovementQty);
			}
			QtyEntered = MUOMConversion.convertProductTo (ctx, M_Product_ID,
				C_UOM_To_ID, QtyEntered);
			if (QtyEntered == null)
				QtyEntered = MovementQty;
			boolean conversion = MovementQty.compareTo(QtyEntered) != 0;
			if (log.isLoggable(Level.FINE)) log.fine("UOM=" + C_UOM_To_ID
				+ ", MovementQty=" + MovementQty
				+ " -> " + conversion
				+ " QtyEntered=" + QtyEntered);
			Env.setContext(ctx, WindowNo, "UOMConversion", conversion ? "Y" : "N");
			mTab.setValue("QtyEntered", QtyEntered);
		}
		//
		return "";
	}	//	qty
}
