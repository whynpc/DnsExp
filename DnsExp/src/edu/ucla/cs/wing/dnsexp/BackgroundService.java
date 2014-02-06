package edu.ucla.cs.wing.dnsexp;

import java.util.Timer;

import org.xbill.DNS.Record;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class BackgroundService extends Service implements ICommander{
	
	private static ICommander commander;
	
	public static ICommander getCommander() {
		return commander;
	}

	
	private SharedPreferences prefs;
	
	private MobileInfo mobileInfo;
	
	private Timer logDnsConfigTimer;	
	private Timer taskTimer;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		commander = this;
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		MobileInfo.init(this);
		mobileInfo = MobileInfo.getInstance();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override
	public void onSendDnsQuery(long transacationId, String domainName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRecvDnsResponse(long transacationId, String domainName,
			Record[] records) {
		// TODO Auto-generated method stub
		
	}

}
