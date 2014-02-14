package edu.ucla.cs.wing.dnsexp;

import java.util.TimerTask;

import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;

public abstract class MeasureTask extends TimerTask {
	
	protected MeasureObject measureObject;
	protected ExpConfig expConfig;	
	protected String task;
	
	public static final String TASK = "task";
	public static final String TASK_QUERY = "dns"; 
	public static final String TASK_PING = "ping";
	public static final String TASK_TCP = "tcp";
	public static final String TASK_APP = "app";
	
	public MeasureTask(MeasureObject measureObject, ExpConfig expConfig) {
		this.measureObject = measureObject;
		this.expConfig = expConfig;		
	}
	
	@Override
	public abstract void run();
	
	public String getTask() {
		return task;
	}

}
