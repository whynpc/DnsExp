package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.R.bool;
import android.R.integer;
import android.os.Debug;
import android.util.Log;
import edu.ucla.cs.wing.dnsexp.EventLog.Type;
import edu.ucla.cs.wing.dnsexp.ExpConfig.AddrGroup;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;

public class PingTask extends MeasureTask {

	private static final Pattern PING_OUTPUT_PATTERN = Pattern
			.compile("time=([\\d\\.]+)\\s*ms");
	private static final Pattern TR_OUTPUT_PATTERN = Pattern
			.compile("([\\d\\.]+)\\s*ms");

	public PingTask(MeasureObject measureObject, ExpConfig expConfig) {
		super(measureObject, expConfig);
		this.measureObject = measureObject;		
	}

	public static class PingLatency {
		private List<Double> pingLatencies;
		private List<Double> trLatencies;

		private double minPingLatency, maxPingLatency, medianPingLatency;
		private double minTrLatency, maxTrLatency, medianTrLatency;

		public double getMinPingLatency() {
			return minPingLatency;
		}

		public double getMaxPingLatency() {
			return maxPingLatency;
		}

		public double getMedianPingLatency() {
			return medianPingLatency;
		}

		public double getMinTrLatency() {
			return minTrLatency;
		}

		public double getMaxTrLatency() {
			return maxTrLatency;
		}

		public double getMedianTrLatency() {
			return medianTrLatency;
		}

		public PingLatency() {
			pingLatencies = new ArrayList<Double>();
			trLatencies = new ArrayList<Double>();
		}

		public boolean isEmptyPingRes() {
			return (pingLatencies.size() == 0);
		}

		public boolean isEmptyTrRes() {
			return (trLatencies.size() == 0);
		}

		public void addPingRes(double val) {
			pingLatencies.add(val);
		}

		public void addTrRes(double val) {
			trLatencies.add(val);
		}

		public void postProcess() {
			if (!isEmptyPingRes()) {
				Collections.sort(pingLatencies);
				minPingLatency = pingLatencies.get(0);
				medianPingLatency = pingLatencies.get(pingLatencies.size() / 2);
				maxPingLatency = pingLatencies.get(pingLatencies.size() - 1);
			}
			if (!isEmptyTrRes()) {
				Collections.sort(trLatencies);
				minTrLatency = trLatencies.get(0);
				medianTrLatency = trLatencies.get(trLatencies.size() / 2);
				maxTrLatency = trLatencies.get(trLatencies.size() - 1);
			}
		}

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
		expConfig.getLogger().writePrivate(Type.PING, data);
	}

	@Override
	public void run() {
		if (measureObject.isPingable()) {
			int groupFailCnt = 0;
			for (String label : measureObject.getAddrGroupLabels()) {
				AddrGroup addrGroup = measureObject.getAddrGroup(label);
				int failCnt = 0;
				for (String addr : addrGroup.getAddrs()) {
					String cmd = String.format("ping -c %d -i %f -w %f %s",
							expConfig.getPingRepeat(),
							expConfig.getPingInterval(),
							expConfig.getPingdeadLine(), addr);

					PingLatency pingLatency = new PingLatency();

					Process process;
					try {
						process = Runtime.getRuntime().exec(cmd);
						BufferedReader in = new BufferedReader(
								new InputStreamReader(process.getInputStream()));
						String line;
						while ((line = in.readLine()) != null) {
							Matcher matcher = PING_OUTPUT_PATTERN.matcher(line);
							if (matcher.find()) {
								pingLatency.addPingRes(Double
										.parseDouble(matcher.group(1)));
							}
						}

						int maxTrHop = 1;
						cmd = String.format("su -c traceroute -m %d %s", maxTrHop, addr);
						for (int i =0; i < expConfig.getTrRepeat(); i ++) {
							process = Runtime.getRuntime().exec(cmd);
							in = new BufferedReader(new InputStreamReader(
									process.getInputStream()));
							while ((line = in.readLine()) != null) {
								Matcher matcher = TR_OUTPUT_PATTERN.matcher(line);
								while (matcher.find()) {
									pingLatency.addTrRes(Double.parseDouble(matcher
											.group(1)));
								}
							}							
						}
					} catch (IOException e) {

					}

					pingLatency.postProcess();					
					onPing(label, addr, pingLatency);					

					if (pingLatency.isEmptyPingRes()) {
						failCnt++;
					}
				}

				if (failCnt == addrGroup.getAddrs().size() && failCnt > 0) {
					groupFailCnt++;
				}
			}

			if (groupFailCnt == measureObject.getAddrGroupLabels().size()
					&& groupFailCnt > 0) {
				measureObject.setPingable(false);
			}
		}

	}

}
