package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadPoolExecutor;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Record;

import edu.ucla.cs.wing.dnsexp.EventLog.Type;
import android.R.integer;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms.Addr;

public class BackgroundService extends Service implements ICommander,
		IExpHandler {

	private static ICommander commander;

	public static ICommander getCommander() {
		return commander;
	}

	private SharedPreferences prefs;

	private MobileInfo mobileInfo;

	private AlarmManager alarmManager;
	private PendingIntent alarmIntent;

	private Timer taskTimer, monitorTimer;

	private ExpConfig expConfig;

	@Override
	public void onCreate() {
		super.onCreate();

		commander = this;

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		MobileInfo.init(this);
		mobileInfo = MobileInfo.getInstance();

		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override
	public void onSendDnsQuery(long transacationId, String domainName) {
		EventLog.write(Type.DNSQUERY,
				new String[] { String.valueOf(transacationId), domainName });
	}

	@Override
	public void onRecvDnsResponse(long transacationId, String domainName,
			Record[] records) {
		List<String> data = new LinkedList<String>();
		data.add(String.valueOf(transacationId));
		data.add(domainName);
		for (Record record : records) {
			if (record instanceof ARecord) {
				ARecord aRecord = (ARecord) record;
				data.add(aRecord.getAddress().getHostAddress() + ","
						+ aRecord.getTTL());
			}
		}
		EventLog.write(Type.DNSREPONSE, data);
	}

	@Override
	public void startAutoTest() {
		Intent intent = new Intent(this, AlarmReceiver.class);
		alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,
				AlarmManager.INTERVAL_HOUR, alarmIntent);

	}

	@Override
	public void stopAutoTest() {
		if (alarmManager != null) {
			alarmManager.cancel(alarmIntent);
		}
	}

	@Override
	public void runOnceAutoTest() {

		ExpConfig expConfig = new ExpConfig();
		expConfig.setExpMode(Integer.parseInt(prefs.getString("exp_mode",
				getString(R.string.pref_default_exp_mode))));
		expConfig.setSelfUpdating(Integer.parseInt(prefs.getString(
				"config_selfupdating",
				getString(R.string.pref_default_config_selfupdating))) != 0);
		expConfig
				.setQueryRepeat(Integer.parseInt(prefs.getString(
						"query_repeat",
						getString(R.string.pref_default_query_repeat))));
		expConfig.setPingRepeat(Integer.parseInt(prefs.getString("ping_repeat",
				getString(R.string.pref_default_ping_repeat))));
		expConfig.setTcpRepeat(Integer.parseInt(prefs.getString("tcp_repeat",
				getString(R.string.pref_default_tcp_repeat))));

		if (!expConfig.load(prefs.getString("config_file",
				getString(R.string.pref_default_config_file)))) {
			EventLog.write(Type.DEBUG, "Fail to load exp config");
			return;
		}

		long delay = 0;
		taskTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				List<String> parameters = new LinkedList<String>();
				parameters.add(String.valueOf(System.currentTimeMillis()));
				parameters.add(mobileInfo.getOperatorName());
				parameters.add(mobileInfo.getNetworkTech());
				parameters.add(mobileInfo.getNetworkTypeStr());
				EventLog.openNewLogFile(EventLog.genLogFileName(parameters));

				startMonitorNetstat();
			}
		}, delay);
		delay++;

		if (expConfig.toQuery()) {
			for (String domainName : expConfig.getDomainNames()) {
				taskTimer
						.schedule(
								new DnsQueryTask(domainName, expConfig
										.getQueryRepeat(), false, this), delay);
				delay++;
			}

		}

		if (expConfig.toPing()) {
			// TODO
		}

		if (expConfig.toTcp()) {
			// TODO

		}

		taskTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				stopMonitorNetstat();
				EventLog.close();

			}
		}, delay);
	}

	@Override
	public void onAlarm() {
		runOnceAutoTest();
	}

	@Override
	public void startMonitorNetstat() {
		if (monitorTimer != null) {
			monitorTimer.cancel();
		}
		monitorTimer = new Timer();
		long interval = Long.parseLong(prefs.getString("monitor_interval",
				getString(R.string.pref_default_monitor_interval)));
		monitorTimer.schedule(new MonitorNetstatTask(), 0, interval);
	}

	@Override
	public void stopMonitorNetstat() {
		if (monitorTimer != null) {
			monitorTimer.cancel();
		}
		monitorTimer = null;
	}

	public class MonitorNetstatTask extends TimerTask {

		@Override
		public void run() {
			List<String> data = new LinkedList<String>();
			data.add(mobileInfo.getOperatorName());
			data.add(mobileInfo.getNetworkTech());
			data.add(mobileInfo.getNetworkTypeStr());
			data.add(mobileInfo.getLocalIpAddress());

			Process process;
			String s1 = null, s2 = null;
			try {
				process = Runtime.getRuntime().exec("getprop net.dns1");
				BufferedReader in = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
				String line = in.readLine();
				s1 = line;
				in.close();

				process = Runtime.getRuntime().exec("getprop net.dns2");
				in = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
				line = in.readLine();
				s2 = line;
				in.close();
			} catch (IOException e) {

				e.printStackTrace();
			}
			data.add(s1);
			data.add(s2);

			data.add((new DnsQueryTask(DnsQueryTask.DNS_EX_IP_NAME, 1, true,
					null)).resolve());

			EventLog.write(Type.MONITOR, data);

		}

	}

}
