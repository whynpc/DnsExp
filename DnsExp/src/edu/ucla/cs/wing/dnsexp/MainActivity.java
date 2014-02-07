package edu.ucla.cs.wing.dnsexp;


import edu.ucla.cs.wing.dnsexp.EventLog.Type;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {
	
	private EditText editTextStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		editTextStatus = (EditText) findViewById(R.id.editTextStatus);
		
		startService(new Intent(this, BackgroundService.class));
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
		EventLog.write(Type.DEBUG, MobileInfo.getInstance().getPhoneModel());		
	}
	
	public void onClickGetStatus(View view) {
		editTextStatus.setText(BackgroundService.getController().getStatus());
		
	}
	
	

}
