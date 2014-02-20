package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Proxy.Type;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.ucla.cs.wing.dnsexp.EventLog.LogType;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class BackgroundService extends Service implements IController {

	private static IController controller;

	public static IController getController() {
		return controller;
	}

	private SharedPreferences prefs;

	private MobileInfo mobileInfo;

	private AlarmManager alarmManager;
	private PendingIntent alarmIntentQuery, alarmIntentPing, alarmIntentTcp,
			alarmIntentApp;

	private boolean autotest;

	private HashSet<ExpConfig> pendingExps;

	@Override
	public void onCreate() {
		super.onCreate();

		controller = this;

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		EventLog.initEnvironment();
		
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

		long appPeriod = 1000 * Long.parseLong(prefs.getString(
				"autotest_app_period",
				getString(R.string.pref_default_autotest_app_period)));
		Intent intent4 = new Intent(this, AlarmReceiver.class);
		//intent4.putExtra(MeasureTask.TASK, MeasureTask.TASK_APP);
		intent4.putExtra("task", "app");
		alarmIntentApp = PendingIntent.getBroadcast(this, 4, intent4, 0);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,
				appPeriod, alarmIntentApp);
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
		
		private int taskCnt = 0;

		public ExpTheadPoolExecutor(int corePoolSize, int maxPoolSize,
				BlockingQueue<Runnable> workQueue, ExpConfig expConfig) {
			super(corePoolSize, maxPoolSize, 1, TimeUnit.SECONDS, workQueue);
			this.expConfig = expConfig;
			
			this.execute(new MonitorNetstatTask(expConfig));
			this.taskCnt = 1 + expConfig.getSize();			
		}
		
		@Override
		public void execute(Runnable runnable) {			
			super.execute(runnable);			
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			finishedCnt += 1;
			EventLog.write(
					LogType.DEBUG,
					String.format("Progress of %s: %d / %d",
							expConfig.getTask(), finishedCnt,
							taskCnt));
			if (finishedCnt == taskCnt) {
				expConfig.cleanUp();
				pendingExps.remove(expConfig);
				if (pendingExps.size() == 0) {					
					sendMsgToUi(Msg.DONE, null);
				}
			}
		}
	}	

	private void runQuery() {
		String configFile = prefs.getString("config_file",
				getString(R.string.pref_default_config_file));
		
		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_QUERY, configFile, this);
		if (!expConfig.init(prefs)) {
			return;
		}
		pendingExps.add(expConfig);

		expConfig.prepareExp();
		ThreadPoolExecutor executor = createTheadPool(
				new LinkedBlockingDeque<Runnable>(), expConfig);
		for (String domainName : expConfig.getDomainNames()) {
			executor.execute(new DnsQueryTask(expConfig
					.getMeasureObject(domainName), expConfig));
		}
	}

	private void runPing() {
		String configFile = prefs.getString("config_file",
				getString(R.string.pref_default_config_file));
		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_PING, configFile, this);
		if (!expConfig.init(prefs)) {
			return;
		}
		pendingExps.add(expConfig);

		expConfig.prepareExp();
		ThreadPoolExecutor executor = createTheadPool(
				new LinkedBlockingDeque<Runnable>(), expConfig);
		for (String domainName : expConfig.getDomainNames()) {
			executor.execute(new PingTask(expConfig
					.getMeasureObject(domainName), expConfig));
		}
	}

	private void runTcp() {
		String configFile = prefs.getString("config_file",
				getString(R.string.pref_default_config_file));
		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_TCP, configFile, this);
		if (!expConfig.init(prefs)) {
			return;
		}
		pendingExps.add(expConfig);

		expConfig.prepareExp();
		ThreadPoolExecutor executor = createTheadPool(
				new LinkedBlockingDeque<Runnable>(), expConfig);
		for (String domainName : expConfig.getDomainNames()) {
			executor.execute(new TcpTask(
					expConfig.getMeasureObject(domainName), expConfig));
		}
	}

	private void runApp() {
		String appConfigFile = prefs.getString("appconfig_file",
				getString(R.string.pref_default_appconfig_file));
		
		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_APP, appConfigFile, this);
		if (!expConfig.init(prefs)) {
			return;
		}
		pendingExps.add(expConfig);

		expConfig.prepareExp();
		ThreadPoolExecutor executor = createTheadPool(
				new LinkedBlockingDeque<Runnable>(), expConfig);

		for (String name : expConfig.getDomainNames()) {
			executor.execute(new AppTask(expConfig.getMeasureObject(name),
					expConfig));
		}

	}

	@Override
	public void runOnceAutoTest() {
		runQuery();
		runPing();
		runTcp();
		runApp();
	}

	@Override
	public void onAlarm(String task) {
		if (task == null) {
			return;
		}
		if (task.equals(MeasureTask.TASK_QUERY)) {
			runQuery();
		} else if (task.equals(MeasureTask.TASK_PING)) {
			runPing();
		} else if (task.equals(MeasureTask.TASK_TCP)) {
			runTcp();
		} else if (task.equals(MeasureTask.TASK_APP)) {
			runApp();
		}
	}

	@Override
	public void startMonitorNetstat() {
	}

	@Override
	public void stopMonitorNetstat() {

	}

	public class MonitorNetstatTask extends TimerTask {
		private ExpConfig expConfig;
		
		public MonitorNetstatTask(ExpConfig expConfig) {
			this.expConfig = expConfig;
		}

		@Override
		public void run() {
			List<String> data = new LinkedList<String>();
			data.add(mobileInfo.getOperatorName());
			data.add(mobileInfo.getNetworkTech());
			data.add(mobileInfo.getNetworkTypeStr());
			data.add(mobileInfo.getLocalIpAddress());						
			String s1 = "", s2 = "";			
			try {
				Process process = Runtime.getRuntime().exec("getprop net.dns1");				
				BufferedReader in = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
				s1 = in.readLine();
				in.close();				
				process = Runtime.getRuntime().exec("getprop net.dns2");				
				in = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
				s2 = in.readLine();
				in.close();			
			} catch (IOException e) {
			}			
			data.add(s1);
			data.add(s2);
			data.add(DnsQueryTask.resolve(DnsQueryTask.DNS_EX_IP_NAME, true));
			expConfig.getLogger().writePrivate(LogType.META, data);			
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

	@Override
	public void runAppStoreTest() {
		String configFile = prefs.getString("appstore_file",
				getString(R.string.pref_default_appstore_file));
		
		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_QUERY, configFile, this);
		expConfig.deployAppstoreFile();
		if (!expConfig.init(prefs)) {
			sendMsgToUi(Msg.ERROR, null);			
			return;
		}
		
		pendingExps.add(expConfig);

		expConfig.prepareExp();
		ThreadPoolExecutor executor = createTheadPool(
				new LinkedBlockingDeque<Runnable>(), expConfig);
		for (String domainName : expConfig.getDomainNames()) {
			executor.execute(new DnsQueryTask(expConfig
					.getMeasureObject(domainName), expConfig));
		}
		
	}
	
	private void sendMsgToUi(int what, Object obj) {
		Handler handler = MainActivity.getHandler();
		if (handler != null) {
			Message message = new Message();
			message.what = what;
			message.obj = obj;
			handler.sendMessage(message);
		}
	}

}
