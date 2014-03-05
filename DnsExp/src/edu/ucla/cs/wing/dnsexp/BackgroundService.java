package edu.ucla.cs.wing.dnsexp;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.ucla.cs.wing.dnsexp.EventLog.LogType;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;

public class BackgroundService extends Service implements IController {

	private static IController controller;

	public static IController getController() {
		return controller;
	}

	private SharedPreferences prefs;

	private MobileInfo mobileInfo;

	private AlarmManager alarmManager;
	private PendingIntent alarmIntentQuery, alarmIntentPing, alarmIntentTcp,
			alarmIntentApp, alarmIntentTrace;

	private boolean autotest;

	private HashSet<ExpConfig> pendingExps;

	private Timer monitorTimer;
	private EventLog monitorLogger;

	private TcpdumpHandler tcpdumpHandler;

	private Boolean traceRunning = false;

	private BroadcastReceiver screenReceiver;

	private NotificationManager notificationManager;
	public static final int NOTIFY_TRACE = 1;

	private static Handler _handler;
	
	private boolean enablePcap = true;

	public static Handler getHandler() {
		return _handler;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		controller = this;

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		EventLog.initEnvironment();

		MobileInfo.init(this);
		mobileInfo = MobileInfo.getInstance();

		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		pendingExps = new HashSet<ExpConfig>();

		_handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case Msg.TRACE_STATE:
					sendMsgToUi(Msg.TRACE_STATE, traceRunning);
					break;
				case Msg.PCAP_STATE:
					sendMsgToUi(Msg.PCAP_STATE, (Boolean) enablePcap);
					break;

				default:
					break;
				}
			}
		};

		tcpdumpHandler = new TcpdumpHandler();
		tcpdumpHandler.deployTcpDump();

		screenReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
					onScreen(true);
				} else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
					onScreen(false);
				}
			}
		};
		registerReceiver(screenReceiver, new IntentFilter(
				Intent.ACTION_SCREEN_ON));
		registerReceiver(screenReceiver, new IntentFilter(
				Intent.ACTION_SCREEN_OFF));

		if (Integer.parseInt(prefs.getString("auto_trace",
				getString(R.string.pref_default_auto_trace))) != 0)
			startTrace();

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
		if (autotest)
			return;
		
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
		// intent4.putExtra(MeasureTask.TASK, MeasureTask.TASK_APP);
		intent4.putExtra("task", "app");
		alarmIntentApp = PendingIntent.getBroadcast(this, 4, intent4, 0);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,
				appPeriod, alarmIntentApp);
		autotest = true;
		
		
	}

	@Override
	public void stopAutoTest() {
		if (!autotest)
			return;
		
		if (alarmManager != null) {
			alarmManager.cancel(alarmIntentQuery);
			alarmManager.cancel(alarmIntentPing);
			alarmManager.cancel(alarmIntentTcp);
			alarmManager.cancel(alarmIntentApp);
		}
		autotest = false;
		sendMsgToUi(Msg.DONE, null);	
		
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
			this.execute(new MonitorNetstatTask(expConfig.getLogger(), 1));
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
							expConfig.getTask(), finishedCnt, taskCnt));
			if (finishedCnt == taskCnt) {
				expConfig.cleanUp();
				pendingExps.remove(expConfig);
				if (pendingExps.size() == 0 && !autotest) {
					sendMsgToUi(Msg.DONE, null);
				}
			}
		}
	}

	private void runQuery() {
		String configFile = prefs.getString("config_file",
				getString(R.string.pref_default_config_file));

		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_QUERY, configFile,
				this);
		expConfig.deployQueryInputFile();
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
		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_PING, configFile,
				this);
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
		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_TCP, configFile,
				this);
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

		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_APP,
				appConfigFile, this);
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

	public class MonitorNetstatTask extends TimerTask {
		private EventLog logger;
		private int mode;

		public MonitorNetstatTask(EventLog logger, int mode) {
			this.logger = logger;
			this.mode = mode;
		}

		@Override
		public void run() {
			List<String> data = new LinkedList<String>();
			data.add(mobileInfo.getOperatorName());
			data.add(mobileInfo.getNetworkTech());
			data.add(mobileInfo.getNetworkTypeStr());
			if (mode == 1) {
				data.add(mobileInfo.getLocalIpAddress());
				data.add(mobileInfo.getDnsServer(1));
				data.add(mobileInfo.getDnsServer(2));
				data.add(DnsQueryTask
						.resolve(DnsQueryTask.DNS_EX_IP_NAME, true));
			} else if (mode == 2) {
				data.add(String.valueOf(mobileInfo.getSignalStrengthDBM()));
				data.add(String.valueOf(mobileInfo.getWifiSignalStrength()));
				data.add(String.valueOf(mobileInfo.getCallState()));
				data.add(mobileInfo.getLocalIpAddress());
				data.add(mobileInfo.getDnsServer(1));
				data.add(mobileInfo.getDnsServer(2));
				data.add(DnsQueryTask
						.resolve(DnsQueryTask.DNS_EX_IP_NAME, true));
			}
			if (logger != null) {
				logger.writePrivate(LogType.META, data);
			} else {
				EventLog.write(LogType.META, data);
			}

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

		ExpConfig expConfig = new ExpConfig(MeasureTask.TASK_QUERY, configFile,
				this);
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

	@Override
	public boolean isTraceRunning() {
		return traceRunning;
	}

	private void setTraceRunning(boolean running) {
		synchronized (traceRunning) {
			traceRunning = running;
			sendMsgToUi(Msg.TRACE_STATE, traceRunning);
		}
	}

	private void refreshMonitorTimer() {
		if (monitorTimer != null) {
			monitorTimer.cancel();
		}
		if (monitorLogger != null) {
			monitorLogger.close();
		}

		long interval = Long.parseLong(prefs.getString("monitor_interval",
				getString(R.string.pref_default_monitor_interval)));
		monitorLogger = new EventLog();
		monitorLogger.open(EventLog.genMonitorLogFileName());
		monitorTimer = new Timer();
		monitorTimer.schedule(new MonitorNetstatTask(monitorLogger, 2), 0,
				interval);
	}

	private void stopMonitorTimer() {
		if (monitorTimer != null) {
			monitorTimer.cancel();
		}
	}

	private void refreshTrace() {
		refreshMonitorTimer();
		if (enablePcap) {
			tcpdumpHandler.startTcpdump();
		}
	}

	@Override
	public void startTrace() {
		if (!isTraceRunning()) {
			setTraceRunning(true);
			refreshTrace();

			Intent intent = new Intent(this, AlarmReceiver.class);
			intent.putExtra(MeasureTask.TASK, MeasureTask.TASK_TRACE);
			alarmIntentTrace = PendingIntent.getBroadcast(this, 10, intent, 0);
			long interval = 1000 * Long.parseLong(prefs.getString(
					"trace_refresh_interval",
					getString(R.string.pref_default_trace_refresh_interval)));
			alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,
					interval, alarmIntentTrace);

			if (notificationManager != null) {
				Intent intent1 = new Intent(this, MainActivity.class);
				PendingIntent contentIntent = PendingIntent.getActivity(this,
						0, intent1, 0);
				Notification notification = new Notification(
						R.drawable.ic_launcher, "Start collecting trace",
						System.currentTimeMillis());
				notification.setLatestEventInfo(this, "DnsExp Trace",
						"Logging network status and packet trace",
						contentIntent);
				notificationManager.notify(NOTIFY_TRACE, notification);
			}
		}
	}

	@Override
	public void stopTrace() {
		if (isTraceRunning()) {
			setTraceRunning(false);
			stopMonitorTimer();
			tcpdumpHandler.stopTcpdump();
			if (alarmManager != null && alarmIntentTrace != null) {
				alarmManager.cancel(alarmIntentTrace);
			}

			if (notificationManager != null) {
				notificationManager.cancel(NOTIFY_TRACE);
			}

		}
	}

	@Override
	public void onScreen(boolean on) {
		if (monitorLogger != null) {
			monitorLogger.writePrivate(LogType.SCREEN, on ? "1" : "0");
		}
	}

	@Override
	public void refreshMonitor() {
		if (isTraceRunning()) {
			refreshMonitorTimer();
		}

	}

	
	@Override
	public void setEnablePcap(boolean enabled) {
		enablePcap = enabled;
		if (!enabled) {
			tcpdumpHandler.stopTcpdump();
			
		}
	}

	@Override
	public boolean isEnablePcap() {
		return enablePcap;

	}

}
