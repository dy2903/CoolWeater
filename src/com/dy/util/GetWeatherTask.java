package com.dy.util;

import java.net.URLEncoder;

import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;

import com.dy.app.Application;
import com.dy.bean.City;
import com.dy.bean.WeatherInfo;

/**
 * 访问服务器,获得相应的天气信息
 * Params : void , Progress void , Result : Integer
 * @author DX2903
 *
 */
public class GetWeatherTask extends AsyncTask<Void, Void, Integer> {
	
	private static final String BASE_URL = "http://sixweather.3gpk.net/SixWeather.aspx?city=%s";
	private static final int SUCCESS = 0;
	private static final int SUCCESS_YUJING = 1;
	private static final int FAIL = -1;
	private Handler mHandler;
	private City mCity;
	private Application mApplication;
	/**
	 * 构造函数中需要传递city和handler
	 * handler是从外部传递过来的.
	 * @param handler
	 * @param city
	 */
	public GetWeatherTask(Handler handler, City city) {
		this.mHandler = handler;
		this.mCity = city;
		mApplication = Application.getInstance();
	};

	/**
	 * 后台运行,开启线程
	 * 首先在Cache中读取,如果有信息,使用Xml进行解析
	 * 如果没有,再执行网络请求,同时设置预警标志
	 */
	@Override
	protected Integer doInBackground(Void... params) {
		try {			
//			替换掉网络URL里面的参数值
//			String类的format()方法用于创建格式化的字符串以及连接多个字符串对象
//			将%s替换为city的名称
			String url = String.format(BASE_URL,
					URLEncoder.encode(mCity.getName(), "utf-8"));
			// 为了避免频繁刷新浪费流量，所以先读取内存中的信息
//			if (mApplication.getAllWeather() != null
//					&& mApplication.getAllWeather().getCity()
//							.equals(mCity.getName())) {
//				L.i("lwp", "get the weather info from memory");
//				return SCUESS;// 直接返回，不继续执行
//			}
			// 读取文件中的缓存信息
			String fileResult = ConfigCache.getUrlCache(mCity.getPinyin());// 读取文件中的缓存
			if (!TextUtils.isEmpty(fileResult)) {
//				XML解析
				WeatherInfo allWeather = XmlPullParseUtil
						.parseWeatherInfo(fileResult);
				if (allWeather != null) {
					mApplication.SetAllWeather(allWeather);
					L.i("lwp", "get the weather info from file");
					return SUCCESS;
				}
			}
			// 最后才执行网络请求
			String netResult = HttpUtil.connServerForResult(url);
			
			if (!TextUtils.isEmpty(netResult)) {
				WeatherInfo allWeather = XmlPullParseUtil
						.parseWeatherInfo(netResult);
				
				if (allWeather != null) {
					mApplication.SetAllWeather(allWeather);
//					存储返回的天气信息,同时设置预警标志
					ConfigCache.setUrlCache(netResult, mCity.getPinyin());
					L.i("lwp", "get the weather info from network");
					String yujin = allWeather.getYujing();
					if (!TextUtils.isEmpty(yujin) && !yujin.contains("暂无预警"))
						return SUCCESS_YUJING;
					return SUCCESS;
				}
				
			};
		} catch (Exception e) {
			e.printStackTrace();
		}
		return FAIL;
	};
	
	
	/**
	 * 后台任务执行完毕同时return了数据以后调用
	 */
	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
//		表示返回的是FAIL
		if(result < 0 ){
//			获取天气失败,通过handler把信息传递到MainActivity
			mHandler.sendEmptyMessage(MainActivity.GET_WEATHER_FAIL);// 获取天气信息失败
			L.i("lwp", "get weather fail");
		}else{
			mHandler.sendEmptyMessage(MainActivity.GET_WEATHER_SCUESS);// 获取天气信息成功，通知主线程更新
			L.i("lwp", "get weather scuess");
			L.i("lwp", mApplication.getAllWeather().toString());
//			预警
			if(result == SUCCESS_YUJING){
				mApplication.showNotification();
			}			
		}
	};
}
