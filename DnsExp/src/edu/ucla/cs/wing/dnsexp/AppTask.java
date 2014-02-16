package edu.ucla.cs.wing.dnsexp;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import edu.ucla.cs.wing.dnsexp.EventLog.LogType;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;

public class AppTask extends MeasureTask {

	private HostnameVerifier hostnameVerifier;

	private static final String[] DL_APP_KEYWORDS = { "dropbox",
			"googleusercontent" };
	
	private static final int BUF_SIZE = 1024;

	public AppTask(MeasureObject measureObject, ExpConfig expConfig) {
		super(measureObject, expConfig);
		task = TASK_APP;

		hostnameVerifier = new HostnameVerifier() {

			@Override
			public boolean verify(String hostname, SSLSession session) {
				EventLog.write(LogType.DEBUG, hostname);
				return true;

			}
		};
	}

	boolean isDownloadTask(String urlStr) {
		for (String keyword : DL_APP_KEYWORDS) {
			if (urlStr.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void run() {
		char[] buffer = new char[BUF_SIZE]; 
		for (String label : measureObject.getAddrGroupLabels()) {
			for (String addr : measureObject.getAddrGroup(label).getAddrs()) {
				int repeat = isDownloadTask(measureObject.getDomainName()) ? expConfig
						.getAppDlRepeat() : expConfig.getAppRepeat();
						
				for (int i = 0; i < repeat; i ++) {
					if (measureObject.getDomainName().startsWith("https://")) {
						HttpsURLConnection conn = null;
						try {
							long t1, t2, t3;
							int size = 0, r = 0;						
							String urlString = replaceNameWithAddr(
									measureObject.getDomainName(), addr);
							URL url = new URL(urlString);
							conn = (HttpsURLConnection) url.openConnection();
							conn.setRequestMethod("GET");
							conn.setDoInput(true);
							conn.setHostnameVerifier(hostnameVerifier);

							t1 = System.currentTimeMillis();
							conn.connect();
							int responseCode = conn.getResponseCode();
							t2 = System.currentTimeMillis();
							InputStreamReader reader = new InputStreamReader(conn.getInputStream());
							while ((r = reader.read(buffer)) > 0) {
								size += r;
							}
							t3 = System.currentTimeMillis();

							onApp(label, addr, responseCode, t1, t2, t3, size);
						} catch (IOException e) {
							expConfig.getLogger().writePrivate(LogType.DEBUG, e.toString());						
						} finally {
							if (conn != null)
								conn.disconnect();
						}
					} else if (measureObject.getDomainName().startsWith("http://")) {
						HttpURLConnection conn = null;
						try {
							long t1, t2, t3;
							int size = 0, r = 0;						
							String urlString = replaceNameWithAddr(
									measureObject.getDomainName(), addr);
							URL url = new URL(urlString);
							conn = (HttpURLConnection) url.openConnection();
							conn.setRequestMethod("GET");
							
							if (measureObject.getDomainName().contains("skype"))
								conn.setRequestProperty("Host", "pricelist.skype.com");						
							conn.setDoInput(true);						

							t1 = System.currentTimeMillis();
							conn.connect();
							int responseCode = conn.getResponseCode();
							t2 = System.currentTimeMillis();
							InputStreamReader reader = new InputStreamReader(conn.getInputStream());
							while ((r = reader.read(buffer)) > 0) {
								size += r;
							}
							t3 = System.currentTimeMillis();

							onApp(label, addr, responseCode, t1, t2, t3, size);
						} catch (IOException e) {
							expConfig.getLogger().writePrivate(LogType.DEBUG, e.toString());
						} finally {
							if (conn != null)
								conn.disconnect();
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

}
