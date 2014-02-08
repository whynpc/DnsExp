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

public class BackgroundService extends Service implements IController,
		IExpResHandler {

	private static IController controller;

	public static IController getController() {
		return controller;
	}

	private SharedPreferences prefs;

	private MobileInfo mobileInfo;

	private AlarmManager alarmManager;
	private PendingIntent alarmIntentQuery, alarmIntentPing;

	private Timer taskTimer, monitorTimer;

	private ExpConfig expConfig;
	private String configFile;

	private ThreadPoolExecutor threadPoolExecutor;

	boolean running, autotest;

	@Override
	public void onCreate() {
		super.onCreate();

		controller = this;

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		MobileInfo.init(this);
		mobileInfo = MobileInfo.getInstance();

		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		taskTimer = new Timer();

		expConfig = new ExpConfig();

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
		if (records != null) {
			for (Record record : records) {
				if (record instanceof ARecord) {
					ARecord aRecord = (ARecord) record;
					data.add(aRecord.getAddress().getHostAddress() + ","
							+ aRecord.getTTL());
				}
			}
		}

		EventLog.write(Type.DNSREPONSE, data);
	}

	@Override
	public void startAutoTest() {
		long queryPeriod = Long.parseLong(prefs.getString(
				"autotest_query_period",
				getString(R.string.pref_default_autotest_query_period)));
		Intent intent1 = new Intent(this, AlarmReceiver.class);
		intent1.putExtra(MeasureTask.TASK, MeasureTask.TASK_QUERY);
		alarmIntentQuery = PendingIntent.getBroadcast(this, 0, intent1, 0);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,
				queryPeriod * 1000, alarmIntentQuery);

		long pingPeriod = Long.parseLong(prefs.getString(
				"autotest_ping_period",
				getString(R.string.pref_default_autotest_ping_period)));
		Intent intent2 = new Intent(this, AlarmReceiver.class);
		alarmIntentPing = PendingIntent.getBroadcast(this, 0, intent2, 0);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,
				pingPeriod * 1000, alarmIntentPing);

		autotest = true;
	}

	@Override
	public void stopAutoTest() {
		if (alarmManager != null) {
			alarmManager.cancel(alarmIntentQuery);
			alarmManager.cancel(alarmIntentPing);
		}
		autotest = false;
	}

	private synchronized boolean updateExpConfig() {
		configFile = prefs.getString("config_file",
				getString(R.string.pref_default_config_file));
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
		expConfig.setPingInterval(Float.parseFloat(prefs
				.getString("ping_interval",
						getString(R.string.pref_default_ping_interval))));
		expConfig.setPingdeadLine(Float.parseFloat(prefs
				.getString("ping_deadline",
						getString(R.string.pref_default_ping_deadline))));
		expConfig.setTrRepeat(Integer.parseInt(prefs.getString("tr_repeat",
				getString(R.string.pref_default_tr_repeat))));

		expConfig.setTcpRepeat(Integer.parseInt(prefs.getString("tcp_repeat",
				getString(R.string.pref_default_tcp_repeat))));
		String tcpPortsStr = prefs.getString("tcp_ports",
				getString(R.string.pref_default_tcp_ports));
		for (String str : tcpPortsStr.split(",")) {
			expConfig.addTcpPort(Short.parseShort(str));
		}

		boolean r = expConfig.load(configFile);
		EventLog.write(Type.DEBUG, "Add names " + expConfig.getDomainNames().size());
		return r;

	}

	public class ExpTheadPoolExecutor extends ThreadPoolExecutor {

		private ExpConfig expConfig;

		private int finishedCnt = 0;

		public ExpTheadPoolExecutor(int corePoolSize, int maximumPoolSize,
				long keepAliveTime, TimeUnit unit,
				BlockingQueue<Runnable> workQueue, ExpConfig expConfig) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);

			this.expConfig = expConfig;
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			finishedCnt += 1;
			if (finishedCnt == expConfig.getSize()) {				
				EventLog.close();
				if (expConfig.isSelfUpdating( )) {
					expConfig.save(configFile);
				}
			}
		}

	}

	private void runOnceQuery() {
		if (!expConfig.toQuery()) {
			return;
		}

		long delay = 0;

		taskTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				List<String> parameters = new LinkedList<String>();
				parameters.add("dns");
				parameters.add(String.valueOf(System.currentTimeMillis()));
				parameters.add(mobileInfo.getOperatorName());
				parameters.add(mobileInfo.getNetworkTech());
				parameters.add(mobileInfo.getNetworkTypeStr());
				parameters.add(mobileInfo.getPhoneModel());
				EventLog.openNewLogFile(EventLog.genLogFileName(parameters));

				running = true;
			}
		}, delay);
		delay++;

		for (String domainName : expConfig.getDomainNames()) {
			taskTimer.schedule(
					new DnsQueryTask(expConfig.getMeasureObject(domainName),
							expConfig, this), delay);
			delay++;
		}

		taskTimer.schedule(new TimerTask() {
			@Override
			public void run() {

				if (expConfig.isSelfUpdating()) {
					expConfig.save(configFile);
				}
				EventLog.close();
				running = false;
			}
		}, delay);
		EventLog.write(Type.DEBUG, String.valueOf(delay));

	}

	private void runOncePing() {
		if (!expConfig.toPing()) {
			return;
		}
		EventLog.write(Type.DEBUG, "To run ping test");

		List<String> parameters = new LinkedList<String>();
		parameters.add("ping");
		parameters.add(String.valueOf(System.currentTimeMillis()));
		parameters.add(mobileInfo.getOperatorName());
		parameters.add(mobileInfo.getNetworkTech());
		parameters.add(mobileInfo.getNetworkTypeStr());
		parameters.add(mobileInfo.getPhoneModel());
		EventLog.openNewLogFile(EventLog.genLogFileName(parameters));

		threadPoolExecutor = new ExpTheadPoolExecutor(5, 10, 1,
				TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(),
				expConfig);
		for (String domainName : expConfig.getDomainNames()) {
			//EventLog.write(Type.DEBUG, "Add task: " + domainName);
			threadPoolExecutor.execute(new PingTask(expConfig
					.getMeasureObject(domainName), expConfig, this));
		}		
	}

	@Override
	public void runOnceAutoTest() {
		if (updateExpConfig()) {
			runOnceQuery();
			runOncePing();
			
		}
		
	}

	@Override
	public void onAlarm(String task) {
		updateExpConfig();
		if (task.equals(MeasureTask.TASK_QUERY)) {
			runOnceQuery();
		} else if (task.equals(MeasureTask.TASK_PING)) {
			runOncePing();
		}
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

			data.add(DnsQueryTask.resolve(DnsQueryTask.DNS_EX_IP_NAME, true));

			EventLog.write(Type.MONITOR, data);

		}

	}

	@Override
	public void onPing(String domainName, String addrGroupLabel, String addr,
			double minPingLatency, double medianPingLatency,
			double maxPingLatency, double minTrLatency, double medianTrLatency,
			double maxTrLatency) {
		List<String> data = new LinkedList<String>();
		data.add(domainName);
		data.add(addrGroupLabel);
		data.add(addr);
		data.add(String.valueOf(minPingLatency));
		data.add(String.valueOf(medianPingLatency));
		data.add(String.valueOf(maxPingLatency));
		data.add(String.valueOf(minTrLatency));
		data.add(String.valueOf(medianTrLatency));
		data.add(String.valueOf(maxTrLatency));
		EventLog.write(Type.PING, data);
	}

	@Override
	public void onTcp(String domainName, String addrGroupLabel, String addr,
			long minLatency, long medianLatency, long maxLatency) {
		List<String> data = new LinkedList<String>();
		data.add(domainName);
		data.add(addrGroupLabel);
		data.add(addr);
		data.add(String.valueOf(minLatency));
		data.add(String.valueOf(medianLatency));
		data.add(String.valueOf(maxLatency));
		EventLog.write(Type.TCP, data);
	}

	@Override
	public String getStatus() {
		StringBuilder stringBuilder = new StringBuilder();
		if (autotest) {
			stringBuilder.append("Auto; ");
		}
		if (running) {
			stringBuilder.append("Running; ");
		}
		if (expConfig != null) {
			if (expConfig.toQuery()) {
				stringBuilder.append("Query; ");
			}
			if (expConfig.toPing()) {
				stringBuilder.append("Ping; ");
			}
			if (expConfig.toTcp()) {
				stringBuilder.append("Tcp; ");
			}
		}

		return null;
	}

}
