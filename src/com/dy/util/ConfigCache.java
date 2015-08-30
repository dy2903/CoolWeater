package com.dy.util;

import java.io.File;
import java.io.IOException;

import android.text.TextUtils;
import android.util.Log;

import com.dy.app.Application;
/**
 * Cache管理类
 * @author DX2903
 *
 */
public class ConfigCache {

	/**
	 * 过期时间
	 */
	public static final int CONFIG_CACHE_MOBILE_TIMEOUT = 2 * 60 * 60 * 1000; // 2 hour mobile net
	public static final int CONFIG_CACHE_WIFI_TIMEOUT = 30 * 60 * 1000; // 30 minute wifi 

	/**
	 * 获取Cache
	 * 如果有网络同时刚刚才修改了Cache,不重复执行,避免多次刷新.
	 * 如果过期了也返回null
	 * @param url
	 * @return
	 */
	public static String getUrlCache(String url) {
		if (TextUtils.isEmpty(url)) {
			return null;
		}
//		获得Cache的绝对路径,将file的后缀用+替换掉.
		File file = new File(Application.getInstance().getCacheDir()
				+ File.separator + ConfigCache.replaceUrlWithPlus(url));
		if (file.exists() && file.isFile()) {
			long expiredTime = System.currentTimeMillis() - file.lastModified();
			Log.i("lwp", url + ": expiredTime="+expiredTime/1000);

//			当有网络同时Cache刚刚才修改了的时候,不使用Cache
//			避免频繁读取
			if (Application.mNetWorkState != NetUtil.NETWORN_NONE
					&& expiredTime < 0) {
				return null;
			}
			//如果是wifi网络，则30分钟过期
			if (Application.mNetWorkState == NetUtil.NETWORN_WIFI
					&& expiredTime > CONFIG_CACHE_WIFI_TIMEOUT) {
				return null;
				//如果是手机网络，则2个小时过期
			} else if (Application.mNetWorkState == NetUtil.NETWORN_MOBILE
					&& expiredTime > CONFIG_CACHE_MOBILE_TIMEOUT) {
				return null;
			}
			try {
//				读取Cache
				String result = FileUtils.readTextFile(file);
				return result;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	};
	/*
	 * 	 1. 处理特殊字符
		2. 去除后缀名带来的文件浏览器的视图凌乱(特别是图片更需要如此类似处理，否则有的手机打开图库，全是我们的缓存图片)
	 */
	public static String replaceUrlWithPlus(String url) {

		if (url != null) {
//			去除开头的http:
//			将所有的,:/等符号替换为+
//			多个连续的+替换为+
			return url.replaceAll("http://(.)*?/", "")
					.replaceAll("[.:/,%?&=]", "+").replaceAll("[+]+", "+");
		}
		return null;
	};

	/**
	 * 设置Cache
	 * @param data
	 * @param url
	 */
	public static void setUrlCache(String data, String url) {
		if (Application.getInstance().getCacheDir() == null) {
			return;
		}
		// File dir = new File(BaseApplication.mSdcardDataDir);
		// if (!dir.exists() &&
		// Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
		// {
		// dir.mkdirs();
		// }
//		在存Cache的时候就需要把http去除掉
		File file = new File(Application.getInstance().getCacheDir()
				+ File.separator + ConfigCache.replaceUrlWithPlus(url));
		try {
			// 创建缓存数据到磁盘，就是创建文件
			FileUtils.writeTextFile(file, data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	};


	/**
	 * delete cache file recursively
	 * 
	 * @param cacheFile
	 *            if null means clear cache function, or clear cache file
	 */
	public static void clearCache(File cacheFile) {
		if (cacheFile == null) {
			try {
				File cacheDir = Application.getInstance().getCacheDir();
				if (cacheDir.exists()) {
//					递归删除
					ConfigCache.clearCache(cacheDir);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (cacheFile.isFile()) {
			cacheFile.delete();
//			如果是目录的话,列出目录,对每个目录进行递归
		} else if (cacheFile.isDirectory()) {
			File[] childFiles = cacheFile.listFiles();
			for (int i = 0; i < childFiles.length; i++) {
				clearCache(childFiles[i]);
			}
		}
	}


};
