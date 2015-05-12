package com.aote.rs;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import com.aote.bts.IDeposit;
import com.aote.bts.ProtocolHandler;
import com.aote.bts.ProtocolHandlerFactory;




@Path("bts")
@Component

public class BankTransService {
	static Logger log = Logger.getLogger(BankTransService.class);
	
	@Autowired
	private HibernateTemplate hibernateTemplate;
	
	@Autowired
	private ApplicationContext appContext;

	public static String TRANS_CODE = "TRANS_CODE";
	public static String TRANS_SN = "TRANS_SN";
	public static String BANK_DATE = "BANK_DATE";
	public static String BANK_TIME = "BANK_TIME";
	public static String BANK_ID = "BANK_ID";
	public static String TELLER_ID = "TELLER_ID";
	public static String CHANNEL_ID = "CHANNEL_ID";
	public static String DEVICE_ID = "DEVICE_ID";
	public static String ACK_TRANS_SN = "ACK_TRANS_SN";
	public static String RESPONSE_CODE = "RESPONSE_CODE";
	public static String RESPONSE = "RESPONSE";
	public static String MAC = "MAC";
	
	public static String ERROR_NO_ERROR = "0000";
	public static String ERROR_WRONG_PACKET_FORMAT = "0001";
	public static String ERROR_WRITE_CARD_PENDING = "0002";
	public static String ERROR_MACHINE_METER_USER = "1001";
	public static String ERROR_MACHINE_CHARGE_NOTIFICATION = "1101";
	public static String ERROR_IC_METER_USER = "1201";
	public static String ERROR_IC_CALC_FEE = "1301";
	public static String ERROR_IC_CHARGE_NOTIFICATION = "1404";
	public static String ERROR_IC_GAS_FEE_MISMATCH = "1401";
	public static String ERROR_IC_MAINTENANCE_FEE_MISMATCH = "1402";
	public static String ERROR_IC_OTHER_FEE_MISMATCH = "1403";
	public static String ERROR_USER_QUERY = "1501";
	public static String ERROR_IC_CANCEL_OFF = "1601";
	public static String ERROR_CHARGE_STATE = "1701";
	public static String ERROR_WRITE_CARD_CONFIRM = "1701";
	public static String ERROR_INTERNAL_ERROR = "8888";
	public static String ERROR_SERVICE_SUSPENDED = "9999";
	
	//	0000	���׳ɹ���
	//	0001	�����ĸ�ʽ����
	//	0002	�����ϴνɷ�δд���������ٴνɷѡ�
	//	100x	�����û���ѯ����
	//	11xx	����ɷ�֪ͨ����
	//	12xx	�����û���ѯ����
	//	13xx	�����۴���
	//	14xx	����ɷ�֪ͨ����
	//	1401	���λ�������������
	//	1402	���λ���ά�ܷѲ�����
	//	1403	���λ����������ò�����
	//	15xx	�û���Ϣģ����ѯ����
	//	16xx	�����������
	//	17xx	����״̬��ѯ����
	//	18xx	д��ȷ�ϴ���
	//	9888	ϵͳ�ڲ�����
	//	9999	ϵͳ��ͣ����

	public static HashMap<String, String> MSG = new HashMap<String, String>(); 
	static
	{
		MSG.put(BankTransService.ERROR_NO_ERROR, "���׳ɹ���");
		MSG.put(BankTransService.ERROR_WRONG_PACKET_FORMAT, "�����ĸ�ʽ����");
		MSG.put(BankTransService.ERROR_WRITE_CARD_PENDING, "�����ϴνɷ�δд���������ٴνɷѡ�");
		MSG.put(BankTransService.ERROR_MACHINE_METER_USER, "�����û���Ϣ��ѯ����");
		MSG.put(BankTransService.ERROR_MACHINE_CHARGE_NOTIFICATION, "����ɷ�֪ͨ����");
		MSG.put(BankTransService.ERROR_IC_METER_USER, "�����û���ѯ����");
		MSG.put(BankTransService.ERROR_IC_CALC_FEE, "�����۴���");
		MSG.put(BankTransService.ERROR_IC_CHARGE_NOTIFICATION, "����ɷ�֪ͨ����");
		MSG.put(BankTransService.ERROR_IC_GAS_FEE_MISMATCH, "���λ�������������");
		MSG.put(BankTransService.ERROR_IC_MAINTENANCE_FEE_MISMATCH, "���λ���ά�ܷѲ�����");
		MSG.put(BankTransService.ERROR_IC_OTHER_FEE_MISMATCH, "���λ����������ò�����");
		MSG.put(BankTransService.ERROR_USER_QUERY, "�û���Ϣģ����ѯ����");
		MSG.put(BankTransService.ERROR_IC_CANCEL_OFF, "�����������");
		MSG.put(BankTransService.ERROR_CHARGE_STATE, "����״̬��ѯ����");
		MSG.put(BankTransService.ERROR_WRITE_CARD_CONFIRM, "д��ȷ�ϴ���");
		MSG.put(BankTransService.ERROR_INTERNAL_ERROR, "ϵͳ�ڲ�����");
		MSG.put(BankTransService.ERROR_SERVICE_SUSPENDED, "ϵͳ��ͣ����");
		Iterator<String> keys = MSG.keySet().iterator();
		while(keys.hasNext())
		{
			String key = keys.next();
			MSG.put(key, BankTransService.encodeChinese(MSG.get(key)));
		}
	}
	
	/**
	 * Э�鴦�����
	 * @return
	 */
	@POST
	public JSONObject dispatcher(String json)
	{
		log.debug("���յı���: " + json);
		JSONObject result = new JSONObject();
		try
		{
			JSONObject cmdObj = parseRequest(json);
			//�����쳣
			if(cmdObj == null)
			{
				emptyResult(result);
				result.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_WRONG_PACKET_FORMAT);
				result.put(BankTransService.RESPONSE, json);
				return result;
			}

			ProtocolHandler ph = ProtocolHandlerFactory.get(appContext, hibernateTemplate, cmdObj.getString(BankTransService.TRANS_CODE), cmdObj, result);

			//���������쳣
			if(ph == null || !ph.isPacketSemanticallyCorrect())
			{
				emptyResult(result);
				result.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_WRONG_PACKET_FORMAT);
				result.put(BankTransService.RESPONSE, json);
				return result;
			}
			
			//MD5����
			if(!ph.isRequestMD5Correct())
			{
				emptyResult(result);
				result.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_WRONG_PACKET_FORMAT);
				result.put(BankTransService.RESPONSE, json);
				return result;
			}
			
			ph.execute();
			return result;
		}
		catch(Exception e)
		{
			//�ڲ�����
			log.debug("�ڲ�����: " + e.getMessage());
			internalError(result);
			return result;
		}
	}

	/**
	 * �ڲ�����
	 * @param result
	 * @param code
	 */
	private void internalError(JSONObject result) 
	{
		try
		{
			emptyResult(result);
			result.put(BankTransService.RESPONSE_CODE, BankTransService.ERROR_INTERNAL_ERROR);
			result.put(BankTransService.RESPONSE, MSG.get(BankTransService.ERROR_INTERNAL_ERROR));
		}
		catch(Exception e)
		{
			
		}
	}

	/**
	 * ��������ֶ�
	 * @param result
	 * @throws JSONException
	 */
	public static void emptyResult(JSONObject result) throws JSONException
	{
		Iterator itr = result.keys();
		while(itr.hasNext())
			result.put(itr.next().toString(), JSONObject.NULL);
	}


	/**
	 * ��������
	 * @param json
	 * @return
	 */
	private JSONObject parseRequest(String json) 
	{	
		try
		{
			return new JSONObject(json);
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public static String encodeChinese(Object plainText)
	{
		return URLEncoder.encode(plainText.toString());
	}
	
	public static String formatDate(Date dt)
	{
		SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd");
		return df.format(dt);
	}
	
	public static int toFen(Object value)
	{
		return (int)(Double.parseDouble(value.toString())*100);
	}

	public static Object toInt(Object value) 
	{
		return (int)(Double.parseDouble(value.toString()));
	}
	
}

