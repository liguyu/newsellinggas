package com.aote.bts;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.aote.rs.BankTransService;



/**
 * 模糊查询协议
 * @author grain
 *
 */
public class ProtocolHandler3001 extends ProtocolHandler 
{
	static Logger log = Logger.getLogger(ProtocolHandler3001.class);

	@Override
	public void execute()
	{
		try
		{
			fillHeader();
			String userType = request.getString("USER_TYPE");
			String condition = request.getString("QUERY_STRING");
			String sql = "from t_userfiles where ";
			if(userType.equals("机表用户"))
				sql += "f_gasmeterstyle='机表'";
			else
				sql += "f_gasmeterstyle='卡表'";
			sql += " and (f_address like '%" + condition + "%' " + 
			" or f_cardid like '%" + condition + "%' " +
			" or f_username like '%" + condition + "%' " +
			" or f_idnumber like '%" + condition + "%' " +
			" or f_phone like '%" + condition + "%' " +
			 " )";	
			//查询用户基本信息
			List<Map<String, Object>> list = (List<Map<String, Object>>)hibernateTemplate.executeFind(new HibernateCall(sql, 0, 10));
			JSONArray users = new JSONArray();
			for(Map<String, Object> row : list)
			{
				JSONObject map = new JSONObject();
				map.put("USER_ID", row.get("f_userid"));
				map.put("MANUFACTURE_ID", BankTransService.encodeChinese(row.get("f_gasmetermanufacturers")));
				map.put("USER_NAME", BankTransService.encodeChinese(row.get("f_username")));
				map.put("USER_ADDRESS", BankTransService.encodeChinese(row.get("f_address")));
				map.put("USER_TYPE", BankTransService.encodeChinese(row.get("f_usertype")));
				map.put("USER_ID_NO", row.get("f_idnumber"));
				map.put("PHONE", BankTransService.toFen(row.get("f_phone")));
				users.put(map);
			}
			
			response.put("USERS", users);
			response.put("USER_COUNT", list.size());

			response.put("ACK_TRANS_SN", UUID.randomUUID().toString().replace("-", ""));
			response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_NO_ERROR);
			response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_NO_ERROR));

			//校验码
			fillMD5();
		}
		catch(Exception e)
		{
			log.debug(e.toString());
			try {
				BankTransService.emptyResult(response);
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_USER_QUERY);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_USER_QUERY));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}

	
	private void fillMD5() throws JSONException 
	{
		StringBuilder pt = new StringBuilder();
		pt.append(response.getString(BankTransService.TRANS_CODE));
		int cnt = response.getInt("USER_COUNT");
		pt.append(cnt);
		if(cnt > 0)
		{
			JSONArray list = (JSONArray)response.get("USERS");
			for(int i=0; i<list.length(); i++)
			{
				JSONObject map = list.getJSONObject(i);
				pt.append(map.get("USER_ID"));
				pt.append(map.get("MANUFACTURE_ID"));
				pt.append(map.get("USER_NAME"));
				pt.append(map.get("USER_ADDRESS"));
				pt.append(map.get("USER_TYPE"));
				pt.append(map.get("USER_ID_NO"));
				pt.append(map.get("PHONE"));
			}
		}
		response.put(BankTransService.MAC, this.getMD5(pt.toString()));
	}


	/**
	 * 发送请求的md5是否正常
	 * @throws JSONException 
	 */
	@Override
	public boolean isRequestMD5Correct() throws JSONException
	{
		String plainText = request.getString(BankTransService.TRANS_CODE) + request.getString("USER_TYPE") + request.getString("QUERY_STRING");
		String md5 = this.getMD5(plainText);
		return md5.equals(request.getString(BankTransService.MAC));
	}

	@Override
	public boolean isPacketSemanticallyCorrect() 
	{
		if(!super.isPacketSemanticallyCorrect())
			return false;
		if(request.isNull("USER_TYPE"))
			return false;
		if(request.isNull("QUERY_STRING"))
			return false;
		return true;
	}


}
