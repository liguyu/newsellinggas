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

import com.aote.rs.SellSer.HibernateSQLCall;

@Path("hand")
@Component
public class BusinessService {

	static Logger log = Logger.getLogger(BusinessService.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;
	
	//简单财务报表
	@GET
	@Path("statement/{sgnetwork}")
	public String dayStatement(@PathParam("sgnetwork") String sgnetwork){
		try{
		Date date = new Date();
		String result="";
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
		String sql ="select " +
				"SUM(f_pregas) f_pregas,SUM(f_preamount) f_preamount," +
				"SUM(f_zhye) f_zhye,SUM(f_zhinajin) f_zhinajin," +
				"SUM(f_grossproceeds) f_grossproceeds,SUM(f_benqizhye) f_benqizhye " +
				"from t_sellinggas where f_deliverydate='"+sdf.format(date)+"' "+
				"and f_sgnetwork='"+sgnetwork+"'";
		List<Map<String, Object>> list =(List<Map<String, Object>>) hibernateTemplate.execute(new HibernateSQLCall(sql));
		Map<String, Object> map=list.get(0);
		result+="[{f_pregas:"+map.get("f_pregas");
		result+=",f_preamount:"+map.get("f_preamount");
		result+=",f_zhye:"+map.get("f_zhye");
		result+=",f_zhinajin:"+map.get("f_zhinajin");
		result+=",f_grossproceeds:"+map.get("f_grossproceeds");
		result+=",f_benqizhye:"+map.get("f_benqizhye")+"}]";
		return result;
		}catch(Exception e){
			log.error(e.getMessage());
			throw new WebApplicationException(401);
		}
	}
	
	
	
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
	@POST
	@Path("download")
	public String downLoadRecord(String condition) {
		
		String sql = "select top 1000 h.f_userid,h.f_username,h.f_address,u.lastinputgasnum " +
				"from t_handplan h join t_userfiles u on u.f_userid=h.f_userid " +
				"where h.shifoujiaofei='否' and h.f_state='未抄表' and u.f_userstate!='销户' and " 
				+ condition + " and h.f_userid in (select f_userid from t_userfiles where f_userstate in ('正常','银行扣款'))" +
						" order by h.f_address,h.f_apartment";
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
			item+="f_address:'"+map.get("f_address")+"',";
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
	@Path("record/one/{userid}/{reading}/{sgnetwork}/{sgoperator}/{lastinputdate}")//userid是表id要注意取出户的id
	@Produces("application/json")
	public String RecordInputForOne(@PathParam("userid") String userid,
			@PathParam("reading") double readingDouble,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate) {

		Map<String,String> singles = getSingles();// 获取所有单值
		BigDecimal reading=new BigDecimal(readingDouble+"");//本期读数
		try {
			String hql="";
			final String sql="select " +
			"h.id handId," +
			"isnull(q.c,0) c," +//handplan
			"u.lastinputgasnum lastinputgasnum,u.f_userid f_userid,u.lastrecord lastrecord,u.lastinputdate u_lastinputdate," +//userfiles
			"ui.f_zhye f_zhye,ui.id infoid,ui.f_userid ui_userid,ui.f_username f_username,ui.f_address f_address," +
			"ui.f_districtname f_districtname,ui.f_gaspricetype f_gaspricetype," +
			"ui.f_idnumber f_idnumber,ui.f_gasprice f_gasprice,ui.f_usertype f_usertype," +
			"ui.f_gasproperties f_gasproperties,ui.f_dibaohu f_dibaohu " +//userinfo
			"from (select * from t_handplan where f_state='未抄表' and f_userid="+userid+
			") h left join (select f_userid, COUNT(*) c from t_handplan where f_state='已抄表' and shifoujiaofei='否' and f_userid=" +userid+
			" group by f_userid) q on h.f_userid=q.f_userid "+
			"join t_userfiles u on h.f_userid=u.f_userid "+
			"join t_userinfo ui on u.f_userinfoid=ui.id and u.f_userstate in ('正常','银行扣款')";
			List<Map<String, Object>> list =(List<Map<String, Object>>) hibernateTemplate.execute(new HibernateCallback() {
				public Object doInHibernate(Session session)throws HibernateException {
					Query q = session.createSQLQuery(sql);
					q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
					List result = q.list();
					return result;
				}
			});

			// 取出该表的未抄表记录以及户的资料
			Map<String, Object> map = (Map<String, Object>) list.get(0);
			// 上期读数（上期的本次抄表底数）上期底数（）
			BigDecimal lastReading = new BigDecimal(map.get("lastinputgasnum").toString());
			// 气量=本次指数-上次指数
			BigDecimal gas = reading.subtract(lastReading);
			
			// 气费
			String dibaohu=map.get("f_dibaohu").toString();
			//低保户临界气量
			BigDecimal boundAmount = new BigDecimal(singles.get("低保户临界气量"));
			//低保户临界外气价
			BigDecimal boundPrice = new BigDecimal(singles.get("低保户临界外气价"));
			//低保户临界内气价
			BigDecimal price = new BigDecimal(singles.get("低保户气价"));
			//普通民用气价
			BigDecimal gasPrice = new BigDecimal(map.get("f_gasprice").toString());
			// 计算金额
			BigDecimal amount = null;
			// 是低保户
			if(dibaohu.equals("1")){
				//大于临界气量
				if(gas.compareTo(boundAmount)>0){
					//临界外气量
					BigDecimal linjiewai=gas.subtract(boundAmount);
					//气费=（临界外气量*临界外气价）+（临界气量*临界内气价）
					amount=(boundPrice.multiply(linjiewai)).add(boundAmount.multiply(price));
				}else{
					amount=price.multiply(gas);//临界内气费
				}
			}else{
				amount=gasPrice.multiply(gas);//普通民用
			}
			
			
			// 从户里取出余额(上期余额)
			BigDecimal f_zhye = new BigDecimal(map.get("f_zhye")+"");
			
			//取出欠费条数
			int qianfeitiaoshu=Integer.parseInt(map.get("c").toString());
			
			//转为double便于存储
			double gasDoube=gas.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();//气量
			double amountDouble=amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();//气费
			double f_zhyeDouble=f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();//上期结余
			double f_zhyeThisDouble=0;//本期结余
			double lastinputgasnumDouble=lastReading.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();//本次上期抄表读数
			
			//取系统当前日期
			Date now=new Date();
			//最后一次抄表日期
			DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
			String dateStr=lastinputdate.substring(0, 10);
			Date lastinputDate=df.parse(dateStr);
			//取出抄表日期得到缴费截止日期DateFormat.parse(String s) 
			Date date=endDate(lastinputdate);//缴费截止日期

			// 余额>=金额，并且前面没有欠费，从余额中扣除，产生交费记录
			if (f_zhye.compareTo(amount)>=0 && qianfeitiaoshu==0) {
				//本期结余
				f_zhyeThisDouble=f_zhye.subtract(amount).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
				
				//添加交费记录
				Map<String, Object> sell = new HashMap<String, Object>();
				sell.put("f_userid", map.get("ui_userid").toString());	// 户的id
				sell.put("lastinputgasnum", lastinputgasnumDouble);	// 总上期指数
				sell.put("lastrecord", readingDouble);	// 总本期指数
				sell.put("f_totalcost", new Double(0));	// 应交金额
				sell.put("f_grossproceeds", new Double(0));	// 收款
				sell.put("f_zhinajin", new Double(0));	// 滞纳金
				sell.put("f_deliverydate", now);	// 交费日期
				sell.put("f_deliverytime", now);	// 交费时间
				sell.put("f_zhye", f_zhyeDouble);		//上期结余
				sell.put("f_benqizhye", f_zhyeThisDouble);	//本期结余
				sell.put("f_gasmeterstyle", "机表");	//气表类型
				sell.put("f_comtype", "天然气公司");	//公司类型，分为天然气公司、银行
				sell.put("f_username", map.get("f_username"));		//用户/单位名称
				sell.put("f_address", map.get("f_address"));	//地址
				sell.put("f_districtname", map.get("f_districtname"));	//小区名称
				sell.put("f_idnumber",  map.get("f_idnumber"));	//身份证号
				sell.put("f_gaswatchbrand", "机表");	//气表品牌
				sell.put("f_gaspricetype", map.get("f_gaspricetype"));	//气价类型
				sell.put("f_gasprice", map.get("f_gasprice")); //气价
				sell.put("f_usertype", map.get("f_usertype"));	//用户类型
				sell.put("f_gasproperties", map.get("f_gasproperties")); //用气性质
				sell.put("f_pregas", gasDoube);	//气量
				sell.put("f_preamount", amountDouble);	//气费
				sell.put("f_payment", "现金");	//付款方式
				sell.put("f_paytype", "现金");	//交费类型，银行代扣/现金
				sell.put("f_sgnetwork", sgnetwork);	//网点
				sell.put("f_sgoperator", sgoperator);	//操 作 员
				sell.put("f_filiale", "安顺达管道天然气有限公司");	//分公司
				sell.put("f_fengongsinum", "11");	//分公司编号
				sell.put("f_payfeetype", "余存交费");	//交易类型
				sell.put("f_payfeevalid", "有效");	//购气有效类型
				//sell.put("f_useful",map.get("handId").toString() );	//抄表记录id
				sell.put("f_users", userid);	//抄表记录用户id
				hibernateTemplate.save("t_sellinggas", sell);
				 final String Sql = "select id from t_sellinggas where f_userid ='" + userid +"' order by f_deliverydate desc";
					List<Map<String, Object>> sellList =(List<Map<String, Object>>) hibernateTemplate.execute(new HibernateCallback() {
						public Object doInHibernate(Session session)throws HibernateException {
							Query q = session.createSQLQuery(Sql);
							q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
							List result = q.list();
							return result;
						}
					});
			// 取出未抄表记录以及用户信息
			Map<String, Object> handmap = (Map<String, Object>) sellList.get(0);
			String sellId = handmap.get("id").toString();
				
				// 更新户里余额
				hibernateTemplate.bulkUpdate(
						"update t_userinfo set f_zhye=? where id=?",
						new Object[]{ f_zhyeThisDouble, map.get("infoid")});
				// 更新用户档案							上次抄表读数
				hql = "update t_userfiles set lastinputgasnum=?,"+
					//最后抄表日期(应该是抄表是生成)
					"lastinputdate=?, "+
					//当前表累计购气量 						  总累计购气量
					"f_metergasnums= f_metergasnums + ?, f_cumulativepurchase=f_cumulativepurchase+? ,"+
					// 最后购气量 			最后购气日期 			最后购气时间
					"f_finallybought=?, f_finabuygasdate=?, f_finabuygastime=? "+
					"where f_userid=?";
				hibernateTemplate.bulkUpdate(hql, new Object[] {readingDouble,lastinputDate,gasDoube,
						gasDoube,gasDoube,now,now, userid });
				// 更新抄表记录的状态				抄表状态			收费状态
				hql = "update t_handplan set f_state ='已抄表',shifoujiaofei='是'," +
					//上次抄表日期					上期指数	   操作员			网点			录入日期
						"scinputdate=?,lastinputgasnum=?,f_operator=?,f_network=?,f_inputdate=?," +
						//本期指数			用气量		用气费用		本次抄表日期
						"lastrecord=?,oughtamount=?,oughtfee=?,f_address=?,f_username=?,lastinputdate=?,f_sellid=?, " +
						//交费截止日期
						"f_endjfdate=? " +
						"where f_userid=? and f_state='未抄表'";
				hibernateTemplate.bulkUpdate(hql, new Object[] {
						map.get("u_lastinputdate"),lastinputgasnumDouble,sgoperator,sgnetwork,now,
						readingDouble,gasDoube,amountDouble,map.get("f_address"),map.get("f_username"),lastinputDate,sellId,date,userid });
				return "";
			} else {
				// 更新用户档案							上次抄表读数
				hql = "update t_userfiles set lastinputgasnum=?,"+
					//最后抄表日期(应该是抄表时生成)
					"lastinputdate=?, "+
					// 当前表累计购气量 						总累计购气量
					"f_metergasnums?, f_cumulativepurchase=? ,"+
					// 最后购气量 			最后购气日期 			最后购气时间
					"f_finallybought=?, f_finabuygasdate=?, f_finabuygastime=? "+
					"where f_userid=?";
				hibernateTemplate.bulkUpdate(hql, new Object[] {
						readingDouble,lastinputDate,gasDoube,gasDoube,gasDoube,
						now,now,userid });
				// 欠费,更新抄表记录的状态f_state、抄表日期
				hql = "update t_handplan set f_state ='已抄表',shifoujiaofei='否'," +
				//上次抄表日期				上期指数	   操作员			网点			录入日期
				"scinputdate=?,lastinputgasnum=?,f_operator=?,f_network=?,f_inputdate=?," +
				//本期指数			用气量		用气费用		本次抄表日期
				"lastrecord=?,oughtamount=?,oughtfee=?,f_address=?,f_username=?,lastinputdate=?," +
				// 交费截止月份
				"f_endjfdate=? " +
				"where f_userid=? and f_state='未抄表'";
				hibernateTemplate.bulkUpdate(hql, new Object[] {
						map.get("u_lastinputdate"),lastinputgasnumDouble,sgoperator,sgnetwork,now,
						readingDouble,gasDoube,amountDouble,map.get("f_address"),map.get("f_username"),lastinputDate,date,userid });
				return "";
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new WebApplicationException(401);
		}
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
	
	
	/**
	 * 产生缴费记录
	 * 
	 * @param userid
	 *            用户id
	 * @param amount
	 *            气量
	 * @param payment
	 *            缴费额
	 */
	private void InsertSellingRecord(String userid, double amount,
			double payment) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("f_userid", userid);
		map.put("f_myqiliang", new Double(amount));
		map.put("f_myqiliangjine", new Double(payment));
		// TODO 其他字段怎么办
		this.hibernateTemplate.saveOrUpdate("t_sellinggas", map);
		this.hibernateTemplate.flush();
	}

	/**
	 * 计算金额
	 * 
	 * @param userid
	 *            用户id
	 * @param amount
	 *            气量
	 * @param userType
	 *            用户类型（民用、非民用）
	 * @param isLowIncome
	 *            是否低保户
	 * @param nonCivilPrice
	 *            非民用气价
	 * @return 应交费金额
	 */
	private double getAmount(String userid, int amount, String userType,
			boolean isLowIncome, double nonCivilPrice) {
		if (userType.equals("民用")) {
			int boundAmount;
			double boundPrice;
			double price;
			if (isLowIncome) {
				
				boundAmount = Integer.parseInt(getSingleValue("低保户临界气量"));
				boundPrice = Double.parseDouble(getSingleValue("低保户临界外气价"));
				price = Double.parseDouble(getSingleValue("低保户气价"));
			} else {
				boundAmount = Integer.parseInt(getSingleValue("民用临界气量"));
				boundPrice = Double.parseDouble(getSingleValue("临界外气价"));
				price = Double.parseDouble(getSingleValue("民用气价"));
			}
			if (boundAmount >= amount)
				return amount * price;
			else
				return amount * price + (amount - boundAmount) * boundPrice;
		}
		// 非民用
		else
			return amount * nonCivilPrice;
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
	@Path("record/batch/{sgnetwork}/{sgoperator}/{lastinputdate}")
	@POST
	public String RecordInputForMore(String data,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate) {
		try {
			// 取出操作员
			// String operator = dmp.getString("operator");
			// 取出所有数据
			JSONArray rows = new JSONArray(data);
			// 对每一个数据，调用单个抄表数据处理过程
			JSONObject row =null;
			for (int i = 0; i < rows.length(); i++) {
				row = rows.getJSONObject(i);
				String userid = row.getString("userid");
				double showNumber = row.getDouble("reading");
				RecordInputForOne(userid,showNumber,sgnetwork,sgoperator,lastinputdate);
//				if (result.getString("ok").equals("nok"))
//					return result.toString();
//				// 如果欠费
//				else if (result.has("err")) {
//					JSONObject err = new JSONObject(result.getString("err"));
//					errs.put(err);
//				}
			}
//			return "{\"ok\":\"ok\",\"err\":" + errs.toString() + "}";
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
		c.set(Calendar.DATE, 20);
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
