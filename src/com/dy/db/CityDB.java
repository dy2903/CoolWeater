package com.dy.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.dy.bean.City;

/**
 * 数据库类,可以把常用的数据库操作封装起来
 * 获得所有的的City信息
 * 获得单个City信息
 * @author DX2903
 *
 */
public class CityDB {
	public static final String CITY_DB_NAME = "city.db";
	private static final String CITY_TABLE_NAME = "city";
	private SQLiteDatabase db;

	
	public CityDB(Context context, String path) {
//		abstract SQLiteDatabase	openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory)
//		Open a new private SQLiteDatabase associated with this Context's application package.
		db = context.openOrCreateDatabase(path, Context.MODE_PRIVATE, null);
	};

	/**
	 * 是否开着
	 * @return
	 */
	public boolean isOpen() {
		return db != null && db.isOpen();
	};
	/**
	 * 关闭数据库
	 */
	public void close() {
		if (db != null && db.isOpen())
			db.close();
	}
	/**
	 * 从数据库里面获得所有的城市的信息
	 * @return List
	 */
	public List<City> getAllCity() {
		List<City> list = new ArrayList<City>();
		Cursor c = db.rawQuery("SELECT * from " + CITY_TABLE_NAME, null);
//		信息包括:省市, 编号, 拼音,简拼 
//		取出城市的信息, 构造city实例,放到List中
		while (c.moveToNext()) {
			String province = c.getString(c.getColumnIndex("province"));
			String city = c.getString(c.getColumnIndex("name"));
			String number = c.getString(c.getColumnIndex("number"));
			String allPY = c.getString(c.getColumnIndex("pinyin"));
			String allFirstPY = c.getString(c.getColumnIndex("py"));
			City item = new City(province, city, number, allPY, allFirstPY);
			list.add(item);
		}
		return list;
	};

	public City getCity(String city) {
//		static boolean	isEmpty(CharSequence str)
//		Returns true if the string is null or 0-length.
		if (TextUtils.isEmpty(city))
			return null;
		City item = this.getCityInfo(city);//先全部搜索
		if (item == null) {
			item = this.getCityInfo(this.parseName(city));//处理一下之后再搜索
		}
		return item;
	}

	/**
	 * 比如把"重庆市"转换为"重庆"	
	 * @param city
	 * @return
	 */
	private String parseName(String city) {
		city = city.replaceAll("市$", "").replaceAll("县$", "")
				.replaceAll("区$", "");
		return city;
	}
	/**
	 * 在数据库中查询所有关于此City的信息
	 * @param String city
	 * @return City
	 */
	private City getCityInfo(String city) {
		City item = null;
		Cursor c = db.rawQuery("SELECT * from " + CITY_TABLE_NAME
				+ " where name=?", new String[] { city });
		if (c.moveToFirst()) {
			String province = c.getString(c.getColumnIndex("province"));
			String name = c.getString(c.getColumnIndex("name"));
			String number = c.getString(c.getColumnIndex("number"));
			String allPY = c.getString(c.getColumnIndex("pinyin"));
			String allFirstPY = c.getString(c.getColumnIndex("py"));
			item = new City(province, name, number, allPY, allFirstPY);
		}
		return item;
	}
}
