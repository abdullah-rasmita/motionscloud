package com.motionscloud.trycamera2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		
		//set intent filter
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,new IntentFilter("server_ready"));
        
        //start localserver
    	Intent intent = new Intent(this, WriterService.class);
    	startService(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() 
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			// Extract data included in the Intent
			int status = 0;
			if(intent.getAction().equals("server_ready"))
			{
				status = intent.getIntExtra("status", 0);
			}
			if(status == 1)
			{
				//create CameraActivity -- this part should be changed to accomodate other functions such as logging in
				//use shared preference to save the login info
				Intent camerastart = new Intent(context, CameraActivity.class);
				camerastart.putExtra("serverName",WriterService.ServerName);
				startActivity(camerastart);
			}
		}
	};
}
