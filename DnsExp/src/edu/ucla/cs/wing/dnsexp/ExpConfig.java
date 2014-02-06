package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ExpConfig {
	public static final int MODE_QUERY = 1;
	public static final int MODE_PING = 2;
	public static final int MODE_TCP = 4;

	private int expMode;

	private int queryRepeat;
	private int pingRepeat;
	private int tcpRepeat;
	
	private boolean selfUpdating;

	private Hashtable<String, MeasureObject> measureObjects;

	public ExpConfig() {
		measureObjects = new Hashtable<String, ExpConfig.MeasureObject>();
	}

	public boolean load(String inputFile) {		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(inputFile)));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] words = line.split("\t");
				MeasureObject measureObject = new MeasureObject();
				measureObject.setDomainName(words[0]);
				if (words.length > 1) {
					for (String subword : words[1].split(",")) {
						measureObject.addAddr(subword);
					}
				}
				if (words.length >= 4) {
					measureObject.setPingable(words[3].equals("0") ? false : true);
					measureObject.setTcpable(words[4].equals("0") ? false : true);
				}
				measureObjects.put(measureObject.getDomainName(), measureObject);
			}			
			reader.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean save(String outputFile) {
		try {
			PrintWriter writer = new PrintWriter(outputFile);
			for (MeasureObject measureObject : measureObjects.values()) {
				StringBuilder sb = new StringBuilder();
				sb.append(measureObject.domainName);
				sb.append("\t");
				for (String addr : measureObject.getAddrs()) {
					sb.append(addr);
					sb.append(",");
				}
				sb.append("\t");
				sb.append(measureObject.isPingable() ? 1 : 0);
				sb.append("\t");
				sb.append(measureObject.isTcpable() ? 1 : 0);
				
				writer.println(sb.toString());
			}
			
			writer.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public Set<String> getDomainNames() {
		return (measureObjects != null ? measureObjects.keySet() : null);
	}
	
	public boolean toQuery() {
		return (expMode & MODE_QUERY) > 0;
	}
	
	public boolean toPing() {
		return (expMode & MODE_PING) > 0;
	}
	
	public boolean toTcp() {
		return (expMode & MODE_TCP) > 0;
	}

	public int getQueryRepeat() {
		return queryRepeat;
	}

	public void setQueryRepeat(int queryRepeat) {
		this.queryRepeat = queryRepeat;
	}

	public int getPingRepeat() {
		return pingRepeat;
	}

	public void setPingRepeat(int pingRepeat) {
		this.pingRepeat = pingRepeat;
	}

	public int getTcpRepeat() {
		return tcpRepeat;
	}

	public void setTcpRepeat(int tcpRepeat) {
		this.tcpRepeat = tcpRepeat;
	}

	public int getExpMode() {
		return expMode;
	}

	public void setExpMode(int expMode) {
		this.expMode = expMode;
	}

	public boolean isSelfUpdating() {
		return selfUpdating;
	}

	public void setSelfUpdating(boolean selfUpdating) {
		this.selfUpdating = selfUpdating;
	}

	public static class MeasureObject {
		private String domainName;

		private List<String> addrs;
		private boolean pingable;
		private boolean tcpable;

		public MeasureObject() {
			addrs = new LinkedList<String>();
			pingable = true;
			tcpable = true;
		}

		public String getDomainName() {
			return domainName;
		}

		public void setDomainName(String domainName) {
			this.domainName = domainName;
		}

		public List<String> getAddrs() {
			return addrs;
		}
		
		public void addAddr(String addr) {
			addrs.add(addr);
		}		

		public boolean isPingable() {
			return pingable;
		}

		public void setPingable(boolean pingable) {
			this.pingable = pingable;
		}

		public boolean isTcpable() {
			return tcpable;
		}

		public void setTcpable(boolean tcpable) {
			this.tcpable = tcpable;
		}

	}

}
