package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.ucla.cs.wing.dnsexp.EventLog.Type;

public class ExpConfig {
	public static final int MODE_QUERY = 1;
	public static final int MODE_PING = 2;
	public static final int MODE_TCP = 4;

	private int expMode;

	private int queryRepeat;
	private int pingRepeat;
	private float pingInterval;
	private float pingdeadLine;
	private int trRepeat;
	
	private int tcpRepeat;
	private List<Short> tcpPorts;

	private boolean selfUpdating;

	private Hashtable<String, MeasureObject> measureObjects;

	public ExpConfig() {
		measureObjects = new Hashtable<String, ExpConfig.MeasureObject>();
		tcpPorts = new ArrayList<Short>();
	}
	
	public int getSize() {
		return measureObjects.size();
	}

	public synchronized boolean load(String inputFile) {		
		measureObjects.clear();
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(inputFile)));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] words = line.split("\t");
				MeasureObject measureObject = new MeasureObject();
				measureObject.setDomainName(words[0]);
				if (words.length > 1) {
					// "label1:1.2.3.4,2.3.4.5;label2:1.2.3.4,3.4.5.6"

					for (String subword : words[1].split(";")) {
						//EventLog.write(Type.DEBUG, "Pre Processing: " + subword);
						int index = subword.indexOf(':');
						String label = subword.substring(0, index);
						String addrs = subword.substring(index + 1);
						//EventLog.write(Type.DEBUG, "Processing: " + label + ": " + addrs);
						measureObject.addAddrs(label, Arrays.asList(addrs.split(",")));
						//EventLog.write(Type.DEBUG, "Post Processing: " + subword);
					}
				}
				if (words.length >= 4) {
					measureObject.setPingable(words[2].equals("0") ? false
							: true);
					measureObject.setTcpable(words[3].equals("0") ? false
							: true);

				}
				measureObjects
						.put(measureObject.getDomainName(), measureObject);
			}
			reader.close();
		} catch (Exception e) {
			EventLog.write(Type.DEBUG, e.toString());
			return false;
		}
		return true;
	}

	public synchronized boolean save(String outputFile) {
		try {
			PrintWriter writer = new PrintWriter(outputFile);
			for (MeasureObject measureObject : measureObjects.values()) {
				StringBuilder sb = new StringBuilder();
				sb.append(measureObject.domainName);
				sb.append("\t");
				
				for (String label : measureObject.getAddrGroupLabels()) {
					sb.append(label);
					sb.append(':');
					AddrGroup addrGroup = measureObject.getAddrGroup(label);
					for (String addr : addrGroup.getAddrs()) {
						sb.append(addr);
						sb.append(",");
					}
					sb.append(';');
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

	public void setPinable(String domainName, boolean pingable) {
		if (measureObjects.containsKey(domainName)) {
			measureObjects.get(domainName).setPingable(pingable);
		}
	}

	public void setTcpable(String domainName, boolean tcpable) {
		if (measureObjects.containsKey(domainName)) {
			measureObjects.get(domainName).setTcpable(tcpable);
		}
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
	
	public void addTcpPort(short port) {
		tcpPorts.add(port);
	}
	
	public List<Short> getTcpPorts() {
		return tcpPorts;
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
	
	

	public int getTrRepeat() {
		return trRepeat;
	}

	public void setTrRepeat(int trRepeat) {
		this.trRepeat = trRepeat;
	}

	
	public float getPingInterval() {
		return pingInterval;
	}

	public void setPingInterval(float pingInterval) {
		this.pingInterval = pingInterval;
	}

	public float getPingdeadLine() {
		return pingdeadLine;
	}

	public void setPingdeadLine(float pingdeadLine) {
		this.pingdeadLine = pingdeadLine;
	}

	public MeasureObject getMeasureObject(String domainName) {
		if (measureObjects.containsKey(domainName)) {
			return measureObjects.get(domainName);
		} else {
			return null;
		}
	}

	public static class AddrGroup {
		private String label;
		private Set<String> addrs;

		public AddrGroup(String label) {
			this.label = label;
			addrs = new HashSet<String>();
		}

		public String getLabel() {
			return label;
		}
		
		public Set<String> getAddrs() {
			return addrs;
		}

		public void addAddr(String addr) {
			addrs.add(addr);
		}

		public void addAddrs(Collection<String> addrs) {
			this.addrs.addAll(addrs);
		}
		
		public int getSize() {
			return addrs.size();
		}

	}

	public static class MeasureObject {
		private String domainName;

		private List<AddrGroup> addrGroups;

		private boolean pingable;
		private boolean tcpable;

		public MeasureObject() {
			pingable = true;
			tcpable = true;
			addrGroups = new ArrayList<ExpConfig.AddrGroup>();
		}

		public String getDomainName() {
			return domainName;
		}

		public void setDomainName(String domainName) {
			this.domainName = domainName;
		}
		
		public int getSize() {
			int cnt = 0;
			for (AddrGroup addrGroup : addrGroups) {
				cnt += addrGroup.getSize();
			}
			return cnt;
		}
		
		public AddrGroup getAddrGroup(String label) {
			AddrGroup ret = null;
			for (AddrGroup addrGroup : addrGroups) {
				if (addrGroup.getLabel().equals(label)) {
					ret = addrGroup;
					break;
				}
			}
			return ret;
		}

		public List<String> getAddrGroupLabels() {
			List<String> labels = new ArrayList<String>();
			for (AddrGroup group : addrGroups) {
				labels.add(group.getLabel());
			}
			return labels;
		}

		public void addAddr(String label, String addr) {
			AddrGroup dstgroup = null;
			for (AddrGroup group : addrGroups) {
				if (group.getLabel().equals(label)) {
					dstgroup = group;
					break;
				}
			}
			if (dstgroup != null) {
				dstgroup.addAddr(addr);
			} else {
				dstgroup = new AddrGroup(label);
				dstgroup.addAddr(addr);
				addrGroups.add(dstgroup);
			}
		}

		public void addAddrs(String label, Collection<String> addrs) {
			AddrGroup dstgroup = null;
			for (AddrGroup group : addrGroups) {
				if (group.getLabel().equals(label)) {
					dstgroup = group;
					break;
				}
			}
			if (dstgroup != null) {
				dstgroup.addAddrs(addrs);
			} else {
				dstgroup = new AddrGroup(label);
				dstgroup.addAddrs(addrs);
				addrGroups.add(dstgroup);
			}
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
