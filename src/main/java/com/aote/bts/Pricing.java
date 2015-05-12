package com.aote.bts;

import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class Pricing implements IPricing{
	static Logger log = Logger.getLogger(Deposit.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;
	
	/**
	 * 处理维管费、气费、申请购气量、允许购气量、费用明细、其他费、总费用等
	 */
	public boolean pricing(String card_id, String dt, int gasAmount, Map<String, Object> map)
	{
		return true;
	}

	
}
