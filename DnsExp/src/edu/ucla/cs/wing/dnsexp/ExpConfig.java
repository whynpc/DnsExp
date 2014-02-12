package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
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
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import edu.ucla.cs.wing.dnsexp.EventLog.Type;

public class ExpConfig {
	public static final int MODE_QUERY = 1;
	public static final int MODE_PING = 2;
	public static final int MODE_TCP = 4;
	public static final int MODE_APP = 8;

	private int expMode;

	private int queryRepeat;
	private int pingRepeat;
	private float pingInterval;
	private float pingdeadLine;
	private int trRepeat;

	private int tcpRepeat;
	private List<Short> tcpPorts;

	private int appRepeat;

	private String configFile;
	private String appConfigFile;
	private boolean selfUpdating;

	protected long expId;

	private String task;

	private Hashtable<String, MeasureObject> measureObjects;

	private EventLog logger;

	private static ReentrantLock configFileLock = new ReentrantLock();
	private static ReentrantLock appConfigFileLock = new ReentrantLock();

	public ExpConfig(String task) {
		measureObjects = new Hashtable<String, ExpConfig.MeasureObject>();
		tcpPorts = new ArrayList<Short>();
		this.task = task;
	}

	public boolean init(SharedPreferences prefs, Context context) {
		// basic config file
		configFile = prefs.getString("config_file",
				context.getString(R.string.pref_default_config_file));
		appConfigFile = prefs.getString("appconfig_file",
				context.getString(R.string.pref_default_appconfig_file));
		setSelfUpdating(Integer.parseInt(prefs.getString("config_selfupdating",
				context.getString(R.string.pref_default_config_selfupdating))) != 0);

		// exp mode
		setExpMode(Integer.parseInt(prefs.getString("exp_mode",
				context.getString(R.string.pref_default_exp_mode))));

		// set task-specific parameters
		if ((task.equals(MeasureTask.TASK_QUERY) && !toQuery())
				|| (task.equals(MeasureTask.TASK_PING) && !toPing())
				|| (task.equals(MeasureTask.TASK_TCP) && !toTcp())
				|| (task.equals(MeasureTask.TASK_APP) && !toApp())) {
			return false;
		}
		if (task.equals(MeasureTask.TASK_QUERY)) {
			if (!toQuery()) {
				return false;
			}
			setQueryRepeat(Integer.parseInt(prefs.getString("query_repeat",
					context.getString(R.string.pref_default_query_repeat))));
		}

		if (task.equals(MeasureTask.TASK_PING)) {
			if (!toPing()) {
				return false;
			}
			setPingRepeat(Integer.parseInt(prefs.getString("ping_repeat",
					context.getString(R.string.pref_default_ping_repeat))));
			setPingInterval(Float.parseFloat(prefs.getString("ping_interval",
					context.getString(R.string.pref_default_ping_interval))));
			setPingdeadLine(Float.parseFloat(prefs.getString("ping_deadline",
					context.getString(R.string.pref_default_ping_deadline))));
			setTrRepeat(Integer.parseInt(prefs.getString("tr_repeat",
					context.getString(R.string.pref_default_tr_repeat))));

		}

		if (task.equals(MeasureTask.TASK_TCP)) {
			if (!toTcp()) {
				return false;
			}
			setTcpRepeat(Integer.parseInt(prefs.getString("tcp_repeat",
					context.getString(R.string.pref_default_tcp_repeat))));
			String tcpPortsStr = prefs.getString("tcp_ports",
					context.getString(R.string.pref_default_tcp_ports));
			for (String str : tcpPortsStr.split(",")) {
				addTcpPort(Short.parseShort(str));
			}
		}

		if (task.equals(MeasureTask.TASK_APP)) {
			if (!toApp()) {
				return false;
			}
			setAppRepeat(Integer.parseInt(prefs.getString("app_repeat",
					context.getString(R.string.pref_default_app_repeat))));
		}

		// load measureObjects
		if (!load()) {
			return false;
		}

		// create logger
		logger = new EventLog();
		MobileInfo mobileInfo = MobileInfo.getInstance();
		List<String> parameters = new LinkedList<String>();
		parameters.add(task);
		expId = System.currentTimeMillis();
		parameters.add(String.valueOf(expId));
		parameters.add(mobileInfo.getOperatorName());
		parameters.add(mobileInfo.getNetworkTech());
		parameters.add(mobileInfo.getNetworkTypeStr());
		parameters.add(mobileInfo.getPhoneModel());
		logger.open(EventLog.genLogFileName(parameters));

		return true;
	}

	public void prepareExp() {
		if (task.equals(MeasureTask.TASK_APP)) {

		}
	}

	public int getSize() {
		return measureObjects.size();
	}

	public EventLog getLogger() {
		return logger;
	}

	public boolean load() {
		boolean ret = true;

		if (task.equals(MeasureTask.TASK_QUERY)
				|| task.equals(MeasureTask.TASK_PING)
				|| task.equals(MeasureTask.TASK_TCP)) {
			configFileLock.lock();
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(configFile)));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] words = line.split("\t");
					MeasureObject measureObject = new MeasureObject();
					measureObject.setDomainName(words[0]);
					if (words.length > 1) {
						for (String subword : words[1].split(";")) {
							int index = subword.indexOf(':');
							String label = subword.substring(0, index);
							String addrs = subword.substring(index + 1);
							measureObject.addAddrs(label,
									Arrays.asList(addrs.split(",")));
						}
					}
					if (words.length >= 4) {
						measureObject.setPingable(words[2].equals("0") ? false
								: true);
						measureObject.setTcpable(words[3].equals("0") ? false
								: true);
					}
					measureObjects.put(measureObject.getDomainName(),
							measureObject);
				}
				reader.close();
			} catch (Exception e) {
				EventLog.write(Type.DEBUG, e.toString());
				ret = false;
			} finally {
				configFileLock.unlock();
			}
		} else if (task.equals(MeasureTask.TASK_APP)) {
			appConfigFileLock.lock();
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								new FileInputStream(appConfigFile)));
				String line;
				MeasureObject measureObject = null;
				while ((line = reader.readLine()) != null) {
					String[] words = line.split("\t");
					if (words.length == 1) {
						if (measureObject != null) {
							measureObjects.put(measureObject.getDomainName(),
									measureObject);
						}
						measureObject = new MeasureObject();
						measureObject.setDomainName(words[0]);
					} else if (words.length > 1) {
						String label = words[0];
						String addr = words[1];
						measureObject.addAddr(label, addr);
					}
				}
				if (measureObject != null) {
					measureObjects.put(measureObject.getDomainName(),
							measureObject);
				}
				reader.close();
			} catch (Exception e) {
				EventLog.write(Type.DEBUG, e.toString());
				ret = false;
			} finally {
				appConfigFileLock.unlock();
			}

		}

		return ret;
	}

	public void cleanUp() {
		if (selfUpdating) {
			save();
		}

		if (task.equals(MeasureTask.TASK_APP)) {
			try {
				Runtime.getRuntime().exec("su -c killall tcpdump");
			} catch (IOException e) {
			}
		}

		logger.close();
	}

	public boolean save() {
		boolean ret = true;
		if (task.equals(MeasureTask.TASK_QUERY)
				|| task.equals(MeasureTask.TASK_PING)
				|| task.equals(MeasureTask.TASK_TCP)) {
			configFileLock.lock();
			try {
				PrintWriter writer = new PrintWriter(configFile);
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
				ret = false;
			} finally {
				configFileLock.unlock();
			}
		}

		return ret;
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

	public String getTask() {
		return task;
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

	public boolean toApp() {
		return (expMode & MODE_APP) > 0;
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

	public int getAppRepeat() {
		return appRepeat;
	}

	public void setAppRepeat(int appRepeat) {
		this.appRepeat = appRepeat;
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
