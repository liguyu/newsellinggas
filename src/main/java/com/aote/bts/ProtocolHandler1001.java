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
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.aote.rs.BankTransService;



/**
 * 查询机表用户信息协议处理
 * @author grain
 *
 */
public class ProtocolHandler1001 extends ProtocolHandler 
{
	static Logger log = Logger.getLogger(ProtocolHandler1001.class);

	@Override
	public void execute()
	{
		try
		{
			fillHeader();
			String userid = request.getString("USER_ID");
			
			//查询用户基本信息
			List list = hibernateTemplate.find("from t_userfiles where f_gasmeterstyle='机表' and f_userid = '" + userid + "'" );
			if(list.size() != 1)
			{
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_IC_METER_USER);
				response.put(BankTransService.RESPONSE_CODE, BankTransService.MSG.get(BankTransService.ERROR_IC_METER_USER));
				return;
			}		
			Map<String, Object> row = (Map<String, Object>)list.get(0);
			response.put("USER_ID", userid);
			response.put("USER_NAME", BankTransService.encodeChinese(row.get("f_username")));
			response.put("USER_TYPE", BankTransService.encodeChinese(row.get("f_usertype")));
			//金额到分
			response.put("BALANCE", BankTransService.toFen(row.get("f_zhye")));
			
			//维管费
			calculateMaintenanceFee((Date)row.get("f_beginfee"), getMonthlyMaintenanceFee(hibernateTemplate));
			
			//抄表欠费记录
			fillMeterReading(userid, hibernateTemplate);
			
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
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_MACHINE_METER_USER);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_MACHINE_METER_USER));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}

	
	private void fillMeterReading(String userid, HibernateTemplate hibernateTemplate) throws JSONException 
	{
		JSONArray readings = new JSONArray();
		List<Map<String, Object>> list = hibernateTemplate.find("from t_handplan where shifoujiaofei = '否' and f_userid =  '" + userid + "'" );
		for(Map<String, Object> row : list)
		{
			JSONObject map = new JSONObject();
			map.put("READING_DATE", BankTransService.formatDate((Date)row.get("f_inputdate")));
			map.put("LAST_READING", BankTransService.toInt(row.get("lastinputgasnum")));
			map.put("CUR_READING", BankTransService.toInt(row.get("lastrecord")));
			map.put("GAS_USED", BankTransService.toInt(row.get("oughtamount")));
			map.put("SHOULD_CHARGE", BankTransService.toFen(row.get("oughtfee")));
			map.put("ALREADY_CHARGED", 0);
			map.put("ARREAR", BankTransService.toFen(row.get("oughtfee")));
			readings.put(map);
		}
		
		response.put("METER_READING_ROWS", readings);
		response.put("METER_READING_COUNT", readings.length());
	}

	private void fillMD5() throws JSONException 
	{
		StringBuilder pt = new StringBuilder();
		pt.append(response.getString(BankTransService.TRANS_CODE));
		pt.append(response.getString("USER_ID"));
		pt.append(response.getString("USER_NAME"));
		pt.append(response.getString("USER_TYPE"));
		pt.append(response.getString("BALANCE"));
		pt.append(response.getString("MAINTENANCE_FEE_TILL"));
		int cnt = response.getInt("MAINTENANCE_FEE_COUNT");
		pt.append(cnt);
		if(cnt > 0)
		{
			JSONArray list = (JSONArray)response.get("MAINTENANCE_FEE_ROWS");
			for(int i=0; i<list.length(); i++)
			{
				JSONObject map = list.getJSONObject(i);
				pt.append(map.get("MAINTENANCE_FEE_SPAN"));
				pt.append(map.get("MAINTENANCE_FEE"));
			}
		}
		cnt = response.getInt("METER_READING_COUNT");
		pt.append(cnt);
		if(cnt > 0)
		{
			JSONArray list = (JSONArray)response.get("METER_READING_ROWS");
			for(int i=0; i<list.length(); i++)
			{
				JSONObject map = list.getJSONObject(i);
				pt.append(map.get("READING_DATE"));
				pt.append(map.get("LAST_READING"));
				pt.append(map.get("CUR_READING"));
				pt.append(map.get("GAS_USED"));
				pt.append(map.get("SHOULD_CHARGE"));
				pt.append(map.get("ALREADY_CHARGED"));
				pt.append(map.get("ARREAR"));
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
		String plainText = request.getString(BankTransService.TRANS_CODE) + request.getString("USER_ID");
		String md5 = this.getMD5(plainText);
		return md5.equals(request.getString(BankTransService.MAC));
	}

	@Override
	public boolean isPacketSemanticallyCorrect() 
	{
		if(!super.isPacketSemanticallyCorrect())
			return false;
		if(request.isNull("USER_ID"))
			return false;
		return true;
	}


}
