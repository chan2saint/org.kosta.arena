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
 * Contributor(s): Carlos Ruiz - globalqss                                    *
 *****************************************************************************/
package org.kosta.arena.component;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.kosta.model.MUMInventory;
import org.kosta.model.MUMInventoryLine;
import org.kosta.model.MUMOrder;
import org.kosta.model.MUMReplenish;
import org.kosta.model.MUMReplenishLine;


public class ArenaModelFactory implements IModelFactory {
	CLogger log = CLogger.getCLogger(ArenaModelFactory.class);
	
	@Override
	public Class<?> getClass(String tableName) {
		// TODO Auto-generated method stub
		
		if ( tableName.equals(MUMReplenish.Table_Name))
		{
			return MUMReplenish.class;
		}
		else if ( tableName.equals(MUMReplenishLine.Table_Name))
		{
			return MUMReplenishLine.class;
		}
		else if ( tableName.equals(MUMInventory.Table_Name))
		{
			return MUMInventory.class;
		}
		else if ( tableName.equals(MUMInventoryLine.Table_Name))
		{
			return MUMInventoryLine.class;
		}
		/*
		else if ( tableName.equals(MUMOrder.Table_Name))
		{
			return MUMOrder.class;
		}
		*/
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		// TODO Auto-generated method stub
		
		Class<?> clazz = getClass(tableName);
		if (clazz == null)
			return null;

		PO model = null;
		Constructor<?> constructor = null;

		try {
			constructor = clazz.getDeclaredConstructor(new Class[] { Properties.class, int.class, String.class });
			model = (PO) constructor.newInstance(new Object[] { Env.getCtx(), new Integer(Record_ID), trxName });
		} catch (Exception e) {
			if (log.isLoggable(Level.WARNING))
				log.warning(String.format("Plugin: %s -> Class can not be instantiated for table: %s", "Kosta", tableName));
		}

		return model;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		// TODO Auto-generated method stub
		
		Class<?> clazz = getClass(tableName);
		if (clazz == null)
			return null;

		PO model = null;
		Constructor<?> constructor = null;

		try {
			constructor = clazz.getDeclaredConstructor(new Class[] { Properties.class, ResultSet.class, String.class });
			model = (PO) constructor.newInstance(new Object[] { Env.getCtx(), rs, trxName });
		} catch (Exception e) {
			if (log.isLoggable(Level.WARNING))
				log.warning(String.format("Plugin: %s -> Class can not be instantiated for table: %s", "Kosta", tableName));
		}

		return model;
	}

	
}
