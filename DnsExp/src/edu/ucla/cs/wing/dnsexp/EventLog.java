package edu.ucla.cs.wing.dnsexp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;

import android.R.anim;
import android.os.Environment;
import android.util.Log;

public class EventLog {

	public static final String TAG = "dnsexp";
	public static final String SEPARATOR = ";";

	public enum Type {
		DEBUG, MONITOR, DNSQUERY, DNSREPONSE, TCP, PING
	};

	private static PrintWriter logFileWriter;

	private static boolean enabled = true;

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean enabled) {
		EventLog.enabled = enabled;
	}

	public static void close() {
		if (logFileWriter != null) {
			logFileWriter.flush();
			logFileWriter.close();
			logFileWriter = null;
		}
	}

	public static void openNewLogFile(String fileName) {
		if (logFileWriter != null) {
			logFileWriter.flush();
			logFileWriter.close();
		}
		try {
			File dir = new File(Environment.getExternalStorageDirectory()
					+ File.separator + "dnsexp" + File.separator + "log");
			if (!dir.exists()) {
				dir.mkdirs();
			}
			logFileWriter = new PrintWriter(new FileOutputStream(new File(
					dir.getAbsolutePath(), fileName)));
		} catch (FileNotFoundException e) {
			logFileWriter = null;
			write(Type.DEBUG, "Fail to open log file: " + e.toString());
		}
	}

	public static String genLogFileName(List<String> parameters) {
		StringBuilder sb = new StringBuilder();
		sb.append("dns");
		for (String parameter : parameters) {
			sb.append("_");
			String p = parameter.replace('&', '-');
			sb.append(p);
		}
		sb.append(".txt");
		
		return sb.toString();
	}
	
	public static synchronized void write(Type type, String data) {
		if (enabled) {
			StringBuilder sb = new StringBuilder();
			sb.append(System.currentTimeMillis());
			sb.append(SEPARATOR);
			sb.append(type);
			if (data != null && !data.isEmpty()) {
				sb.append(SEPARATOR);
				sb.append(data);				
			}
			sb.append(SEPARATOR);

			if (type != Type.MONITOR) {
				Log.d(TAG, sb.toString());
			}

			if (logFileWriter != null) {
				logFileWriter.println(sb.toString());
				logFileWriter.flush();
			}
		}
		
	}
	
	public static synchronized void write(Type type, List<String> data) {
		if (enabled) {
			StringBuilder sb = new StringBuilder();
			sb.append(System.currentTimeMillis());
			sb.append(SEPARATOR);
			sb.append(type);
			if (data != null) {
				for (String str : data) {
					sb.append(SEPARATOR);
					sb.append(data);
				}
			}
			sb.append(SEPARATOR);

			/*if (type != Type.MONITOR) {
				Log.d(TAG, sb.toString());
			}*/

			if (logFileWriter != null) {
				logFileWriter.println(sb.toString());
				logFileWriter.flush();
			}
		}
	}

	public static synchronized void write(Type type, String[] data) {
		if (enabled) {
			StringBuilder sb = new StringBuilder();
			sb.append(System.currentTimeMillis());
			sb.append(SEPARATOR);
			sb.append(type);
			if (data != null && data.length > 0) {
				for (String str : data) {
					sb.append(SEPARATOR);
					sb.append(data);
				}
			}
			sb.append(SEPARATOR);

			if (type != Type.MONITOR) {
				Log.d(TAG, sb.toString());
			}

			if (logFileWriter != null) {
				logFileWriter.println(sb.toString());
				logFileWriter.flush();
			}
		}
	}
	
	

}
