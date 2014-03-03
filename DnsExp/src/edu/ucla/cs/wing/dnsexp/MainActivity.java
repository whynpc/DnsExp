package edu.ucla.cs.wing.dnsexp;


import edu.ucla.cs.wing.dnsexp.EventLog.LogType;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
	private EditText editTextStatus;
	
	private ToggleButton toggleButtonTrace, toggleButtonPcap;  
	
	private static Handler _handler;
	
	public static Handler getHandler() {
		return _handler;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		editTextStatus = (EditText) findViewById(R.id.editTextStatus);		
		toggleButtonTrace = (ToggleButton) findViewById(R.id.toggleButtonTrace);
		toggleButtonTrace.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					BackgroundService.getController().startTrace();					
				} else {
					BackgroundService.getController().stopTrace();
				}
			}
		});			
		
		toggleButtonPcap = (ToggleButton) findViewById(R.id.toggleButtonPcap);		
		toggleButtonPcap.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				BackgroundService.getController().setEnablePcap(isChecked);				
			}
		});
		
		
		_handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case Msg.DONE:
					editTextStatus.setText("Done");
					break;
				case Msg.ERROR:
					editTextStatus.setText("Error");
					break;
				case Msg.TRACE_STATE:
					Boolean running = (Boolean) msg.obj;
					toggleButtonTrace.setChecked(running);
				case Msg.PCAP_STATE:
					Boolean runningp = (Boolean) msg.obj;
					toggleButtonPcap.setChecked(runningp);
				default:
					break;
				}
			}
		};
		
		startService(new Intent(this, BackgroundService.class));		
	}
	
	private void sendMsgToBg(int what, Object obj) {		
		Handler handler = BackgroundService.getHandler();
		if (handler != null) {
			Message msg = new Message();
			msg.what = what;
			msg.obj = obj;
			handler.sendMessage(msg);
		}		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		sendMsgToBg(Msg.TRACE_STATE, null);		
		sendMsgToBg(Msg.PCAP_STATE, null);
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		_handler = null;
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			this.startActivity(new Intent(MainActivity.this,
					SettingsActivity.class));
			break;

		default:
			break;
		}
		return true;
	}	
	
	public void onClickStartAutoTest(View view) {
		editTextStatus.setText("Running");	
		BackgroundService.getController().startAutoTest();
	}
	
	
	public void onClickStopAutoTest(View view) {
		editTextStatus.setText("Stopped");
		BackgroundService.getController().stopAutoTest();		
	}
	
	public void onClickRunOnceAutoTest(View view) {
		BackgroundService.getController().runOnceAutoTest();
	}
	
	public void onClickDebug(View view) {
		EventLog.write(LogType.DEBUG, MobileInfo.getInstance().getPhoneModel());		
	}
	
	public void onClickGetStatus(View view) {
		editTextStatus.setText(BackgroundService.getController().getStatus());
		
	}
	
	public void onClickAppStoreTest(View view) {
		editTextStatus.setText("Running");
		BackgroundService.getController().runAppStoreTest();
	}
	
	public void onClickRefreshMonitor(View view) {
		BackgroundService.getController().refreshMonitor();
	}

}
