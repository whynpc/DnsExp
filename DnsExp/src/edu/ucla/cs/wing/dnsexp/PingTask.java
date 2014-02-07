package edu.ucla.cs.wing.dnsexp;

import java.util.Set;
import java.util.TimerTask;
import java.util.regex.Pattern;

import edu.ucla.cs.wing.dnsexp.ExpConfig.MeasureObject;

public class PingTask extends TimerTask {
	
	private static final Pattern PING_OUTPUT_PATTERN = Pattern.compile("time=([\\d\\.]+)\\s*ms");
	
	private MeasureObject measureObject;
	private int repeat;
	
	public PingTask(MeasureObject measureObject, int repeat) {
		this.measureObject = measureObject;
		this.repeat = repeat;
	}

	@Override
	public void run() {
		
		if (measureObject.isPingable()) {
			
			
			
		}
		
		
		
		/*
		 * String cmd = "ping -c " + runs + " " + addr;
				Log.d("dnstest", cmd);
				Process process;
				try {
					process = Runtime.getRuntime().exec(cmd);
					BufferedReader in = new BufferedReader(new InputStreamReader(
							process.getInputStream()));
					String line;
					while ((line = in.readLine()) != null) {
						Log.d("dnstest", "read line: " + line);
						Matcher matcher = pingLogPattern.matcher(line);
						if (matcher.find()) {
							//Log.d("dnstest", "ping delay: " + matcher.group(1));
							delays.add(Float.parseFloat(matcher.group(1)));							
						} else {
							//Log.d("dnstest", "not match" );
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 * 
		 * */
		

	}

}
