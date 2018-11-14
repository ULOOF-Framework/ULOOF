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

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import br.com.lealdn.offload.utils.Utils;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class RTTService extends Service {
	// constant
    public static final long NOTIFY_INTERVAL = 30 * 1000;
    public static final String RTT_BAND_TX = "rrt-tx";
    public static final String RTT_BAND_RX = "rrt-rx";
 
    SharedPreferences preferences;
    // run on another Thread to avoid crash
    private Handler handler = new Handler();
    // timer handling
    private Timer timer = null;
    final IBinder binder = new LocalBind();
    PackageManager pm;
    
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	@Override
	public void onCreate() {
		Intercept.getRxTxCount();
	 	this.preferences = this.getSharedPreferences("br.com.lealdn.offload.STORAGE", Context.MODE_PRIVATE);
	 	this.pm = this.getPackageManager();
	    this.startTimer();
    }

	@Override
	public void onDestroy() {	
		stopTimer();
	}
	
	public void stopTimer() {
		if (timer != null) {
			timer.cancel();
		}
	}
	
	public void startTimer() {
		if (timer != null) {
			timer.cancel();
		}
		
		timer = new Timer();
        timer.scheduleAtFixedRate(new PerformRTTCheck(), 0, NOTIFY_INTERVAL);
	}
	
	private void performPing() {
		int uid;
		try {
			uid = pm.getApplicationInfo(OffloadingManager.APPLICATION_NAME, 0).uid;
    		final Long rxBytes = TrafficStats.getUidRxBytes(uid);
    		final Long txBytes = TrafficStats.getUidTxBytes(uid);
    		final Long rtt = ConnectionUtils.pingServer();
    		if (rtt != null) {
        		final Long totalRx = TrafficStats.getUidRxBytes(uid) - rxBytes;
        		final Long totalTx = TrafficStats.getUidTxBytes(uid) - txBytes;
    			Editor editor = this.preferences.edit();
    			editor.putLong("rtt", rtt);
    			editor.commit();

    			OffloadingManager.getBandwidthManager().setUploadBandwidth(totalTx / (float)rtt);
    			OffloadingManager.getBandwidthManager().setDownloadBandwidth(totalRx / (float)rtt);
    		}
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private long getRTT() {
		return this.preferences.getLong("rtt", -1);
	}
	
	public class LocalBind extends Binder {
		RTTService getService() {
			return RTTService.this;
		}
	}

	private Double leastCPUUsage() {
		return Utils.getCpuUsage();
	}

	class PerformRTTCheck extends TimerTask {
		@Override
		public void run() {
			if (!OffloadingManager.selfExists()) {
				stopSelf();
				return;
			}

			if (!OffloadingManager.shouldRunRTT()) {
				return;
			}

			RTTService.this.performPing();
			final Double leastCPUUsage = RTTService.this.leastCPUUsage();
			if (leastCPUUsage != null) {
				OffloadingManager.getExecutionManager().updateCPUUsage(leastCPUUsage);
			}

			final String timer = "Timer: " + getRTT();

			// run on another thread
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // display toast
                    Toast.makeText(getApplicationContext(), timer,
                            Toast.LENGTH_SHORT).show();
                }
            });
		}
	}
}
