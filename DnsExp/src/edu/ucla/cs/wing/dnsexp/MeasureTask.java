package edu.ucla.cs.wing.dnsexp;

import java.util.TimerTask;

import android.R.integer;
import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;

public abstract class MeasureTask extends TimerTask {
	
	protected MeasureObject measureObject;
	protected ExpConfig expConfig;
	protected IExpResHandler handler;
	
	public static final String TASK = "Task";
	public static final String TASK_QUERY = "Query"; 
	public static final String TASK_PING = "Ping";
	public static final String TASK_TCP = "Tcp";
	
	public MeasureTask(MeasureObject measureObject, ExpConfig expConfig, IExpResHandler handler) {
		this.measureObject = measureObject;
		this.expConfig = expConfig;
		this.handler = handler;
	}
	
	@Override
	public abstract void run();

}
