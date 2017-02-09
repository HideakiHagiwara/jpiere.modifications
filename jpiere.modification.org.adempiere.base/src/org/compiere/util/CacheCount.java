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

import org.compiere.model.MTable;
import org.compiere.process.SvrProcess;
import org.compiere.util.CacheMgt;

/**
 *	JPIERE-0284
 *  Cache Count
 *
 * 	@author 	Hideaki Hagiwara
 * 
 */
public class CacheCount extends SvrProcess 
{
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
	}	//	prepare


	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception
	 */
	protected String doIt() throws java.lang.Exception
	{	
		CacheMgt cacheMgt = CacheMgt.get();
		addBufferLog(0, null, null, "Cache Count Process Log", MTable.getTable_ID("AD_PInstance"), getAD_PInstance_ID());
		
		int counter = 0;
		int total = 0;
		CacheInterface[] instances = cacheMgt.getInstancesAsArray();
		for (CacheInterface stored : instances)
		{
			if (stored != null && stored instanceof CCache)
			{
				CCache<?, ?> cc = (CCache<?, ?>)stored;
				if (cc.getTableName() != null)
					addLog("Table Name : " + cc.getTableName() + " - #" + cc.sizeNoExpire() + " ／ ExpireMinutes - #" + cc.getExpireMinutes());
				else if(cc.getName() != null)
					addLog("Name : " + cc.getName() + " - #" + cc.sizeNoExpire() + " ／ ExpireMinutes - #" + cc.getExpireMinutes());
				else
					addLog("Null Cache - #" + cc.sizeNoExpire() + " ／ ExpireMinutes - #" + cc.getExpireMinutes());
				
				total += cc.sizeNoExpire();
				counter++;
			}
		}//for
		
		
		return "Cache Count -> Cache Object - #" + counter + "  Total Cache - #" + total;
	}	//	doIt

}	//	CacheReset
