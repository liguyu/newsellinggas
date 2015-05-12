package com.aote.bts;

import org.codehaus.jettison.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateTemplate;


public class ProtocolHandlerFactory 
{

	public static ProtocolHandler get(ApplicationContext appContext, HibernateTemplate hibernateTemplate, String transCode, JSONObject request, JSONObject response) 
	{
		ProtocolHandler ph = null;
		if(transCode.equals("1001"))
			ph = new ProtocolHandler1001();
		else if(transCode.equals("1002"))
			ph = new ProtocolHandler1002();
		else if(transCode.equals("2001"))
			ph = new ProtocolHandler2001();
		else if(transCode.equals("2002"))
			ph = new ProtocolHandler2002();
		else if(transCode.equals("2003"))
			ph = new ProtocolHandler2003();
		else if(transCode.equals("2004"))
			ph = new ProtocolHandler2004();
		else if(transCode.equals("2005"))
			ph = new ProtocolHandler2005();
		else if(transCode.equals("2006"))
			ph = new ProtocolHandler2006();
		else if(transCode.equals("3001"))
			ph = new ProtocolHandler3001();
		ph.hibernateTemplate = hibernateTemplate;
		ph.request = request;
		ph.response = response;
		ph.appContext = appContext;
		return ph;
	}

}
