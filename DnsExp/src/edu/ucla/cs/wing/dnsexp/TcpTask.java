package edu.ucla.cs.wing.dnsexp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import edu.ucla.cs.wing.dnsexp.EventLog.LogType;
import edu.ucla.cs.wing.dnsexp.ExpConfig.AddrGroup;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;

public class TcpTask extends MeasureTask {

	public TcpTask(MeasureObject measureObject, ExpConfig expConfig) {
		super(measureObject, expConfig);
		
		task = TASK_PING;
	}

	private void onTcp(String label, String addr, double minLatency,
			double medLatency, double maxLatency) {
		List<String> data = new LinkedList<String>();
		data.add(measureObject.getDomainName());
		data.add(label);
		data.add(addr);
		data.add(String.valueOf(minLatency));
		data.add(String.valueOf(medLatency));
		data.add(String.valueOf(maxLatency));
		
		expConfig.getLogger().writePrivate(LogType.TCP, data);
	}

	@Override
	public void run() {
		List<Short> tcpPorts = expConfig.getTcpPorts();

		if (tcpPorts.size() > 0) {
			int groupFailCnt = 0;
			for (String label : measureObject.getAddrGroupLabels()) {
				AddrGroup addrGroup = measureObject.getAddrGroup(label);
				int failCnt = 0;
				for (String addr : addrGroup.getAddrs()) {
					boolean anyOpenPort = false;

					for (short port : tcpPorts) {
						List<Long> latencies = new ArrayList<Long>();

						for (int i = 0; i < expConfig.getTcpRepeat(); i++) {
							Socket socket = new Socket();
							long t1 = 0, t2 = 0;
							try {
								t1 = System.currentTimeMillis();
								socket.connect(new InetSocketAddress(addr, port));
								t2 = System.currentTimeMillis();
								socket.close();
							} catch (IOException e) {
							}

							if (t2 != 0) {
								latencies.add(t2 - t1);
							}
						}

						if (latencies.size() > 0) {
							anyOpenPort = true;
							Collections.sort(latencies);
							onTcp(label, addr, latencies.get(0),
									latencies.get(latencies.size() / 2),
									latencies.get(latencies.size() - 1));
						}
					}

					if (!anyOpenPort) {
						failCnt++;
					}
				}
				if (failCnt > 0 && failCnt == addrGroup.getAddrs().size()) {
					groupFailCnt++;
				}
			}

			if (groupFailCnt > 0
					&& groupFailCnt == measureObject.getAddrGroupLabels()
							.size()) {
				measureObject.setTcpable(false);
			}
		}
	}

}
