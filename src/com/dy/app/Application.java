package com.dy.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dy.bean.City;
import com.dy.bean.WeatherInfo;
import com.dy.db.CityDB;
import com.dy.util.L;
import com.dy.util.NetUtil;
import com.dy.util.SharePreferenceUtil;
import com.dy.util.T;
import com.way.weather.R;

/**
 * 自定义的Application,
 * 可以存放全局的属性,变量,传递数据等
 * @author DX2903
 *
 */
public class Application extends android.app.Application {
	public static final int CITY_LIST_SUCESS = 100;
	/*
	 * 静态变量
	 */
//	只能是字母
	private static final String FORMAT = "^[a-z,A-Z].*$";
	private static Application mApplication;
	
	private CityDB mCityDB;
	//	图标
	private HashMap<String, Integer> iconIdMap;// 天气图标
	private HashMap<String, Integer> widgetIconIdMap;// 插件天气图标
	//存放City
	private List<City> mCityList;
	// 首字母集
	private List<String> initialList;
	// 根据首字母存放数据
	private Map<String, List<City>> initialCityMap;
	// 首字母位置集
	private List<Integer> initialPosList;
	// 首字母对应的位置
	private Map<String, Integer> initialPosMap;

	private LocationClient mLocationClient = null;
	private SharePreferenceUtil mSpUtil;
	
	private WeatherInfo allWeatherInfo;
	
	public static int mNetWorkState;
	
	private NotificationManager mNotificationManager;
	private static Context context;	
//	单例模式
	public static synchronized Application getInstance() {
		return mApplication;
	};
	
	/*
	 * 开启程序,
	 * 自动执行,
	 * 比其他的组件都要早	
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		mApplication = this;
		/**
		 * 启动的时候需要先把数据库复制到内存中.
		 * 2500多个城市，要想快速匹配，临时读取数据库是不可能达到快速的，
		 * 所以，我们在应用启动时，就可以把这2000多个城市从数据库中读入内存中
		 */
		// 这个必须最先复制完,所以我放在单线程中处理,待优化
		
		mCityDB = this.openCityDB();
		this.initData();
		Application.context = this.getApplicationContext();
	};
	
	/**
	 * 获得CityDB实例,
	 * @return
	 */
	private CityDB openCityDB() {
		String path = "/data"
				+ Environment.getDataDirectory().getAbsolutePath()
				+ File.separator + "com.way.weather" + File.separator
				+ CityDB.CITY_DB_NAME;
		
		File db = new File(path);
//		如果db不存在,或者从sharePreferences中读区的version信息小于0,复制一份dB
		if (!db.exists() || this.getSharePreferenceUtil().getVersion() < 0) {
			L.i("db is not exists");
			try {
			/*
			 * 复制一份
			 */				
//				Return an AssetManager instance for your application's package.
//				getAssets():返回一个Manager
				InputStream is = this.getAssets().open(CityDB.CITY_DB_NAME);
				FileOutputStream fos = new FileOutputStream(db);
				int len = -1;
				byte[] buffer = new byte[1024];
				while ((len = is.read(buffer)) != -1) {
					fos.write(buffer, 0, len);
					fos.flush();
				}
				fos.close();
				is.close();
				// 用于管理数据库版本，如果数据库有重大更新时使用
				getSharePreferenceUtil().setVersion(1);
				
			} catch (IOException e) {
				e.printStackTrace();
				T.showLong(mApplication, e.getMessage());
				System.exit(0);
			}
		}
		
		return new CityDB(this, path);
	};


	/*
	 * 初始化数据
	 * 
	 * 获得网络状态
	 * 开启一个线程把dB里面City信息提取出来,初始化initialList , initialCityMap , initialPosMap , initialPosList
	 * 获得locationClient
	 * 初始化图片ID和key的对应关系
	 * 初始化一个SharePreferenceUtil
	 * 初始化一个通知管理器
	 */
	public void initData() {
//		获得当前网络状态
		mNetWorkState = NetUtil.getNetworkState(this);
//		初始化CityList,通过一个线程
		this.initCityList();
//		获得locationClient
		mLocationClient = new LocationClient(this, this.getLocationClientOption());
//		初始化图片ID和key的对应关系
		this.initWeatherIconMap();
		this.initWidgetWeather();
//		新建一个SharePreferenceUtil
		mSpUtil = new SharePreferenceUtil(this,
				SharePreferenceUtil.CITY_SHAREPRE_FILE);
//		通知管理器
		mNotificationManager = (NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
	};
	
	
	/*
	 * 获得LocationClientOption,并配置.
	 */
	private LocationClientOption getLocationClientOption() {
		LocationClientOption option = new LocationClientOption();
//		加上各种参数
		option.setOpenGps(true);
		option.setAddrType("all");
		option.setServiceName(this.getPackageName());
		option.setScanSpan(0);// 获取 设置的扫描间隔，单位是毫秒
		option.disableCache(true);
		return option;
	};
	
	/*
//	 * 初始化CityList,通过一个线程
	 */
	private void initCityList() {
	
		new Thread(new Runnable() {

			@Override
			public void run() {
				Application.this.prepareCityList();
			}
		}).start();
	};
	
	

	/**
	 * 被initCityList的线程调用,
	 * 不断的从数据库里面获得城市信息.
	 * 从数据库里面获得City信息
	 * 初始化initialCityMap, initialPosList , initialPosmap
	 * @return
	 */
	private boolean prepareCityList() {
		
	
		mCityList = new ArrayList<City>();
		initialList = new ArrayList<String>();
		initialCityMap = new HashMap<String, List<City>>();
		initialPosList = new ArrayList<Integer>();
		initialPosMap = new HashMap<String, Integer>();
		
		// 获取数据库中所有城市
		mCityList = mCityDB.getAllCity();
		
//		取出每一个city对象,获得里面拼音属性,
		for (City city : mCityList) {
			// 第一个字拼音的第一个字母,统一为大写.
			String initial = city.getPy().substring(0, 1).toUpperCase();			
//			====================第一步:填充initialCityMap=====================
			/**
			 * 填充Map <String , List <City>>
			 * String:A , B , C...
			 * List<City>:加入City
			 * 当某个首字母第一次出现的时候,在initialCityMap中新建一个子项
			 * 其他的情况,取得首字母对应的项,加入City信息
			 */
//			首字母是字母
			if (initial.matches(FORMAT)) {
//				// 根据首字母存放数据
				if (initialList.contains(initial)) {
//					通过首字母存放city信息
					initialCityMap.get(initial).add(city);
				} else {
//					首字母集加上此不存在的首字母
					initialList.add(initial);
					List<City> list = new ArrayList<City>();
					list.add(city);
					initialCityMap.put(initial, list);
				}
//				如果首字母不是字母,统一放到"#"中
			} else {
				if (initialList.contains("#")) {
					initialCityMap.get("#").add(city);
				} else {
					initialList.add("#");
					List<City> list = new ArrayList<City>();
					list.add(city);
					initialCityMap.put("#", list);
				}
			}
		};
//		====================第二步:按照字母对首字母集进行重新排序=====================
		Collections.sort(initialList);
		
//		====================第三步:填充initialPosMap , initialPosList=====================
		int position = 0;
		for (int i = 0; i < initialList.size(); i++) {
			// 存入map中，key为首字母字符串，value为首字母在listview中位置
			initialPosMap.put(initialList.get(i), position);
			initialPosList.add(position);// 首字母在listview中位置，存入list中
			// 计算下一个首字母在listview的位置 (每一个首字母对应多个City信息)
//			initialCityMap.get(initialList.get(i))得到List<City>
			position += initialCityMap.get(initialList.get(i)).size();
		}
		return true;
	};
	
	
	/**
	 * 把图片ID和关键字联系起来
	 * @return
	 */
	private HashMap<String, Integer> initWeatherIconMap() {
		if (iconIdMap != null && !iconIdMap.isEmpty())
			return iconIdMap;
		iconIdMap = new HashMap<String, Integer>();
		iconIdMap.put("暴雪", R.drawable.biz_plugin_weather_baoxue);
		iconIdMap.put("暴雨", R.drawable.biz_plugin_weather_baoyu);
		iconIdMap.put("大暴雨", R.drawable.biz_plugin_weather_dabaoyu);
		iconIdMap.put("大雪", R.drawable.biz_plugin_weather_daxue);
		iconIdMap.put("大雨", R.drawable.biz_plugin_weather_dayu);

		iconIdMap.put("多云", R.drawable.biz_plugin_weather_duoyun);
		iconIdMap.put("雷阵雨", R.drawable.biz_plugin_weather_leizhenyu);
		iconIdMap.put("雷阵雨冰雹",
				R.drawable.biz_plugin_weather_leizhenyubingbao);
		iconIdMap.put("晴", R.drawable.biz_plugin_weather_qing);
		iconIdMap.put("沙尘暴", R.drawable.biz_plugin_weather_shachenbao);

		iconIdMap.put("特大暴雨", R.drawable.biz_plugin_weather_tedabaoyu);
		iconIdMap.put("雾", R.drawable.biz_plugin_weather_wu);
		iconIdMap.put("小雪", R.drawable.biz_plugin_weather_xiaoxue);
		iconIdMap.put("小雨", R.drawable.biz_plugin_weather_xiaoyu);
		iconIdMap.put("阴", R.drawable.biz_plugin_weather_yin);

		iconIdMap.put("雨夹雪", R.drawable.biz_plugin_weather_yujiaxue);
		iconIdMap.put("阵雪", R.drawable.biz_plugin_weather_zhenxue);
		iconIdMap.put("阵雨", R.drawable.biz_plugin_weather_zhenyu);
		iconIdMap.put("中雪", R.drawable.biz_plugin_weather_zhongxue);
		iconIdMap.put("中雨", R.drawable.biz_plugin_weather_zhongyu);
		return iconIdMap;
	};
	
	private HashMap<String, Integer> initWidgetWeather() {
		if (widgetIconIdMap != null && !widgetIconIdMap.isEmpty())
			return widgetIconIdMap;
		widgetIconIdMap = new HashMap<String, Integer>();
		widgetIconIdMap.put("暴雪", R.drawable.w17);
		widgetIconIdMap.put("暴雨", R.drawable.w10);
		widgetIconIdMap.put("大暴雨", R.drawable.w10);
		widgetIconIdMap.put("大雪", R.drawable.w16);
		widgetIconIdMap.put("大雨", R.drawable.w9);

		widgetIconIdMap.put("多云", R.drawable.w1);
		widgetIconIdMap.put("雷阵雨", R.drawable.w4);
		widgetIconIdMap.put("雷阵雨冰雹", R.drawable.w19);
		widgetIconIdMap.put("晴", R.drawable.w0);
		widgetIconIdMap.put("沙尘暴", R.drawable.w20);

		widgetIconIdMap.put("特大暴雨", R.drawable.w10);
		widgetIconIdMap.put("雾", R.drawable.w18);
		widgetIconIdMap.put("小雪", R.drawable.w14);
		widgetIconIdMap.put("小雨", R.drawable.w7);
		widgetIconIdMap.put("阴", R.drawable.w2);

		widgetIconIdMap.put("雨夹雪", R.drawable.w6);
		widgetIconIdMap.put("阵雪", R.drawable.w13);
		widgetIconIdMap.put("阵雨", R.drawable.w3);
		widgetIconIdMap.put("中雪", R.drawable.w15);
		widgetIconIdMap.put("中雨", R.drawable.w8);
		return widgetIconIdMap;
	};

	
	
	/*
	 * 显示预警通知
	 */
	public void showNotification() {
		
		int icon = R.drawable.logo;
		CharSequence tickerText = allWeatherInfo.getYujing();
		
		long when = System.currentTimeMillis();
		
		Notification mNotification = new Notification(icon, tickerText, when);

		mNotification.defaults |= Notification.DEFAULT_SOUND;
//		The view that will represent this notification in the expanded status bar
		mNotification.contentView = null;
//		点击跳转
		Intent intent = new Intent(this, MainActivity.class);
//		public static PendingIntent getActivity (Context context, int requestCode, Intent intent, int flags) 
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		//设置通知布局
//		setLatestEventInfo(Context, CharSequence, CharSequence, PendingIntent) f
		mNotification.setLatestEventInfo(mApplication, "CoolWeather's early warning", tickerText,
				contentIntent);
//		NotificationManager.notify(int id, Notification notification)		
		mNotificationManager.notify(0x001, mNotification);
	};
	
	
	
	/**
	 * getter , setter
	 * @return
	 */
	/*
	 * 获得SharePreferenceUtil
	 */
	public synchronized SharePreferenceUtil getSharePreferenceUtil() {
		if (mSpUtil == null)
			mSpUtil = new SharePreferenceUtil(this,
					SharePreferenceUtil.CITY_SHAREPRE_FILE);
		return mSpUtil;
	}
	
	public synchronized LocationClient getLocationClient() {
		if (mLocationClient == null)
			mLocationClient = new LocationClient(this,
					getLocationClientOption());
		return mLocationClient;
	}
	

	public List<City> getCityList() {
		return mCityList;
	}

	public List<String> getSections() {
		return initialList;
	}

	public Map<String, List<City>> getMap() {
		return initialCityMap;
	}

	public List<Integer> getPositions() {
		return initialPosList;
	}

	public Map<String, Integer> getIndexer() {
		return initialPosMap;
	}


	public Map<String, Integer> getWeatherIconMap() {
		if (iconIdMap == null || iconIdMap.isEmpty())
			iconIdMap = initWeatherIconMap();
		return iconIdMap;
	}

	public NotificationManager getNotificationManager() {
		return mNotificationManager;
	}

	
	/**
	 * 获得dB连接
	 * @return
	 */
	public synchronized CityDB getCityDB() {
		if (mCityDB == null || !mCityDB.isOpen())
			mCityDB = this.openCityDB();
		return mCityDB;
	};

	/**
	 * 获得天气图片
	 * 对传入的天气描述(多云转晴)进行处理,提取关键字,
	 * 通过提取出来的关键字在Map中取出相应的图片ID
	 * @param climate
	 * @return int
	 */
	public int getWeatherIcon(String climate) {
		
		int weatherRes = R.drawable.biz_plugin_weather_qing;
//		默认Icon(默认是晴天)
		if (TextUtils.isEmpty(climate))
			return weatherRes;
//		默认值
		String[] strs = { "晴", "晴" };
//		对climate这个字符串进行处理
//		多云转晴
		if (climate.contains("转")) {// 天气带转字，取前面那部分
			strs = climate.split("转");
			climate = strs[0];
//			大到暴雨
			if (climate.contains("到")) {// 如果转字前面那部分带到字，则取它的后部分
				strs = climate.split("到");
				climate = strs[1];
			}
		};
		if (iconIdMap == null || iconIdMap.isEmpty())
			iconIdMap = this.initWeatherIconMap();
//		通过提取出来的关键字在Map中取出相应的图片ID
		if (iconIdMap.containsKey(climate)) {
			weatherRes = iconIdMap.get(climate);
		}
		return weatherRes;
	};

	public int getWidgetWeatherIcon(String climate) {
		int weatherRes = R.drawable.na;
		if (TextUtils.isEmpty(climate))
			return weatherRes;
		String[] strs = { "晴", "晴" };
		if (climate.contains("转")) {// 天气带转字，取前面那部分
			strs = climate.split("转");
			climate = strs[0];
			if (climate.contains("到")) {// 如果转字前面那部分带到字，则取它的后部分
				strs = climate.split("到");
				climate = strs[1];
			}
		}
		if (widgetIconIdMap == null || widgetIconIdMap.isEmpty())
			widgetIconIdMap = initWidgetWeather();
		if (widgetIconIdMap.containsKey(climate)) {
			weatherRes = widgetIconIdMap.get(climate);
		}
		return weatherRes;
	};

	public WeatherInfo getAllWeather() {
		return allWeatherInfo;
	};

	public void SetAllWeather(WeatherInfo allWeather) {
		allWeatherInfo = allWeather;
	}	

	
	/**
	 * 返回当前的Context对象
	 * @return
	 */
	public static Context getContext (){
		return Application.context;
	};
	
	
	
	
	
	/**
	 * Application结束的时候需要做的工作
	 */
	@Override
	public void onTerminate() {
		L.i("Application onTerminate...");
		super.onTerminate();
		if (mCityDB != null && mCityDB.isOpen())
			mCityDB.close();
	}

	/*
	 * 当程序在后台运行时，释放这部分最占内存的资源
	 */
	public void free() {
		mCityList = null;
		initialList = null;
		initialCityMap = null;
		initialPosList = null;
		initialPosMap = null;
		iconIdMap = null;
		allWeatherInfo = null;
		System.gc();
	};
	
	
};



