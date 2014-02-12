package edu.ucla.cs.wing.dnsexp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import android.os.Environment;
import edu.ucla.cs.wing.dnsexp.EventLog.Type;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;

public class AppTask extends MeasureTask {
	
	private HostnameVerifier hostnameVerifier;

	public AppTask(MeasureObject measureObject, ExpConfig expConfig) {
		super(measureObject, expConfig);		 
		task = TASK_APP;
		
	
		hostnameVerifier = new HostnameVerifier() {
			
			@Override
			public boolean verify(String hostname, SSLSession session) {
				HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
				EventLog.write(Type.DEBUG, hostname);
				return true;

			}
		};
	}

	@Override
	public void run() {
		for (int i = 0; i < expConfig.getAppRepeat(); i++) {
			for (String label : measureObject.getAddrGroupLabels()) {
				for (String addr : measureObject.getAddrGroup(label).getAddrs()) {
					if (measureObject.getDomainName().startsWith("https://")) {
						HttpsURLConnection conn = null;
						try {
							long t1, t2;
							String urlString = replaceNameWithAddr(measureObject.getDomainName(), addr);							
							URL url = new URL(urlString);
							conn = (HttpsURLConnection) url.openConnection();
							conn.setRequestMethod("GET");
							conn.setDoInput(true);
							conn.setHostnameVerifier(hostnameVerifier);
							
							t1 = System.currentTimeMillis();
							conn.connect();
							int responseCode = conn.getResponseCode();
							t2 = System.currentTimeMillis();

							onApp(label, addr, responseCode, t1, t2);
						} catch (MalformedURLException e) {
							EventLog.write(Type.DEBUG, e.toString());
						} catch (IOException e) {
							EventLog.write(Type.DEBUG, e.toString());
						} finally {
							if (conn != null)
								conn.disconnect();
						}
					} else if (measureObject.getDomainName().startsWith(
							"http://")) {
						HttpURLConnection conn = null;
						try {
							long t1, t2;
							String urlString = replaceNameWithAddr(measureObject.getDomainName(), addr);							
							URL url = new URL(urlString);
							conn = (HttpURLConnection) url.openConnection();
							conn.setRequestMethod("GET");
							conn.setDoInput(true);
							t1 = System.currentTimeMillis();
							conn.connect();
							int responseCode = conn.getResponseCode();
							t2 = System.currentTimeMillis();

							onApp(label, addr, responseCode, t1, t2);
						} catch (MalformedURLException e) {
							e.printStackTrace();
						} catch (ProtocolException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
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
		
		
		/*File dir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "dnsexp");
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileOutputStream(new File(
					dir.getAbsolutePath(), "hosts")));
			writer.println("127.0.0.1\tlocalhost");
			writer.print(addr);
			writer.print('\t');
			writer.println(domainName);
			writer.flush();
		} catch (FileNotFoundException e) {
			
		} finally {
			writer.close();
		}
		
		try {
			Runtime.getRuntime().exec("su -c cp /sdcard/dnsexp/hosts /system/etc/");
		} catch (IOException e) {		
			e.printStackTrace();
		}*/
	}

	public void onApp(String label, String addr, int responseCode,
			long reqTime, long responseTime) {
		List<String> data = new LinkedList<String>();
		data.add(measureObject.getDomainName());
		data.add(label);
		data.add(addr);
		data.add(String.valueOf(responseCode));
		data.add(String.valueOf(reqTime));
		data.add(String.valueOf(responseTime));
		expConfig.getLogger().writePrivate(Type.APP, data);
	}

}
