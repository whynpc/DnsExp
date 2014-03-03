package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import edu.ucla.cs.wing.dnsexp.EventLog.LogType;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;
import edu.ucla.cs.wing.dnsexp.PingTask.PingLatency;
import edu.ucla.cs.wing.dnsexp.R.string;

public class AppTask extends MeasureTask {

	private HostnameVerifier hostnameVerifier;

	private static final String[] DL_APP_KEYWORDS = { "dropbox",
			"googleusercontent" };

	private static final int BUF_SIZE = 1024;

	public AppTask(MeasureObject measureObject, ExpConfig expConfig) {
		super(measureObject, expConfig);
		task = TASK_APP;
		CookieHandler.setDefault(new CookieManager(null,
				CookiePolicy.ACCEPT_ALL));
		HttpsURLConnection.setFollowRedirects(true);

		hostnameVerifier = new HostnameVerifier() {

			@Override
			public boolean verify(String hostname, SSLSession session) {
				EventLog.write(LogType.DEBUG, hostname);
				return true;

			}
		};
	}

	private boolean httping, pinging;

	private String currentAddr, currentLabel, currentName;

	boolean isDownloadTask(String urlStr) {
		for (String keyword : DL_APP_KEYWORDS) {
			if (urlStr.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	private void runHttps(String addr, String label) {
		HttpsURLConnection conn = null;
		char[] buffer = new char[BUF_SIZE];
		try {
			long t1, t2, t3;
			int size = 0, r = 0;
			String urlString = replaceNameWithAddr(
					measureObject.getDomainName(), addr);
			URL url = new URL(urlString);
			EventLog.write(LogType.DEBUG, urlString);
			conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setInstanceFollowRedirects(true);

			// conn.setRequestProperty("Host", getDomainName(urlString));
			conn.setDoInput(true);
			conn.setHostnameVerifier(hostnameVerifier);

			t1 = System.currentTimeMillis();
			conn.connect();
			int responseCode = conn.getResponseCode();
			t2 = System.currentTimeMillis();
			InputStreamReader reader = new InputStreamReader(
					conn.getInputStream());
			while ((r = reader.read(buffer)) > 0) {
				size += r;
			}
			t3 = System.currentTimeMillis();

			onApp(label, addr, responseCode, t1, t2, t3, size);
		} catch (IOException e) {
			EventLog.write(LogType.DEBUG, "Error: " + currentName);
			expConfig.getLogger().writePrivate(LogType.DEBUG, e.toString());
		} finally {
			if (conn != null)
				conn.disconnect();
		}
		httping = false;

	}

	private void runHttp(String addr, String label) {
		HttpURLConnection conn = null;
		char[] buffer = new char[BUF_SIZE];
		try {
			long t1, t2, t3;
			int size = 0, r = 0;
			String urlString = replaceNameWithAddr(
					measureObject.getDomainName(), addr);
			EventLog.write(LogType.DEBUG, urlString);
			URL url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setInstanceFollowRedirects(true);
			conn.setDoInput(true);
			
			//conn.setRequestProperty("Host", getDomainName(urlString));
			
			
			

			t1 = System.currentTimeMillis();
			try {
				conn.connect();
			} catch (Exception e) {
			}
			int responseCode = conn.getResponseCode();
			t2 = System.currentTimeMillis();
			InputStreamReader reader = new InputStreamReader(
					conn.getInputStream());
			while ((r = reader.read(buffer)) > 0) {
				size += r;
			}
			t3 = System.currentTimeMillis();

			onApp(label, addr, responseCode, t1, t2, t3, size);
		} catch (IOException e) {
			EventLog.write(LogType.DEBUG, "Error: " + currentName);
			expConfig.getLogger().writePrivate(LogType.DEBUG, e.toString());
		} finally {
			if (conn != null)
				conn.disconnect();
		}
		httping = false;
	}

	private void runPing(String addr, String label) {

		String cmd = String.format("ping -c %d -i %f -w %f %s",
				expConfig.getPingRepeat(), expConfig.getPingInterval(),
				expConfig.getPingdeadLine(), addr);

		PingLatency pingLatency = new PingLatency();

		Process process;
		try {
			process = Runtime.getRuntime().exec(cmd);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				Matcher matcher = PingTask.PING_OUTPUT_PATTERN.matcher(line);
				if (matcher.find()) {
					pingLatency
							.addPingRes(Double.parseDouble(matcher.group(1)));
				}
			}

			int maxTrHop = 1;
			cmd = String.format(Locale.US, "su -c traceroute -m %d %s",
					maxTrHop, addr);
//			for (int i = 0; i < expConfig.getTrRepeat(); i++) {
//				process = Runtime.getRuntime().exec(cmd);
//				in = new BufferedReader(new InputStreamReader(
//						process.getInputStream()));
//				while ((line = in.readLine()) != null) {
//					Matcher matcher = PingTask.TR_OUTPUT_PATTERN.matcher(line);
//					while (matcher.find()) {
//						pingLatency.addTrRes(Double.parseDouble(matcher
//								.group(1)));
//					}
//				}
//			}
		} catch (IOException e) {

		}
		pingLatency.postProcess();
		onPing(label, addr, pingLatency);

		pinging = false;

	}

	@Override
	public void run() {
		int repeat = isDownloadTask(measureObject.getDomainName()) ? expConfig
				.getAppDlRepeat() : expConfig.getAppRepeat();
		for (int i = 0; i < repeat; i++) {
			for (String label : measureObject.getAddrGroupLabels()) {
				for (String addr : measureObject.getAddrGroup(label).getAddrs()) {
					currentName = measureObject.getDomainName();
					currentAddr = addr;
					currentLabel = label;

					httping = true;
					new Thread() {
						@Override
						public void run() {
							CookieHandler.setDefault(new CookieManager(null,
									CookiePolicy.ACCEPT_ALL));

							if (measureObject.getDomainName().startsWith(
									"https://")) {
								runHttps(currentAddr, currentLabel);

							} else if (measureObject.getDomainName()
									.startsWith("http://")) {
								runHttp(currentAddr, currentLabel);
							}
						}
					}.start();

					pinging = true;
					new Thread() {
						@Override
						public void run() {
							runPing(currentAddr, currentLabel);
						}
					}.start();

					while (httping || pinging) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private static String getDomainName(String urlStr) {
		String domainName = null;
		int i = urlStr.indexOf("://");
		int j = urlStr.indexOf('/', i + 3);
		if (j > 0) {
			domainName = urlStr.substring(i + 3, j);
		} else {
			domainName = urlStr.substring(i + 3);
		}
		return domainName;
	}

	private static String replaceNameWithAddr(String urlStr, String addr) {
		String domainName = getDomainName(urlStr);
		return urlStr.replace(domainName, addr);
	}

	public void onApp(String label, String addr, int responseCode,
			long reqTime, long responseTime, long dataTime, int size) {
		List<String> data = new LinkedList<String>();
		data.add(measureObject.getDomainName());
		data.add(label);
		data.add(addr);
		data.add(String.valueOf(responseCode));
		data.add(String.valueOf(reqTime));
		data.add(String.valueOf(responseTime));
		data.add(String.valueOf(dataTime));
		data.add(String.valueOf(size));
		expConfig.getLogger().writePrivate(LogType.APP, data);
	}

	private void onPing(String label, String addr, PingLatency latency) {
		List<String> data = new LinkedList<String>();
		data.add(measureObject.getDomainName());
		data.add(label);
		data.add(addr);
		data.add(String.valueOf(latency.getMinPingLatency()));
		data.add(String.valueOf(latency.getMedianPingLatency()));
		data.add(String.valueOf(latency.getMaxPingLatency()));
		data.add(String.valueOf(latency.getMinTrLatency()));
		data.add(String.valueOf(latency.getMedianTrLatency()));
		data.add(String.valueOf(latency.getMaxTrLatency()));
		expConfig.getLogger().writePrivate(LogType.PING, data);
	}

}
