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
 * 卡表缴费通知
 * @author grain
 *
 */
public class ProtocolHandler2003 extends ProtocolHandler 
{
	static Logger log = Logger.getLogger(ProtocolHandler2003.class);

	@Override
	public void execute()
	{
		try
		{
			fillHeader();
			String cardId = request.getString("CARD_ID");
			String gas = request.getString("GAS_AMOUNT_APPROVED");
			String gasFee = request.getString("GAS_FEE");
			String mainFee = request.getString("MAINTENANCE_FEE");
			String otherFee = request.getString("OTHER_FEE");
			String totalFee = request.getString("TOTAL_FEE");
			
			if(isDuplicateRequest())
			{
				response.put("ACK_TRANS_SN", UUID.randomUUID().toString().replace("-", ""));
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_NO_ERROR);
				response.put(BankTransService.RESPONSE, BankTransService.encodeChinese("重复缴费通知。"));
			}
			else
			{
				//划价并比较
				String dt = request.getString(BankTransService.BANK_DATE);
				String tm = request.getString(BankTransService.BANK_TIME);

				Map<String, Object> map = new HashMap<String, Object>();
				IPricing pricing = (IPricing)appContext.getBean("pricing");
				pricing.pricing(cardId, dt, Integer.parseInt(gas), map);
				
				//比较价格
				comparePricing();
				remoteSellGas(cardId, gas, dt, tm);
				
				response.put("CARD_ID", cardId);
				response.put("PAYMENT_DATE", dt + " " + tm);
				response.put("GAS_TOTAL_AMOUNT", Integer.parseInt(gas));
				response.put("GAS_AMOUNT_APPROVED", Integer.parseInt(gas));
				response.put("GAS_AMOUNT_FREE", 0);
				response.put("CARD_ENCRYPT", "");
				response.put("MANUFACTURE_ID", "KeLuoMu");
							
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
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_IC_CHARGE_NOTIFICATION);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_IC_CHARGE_NOTIFICATION));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}

	
	/**
	 * 比较划价
	 */
	private String comparePricing() {
		// TODO Auto-generated method stub
		return BankTransService.ERROR_IC_GAS_FEE_MISMATCH;
	}


	/**
	 * 售气
	 * @throws JSONException
	 */
	private void remoteSellGas(String cardId, String gas, String dt, String tm) throws JSONException
	{
		ISale sale = (ISale) appContext.getBean("sale");
		String saleId = sale.sell(cardId, gas, dt, tm);
		logRequest(saleId);
	}

	/**
	 * 记录机表缴费通知
	 * @throws JSONException 
	 */
	private void logRequest(String saleId) throws JSONException 
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
		map.put("CARD_ID", request.getString("CARD_ID"));
		map.put("GAS_AMOUNT_APPROVED", request.getString("GAS_AMOUNT_APPROVED"));
		map.put("GAS_FEE", request.getString("GAS_FEE"));
		map.put("MAINTENANCE_FEE", request.getString("MAINTENANCE_FEE"));
		map.put("OTHER_FEE", request.getString("OTHER_FEE"));
		map.put("TOTAL_FEE", request.getString("TOTAL_FEE"));
		map.put("STATUS", "准缴费");
		map.put("SALE_ID", saleId);
		
		hibernateTemplate.save("t_bank_trans", map);
	}

	private void fillMD5() throws JSONException 
	{
		StringBuilder pt = new StringBuilder();
		pt.append(response.getString(BankTransService.TRANS_CODE));
		pt.append(response.getString("CARD_ID"));
		pt.append(response.getString("PAYMENT_DATE"));
		pt.append(response.getString("GAS_TOTAL_AMOUNT"));
		pt.append(response.getString("GAS_AMOUNT_APPROVED"));
		pt.append(response.getString("GAS_AMOUNT_FREE"));
		pt.append(response.getString("CARD_ENCRYPT"));
		pt.append(response.getString("MANUFACTURE_ID"));
		response.put(BankTransService.MAC, this.getMD5(pt.toString()));
	}


	/**
	 * 发送请求的md5是否正常
	 * @throws JSONException 
	 */
	@Override
	public boolean isRequestMD5Correct() throws JSONException
	{
		String plainText = request.getString(BankTransService.TRANS_CODE) 
		+ request.getString("CARD_ID") 
		+ request.getString("GAS_AMOUNT_APPROVED") 
		+ request.getString("GAS_FEE") 
		+ request.getString("MAINTENANCE_FEE") 
		+ request.getString("OTHER_FEE") 
		+ request.getString("TOTAL_FEE");
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
		if(request.isNull("GAS_AMOUNT_APPROVED"))
			return false;
		if(request.isNull("GAS_FEE"))
			return false;
		if(request.isNull("MAINTENANCE_FEE"))
			return false;
		if(request.isNull("OTHER_FEE"))
			return false;
		if(request.isNull("TOTAL_FEE"))
			return false;
		return true;
	}


}
