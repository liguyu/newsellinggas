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
	 * 生成MD5字串
	 * @param data
	 * @return
	 */
	protected String getMD5(String data)
	{
		return DigestUtils.md5Hex(data);
	}

	/**
	 * 填充协议头
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
		//已经检查过协议，忽略
		catch (JSONException e) 
		{
		}
	}

	/**
	 * 拷贝字段
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
	 * 判断是不是重复请求
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
		List list = hibernateTemplate.find("from t_singlevalue where name='民用月维护费'" );
		Map<String, Object> row = (Map<String, Object>)list.get(0);
		return (int)(Integer.parseInt(row.get("value").toString()));
	}

	protected void calculateMaintenanceFee(Date ddt, int monthlyFee) throws Exception
	{
		//在该日期上累加年份，如果大于当前时间，则停止
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

// 执行sql分页查询，结果集形式可以设置
class HibernateSQLCall implements HibernateCallback {
	String sql;
	int page;
	int rows;
	//查询结果转换器，可以转换成Map等。
	public ResultTransformer transformer = null;
	
	public HibernateSQLCall(String sql, int page, int rows) {
		this.sql = sql;
		this.page = page;
		this.rows = rows;
	}

	public Object doInHibernate(Session session) {
		Query q = session.createSQLQuery(sql);
		//有转换器，设置转换器
		if(transformer != null) {
			q.setResultTransformer(transformer);
		}
		List result = q.setFirstResult(page * rows).setMaxResults(rows).list();
		return result;
	}
}

// 执行分页查询
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
