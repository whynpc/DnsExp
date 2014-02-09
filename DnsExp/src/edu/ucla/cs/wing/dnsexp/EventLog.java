package edu.ucla.cs.wing.dnsexp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import android.R.anim;
import android.os.Environment;
import android.util.Log;

public class EventLog {

	public static final String TAG = "dnsexp";
	public static final String SEPARATOR = ";";

	public static enum Type {
		DEBUG, MONITOR, DNSQUERY, DNSREPONSE, TCP, PING
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
	
	public void writePrivate(Type type, String data) {
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
					dir.getAbsolutePath(), fileName)));
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
	
	public static synchronized void write(PrintWriter writer, Type type, String data) {
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
			writer.println(sb.toString());
			writer.flush();
		}
	}
	
	
	public static void write(Type type, String data) {
		write(null, type, data);
	}
	
	public static void write(Type type, List<String> data) {		
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

	public static void write(Type type, String[] data) {
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
	
	public void writePrivate(Type type, List<String> data) {		
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

	public void writePrivate(Type type, String[] data) {
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
