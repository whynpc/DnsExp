package edu.ucla.cs.wing.dnsexp;

import edu.ucla.cs.wing.dnsexp.EventLog.LogType;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		EventLog.write(LogType.DEBUG, "Recv Alarm: "
				+ intent.getExtras().getString(MeasureTask.TASK));

		BackgroundService.getController().onAlarm(
				intent.getExtras().getString(MeasureTask.TASK));

	}

}
