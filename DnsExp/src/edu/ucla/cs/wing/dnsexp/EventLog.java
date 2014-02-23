package edu.ucla.cs.wing.dnsexp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.os.Environment;
import android.util.Log;

public class EventLog {

	public static final String TAG = "dnsexp";
	public static final String SEPARATOR = ";";

	public static enum LogType {
		DEBUG, DNSQUERY, DNSREPONSE, TCP, PING, APP, META, SCREEN
	};

	private PrintWriter writer;
	
	public void close() {
		if (writer != null) {
			writer.flush();
			writer.close();
		}
	}
	
	public EventLog() {
		
	}
	
	public static void initEnvironment() {
		File dir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "dnsexp" + File.separator + "log");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		dir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "dnsexp" + File.separator + "pcap");
		if (!dir.exists()) {
			dir.mkdir();
		}
	}
	 
	
	public void writePrivate(LogType type, String data) {
		write(writer, type, data);		
	}

	public boolean open(String fileName) {
		boolean ret = true;
		try {
			File dir = new File(Environment.getExternalStorageDirectory()
					+ File.separator + "dnsexp" + File.separator + "log");
			if (!dir.exists()) {
				dir.mkdirs();
			}			
			writer = new PrintWriter(new FileOutputStream(new File(
					dir.getAbsolutePath(), fileName)), true);
		} catch (FileNotFoundException e) {
			writer = null;
			ret = false;
		}
		return ret;
	}

	public static String genLogFileName(List<String> parameters) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String parameter : parameters) {
			if (first) {
				first = false;
			} else {
				sb.append("_");
			}
			String p = parameter.replace('&', '-');
			sb.append(p);
		}
		sb.append(".txt");
		
		return sb.toString();
	}
	
	public static String genMonitorLogFileName() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd", Locale.US);		
		StringBuilder sb = new StringBuilder();
		sb.append("mon_");
		sb.append(sdf.format(new Date(System.currentTimeMillis())));		
		sb.append(".txt");
		return sb.toString();
	}
	
	public static void write(PrintWriter writer, LogType type, String data) {
		StringBuilder sb = new StringBuilder();
		sb.append(System.currentTimeMillis());
		sb.append(SEPARATOR);
		sb.append(type);
		sb.append(SEPARATOR);
		if (data != null) {
			sb.append(data);
			sb.append(SEPARATOR);
		}
		
		Log.d(TAG, sb.toString());		
		if (writer != null) {
			synchronized (writer) {
				writer.println(sb.toString());
				writer.flush();
			}			
		}
	}
	
	
	public static void write(LogType type, String data) {
		write(null, type, data);
	}
	
	public static void write(LogType type, List<String> data) {		
		if (data != null) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String str : data) {
				if (!first) {
					sb.append(SEPARATOR);
				} else {
					first = false;
				}
				sb.append(str);
			}
			write(type, sb.toString());
		}		
	}

	public static void write(LogType type, String[] data) {
		if (data != null) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String str : data) {
				if (!first) {
					sb.append(SEPARATOR);
				} else {
					first = false;
				}
				sb.append(str);
			}
			write(type, sb.toString());
		}		
	}
	
	public void writePrivate(LogType type, List<String> data) {		
		if (data != null) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String str : data) {
				if (!first) {
					sb.append(SEPARATOR);
				} else {
					first = false;
				}
				sb.append(str);
			}
			writePrivate(type, sb.toString());
		}		
	}

	public void writePrivate(LogType type, String[] data) {
		if (data != null) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String str : data) {
				if (!first) {
					sb.append(SEPARATOR);
				} else {
					first = false;
				}
				sb.append(str);
			}
			writePrivate(type, sb.toString());
		}		
	}	

}
