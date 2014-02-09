package edu.ucla.cs.wing.dnsexp;

import java.util.TimerTask;

import android.R.integer;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;

public abstract class MeasureTask extends TimerTask {
	
	protected MeasureObject measureObject;
	protected ExpConfig expConfig;	
	
	public static final String TASK = "Task";
	public static final String TASK_QUERY = "dns"; 
	public static final String TASK_PING = "ping";
	public static final String TASK_TCP = "tcp";
	
	public MeasureTask(MeasureObject measureObject, ExpConfig expConfig) {
		this.measureObject = measureObject;
		this.expConfig = expConfig;		
	}
	
	@Override
	public abstract void run();

}
