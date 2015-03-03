package com.aote.rs.charge;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.collection.PersistentSet;
import org.hibernate.proxy.map.MapProxy;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import com.aote.helper.StringHelper;
import com.aote.rs.util.RSException;

@Path("charge")
@Component
/**
 * 卡表收费业务阶梯气价运算
 */
public class CardCharge {

	static Logger log = Logger.getLogger(CardCharge.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;

	private String stairtype;
	private BigDecimal gasprice;
	private BigDecimal stair1amount;
	private BigDecimal stair2amount;
	private BigDecimal stair3amount;
	private BigDecimal stair1price;
	private BigDecimal stair2price;
	private BigDecimal stair3price;
	private BigDecimal stair4price;
	private int stairmonths;
	private BigDecimal zhye;

	private String stardate;
	private String enddate;

	private BigDecimal sumamont;

	// 根据前台录入购气量计算各阶梯气量金额
	@GET
	@Path("/num/{userid}/{pregas}")
	public JSONObject txpregas(@PathParam("userid") String userid,
			@PathParam("pregas") double pregas) {
		log.debug("计算各阶梯气量金额 开始：userid：" + userid + "|pregas:" + pregas);
		JSONObject obj = new JSONObject();
		try {
			BigDecimal chargenum = new BigDecimal(0);
			BigDecimal stair1num = new BigDecimal(0);
			BigDecimal stair2num = new BigDecimal(0);
			BigDecimal stair3num = new BigDecimal(0);
			BigDecimal stair4num = new BigDecimal(0);
			BigDecimal stair1fee = new BigDecimal(0);
			BigDecimal stair2fee = new BigDecimal(0);
			BigDecimal stair3fee = new BigDecimal(0);
			BigDecimal stair4fee = new BigDecimal(0);
			BigDecimal pregas_bd = new BigDecimal(pregas);
			txSearchStair(userid);
			// 针对设置阶梯气价的用户运算
			if (!stairtype.equals("未设")) {
				// 累计购气量
				BigDecimal allamont = sumamont.add(pregas_bd);
				// 当前购气量在第一阶梯
				if (sumamont.compareTo(stair1amount) < 0) {
					if (allamont.compareTo(stair1amount) < 0) {
						stair1num = pregas_bd;
						stair1fee = pregas_bd.multiply(stair1price);
						chargenum = pregas_bd.multiply(stair1price);
					} else if (allamont.compareTo(stair1amount) >= 0
							&& allamont.compareTo(stair2amount) < 0) {
						stair1num = stair1amount.subtract(sumamont);
						stair1fee = (stair1amount.subtract(sumamont))
								.multiply(stair1price);
						stair2num = allamont.subtract(stair1amount);
						stair2fee = (allamont.subtract(stair1amount))
								.multiply(stair2price);
						chargenum = stair1fee.add(stair2fee);
					} else if (allamont.compareTo(stair2amount) >= 0
							&& allamont.compareTo(stair3amount) < 0) {
						stair1num = stair1amount.subtract(sumamont);
						stair1fee = (stair1amount.subtract(sumamont))
								.multiply(stair1price);
						stair2num = stair2amount.subtract(stair1amount);
						stair2fee = (stair2amount.subtract(stair1amount))
								.multiply(stair2price);
						stair3num = allamont.subtract(stair2amount);
						stair3fee = (allamont.subtract(stair2amount))
								.multiply(stair3price);
						chargenum = stair1fee.add(stair2fee).add(stair3fee);
					} else if (allamont.compareTo(stair3amount) >= 0) {
						stair1num = stair1amount.subtract(sumamont);
						stair1fee = (stair1amount.subtract(sumamont))
								.multiply(stair1price);
						stair2num = stair2amount.subtract(stair1amount);
						stair2fee = (stair2amount.subtract(stair1amount))
								.multiply(stair2price);
						stair3num = stair3amount.subtract(stair2amount);
						stair3fee = (stair3amount.subtract(stair2amount))
								.multiply(stair3price);
						stair4num = allamont.subtract(stair3amount);
						stair4fee = (allamont.subtract(stair3amount))
								.multiply(stair4price);
						chargenum = stair1fee.add(stair2fee).add(stair3fee)
								.add(stair4fee);
					}
					// 当前已购气量在阶梯二内
				} else if (sumamont.compareTo(stair1amount) >= 0
						&& sumamont.compareTo(stair2amount) < 0) {
					if (allamont.compareTo(stair2amount) < 0) {
						stair2num = pregas_bd;
						stair2fee = pregas_bd.multiply(stair2price);
						chargenum = stair2fee;
					} else if (allamont.compareTo(stair2amount) >= 0
							&& allamont.compareTo(stair3amount) < 0) {
						stair2num = stair2amount.subtract(sumamont);
						stair2fee = (stair2amount.subtract(sumamont))
								.multiply(stair2price);
						stair3num = allamont.subtract(stair2amount);
						stair3fee = (allamont.subtract(stair2amount))
								.multiply(stair3price);
						chargenum = stair2fee.add(stair3fee);
					} else {
						stair2num = stair2amount.subtract(sumamont);
						stair2fee = (stair2amount.subtract(sumamont))
								.multiply(stair2price);
						stair3num = stair3amount.subtract(stair2amount);
						stair3fee = (stair3amount.subtract(stair2amount))
								.multiply(stair3price);
						stair4num = allamont.subtract(stair3amount);
						stair4fee = (allamont.subtract(stair3amount))
								.multiply(stair4price);
						chargenum = stair2fee.add(stair3fee).add(stair4fee);
					}
					// 当前已购气量在阶梯三内
				} else if (sumamont.compareTo(stair2amount) >= 0
						&& sumamont.compareTo(stair3amount) < 0) {
					if (allamont.compareTo(stair3amount) < 0) {
						stair3num = pregas_bd;
						stair3fee = pregas_bd.multiply(stair3price);
						chargenum = stair3fee;
					} else {
						stair3num = stair3amount.subtract(sumamont);
						stair3fee = (stair3amount.subtract(sumamont))
								.multiply(stair3price);
						stair4num = allamont.subtract(stair3amount);
						stair4fee = (allamont.subtract(stair3amount))
								.multiply(stair4price);
						chargenum = stair3fee.add(stair4fee);
					}
					// 当前已购气量超过阶梯三
				} else if (sumamont.compareTo(stair3amount) >= 0) {
					stair4num = pregas_bd;
					stair4fee = pregas_bd.multiply(stair4price);
					chargenum = stair4fee;
				}

				// 该用户未设置阶梯气价
			} else {
				chargenum = pregas_bd.multiply(gasprice);
				stair1num = new BigDecimal(0);
				stair2num = new BigDecimal(0);
				stair3num = new BigDecimal(0);
				stair4num = new BigDecimal(0);
				stair1fee = new BigDecimal(0);
				stair2fee = new BigDecimal(0);
				stair3fee = new BigDecimal(0);
				stair4fee = new BigDecimal(0);
			}
			Map sell = new HashMap();
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
			sell.put("f_allamont", sumamont);
			sell.put("f_chargenum", chargenum);
			sell.put("f_stardate", stardate);
			sell.put("f_enddate", enddate);
			sell.put("f_totalcost", chargenum.subtract(zhye));
			obj = MapToJson(sell);
			log.debug("计算各阶梯气量金额 结束:" + sell.toString());
			// 抓取自定义异常
		} catch (RSException e) {
			log.debug("查找出入金额失败!");
			obj.put("error", e.getMessage());
		} catch (Exception e) {
			log.debug("计算各阶梯气量金额失败" + e.getMessage());
			obj.put("error", e.getMessage());
		} finally {
			return obj;
		}
	}

	// 根据前台录入收款计算购气量
	@GET
	@Path("/fee/{userid}/{prefee}")
	public JSONObject txprefee(@PathParam("userid") String userid,
			@PathParam("prefee") double prefee) {
		log.debug("查找出入金额 ：用户编号：" + userid + ",收款：" + prefee);
		JSONObject obj = new JSONObject();
		try {
			BigDecimal chargeamont = new BigDecimal(0);
			BigDecimal stair1num = new BigDecimal(0);
			BigDecimal stair2num = new BigDecimal(0);
			BigDecimal stair3num = new BigDecimal(0);
			BigDecimal stair4num = new BigDecimal(0);
			BigDecimal stair1fee = new BigDecimal(0);
			BigDecimal stair2fee = new BigDecimal(0);
			BigDecimal stair3fee = new BigDecimal(0);
			BigDecimal stair4fee = new BigDecimal(0);
			BigDecimal prefee_bd = new BigDecimal(prefee);
			// 查询用户阶梯气价信息
			txSearchStair(userid);
			prefee_bd = prefee_bd.add(zhye);
			// 针对设置阶梯气价的用户运算
			if (!stairtype.equals("未设")) {
				// 当前购气量在第一阶梯
				if (sumamont.compareTo(stair1amount) < 0) {
					// 阶段一剩下气量的金额大于本次购气金额 直接按阶梯一的价格算出气量
					if ((stair1amount.subtract(sumamont)).multiply(stair1price)
							.compareTo(prefee_bd) > 0) {
						stair1num = prefee_bd.divide(stair1price);
						stair1fee = prefee_bd;
						chargeamont = stair1num;
						// 当前购气金额所对应的气量超过阶梯一
					} else {
						// 先计算出阶段一的气量和金额
						stair1num = stair1amount.subtract(sumamont);
						stair1fee = stair1num.multiply(stair1price);
						// 当前购气金额对应的气量未超过阶梯二时 算出气量和金额
						if ((prefee_bd.subtract(stair1fee)).divide(stair2price)
								.compareTo(stair2amount.subtract(stair1amount)) < 0) {
							stair2num = (prefee_bd.subtract(stair1fee))
									.divide(stair2price);
							stair2fee = prefee_bd.subtract(stair1fee);
							chargeamont = stair1num.add(stair2num);
							// 当前购气金额对应的气量超出阶梯二
						} else {
							// 计算阶梯二的气量和金额
							if ((prefee_bd.subtract(stair2fee)).divide(
									stair2price).compareTo(
									stair3amount.subtract(stair2amount)) < 0) {
								stair2num = stair2amount.subtract(stair1amount);
								stair2fee = stair2num.multiply(stair2price);
								stair3num = (prefee_bd.subtract(stair2fee)
										.subtract(stair1fee))
										.divide(stair3price);
								stair3fee = prefee_bd.subtract(stair2fee)
										.subtract(stair1fee);
								chargeamont = stair1num.add(stair2num).add(
										stair3num);
							} else {
								stair2num = stair2amount.subtract(stair1amount);
								stair2fee = stair2num.multiply(stair2price);
								stair3num = stair3amount.subtract(stair2amount);
								stair3fee = stair3num.multiply(stair3price);
								stair4num = (prefee_bd.subtract(stair3fee)
										.subtract(stair2fee)
										.subtract(stair1fee))
										.divide(stair4price);
								stair4fee = prefee_bd.subtract(stair3fee)
										.subtract(stair2fee)
										.subtract(stair1fee);
								chargeamont = stair1num.add(stair2num)
										.add(stair3num).add(stair4num);
							}
						}
					}
					// 当前已购气量在阶梯二内
				} else if (sumamont.compareTo(stair1amount) >= 0
						&& sumamont.compareTo(stair2amount) < 0) {
					// 阶段二剩下气量的金额大于本次购气金额 直接按阶梯二的价格算出气量
					if ((stair2amount.subtract(sumamont)).multiply(stair2price)
							.compareTo(prefee_bd) > 0) {
						stair2num = prefee_bd.divide(stair2price);
						stair2fee = prefee_bd;
						chargeamont = stair2num;
						// 当前购气金额所对应的气量超过阶梯二
					} else {
						// 先计算出阶段二的气量和金额
						stair2num = stair2amount.subtract(sumamont);
						stair2fee = stair2num.multiply(stair2price);
						// 当前购气金额对应的气量未超过阶梯三时 算出气量和金额
						if ((prefee_bd.subtract(stair2fee)).divide(stair3price)
								.compareTo(stair3amount.subtract(stair2amount)) < 0) {
							stair3num = (prefee_bd.subtract(stair2fee))
									.divide(stair3price);
							stair3fee = prefee_bd.subtract(stair2fee);
							chargeamont = stair2num.add(stair3num);
						} else {
							stair3num = stair3amount.subtract(stair2amount);
							stair3fee = stair3num.multiply(stair3price);
							stair4num = (prefee_bd.subtract(stair3fee)
									.subtract(stair2fee)).divide(stair4price);
							stair4fee = prefee_bd.subtract(stair3fee).subtract(
									stair2fee);
							chargeamont = stair2num.add(stair3num).add(
									stair4num);
						}
					}
					// 当前已购气量在阶梯三内
				} else if (sumamont.compareTo(stair2amount) >= 0
						&& sumamont.compareTo(stair3amount) < 0) {
					// 阶段三剩下气量的金额大于本次购气金额 直接按阶段三的价格算出气量
					if ((stair3amount.subtract(sumamont)).multiply(stair3price)
							.compareTo(prefee_bd) > 0) {
						stair3num = prefee_bd.divide(stair3price);
						stair3fee = prefee_bd;
						chargeamont = stair3num;
						// 当前购气金额所对应的气量超过阶梯三
					} else {
						// 先计算出阶段三的气量和金额
						stair3num = stair3amount.subtract(sumamont);
						stair3fee = stair3num.multiply(stair3price);
						stair4num = (prefee_bd.subtract(stair3fee))
								.divide(stair4price);
						stair4fee = prefee_bd.subtract(stair3fee);
						chargeamont = stair3num.add(stair4num);

					}
					// 当前已购气量超过阶梯三
				} else if (sumamont.compareTo(stair3amount) >= 0) {
					stair4num = prefee_bd.divide(stair4price);
					stair4fee = prefee_bd;
					chargeamont = stair4num;
				}
				// 该用户未设置阶梯气价
			} else {
				log.debug("该用户未设置阶梯气价");
				;
				chargeamont = prefee_bd.divide(gasprice);
				stair1num = new BigDecimal(0);
				stair2num = new BigDecimal(0);
				stair3num = new BigDecimal(0);
				stair4num = new BigDecimal(0);
				stair1fee = new BigDecimal(0);
				stair2fee = new BigDecimal(0);
				stair3fee = new BigDecimal(0);
				stair4fee = new BigDecimal(0);
			}
			Map sell = new HashMap();
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
			sell.put("f_allamont", sumamont);
			sell.put("chargeamont", chargeamont);
			sell.put("f_stardate", stardate);
			sell.put("f_enddate", enddate);
			sell.put("f_preamount", prefee);
			obj = MapToJson(sell);
			log.debug("查找出入金额信息:" + sell.toString());
			// 抓取自定义异常
		} catch (RSException e) {
			log.debug("查找出入金额失败!");
			obj.put("error", e.getMessage());
		} catch (Exception e) {
			log.debug("查找出入金额失败" + e.getMessage());
			obj.put("error", e.getMessage());
		} finally {
			return obj;
		}
	}

	// 查询用户阶梯气价信息
	public void txSearchStair(String userid) throws Exception {
		log.debug("查找用户阶梯气价信息 开始，用户编号：" + userid);
		try {
			final String usersql = "select isnull(f_stairtype,'未设')f_stairtype, isnull(f_gasprice,0)f_gasprice, "
					+ "isnull(f_stair1amount,0)f_stair1amount,isnull(f_stair2amount,0)f_stair2amount,"
					+ "isnull(f_stair3amount,0)f_stair3amount,isnull(f_stair1price,0)f_stair1price,"
					+ "isnull(f_stair2price,0)f_stair2price,isnull(f_stair3price,0)f_stair3price,"
					+ "isnull(f_stair4price,0)f_stair4price,isnull(f_stairmonths,0)f_stairmonths,isnull(f_zhye,0)f_zhye "
					+ "from t_userfiles where f_userid = '" + userid + "'";
			List<Map<String, Object>> userlist = (List<Map<String, Object>>) hibernateTemplate
					.execute(new HibernateCallback() {
						public Object doInHibernate(Session session)
								throws HibernateException {
							Query q = session.createSQLQuery(usersql);
							q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
							List result = q.list();
							return result;
						}
					});
			if (userlist.size() != 1) {
				log.debug("查询用户阶梯气价信息, 查询到多条数据，抛出异常");
				// 查询到多条数据，抛出异常
				throw new RSException("查询到多条阶梯气价设置信息");
			}
			Map<String, Object> usermap = (Map<String, Object>) userlist.get(0);
			stairtype = usermap.get("f_stairtype").toString();
			gasprice = new BigDecimal(usermap.get("f_gasprice").toString());
			stair1amount = new BigDecimal(usermap.get("f_stair1amount")
					.toString());
			stair2amount = new BigDecimal(usermap.get("f_stair2amount")
					.toString());
			stair3amount = new BigDecimal(usermap.get("f_stair3amount")
					.toString());
			stair1price = new BigDecimal(usermap.get("f_stair1price")
					.toString());
			stair2price = new BigDecimal(usermap.get("f_stair2price")
					.toString());
			stair3price = new BigDecimal(usermap.get("f_stair3price")
					.toString());
			stair4price = new BigDecimal(usermap.get("f_stair4price")
					.toString());
			stairmonths = Integer.parseInt(usermap.get("f_stairmonths")
					.toString());
			zhye = new BigDecimal(usermap.get("f_zhye").toString());

			CountDate();
			// 查出该用户阶梯气价信息
			final String sellsql = "select isnull(sum(f_pregas),0)f_pregas from "
					+ "t_sellinggas where f_userid='"
					+ userid
					+ "' and f_deliverydate>='"
					+ stardate
					+ "' and f_deliverydate<='" + enddate + "'";
			List<Map<String, Object>> selllist = (List<Map<String, Object>>) hibernateTemplate
					.execute(new HibernateCallback() {
						public Object doInHibernate(Session session)
								throws HibernateException {
							Query q = session.createSQLQuery(sellsql);
							q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
							List result = q.list();
							return result;
						}
					});
			if (selllist.size() != 1) {
				log.debug("查询用户阶梯气价信息, 查询用户收费信息查到多条数据，抛出异常");
				// 查询到多条数据，抛出异常
				throw new RSException("查询到多条用户收费信息");
			}
			Map<String, Object> sellmap = (Map<String, Object>) selllist.get(0);
			sumamont = new BigDecimal(sellmap.get("f_pregas").toString());
			log.debug("查询用户阶梯信息 结束");
		} catch (Exception e) {
			log.debug("查询用户阶梯信息  失败" + e.getMessage());
			throw e;
		}

	}

	// 计算开始结束时间方法
	private void CountDate() {
		// 计算当前月在哪个阶梯区间
		Calendar cal = Calendar.getInstance();
		int thismonth = cal.get(Calendar.MONTH) + 1;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		if (stairmonths == 1) {
			cal.set(Calendar.DAY_OF_MONTH, 1);
			stardate = format.format(cal.getTime());
			cal.set(Calendar.DAY_OF_MONTH,
					cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			enddate = format.format(cal.getTime());
		} else {
			/*
			 * 阶梯起始月数计算起始月 = 当前月/阶梯月数*阶梯月数+1结束月 = 当前月/阶梯月数*阶梯月数+阶梯月数注：该运算
			 * 当前月是12月时则需要剪1 上面已经算出阶梯月数为1个月时的金额一下运算阶梯月数至少为两个月 所以对算区间没有影响
			 */
			if (thismonth == 12) {
				thismonth = 11;
			}
			// 计算起始月
			int star = Math.round(thismonth / stairmonths) * stairmonths + 1;
			// 计算结束月
			int end = Math.round(thismonth / stairmonths) * stairmonths
					+ stairmonths;
			// 获得起始日期和结束日期
			cal.set(Calendar.MONTH, star - 1);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			stardate = format.format(cal.getTime());
			cal.set(Calendar.MONTH, end - 1);
			cal.set(Calendar.DAY_OF_MONTH,
					cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			enddate = format.format(cal.getTime());
		}
		log.debug("阶梯气价 开始日期：" + stardate + ",结束日期：" + enddate);
	}

	// 把单个map转换成JSON对象
	private JSONObject MapToJson(Map<String, Object> map) {
		JSONObject json = new JSONObject();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			try {
				String key = entry.getKey();
				Object value = entry.getValue();
				// 空值转换成JSON的空对象
				if (value == null) {
					value = JSONObject.NULL;
				} else if (value instanceof PersistentSet) {
					PersistentSet set = (PersistentSet) value;
					value = ToJson(set);
				}
				// 如果是$type$，表示实体类型，转换成EntityType
				if (key.equals("$type$")) {
					json.put("EntityType", value);
				} else if (value instanceof Date) {
					Date d1 = (Date) value;
					Calendar c = Calendar.getInstance();
					long time = d1.getTime() + c.get(Calendar.ZONE_OFFSET);
					json.put(key, time);
				} else if (value instanceof HashMap) {
					JSONObject json1 = MapToJson((Map<String, Object>) value);
					json.put(key, json1);
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
	private Object ToJson(PersistentSet set) {
		// 没加载的集合当做空
		if (!set.wasInitialized()) {
			return JSONObject.NULL;
		}
		JSONArray array = new JSONArray();
		for (Object obj : set) {
			Map<String, Object> map = (Map<String, Object>) obj;
			JSONObject json = MapToJson(map);
			array.put(json);
		}
		return array;
	}
}
