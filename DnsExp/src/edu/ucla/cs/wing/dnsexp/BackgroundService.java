package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

public class BackgroundService extends Service implements IController {

	private static IController controller;

	public static IController getController() {
		return controller;
	}

	private SharedPreferences prefs;

	private MobileInfo mobileInfo;

	private AlarmManager alarmManager;
	private PendingIntent alarmIntentQuery, alarmIntentPing, alarmIntentTcp;

	private boolean autotest;

	private HashSet<ExpConfig> pendingExps;

	@Override
	public void onCreate() {
		super.onCreate();

		controller = this;

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		MobileInfo.init(this);
		mobileInfo = MobileInfo.getInstance();

		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		pendingExps = new HashSet<ExpConfig>();
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
	public void startAutoTest() {
		long queryPeriod = 1000 * Long.parseLong(prefs.getString(
				"autotest_query_period",
				getString(R.string.pref_default_autotest_query_period)));
		Intent intent1 = new Intent(this, AlarmReceiver.class);
		intent1.putExtra(MeasureTask.TASK, MeasureTask.TASK_QUERY);
		alarmIntentQuery = PendingIntent.getBroadcast(this, 1, intent1, 0);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,
				queryPeriod, alarmIntentQuery);

		long pingPeriod = 1000 * Long.parseLong(prefs.getString(
				"autotest_ping_period",
				getString(R.string.pref_default_autotest_ping_period)));
		Intent intent2 = new Intent(this, AlarmReceiver.class);
		intent2.putExtra(MeasureTask.TASK, MeasureTask.TASK_PING);
		alarmIntentPing = PendingIntent.getBroadcast(this, 2, intent2, 0);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,
				pingPeriod, alarmIntentPing);

		long tcpPeriod = 1000 * Long.parseLong(prefs.getString(
				"autotest_tcp_period",
				getString(R.string.pref_default_autotest_tcp_period)));
		Intent intent3 = new Intent(this, AlarmReceiver.class);
		intent3.putExtra(MeasureTask.TASK, MeasureTask.TASK_TCP);
		alarmIntentTcp = PendingIntent.getBroadcast(this, 3, intent3, 0);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,
				tcpPeriod, alarmIntentTcp);

		autotest = true;
	}

	@Override
	public void stopAutoTest() {
		if (alarmManager != null) {
			alarmManager.cancel(alarmIntentQuery);
			alarmManager.cancel(alarmIntentPing);
			alarmManager.cancel(alarmIntentTcp);
		}
		autotest = false;
	}

	private ExpTheadPoolExecutor createTheadPool(
			BlockingQueue<Runnable> workQueue, ExpConfig expConfig) {
		int corePoolSize = Integer.parseInt(prefs.getString(
				"threadpool_coresize",
				this.getString(R.string.pref_default_threadpool_coresize)));
		int maxPoolSize = Integer.parseInt(prefs.getString(
				"threadpool_maxSize",
				this.getString(R.string.pref_default_threadpool_maxsize)));
		return (new ExpTheadPoolExecutor(corePoolSize, maxPoolSize, workQueue,
				expConfig));

	}

	public class ExpTheadPoolExecutor extends ThreadPoolExecutor {

		private ExpConfig expConfig;

		private int finishedCnt = 0;

		public ExpTheadPoolExecutor(int corePoolSize, int maxPoolSize,
				BlockingQueue<Runnable> workQueue, ExpConfig expConfig) {
			super(corePoolSize, maxPoolSize, 1, TimeUnit.SECONDS, workQueue);
			this.expConfig = expConfig;
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			finishedCnt += 1;
			EventLog.write(
					Type.DEBUG,
					String.format("Progress of %s: %d / %d",
							expConfig.getTask(), finishedCnt,
							expConfig.getSize()));
			if (finishedCnt == expConfig.getSize()) {
				expConfig.cleanUp();
				pendingExps.remove(expConfig);
			}
		}
	}

	private void runQuery() {
		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_QUERY);
		if (!expConfig.init(prefs, this)) {
			return;
		}
		pendingExps.add(expConfig);

		ThreadPoolExecutor executor = createTheadPool(
				new LinkedBlockingDeque<Runnable>(), expConfig);

		for (String domainName : expConfig.getDomainNames()) {
			executor.execute(new DnsQueryTask(expConfig
					.getMeasureObject(domainName), expConfig));
		}
	}

	private void runPing() {
		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_PING);
		if (!expConfig.init(prefs, this)) {
			return;
		}
		pendingExps.add(expConfig);

		ThreadPoolExecutor executor = createTheadPool(
				new LinkedBlockingDeque<Runnable>(), expConfig);

		for (String domainName : expConfig.getDomainNames()) {
			executor.execute(new PingTask(expConfig
					.getMeasureObject(domainName), expConfig));
		}
	}

	private void runTcp() {
		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_TCP);
		if (!expConfig.init(prefs, this)) {
			return;
		}
		pendingExps.add(expConfig);

		ThreadPoolExecutor executor = createTheadPool(
				new LinkedBlockingDeque<Runnable>(), expConfig);

		for (String domainName : expConfig.getDomainNames()) {
			executor.execute(new TcpTask(
					expConfig.getMeasureObject(domainName), expConfig));
		}
	}

	@Override
	public void runOnceAutoTest() {
		runQuery();
		runPing();
		runTcp();
	}

	@Override
	public void onAlarm(String task) {
		if (task.equals(MeasureTask.TASK_QUERY)) {
			runQuery();
		} else if (task.equals(MeasureTask.TASK_PING)) {
			runPing();
		} else if (task.equals(MeasureTask.TASK_TCP)) {
			runTcp();
		}
	}

	@Override
	public void startMonitorNetstat() {
	}

	@Override
	public void stopMonitorNetstat() {

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

			data.add(DnsQueryTask.resolve(DnsQueryTask.DNS_EX_IP_NAME, true));

			EventLog.write(Type.MONITOR, data);
		}

	}

	@Override
	public String getStatus() {
		StringBuilder stringBuilder = new StringBuilder();
		if (autotest) {
			stringBuilder.append("autotest;");
		}
		for (ExpConfig expConfig : pendingExps) {
			stringBuilder.append(expConfig.getTask());
		}

		return stringBuilder.toString();
	}

}
