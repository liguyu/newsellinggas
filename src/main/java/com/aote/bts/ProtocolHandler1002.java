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
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.aote.rs.BankTransService;



/**
 * 机表缴费通知
 * @author grain
 *
 */
public class ProtocolHandler1002 extends ProtocolHandler 
{
	static Logger log = Logger.getLogger(ProtocolHandler1002.class);

	@Override
	public void execute()
	{
		try
		{
			fillHeader();
			String userId = request.getString("USER_ID");
			String payment = request.getString("PAYMENT");
			String dt = request.getString(BankTransService.BANK_DATE);
			String tm = request.getString(BankTransService.BANK_TIME);
			
			response.put("USER_ID", userId);
			response.put("PAYMENT", payment);

			if(isDuplicateRequest())
			{
				response.put("ACK_TRANS_SN", UUID.randomUUID().toString().replace("-", ""));
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_NO_ERROR);
				response.put(BankTransService.RESPONSE, BankTransService.encodeChinese("重复缴费通知。"));
			}
			else
			{
				remoteSellGas(userId, payment, dt, tm);
				
				response.put("ACK_TRANS_SN", UUID.randomUUID().toString().replace("-", ""));
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_NO_ERROR);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_NO_ERROR));
			}
			//校验码
			fillMD5();
		}
		catch(Exception e)
		{
			log.debug(e.toString());
			try {
				BankTransService.emptyResult(response);
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_MACHINE_CHARGE_NOTIFICATION);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_MACHINE_CHARGE_NOTIFICATION));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}


	/**
	 * 售气
	 * @param tm 
	 * @param dt 
	 * @param payment 
	 * @param userId 
	 * @throws JSONException
	 */
	private void remoteSellGas(String userId, String payment, String dt, String tm) throws JSONException
	{
		logRequest();
		IDeposit idpt = (IDeposit) appContext.getBean("deposit");
		idpt.deposit(userId, dt, tm, payment);
	}

	/**
	 * 记录机表缴费通知
	 * @throws JSONException 
	 */
	private void logRequest() throws JSONException 
	{
		//no entity syndrome
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(BankTransService.TRANS_CODE, request.getString(BankTransService.TRANS_CODE));
		map.put(BankTransService.TRANS_SN, request.getString(BankTransService.TRANS_SN));
		map.put(BankTransService.BANK_DATE, request.getString(BankTransService.BANK_DATE));
		map.put(BankTransService.BANK_TIME, request.getString(BankTransService.BANK_TIME));
		map.put(BankTransService.BANK_ID, request.getString(BankTransService.BANK_ID));
		map.put(BankTransService.TELLER_ID, request.getString(BankTransService.TELLER_ID));
		map.put(BankTransService.CHANNEL_ID, request.getString(BankTransService.CHANNEL_ID));
		map.put(BankTransService.DEVICE_ID, request.getString(BankTransService.DEVICE_ID));
		map.put(BankTransService.MAC, request.getString(BankTransService.MAC));
		map.put("USER_ID", request.getString("USER_ID"));
		map.put("PAYMENT", request.getString("PAYMENT"));
		
		hibernateTemplate.save("t_bank_trans", map);
	}

	private void fillMD5() throws JSONException 
	{
		StringBuilder pt = new StringBuilder();
		pt.append(response.getString(BankTransService.TRANS_CODE));
		pt.append(response.getString("USER_ID"));
		pt.append(response.getString("PAYMENT"));
		pt.append(response.getString("BANK_DATE"));
		response.put(BankTransService.MAC, this.getMD5(pt.toString()));
	}


	/**
	 * 发送请求的md5是否正常
	 * @throws JSONException 
	 */
	@Override
	public boolean isRequestMD5Correct() throws JSONException
	{
		String plainText = request.getString(BankTransService.TRANS_CODE) + request.getString("USER_ID") + request.getString("PAYMENT");
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
		if(request.isNull("PAYMENT"))
			return false;
		
		try {
			if(request.getInt("PAYMENT")<=0)
				return false;
		} catch (JSONException e) {
			return false;
		}
		return true;
	}


}
