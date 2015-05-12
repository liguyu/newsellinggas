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
 * 卡表用户查询
 * @author grain
 *
 */
public class ProtocolHandler2001 extends ProtocolHandler 
{
	static Logger log = Logger.getLogger(ProtocolHandler2001.class);

	@Override
	public void execute()
	{
		try
		{
			fillHeader();
			String cardid = request.getString("CARD_ID");
			
			//查询用户基本信息
			List list = hibernateTemplate.find("from t_userfiles where " +
					//"f_gasmeterstyle='卡表' and " +
					" f_cardid = '" + cardid + "'" );
			if(list.size() != 1)
			{
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_IC_METER_USER);
				response.put(BankTransService.RESPONSE_CODE, BankTransService.MSG.get(BankTransService.ERROR_IC_METER_USER));
				return;
			}		
			Map<String, Object> row = (Map<String, Object>)list.get(0);
			
			response.put("CARD_ID", cardid);
			response.put("USER_NAME", BankTransService.encodeChinese(row.get("f_username")));
			response.put("USER_ADDRESS", BankTransService.encodeChinese(row.get("f_address")));
			response.put("USER_TYPE", BankTransService.encodeChinese(row.get("f_usertype")));
			response.put("GAS_UPPER_LIMIT", row.get("czsx"));
			response.put("GAS_LOWER_LIMIT", "0");
			response.put("CURRENCY_OR_COUNTER_METER", BankTransService.encodeChinese("流量表"));
			response.put("PRICE", BankTransService.toFen("2.54"));
			response.put("IS_LADDER_PRICE", BankTransService.encodeChinese("否"));
				
			//维管费
			calculateMaintenanceFee((Date)row.get("f_beginfee"), getMonthlyMaintenanceFee(hibernateTemplate));
				
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
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_IC_METER_USER);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_IC_METER_USER));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}


	private void fillMD5() throws JSONException 
	{
		StringBuilder pt = new StringBuilder();
		pt.append(response.getString(BankTransService.TRANS_CODE));
		
		pt.append(response.getString("CARD_ID"));
		pt.append(response.getString("USER_NAME"));
		pt.append(response.getString("USER_ADDRESS"));
		pt.append(response.getString("USER_TYPE"));
		pt.append(response.getString("GAS_UPPER_LIMIT"));
		pt.append(response.getString("GAS_LOWER_LIMIT"));
		pt.append(response.getString("CURRENCY_OR_COUNTER_METER"));
		pt.append(response.getString("PRICE"));
		pt.append(response.getString("IS_LADDER_PRICE"));

		int cnt = response.getInt("MAINTENANCE_FEE_COUNT");
		pt.append(cnt);
		if(cnt > 0)
		{
			JSONArray list = (JSONArray)response.getJSONArray("MAINTENANCE_FEE_ROWS");
			for(int i=0; i<list.length(); i++)
			{
				JSONObject map = list.getJSONObject(i);
				pt.append(map.get("MAINTENANCE_FEE_SPAN"));
				pt.append(map.get("MAINTENANCE_FEE"));
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
		String plainText = request.getString(BankTransService.TRANS_CODE) + request.getString("CARD_ID");
		String md5 = this.getMD5(plainText);
		return md5.equals(request.getString(BankTransService.MAC));
	}

	@Override
	public boolean isPacketSemanticallyCorrect() 
	{
		if(!super.isPacketSemanticallyCorrect())
			return false;
		if(request.isNull("CARD_ID"))
			return false;
		return true;
	}


}
