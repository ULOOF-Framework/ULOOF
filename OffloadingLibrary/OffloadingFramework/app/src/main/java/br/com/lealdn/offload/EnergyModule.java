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

import org.apache.log4j.Logger;

import br.com.lealdn.offload.utils.InterpolateResult;

public class EnergyModule {
    private final static Logger log = Logger.getLogger(EnergyModule.class);
    //public static Double calculateEnergyLocal(Double[] interpolateLocal) {
    public static Double calculateEnergyLocal(InterpolateResult local_result, Double bandwidthDown) {
        //final Double bandwidthDown = getBandwidth(true);

        //return energyCPU(interpolateLocal[3], interpolateLocal[0]) + energyRadio(interpolateLocal[1], bandwidthDown);
        Double result = energyCPU(local_result.getCPU_ticks(), local_result.getRunning_time()/1000)+ energyRadio(local_result.getTransfer_bytes(), bandwidthDown);
        local_result.setEnergy(result);
        return result;
    }

    //public static Double calculateEnergyRemote(Double[] interpolateRemote, final int sizeOfSerializedObject) {
    public static Double calculateEnergyRemote(InterpolateResult remote_result, final int sizeOfSerializedObject, Double bandwidthDown, Double bandwidthup) {
       // final Double bandwidthDown = getBandwidth(true);
        //final Double bandwidthup = getBandwidth(false);

        //return energyRadio(sizeOfSerializedObject, bandwidthUp, interpolateRemote[2], bandwidthDown);
        Double result = energyRadio(sizeOfSerializedObject, bandwidthup, remote_result.getResult_size(), bandwidthDown);
        remote_result.setEnergy(result);
        return result;
    }

    private static double energyCPU(final double ticks, final double runtime) {

        double lcpu = lCPU(ticks/runtime/1000D);
        double result = lcpu * (runtime/1000D);
        return result;
        //return lCPU(ticks/runtime) * runtime;
        //return lCPU(ticks);
    }

    private static double energyRadio(final double totalBytesUplink, final double bandwidthUp, final double totalBytesDownLink, final  double bandwidthDown ) {
        return energyRadio(totalBytesUplink, bandwidthUp) + energyRadio(totalBytesDownLink, bandwidthDown);
        //return energyRadio(totalBytesUplink) + energyRadio(totalBytesDownLink);
    }

    //private static double energyRadio(final double totalBytes, final double bandwidth) {
    private static double energyRadio(final double totalBytes, final double bandwidth) {
            //return lRadio(bandwidth*1000D) * (totalBytes / bandwidth);
        if (OffloadingManager.getNetworkState() == OffloadingManager.WIFI) return wifiRadio(bandwidth*1000D) * (totalBytes / bandwidth/1000D);
        else if (OffloadingManager.getNetworkState() == OffloadingManager.MOBILE) return mobileRadio(bandwidth*1000D) * (totalBytes / bandwidth/ 1000D);
        return 0;
    }

    //in milliwatts
    private static Double lCPU(double s) {
        return 51.422D
            + 2.9076D * s 
            + 0.019306D * Math.pow(s, 2)
            + 6.7841D * Math.pow(10, -5) * Math.pow(s, 3)
            - 8.4491D * Math.pow(10, -8) * Math.pow(s, 4);
    }

    /*private static double lRadio(double s) {
        return 158.37D + 9.8423 * Math.pow(10, -8) * s
               - 1.0223D * Math.pow(10, -16) * Math.pow(s, 2)
               + 3.5564D * Math.pow(10, -26) * Math.pow(s, 3)
               + 9.0634D * Math.pow(10, -35) * Math.pow(s, 4);
    }*/

    private static double wifiRadio(double s) {
        return 158.37D + 1.1811 * Math.pow(10, -5) * s
                - 1.4722D * Math.pow(10, -12) * Math.pow(s, 2)
                + 6.1454D * Math.pow(10, -20) * Math.pow(s, 3)
                + 1.8794D * Math.pow(10, -26) * Math.pow(s, 4);
    }

    private static double mobileRadio(double s) {
        return 111.24D - 7.9499 * Math.pow(10, -5) * s
                + 1.5999D * Math.pow(10, -10) * Math.pow(s, 2)
                - 8.3738D * Math.pow(10, -17) * Math.pow(s, 3)
                + 1.3748D * Math.pow(10, -23) * Math.pow(s, 4);
    }
    /*private static double 4GRadio(double s) {
        return 111.24D - 7.9499 * Math.pow(10, -5) * s
                + 1.5999D * Math.pow(10, -10) * Math.pow(s, 2)
                - 8.3738D * Math.pow(10, -17) * Math.pow(s, 3)
                + 1.3748D * Math.pow(10, -23) * Math.pow(s, 4);
    }*/

   /* private static Double getBandwidth(boolean isDownload) {
    	Double bandwidth = OffloadingManager.getBandwidthManager().getBandwidth(isDownload);
    	return bandwidth;
    }*/

}
