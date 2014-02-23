package edu.ucla.cs.wing.dnsexp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class MobileInfo {

	private int networkType = 0;
	private int signalStrengthDBM = -113;
	private int wifiSignalStrength = -80;
	private int cellId = 0; /* Cell Id */
	private int lac = 0; /* Location Area */
	private int psc = 0; /* Scrambling code */
	private float speed = 0;
	private String geoLat = ""; /* ex: 41.11665289 */
	private String geoLong = ""; /* ex: -73.71944653 */
	private String networkTech = ""; /* ex: GSM or CDMA */
	private String neighborStatus = "";
	private String networkTypeStr = ""; /* ex: GPRS, UMTS, HSPA, .. */
	private TelephonyManager telMgr;
	private ConnectivityManager connectivityManager;
	private WifiManager wifiManager;
	private MobilePhoneStateListener mListener;
	private LocationManager mlocManager;
	private LocationListener mlocListener;
	private Context mContext;
	private int callState = 0;
	private String ip;
	private boolean connected = true;

	// private Timer statsPullTimer = new Timer();

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	private Method dataMtd = null;

	private static MobileInfo instance;

	public static void init(Context context) {
		instance = new MobileInfo(context);

	}

	public static MobileInfo getInstance() {
		return instance;
	}

	public MobileInfo(Context context) {		
		mContext = context;
		mListener = new MobilePhoneStateListener();
		telMgr = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		telMgr.listen(mListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		telMgr.listen(mListener, PhoneStateListener.LISTEN_CELL_LOCATION);		
		telMgr.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);

		connectivityManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			dataMtd = ConnectivityManager.class.getDeclaredMethod(
					"setMobileDataEnabled", boolean.class);
		} catch (NoSuchMethodException e) {

		}

		wifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);		

		mContext.registerReceiver(new BroadcastReceiver() {			
			@Override
			public void onReceive(Context context, Intent intent) {
				WifiInfo info = wifiManager.getConnectionInfo();
				wifiSignalStrength = info.getRssi();				
			}
		}, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
	}

	public void setMobileDataEnabled(boolean enabled) {
		try {
			dataMtd.invoke(connectivityManager, enabled);
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
	}
	
	public int getWifiSignalStrength() {
		return wifiSignalStrength;
	}

	/* Retrieve Phone Model */
	public String getPhoneModel() {
		return Build.MODEL;
	}

	/* Retrieve Operator Name */
	public String getOperatorName() {
		return telMgr.getNetworkOperatorName();
	}

	public String getDeviceId() {
		return telMgr.getDeviceId();
	}

	/* Retrieve Network Type */
	public int getNetworkType() {
		networkType = telMgr.getNetworkType();
		return networkType;
	}

	/* Retrieve Signal Strength */
	public int getSignalStrengthDBM() {
		return signalStrengthDBM;
	}

	/* Retrieve Cell id */
	public int getCellId() {
		return cellId;
	}

	/* Retrieve Location Area Id */
	public int getLac() {
		return lac;
	}

	/* Retrieve primary scrambling code */
	public int getPsc() {
		return psc;
	}

	/* Retrieve current mobility speed */
	public float getSpeed() {
		return speed;
	}

	/* Retrieve the latitude of current location */
	public String getGeoLat() {
		return geoLat;
	}

	/* Retrieve the longitude of current location */
	public String getGeoLong() {
		return geoLong;
	}

	/* Retrieve the networks technologies, GSM or CDMA */
	public String getNetworkTech() {
		NetworkInfo cInfo = connectivityManager.getActiveNetworkInfo();
		if (cInfo == null || !cInfo.isConnectedOrConnecting()) {
			return "NULL";
		} else if (cInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			return "WIFI";
		}
		return networkTech;
	}
	
	public String getDnsServer(int num) {
		String server = null;		
		try {
			Process process = Runtime.getRuntime().exec("getprop net.dns" + num);				
			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			server = in.readLine();
			in.close();
		} catch (IOException e) {
		}		
		return server;					
	}

	/* Retrieve the network type, GPRS, UMTS, LTE, HSPA */
	public String getNetworkTypeStr() {
		return networkTypeStr;
	}

	/* Retrieve the neighboring information */
	public String getNeighborStatus() {
		return neighborStatus;
	}

	/* Get total byte of sent via mobile networks */
	public long getMobileTxByte() {
		return TrafficStats.getMobileTxBytes();
	}

	/* Get total byte of received via mobile networks */
	public long getMobileRxByte() {
		return TrafficStats.getMobileRxBytes();
	}

	/* Get total number of packets sent via mobile networks */
	public long getMobileTxPacketNum() {
		return TrafficStats.getMobileTxPackets();
	}

	/* Get total number of packets received via mobile networks */
	public long getMobileRxPacketNum() {
		return TrafficStats.getMobileRxPackets();
	}

	/*
	 * Get total byte of sent via all interfaces including mobile networks and
	 * wifi networks.
	 */
	public long getTotalTxByte() {
		return TrafficStats.getTotalTxBytes();
	}

	/*
	 * Get total byte of received via all interfaces including mobile networks
	 * and wifi networks.
	 */
	public long getTotalRxByte() {
		return TrafficStats.getTotalRxBytes();
	}

	/*
	 * Get total number of packets sent all interfaces including mobile networks
	 * and wifi networks.
	 */
	public long getTotalTxPacketNum() {
		return TrafficStats.getTotalTxPackets();
	}

	/*
	 * Get total number of packets received all interfaces including mobile
	 * networks and wifi networks.
	 */
	public long getTotalRxPacketNum() {
		return TrafficStats.getTotalRxPackets();
	}

	/*
	 * Get total byte of data sent by particular user id via all interfaces
	 * including mobile networks and wifi networks.
	 */
	public long getUidTxBytes(int uid) {
		return TrafficStats.getUidTxBytes(uid);
	}

	/*
	 * Get total byte of data received by particular user id via all interfaces
	 * including mobile networks and wifi networks.
	 */
	public long getUidRxBytes(int uid) {
		return TrafficStats.getUidRxBytes(uid);
	}

	public void setNetworkType(int networkType) {
		this.networkType = networkType;
	}

	public int getCallState() {
		return this.callState;
	}

	public String getNetworkType(int networkType) {

		switch (networkType) {
		case 7:
			return "1xRTT";
		case 4:
			return "CDMA";
		case 2:
			return "EDGE";
		case 14:
			return "eHRPD";
		case 5:
			return "EVDO rev. 0";
		case 6:
			return "EVDO rev. A";
		case 12:
			return "EVDO rev. B";
		case 1:
			return "GPRS";
		case 8:
			return "HSDPA";
		case 10:
			return "HSPA";
		case 15:
			return "HSPA+";
		case 9:
			return "HSUPA";
		case 11:
			return "iDen";
		case 13:
			return "LTE";
		case 3:
			return "UMTS";
		case 0:
			return "Unknown";
		default:
			return "" + networkType;
		}
	}

	/* Telephony Service */
	public class MobilePhoneStateListener extends PhoneStateListener {
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			int dbm;
			if (signalStrength.isGsm() == true) {
				dbm = signalStrength.getGsmSignalStrength() * 2 - 113;
			} else {
				dbm = signalStrength.getCdmaDbm();
			}
			signalStrengthDBM = dbm;
		}

		@Override
		public void onDataConnectionStateChanged(int state, int networkType) {
			setNetworkType(networkType);
		}

		@Override
		public void onCellLocationChanged(CellLocation location) {
			int newCellId = 0, newLAC = 0, newPSC = 0;

			networkTypeStr = getNetworkType(telMgr.getNetworkType());

			if (location instanceof GsmCellLocation) {
				GsmCellLocation gsmCellLocation = (GsmCellLocation) location;
				newCellId = gsmCellLocation.getCid();
				newLAC = gsmCellLocation.getLac();
				networkTech = "GSM";
			} else {
				CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) location;
				newCellId = cdmaCellLocation.getBaseStationId();
				newLAC = cdmaCellLocation.getNetworkId();
				networkTech = "CDMA";
			}
			if (newCellId != cellId || newLAC != lac || newPSC != psc) {
				cellId = newCellId;
				lac = newLAC;
				psc = newPSC;
			}
		}

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			callState = state;
		}

	};/* End of private Class */

	public class MobileLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location loc) {
			geoLat = "" + loc.getLatitude();
			geoLong = "" + loc.getLongitude();
			speed = loc.getSpeed();
		}

		@Override
		public void onProviderDisabled(String provider) {

		}

		@Override
		public void onProviderEnabled(String provider) {

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}
	}/* End of Class MyLocationListener */

	public String getLocalIpAddress() {
		updateIpAddress();
		return ip;
	}

	public void updateIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& !inetAddress.isSiteLocalAddress()) {
						ip = inetAddress.getHostAddress();
					}
				}
			}

		} catch (Exception e) {

		}

	}

	public String getNeighboringCells() {
		String result = "";
		List<NeighboringCellInfo> neighboringCells = telMgr
				.getNeighboringCellInfo();
		for (int i = 0; i < neighboringCells.size(); i++) {
			NeighboringCellInfo a = neighboringCells.get(i);
			int signalStrength = 0;
			// if (a.getNetworkType() == 1 || a.getNetworkType() == 2) {
			/* GPRS or EDGE */
			signalStrength = a.getRssi() * 2 - 113;
			// } else {
			// signalStrength = a.getRssi();
			// }
			result += getNetworkType(neighboringCells.get(i).getNetworkType())
					+ ";" + neighboringCells.get(i).getLac() + ":"
					+ neighboringCells.get(i).getCid() + ":"
					+ neighboringCells.get(i).getPsc() + ":" + signalStrength
					+ ":";
		}
		return result;
	}
}
