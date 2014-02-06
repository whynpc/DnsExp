package edu.ucla.cs.wing.dnsexp;

import org.xbill.DNS.Record;

public interface ICommander {
	
	public void onSendDnsQuery(long transacationId, String domainName);
	
	public void onRecvDnsResponse(long transacationId, String domainName, Record[] records);

}
