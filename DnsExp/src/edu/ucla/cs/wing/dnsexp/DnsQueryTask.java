package edu.ucla.cs.wing.dnsexp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import edu.ucla.cs.wing.dnsexp.EventLog.LogType;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;

public class DnsQueryTask extends MeasureTask {

	public static final String DNS_EX_IP_NAME = "ex.dnstest.whynpc.info";

	public DnsQueryTask(MeasureObject measureObject, ExpConfig expConfig) {
		super(measureObject, expConfig);

		task = TASK_QUERY;
	}

	private void onSendDnsQuery(long id) {
		expConfig.getLogger().writePrivate(
				LogType.DNSQUERY,
				new String[] { String.valueOf(id),
						measureObject.getDomainName() });
	}

	private void onRecvDnsResponse(long id, Record[] records,
			List<String> cnames) {
		List<String> data = new LinkedList<String>();

		data.add(String.valueOf(id));
		data.add(measureObject.getDomainName());
		// EventLog.write(LogType.DEBUG, "# of records: " + records.length);
		if (records != null) {
			for (Record record : records) {
				if (record instanceof ARecord) {
					ARecord aRecord = (ARecord) record;
					data.add(aRecord.getAddress().getHostAddress() + ","
							+ aRecord.getTTL());
				}
			}
		} else {
			data.add("");
		}
		// cnames
		if (cnames.size() > 0) {
			StringBuilder builder = new StringBuilder();
			boolean first = true;
			for (String name : cnames) {
				if (first) {
					first = false;
				} else {
					builder.append(",");
				}
				builder.append(name);
			}
			data.add(builder.toString());			
		} else {
			data.add("");
		}
		

		expConfig.getLogger().writePrivate(LogType.DNSREPONSE, data);
	}

	public void searchCache(Cache cache, Name name, List<String> cnames) {
		SetResponse rep = cache.lookupRecords(name.canonicalize(), Type.CNAME,
				0);
		if (rep.isSuccessful()) {
			RRset[] rsets = rep.answers();
			for (RRset rset : rsets) {
				Iterator<Record> r = rset.rrs();
				while (r.hasNext()) {
					Record record = r.next();
					CNAMERecord crecord = (CNAMERecord) record;
					cnames.add(crecord.getAlias().toString(true));
					searchCache(cache, crecord.getTarget(), cnames);
				}
			}
		} else {
			// EventLog.write(LogType.DEBUG, "lookup failed");
		}
	}

	@Override
	public void run() {
		Cache cache = new Cache();
		for (int i = 0; i < expConfig.getQueryRepeat(); i++) {

			long id = System.currentTimeMillis();

			Lookup lookup;
			try {
				lookup = new Lookup(measureObject.getDomainName());
				lookup.setCache(cache);
				onSendDnsQuery(id);
				Record[] records = lookup.run();

				List<String> cnames = new ArrayList<String>();
				searchCache(cache, new Name(measureObject.getDomainName()
						.endsWith(".") ? measureObject.getDomainName()
						: measureObject.getDomainName() + "."), cnames);
				onRecvDnsResponse(id, records, cnames);
			} catch (TextParseException e) {
			}
			cache.clearCache();
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
