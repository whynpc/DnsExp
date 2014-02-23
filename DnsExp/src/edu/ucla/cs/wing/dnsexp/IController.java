package edu.ucla.cs.wing.dnsexp;

public interface IController {
	
	public void startAutoTest();
	public void stopAutoTest();
	public void runOnceAutoTest();
	
	public void runAppStoreTest();
	
	public void onAlarm(String task);	
	
	public String getStatus();
	
	public boolean isTraceRunning();
	
	public void startTrace();	
	
	public void stopTrace();
	
	public void onScreen(boolean on);
	
	public void refreshMonitor();

}
