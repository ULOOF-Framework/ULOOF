/*
 * Offloading Library -  ULOOF Project
 *
 * Copyright (C) 2017-2018  Stefano Secci <stefano.secci@cnam.fr>
 * Copyright (C) 2017-2018  Alessio Diamanti <alessio.diama@gmail.com>
 * Copyright (C) 2017-2018  José Leal Neto - Federal University of Minas Gerais
 * Copyright (C) 2017-2018  Daniel F. Macedo - Federal University of Minas Gerais
 * Copyright (C) 2017-2018  Alessio Mora	<mora.alessio20@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package br.com.lealdn.offload;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.mindpipe.android.logging.log4j.LogConfigurator;

import br.com.lealdn.offload.RTTService.LocalBind;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import android.os.Process;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

/*
	This class manage the offloading framework.
	The Android Application modified by the post-compiler
	will call the initialize method of this class as soon
	as possible, and this method enables the offloading.
 */

public class OffloadingManager {
	private static OffloadingManager self;
	private final Context mainActivity;
	private RTTService rttService;
	private SharedPreferences preferences;
	private BandwidthManager bandwidthManager;
	private ExecutionManager executionManager;
	private LogManager logManager;
	private ActivityManager activityManager;
	private PackageManager pm;
	private int uid;
	public final static String APPLICATION_NAME = "com.waze";
	private volatile boolean shouldRunRTT = true;
	private ExecutorService executor;
	private LocationManager locationManager;
	private ConnectivityManager connectivityManager;
	private LogConfigurator logConfigurator;
	private TelephonyUtils telephonyUtils;
	private final static Logger log = Logger.getLogger(OffloadingManager.class);
	private final static String SETTINGS_FILENAME = "settings.txt";
	//public static File file;
	//public static PrintWriter writer;
	private KryoPool kyroPool ;
	public static ArrayList<String> unserializable = new ArrayList<String>();



	public final static int WIFI = 0;
	public final static int MOBILE = 1;
	public final static int NONE = 2;
	public static String lastOffladed = "";
	public static int serialzationStops = 0;

	public static boolean enabled = false;

	private OffloadingManager(final Context mainActivity) {
		this.mainActivity = mainActivity;
	}
	
	public static boolean shouldRunRTT() {
		return self.shouldRunRTT;
	}
	
	public static boolean selfExists() {
		return self != null;
	}
	
	public synchronized static void setShouldRunRTT(boolean shouldRunRTT) {
		self.shouldRunRTT = shouldRunRTT;
	}
	
	public static void initialize(final Context mainActivity,String appPackage) {
		//System.out.println("BOOTSTRAP FRAMEWORK");
		if (self == null) {
			AssetManager assets = mainActivity.getAssets();
			InputStream in = null;
			String content = "";
			try{
				in = assets.open("unoffloadable.txt");
				BufferedReader buffRead = new BufferedReader(new InputStreamReader(in));
				content=buffRead.readLine();
				//System.out.println("content= "+content);
				while (content!=null){
				//	System.out.println("content= "+content);
					unserializable.add(content);
					content=buffRead.readLine();
				}
			}catch(java.io.IOException ex){
				ex.printStackTrace();
			}
			//System.out.println("mainActivity.getFilesDir()= "+mainActivity.getFilesDir());
			//file = new File(Environment.getExternalStorageDirectory(),"execTime.txt");
			/*try{
				writer=  new PrintWriter(new BufferedWriter(new FileWriter(OffloadingManager.file)));
			}catch(java.io.IOException ex){
				ex.printStackTrace();
			}*/

			OffloadingManager.setEnabled(true);
			//BasicConfigurator.configure();
			self = new OffloadingManager(mainActivity);
			self.preferences = self.mainActivity.getSharedPreferences("br.com.lealdn.offload.STORAGE", Context.MODE_PRIVATE);
		    self.bandwidthManager = new BandwidthManager(self.preferences);
		    self.executionManager = new ExecutionManager(self.preferences);
		    self.logManager = new LogManager();
		    self.activityManager = (ActivityManager)self.mainActivity.getSystemService(Context.ACTIVITY_SERVICE);
		    self.pm = self.mainActivity.getPackageManager();
		    self.executor = Executors.newFixedThreadPool(50);
		    self.locationManager = (LocationManager)self.mainActivity.getSystemService(Context.LOCATION_SERVICE);
		    self.connectivityManager = (ConnectivityManager)self.mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
		    final TelephonyManager tm = (TelephonyManager)(TelephonyManager)self.mainActivity.getSystemService(Context.TELEPHONY_SERVICE);
		    self.telephonyUtils = new TelephonyUtils(tm, (WifiManager)self.mainActivity.getSystemService(Context.WIFI_SERVICE));
		    tm.listen(self.telephonyUtils, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		    self.initializeLogging();
			KryoFactory factory = new KryoFactory() {
				@Override
				public Kryo create() {
					return new Kryo();
				}
			};
		    self.kyroPool= new KryoPool.Builder(factory).build();
			//ConnectionUtils.setIp("132.227.79.188");
			//ConnectionUtils.setPort(8080);
			setConfigFromConfigFile(getSettingsFile());

		    try {
		    	self.uid = self.pm.getApplicationInfo(appPackage, 0).uid;
		    } catch (NameNotFoundException e) {
		    	log.debug("Error on initialize UID. " + e.getMessage());
                e.printStackTrace();
		    }
		    // UPDATE: I'll not start the ping service anymore
			//startService();
		}
	}
	
	private void initializeLogging() {
		this.logConfigurator = new LogConfigurator();

		this.logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "offloadinglog.txt");
		this.logConfigurator.setRootLevel(Level.DEBUG);
		// Set log level of a specific logger
		this.logConfigurator.setLevel("org.apache", Level.ERROR);
		this.logConfigurator.setMaxBackupSize(100);
		this.logConfigurator.setImmediateFlush(true);
		this.logConfigurator.configure();
	}
	
	public static ExecutorService getExecutor() {
		return self.executor;
	}

	/*public static synchronized KryoPool getKryoPool(){
		return self.kyroPool;
	}*/

	public static synchronized Kryo borrowKryoInstance(){
		return self.kyroPool.borrow();
	}

	public static synchronized void releaseKryoInstyance(Kryo kryo){
		self.kyroPool.release(kryo);
	}
	public static int getUid() {
		return self.uid;
	}

    public static int getPid() {
        return Process.myPid();
    }
	
	public static void onResume() {
		if (self != null && self.rttService != null) {
			self.rttService.startTimer();
		}
	}
	
	public static void onPause() {
		if (self != null && self.rttService != null) {
			self.rttService.stopTimer();
		}
	}
	
	public static void onStop() {
		if (self != null) {
            if (self.rttService != null) {
                self.rttService.stopTimer();
            }
            OffloadingManager.getBandwidthManager().saveLocationBasedBandwidthList();
            OffloadingManager.getExecutionManager().saveExecutionsObject();
        }
    }
	
	public static void startService() {
		if (self.rttService == null) {
			final Intent intent = new Intent(self.mainActivity, RTTService.class);
			self.mainActivity.startService(intent);
	        self.mainActivity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
		}
	}
	
	public static Location getLastKnownLocation() {
		return self.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	}
	
	public static int getNetworkState() {
		final android.net.NetworkInfo wifi = self.connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final android.net.NetworkInfo mobile = self.connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		if (wifi.isConnected()) {
			return WIFI;
		} else if (mobile.isConnected()) {
			return MOBILE;
		} else {
			return NONE;
		}
	}
	
	public static LogManager getLogManager() {
		return self.logManager;
	}
	public static BandwidthManager getBandwidthManager() {
		return self.bandwidthManager;
	}
	public static ExecutionManager getExecutionManager() {
		return self.executionManager;
	}
	
	public static TelephonyUtils getTelephonyUtils() {
		return self.telephonyUtils;
	}
	
	 /** Callbacks for service binding, passed to bindService() */
    private static ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get MyService instance
            LocalBind binder = (LocalBind) service;
            self.rttService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };

    public static void setEnabled(boolean val){
    	OffloadingManager.enabled = val;
	}
	public static boolean getEnabled(){
		return OffloadingManager.enabled;
	}
	public static File getSettingsFile() {
		// Get the directory for the user's public downloads directory.
		File file = new File(Environment.getExternalStorageDirectory() + File.separator + SETTINGS_FILENAME);
		if (!file.exists()) {
			System.out.println("The settings file does not exist");
		}
		return file;
	}

	public static void setConfigFromConfigFile(File file){
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		ArrayList<String> readStr = new ArrayList<>();
		String line;
		try {
			while ((line = br.readLine()) != null) {
				readStr.add(line);
			}
			br.close();
		}catch (java.io.IOException ex){
			ex.printStackTrace();
		}
		ConnectionUtils.setIp(readStr.get(0));
		ConnectionUtils.setPort(Integer.parseInt(readStr.get(1)));

    	/*
		//log.debug("setConfigFromFile starts!");
		Scanner s = null;
		try{
			s = new Scanner(file);
			s.useDelimiter("\n");
		}catch(java.io.FileNotFoundException e){
			e.printStackTrace();
		}
		ArrayList<String> readStr = new ArrayList<>();
		while(s.hasNext()){
			String str = s.next();
			//System.out.println("ConfigFileStr= "+str);
			readStr.add(str);
		}
		s.close();
		ConnectionUtils.setIp(readStr.get(0));
		ConnectionUtils.setPort(Integer.parseInt(readStr.get(1)));
*/
	}
}
