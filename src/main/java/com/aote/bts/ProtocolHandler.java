package com.aote.bts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.ResultTransformer;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.aote.rs.BankTransService;



public abstract class ProtocolHandler {
	JSONObject request;
	JSONObject response;
	HibernateTemplate hibernateTemplate;
	ApplicationContext appContext;
	
	public abstract void execute();
	
	public abstract boolean isRequestMD5Correct() throws JSONException;
	
	/**
	 * ����MD5�ִ�
	 * @param data
	 * @return
	 */
	protected String getMD5(String data)
	{
		return DigestUtils.md5Hex(data);
	}

	/**
	 * ���Э��ͷ
	 * @param result
	 * @param cmdObj
	 */
	protected void fillHeader()
	{
		try 
		{
			fillField(response, request, BankTransService.TRANS_CODE);
			fillField(response, request, BankTransService.TRANS_SN);
			fillField(response, request, BankTransService.BANK_DATE);
			fillField(response, request, BankTransService.BANK_TIME);
			fillField(response, request, BankTransService.BANK_ID);
			fillField(response, request, BankTransService.TELLER_ID);
			fillField(response, request, BankTransService.CHANNEL_ID);
			fillField(response, request, BankTransService.DEVICE_ID);
		} 
		//�Ѿ�����Э�飬����
		catch (JSONException e) 
		{
		}
	}

	/**
	 * �����ֶ�
	 * @param result
	 * @param cmdObj
	 * @param key
	 * @throws JSONException
	 */
	private void fillField(JSONObject result, JSONObject cmdObj, String key) throws JSONException
	{
		result.put(key, cmdObj.optString(key));
	}
	
	public boolean isPacketSemanticallyCorrect()
	{
		if(request.isNull(BankTransService.TRANS_CODE))
			return false;
		if(request.isNull(BankTransService.TRANS_SN))
			return false;
		if(request.isNull(BankTransService.BANK_DATE))
			return false;
		if(request.isNull(BankTransService.BANK_TIME))
			return false;
		if(request.isNull(BankTransService.BANK_ID))
			return false;
		if(request.isNull(BankTransService.TELLER_ID))
			return false;
		if(request.isNull(BankTransService.CHANNEL_ID))
			return false;
		if(request.isNull(BankTransService.DEVICE_ID))
			return false;
		if(request.isNull(BankTransService.MAC))
			return false;
		return true;
	}
	
	/**
	 * �ж��ǲ����ظ�����
	 * @return
	 * @throws JSONException 
	 */
	public boolean isDuplicateRequest() throws JSONException 
	{
		String MAC = request.getString(BankTransService.MAC);
		String sql = "from t_bank_trans where MAC = '" + MAC + "' and TRANS_SN='" + request.getString(BankTransService.TRANS_SN) + "'";
		List list = hibernateTemplate.find(sql);
		if(list.size()>0)
			return true;
		else
			return false;
	}
	
	
	protected int getMonthlyMaintenanceFee(HibernateTemplate hibernateTemplate) 
	{
		List list = hibernateTemplate.find("from t_singlevalue where name='������ά����'" );
		Map<String, Object> row = (Map<String, Object>)list.get(0);
		return (int)(Integer.parseInt(row.get("value").toString()));
	}

	protected void calculateMaintenanceFee(Date ddt, int monthlyFee) throws Exception
	{
		//�ڸ��������ۼ���ݣ�������ڵ�ǰʱ�䣬��ֹͣ
		Calendar cal = Calendar.getInstance();
		cal.setTime(ddt);
		Date preDate = ddt;
		//cal.add(Calendar.YEAR, 1);
		Date dt = new Date();
		JSONArray list = new JSONArray();
		while(dt.after(cal.getTime()))
		{
			preDate = cal.getTime();
			cal.add(Calendar.YEAR, 1);
			JSONObject map = new JSONObject();
			map.put("MAINTENANCE_FEE_SPAN", BankTransService.formatDate(preDate) + "~" + BankTransService.formatDate(cal.getTime()));
			map.put("MAINTENANCE_FEE", 12 * monthlyFee);
			list.put(map);
		}
		response.put("MAINTENANCE_FEE_ROWS", list);
		response.put("MAINTENANCE_FEE_TILL", BankTransService.encodeChinese(BankTransService.formatDate(ddt)));
		response.put("MAINTENANCE_FEE_COUNT", list.length());
	}

	
}

// ִ��sql��ҳ��ѯ���������ʽ��������
class HibernateSQLCall implements HibernateCallback {
	String sql;
	int page;
	int rows;
	//��ѯ���ת����������ת����Map�ȡ�
	public ResultTransformer transformer = null;
	
	public HibernateSQLCall(String sql, int page, int rows) {
		this.sql = sql;
		this.page = page;
		this.rows = rows;
	}

	public Object doInHibernate(Session session) {
		Query q = session.createSQLQuery(sql);
		//��ת����������ת����
		if(transformer != null) {
			q.setResultTransformer(transformer);
		}
		List result = q.setFirstResult(page * rows).setMaxResults(rows).list();
		return result;
	}
}

// ִ�з�ҳ��ѯ
class HibernateCall implements HibernateCallback {
	String hql;
	int page;
	int rows;

	public HibernateCall(String hql, int page, int rows) {
		this.hql = hql;
		this.page = page;
		this.rows = rows;
	}

	public Object doInHibernate(Session session) {
		Query q = session.createQuery(hql);
		List result = q.setFirstResult(page * rows).setMaxResults(rows)
				.list();
		return result;
	}
}
