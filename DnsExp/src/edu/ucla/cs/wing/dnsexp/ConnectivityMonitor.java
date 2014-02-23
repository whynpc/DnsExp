package edu.ucla.cs.wing.dnsexp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ConnectivityMonitor extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		Bundle bundle = intent.getExtras();
		
		for (String key : bundle.keySet()) {
			Log.d("test", key + ";" + bundle.get(key).toString());
		}

	}

}
