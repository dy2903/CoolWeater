package com.dy.util;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.dy.app.Application;

public class HttpUtil {
	/**
	 * 访问网络地址，获取返回结果
	 * static String connServerForResult(String url)
	 * 由于是在doInBackground中进行调用的,所以不需要开启线程
	 * @param url
	 * @return String
	 */
	public static String connServerForResult(String url) {
		if (NetUtil.getNetworkState(Application.getInstance()) == NetUtil.NETWORN_NONE)
			return null;
		HttpGet httpRequest = new HttpGet(url);
		String strResult = "";
		try {
			// HttpClient对象
			HttpClient httpClient = new DefaultHttpClient();
			// 获得HttpResponse对象
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
				// 取得返回的数据
				strResult = EntityUtils.toString(httpResponse.getEntity());

		} catch (Exception e) {
			e.printStackTrace();
			// 出错会返回空数据，注意处理
		}
		return strResult; // 返回结果
	};
};
