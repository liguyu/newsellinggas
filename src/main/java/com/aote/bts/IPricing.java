package com.aote.bts;

import java.util.Map;

public interface IPricing {
	/**
	 * ����ά�ܷѡ����ѡ����빺����������������������ϸ�������ѡ��ܷ��õ�
	 */
	public boolean pricing(String card_id, String dt, int gasAmount, Map<String, Object> map);
}
