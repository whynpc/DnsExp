package edu.ucla.cs.wing.dnsexp;

import edu.ucla.cs.wing.dnsexp.EventLog.Type;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		BackgroundService.getController().onAlarm(
				intent.getExtras().getString(MeasureTask.TASK));

	}

}
