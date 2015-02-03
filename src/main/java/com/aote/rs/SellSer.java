package com.aote.rs;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

@Path("sell")
@Component
public class SellSer {
	static Logger log = Logger.getLogger(SellSer.class);
	@Autowired
	private HibernateTemplate hibernateTemplate;

	// 定义sell方法，处理交费
	@GET
	@Path("{userid}/{money}/{zhinajin}/{payment}/{opid}")
	public String txSell(@PathParam("userid") String userid,
			@PathParam("money") double dMoney,
			@PathParam("zhinajin") double dZhinajin,
			@PathParam("payment") String payment, @PathParam("opid") String opid) {
		try {
			// 查找登陆用户,获取登陆网点,操作员
			Map<String, Object> loginUser = this.findUser(opid);
			if (loginUser == null) {
				log.error("机表缴费处理时未找到登陆用户,登陆id" + opid);
				throw new WebApplicationException(401);
			}
			String sgnetwork = loginUser.get("f_parentname").toString();
			String sgoperator = loginUser.get("name").toString();
			String fengongsi = loginUser.get("f_fengongsi").toString();
			String fengongnum = loginUser.get("f_fengongsinum").toString();

			// 根据用户编号找到用户档案中的信息,以及抄表记录
			String sql = " select u.f_zhye f_zhye, u.f_username f_username,u.f_cardid f_cardid, u.f_address f_address,u.f_districtname f_districtname,u.f_cusDom f_cusDom,u.f_cusDy f_cusDy,u.f_beginfee f_beginfee, u.f_metergasnums f_metergasnums, u.f_cumulativepurchase f_cumulativepurchase,"
					+ "u.f_idnumber f_idnumber, u.f_gaspricetype f_gaspricetype, u.f_gasprice f_gasprice, u.f_usertype f_usertype,"
					+ "u.f_gasproperties f_gasproperties, u.f_userid f_userid, h.id handid, h.oughtamount oughtamount, h.lastinputgasnum lastinputgasnum,"
					+ "h.lastrecord lastrecord, h.shifoujiaofei shifoujiaofei, h.oughtfee oughtfee from t_userfiles u "
					+ "left join (select * from t_handplan where f_state = '已抄表' and shifoujiaofei = '否') h on u.f_userid = h.f_userid where u.f_userid = '"
					+ userid
					+ "' "
					+ "order by u.f_userid, h.lastinputdate, h.lastinputgasnum";
			HibernateSQLCall sqlCall = new HibernateSQLCall(sql);
			List<Map<String, Object>> list = this.hibernateTemplate
					.executeFind(sqlCall);
			// 将收款转为BigDecimal
			BigDecimal money = new BigDecimal(dMoney + "");
			BigDecimal zhinajin = new BigDecimal(dZhinajin + "");
			// 取出第一条记录，以便从用户档案中取数据
			Map<String, Object> userinfo = (Map<String, Object>) list.get(0);
			// 从用户档案中取出累计购气量
			BigDecimal f_metergasnums = new BigDecimal(userinfo.get(
					"f_metergasnums").toString());
			BigDecimal f_cumulativepurchase = new BigDecimal(userinfo.get(
					"f_cumulativepurchase").toString());
			// 记录上次购气量（冲正时使用）
			BigDecimal oldf_metergasnums = new BigDecimal(userinfo.get(
					"f_metergasnums").toString());// 旧的表当前累计购气量
			BigDecimal oldf_cumulativepurchase = new BigDecimal(userinfo.get(
					"f_cumulativepurchase").toString());// 旧的总累计购气量

			// 从用户档案中取出余额
			BigDecimal f_zhye = new BigDecimal(userinfo.get("f_zhye")
					.toString());
			// 拿余额+实际收费金额-滞纳金 再和应交金额比较，判断未交费的抄表记录是否能够交费
			BigDecimal total = f_zhye.add(money).subtract(zhinajin);
			// 总的上期指数
			BigDecimal lastnum = new BigDecimal("0");
			// 总气量
			BigDecimal gasSum = new BigDecimal("0");
			// 总气费
			BigDecimal feeSum = new BigDecimal("0");
			// 抄表记录id
			String handIds = "";
			for (Map<String, Object> map : list) {

				// 取出应交金额
				String h = (map.get("oughtfee") + "");
				if (h.equals("null")) {
					h = "0.0";
				} else {
				}
				BigDecimal oughtfee = new BigDecimal(h);
				// 当前用户实际缴费够交，则扣除，交费记录变为已交
				int equals = total.compareTo(oughtfee);// 判断total和oughtfee的大小
				if (equals >= 0) {
					// 扣费，并产生本次余额
					total = total.subtract(oughtfee);
					// 交费成功，上期指数相加
					String lastinputgasnum1 = (map.get("lastinputgasnum") + "");
					if (lastinputgasnum1.equals("null"))
						lastinputgasnum1 = "0.0";
					BigDecimal lastinputgasnum = new BigDecimal(
							lastinputgasnum1);
					lastnum = lastnum.add(lastinputgasnum);

					// 气量相加
					String oughtamount1 = (map.get("oughtamount") + "");
					if (oughtamount1.equals("null"))
						oughtamount1 = "0.0";
					BigDecimal gas = new BigDecimal(oughtamount1);
					gasSum = gasSum.add(gas);
					// 累计购气量
					f_metergasnums = f_metergasnums.add(gasSum);
					f_cumulativepurchase = f_cumulativepurchase.add(gasSum);
					// 气费相加
					feeSum = feeSum.add(oughtfee);
					// 获取抄表记录ID
					Integer handId1 = (Integer) map.get("handid");
					if (handId1 == null)
						handId1 = 0;
					int handId = handId1;
					// 抄表记录Ids
					handIds = add(handIds, handId + "");
					// 更新抄表记录
					System.out.println(handIds + "：update开始");
					String updateHandplan = "update t_handplan set shifoujiaofei='是' where id="
							+ handId;
					hibernateTemplate.bulkUpdate(updateHandplan);
				}
			}
			// 更新用户档案
			String updateUserinfo = "update t_userfiles set f_zhye=" + total
					+ " ,f_metergasnums=" + f_metergasnums
					+ " ,f_cumulativepurchase=" + f_cumulativepurchase
					+ " where f_userid='" + userid + "'";
			hibernateTemplate.bulkUpdate(updateUserinfo);
			// 产生交费记录
			Map<String, Object> sell = new HashMap<String, Object>();

			sell.put("f_userid", userid); // 户的id
			sell.put("lastinputgasnum", lastnum.setScale(1,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // 上期指数
			sell.put("lastrecord", lastnum.add(gasSum).setScale(1,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // 本期指数
			sell.put("f_totalcost", zhinajin.add(feeSum).subtract(f_zhye)
					.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 应交金额
			sell.put("f_grossproceeds", money.setScale(2,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // 收款
			sell.put("f_zhinajin", zhinajin.setScale(2,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // 滞纳金

			Date now = new Date();
			sell.put("f_deliverydate", now); // 交费日期
			sell.put("f_deliverytime", now); // 交费时间

			sell.put("f_zhye", f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP)
					.doubleValue()); // 上期结余
			sell.put("f_benqizhye", total.setScale(2, BigDecimal.ROUND_HALF_UP)
					.doubleValue()); // 本期结余
			sell.put("f_beginfee", userinfo.get("f_beginfee")); // 维管费
			sell.put("f_premetergasnums", oldf_metergasnums.setScale(2,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // 表上次累计购气量
			sell.put("f_upbuynum", oldf_cumulativepurchase.setScale(2,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // 上次总累计购气量
			sell.put("f_gasmeterstyle", "机表"); // 气表类型
			sell.put("f_comtype", "天然气公司"); // 公司类型，分为天然气公司、银行
			sell.put("f_username", userinfo.get("f_username")); // 用户/单位名称
			sell.put("f_address", userinfo.get("f_address")); // 地址
			sell.put("f_districtname", userinfo.get("f_districtname")); // 地址
			sell.put("f_cusDom", userinfo.get("f_cusDom")); // 地址
			sell.put("f_cusDy", userinfo.get("f_cusDy")); // 地址
			sell.put("f_idnumber", userinfo.get("f_idnumber")); // 身份证号
			sell.put("f_gaswatchbrand", "机表"); // 气表品牌
			sell.put("f_gaspricetype", userinfo.get("f_gaspricetype")); // 气价类型
			sell.put("f_gasprice", userinfo.get("f_gasprice")); // 气价
			sell.put("f_usertype", userinfo.get("f_usertype")); // 用户类型
			sell.put("f_gasproperties", userinfo.get("f_gasproperties"));// 用气性质
			//机表中，将卡号作为存储折子号，磁条卡的信息字段
			if(userinfo.containsKey("f_cardid")&& userinfo.get("f_cardid")!=null)
			{
			  String kh = userinfo.get("f_cardid").toString();
			  sell.put("f_cardid", kh);
			}
				
			sell.put("f_pregas", gasSum.setScale(1, BigDecimal.ROUND_HALF_UP)
					.doubleValue()); // 气量
			sell.put("f_preamount", feeSum
					.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 气费
			sell.put("f_payment", payment); // 付款方式
			sell.put("f_paytype", "现金"); // 交费类型，银行代扣/现金
			sell.put("f_sgnetwork", sgnetwork); // 网点
			sell.put("f_sgoperator", sgoperator); // 操 作 员
			sell.put("f_filiale", fengongsi); // 分公司
			sell.put("f_fengongsinum", fengongnum); // 分公司编号
			sell.put("f_payfeetype", "机表收费"); // 交易类型
			sell.put("f_payfeevalid", "有效"); // 购气有效类型
			sell.put("f_useful", handIds); // 抄表记录id
			int sellId = (Integer) hibernateTemplate.save("t_sellinggas", sell);
			// 格式化交费日期
			SimpleDateFormat f2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			String result = "{id:" + sellId + ", f_deliverydate:'"
					+ f2.format(now) + "'}";
			// 更新抄表记录sellid
			String updateHandplan = "update t_handplan set f_sellid =" + sellId
					+ " where id in (" + handIds + ")";
			hibernateTemplate.bulkUpdate(updateHandplan);
			return "";
		} catch (Exception ex) {
			// 登记异常信息
			log.error(ex.getMessage());
			throw new WebApplicationException(401);
		}
	}

	// 查找登陆用户
	private Map<String,Object> findUser(String loginId) {
		String findUser = "from t_user where id='" + loginId + "'";
		List<Object> userList = this.hibernateTemplate.find(findUser);
		if (userList.size() != 1) {
			return null;
		}
		return (Map<String,Object>)userList.get(0);
	}

	// 执行sql查询
	class HibernateSQLCall implements HibernateCallback {
		String sql;

		public HibernateSQLCall(String sql) {
			this.sql = sql;
		}

		public Object doInHibernate(Session session) {
			Query q = session.createSQLQuery(sql);
			q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
			List result = q.list();
			return result;
		}
	}

	// 给字符串添加逗号分隔的内容
	private String add(String source, String str) {
		if (source.equals("")) {
			return source + str;
		} else {
			return source + "," + str;
		}
	}
}
