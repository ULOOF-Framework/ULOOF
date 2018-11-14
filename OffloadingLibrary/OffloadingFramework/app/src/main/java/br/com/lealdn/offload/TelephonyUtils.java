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

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

import android.net.wifi.WifiManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

public class TelephonyUtils extends PhoneStateListener {
	final TelephonyManager telephonyManager;
	final WifiManager wifiManager;
	final String LTE_SNR = "getLteRssnr";
	private final static Logger log = Logger.getLogger(TelephonyUtils.class);

	int snr = -1;
	
	public TelephonyUtils(TelephonyManager telephonyManager, WifiManager wifiManager) {
		this.telephonyManager = telephonyManager;
		this.wifiManager = wifiManager;
	}
	
	public int getLac() {
		return ((GsmCellLocation)this.telephonyManager.getCellLocation()).getLac();
	}

	public int getCid() {
		return ((GsmCellLocation)this.telephonyManager.getCellLocation()).getCid() & 0xFFFF;
	}
	
	public String getCurrentSSID() {
		return this.wifiManager.getConnectionInfo().getSSID();
	}
	
	public int getWifiLinkSpeed() {
		return this.wifiManager.getConnectionInfo().getLinkSpeed();
	}
	
	public int getSNR() {
		return this.snr;
	}

	@Override
	public void onSignalStrengthsChanged(SignalStrength signalStrength) {
		super.onSignalStrengthsChanged(signalStrength);
		
		try {
			this.snr = (Integer)signalStrength.getClass().getMethod(LTE_SNR, new Class[] {}).invoke(signalStrength, new Object[]{});
		} catch (Exception e) {
			this.snr = -1;
			log.debug("Error while trying to get LTE_SNR", e);
		}
	}
}
