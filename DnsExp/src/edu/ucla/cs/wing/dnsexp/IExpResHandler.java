package edu.ucla.cs.wing.dnsexp;

import org.xbill.DNS.Record;

public interface IExpResHandler {

	public void onSendDnsQuery(long transacationId, String domainName);

	public void onRecvDnsResponse(long transacationId, String domainName,
			Record[] records);

	public void onPing(String domainName, String addrGroupLabel, String addr, double minPingLatency,
			double medianPingLatency, double maxPingLatency,
			double minTrLatency, double medianTrLatency, double maxTrLatency);

	public void onTcp(String domainName, String addrGroupLabel, String addr, long minLatency,
			long medianLatency, long maxLatency);

}
