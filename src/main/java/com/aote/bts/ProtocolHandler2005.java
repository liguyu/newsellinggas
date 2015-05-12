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
 * 卡表缴费状态查询
 * @author grain
 *
 */
public class ProtocolHandler2005 extends ProtocolHandler 
{
	static Logger log = Logger.getLogger(ProtocolHandler2005.class);

	@Override
	public void execute()
	{
		try
		{
			fillHeader();
			String cardId = request.getString("CARD_ID");
			
			//如果交易不存在
			Map<String, Object> sale = getSale();
			if(sale == null)
			{
				response.put("ACK_TRANS_SN", UUID.randomUUID().toString().replace("-", ""));
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_CHARGE_STATE);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_CHARGE_STATE));
			}
			else
			{
				//交易存在，验证
				if(sale.get("STATUS").toString().equals("准缴费"))
					response.put("LAST_PURCHASE_STATE", BankTransService.encodeChinese("未写卡"));
				else
					response.put("LAST_PURCHASE_STATE", BankTransService.encodeChinese("已写卡"));
				
				//返回响应
				response.put("CARD_ID", cardId);
				response.put("PAYMENT_DATE", sale.get("BANK_DATE").toString());
				response.put("GAS_TOTAL_AMOUNT", BankTransService.toInt(sale.get("GAS_AMOUNT_APPROVED")));
				response.put("GAS_AMOUNT_APPROVED", BankTransService.toInt(sale.get("GAS_AMOUNT_APPROVED")));
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
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_CHARGE_STATE);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_CHARGE_STATE));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 检查交易是否正常
	 * @return
	 */
	private Map<String, Object> getSale() throws JSONException
	{
		String sql = "from t_bank_trans where " +
		" TRANS_CODE='2003' " +
		" and TRANS_SN='" + request.getString("NOTIFICATION_TRANS_SN") + "' ";
		List list = hibernateTemplate.find(sql);
		if(list.size()>0)
			return ((Map<String, Object>)list.get(0));
		else
			return null;
	}
	
	private void fillMD5() throws JSONException 
	{
		StringBuilder pt = new StringBuilder();
		pt.append(response.getString(BankTransService.TRANS_CODE));
		pt.append(response.getString("LAST_PURCHASE_STATE"));
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
		+ request.getString("NOTIFICATION_TRANS_SN") 
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
		if(request.isNull("NOTIFICATION_TRANS_SN"))
			return false;
		if(request.isNull("GAS_AMOUNT_APPROVED"))
			return false;
		if(request.isNull("CARD_ID"))
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
