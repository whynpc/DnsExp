package edu.ucla.cs.wing.dnsexp;

import org.xbill.DNS.Record;

public interface ICommander {
	
	public void startAutoTest();
	public void stopAutoTest();
	public void runOnceAutoTest();
	
	public void onAlarm();
	
	public void startMonitorNetstat();
	public void stopMonitorNetstat();
	

}
