package com.aote.bts;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.sun.jersey.api.spring.Autowire;

public class Deposit implements IDeposit 
{
	static Logger log = Logger.getLogger(Deposit.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;
	
	public void deposit(String userId, String dt, String tm, String payment) {
		log.debug("unimplemented.");
		
	}

}
