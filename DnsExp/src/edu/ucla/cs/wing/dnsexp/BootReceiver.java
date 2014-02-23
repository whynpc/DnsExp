package edu.ucla.cs.wing.dnsexp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent startServiceIntent = new Intent(context, BackgroundService.class);
		context.startService(startServiceIntent);

	}

}
