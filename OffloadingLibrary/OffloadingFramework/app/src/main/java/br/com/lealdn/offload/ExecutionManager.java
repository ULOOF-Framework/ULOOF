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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import br.com.lealdn.offload.utils.InterpolateResult;
import br.com.lealdn.offload.utils.Utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;



public class ExecutionManager {
    public Map<String, MethodExecution> executions;
    private SharedPreferences preferences;
    private final String EX_MANAGER_KEY = "executionManagerKey";
    private final String CPU_USAGE_KEY = "cpuUsageKey";
    private final String SERIALIZATION_TIME_KEY = "serializationTimeKey";
    private Gson gson = new Gson();
    private final static Logger log = Logger.getLogger(ExecutionManager.class);
    private Map<Class<?>, AssessmentConverter> assesmentConverterCache = new HashMap<Class<?>, AssessmentConverter>();

    //private int usageCount = 0;
    
    public ExecutionManager(final SharedPreferences preferences) {
        log.setLevel(Level.OFF);
        this.preferences = preferences;
        this.initialize();
    }
    
    private void initialize() {
        if (executions == null) {
            if ("".equals(this.preferences.getString(EX_MANAGER_KEY, ""))) {
                this.executions = new HashMap<String, MethodExecution>();
            }
            else {
                try {
                    final Type t = new TypeToken<Map<String, MethodExecution>>() {}.getType();
                    this.executions = gson.fromJson(this.preferences.getString(EX_MANAGER_KEY, ""), t);
                    //this.fixExecutionMapOrder();
                } catch(Exception ex) {
                    log.error("Error while reading from saved executions object. Starting again.");
                    this.executions = new HashMap<String, MethodExecution>();
                }
            }
        }
        
        assesmentConverterCache.put(DefaultAssesmentConverter.class, new DefaultAssesmentConverter());
    }

    // TODO: NEED MORE OPTIMAL WAYS TO DO THIS
    public void saveExecutionsObject() {
        //if (usageCount++ > 300) {
        //    usageCount = 0;
        //    final Type t = new TypeToken<Map<String, MethodExecution>>() {}.getType();
        //    final String executionsString = gson.toJson(this.executions, t);

        //    Editor edit = this.preferences.edit();
        //    edit.putString(EX_MANAGER_KEY, executionsString);
        //    edit.commit();
        //}
    }
    
    public Map<String, MethodExecution> getExecutions() {
        return this.executions;
    }
    
    public void saveExecutions() {
        final Editor editor = this.preferences.edit();
        editor.putString(EX_MANAGER_KEY, gson.toJson(this.executions));
        editor.commit();
    }
    
    private String reverseMapClassName(Class<?> clazz) {
        final String className = clazz.getName();
        final String preffix = "[";
        if (className.startsWith(preffix)) {
            final String strippedName = className.substring(preffix.length());
            if ("B".equals(strippedName)) {
                return "byte[]";
            } else if ("F".equals(strippedName)) {
                return "float[]";
            } else if ("D".equals(strippedName)) {
                return "double[]";
            } else if ("J".equals(strippedName)) {
                return "long[]";
            } else if ("S".equals(strippedName)) {
                return "short[]";
            } else if ("I".equals(strippedName)) {
                return "int[]";
            } else if ("C".equals(strippedName)) {
                return "char[]";
            } else if (strippedName.startsWith("L")) {
                return strippedName.substring(1) + "[]";
            }
        }
        return className;
    }
    
    public String getMethodSignature(final Method method) {
        return "<"+ method.getDeclaringClass().getName()+ ": " + reverseMapClassName(method.getReturnType()) + " " + method.getName() + "(" + parametersAsString(method, true) + ")>";
    }
    
    private String parametersAsString ( Method method, boolean longTypeNames ) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if ( parameterTypes.length == 0 ) return "";
        StringBuilder paramString = new StringBuilder();

        paramString.append(reverseMapClassName(parameterTypes[0]));
        for ( int i = 1 ; i < parameterTypes.length ; i++ )
        {
            paramString.append(",").append(reverseMapClassName(parameterTypes[i]));
        }
        return paramString.toString();
    }
    
    public int getMethodRuntimeCount(final Method method, final boolean local) {
        MethodExecution executionRounds = this.executions.get(getMethodSignature(method));
        if (executionRounds != null) {
            return local ? executionRounds.localRoundsTime.size() : executionRounds.remoteRoundsTime.size();
        }
        else {
            return 0;
        }
    }

    public float getSerializationTime() {
        return this.preferences.getFloat(SERIALIZATION_TIME_KEY, 1F);
    }

    public void updateSerializationTime(final long startTime, final long endTime, final int size) {
        Editor edit = this.preferences.edit();
        final Double serializationTime = Utils.calculateBandwidth(startTime, endTime, size);
        edit.putFloat(SERIALIZATION_TIME_KEY, serializationTime.floatValue());
        edit.commit();
    }
    
    public void updateMethodRuntimeAssessment(final Method method, final boolean local, long time, final Object[] args, final long rxTxBytes, final Integer resultSize, final Long cpuTicks) {
        final long addTime = System.nanoTime();

        final String signature = getMethodSignature(method);
       // final double assessment = calculateAssessment(method, args);
        
        MethodExecution executionRounds = this.executions.get(signature);
        if (executionRounds == null) {
            executionRounds = new MethodExecution(signature);
        }
        executionRounds.timesExecuted++;

        time = time + (long)(((System.nanoTime() - addTime)));
        
        //log.debug("updateMethodRuntimeAssessment: sig: " + signature + " asses: " + assessment +  " | local: " + local + "| time: " + time + " | rxTxB: " +  rxTxBytes + " | cpuTicks " + cpuTicks);
        executionRounds.addRound(new ExecutionRound(signature, 0.0, time, local, rxTxBytes, resultSize, cpuTicks));
        this.executions.put(signature, executionRounds);
       // this.saveExecutionsObject();
    }
    
    public boolean canInterpolateAssessment(final Method method, final Object[] args, final boolean local) {
        final double assessment = calculateAssessment(method, args);
        final MethodExecution execution = this.executions.get(getMethodSignature(method));
        if (execution != null) {
 
            return execution.canInterpolate(assessment, local);
        }
        else {
            return false;
        }
    }
    
    //public Double[] interpolateAssessment(final Method method, final Object[] args, final boolean local) {
    public InterpolateResult interpolateAssessment(final Method method, final Object[] args, final boolean local) {
        final double assessment = 0;//calculateAssessment(method, args);
        final MethodExecution execution = this.executions.get(getMethodSignature(method));
        if (execution != null) {
           // System.out.println("EXECUTION NOT NULLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLl");
          //  log.debug("interpolateAssessment asses: " + assessment + " | local: " + local);
            final Integer resultSize = execution.getResultSize(assessment, local);
            Double running_time_predicted = execution.interpolateTime(assessment, local);
           // System.out.println("running_time_predicted= "+running_time_predicted);
            Double transfer_time_predicted = execution.interpolateRxTx(assessment, local);
           // System.out.println("transfer_time_predicted= "+transfer_time_predicted);
            Double CPU_ticks_predicted = execution.interpolateCPUTicks(assessment, local);
            InterpolateResult result = new InterpolateResult(running_time_predicted,transfer_time_predicted,CPU_ticks_predicted,resultSize,local);
            return result;
            //return new Double[] { execution.interpolateTime(assessment, local), execution.interpolateRxTx(assessment, local), resultSize != null ? resultSize.doubleValue() : null,
            //        execution.interpolateCPUTicks(assessment, local) };
        }
        else {
          //  System.out.println("NULLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLlooooooooo");
            //return null;
            InterpolateResult result = new InterpolateResult(null,null,null,null,local);
            return result;
        }
    }

    private boolean isCompletelyNull(final RelevantParameter[] relevantParameters) {
        for (RelevantParameter par : relevantParameters) {
            if (par != null) {
                return false;
            }
        }
        return true;
    }
    
    private AssessmentConverter getConverter(Class<?> assessmentConverter) {
        if (assesmentConverterCache.containsKey(assessmentConverter)) {
            return assesmentConverterCache.get(assessmentConverter);
        }
        AssessmentConverter converter = assesmentConverterCache.get(DefaultAssesmentConverter.class); 
        
        try {
            converter = (AssessmentConverter)assessmentConverter.newInstance();
        } catch (InstantiationException e) {
            log.error("Error instantiating custom converter!. " + e.getMessage());
        } catch (IllegalAccessException e) {
            log.error("Error instantiating custom converter!. " + e.getMessage());
        }
        
        assesmentConverterCache.put(assessmentConverter, converter);
        return converter;
    }

    private AssessmentConverter getMethodAllParametersAssessmentConverter(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof MethodAssessmentConverter) {
                return getConverter(((MethodAssessmentConverter)annotation).converter());
            }
        }
        return null;
    }

    private double calculateAssessment(final Method method, final Object[] args) {
        final AssessmentConverter methodAssessmentConverter = getMethodAllParametersAssessmentConverter(method);
        if (methodAssessmentConverter != null && !(methodAssessmentConverter instanceof DefaultAssesmentConverter)) {
            return methodAssessmentConverter.convertAllArgumentsToAssesment(args);
        }
        //System.out.println("calculateAssessment starts!!!!");
        final RelevantParameter[] relevantParameters = new RelevantParameter[args.length];
        int i = 0;

        for (Annotation[] annotations : method.getParameterAnnotations()) {
            relevantParameters[i] = null;
            if (annotations.length > 0) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof RelevantParameter) {
                        relevantParameters[i] = (RelevantParameter)annotation;
                    }
                }
            }
            i++;
        }

        final double[] weights = new double[args.length];
        final AssessmentConverter[] converters = new AssessmentConverter[args.length];
        if (isCompletelyNull(relevantParameters)) {
            for (int index = 0; index < weights.length; index++) {
                weights[index] = 1D/weights.length;
                converters[index] = getConverter(DefaultAssesmentConverter.class);
            }   
        }
        else {
            for (int index = 0; index < weights.length; index++) {
                if (relevantParameters[index] != null) {
                    weights[index] = relevantParameters[index].weight();
                    converters[index] = getConverter(relevantParameters[index].converter());
                }
                else {
                    weights[index] = 0;
                    converters[index] = getConverter(DefaultAssesmentConverter.class);
                }
            }
        }
        return assessArgs(weights, converters, args);
    }
    
    private double assessArgs(final double[] weights, final AssessmentConverter[] converters, final Object[] args) {
        double calculation = 0;
        for (int i = 0; i < weights.length; i++) {
            final double weight = weights[i];
            final double assessment = converters[i].convertAssesment(args[i]);
            calculation += weight * assessment;
        }
        return calculation;
    }

    
    public class MethodExecution {
        public final String methodSignature;

        public Interpolator<Double, Double> localRoundsTime;
        public Interpolator<Double, Double> localRoundsRxTx;
        public Interpolator<Double, Double> remoteRoundsTime;
        public Interpolator<Double, Double> remoteRoundsResultSize;
        public Interpolator<Double, Double> localCPUTicks;
        public int timesExecuted=0;

        public MethodExecution(final String methodSignature) {
            final Interpolator.Smoothable<Double> smoother = new Interpolator.Smoothable<Double>() {
                @Override
                public Double smooth(Double previous, Double current) {
                    return previous * 0.4 + current * 0.6;
                }
            };

            this.methodSignature = methodSignature;
            this.localRoundsTime = new Interpolator<Double, Double>(smoother);
            this.localRoundsRxTx = new Interpolator<Double, Double>(smoother);
            this.remoteRoundsTime = new Interpolator<Double, Double>(smoother);
            this.remoteRoundsResultSize = new Interpolator<Double, Double>(smoother);
            this.localCPUTicks = new Interpolator<Double, Double>(smoother);
           // timesExecuted++;
        }

        public void addRound(final ExecutionRound round) {
            if (round.local) {
                localRoundsTime.addRound(round.assessment, ((Number)round.time).doubleValue());
                localRoundsRxTx.addRound(round.assessment, ((Number)round.rxTxBytes).doubleValue());
                localCPUTicks.addRound(round.assessment, ((Number)round.cpuTicks).doubleValue());
            } else {
                remoteRoundsTime.addRound(round.assessment, ((Number)round.time).doubleValue());
                remoteRoundsResultSize.addRound(round.assessment, ((Number)round.resultSize).doubleValue());
            }
        }
        
        public boolean canInterpolate(final double assessment, final boolean local) {
            throw new RuntimeException("Not implemented!");
        }
        public Double interpolateTime(final double assessment, final boolean local) {
            Interpolator<Double, Double> interpolator = local ? localRoundsTime : remoteRoundsTime;

            return interpolator.interpolate(assessment);
        }
        public Double interpolateRxTx(final double assessment, final boolean local) {
            if (!local) {
                return null;
            }

            return localRoundsRxTx.interpolate(assessment);
        }

        public Double interpolateCPUTicks(final double assessment, final boolean local) {
            if (!local) {
                return null;
            }

            return localCPUTicks.interpolate(assessment);
        }

        //interpolate result size from previous execution
        public Integer getResultSize(final double assessment, final boolean local) {
            if (local) {
                return null;
            }
            Double result_size = remoteRoundsResultSize.interpolate(assessment);
            if (result_size == null) return null;
            return (result_size).intValue();
        }
    }

    class ExecutionRound {
        public final String methodSignature;
        public final double assessment;
        public long time;
        public final boolean local;
        public final long rxTxBytes;
        public final Integer resultSize;
        public final Long cpuTicks;
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(assessment);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ExecutionRound other = (ExecutionRound) obj;
            if (Double.doubleToLongBits(assessment) != Double
                    .doubleToLongBits(other.assessment))
                return false;
            return true;
        }

        public ExecutionRound(final String methodSignature, final double assessment, final long time, final boolean local, final long rxTxBytes, final Integer resultSize, final Long cpuTicks) {
            this.methodSignature = methodSignature;
            this.assessment = assessment;
            this.time = time;
            this.local = local;
            this.rxTxBytes = rxTxBytes;
            this.resultSize = resultSize;
            this.cpuTicks = cpuTicks;
        }

        public String toString() {
            return this.assessment + " | " + this.time;
        }
    }
    
    public void updateCPUUsage(final double usage) {
        final Editor editor = this.preferences.edit();
        editor.putFloat(CPU_USAGE_KEY, (float)usage);
        editor.commit();
    }
    
    public double getCPUUsage() {
        return this.preferences.getFloat(CPU_USAGE_KEY, 0);
    }

    /*public boolean remoteExecution(){
        boolean result=true;
        this.executions.get
        return result;
    }*/
}
