package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;
import java.util.regex.Matcher;

import edu.ucla.cs.wing.dnsexp.ExpConfig.AddrGroup;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;
import android.R.bool;
import android.util.Log;

public class TcpTask extends MeasureTask {

	public TcpTask(MeasureObject measureObject, ExpConfig expConfig,
			IExpResHandler handler) {
		super(measureObject, expConfig, handler);
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
							boolean success = true;
							try {
								t1 = System.currentTimeMillis();
								socket.connect(new InetSocketAddress(addr, port));
								t2 = System.currentTimeMillis();
							} catch (IOException e) {
								success = false;
							}
							if (success) {
								latencies.add(t2 - t1);
							}
						}

						if (latencies.size() > 0) {
							anyOpenPort = true;

							Collections.sort(latencies);
							handler.onTcp(measureObject.getDomainName(), label,
									addr, latencies.get(0),
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
