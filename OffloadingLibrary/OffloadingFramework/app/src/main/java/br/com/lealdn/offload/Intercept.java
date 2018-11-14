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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import android.net.TrafficStats;

import br.com.lealdn.offload.utils.InterpolateResult;
import br.com.lealdn.offload.utils.Utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Intercept {

    private static double alpha = 0.993;

    private static String log_entry = "";
    private static String testing = "NOT MODIFIED";
   // private static Kryo kryo = new Kryo(OffloadingManager.unserializable);
    private static double ALPHA = 0.8;
    private static double CPU_MODIFIER = 1;
    private static double RADIO_MODIFIER = 1;
	private final static  Logger log = Logger.getLogger(Intercept.class);
	private static boolean is_Offloaded = false;
	private static int num_offload = 0;
    private static int num_local = 0;
    private static Double energy_cons = 0.0;
   // private static ArrayList<String> metSerStop = new ArrayList<String>();
   // private static HashMap<String,MethodStats> metSerOk = new HashMap<String,MethodStats>();
   // private static ArrayList<Double> metTime = new ArrayList<Double>();
    private static Double shouldOffloadTime = 0.0;
    private static int shouldOffloadTimes = 0;
    final static long[] times = new long[6];
    private static Map<Object, Object> objectCache = new HashMap<Object, Object>();
    private final static String METHOD_SIG = "--methodSignature";
    final static Pattern fieldSignaturePattern = Pattern.compile("<([0-9a-zA-Z.$#-]+): ([0-9a-zA-Z.$#\\[\\]]+) ([0-9a-zA-Z.$#]+)>");
    private static long extime;


  /*  public  Intercept() {
        this.kryo = OffloadingManager.getKryoPool().borrow();
    }*/


   /* public boolean manageInterception(final String methodSignature, final Map<Object, Object> args){
        log.debug("methodSignature= "+methodSignature);

        /* Should never offload if we have no internet */

       /* if (OffloadingManager.getNetworkState() == OffloadingManager.NONE) {
            log.debug("manageInterception: no network, halting. Res: false");
            //log.debug("S houldoffload takes = "+ (Sysendstem.nanoTime()-startShould));
            return false;
        }

        double startShould = System.nanoTime();

        /* if the offloading is not enabled always execute the method locally */

       /* if(!OffloadingManager.getEnabled()){
          log.debug("manageInterception: offloading still not enabled. Res: false");
            //  shouldOffloadTimes++;
            //  Double elspsedTime = (System.nanoTime()-startShould);
            //  log.debug("Shouldoffload stop not enabeled takes = "+ elspsedTime);
            // shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
            // log.debug("shouldOffloadTime = "+ shouldOffloadTime);
            return false;
        }

        /* TODO: add a list of methods that should be always stopped because "immutable" variables are un-serializable (if this is possible after static analysis)*/


        /* Now spawn a dedicated thread to serve the offloading request */

        // return true;
/*
        DecisionEngine decisionEngine = new DecisionEngine(methodSignature, args);
        Thread t = new Thread(decisionEngine);
        t.start();

    }*/





    public static void setAlpha(double alpha) {
        ALPHA = alpha;
    }


	public static boolean checkOffloading(){return is_Offloaded;}

    public static int checkNumOffload(){return num_offload;}

    public static int checkNumLoal(){return num_local;}
//    public static double getRTT(){return rtt;}
    public static long getextime(){return extime;}
    public static Double getEnergy_cons(){return energy_cons;}


    
    public static boolean getRandomResponse() {
    	return Math.random() > 0.5;
    }



    /*public static boolean shouldOffload(final String methodSignature, final Map<Object, Object> args) {
       // log.setLevel(Level.OFF);
        log.debug("methodSignature= "+methodSignature);
        double startShould = System.nanoTime();
        if(!OffloadingManager.getEnabled() /*|| metSerStop.contains(methodSignature)){
    /*        shouldOffloadTimes++;
            Double elspsedTime = (System.nanoTime()-startShould);
            log.debug("Shouldoffload stop not enabeled takes = "+ elspsedTime);
            shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
            log.debug("shouldOffloadTime = "+ shouldOffloadTime);
            return false;
        }
        //Log.TRACE();
		try {
			// Should never offload if we have no internet
			/*if (OffloadingManager.getNetworkState() == OffloadingManager.NONE) {
			//	log.debug("shouldOffload: no network, halting. Res: false");
             //   addDataLog(log, methodSignature,args,new InterpolateResult(null,null,null,null,false),new InterpolateResult(null,null,null,null,true),0,null,null,0,null,null,false);
                is_Offloaded = false;
                num_local ++;
           //     log.debug("Shouldoffload takes = "+ (System.nanoTime()-startShould));
				return false;

			}*/
			//log.debug("================ shouldOffload: network state: " + OffloadingManager.getNetworkState());
			//log.debug("== Alpha parameter: " + ALPHA);
			//log.debug("== method: " + methodSignature);
     /*        Method method = null;
            Object[] argArray = null;
            Class<?> clazz = null;
            long startSerialization = 0;
            long endSerialization =0;
            ByteArrayOutputStream serObj = null;
          //  if(!metSerOk.containsKey(methodSignature)){
                clazz = Utils.getClassFromSignature(methodSignature);
               // log.debug("== Clazz: " + clazz);
                method= Utils.getMethodFromSignature(clazz, methodSignature);
               // log.debug("== Method: " + method);
                argArray = getArgsAsArray(args);

              //  final long startSerialization = System.nanoTime();
                startSerialization = System.nanoTime();
                serObj = serialize(methodSignature, args);
                endSerialization = System.nanoTime();
                if(serObj == null){
                  //  metSerStop.add(methodSignature);
                    OffloadingManager.serialzationStops++;
                    log.debug("SerStop= "+OffloadingManager.serialzationStops);
                    shouldOffloadTimes++;
                    Double elspsedTime = (System.nanoTime()-startShould);
                    log.debug("Shouldoffload stop not serializable takes = "+ elspsedTime);
                    shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
                    log.debug("shouldOffloadTime = "+ shouldOffloadTime);
                    return false;
                }
                return true;
              //  metSerOk.put(methodSignature,new MethodStats(methodSignature,0.0,0.0,0,0));

           // }

               /*We arrive here iff method and related obj can be serialized. Here we have to plug the decision engine*/
            /*MethodStats current = metSerOk.get(methodSignature);
            if(current.getExecutionTimesRemote()<=1){//firts two execution always remote
                System.out.println("First or second remote exec, returning true");
                Double elspsedTime = (System.nanoTime()-startShould);
                log.debug("Shouldoffload ok takes = "+ elspsedTime);
                shouldOffloadTimes++;
                shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
                log.debug("shouldOffloadTime = "+ shouldOffloadTime);
                return true;
            }else if(current.getExecutionTimesLocal()<=1){//second two execution always local
                System.out.println("First or second local exec, returning false");
                Double elspsedTime = (System.nanoTime()-startShould);
                log.debug("Shouldoffload ok takes = "+ elspsedTime);
                shouldOffloadTimes++;
                shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
                log.debug("shouldOffloadTime = "+ shouldOffloadTime);
                return false;
            }else{
               /*We look at past history and we offload only if total time nedeed for the remote execution is less than local execution time*/
            /*   if(current.getMeanLocalexecTime()>current.getMeanRemoteExecTime()){
                   System.out.println("current.getMeanLocalexecTime()= "+current.getMeanLocalexecTime());
                   System.out.println("current.getMeanRemoteExecTime()= "+current.getMeanRemoteExecTime());
                   System.out.println("Time remote low, returning true");
                   Double elspsedTime = (System.nanoTime()-startShould);
                   log.debug("Shouldoffload ok takes = "+ elspsedTime);
                   shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
                   log.debug("shouldOffloadTime = "+ shouldOffloadTime);
                   return true;
               }else{
                   System.out.println("Time remote hig, returning false");
                   System.out.println("current.getMeanLocalexecTime()= "+current.getMeanLocalexecTime());
                   System.out.println("current.getMeanRemoteExecTime()= "+current.getMeanRemoteExecTime());
                   Double elspsedTime = (System.nanoTime()-startShould);
                   log.debug("Shouldoffload ok takes = "+ elspsedTime);
                   shouldOffloadTime=(shouldOffloadTime)*((shouldOffloadTimes-1)/shouldOffloadTimes)+ (elspsedTime/shouldOffloadTimes);
                   log.debug("shouldOffloadTime = "+ shouldOffloadTime);
                   return false;
               }
            }
*/
          //  return true;


/*

            log.debug("Elapsed before interp ="+ (System.nanoTime()-startShould));
            long startInterp = System.nanoTime();
            InterpolateResult local_result = OffloadingManager.getExecutionManager().interpolateAssessment(method, argArray, true);
            InterpolateResult remote_result = OffloadingManager.getExecutionManager().interpolateAssessment(method, argArray, false);
            log.debug("Elapsed time in in interp ="+ (System.nanoTime()-startInterp));
          //  System.out.println("remResSuccess= "+remote_result.success());
           // System.out.println("locResSuccess= "+local_result.success());
            long startInterpPostInterp = System.nanoTime();
            if (!remote_result.success() || !local_result.success()) {

            	boolean resp = getRandomResponse();
                log.debug("returning random response = "+resp);
            	OffloadingManager.getLogManager().addToLog(methodSignature, resp);
                is_Offloaded = resp;
                if (is_Offloaded) num_offload++;
                else num_local ++;
            	return resp;
            }
            if (local_result.getRunning_time()<= 1000000){ //fast method just not offload
                log.debug("fast method, ret false");
                return false;
            }
            final int sizeOfSerializedObject = serObj.toByteArray().length;
            long startUpserTime = System.nanoTime();
            OffloadingManager.getExecutionManager().updateSerializationTime(startSerialization, endSerialization, sizeOfSerializedObject);
            long startGetBandwidth = System.nanoTime();
           // log.debug("Elapsed in nothing ="+ (startGetBandwidth-startUpserTime));
            final double[] bandwidthDU = getBandwidth();
            log.debug("badwidth d="+bandwidthDU[0]+"up"+bandwidthDU[1]);

            log.debug("Elapsed in bandwodth calc ="+ (System.nanoTime()-startGetBandwidth));
            if (bandwidthDU[0] == 0 || bandwidthDU[1] == 0) {
             //   log.setLevel(Level.ALL);
           // 	log.debug("bw is 0. I will not offload. down: " + bandwidthDownload + " | " + bandwidthUpload);
             //   addDataLog(log, methodSignature,args,local_result,remote_result,sizeOfSerializedObject,bandwidthUpload,bandwidthDownload,0.0,null,null,false);
            	OffloadingManager.getLogManager().addToLog(methodSignature, false);
                double time = (System.nanoTime()-startShould);
             //   log.debug("Shouldoffload takes = "+ time);

                return false;
            }
            final float serializationTime = OffloadingManager.getExecutionManager().getSerializationTime();
            final double remoteResultSize = (remote_result.getResult_size() == null ? 0 : remote_result.getResult_size());
            double totalTimeInUploadingAndDownloadingArgs = (sizeOfSerializedObject / bandwidthDU[1]) + (remoteResultSize / bandwidthDU[0]) + (sizeOfSerializedObject / serializationTime) +
                    (remoteResultSize / serializationTime);
            rtt = totalTimeInUploadingAndDownloadingArgs;
            //log.debug(String.format("uploading time = %s, downloading_time = %s, serialization_arg = %s, serialization_result = %s",(sizeOfSerializedObject / bandwidthUpload),
                   // (remoteResultSize / bandwidthDownload),(sizeOfSerializedObject / serializationTime),(remoteResultSize / serializationTime)));
            final Double timeLocal = local_result.getRunning_time();
            log.debug("timeLocal= "+timeLocal);
            log.debug("remote_result.getRunning_time()= "+remote_result.getRunning_time());
            log.debug("totalTimeInUploadingAndDownloadingArgs= "+totalTimeInUploadingAndDownloadingArgs);
            final Double timeRemote = remote_result.getRunning_time() + totalTimeInUploadingAndDownloadingArgs;
            long startEnergyCalc = System.nanoTime();
            final Double energyLocal = EnergyModule.calculateEnergyLocal(local_result, bandwidthDU[0]);
            final Double energyRemote = EnergyModule.calculateEnergyRemote(remote_result, sizeOfSerializedObject,bandwidthDU[0],bandwidthDU[1]);
            log.debug("Elapsed in energy calc ="+ (System.nanoTime()-startEnergyCalc));
         //   log.debug("shouldOffload: totalTimeInUploadingAndDownloadingArgs: " + totalTimeInUploadingAndDownloadingArgs);
         //   log.debug("shouldOffload: timeLocal: " + timeLocal + " | timeRemote: " + timeRemote);
         //   log.debug("shouldOffload: interpolateLocal[1]: " + (local_result.getTransfer_bytes() / bandwidthDownload));
         //   log.debug("shouldOffload: energyLocal: " + energyLocal + " | energyRemote: " + energyRemote);

            final Double utilityLocal = /*ALPHAtimeLocal /*+ (1-ALPHA)energyLocal;
            final Double utilityRemote = /*ALPHA timeRemote /*+ (1-ALPHA)energyRemote;
            log.debug("shouldOffload: utilityRemote: " + utilityRemote + " | utilityLocal: " + utilityLocal);

            final boolean response = utilityRemote < utilityLocal;


          //  boolean response = true;
            is_Offloaded = response;
            if (is_Offloaded) {
                num_offload++;
                energy_cons = energyRemote;
            }
            else {
                num_local ++;
                energy_cons = energyLocal;
            }
         //   log.setLevel(Level.ALL);
         //   log.debug("shouldOffload: Res: " + response);
           //
            //
            // addDataLog(log, methodSignature, args, local_result, remote_result, sizeOfSerializedObject, bandwidthUpload,bandwidthDownload, totalTimeInUploadingAndDownloadingArgs, utilityLocal, utilityRemote, response);
            OffloadingManager.getLogManager().addToLog(methodSignature, response);
            long finsishTime = System.nanoTime();
            log.debug("elapsed time post interp"+(finsishTime-startInterpPostInterp));
            log.debug("Total time should"+(finsishTime-startShould)+"/resp= "+response);
          return response;
		} catch (Exception e) {
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
    		return false;
		}
		//return true;
    }*/



    public static void addDataLog(Logger log, String methodSignature, Map<Object, Object> args, InterpolateResult local_result, InterpolateResult remote_result, int sizeOfSerializedObject,
                                   Double bandwidthUpload, Double bandwidthDownload, double totalTimeInUploadingAndDownloadingArgs, Double utility_local, Double utility_remote, boolean response){
        //logging for data analysis
        //(Method_name, argument, alpha, network_status, local_interpolation_success, local_running_time_estimate, local_transfer_size, local_cpu_ticks, result_size_estimate,
        // remote_interpolation_success, remote_running_time_estimate, remote_transfer_size, remote_cpu_ticks, result_size_estimate, size_of_arg)

        log_entry = String.format("!-- %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s", methodSignature, Arrays.toString(getArgsAsArray(args)), ALPHA,
                OffloadingManager.getNetworkState(), bandwidthUpload, bandwidthDownload, local_result.success(), local_result.getRunning_time(), local_result.getTransfer_bytes(),
                local_result.getCPU_ticks(), local_result.getEnergy(), remote_result.success(), remote_result.getRunning_time(),
                remote_result.getEnergy(), remote_result.getResult_size(), sizeOfSerializedObject, totalTimeInUploadingAndDownloadingArgs, utility_local, utility_remote, response);
    }

    private static double[] getBandwidth() {
    	double[] bandwidth = OffloadingManager.getBandwidthManager().getBandwidth();
    	return bandwidth;
    }

    //update local runtime data
    public static void updateMethodRuntime(final String methodSignature, final long startTime, final long finishTime, final Map<Object, Object> args, final long[] rxTxBytes, final long cpuTicks) {
        try {
            long startUp = System.nanoTime();
            final long currentCpuTicks = getCurrentCPUTickCount();
            final long[] currentRxTxBytes = getRxTxCount();
            final Class<?> clazz = Utils.getClassFromSignature(methodSignature);
            final Method method = Utils.getMethodFromSignature(clazz, methodSignature);

            final long time = finishTime - startTime;
            log.debug("Execution time local: " + time+" for: "+methodSignature);
            extime = time;
            OffloadingManager.getExecutionManager().updateMethodRuntimeAssessment(method, true, time, getArgsAsArray(args),
                    (currentRxTxBytes[0] - rxTxBytes[0]) + (currentRxTxBytes[1] - rxTxBytes[1]), null, (currentCpuTicks-cpuTicks));
           // log.debug("update runtime local exec"+(System.nanoTime()-startUp));
        } catch(Exception ex) {
            log.error("Error on updatingMethodRuntime. " + ex.getMessage());
        }


        /*try {

    		final long currentCpuTicks = getCurrentCPUTickCount();
    		//final long[] currentRxTxBytes = getRxTxCount();
	    	final Class<?> clazz = Utils.getClassFromSignature(methodSignature);
	    	final Method method = Utils.getMethodFromSignature(clazz, methodSignature);
    		final long time = (long)((finishTime - startTime));
    		log.debug("method:"+methodSignature+";localExecTime:"+time);
    		//System.out.println("method:"+methodSignature+"takes "+time+"locally");
          //  log.debug(log_entry + String.format(", %s",time));
            extime = time;
            MethodStats current = metSerOk.get(methodSignature);
            current.setexEcutionTimesLocal(current.getExecutionTimesLocal()+1);
            log.debug("current.getExecutionTimesLocal()="+current.getExecutionTimesLocal());
            current.setMeanLocalexecTime((((current.getMeanLocalexecTime())*(current.getExecutionTimesLocal()-1))/current.getExecutionTimesLocal())+((time)/(current.getExecutionTimesLocal())));
           /* if(current.getExecutionTimesLocal()!=1){
                current.setMeanLocalexecTime(current.getMeanLocalexecTime()*0.4+time*0.6);
            }else{
                current.setMeanLocalexecTime(time);
            }
            log.debug("method:"+methodSignature+";localMeanExecTime:"+current.getMeanLocalexecTime());

            /*if(current.getBigVariation()){
                current.resetStats();
                current.setBigVariation(false);
            }
    	} catch(Exception ex) {
    		log.debug("Error on updatingMethodRuntime. " + ex.getMessage());
    	}*/
    }

    public static void updateMethodRuntime(final String methodSignature, final Map<Object, Object> args, final long[] rxTxBytes, final long cpuTicks) {
        try {
            final long currentCpuTicks = getCurrentCPUTickCount();
            final long[] currentRxTxBytes = getRxTxCount();
            final Class<?> clazz = Utils.getClassFromSignature(methodSignature);
            final Method method = Utils.getMethodFromSignature(clazz, methodSignature);

            final long time = System.nanoTime() ;
           // log.debug("*updateMethodRuntime** delta time: " + time);
            //log.debug(log_entry + String.format(", %s",time));
           // extime = (long)(time*0.001);
            OffloadingManager.getExecutionManager().updateMethodRuntimeAssessment(method, true, time, getArgsAsArray(args),
                    (currentRxTxBytes[0] - rxTxBytes[0]) + (currentRxTxBytes[1] - rxTxBytes[1]), null, (currentCpuTicks-cpuTicks));
        } catch(Exception ex) {
            log.debug("Error on updatingMethodRuntime. " + ex.getMessage());
        }
    }
    
    private static Object[] getArgsAsArray(final Map<Object, Object> args) {
      //  System.out.println("getArgsArray starts!!!!!!!!!!!!!!!!!!!!");
    	final Object[] params = new Object[args.keySet().size()];
    	int counter = 0;
    	for (final Map.Entry<Object, Object> entry : args.entrySet()) {
          //  log.debug("key ="+entry.getKey()+"= ");
          //  log.debug("value ="+(entry.getValue()));
    		if (((String)entry.getKey()).startsWith("@arg")) {
    			final int index = Integer.valueOf(((String)entry.getKey()).substring(4));
    			params[index] = entry.getValue();
               // System.out.println("params[index]"+params[index]);
    			counter++;
    		}
    	}
    	final Object[] copy = new Object[counter];
    	for (int i = 0; i < copy.length; i++) {
    		copy[i] = params[i];
    	}
    	return copy;
    }

    private static Map<Object, Object> stripCachedObjects(final String methodSignature, final Map<Object, Object> args) {
        if (objectCache != null && methodSignature.equals(objectCache.get(METHOD_SIG))) {
            Map<Object, Object> resMap = new HashMap<Object, Object>();
            for (Map.Entry<Object, Object> entry : args.entrySet()) {
                final Object counterPart = objectCache.get(entry.getKey());

                if (counterPart == null || counterPart.hashCode() != entry.hashCode()) {
                    resMap.put(entry.getKey(), entry.getValue());
                }
            }
            return resMap;
        }
        return args;
    }

    private static void saveCache(final String methodSignature, final Map<Object, Object> args) {
        objectCache = new HashMap<Object, Object>(args);
    }
    
    private static ByteArrayOutputStream serialize(final String methodSignature, final Map<Object, Object> args, Kryo kryo) {
       final ByteArrayOutputStream baos = new ByteArrayOutputStream();
       final Map<Object, Object> cachedArgs = stripCachedObjects(methodSignature, args);
       final Output output = new Output(baos, 1024);

       kryo.setAsmEnabled(true);
        kryo.writeObject(output, methodSignature);
        kryo.writeObject(output, cachedArgs);
       /*int answ = kryo.writeObject(output, methodSignature);
       try{
           answ = kryo.writeObject(output, cachedArgs);
           if(answ == -100){
               return null;
           }
        } catch (IllegalArgumentException ex){
            ex.printStackTrace();
        }*/
       output.close();
       return baos;
    }
    
    public static Object sendAndSerialize(final String methodSignature, final Map<Object, Object> args) throws Throwable {
        double startSendAndSerTime= System.nanoTime();
        try {
            saveCache(methodSignature, args);
            final Class<?> clazz = Utils.getClassFromSignature(methodSignature);
            final Method method = Utils.getMethodFromSignature(clazz, methodSignature);
            final Object[] params = getArgsAsArray(args);
            Object oldInstance = args.get("@this");

            Future<Object> result = OffloadingManager.getExecutor().submit(new Callable<Object>() {
				@Override
				public Object call() throws ClientProtocolException, IOException {
                try {
                     Object res =null;
                     Kryo kryo = OffloadingManager.borrowKryoInstance();
                     System.out.println("sendAndSerialize for "+methodSignature);
                     res = Intercept.sendFile(method, params, methodSignature, args,kryo);
                     OffloadingManager.releaseKryoInstyance(kryo);
                    return res;
                } catch(ClientProtocolException ex) {
                    ex.printStackTrace();
                    log.error("Error in sendFile." ,ex);
                    log.error(ex);
                    throw ex;
                } catch(IOException ex) {
                    ex.printStackTrace();
                    log.error("Error in sendFile." ,ex);
                    log.error(ex);
                    throw ex;
                } catch (Exception e){
                   // System.out.println("ERRORE in sendFILEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE:"+e.getMessage());
                    throw e;
                }
				}
			});
            long startPropagation = System.nanoTime();
            HashMap<String, Object> hashmap = (HashMap<String, Object>) result.get();
            updateClassFields(hashmap);
            if(oldInstance!=null) {
                Object newInstance = hashmap.get("@this");
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    field.set(oldInstance, field.get(newInstance));
                }
            }
            double totalTimeRemote = (System.nanoTime()-startSendAndSerTime);
         //   log.debug("method "+methodSignature+"total time= "+totalTimeRemote);
          //  log.debug("current.getExecutionTimesRemote="+current.getExecutionTimesRemote());
         //   current.setMeanRemoteExecTime((((current.getMeanRemoteExecTime())*(current.getExecutionTimesRemote()-1))/current.getExecutionTimesRemote())+((totalTimeRemote)/(current.getExecutionTimesRemote())));
            /* if(current.getExecutionTimesRemote() != 1){
                current.setMeanRemoteExecTime(current.getMeanRemoteExecTime()*0.4+totalTimeRemote*0.6);
            }else{
                current.setMeanRemoteExecTime(totalTimeRemote);
            }*/
           // log.debug("method "+methodSignature+"meanTotaltimeRemote= "+current.getMeanRemoteExecTime());
           /*if(current.getBigVariation()){
                current.resetStats();
                current.setBigVariation(false);
            }*/
          //  log.debug("Elapsed time propagation"+(System.nanoTime()-startPropagation));
            Object retObj = hashmap.get("@res");
            Exception thrownEx = (Exception) hashmap.get("@ex");
            if(retObj!= null){
                return retObj;
            }else if(thrownEx!=null) {
                System.out.println("RETURNED AN EXCEPTION!!!");
                throw thrownEx;
            }else {
                return null;
            }

        } catch(ClassNotFoundException ex) {
            log.error(ex.getMessage());
            return null;
        } catch(NoSuchMethodException ex) {
            log.error(ex.getMessage());
            return null;
        } catch(ExecutionException ex) {
        	throw ex.getCause();
        } catch(InterruptedException ex) {
            log.error(ex.getMessage());
            return null;
        } catch(Exception ex) {
        	log.error("sendAndSerialize error in execution", ex);
        	throw ex;
        }
    }
    private static void updateClassFields(HashMap<String, Object> hashmap) {
        for(Map.Entry<String, Object> element : hashmap.entrySet()) {

            if (element.getKey().startsWith("field-")) {
                final String fieldSig = element.getKey().split("-")[1];
                final Matcher matcher = fieldSignaturePattern.matcher(fieldSig);
                try {
                    matcher.find();
                    final Class<?> clazz = Class.forName(matcher.group(1));
                    final Field field = clazz.getDeclaredField(matcher.group(3));
                    field.setAccessible(true);
                    field.set(matcher.group(3), element.getValue());
                } catch (Exception e) {
                    continue;
                }
            }
        }
    }

    private static String readErrorMessageFromResponse(final HttpResponse response) {
       // log.debug("readErrorMessageFromResponse starts");
    	final byte[] bresp = readResponse(response);
    	//log.debug("Respose read:"+new String(bresp));
    	return new String(bresp);
    }
    
    private static Object readKryoObjectFromResponse(long serTime,final HttpResponse response, final long uploadElapsedTime, final long startTime, final Method method, final Object[] args, Kryo kryo) {
        System.out.println("readKryoObjectFromResponse for "+method.getName());
        long startDes= System.nanoTime();
        final byte[] bresp = readResponse(response);
    	final Input input = new Input(bresp);
    	try {
    		kryo.setAsmEnabled(true);
            // Change the InstatiatorStrategy to prevent "Class cannot be created (missing no-arg constructor)" exception
            kryo.setInstantiatorStrategy(new InstantiatorStrategy() {
                @Override
                public ObjectInstantiator newInstantiatorOf(Class type) {
                    try {
                        type.getConstructor();
                        return new Kryo.DefaultInstantiatorStrategy().newInstantiatorOf(type);
                    } catch (NoSuchMethodException | SecurityException e) {
                        return new StdInstantiatorStrategy().newInstantiatorOf(type);
                    }
                }
            });
    		final Map<Object, Object> result = kryo.readObject(input, HashMap.class);
            final long endDeser = System.nanoTime();
            long elapsedTimeinDeser = (long) ((endDeser-startDes)*0.001);
            long  remoteRequestTime = (long)((startDes - startTime)*0.001); //time elapsed between upload and that point
            //OffloadingManager.getExecutionManager().updateSerializationTime(startDes, totalRequestTime, bresp.length);

    		final long executionTimeInServer = (Long)result.get("t");
            final long timeDeser = (Long)result.get("tD");
          //  final long  timeSerial = (Long)result.get("tS");

    		final double downloadBandwidth = Utils.calculateBandwidth(startTime + uploadElapsedTime + executionTimeInServer, endDeser, (long)bresp.length);
            OffloadingManager.getBandwidthManager().setLocationBasedBandwidth(downloadBandwidth, -1);
            log.debug("method:"+method.getName()+";remoteRequestTime:"+remoteRequestTime +";remoteExecTime:" + executionTimeInServer+";timeSerDev:"+serTime+";timeDeserDev:"+elapsedTimeinDeser+";timeDeserServ:"+timeDeser);

            // log.debug("StartTime = " + startTime + "\tuploadElapsedTime = " + uploadElapsedTime + "\tremote execution time : " + executionTimeInServer + "\ttotalRequestTime = " + totalRequestTime);
            //log.debug("size of result : " + bresp.length + "\ttime taken : " + (totalRequestTime - (startTime + uploadElapsedTime + executionTimeInServer)));
            //log.debug("Setting Download speed " + downloadBandwidth);
            OffloadingManager.getExecutionManager().updateMethodRuntimeAssessment(method, false, executionTimeInServer, args, 0, bresp.length, null);
           /* MethodStats current = metSerOk.get(methodSignature);
            current.setexEcutionTimesRemote(current.getExecutionTimesRemote()+1);
            current.setMeanRemoteExecTime(current.getMeanRemoteExecTime()*((current.getExecutionTimesRemote()-1)/current.getExecutionTimesRemote())+((rtt+serTime+elapsedTimeinDeser)/(current.getExecutionTimesRemote())));
           */
           log.debug("Elapsed time in readKryoObjectFromResponse= "+(System.nanoTime()-startDes));
           return result.get("r");
    	} catch(Exception ex) {
    		ex.printStackTrace();
    		return null;
    	} finally {
    		input.close();
    	}
    }
    
    public static long[] getRxTxCount() {
    	final int uid = OffloadingManager.getUid();
    	final Long rxBytes = TrafficStats.getUidRxBytes(uid);
    	final Long txBytes = TrafficStats.getUidTxBytes(uid);
    	
    	return new long[] { rxBytes, txBytes };
    }

    public static long getCurrentCPUTickCount() {
        Utils.getPidUsrSysTime(OffloadingManager.getPid(), times);
        return times[0] + times[1];
    }
    
    private static byte[] readResponse(final HttpResponse response) {
    //	log.debug(Arrays.toString(response.getAllHeaders()));
    	final int length = Integer.valueOf(response.getFirstHeader("Content-Length").getValue());
		//log.debug("Length: " + length);
		final byte[] buffer = new byte[(int)length];

		DataInputStream dataIs;
		try {
			dataIs = new DataInputStream(response.getEntity().getContent());
			dataIs.readFully(buffer);
		} catch (Exception e1) {
			log.error(e1.getMessage());
			e1.printStackTrace();
			return null;
		}

		return buffer;
    }

    public static boolean checkConnection(){
        //ConnectionUtils.setIp(ip);
       // ConnectionUtils.setPort(port);
        setAlpha(alpha);
      //  log.debug("Checking connection");
        Long rtt = ConnectionUtils.pingServer();
        if (rtt == null) return false;
        return true;
    }


    private static Object sendFile(final Method method, final Object[] methodParams, String methodSignature, Map<Object, Object> args, Kryo kryo) throws IOException {
        System.out.println("sendFile for "+methodSignature);
        final long timeStart = System.nanoTime();
    	String url = "http://"+ ConnectionUtils.getIp() +":"+String.valueOf(ConnectionUtils.getPort())+"/execute";
    	//log.debug(""?);
    	HttpParams params = new BasicHttpParams();
    	HttpConnectionParams.setConnectionTimeout(params, 5000);
    	// 1 hour socket timeout ;D
    	// Socket timeout is actually the timeout when there is no data passing through the socket anymore
    	// that means that the socket is then considered stale or old
    	HttpConnectionParams.setSoTimeout(params, 1000 * 60 * 60);
    	final HttpClient httpclient = new DefaultHttpClient(params);
    	final HttpPost httppost = new HttpPost(url);
    	final UploadStream uploadStream = new UploadStream();
       /* for (Map.Entry<Object, Object> entry : args.entrySet()) {
            System.out.println("INTECEPTTTTT: key: " + entry.getKey() + " value: "
                    + entry.getValue()+"for method "+methodSignature);
        }*/

        final long startSerialization = System.nanoTime();
        final ByteArrayOutputStream baos = serialize(methodSignature, args, kryo);
        final long serializTime = (long)((System.nanoTime()-startSerialization));
        final byte[] fileUpload = baos.toByteArray();
       // log.debug("sendFile: upload size: " + fileUpload.length);
    	final ByteArrayEntity fileEntity = new ByteArrayEntity(fileUpload) {
    		@Override
    		public void writeTo(final OutputStream outstream)   {
    			uploadStream.setOutputStream(outstream);
    			try{
    			    super.writeTo(uploadStream);
    		    }catch(java.io.IOException ex){
    			    ex.printStackTrace();
    			    log.error("ERROREEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEee");
                }
    		}
    	};
       // System.out.println("Prima getExecManager");
        OffloadingManager.getExecutionManager().updateSerializationTime(startSerialization, System.nanoTime(), fileUpload.length);
      //  System.out.println("Dopo getExecManager");
    	final long uploadStartTime = System.nanoTime();
    	httppost.setEntity(fileEntity);
    	//log.debug("EXECUTING POST FOR METHOD_:"+methodSignature);
    	final HttpResponse response = httpclient.execute(httppost);

    	final long uploadEndTime = uploadStream.getLastTime();
    	final double upB = Utils.calculateBandwidth(uploadStartTime, uploadEndTime, uploadStream.getTransferred());
    	OffloadingManager.getBandwidthManager().setLocationBasedBandwidth(-1,upB);
       // log.debug("Setting upload speed = " + bandwidth);
    	//log.debug("status: " + response.getStatusLine().getStatusCode());
        int statcode = response.getStatusLine().getStatusCode();
      //  log.debug("*****************STATCODE="+statcode+"*********************");
    	try {
    		switch(statcode) {
    		case 200:
    			return readKryoObjectFromResponse(serializTime,response, (long)((uploadEndTime - uploadStartTime)), uploadStartTime,method, methodParams, kryo);
    		case 204:
    			//log.debug("OKAY");
    			return null;
    		case 500:
    			log.error(readErrorMessageFromResponse(response));
    			return null;
    		default:
    			return null;
    		}

        }catch (Exception ex){
    	    ex.printStackTrace();
            return null;
        }
    	finally {
    		//log.debug("SendFileFinito|||||||||||||||||||||!!!!!!!!!!!!!!!1111");
            extime = (long)((System.nanoTime() - timeStart));
            //log.debug(log_entry + String.format(", %s", extime));

    	}
    }

}
