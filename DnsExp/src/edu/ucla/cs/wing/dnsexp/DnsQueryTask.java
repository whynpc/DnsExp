package edu.ucla.cs.wing.dnsexp;

import java.util.TimerTask;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;



public class DnsQueryTask extends TimerTask {
	
	public static final String DNS_EX_IP_NAME = "ex.whynpc.info";
	
	private String name;
	private int repeat;
	private boolean nounce;
	
	private IExpHandler handler;
	
	public DnsQueryTask(String name, int repeat, boolean nounce, IExpHandler handler) {
		this.name = name;
		this.nounce = nounce;
		this.handler = handler;
	}
	
	@Override
	public void run() {
		try {
			for (int i = 0; i < repeat; i ++) {
				long id = System.currentTimeMillis();
				String domainName = nounce ? id + "." + name : name;
				Lookup lookup = new Lookup(domainName);
				
				if (handler != null) {
					handler.onSendDnsQuery(id, domainName);				
				}
				Record[] records = lookup.run();
				if (handler != null) {
					handler.onRecvDnsResponse(id, domainName, records);
				}
			}			
		} catch (TextParseException e) {
		
		}

	}
	
	public String resolve() {
		String addr = null;
		long id = System.currentTimeMillis();
		String domainName = nounce ? id + "." + name : name;
		try {
			Lookup lookup = new Lookup(domainName);
			Record[] records = lookup.run();
			for (Record record : records) {
				if (record instanceof ARecord) {
					addr = ((ARecord) record).getAddress().getHostAddress();
					break;
				}
			}
		} catch (TextParseException e) {

		
		}
		return addr;
	}

}
