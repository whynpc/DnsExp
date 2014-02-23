package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TcpdumpHandler {

	public TcpdumpHandler() {

	}
	

	public void stopTcpdump() {
		try {
			Runtime.getRuntime().exec("su -c killall tcpdump");
		} catch (IOException e) {

		}
	}

	public boolean isRunning() {
		boolean ret = false;
		try {
			String line = null;
			Process process = Runtime.getRuntime().exec("ps");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			while ((line = in.readLine()) != null) {
				if (line.contains("tcpdump")) {
					ret = true;
					break;
				}
			}
			in.close();
		} catch (IOException e) {

		}
		return ret;
	}
	
	public void startTcpdump() {
		if (!isRunning()) {
			try {
				StringBuilder cmd = new StringBuilder();
				cmd.append("su -c tcpdump -i any -s 0 -w ");
				cmd.append("/sdcard/dnsexp/pcap/tr_");
				cmd.append(System.currentTimeMillis());
				cmd.append(".pcap");
				Runtime.getRuntime().exec(cmd.toString());
			} catch (Exception e) {

			}			
		}
	}

}
