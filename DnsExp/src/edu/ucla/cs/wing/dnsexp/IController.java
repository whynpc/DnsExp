package edu.ucla.cs.wing.dnsexp;

public interface IController {
	
	public void startAutoTest();
	public void stopAutoTest();
	public void runOnceAutoTest();
	
	public void onAlarm(String task);
	
	public void startMonitorNetstat();
	public void stopMonitorNetstat();
	
	public String getStatus();

}
