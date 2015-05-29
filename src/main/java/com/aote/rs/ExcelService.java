﻿package com.aote.rs;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

@Path("excel")
@Component
public class ExcelService {
	static Logger log = Logger.getLogger(ExcelService.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;

	//导excel
	@POST
	@Path("/{count}/{cols}")
	public String exporttoexcel(String query, @PathParam("count") int count,
			@PathParam("cols") String cols) {
		log.debug("in to excel count=" + count + ", cols=" + cols);
		try {
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet sheet = workbook.createSheet();
			HSSFFont font = workbook.createFont();
			font.setColor((short) HSSFFont.COLOR_NORMAL);
			font.setBoldweight((short) HSSFFont.BOLDWEIGHT_BOLD);
			// 设置格式
			HSSFCellStyle cellStyle = workbook.createCellStyle();
			cellStyle.setAlignment((short) HSSFCellStyle.ALIGN_CENTER);
			cellStyle.setFont(font);
			HSSFRow row = null;
			HSSFCell cell = null;
			// 创建第0行 标题
			int rowNum = 0;
			row = sheet.createRow((short) rowNum);
			String[] colsStr = cols.split("\\|");
			for (int titleCol = 0; titleCol < colsStr.length; titleCol++) {
				cell = row.createCell((short) (titleCol));
				// 设置列类型
				cell.setCellType(HSSFCell.CELL_TYPE_STRING);
				// 设置列的字符集为中文
				cell.setEncoding((short) HSSFCell.ENCODING_UTF_16);
				// 设置内容
				String[] names = colsStr[titleCol].split(":");
				// 有别名,设置内容为别名，否则，为字段名。
				if (names.length > 1) {
					cell.setCellValue(names[1]);
				} else {
					cell.setCellValue(colsStr[titleCol]);
				}
				cell.setCellStyle(cellStyle);
			} 
			
			StatelessSession session = this.hibernateTemplate.getSessionFactory().openStatelessSession();
			ScrollableResults sr;
			if (query.startsWith("sql:")) {
				String sql = query.substring(4);
				sr = session.createSQLQuery(sql).scroll(ScrollMode.FORWARD_ONLY);
			} else {
				sr = session.createQuery(query).scroll(ScrollMode.FORWARD_ONLY);
			}
			
			while (sr.next()) {
				Map<String, Object> map = (Map<String, Object>)sr.get(0);
				rowNum++;
				row = sheet.createRow((short) rowNum);
				for (int z = 0; z < colsStr.length; z++) {
					// 得到数据
					String[] names = colsStr[z].split(":");
					// 有别名，字段名为第一项，否则，整个是字段名
					String colName = colsStr[z];
					if (names.length > 1) {
						colName = names[0];
					}
					String data = "";
					if (map.get(colName) != null) {
						data = map.get(colName).toString();
					}
					cell = row.createCell((short) (z));
					// 设置列类型
					cell.setCellType(HSSFCell.CELL_TYPE_STRING);
					// 设置列的字符集为中文
					cell.setEncoding((short) HSSFCell.ENCODING_UTF_16);
					cell.setCellValue(data);
				}
			}
			session.close();

			// 产生临时文件
			File file = File.createTempFile("temp", ".xls");
			file.deleteOnExit();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			// 写文件
			workbook.write(fileOutputStream);
			fileOutputStream.flush();
			fileOutputStream.close();
			// 返回文件名
			String result = file.getAbsolutePath();
			log.debug(result);
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	//导excel
	@POST
	@Path("/{count}")
	public String exporttoexcel2(String json, @PathParam("count") int count) {
		log.debug("in to excel count=" + count);
		try {
			String[] strs = json.split("~");
			String query = strs[0];
			String cols = strs[1];

			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet sheet = workbook.createSheet();
			HSSFFont font = workbook.createFont();
			font.setColor((short) HSSFFont.COLOR_NORMAL);
			font.setBoldweight((short) HSSFFont.BOLDWEIGHT_BOLD);
			// 设置格式
			HSSFCellStyle cellStyle = workbook.createCellStyle();
			cellStyle.setAlignment((short) HSSFCellStyle.ALIGN_CENTER);
			cellStyle.setFont(font);
			HSSFRow row = null;
			HSSFCell cell = null;
			// 创建第0行 标题
			int rowNum = 0;
			row = sheet.createRow((short) rowNum);
			String[] colsStr = cols.split("\\|");
			for (int titleCol = 0; titleCol < colsStr.length; titleCol++) {
				cell = row.createCell((short) (titleCol));
				// 设置列类型
				cell.setCellType(HSSFCell.CELL_TYPE_STRING);
				// 设置列的字符集为中文
				cell.setEncoding((short) HSSFCell.ENCODING_UTF_16);
				// 设置内容
				String[] names = colsStr[titleCol].split(":");
				// 有别名,设置内容为别名，否则，为字段名。
				if (names.length > 1) {
					cell.setCellValue(names[1]);
				} else {
					cell.setCellValue(colsStr[titleCol]);
				}
				cell.setCellStyle(cellStyle);
			}

			StatelessSession session = this.hibernateTemplate.getSessionFactory().openStatelessSession();
			ScrollableResults sr;
			if (query.startsWith("sql:")) {
				String sql = query.substring(4);
				sr = session.createSQLQuery(sql).scroll(ScrollMode.FORWARD_ONLY);
			} else {
				sr = session.createQuery(query).scroll(ScrollMode.FORWARD_ONLY);
			}
			
			while (sr.next()) {
				Map<String, Object> map = (Map<String, Object>)sr.get(0);
				rowNum++;
				row = sheet.createRow((short) rowNum);
				for (int z = 0; z < colsStr.length; z++) {
					// 得到数据
					String[] names = colsStr[z].split(":");
					// 有别名，字段名为第一项，否则，整个是字段名
					String colName = colsStr[z];
					if (names.length > 1) {
						colName = names[0];
					}
					String data = "";
					if (map.get(colName) != null) {
						data = map.get(colName).toString();
					}
					cell = row.createCell((short) (z));
					// 设置列类型
					cell.setCellType(HSSFCell.CELL_TYPE_STRING);
					// 设置列的字符集为中文
					cell.setEncoding((short) HSSFCell.ENCODING_UTF_16);
					cell.setCellValue(data);
				}
			}
			session.close();
			// 产生临时文件
			File file = File.createTempFile("temp", ".xls");
			file.deleteOnExit();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			// 写文件
			workbook.write(fileOutputStream);
			fileOutputStream.flush();
			fileOutputStream.close();
			// 返回文件名
			String result = file.getAbsolutePath();
			log.debug(result);
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// 执行分页查询
	class HibernateCall implements HibernateCallback {
		String hql;
		int page;
		int rows;

		public HibernateCall(String hql, int page, int rows) {
			this.hql = hql;
			this.page = page;
			this.rows = rows;
		}

		public Object doInHibernate(Session session) {
			Query q = session.createQuery(hql);
			List result = q.setFirstResult(page * rows).setMaxResults(rows)
					.list();
			return result;
		}
	}

	// 执行sql分页查询，结果集形式可以设置
	class HibernateSQLCall implements HibernateCallback {
		String sql;
		int page;
		int rows;
		// 查询结果转换器，可以转换成Map等。
		public ResultTransformer transformer = null;

		public HibernateSQLCall(String sql, int page, int rows) {
			this.sql = sql;
			this.page = page;
			this.rows = rows;
		}

		public Object doInHibernate(Session session) {
			Query q = session.createSQLQuery(sql);
			// 有转换器，设置转换器
			if (transformer != null) {
				q.setResultTransformer(transformer);
			}
			List result = q.setFirstResult(page * rows).setMaxResults(rows)
					.list();
			return result;
		}
	}
}
