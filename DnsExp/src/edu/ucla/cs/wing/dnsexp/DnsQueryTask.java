package edu.ucla.cs.wing.dnsexp;

import java.util.TimerTask;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import edu.ucla.cs.wing.dnsexp.EventLog.Type;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;



public class DnsQueryTask extends MeasureTask {
	
	public static final String DNS_EX_IP_NAME = "ex.dnstest. whynpc.info";
	
	public DnsQueryTask(MeasureObject measureObject, ExpConfig expConfig, IExpResHandler handler) {
		super(measureObject, expConfig, handler);
	}
	
	
	@Override
	public void run() {
		try {
			for (int i = 0; i < expConfig.getQueryRepeat(); i ++) {
				
				long id = System.currentTimeMillis();
				
				Lookup lookup = new Lookup(measureObject.getDomainName());				
				if (handler != null) {
					handler.onSendDnsQuery(id, measureObject.getDomainName());				
				}
				Record[] records = lookup.run();
				if (handler != null) {
					handler.onRecvDnsResponse(id, measureObject.getDomainName(), records);
				}
			}			
		} catch (TextParseException e) {
			EventLog.write(Type.DEBUG, e.toString());
		}
	}
	
	public static String resolve(String name, boolean nounce) {
		String addr = null;
		long id = System.currentTimeMillis();
		String domainName = nounce ? id + "." + name : name;
		try {
			
			Lookup lookup = new Lookup(domainName);
			Record[] records = lookup.run();
			if (records != null) {
				for (Record record : records) {
					if (record instanceof ARecord) {
						addr = ((ARecord) record).getAddress().getHostAddress();
						break;
					}
				}
			}
		} catch (TextParseException e) {		
		}
		return addr;
	}

}
