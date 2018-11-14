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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import br.com.lealdn.offload.utils.Utils;
import br.com.lealdn.offload.utils.InterpolateResult;


/*
    This class represents the engine of the offloading framework.
    If the offloading is enabled, the DecisionEngine performs firstly
    a serialization check and then it computes a cost function to
    understand whether or not it is convenient to offload the method.
 */

public class DecisionEngine implements Runnable {

    /* Static variables */

    private static double ALPHA = 0.8;
    private final static Logger log = Logger.getLogger(DecisionEngine.class);
    private static HashMap<String,MethodStats>  executedMethod = new HashMap<>();

    /* Instance variables */

    private Kryo kryo = null;
    private String methodSignature;
    private Map<Object, Object> args;
    private boolean answer;

    public DecisionEngine(final String methodSignature, final Map<Object, Object> args){
        this.kryo = OffloadingManager.borrowKryoInstance();
        this.methodSignature = methodSignature;
        this.args = args;
    }

    /* ShouldOffload logic here */

    @Override
    public void run() {
        this.answer=true;

        log.debug("methodSignature= "+methodSignature);

        /* Should never offload if we have no internet */

        if (OffloadingManager.getNetworkState() == OffloadingManager.NONE) {
            log.debug("manageInterception: no network, halting. Res: false");
            //log.debug("Shouldoffload takes = "+ (System.nanoTime()-startShould));
            this.answer = false;
            OffloadingManager.releaseKryoInstyance(this.kryo);
            return;
        }

        double startShould = System.nanoTime();

        /* If the offloading is not enabled always execute the method locally */

        if(!OffloadingManager.getEnabled()){
            log.debug("manageInterception: offloading still not enabled. Res: false");
            this.answer = false;
            OffloadingManager.releaseKryoInstyance(this.kryo);
            return;
        }


        /* TODO: add a list of methods that should be always stopped because "immutable" variables are un-serializable (if this is possible after static analysis)*/

        /* Serialization check */
        Method method = null;
        Object[] argArray = null;
        Class<?> clazz = null;
        long startSerialization = 0;
        long endSerialization =0;
        ByteArrayOutputStream serObj = null;

        try {
            clazz = Utils.getClassFromSignature(this.methodSignature);
            method= Utils.getMethodFromSignature(clazz, this.methodSignature);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        //log.debug("Decision Engine== Clazz: " + clazz);
        //log.debug("Decision Engine== Method: " + method);
        //argArray = getArgsAsArray(args);

        //  final long startSerialization = System.nanoTime();
        startSerialization = System.nanoTime();
        serObj = this.serialize(methodSignature, args);
        endSerialization = System.nanoTime();
        if(serObj == null){
            //  metSerStop.add(methodSignature);
            OffloadingManager.serialzationStops++;
            this.answer = false;
            log.debug("manageInterception: not serializable. Res: false");
            OffloadingManager.releaseKryoInstyance(this.kryo);
            return;
        }
        log.debug("----manageInterception: OK serializable. Res: TRUE");
        this.answer= true;

     /*   MethodStats current;
        if(!this.isMethodPresent(methodSignature)) {
            current = new MethodStats(methodSignature, 0, 0, 0, 0);
            this.addExecMethod(methodSignature, current);
        }else{
            current = this.getExecMethod(methodSignature);
        }
        if(current.getExecutionTimesRemote()<=1){//firts two execution always remote
            // System.out.println("First or second remote exec, returning true");
           // Double elspsedTime = (System.nanoTime()-startShould);
          //  log.debug("Shouldoffload ok takes = "+ elspsedTime);
           // shouldOffloadTimes++;
          //  shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
          //  log.debug("shouldOffloadTime = "+ shouldOffloadTime);
            current.setexEcutionTimesLocal(current.getExecutionTimesLocal()+1);
            this.addExecMethod(methodSignature,current);
            this.answer = true;
            return ;
        }else if(current.getExecutionTimesLocal()<=1){//second two execution always local
         //   System.out.println("First or second local exec, returning false");
          //  Double elspsedTime = (System.nanoTime()-startShould);
           // log.debug("Shouldoffload ok takes = "+ elspsedTime);
           // shouldOffloadTimes++;
          //  shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
           // log.debug("shouldOffloadTime = "+ shouldOffloadTime);
            current.setexEcutionTimesRemote(current.getExecutionTimesRemote()+1);
            this.addExecMethod(methodSignature,current);
            this.answer = false;
            return ;
        }else{
            /*We look at past history and we offload only if total time nedeed for the remote execution is less than local execution time*/
         /*      if(current.getMeanLocalexecTime()>current.getMeanRemoteExecTime()){
                /*   System.out.println("current.getMeanLocalexecTime()= "+current.getMeanLocalexecTime());
                   System.out.println("current.getMeanRemoteExecTime()= "+current.getMeanRemoteExecTime());
                   System.out.println("Time remote low, returning true");
                   Double elspsedTime = (System.nanoTime()-startShould);
                   log.debug("Shouldoffload ok takes = "+ elspsedTime);
                   shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
                   log.debug("shouldOffloadTime = "+ shouldOffloadTime);*/
            /*       this.answer = true;
                   return;
               }else{
                  /* System.out.println("Time remote hig, returning false");
                   System.out.println("current.getMeanLocalexecTime()= "+current.getMeanLocalexecTime());
                   System.out.println("current.getMeanRemoteExecTime()= "+current.getMeanRemoteExecTime());
                   Double elspsedTime = (System.nanoTime()-startShould);
                   log.debug("Shouldoffload ok takes = "+ elspsedTime);
                   shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
                   log.debug("shouldOffloadTime = "+ shouldOffloadTime);*/
          /*         this.answer = false;
                   return ;
               }
            }*/

          /* Interpolation of past executions */

     /*   log.debug("Elapsed before interp ="+ (System.nanoTime()-startShould));
        long startInterp = System.nanoTime();
        InterpolateResult local_result = OffloadingManager.getExecutionManager().interpolateAssessment(method, argArray, true);
        InterpolateResult remote_result = OffloadingManager.getExecutionManager().interpolateAssessment(method, argArray, false);
        log.debug("Elapsed time in in interp ="+ (System.nanoTime()-startInterp));
        //  System.out.println("remResSuccess= "+remote_result.success());
        // System.out.println("locResSuccess= "+local_result.success());
        long startInterpPostInterp = System.nanoTime();
        if (!remote_result.success() || !local_result.success()) {
            boolean resp = Math.random() > 0.5;
            log.debug("returning random response = "+resp);
            OffloadingManager.getLogManager().addToLog(methodSignature, resp);
           // is_Offloaded = resp;
           // if (is_Offloaded) num_offload++;
            //else num_local ++;
            this.answer = resp;
            return;
        }
        if (local_result.getRunning_time()<= 1000000){ //fast method just not offload
            log.debug("fast method, ret false");
            this.answer = false;
            return;
        }
        final int sizeOfSerializedObject = serObj.toByteArray().length;
        long startUpserTime = System.nanoTime();
        OffloadingManager.getExecutionManager().updateSerializationTime(startSerialization, endSerialization, sizeOfSerializedObject);
        long startGetBandwidth = System.nanoTime();
        // log.debug("Elapsed in nothing ="+ (startGetBandwidth-startUpserTime));
        final double[] bandwidthDU = OffloadingManager.getBandwidthManager().getBandwidth();
        //log.debug("badwidth d="+bandwidthDU[0]+"up"+bandwidthDU[1]);

       // log.debug("Elapsed in bandwodth calc ="+ (System.nanoTime()-startGetBandwidth));
        if (bandwidthDU[0] == 0 || bandwidthDU[1] == 0) {
            //   log.setLevel(Level.ALL);
            // 	log.debug("bw is 0. I will not offload. down: " + bandwidthDownload + " | " + bandwidthUpload);
            //   addDataLog(log, methodSignature,args,local_result,remote_result,sizeOfSerializedObject,bandwidthUpload,bandwidthDownload,0.0,null,null,false);
           // OffloadingManager.getLogManager().addToLog(methodSignature, false);
         //   double time = (System.nanoTime()-startShould);
            //   log.debug("Shouldoffload takes = "+ time);
            this.answer = false;
            return;
        }
        final float serializationTime = OffloadingManager.getExecutionManager().getSerializationTime();
        final double remoteResultSize = (remote_result.getResult_size() == null ? 0 : remote_result.getResult_size());
        double totalTimeInUploadingAndDownloadingArgs = (sizeOfSerializedObject / bandwidthDU[1]) + (remoteResultSize / bandwidthDU[0]) + (sizeOfSerializedObject / serializationTime) +
                (remoteResultSize / serializationTime);
        //rtt = totalTimeInUploadingAndDownloadingArgs;
        //log.debug(String.format("uploading time = %s, downloading_time = %s, serialization_arg = %s, serialization_result = %s",(sizeOfSerializedObject / bandwidthUpload),
        // (remoteResultSize / bandwidthDownload),(sizeOfSerializedObject / serializationTime),(remoteResultSize / serializationTime)));
        final Double timeLocal = local_result.getRunning_time();
      //  log.debug("timeLocal= "+timeLocal);
       // log.debug("remote_result.getRunning_time()= "+remote_result.getRunning_time());
       // log.debug("totalTimeInUploadingAndDownloadingArgs= "+totalTimeInUploadingAndDownloadingArgs);
        final Double timeRemote = remote_result.getRunning_time() + totalTimeInUploadingAndDownloadingArgs;
        long startEnergyCalc = System.nanoTime();
        final Double energyLocal = EnergyModule.calculateEnergyLocal(local_result, bandwidthDU[0]);
        final Double energyRemote = EnergyModule.calculateEnergyRemote(remote_result, sizeOfSerializedObject,bandwidthDU[0],bandwidthDU[1]);
       // log.debug("Elapsed in energy calc ="+ (System.nanoTime()-startEnergyCalc));
        //   log.debug("shouldOffload: totalTimeInUploadingAndDownloadingArgs: " + totalTimeInUploadingAndDownloadingArgs);
        //   log.debug("shouldOffload: timeLocal: " + timeLocal + " | timeRemote: " + timeRemote);
        //   log.debug("shouldOffload: interpolateLocal[1]: " + (local_result.getTransfer_bytes() / bandwidthDownload));
        //   log.debug("shouldOffload: energyLocal: " + energyLocal + " | energyRemote: " + energyRemote);

        final Double utilityLocal = ALPHA*timeLocal + (1-ALPHA)*energyLocal;
        final Double utilityRemote = ALPHA* timeRemote+ (1-ALPHA)*energyRemote;
        // log.debug("shouldOffload: utilityRemote: " + utilityRemote + " | utilityLocal: " + utilityLocal);
        final boolean response = utilityRemote < utilityLocal;


          //  boolean response = true;
         //   is_Offloaded = response;
         //   if (is_Offloaded) {
           //     num_offload++;
          //      energy_cons = energyRemote;
          //  }
          //  else {
          //      num_local ++;
         //       energy_cons = energyLocal;
    //        }
         //   log.setLevel(Level.ALL);
         //   log.debug("shouldOffload: Res: " + response);
           //
            //
            // addDataLog(log, methodSignature, args, local_result, remote_result, sizeOfSerializedObject, bandwidthUpload,bandwidthDownload, totalTimeInUploadingAndDownloadingArgs, utilityLocal, utilityRemote, response);
           // OffloadingManager.getLogManager().addToLog(methodSignature, response);
          //  long finsishTime = System.nanoTime();
           // log.debug("elapsed time post interp"+(finsishTime-startInterpPostInterp));
           // log.debug("Total time should"+(finsishTime-startShould)+"/resp= "+response);
        this.answer = response;
          return;
	/*	} catch (Exception e) {
    		log.debug("Error on shouldOffload. " + e.getMessage());
    		log.debug(Arrays.toString(e.getStackTrace()));
            double time = (System.nanoTime()-startShould);
         //   log.debug("Shouldoffload takes = "+ time);
            //shouldOffloadTime.add(time);
          //  double mean = 0.0;
          //  double acc = 0.0;
          //  for(int i=0;i<shouldOffloadTime.size();i++){
          //      acc+= shouldOffloadTime.get(i);
          //  }
          //  log.debug("meanTime shouldOffload ="+(acc/(double)shouldOffloadTime.size()));
         //   log.setLevel(Level.OFF);
    		return false;*/
		//}
		//return true;


     /*   this.answer = true;
        log.debug("manageInterception: everything ok. Res: TRUEEEEEEEEEEEEEE");
        OffloadingManager.releaseKryoInstyance(this.kryo);
        return;*/
    }


    public boolean getAnswer() {
        return answer;
    }

    private Map<Object, Object> stripCachedObjects(final String methodSignature, final Map<Object, Object> args) {
        /* if (objectCache != null && methodSignature.equals(objectCache.get(METHOD_SIG))) {
            Map<Object, Object> resMap = new HashMap<Object, Object>();
            for (Map.Entry<Object, Object> entry : args.entrySet()) {
                final Object counterPart = objectCache.get(entry.getKey());

                if (counterPart == null || counterPart.hashCode() != entry.hashCode()) {
                    resMap.put(entry.getKey(), entry.getValue());
                }
            }
            return resMap;
        }*/
        return args;
    }


    private ByteArrayOutputStream serialize(final String methodSignature, final Map<Object, Object> args) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Map<Object, Object> cachedArgs = stripCachedObjects(methodSignature, args);
        final Output output = new Output(baos, 1024);

        this.kryo.setAsmEnabled(true);
        kryo.writeObject(output, methodSignature);
        kryo.writeObject(output, cachedArgs);
       /* int answ;
        try{
            answ = this.kryo.writeObject(output, cachedArgs);
            if(answ == -100){
                return null;
            }
        } catch (IllegalArgumentException ex){
            ex.printStackTrace();
        }*/
        output.close();
        return baos;
    }

    private static synchronized MethodStats getExecMethod(String key){
         return executedMethod.get(key);
    }

    private static synchronized MethodStats addExecMethod(String key, MethodStats value){
        return executedMethod.put(key,value);
    }
    
    private static synchronized boolean isMethodPresent(String key){
        return executedMethod.containsKey(key);
    }
}
