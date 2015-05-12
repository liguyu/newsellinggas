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
 * 卡表缴费冲正
 * @author grain
 *
 */
public class ProtocolHandler2006 extends ProtocolHandler 
{
	static Logger log = Logger.getLogger(ProtocolHandler2006.class);

	@Override
	public void execute()
	{
		try
		{
			fillHeader();
			String cardId = request.getString("CARD_ID");
			String saleSN = request.getString("NOTIFICATION_TRANS_SN");
			
			//如果交易不存在
			Map<String, Object> sale = getSale();
			if(sale == null)
			{
				response.put("ACK_TRANS_SN", UUID.randomUUID().toString().replace("-", ""));
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_WRITE_CARD_CONFIRM);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_WRITE_CARD_CONFIRM));
			}
			else
			{			
				//返回响应
				response.put("CARD_ID", cardId);
				
				//更新缴费状态为已写卡
				updateSaleState();

				response.put("ACK_TRANS_SN", UUID.randomUUID().toString().replace("-", ""));
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_NO_ERROR);
				//交易存在，验证
				if(sale.get("STATUS").toString().equals("已写卡"))
					response.put(BankTransService.RESPONSE, BankTransService.encodeChinese("重复写卡确认。"));
				else
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
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_WRITE_CARD_CONFIRM);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_WRITE_CARD_CONFIRM));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 修改缴费状态
	 * @throws JSONException
	 */
	private void updateSaleState() throws JSONException
	{
		String sql = "update t_bank_trans set STATUS='已写卡' where STATUS='准缴费'" +
		" and TRANS_CODE='2003' " +
		" and TRANS_SN='" + request.getString("NOTIFICATION_TRANS_SN") + "' ";
		hibernateTemplate.bulkUpdate(sql);
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
		pt.append(response.getString("CARD_ID"));
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
		+ request.getString("TOTAL_FEE")
		+ request.getString("GAS_FEE") 
		+ request.getString("MAINTENANCE_FEE") 
		+ request.getString("OTHER_FEE") 
		+ request.getString("CARD_ENCRYPT");
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
		if(request.isNull("CARD_ENCRYPT"))
			return false;
		return true;
	}


}
