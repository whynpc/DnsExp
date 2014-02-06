package edu.ucla.cs.wing.dnsexp;

import java.util.TimerTask;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;



public class DnsQueryTask extends TimerTask {
	
	private String name;
	private long id;
	private boolean nounce;
	
	public DnsQueryTask(String name, boolean nounce) {
		this.name = name;
		this.nounce = nounce;
	}
	
	@Override
	public void run() {
		try {
			id = System.currentTimeMillis();
			String domainName = nounce ? id + "." + name : name;
			Lookup lookup = new Lookup(domainName);
			
			BackgroundService.getCommander().onSendDnsQuery(id, domainName);
			Record[] records = lookup.run();
			BackgroundService.getCommander().onRecvDnsResponse(id, domainName, records);
		} catch (TextParseException e) {
		
		}

	}

}
