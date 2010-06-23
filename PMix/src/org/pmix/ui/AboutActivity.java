package org.pmix.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.TextView;


public class AboutActivity extends Activity {



	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.about);

		TextView versionInfo = (TextView) findViewById(R.id.versionText);
		versionInfo.setText(getResources().getString(R.string.version) + ": " + getVersionName(this, Activity.class));

	}
	public static String getVersionName(Context context, Class cls) 
	{
	  try {
	    ComponentName comp = new ComponentName(context, cls);
	    PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
	    return pinfo.versionName;
	  } catch (android.content.pm.PackageManager.NameNotFoundException e) {
	    return null;
	  }
	}

	@Override
	protected void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication)getApplicationContext();
		app.setActivity(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication)getApplicationContext();
		app.unsetActivity(this);
	}
	
}
