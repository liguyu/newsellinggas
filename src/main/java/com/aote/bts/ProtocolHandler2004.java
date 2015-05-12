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
 * ����ɷѳ���
 * @author grain
 *
 */
public class ProtocolHandler2004 extends ProtocolHandler 
{
	static Logger log = Logger.getLogger(ProtocolHandler2004.class);

	@Override
	public void execute()
	{
		try
		{
			fillHeader();
			String cardId = request.getString("CARD_ID");
			String saleSN = request.getString("NOTIFICATION_TRANS_SN");
			//������ײ�����
			String saleId = getSaleID();
			if(saleId == null)
			{
				response.put("ACK_TRANS_SN", UUID.randomUUID().toString().replace("-", ""));
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_IC_CANCEL_OFF);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_IC_CANCEL_OFF));
			}
			else
			{
				//���ó�������
				ICancelOff cancelOff = (ICancelOff)appContext.getBean("cancelOff");
				cancelOff.cancelOff(saleId);

				//���³���״̬
				updateSaleState();
				
				//������Ӧ
				response.put("NOTIFICATION_TRANS_SN", saleSN);
				response.put("CARD_ID", cardId);
				response.put("GAS_AMOUNT_APPROVED", request.getString("GAS_AMOUNT_APPROVED"));
				response.put("GAS_FEE", request.getString("GAS_FEE"));
				response.put("MAINTENANCE_FEE", request.getString("MAINTENANCE_FEE"));
				response.put("OTHER_FEE", request.getString("OTHER_FEE"));
				response.put("TOTAL_FEE", request.getString("TOTAL_FEE"));
				
				response.put("ACK_TRANS_SN", UUID.randomUUID().toString().replace("-", ""));
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_NO_ERROR);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_NO_ERROR));
			}
			//У����
			fillMD5();
		}
		catch(Exception e)
		{
			log.debug(e.toString());
			try {
				BankTransService.emptyResult(response);
				response.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_IC_CANCEL_OFF);
				response.put(BankTransService.RESPONSE, BankTransService.MSG.get(BankTransService.ERROR_IC_CANCEL_OFF));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * �޸Ľɷ�״̬
	 * @throws JSONException
	 */
	private void updateSaleState() throws JSONException
	{
		String sql = "update t_bank_trans set STATUS='�ѳ���' where STATUS='׼�ɷ�'" +
		" and TRANS_CODE='2003' " +
		" and TRANS_SN='" + request.getString("NOTIFICATION_TRANS_SN") + "' ";
		hibernateTemplate.bulkUpdate(sql);
	}


	/**
	 * ����Ƿ��нɷѵ���û�г����ļ�¼
	 * @return
	 */
	private String getSaleID() throws JSONException
	{
		String sql = "from t_bank_trans where STATUS='׼�ɷ�'" +
		" and TRANS_CODE='2003' " +
		" and TRANS_SN='" + request.getString("NOTIFICATION_TRANS_SN") + "' ";
		List list = hibernateTemplate.find(sql);
		if(list.size()>0)
		//TODO ��ʱ���Ƿ���1
			return "1";
			//return ((Map<String, Object>)list.get(0)).get("SALE_ID").toString();
		else
			return null;
	}


	private void fillMD5() throws JSONException 
	{
		StringBuilder pt = new StringBuilder();
		pt.append(response.getString(BankTransService.TRANS_CODE));
		pt.append(response.getString("NOTIFICATION_TRANS_SN"));
		pt.append(response.getString("CARD_ID"));
		pt.append(response.getString("GAS_AMOUNT_APPROVED"));
		pt.append(response.getString("GAS_FEE"));
		pt.append(response.getString("MAINTENANCE_FEE"));
		pt.append(response.getString("OTHER_FEE"));
		pt.append(response.getString("TOTAL_FEE"));
		response.put(BankTransService.MAC, this.getMD5(pt.toString()));
	}


	/**
	 * ���������md5�Ƿ�����
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
