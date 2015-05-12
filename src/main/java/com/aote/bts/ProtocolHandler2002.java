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
 * 卡表划价
 * @author grain
 *
 */
public class ProtocolHandler2002 extends ProtocolHandler 
{
	static Logger log = Logger.getLogger(ProtocolHandler2002.class);

	@Override
	public void execute()
	{
		try
		{
			fillHeader();
			String cardid = request.getString("CARD_ID");
			String amount = request.getString("GAS_AMOUNT");
			String dt = request.getString(BankTransService.BANK_DATE);
			
			response.put("CARD_ID", cardid);
			response.put("GAS_AMOUNT_REQUESTED", amount);
			response.put("GAS_AMOUNT_APPROVED", amount);
			response.put("GAS_FEE", 9200);
			response.put("GAS_FEE_DETAIL", BankTransService.encodeChinese("费用详情描述......"));
			response.put("OTHER_FEE", 0);
			response.put("TOTAL_FEE", 9200);
			

			Map<String, Object> map = new HashMap<String, Object>();
			IPricing pricing = (IPricing)appContext.getBean("pricing");
			pricing.pricing(cardid, dt, Integer.parseInt(amount), map);
			
			List list = hibernateTemplate.find("from t_userfiles where " +
					//"f_gasmeterstyle='卡表' and " +
					" f_cardid = '" + cardid + "'" );
			if(list.size() != 1)
			{
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_IC_CALC_FEE);
				response.put(BankTransService.RESPONSE_CODE, BankTransService.MSG.get(BankTransService.ERROR_IC_CALC_FEE));
				return;
			}		
			Map<String, Object> row = (Map<String, Object>)list.get(0);

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
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_IC_CALC_FEE);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_IC_CALC_FEE));
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
		pt.append(response.getString("GAS_AMOUNT_REQUESTED"));
		pt.append(response.getString("GAS_AMOUNT_APPROVED"));
		pt.append(response.getString("GAS_FEE"));
		pt.append(response.getString("GAS_FEE_DETAIL"));

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
		
		pt.append(response.getString("OTHER_FEE"));
		pt.append(response.getString("TOTAL_FEE"));
		
		response.put(BankTransService.MAC, this.getMD5(pt.toString()));
	}


	/**
	 * 发送请求的md5是否正常
	 * @throws JSONException 
	 */
	@Override
	public boolean isRequestMD5Correct() throws JSONException
	{
		String plainText = request.getString(BankTransService.TRANS_CODE) + request.getString("CARD_ID") + request.getString("GAS_AMOUNT");
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
		if(request.isNull("GAS_AMOUNT"))
			return false;
		try {
			if(request.getInt("GAS_AMOUNT")<=0)
				return false;
		} catch (JSONException e) {
			return false;
		}		
		return true;
	}


}
