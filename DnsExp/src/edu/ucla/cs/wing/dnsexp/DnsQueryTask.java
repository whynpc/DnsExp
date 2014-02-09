package edu.ucla.cs.wing.dnsexp;

import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import edu.ucla.cs.wing.dnsexp.EventLog.Type;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;



public class DnsQueryTask extends MeasureTask {
	
	public static final String DNS_EX_IP_NAME = "ex.dnstest. whynpc.info";
	
	public DnsQueryTask(MeasureObject measureObject, ExpConfig expConfig) {
		super(measureObject, expConfig);
	}
	
	private void onSendDnsQuery(long id) {
		expConfig.getLogger().writePrivate(Type.DNSQUERY,
				new String[] { String.valueOf(id), measureObject.getDomainName()});
	}
	
	private void onRecvDnsResponse(long id, Record[] records) {
		List<String> data = new LinkedList<String>();
		data.add(String.valueOf(id));
		data.add(measureObject.getDomainName());
		if (records != null) {
			for (Record record : records) {
				if (record instanceof ARecord) {
					ARecord aRecord = (ARecord) record;
					data.add(aRecord.getAddress().getHostAddress() + ","
							+ aRecord.getTTL());
				}
			}
		}

		expConfig.getLogger().writePrivate(Type.DNSREPONSE, data);
	}
	
	
	@Override
	public void run() {
		try {
			for (int i = 0; i < expConfig.getQueryRepeat(); i ++) {
				
				long id = System.currentTimeMillis();
				
				Lookup lookup = new Lookup(measureObject.getDomainName());
				onSendDnsQuery(id);
				Record[] records = lookup.run();				
				onRecvDnsResponse(id, records);				
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
