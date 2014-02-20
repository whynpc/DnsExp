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
import android.widget.EditText;

public class MainActivity extends Activity {
	
	private EditText editTextStatus;
	
	private static Handler _handler;
	
	public static Handler getHandler() {
		return _handler;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		editTextStatus = (EditText) findViewById(R.id.editTextStatus);
		
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

				default:
					break;
				}
			}
		};
		
		startService(new Intent(this, BackgroundService.class));
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

	
	public void onClickStartMonitorNetstat(View view) {
		BackgroundService.getController().startMonitorNetstat();
	}
	
	public void onClickStopMonitorNetstat(View view) {
		BackgroundService.getController().stopMonitorNetstat();
	}
	
	public void onClickStartAutoTest(View view) {
		BackgroundService.getController().startAutoTest();
	}
	
	
	public void onClickStopAutoTest(View view) {
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
	
	

}
