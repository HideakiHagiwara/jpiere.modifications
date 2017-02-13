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
 *	JPIERE-0287
 *  Expiere Cache Reset of Table
 *
 * 	@author 	Hideaki Hagiwara
 */
public class CacheReset_ExpireTable extends SvrProcess 
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
		int totalRemainCache = 0;
		int totalDeleteCache = 0;
		int objectCounter = 0;

		int remainCachecount = 0;
		int beforeCachecount = 0;
		int deleteCount = 0;
		
		
		if(p_AD_Table_ID == 0)
		{
			
			addBufferLog(0, null, null, "Process Log - Expiere Cache Reset of Table", MTable.getTable_ID("AD_PInstance"), getAD_PInstance_ID());	
			
			int[] tableIDs = PO.getAllIDs("AD_Table", null, get_TrxName());
			for(int i = 0; i < tableIDs.length; i++)
			{
				remainCachecount = 0;
				beforeCachecount = 0;
				deleteCount = 0;
				
				table = new MTable(getCtx(), tableIDs[i], null);
//				if(table.getTableName().equals("AD_Tab"))//ignore AD_Tab table
//					continue;
				
				beforeCachecount = beforeCacheCount(table.getTableName());
				remainCachecount = ExpireCacheReset(table.getTableName());
				deleteCount = beforeCachecount - remainCachecount;
				totalRemainCache = totalRemainCache + remainCachecount;
				totalDeleteCache = totalDeleteCache + deleteCount;
				if(remainCachecount > 0)
					addLog("Delete Cache # " + deleteCount + " Remain Cache # " + remainCachecount + " at " + table.getTableName());
				
				objectCounter++;
			}
			
			/**
			 * Other object of Table
			 */
			//element
			remainCachecount = 0;
			beforeCachecount = 0;
			beforeCachecount = beforeCacheCount("element");
			remainCachecount = ExpireCacheReset("element");
			deleteCount = beforeCachecount - remainCachecount;
			totalRemainCache = totalRemainCache + remainCachecount;
			totalDeleteCache = totalDeleteCache + deleteCount;
			if(remainCachecount > 0)
				addLog("Delete Cache # " + deleteCount + " Remain Cache # " + remainCachecount + " at element");
			
			//po_trl
			remainCachecount = 0;
			beforeCachecount = 0;
			beforeCachecount = beforeCacheCount("po_trl");
			remainCachecount = ExpireCacheReset("po_trl");
			deleteCount = beforeCachecount - remainCachecount;
			totalRemainCache = totalRemainCache + remainCachecount;
			totalDeleteCache = totalDeleteCache + deleteCount;
			if(remainCachecount > 0)
				addLog("Delete Cache # " + deleteCount + " Remain Cache # " + remainCachecount + " at element");
			
		}else{
			table = MTable.get(getCtx(), p_AD_Table_ID);
			beforeCachecount = beforeCacheCount(table.getTableName());
			remainCachecount = ExpireCacheReset(table.getTableName());
			String msg = "Delete Cache # " + (beforeCachecount - remainCachecount) + " Remain Cache # " + remainCachecount + " at " + table.getTableName();
			addBufferLog(0, null, null, msg, MTable.getTable_ID("AD_PInstance"), getAD_PInstance_ID());		 
		}
		
		
		return "Total Delete Cache # "+totalDeleteCache +"  Total Remain Cache  # " + totalRemainCache + " /  Table Object  # " + objectCounter;
	}	//	doIt
	
	private int ExpireCacheReset(String tableName)
	{
		IServiceHolder<IClusterService> holder = Service.locator().locate(IClusterService.class);
		IClusterService service = holder.getService();
		if (service != null) {			
			ResetExpireCacheCallable callable = new ResetExpireCacheCallable(tableName);
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
	
	private int beforeCacheCount(String tableName)
	{
		IServiceHolder<IClusterService> holder = Service.locator().locate(IClusterService.class);
		IClusterService service = holder.getService();
		if (service != null) {			
			CacheCountCallable callable = new CacheCountCallable(tableName);
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
