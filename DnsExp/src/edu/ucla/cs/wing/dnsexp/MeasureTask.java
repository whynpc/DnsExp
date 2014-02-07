package edu.ucla.cs.wing.dnsexp;

import java.util.TimerTask;

import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;

public abstract class MeasureTask extends TimerTask{
	
	protected MeasureObject measureObject;
	protected ExpConfig expConfig;
	protected IExpResHandler handler;
	
	
	public MeasureTask(MeasureObject measureObject, ExpConfig expConfig, IExpResHandler handler) {
		this.measureObject = measureObject;
		this.expConfig = expConfig;
		this.handler = handler;
	}
	
	@Override
	public abstract void run();

}