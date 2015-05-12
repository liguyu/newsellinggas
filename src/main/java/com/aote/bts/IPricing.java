package com.aote.bts;

import java.util.Map;

public interface IPricing {
	/**
	 * 处理维管费、气费、申请购气量、允许购气量、费用明细、其他费、总费用等
	 */
	public boolean pricing(String card_id, String dt, int gasAmount, Map<String, Object> map);
}
