package com.aote.rs;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.collection.PersistentSet;
import org.hibernate.proxy.map.MapProxy;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import com.aote.rs.HandService.HibernateSQLCall;

@Path("hand")
@Component
public class HandService {

	static Logger log = Logger.getLogger(HandService.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;

	// 抄表单下载，返回JSON串
	// operator 抄表员中文名
	@GET
	@Path("{operator}")
	@Produces("application/json")
	public JSONArray ReadRecordInput(@PathParam("operator") String operator) {
		JSONArray array = new JSONArray();
		List<Object> list = this.hibernateTemplate.find(
				"from t_handplan where f_inputtor=? and f_state='未抄表'",
				operator);
		for (Object obj : list) {
			// 把单个map转换成JSON对象
			Map<String, Object> map = (Map<String, Object>) obj;
			JSONObject json = (JSONObject) new JsonTransfer().MapToJson(map);
			array.put(json);
		}
		return array;
	}
	//查询批量抄表单	
    @POST
@Path("download")
public String downLoadRecord(String condition) {
	
	String sql = "select top 1000 u.f_userid,u.f_username,u.f_districtname,u.f_address,u.lastinputgasnum " +
			"from t_handplan h left join t_userfiles u on h.f_userid = u.f_userid where h.shifoujiaofei='否' and u.f_userstate!='注销' and h.f_state='未抄表' and " 
			+ condition + "	order by u.f_address,u.f_apartment";
	List<Object> list = this.hibernateTemplate.executeFind(new HibernateSQLCall(sql));
	String result="[";
	boolean check=false;
	for (Object obj : list) {
		Map<String, Object> map = (Map<String, Object>) obj;
		if(!result.equals("[")){
			result+=",";
		}
		String item="";
		//计划月份用户编号用户姓名地址上次底数本次底数用气量
		item+="{";
		item+="f_userid:'"+map.get("f_userid")+"',";
		item+="f_username:'"+map.get("f_username")+"',";
		item+="f_districtname:'"+map.get("f_districtname")+"',";
		item+="lastinputgasnum:"+map.get("lastinputgasnum");
		item+="}";
		
		result += item;
	}
	result+="]";
	System.out.println(result);
	return result;
}

	// 单块表抄表录入
	// 本方法不可重入
	@SuppressWarnings("unchecked")
	@GET
	@Path("record/one/{userid}/{reading}/{sgnetwork}/{sgoperator}/{lastinputdate}/{handdate}")
	@Produces("application/json")
	public String RecordInputForOne(
			@PathParam("userid") String userid,
			@PathParam("reading") double reading,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate,
			@PathParam("handdate") String handdate) {
		Map<String,String> singles = getSingles();// 获取所有单值
		try {
			String hql = "";
			final String sql = "select u.f_userid f_userid, u.f_zhye f_zhye , u.lastinputgasnum lastinputgasnum, u.f_gasprice , u.f_username  f_username,"
					+ "u.f_address f_address,u.f_districtname f_districtname,u.f_gasmeterstyle f_gasmeterstyle, u.f_idnumber f_idnumber, u.f_gaswatchbrand f_gaswatchbrand, u.f_usertype f_usertype, "
					+ "u.f_gasproperties f_gasproperties,u.f_dibaohu f_dibaohu, u.f_payment f_payment,u.f_zerenbumen f_zerenbumen,u.f_menzhan f_menzhan,u.f_inputtor f_inputtor, isnull(q.c,0) c,"
					+ "u.lastinputgasnum lastinputgasnum, h.id id from (select * from t_handplan where f_state='未抄表' and f_userid='"
					+ userid
					+ "') h "
					+ "left join (select f_userid, COUNT(*) c from t_handplan where f_state='已抄表' and shifoujiaofei='否' "
					+ "group by f_userid) q on h.f_userid=q.f_userid join t_userfiles u on h.f_userid=u.f_userid";
			List<Map<String, Object>> list =(List<Map<String, Object>>) hibernateTemplate.execute(new HibernateCallback() {
				public Object doInHibernate(Session session)throws HibernateException {
					Query q = session.createSQLQuery(sql);
					q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
					List result = q.list();
					return result;
				}
			});
			// 取出未抄表记录以及资料
			Map<String, Object> map = (Map<String, Object>) list.get(0);
			// 上期读数（上期的本次抄表底数）上期底数（）
			BigDecimal lastReading = new BigDecimal(map.get("lastinputgasnum")+ "");
			// 将本次读数转成BigDecimal
			BigDecimal readingThis = new BigDecimal(reading + "");
			// 低保户
			String dibaohu=map.get("f_dibaohu")+"";
			//低保户临界气量
			BigDecimal poorBorderAmount = new BigDecimal(singles.get("低保户临界气量"));
			//民用临界气量
			BigDecimal borderAmount = new BigDecimal(singles.get("民用临界气量"));
			//低保户临界外气价
			BigDecimal poorOverPrice = new BigDecimal(singles.get("低保户临界外气价"));
			//低保户临界内气价
			BigDecimal poorPrice = new BigDecimal(singles.get("低保户气价"));
			//普通民用临界外气价
			BigDecimal overPrice = new BigDecimal(singles.get("临界外气价"));
			//普通民用临界内气价
			BigDecimal gasPrice = new BigDecimal(singles.get("民用气价"));
			// 计算金额
			BigDecimal amount = null;
			// 气量
			BigDecimal gas = readingThis.subtract(lastReading);
			// 是低保户
			if(dibaohu.equals("1")){
				//大于低保户临界气量
				if(gas.compareTo(poorBorderAmount)>0 && gas.compareTo(borderAmount)<=0){
					//临界外气量(计算大于低保户临界气量的部分)
					BigDecimal multiOverPoorBorderAmount=gas.subtract(poorBorderAmount);
					//气费=（低保户临界气量*低保户临界内气价）+(临界外气量*低保户临界外气价)
					amount=(poorBorderAmount.multiply(poorPrice)).add(multiOverPoorBorderAmount.multiply(poorOverPrice));
				}
				//计算气量大于民用临界气量
				else if(gas.compareTo(borderAmount)>0){
					//临界外气量(计算大于民用临界气量的部分)
					BigDecimal multiOverBorderAmount=gas.subtract(borderAmount);
					//气费=((民用临界气量-低保户临界气量)*临界外气价）+（低保户临界气量*低保户临界内气价）+(临近外气量*民用气价)
					amount=((borderAmount.subtract(poorBorderAmount)).multiply(poorOverPrice)).add(poorBorderAmount.multiply(poorPrice)).add(multiOverBorderAmount.multiply(gasPrice));
				}
				else{
					amount=poorPrice.multiply(gas);//临界内气费
				}
			//不是低保户
			}else{
				//大于临界气量	
				if(gas.compareTo(borderAmount)>0){
					BigDecimal overBorderAmount=gas.subtract(borderAmount);
					//气费=（临界外气量*临界外气价）+（临界气量*临界内气价）
					amount=(overBorderAmount.multiply(overPrice)).add(borderAmount.multiply(gasPrice));
					}
				else{
					//临界内气费
					amount=gasPrice.multiply(gas);
				}
			}
			// 从户里取出余额(上期余额)
			BigDecimal f_zhye = new BigDecimal(map.get("f_zhye") + "");
			//用户地址
			String address = map.get("f_address").toString();
			//用户姓名
			String username = map.get("f_username").toString();
			// 以前欠费条数
			int items = Integer.parseInt(map.get("c") + "");
			//抄表id
			String handid = map.get("id") + "";
			// 用户缴费类型
			String payment = map.get("f_payment").toString();
			// 责任部门
			String zerenbumen ="空";
			// 门站
			String menzhan ="空";
			// 抄表员
			String inputtor = map.get("f_inputtor")+"";
			//最后一次抄表日期
			DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
			String dateStr=lastinputdate.substring(0, 10);
			Date lastinputDate=df.parse(dateStr);
			//取出抄表日期得到缴费截止日期DateFormat.parse(String s) 
			Date date=endDate(lastinputdate);//缴费截止日期
			//录入日期
			Date date1=new Date();
			String date1Str=lastinputdate.substring(0, 10);
			Date inputdate=df.parse(date1Str);
			//计划月份
//			DateFormat hd=new SimpleDateFormat("yyyy-MM");
//			String dateStr1=handdate.substring(0, 7);
//			Date handDate=hd.parse(dateStr1);
			// 如果用户缴费类型是免费
			if (payment.equals("免费")) {
				// 插入缴费记录
				Map<String, Object> sell = new HashMap<String, Object>();
				sell.put("f_userid", map.get("f_userid")); // 用户ID
				sell.put("f_payfeevalid", "有效");// 交费是否有效
				sell.put("f_payfeetype", "免费");// 收费类型
				sell.put("lastinputgasnum", lastReading.setScale(0).doubleValue()); // 上期底数
				sell.put("lastrecord", readingThis.setScale(0).doubleValue()); // 本期底数
				sell.put("f_totalcost", 0.0); // 应交金额
				sell.put("f_grossproceeds", 0.0); // 收款
				sell.put("f_deliverydate", new Date()); // 交费日期
				sell.put("f_zhye", f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 上期结余
				sell.put("f_benqizhye", f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 本期结余
				sell.put("f_gasmeterstyle", map.get("f_gasmeterstyle")); // 气表类型
				sell.put("f_comtype", "天然气公司"); // 公司类型，分为天然气公司、银行
				sell.put("f_username", map.get("f_username")); // 用户/单位名称
				sell.put("f_address", map.get("f_address")); // 地址
				sell.put("f_districtname", map.get("f_districtname")); // 小区名称
				sell.put("f_idnumber", map.get("f_idnumber")); // 身份证号
				sell.put("f_gaswatchbrand", map.get("f_gaswatchbrand")); // 气表品牌
				sell.put("f_gaspricetype", map.get("f_gaspricetype")); // 气价类型
				sell.put("f_gasprice", gasPrice.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 气价
				sell.put("f_usertype", map.get("f_usertype")); // 用户类型
				sell.put("f_gasproperties", map.get("f_gasproperties")); // 用气性质
				sell.put("f_pregas", gas.setScale(0).doubleValue()); // 气量
				sell.put("f_payment", "现金"); // 付款方式
				sell.put("f_sgnetwork", sgnetwork); // 网点
				sell.put("f_sgoperator", sgoperator); // 操 作 员
				sell.put("f_filiale", "新康天然气公司"); // 分公司
				sell.put("f_useful", handid); // 抄表id
				hibernateTemplate.save("t_sellinggas", sell);

				hql = "update t_userfiles set f_zhye=?,lastinputgasnum=?," +
				// 本次抄表日期
						"  lastinputdate=? " +
						// 当前表累计购气量 （暂） 总累计购气量
						// "f_metergasnums= f_metergasnums + ?, f_cumulativepurchase=f_cumulativepurchase+? ,"
						// 最后购气量 最后购气日期 最后购气时间
						// "f_finallybought= ?, f_finabuygasdate=?, f_finabuygastime=? "
						"where f_userid=?";

				hibernateTemplate.bulkUpdate(hql, new Object[] {
						f_zhye.setScale(0).doubleValue(),
						readingThis.setScale(0).doubleValue(), lastinputDate,
						userid });

				// 更新抄表记录
				hql = "update t_handplan set f_state ='已抄表',shifoujiaofei='是',"
						+
						// 本次抄表日期 本期底数
						"lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=?,lastrecord=? ,oughtamount=? ,oughtfee=? ,f_address=?, f_username=?"
						+ "where f_userid=? and f_state='未抄表'";
				hibernateTemplate.bulkUpdate(hql, new Object[] { lastinputDate,
						zerenbumen, menzhan, inputtor,
						readingThis.setScale(0).doubleValue(),
						gas.setScale(0).doubleValue(),
						amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(), address, username,userid });
			}
			// 如果用户档案的交费类型为工资代扣
			else if (payment.equals("工资代扣")) {
				// 插入缴费记录
				Map<String, Object> sell = new HashMap<String, Object>();
				sell.put("f_userid", map.get("f_userid")); // 用户ID
				sell.put("f_payfeevalid", "有效");// 交费是否有效
				sell.put("f_payfeetype", "工资代扣");// 收费类型
				sell.put("lastinputgasnum", lastReading.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 上期底数
				sell.put("lastrecord", readingThis.setScale(0).doubleValue()); // 本期底数
				sell.put("f_totalcost", amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 应交金额
				sell.put("f_grossproceeds", amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 收款
				sell.put("f_deliverydate", new Date()); // 交费日期
				sell.put("f_zhye", f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 上期结余
				sell.put("f_benqizhye", f_zhye.setScale(0).doubleValue()); // 本期结余
				sell.put("f_gasmeterstyle", map.get("f_gasmeterstyle")); // 气表类型
				sell.put("f_comtype", "天然气公司"); // 公司类型，分为天然气公司、银行
				sell.put("f_username", map.get("f_username")); // 用户/单位名称
				sell.put("f_address", map.get("f_address")); // 地址
				sell.put("f_districtname", map.get("f_districtname")); // 小区名称
				sell.put("f_idnumber", map.get("f_idnumber")); // 身份证号
				sell.put("f_gaswatchbrand", map.get("f_gaswatchbrand")); // 气表品牌
				sell.put("f_gaspricetype", map.get("f_gaspricetype")); // 气价类型
				sell.put("f_gasprice", gasPrice.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 气价
				sell.put("f_usertype", map.get("f_usertype")); // 用户类型
				sell.put("f_gasproperties", map.get("f_gasproperties")); // 用气性质
				sell.put("f_pregas", gas.setScale(0).doubleValue()); // 气量
				sell.put("f_payment", "现金"); // 付款方式
				sell.put("f_sgnetwork", sgnetwork); // 网点
				sell.put("f_sgoperator", sgoperator); // 操 作 员
				sell.put("f_filiale", "新康燃气"); // 分公司
				sell.put("f_useful", handid); // 抄表id
				hibernateTemplate.save("t_sellinggas", sell);

				// 更新用户档案

				hql = "update t_userfiles set f_zhye=?,lastinputgasnum=?," +
				// 本次抄表日期
						"  lastinputdate=? " +
						// 当前表累计购气量 （暂） 总累计购气量
						// "f_metergasnums= f_metergasnums + ?, f_cumulativepurchase=f_cumulativepurchase+? ,"
						// 最后购气量 最后购气日期 最后购气时间
						// "f_finallybought= ?, f_finabuygasdate=?, f_finabuygastime=? "
						"where f_userid=?";

				hibernateTemplate.bulkUpdate(hql, new Object[] {
						f_zhye.setScale(0).doubleValue(),
						readingThis.setScale(0).doubleValue(), lastinputDate,
						userid });

				// 更新抄表记录
				hql = "update t_handplan set f_state ='已抄表',shifoujiaofei='是',"
						+
						// 本次抄表日期 本期底数
						"lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=? ,lastrecord=? ," 
						+
						// 用气量 用气费用
						"oughtamount=?,     oughtfee=? ,f_address=?, f_username=?, f_endjfdate=?"
						+ "where f_userid=? and f_state='未抄表'";
				hibernateTemplate.bulkUpdate(hql, new Object[] { lastinputDate,
						zerenbumen, menzhan, inputtor,
						readingThis.setScale(0).doubleValue(),
						gas.setScale(0).doubleValue(),
						amount.setScale(2).doubleValue(), address, username , date,userid });
			}
			// 如果用户结余大于用气费用且无欠费 记录
			else if (f_zhye.compareTo(amount)>=0 && items==0) {
				// 插入缴费记录
				Map<String, Object> sell = new HashMap<String, Object>();
				sell.put("f_userid", map.get("f_userid")); // 用户ID
				sell.put("f_payfeevalid", "有效");// 交费是否有效
				sell.put("f_payfeetype", "余存交费");// 收费类型
				sell.put("lastinputgasnum", lastReading.setScale(0).doubleValue()); // 上期底数
				sell.put("lastrecord", readingThis.setScale(0).doubleValue()); // 本期底数
				sell.put("f_totalcost", amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 应交金额
				sell.put("f_grossproceeds", amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 收款
				//sell.put("f_zhinajin", amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 收款
				sell.put("f_deliverydate", new Date()); // 交费日期
				sell.put("f_zhye", f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 上期结余
				sell.put("f_benqizhye", f_zhye.subtract(amount).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 本期结余
				sell.put("f_gasmeterstyle", map.get("f_gasmeterstyle")); // 气表类型
				sell.put("f_comtype", "天然气公司"); // 公司类型，分为天然气公司、银行
				sell.put("f_username", map.get("f_username")); // 用户/单位名称
				sell.put("f_address", map.get("f_address")); // 地址
				sell.put("f_districtname", map.get("f_districtname")); // 小区名称
				sell.put("f_idnumber", map.get("f_idnumber")); // 身份证号
				sell.put("f_gaswatchbrand", map.get("f_gaswatchbrand")); // 气表品牌
				sell.put("f_gaspricetype", map.get("f_gaspricetype")); // 气价类型
				sell.put("f_gasprice", gasPrice.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // 气价
				sell.put("f_usertype", map.get("f_usertype")); // 用户类型
				sell.put("f_gasproperties", map.get("f_gasproperties")); // 用气性质
				sell.put("f_pregas", gas.setScale(0).doubleValue()); // 气量
				sell.put("f_payment", "现金"); // 付款方式
				sell.put("f_sgnetwork", sgnetwork); // 网点
				sell.put("f_sgoperator", sgoperator); // 操 作 员
				sell.put("f_filiale", "新康天然气公司"); // 分公司
				sell.put("f_useful", handid); // 抄表id
				hibernateTemplate.save("t_sellinggas", sell);

				// 更新用户档案

				// 结余 上期底数
				hql = "update t_userfiles set f_zhye=?,lastinputgasnum=?," +
				// 本次抄表日期
						"  lastinputdate=? " +
						// 当前表累计购气量 （暂） 总累计购气量
						// "f_metergasnums= f_metergasnums + ?, f_cumulativepurchase=f_cumulativepurchase+? ,"
						// 最后购气量 最后购气日期 最后购气时间
						// "f_finallybought= ?, f_finabuygasdate=?, f_finabuygastime=? "
						
						"where f_userid=?";

				hibernateTemplate.bulkUpdate(hql, new Object[] {
						f_zhye.subtract(amount).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(),
						readingThis.setScale(0).doubleValue(), lastinputDate,
						userid });

				// 更新抄表记录
				hql = "update t_handplan set f_state ='已抄表',shifoujiaofei='是',"
						+
						// 本次抄表日期 本期底数
						"lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=? ,lastrecord=? ," 
						+
						// 用气量 用气费用
						"oughtamount=?,     oughtfee=? ,f_inputdate=?,f_network=?,f_operator=? ,f_address=?, f_username=?"
						+ "where f_userid=? and f_state='未抄表'";
				hibernateTemplate.bulkUpdate(hql, new Object[] { lastinputDate,
						zerenbumen, menzhan, inputtor,
						readingThis.setScale(0).doubleValue(),
						gas.setScale(0).doubleValue(),
						amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(),inputdate,sgnetwork,sgoperator ,address, username , userid });
			} else {
				// 更新用户档案
				hql = "update t_userfiles " +
				// 本次抄表底数 本次抄表日期
						"set lastinputgasnum=? ,  lastinputdate=?  where f_userid=?";
				hibernateTemplate.bulkUpdate(hql, new Object[] {
						readingThis.setScale(0).doubleValue(), lastinputDate, userid });

				// 欠费,更新抄表记录的状态f_state、抄表日期、本次抄表底数
				hql = "update t_handplan set f_state ='已抄表', shifoujiaofei='否',"
					+
					// 本次抄表日期  责任部门  门站  抄表员  本期底数
					"lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=?, lastrecord=? ," 
					+
					// 用气量     交费截止日期 用气费用
					"oughtamount=?,  f_endjfdate=? , oughtfee=?, f_inputdate=?,f_network=?,f_operator=? ,f_address=?, f_username=?"
					+ "where f_userid=? and f_state='未抄表'";
			hibernateTemplate.bulkUpdate(hql, new Object[] { lastinputDate,
					zerenbumen, menzhan, inputtor,
					readingThis.setScale(0).doubleValue(),
					gas.setScale(0).doubleValue(),date,
					amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(),inputdate,sgnetwork,sgoperator, address, username, userid });
			
			return "";
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new WebApplicationException(401);
		}
		return null;
	}
	// 获取所有单值，转换成Map
	private Map<String, String> getSingles() {
		Map result = new HashMap<String, String>();
		
		String sql = "select name,value from t_singlevalue";
		List<Map<String, Object>> list = this.hibernateTemplate.executeFind(new HibernateSQLCall(sql));
		for (Map<String, Object> hand : list) {
			result.put(hand.get("name"), hand.get("value"));
		}
		return result;
	}
	private Object String(String zerenbumen) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 取单值
	 * 
	 * @param name
	 *            单值名
	 * @return 单值值
	 */
	private String getSingleValue(String name) {
		final String sql = "select value from t_singlevalue where name='"
				+ name + "'";
		String value = (String) hibernateTemplate
				.execute(new HibernateCallback() {
					public Object doInHibernate(Session session)
							throws HibernateException {
						SQLQuery query = session.createSQLQuery(sql);
						// addScalar 显式指定返回数据的类型
						query.addScalar("value", Hibernate.STRING);
						return query.uniqueResult();
					}
				});
		return value;
	}

	// 批量抄表记录上传
	// data以JSON格式上传，[{userid:'用户编号', showNumber:本期抄表数},{}]
	@Path("record/batch/{handdate}/{sgnetwork}/{sgoperator}/{lastinputdate}")
	@POST
	public String RecordInputForMore(String data,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate,
			@PathParam("handdatete") String handdate)  {
		try {
			// 取出所有数据
			JSONArray rows = new JSONArray(data);
			// 对每一个数据，调用单个抄表数据处理过程
			for (int i = 0; i < rows.length(); i++) {
				JSONObject row = rows.getJSONObject(i);
				String userid = row.getString("userid");
				double reading = row.getDouble("reading");
				
				BigDecimal readingThis = new BigDecimal(reading + "");
				RecordInputForOne(userid, reading, sgnetwork, sgoperator,lastinputdate,handdate);
			}
		} catch (Exception e) {
			return "{\"ok\":\"nok\", msg:\"" + e.toString() + "\"}";
		}
		return "";
	}
	
	//产生交费截止日期
	private Date endDate(String str) throws ParseException {
		DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
		String dateStr=str.substring(0, 10);
		Date now=df.parse(dateStr);
		Calendar c=Calendar.getInstance();
		c.setTime(now);
		c.add(Calendar.MONTH, 1);
		c.set(Calendar.DATE, 10);
		return c.getTime();
	}
	// 转换器，在转换期间会检查对象是否已经转换过，避免重新转换，产生死循环
	class JsonTransfer {
		// 保存已经转换过的对象
		private List<Map<String, Object>> transed = new ArrayList<Map<String, Object>>();

		// 把单个map转换成JSON对象
		public Object MapToJson(Map<String, Object> map) {
			// 转换过，返回空对象
			if (contains(map))
				return JSONObject.NULL;
			transed.add(map);
			JSONObject json = new JSONObject();
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				try {
					String key = entry.getKey();
					Object value = entry.getValue();
					// 空值转换成JSON的空对象
					if (value == null) {
						value = JSONObject.NULL;
					} else if (value instanceof HashMap) {
						value = MapToJson((Map<String, Object>) value);
					}
					// 如果是$type$，表示实体类型，转换成EntityType
					if (key.equals("$type$")) {
						json.put("EntityType", value);
					} else if (value instanceof Date) {
						Date d1 = (Date) value;
						Calendar c = Calendar.getInstance();
						long time = d1.getTime() + c.get(Calendar.ZONE_OFFSET);
						json.put(key, time);
					} else if (value instanceof MapProxy) {
						// MapProxy没有加载，不管
					} else if (value instanceof PersistentSet) {
						PersistentSet set = (PersistentSet) value;
						// 没加载的集合不管
						if (set.wasInitialized()) {
							json.put(key, ToJson(set));
						}
					} else {
						json.put(key, value);
					}
				} catch (JSONException e) {
					throw new WebApplicationException(400);
				}
			}
			return json;
		}

		// 把集合转换成Json数组
		public Object ToJson(PersistentSet set) {
			JSONArray array = new JSONArray();
			for (Object obj : set) {
				Map<String, Object> map = (Map<String, Object>) obj;
				JSONObject json = (JSONObject) MapToJson(map);
				array.put(json);
			}
			return array;
		}

		// 判断已经转换过的内容里是否包含给定对象
		public boolean contains(Map<String, Object> obj) {
			for (Map<String, Object> map : this.transed) {
				if (obj == map) {
					return true;
				}
			}
			return false;
		}
	}
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

}