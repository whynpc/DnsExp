package edu.ucla.cs.wing.dnsexp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ConnectivityMonitor extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (MobileInfo.getInstance() != null) {
			MobileInfo.getInstance().updateIpAddress();
		}

	}

}
