package com.aote.rs.charge;

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

import com.aote.rs.charge.HandCharge.HibernateSQLCall;

@Path("handcharge")
@Component
public class HandCharge {

	static Logger log = Logger.getLogger(HandCharge.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;


	private int stairmonths;
	
	private String stardate;
	private String enddate;
	
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
		
		String sql = "select top 1000 u.f_userid,u.f_username,u.f_address,u.lastinputgasnum " +
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
	@Path("record/one/{userid}/{reading}/{sgnetwork}/{sgoperator}/{lastinputdate}/{handdate}")
	@Produces("application/json")
	public String RecordInputForOne(
			@PathParam("userid") String userid,
			@PathParam("reading") double reading,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate,
			@PathParam("handdate") String handdate) {
		double chargenum = 0;
		double stair1num = 0;
		double stair2num = 0;
		double stair3num = 0;
		double stair4num = 0;
		double stair1fee = 0;
		double stair2fee = 0;
		double stair3fee = 0;
		double stair4fee = 0;
		double sumamont = 0;
		try {
			String hql = "";
			final String sql = "select isnull(u.f_userid,'') f_userid, isnull(u.f_zhye,'') f_zhye , isnull(u.lastinputgasnum,'') lastinputgasnum, isnull(u.f_gasprice,0) f_gasprice, isnull(u.f_username,'')  f_username,"
					+ "isnull(u.f_stair1amount,0)f_stair1amount,isnull(u.f_stair2amount,0)f_stair2amount,isnull(u.f_stair3amount,0)f_stair3amount,isnull(u.f_stair1price,0)f_stair1price,isnull(u.f_stair2price,0)f_stair2price,isnull(u.f_stair3price,0)f_stair3price,isnull(u.f_stair4price,0)f_stair4price,isnull(u.f_stairmonths,0)f_stairmonths,isnull(u.f_stairtype,'未设')f_stairtype,"
					+ "isnull(u.f_address,'')f_address ,isnull(u.f_districtname,'')f_districtname,isnull(u.f_gasmeterstyle,'') f_gasmeterstyle, isnull(u.f_idnumber,'') f_idnumber, isnull(u.f_gaswatchbrand,'')f_gaswatchbrand, isnull(u.f_usertype,'')f_usertype, "
					+ "isnull(u.f_gasproperties,'')f_gasproperties,isnull(u.f_dibaohu,0)f_dibaohu,isnull(u.f_payment,'')f_payment,isnull(u.f_zerenbumen,'')f_zerenbumen,isnull(u.f_menzhan,'')f_menzhan,isnull(u.f_inputtor,'')f_inputtor, isnull(q.c,0) c,"
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
			double gasprice = Double.parseDouble(map.get("f_gasprice").toString());
			String stairtype = map.get("f_stairtype").toString();
			double stair1amount = Double.parseDouble(map.get("f_stair1amount").toString());
			double stair2amount = Double.parseDouble(map.get("f_stair2amount").toString());
			double stair3amount = Double.parseDouble(map.get("f_stair3amount").toString());
			double stair1price = Double.parseDouble(map.get("f_stair1price").toString());
			double stair2price = Double.parseDouble(map.get("f_stair2price").toString());
			double stair3price = Double.parseDouble(map.get("f_stair3price").toString());
			double stair4price = Double.parseDouble(map.get("f_stair4price").toString());
			stairmonths = Integer.parseInt(map.get("f_stairmonths").toString());
			// 上期读数（上期的本次抄表底数）上期底数（）
			double lastReading = Double.parseDouble(map.get("lastinputgasnum")+ "");
			// 气量
			double gas = reading-lastReading;
			// 从户里取出余额(上期余额)
			double f_zhye = Double.parseDouble(map.get("f_zhye") + "");
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
			Date inputdate=new Date();
			//计划月份
			DateFormat hd=new SimpleDateFormat("yyyy-MM");
			String dateStr1=handdate.substring(0, 7);
			Date handDate=hd.parse(dateStr1);
			//针对设置阶梯气价的用户运算
			CountDate();
			if(!stairtype.equals("未设")){
		        final String gassql = " select isnull(sum(oughtamount),0)oughtamount from t_handplan " +
		        		"where f_userid='"+userid+"' and lastinputdate>='"+stardate+"' and lastinputdate<='"+enddate+"'";
				List<Map<String, Object>> gaslist =(List<Map<String, Object>>) hibernateTemplate.execute(new HibernateCallback() {
					public Object doInHibernate(Session session)throws HibernateException {
						Query q = session.createSQLQuery(gassql);
						q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
						List result = q.list();
						return result;
					}
				});
				Map<String, Object> gasmap = (Map<String, Object>) gaslist.get(0);
				//当前购气量
				sumamont = Double.parseDouble(gasmap.get("oughtamount").toString());
				//累计购气量
				double allamont = sumamont+gas;
				//当前购气量在第一阶梯
				if(sumamont<stair1amount){
					if(allamont<stair1amount){
						stair1num = gas;
						stair1fee = gas*stair1price;
						chargenum = gas*stair1price;
					}else if(allamont>=stair1amount && allamont<stair2amount){
						stair1num = stair1amount-sumamont;
						stair1fee = (stair1amount-sumamont)*stair1price;
						stair2num = allamont-stair1amount;
						stair2fee = (allamont-stair1amount)*stair2price;
						chargenum = stair1fee+stair2fee;
					}else if(allamont>=stair2amount && allamont<stair3amount){
						stair1num = stair1amount-sumamont;
						stair1fee = (stair1amount-sumamont)*stair1price;
						stair2num = stair2amount-stair1amount;
						stair2fee = (stair2amount-stair1amount)*stair2price;
						stair3num = allamont-stair2amount;
						stair3fee = (allamont-stair2amount)*stair3price;
						chargenum = stair1fee+stair2fee+stair3fee;
					}else if(allamont>=stair3amount){
						stair1num = stair1amount-sumamont;
						stair1fee = (stair1amount-sumamont)*stair1price;
						stair2num = stair2amount-stair1amount;
						stair2fee = (stair2amount-stair1amount)*stair2price;
						stair3num = stair3amount-stair2amount;
						stair3fee = (stair3amount-stair2amount)*stair3price;
						stair4num = allamont-stair3amount;
						stair4fee = (allamont-stair3amount)*stair4price;
						chargenum = stair1fee+stair2fee+stair3fee+stair4fee;
					}
				//当前已购气量在阶梯二内
				}else if(sumamont>=stair1amount && sumamont<stair2amount){
					if(allamont<stair2amount){
						stair2num = gas;
						stair2fee = gas*stair2price;
						chargenum = stair2fee;
					}else if(allamont>=stair2amount && allamont<stair3amount){
						stair2num = stair2amount-sumamont;
						stair2fee = (stair2amount-sumamont)*stair2price;
						stair3num = allamont-stair2amount;
						stair3fee = (allamont-stair2amount)*stair3price;
						chargenum = stair2fee+stair3fee;
					}else{
						stair2num = stair2amount-sumamont;
						stair2fee = (stair2amount-sumamont)*stair2price;
						stair3num = stair3amount-stair2amount;
						stair3fee = (stair3amount-stair2amount)*stair3price;
						stair4num = allamont-stair3amount;
						stair4fee = (allamont-stair3amount)*stair4price;
						chargenum = stair2fee+stair3fee+stair4fee;
					}
				//当前已购气量在阶梯三内
				}else if(sumamont>=stair2amount && sumamont<stair3amount){
					if(allamont<stair3amount){
						stair3num = gas;
						stair3fee = gas*stair3price;
						chargenum = stair3fee;
					}else{
						stair3num = stair3amount-sumamont;
						stair3fee = (stair3amount-sumamont)*stair3price;
						stair4num = allamont-stair3amount;
						stair4fee = (allamont-stair3amount)*stair4price;
						chargenum = stair3fee+stair4fee;
					}
				//当前已购气量超过阶梯三
				}else if(sumamont>=stair3amount){
					stair4num = gas;
					stair4fee = gas*stair4price;
					chargenum =	stair4fee;
				}
			//该用户未设置阶梯气价
			}else{
				chargenum = gas*gasprice;
				stair1num = 0;
				stair2num = 0;
				stair3num = 0;
				stair4num = 0;
				stair1fee = 0;
				stair2fee = 0;
				stair3fee = 0;
				stair4fee = 0;
			}
			if(chargenum<f_zhye && items<1){
				//自动下账
				Map<String, Object> sell = new HashMap<String, Object>();
				sell.put("f_userid", map.get("f_userid")); // 用户ID
				sell.put("f_payfeevalid", "有效");// 交费是否有效
				sell.put("f_payfeetype", "自动下账");// 收费类型
				sell.put("lastinputgasnum", lastReading); // 上期底数
				sell.put("lastrecord", reading); // 本期底数
				sell.put("f_totalcost", chargenum); // 应交金额
				sell.put("f_grossproceeds", chargenum); // 收款
				sell.put("f_deliverydate", new Date()); // 交费日期
				sell.put("f_zhye", f_zhye); // 上期结余
				sell.put("f_benqizhye", f_zhye-chargenum); // 本期结余
				sell.put("f_gasmeterstyle", map.get("f_gasmeterstyle")); // 气表类型
				sell.put("f_comtype", "天然气公司"); // 公司类型，分为天然气公司、银行
				sell.put("f_username", map.get("f_username")); // 用户/单位名称
				sell.put("f_address", map.get("f_address")); // 地址
				sell.put("f_districtname", map.get("f_districtname")); // 小区名称
				sell.put("f_idnumber", map.get("f_idnumber")); // 身份证号
				sell.put("f_gaswatchbrand", map.get("f_gaswatchbrand")); // 气表品牌
				sell.put("f_gaspricetype", map.get("f_gaspricetype")); // 气价类型
				sell.put("f_gasprice", gasprice); // 气价
				sell.put("f_usertype", map.get("f_usertype")); // 用户类型
				sell.put("f_gasproperties", map.get("f_gasproperties")); // 用气性质
				sell.put("f_pregas", gas); // 气量
				sell.put("f_payment", "现金"); // 付款方式
				sell.put("f_sgnetwork", sgnetwork); // 网点
				sell.put("f_sgoperator", sgoperator); // 操 作 员
				sell.put("f_filiale", "淄博绿川天然气有限公司"); // 分公司
				sell.put("f_useful", handid); // 抄表id
				sell.put("f_stair1amount", stair1num);
				sell.put("f_stair2amount", stair2num);
				sell.put("f_stair3amount", stair3num);
				sell.put("f_stair4amount", stair4num);
				sell.put("f_stair1fee", stair1fee);
				sell.put("f_stair2fee", stair2fee);
				sell.put("f_stair3fee", stair3fee);
				sell.put("f_stair4fee", stair4fee);
				sell.put("f_stair1price", stair1price);
				sell.put("f_stair2price", stair2price);
				sell.put("f_stair3price", stair3price);
				sell.put("f_stair4price", stair4price);
				sell.put("f_stardate", stardate);
				sell.put("f_enddate", enddate);
				sell.put("f_allamont", sumamont);
				int sellid = (Integer)hibernateTemplate.save("t_sellinggas", sell);
				hql = "update t_userfiles set f_zhye=?,lastinputgasnum=?," +
				// 本次抄表日期
						"  lastinputdate=? " +
						// 当前表累计购气量 （暂） 总累计购气量
						// "f_metergasnums= f_metergasnums + ?, f_cumulativepurchase=f_cumulativepurchase+? ,"
						// 最后购气量 最后购气日期 最后购气时间
						// "f_finallybought= ?, f_finabuygasdate=?, f_finabuygastime=? "
						"where f_userid=?";

				hibernateTemplate.bulkUpdate(hql, new Object[] {
						f_zhye-chargenum,reading, lastinputDate,userid });
				String sellId = sellid+"";
				// 更新抄表记录
				hql = "update t_handplan set f_state ='已抄表',shifoujiaofei='是',f_handdate=?,f_stairtype=?,"
						+ "lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=?,lastrecord=? ,oughtamount=? ,oughtfee=? ,f_address=?, f_username=?, f_zhye=?, f_bczhye=?,"
						+ "f_stair1amount=?,f_stair2amount=?,f_stair3amount=?,f_stair4amount=?,f_stair1fee=?,f_stair2fee=?,f_stair3fee=?,f_stair4fee=?,f_stair1price=?,f_stair2price=?,f_stair3price=?,f_stair4price=?,"
						+ "f_stardate=?,f_enddate=?,f_allamont=? ,f_sellid=?"
						+ "where f_userid=? and f_state='未抄表'";
				hibernateTemplate.bulkUpdate(hql, new Object[] { handDate,stairtype,
						lastinputDate,zerenbumen, menzhan, inputtor,reading,
						gas,chargenum, address, username,f_zhye,f_zhye-chargenum,
						stair1num,stair2num,stair3num,stair4num,
						stair1fee,stair2fee,stair3fee,stair4fee,
						stair1price,stair2price,stair3price,stair4price,
						stardate,enddate,sumamont,sellId,
						userid });
			}else{
				// 更新用户档案
				hql = "update t_userfiles " +
				// 本次抄表底数 本次抄表日期
						"set lastinputgasnum=? ,  lastinputdate=?  where f_userid=?";
				hibernateTemplate.bulkUpdate(hql, new Object[] {reading, lastinputDate, userid });

				// 欠费,更新抄表记录的状态f_state、抄表日期、本次抄表底数
				hql = "update t_handplan set f_state ='已抄表', shifoujiaofei='否',f_handdate=?,"
					+ "lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=?, lastrecord=? ,f_stairtype=?," 
					+ "oughtamount=?,  f_endjfdate=? , oughtfee=?, f_inputdate=?,f_network=?,f_operator=? ,f_address=?, f_username=?,"
					+ "f_stair1amount=?,f_stair2amount=?,f_stair3amount=?,f_stair4amount=?,f_stair1fee=?,f_stair2fee=?,f_stair3fee=?,f_stair4fee=?," 
					+ "f_stair1price=?,f_stair2price=?,f_stair3price=?,f_stair4price=?,"
					+ "f_stardate=?,f_enddate=?,f_allamont=? "
					+ "where f_userid=? and f_state='未抄表'";
			hibernateTemplate.bulkUpdate(hql, new Object[] { handDate,lastinputDate,
					zerenbumen, menzhan, inputtor,reading,stairtype,gas,date,
					chargenum,inputdate,sgnetwork,sgoperator, address, username,
					stair1num,stair2num,stair3num,stair4num,
					stair1fee,stair2fee,stair3fee,stair4fee,
					stair1price,stair2price,stair3price,stair4price,
					stardate,enddate,sumamont,
					userid });
			}
			return "";
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new WebApplicationException(401);
		}
	}
	
	//计算开始时间方法
	private	void CountDate(){
		//计算当前月在哪个阶梯区间
		Calendar cal = Calendar.getInstance();
		int thismonth = cal.get(Calendar.MONTH) + 1;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		if(stairmonths==1){
			cal.set(Calendar.DAY_OF_MONTH,1);
	        stardate = format.format(cal.getTime());
	        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));  
	        enddate = format.format(cal.getTime());
		}else{
			/*阶梯起始月数计算
			 *起始月 = 当前月/阶梯月数*阶梯月数+1
			 *结束月 = 当前月/阶梯月数*阶梯月数+阶梯月数
			 *注：该运算 当前月是12月时则需要剪1 上面已经算出阶梯月数为1个月时的金额
			 *一下运算阶梯月数至少为两个月  所以对算区间没有影响
			 * */
			if(thismonth==12){
				thismonth=11;
			}
			//计算起始月
			int star = Math.round(thismonth/stairmonths)*stairmonths+1;
			//计算结束月
			int end = Math.round(thismonth/stairmonths)*stairmonths+stairmonths;
			//获得起始日期和结束日期
	        cal.set(Calendar.MONTH, star-1);
	        cal.set(Calendar.DAY_OF_MONTH,1);
	        stardate = format.format(cal.getTime());
		    cal.set(Calendar.MONTH, end-1);
	        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
	        enddate = format.format(cal.getTime());
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
	private Object String(String zerenbumen) {
		// TODO Auto-generated method stub
		return null;
	}



	// 批量抄表记录上传
	// data以JSON格式上传，[{userid:'用户编号', showNumber:本期抄表数},{}]
	@Path("record/batch/{handdate}/{sgnetwork}/{sgoperator}/{lastinputdate}")
	@POST
	public String RecordInputForMore(String data,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate,
			@PathParam("handdate") String handdate)  {
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