package org.musicpd.android.tools;

public final class Log {
	public static String tag = "";

	public static void d(String msg)
	{
		android.util.Log.d(tag, msg); 
	}
	
	public static void v(String msg)
	{
		android.util.Log.v(tag, msg);
	}

	public static void i(String msg)
	{
		android.util.Log.d(tag, msg); 
	}
	
	public static void w(String msg)
	{
		android.util.Log.w(tag, msg);
	}
	
	public static void w(Throwable tr)
	{
		android.util.Log.w(tag, tr);
	}
	
	public static void e(String msg)
	{
		android.util.Log.e(tag, msg);
	}
	
	public static void e(String msg, Throwable tr)
	{
		android.util.Log.e(tag, msg, tr);
	}
	
	public static void e(Throwable tr)
	{
		e("Exception", tr);
	}
}
