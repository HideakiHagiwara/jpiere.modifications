/******************************************************************************
 * Product: JPiere                                                            *
 * Copyright (C) Hideaki Hagiwara (h.hagiwara@oss-erp.co.jp)                  *
 *                                                                            *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY.                          *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * JPiere is maintained by OSS ERP Solutions Co., Ltd.                        *
 * (http://www.oss-erp.co.jp)                                                 *
 *****************************************************************************/
package org.compiere.util;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.adempiere.base.IServiceHolder;
import org.adempiere.base.Service;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.idempiere.distributed.IClusterMember;
import org.idempiere.distributed.IClusterService;

/**
 *	JPIERE-0285
 *  Reset Cache of Table
 *
 * 	@author 	Hideaki Hagiwara
 */
public class CacheReset_Table extends SvrProcess 
{
	int p_AD_Table_ID = 0;
	CacheMgt cacheMgt = CacheMgt.get();
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("AD_Table_ID"))
				p_AD_Table_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return Message to be translated
	 *  @throws Exception
	 */
	protected String doIt() throws java.lang.Exception
	{
		
		MTable table = null;
		int counter = 0;
		int total = 0;
		
		
		if(p_AD_Table_ID == 0)
		{
			
			addBufferLog(0, null, null, "Process Log - Cache Reset of Table", MTable.getTable_ID("AD_PInstance"), getAD_PInstance_ID());	
			
			int[] tableIDs = PO.getAllIDs("AD_Table", null, get_TrxName());
			int count = 0;
			for(int i = 0; i < tableIDs.length; i++)
			{
				count = 0;
				table = new MTable(getCtx(), tableIDs[i] ,null);
				count = cacheReset(table.getTableName());
				total = total + count;
				if(count > 0)
					addLog("Reset Cache - #" + count + " at Table Name : " + table.getTableName());
				
				counter++;
			}
			
			
			/**
			 * 
			 */
			count = cacheReset("element");
			total = total + count;
			if(count > 0)
				addLog("Reset Cache - #" + count + " at Table Name : " + "element");
			
			count = cacheReset("po_trl");
			total = total + count;
			if(count > 0)
				addLog("Reset Cache - #" + count + " at Table Name : " + "po_trl");
			
		}else{
			 table = MTable.get(getCtx(), p_AD_Table_ID);
			 int count =cacheReset(table.getTableName());
			String msg = "Cache Reset - Table: " + table.getTableName() +" #" + count;
			addBufferLog(0, null, null, msg, MTable.getTable_ID("AD_PInstance"), getAD_PInstance_ID());		 
		}
		
		
		return "Cache Count -> Total Cache  #" + total + " /  Table Object  #" + counter;
	}	//	doIt
	
	private int cacheReset(String tableName)
	{
		IServiceHolder<IClusterService> holder = Service.locator().locate(IClusterService.class);
		IClusterService service = holder.getService();
		if (service != null) {			
			ResetCacheCallable callable = new ResetCacheCallable(tableName, 0);
			Map<IClusterMember, Future<Integer>> futureMap = service.execute(callable, service.getMembers());
			if (futureMap != null) {
				int total = 0;
				try {
					Collection<Future<Integer>> results = futureMap.values();
					for(Future<Integer> future : results) {						
						Integer i = future.get();
						total += i.intValue();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				return total;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}
	
}	//	CacheReset
