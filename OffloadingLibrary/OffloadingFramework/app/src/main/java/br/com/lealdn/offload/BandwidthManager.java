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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class BandwidthManager {
    private SharedPreferences preferences;
    private float updateConstant = 0.2F;
    private final String upBand = "rtt-upBand";
    private final String downBand = "rtt-downBand";
    public final String BW_MANAGER_KEY = "bandwidthManagerKey";
    private Gson gson = new Gson();

    // TODO: Change to implementation with a Splay tree
    // TODO: Think about some kind of eviction policy
    private Map<String,LocationBasedBandwidth> locationBasedBandwidthList;

    private int usageCount = 0;

    private final static Logger log = Logger.getLogger(BandwidthManager.class);

    public BandwidthManager(final SharedPreferences preferences) {
        log.setLevel(Level.OFF);
        this.preferences = preferences;
        this.initiliaze();
    }

    private void initiliaze() {
        if (locationBasedBandwidthList == null) {
            if ("".equals(this.preferences.getString(BW_MANAGER_KEY, ""))) {
                this.locationBasedBandwidthList = new HashMap<String,LocationBasedBandwidth>();
            }
            else {
                try {
                    Type t = new TypeToken<Set<LocationBasedBandwidth>>() {}.getType();
                    this.locationBasedBandwidthList = gson.fromJson(this.preferences.getString(BW_MANAGER_KEY, ""), t);
                } catch(Exception ex) {
                    log.error("Error while reading from saved executions object. Starting again.");
                    this.locationBasedBandwidthList = new HashMap<String,LocationBasedBandwidth>();
                }
            }
        }
    }

    public void saveLocationBasedBandwidthList() {
        //if (usageCount++ > 300) {
        //    usageCount = 0;
        //    Type t = new TypeToken<Set<LocationBasedBandwidth>>() {}.getType();
        //    final String locationListString = gson.toJson(this.locationBasedBandwidthList, t);

        //    Editor edit = this.preferences.edit();
        //    edit.putString(BW_MANAGER_KEY, locationListString);
        //    edit.commit();
        //}
    }

    public void setUploadBandwidth(final double bandwidth) {
        setBandwidth(upBand, bandwidth);
    }

    public void setDownloadBandwidth(final double bandwidth) {
        setBandwidth(downBand, bandwidth);
    }
    
    public void setBandwidth(final String key, final double value) {
        Editor editor = this.preferences.edit();
      // log.debug("BandwidthValue: key: " + key + " | " + value);
        editor.putFloat(key, (float)value);
        editor.commit();
    }
    
    public Float calculateBandwidth(Float oldBandwidth, Double newBandwidth) {
        if (oldBandwidth == null && newBandwidth == null) {
            return null;
        }
        if (oldBandwidth == null) {
            return newBandwidth.floatValue();
        }
        else if (newBandwidth == null) {
            return oldBandwidth;
        }

        return oldBandwidth * updateConstant + newBandwidth.floatValue() * (1-updateConstant);
    }
    
    public void setLocationBasedBandwidth(double downB, double upB) {
        if (((Double)downB).isNaN() || ((Double)upB).isNaN()) {
          //  log.debug("Double is NaN! Not updating bandwidth");
            return;
        }

        final int networkState = OffloadingManager.getNetworkState();
         Double currentBandwidth = null;
        if(downB == -1){
            log.debug("upB= "+upB);
            currentBandwidth = upB;
        }else{
            log.debug("downB= "+downB);
            currentBandwidth = downB;
        }
        final String ssid = OffloadingManager.getTelephonyUtils().getCurrentSSID();
        LocationBasedBandwidth lbb = null;
        LocationBasedBandwidth lbbCurr = this.locationBasedBandwidthList.get(ssid);

        if (networkState == OffloadingManager.WIFI) {
            if(downB == -1){
                if(lbbCurr == null){
                    lbb = new LocationBasedBandwidth(-1, currentBandwidth, ssid);
                    locationBasedBandwidthList.put(ssid,lbb);
                }else{
                    lbbCurr.setUB(currentBandwidth);
                }
            }else{
                if (lbbCurr == null){
                    lbb = new LocationBasedBandwidth(currentBandwidth, -1, ssid);
                    locationBasedBandwidthList.put(ssid,lbb);
                }else{
                    lbbCurr.setDB(currentBandwidth);
                }
            }
           // log.debug("LocationBasedBandwidth [WIFI] " + bandwidth + " | network: " + networkState + " | ssid: " + ssid);
        }
        else {
            final int lac = OffloadingManager.getTelephonyUtils().getLac();
            final int cid = OffloadingManager.getTelephonyUtils().getCid();
           // log.debug("LocationBasedBandwidth [MOBILE] " + bandwidth + " | network: " + networkState + " | lac/cid: " + lac+"/"+cid);
            final double shannonsBW = currentBandwidth / log2(1 + OffloadingManager.getTelephonyUtils().getSNR());
            if(downB == -1){
                lbb = new LocationBasedBandwidth(-1, shannonsBW, lac+"/"+cid);
            }else{
                lbb = new LocationBasedBandwidth(shannonsBW, -1, lac+"/"+cid);
            }
        }

        //this.saveLocationBasedBandwidthList();

        /*if (isDownload) {
            setDownloadBandwidth(bandwidth);
        }
        else {
            setUploadBandwidth(bandwidth);
        }*/
    }
    
    public double[] getBandwidth() {
        final int networkState = OffloadingManager.getNetworkState();
        //log.debug("getBandwidth: NetworkState: " + networkState);
        double[] bandwidth = new double[2];
        if (networkState == OffloadingManager.WIFI) {
            return getWifiBasedBandwidth();
        }
        else {
            LocationBasedBandwidth lbb = getMobileBasedBandwidth();
            if (lbb == null) {
                bandwidth[0] = 0.0;
                bandwidth[1] = 0.0;
            }
            else {
                // Basically Shannon's equation
                int snr =OffloadingManager.getTelephonyUtils().getSNR();
                bandwidth[0] = lbb.getDownloadB() * log2(1 + snr);
                bandwidth[1] = lbb.getUploadB() * log2(1 + snr);
            }
        }
        return bandwidth;
    }
    
    private double log2(double a) {
        return Math.log(a) / Math.log(2);
    }
    
    public String getNetworkIdentification() {
        if (OffloadingManager.getNetworkState() == OffloadingManager.WIFI) {
            return OffloadingManager.getTelephonyUtils().getCurrentSSID();
        }
        else {
            final int lac = OffloadingManager.getTelephonyUtils().getLac();
            final int cid = OffloadingManager.getTelephonyUtils().getCid();
            return lac+"/"+cid;
        }
    }

    public double getBackupBandwidth(boolean isDownload) {
        double bw = isDownload ? getBackupDownloadBandwidth() : getBackupUploadBandwidth();
        // if 0, try the reverse to see if we got something already
        //if (bw == 0) {
            //return 0;
        //}

        return bw;
    }

    public double[] getWifiBasedBandwidth() {
        long startNetId = System.nanoTime();
        final String ssid = OffloadingManager.getTelephonyUtils().getCurrentSSID();
        long finischNetId = System.nanoTime();
        log.debug("time in netId= "+(finischNetId-startNetId));
        LocationBasedBandwidth locBB= locationBasedBandwidthList.get(ssid);
        double[] result = new double[2];
        if(locBB != null){
            result[0] = locBB.getDownloadB();
            result[1]= locBB.getUploadB();
            log.debug("time bandwidth list NOOOO= "+(System.nanoTime()-finischNetId));
            return result;
        }
        result[0] =0.0;
        result[1]= 0.0;
      //  log.debug("cannot find bandwidth from list. return backup bandwidth");
        return result;
    }
    
    public LocationBasedBandwidth getMobileBasedBandwidth() {
        final String id = getNetworkIdentification();
        LocationBasedBandwidth locBB= locationBasedBandwidthList.get(id);
       if(locBB!= null){
           return locBB;
       }
        return null;
    }
    
    public double getBackupUploadBandwidth() {
        return (double)this.preferences.getFloat(upBand, 0);
    }
    public double getBackupDownloadBandwidth() {
        return (double)this.preferences.getFloat(downBand, 0);
    }
    
    public static class LocationBasedBandwidth {

        public double getDownloadB(){
            return this.downB;
        }
        public double getUploadB(){
            return this.upB;
        }

        public void setDB(double b){
            this.downB = b;
        }

        public void setUB(double b){
            this.upB = b;
        }



        // Constant from Shannon's equation
       // public final double bandwidth;
        private double downB;
        private double upB;
      //  public final int connectionType;
        // LAC/CID for GSM, SSID for WIFI
        public final String id; // ssid or lac/cid
       // public final boolean download;
        
       /* public LocationBasedBandwidth(boolean download, double bandwidth, int connectionType, int lac, int cid) {
            this.bandwidth = bandwidth;
            this.connectionType = connectionType;
            this.identifier = lac+"/"+cid;
            this.download = download;
        }
*/
        public LocationBasedBandwidth(double downB, double upB, String id) {
            this.downB = downB;
            this.upB = upB;
            this.id = id;
        }
        
       /* @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + connectionType;
            result = prime * result + identifier.hashCode();
            result = prime * result + (download ? 1 : 0);
            return result;
        }*/

        /*@Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LocationBasedBandwidth other = (LocationBasedBandwidth) obj;
            if (connectionType != other.connectionType)
                return false;
            if (download != other.download)
                return false;
            if (!identifier.equals(other.identifier))
                return false;
            return true;
        }*/
    }
}
