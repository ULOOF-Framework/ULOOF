/*
 * Offloading Server -  ULOOF Project
 *
 * Copyright (C) 2017-2018  Stefano Secci <stefano.secci@cnam.fr>
 * Copyright (C) 2017-2018  Alessio Diamanti <alessio.diama@gmail.com>
 * Copyright (C) 2017-2018  Alessio Mora	<mora.alessio20@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU  General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; If not, see <http://www.gnu.org/licenses/>.
 */

package br.com.lealdn.server;

import android.util.Log;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/*
    This class contains the methods used to remotely perform
    the execution of the methods. Basically, it contains two public methods:
        - executeMethod, that performs the remote invocation;
        - serializeResult, that serializes the execution's result (stored in a map
            with the updated object instance) that has to be sent back to the Android Application.
 */
public class ExecuteMethod {
	final static Pattern methodSignaturePattern = Pattern.compile("<([0-9a-zA-Z.$#]+): ([0-9a-zA-Z.$#\\[\\]]+) ([0-9a-zA-Z.$#<>]+)\\(([0-9a-zA-Z.$#,\\[\\]]*)\\)>");
	final static Pattern fieldSignaturePattern = Pattern.compile("<([0-9a-zA-Z.$#-]+): ([0-9a-zA-Z.$#\\[\\]]+) ([0-9a-zA-Z.$#]+)>");

    private static ArrayList<String> metSig = new ArrayList<String>();
    private static ArrayList<Double> metTime = new ArrayList<Double>();
    final static Map<String, Map<Object, Object>> objectCache = new HashMap<String, Map<Object, Object>>();
    private final static String METHOD_SIG = "--methodSignature";
    public static long desTime=0;
    public static long serTime=0;


    public static Object executeMethod(final String clientIp, final byte[] bytes,File stastFile,Kryo kryo) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        // Method's signature has to be deserialized using Kyro's readObject
        final Input input = new Input(bytes);

        final String methodSignature = kryo.readObject(input, String.class);

        long startDes = System.nanoTime();
        if(!metSig.contains(methodSignature)){
            metSig.add(methodSignature);
        }

        int index;
        index = metSig.indexOf(methodSignature);

		ServerActivity.debug("Method: " + methodSignature);

		// In addition to the method's signature, in the serialized byte stream
        // there is a <String, Object> hashmap that contains the arguments to
        // correctly execute the method, and the method's class instance that is
        // the object on which the method has to be invoked

        final Map<Object, Object> vars = kryo.readObjectOrNull(input, HashMap.class);

        desTime = (long)((System.nanoTime()-startDes)*0.001);
        input.close();

        setStaticFieldsPublic(vars);

        //Here the method is invoked, it is also measured the time needed to execute,
        //in order to inform the offloading engine, reminding that to offload a method
        //it has to be calculated that it is convenient, so this information could
        //change the next forecasts)
        double startTime = System.nanoTime();
        Object obj = invokeMethod(clientIp, methodSignature, vars);
        double finishTime = System.nanoTime();

        //This cache feature is actually disabled, it does not influence the server's behaviour
        saveToCache(clientIp, methodSignature, vars);


        double execTime = (finishTime-startTime);
        if(index>=metTime.size()){
            metTime.add(execTime);
        }else{
           double temp =  metTime.get(index);
           metTime.set(index,(temp+execTime)/2.0);
        }

        return obj;
    }

    // The method execution's result has to be serialized and sent back
    public static ByteArrayOutputStream serializeResult(final Object result, final long startTime, Kryo kryo) {
    	final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        final Output output = new Output(baos, 1024);
        final Map<Object, Object> mapResult = new HashMap<>();
        mapResult.put("r", result);
        double timeElapsed =  (System.nanoTime() - startTime)*0.001;
        System.out.println("timeElapsed= "+timeElapsed);
        System.out.println("timeElapsed= "+(long)timeElapsed);
        mapResult.put("t",(long)timeElapsed);
        mapResult.put("tD",(long)desTime);

        kryo.setAsmEnabled(true);
        if(mapResult != null)
            System.out.println("[Serialize result] result NOT NULL ");

        kryo.writeObject(output, mapResult);

       // mapResult.put("tS",(long)serTime);

        output.close();
        
        return baos;
    }

    private static void setStaticFieldsPublic(final Map<Object, Object> vars)
            throws ClassNotFoundException, NoSuchFieldException,
            SecurityException, IllegalArgumentException, IllegalAccessException {
        for (final Object keyObj : vars.keySet()) {
            final String keyName = (String)keyObj;
            if (keyName.startsWith("field-")) {
                final String fieldSig = keyName.split("-")[1];
                final Matcher matcher = fieldSignaturePattern.matcher(fieldSig);
                try {
                	matcher.find();
                	final Class<?> clazz = Class.forName(matcher.group(1));
                	final Field field = clazz.getDeclaredField(matcher.group(3));
                	field.setAccessible(true);
                    Log.d("UPDATE OLD FIELD", matcher.group(3) + ": " + field.get(matcher.group(3)));
                    field.set(null, vars.get(keyObj));
                    Log.d("UPDATE NEW FIELD", matcher.group(3) + ": " + field.get(matcher.group(3)));
                } catch(Exception state) {
                	continue;
                }
            }
        }
    }

    // Actually it is not used
    private static void saveToCache(final String clientIp, final String methodSignature, final Map<Object, Object> vars) {
        vars.put(METHOD_SIG, methodSignature);
        objectCache.put(clientIp, vars);
    }

    //Remote invocation of the method
    private static Object invokeMethod(final String clientIp, final String methodSignature, final Map<Object, Object> vars) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // Before invoking the method:
        //      - retrieving the method class;
        //      - retrieving the method signature;
        //      - retrieving the argument list;

        final Class<?> clazz = getClassFromSignature(methodSignature);
        final Method method = getMethodFromSignature(clazz, methodSignature);
        method.setAccessible(true);
        final List<Object> argumentList = new ArrayList<>();

        // Cache is actually disabled, no effectiveness
        Map<Object, Object> cache = objectCache.get(clientIp);
        // Nullifies the cache if its not the same method
        if (cache != null && !cache.get(METHOD_SIG).equals(methodSignature)) {
            cache = null;
        }

        // Retrieving the argument list from a serialized map sent from
        // the Android application. The map is a <String, Object> hashmap.
        // The arguments are stored with key arg0, arg1, ....
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            argumentList.add(getArgObjectOrCache(methodSignature, vars, "@arg" + i, cache));
        }

        // The method is invoked on the object retrieved by getThisObjectOrCache
        // That object is the instance of the method's class, sent to the offloading
        // server by the Android application. It is stored in a hashmap with key "this".
        // The result of execution is stored, it has to be returned to the app.
        Object result = method.invoke(getThisObjectOrCache(method, methodSignature, vars, cache), argumentList.toArray(new Object[argumentList.size()]));

        // The method's result and the updated field of the object are stored in
        // a map, to be sent to the app, that will propagate the object's fields
        // modification.
        HashMap<String, Object> map = getFieldsValueFromClass(vars);
        map.put("@res", result);
        return map;
        //old code: return method.invoke(getThisObjectOrCache(method, methodSignature, vars, cache), argumentList.toArray(new Object[argumentList.size()]));
    }

    // Returns the arg object requested
    private static Object getArgObjectOrCache(final String methodSignature, final Map<Object, Object> vars, final String paramName, final Map<Object, Object> cache) {
        // Cache actually always null (disabled basically)
        if (cache != null && vars.get(paramName) == null) {
            return cache.get(paramName);
        }
        return vars.get(paramName);
    }

    // Returns the method's class instance received from the
    // Android application, and stored in a map with key "@this"
    private static Object getThisObjectOrCache(final Method method, final String methodSignature, final Map<Object, Object> vars, final Map<Object, Object> cache) {
        // Cache actually always null
        if (cache != null && vars.get("@this") == null) {
            return cache.get("@this");
        }

        return vars.get("@this");
    }

    // Called by getClassFromSignature and by getMethodFromSignature
    private static String[] getGroupsFromSignature(final String methodSignature) throws ClassNotFoundException {
        final Matcher matcher = methodSignaturePattern.matcher(methodSignature);
        if (matcher != null && matcher.find()) {
            final List<String> groups = new ArrayList<>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                groups.add(matcher.group(i));
            }
            return groups.toArray(new String[groups.size()]);
        }
        return null;
    }

    // Retrieve the Method object from the method's signature,
    // in order to make possible the invocation using Java reflection
    private static Method getMethodFromSignature(final Class<?> rootClass, final String methodSignature) throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        final String[] matcher = getGroupsFromSignature(methodSignature);
        if (matcher != null) {
            final String methodName = matcher[3];
            final String args = matcher[4];
            final List<Class<?>> argsClassList = new ArrayList<>();
            if (args.length() > 0) {
	            for (final String argClass : args.split(",")) {
	                final Class<?> clazz = getClassForName(argClass);
	                argsClassList.add(clazz);
	            }
            }

            return rootClass.getDeclaredMethod(methodName, argsClassList.toArray(new Class[argsClassList.size()]));
        }
        return null;
    }

    // Retrieve values of shared fields
    private static HashMap<String, Object> getFieldsValueFromClass(Map<Object, Object> vars) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (final Object keyObj : vars.keySet()) {
            final String keyName = (String)keyObj;
            if (keyName.startsWith("field-")) {
                final String fieldSig = keyName.split("-")[1];
                final Matcher matcher = fieldSignaturePattern.matcher(fieldSig);
                try {
                    matcher.find();
                    final Class<?> clazz = Class.forName(matcher.group(1));
                    final Field field = clazz.getDeclaredField(matcher.group(3));
                    field.setAccessible(true);
                    Log.d("UPDATE OLD MAP", keyName + ": " + vars.get(keyName));

                    map.put(keyName, field.get(null));
                    Log.d("UPDATE NEW MAP", keyName + ": " + map.get(keyName));
                    System.out.println("PASSATO QUI");
                } catch(Exception state) {
                    System.out.println("Eccezione: "+state.getStackTrace());
                    continue;
                }
            } else if(keyName.startsWith("@this")){
                map.put("@this", vars.get(keyObj));
            }
        }
        return map;
    }

    // Called by getMethodFromSignature
	public static Class<?> getClassForName(String name) throws ClassNotFoundException {
		if ("int".equals(name)) {
			return int.class;
        }
		if ("double".equals(name)) {
			return double.class;
        }
		if ("long".equals(name)) {
			return long.class;
        }
		if ("float".equals(name)) {
			return float.class;
        }
		if ("char".equals(name)) {
			return char.class;
        }
		if ("byte".equals(name)) {
			return byte.class;
        }
		if ("short".equals(name)) {
			return short.class;
        }
        if("boolean".equals(name)){
		    return boolean.class;
        }
		if (name.endsWith("[]")) {
			final String preffix = name.substring(0, name.length()-2);
			if ("int".equals(preffix)) {
				name = "[I";
			} else if ("double".equals(preffix)) {
				name = "[D";
			} else if ("long".equals(preffix)) {
				name = "[J";
			} else if ("float".equals(preffix)) {
				name = "[F";
			} else if ("char".equals(preffix)) {
				name = "[C";
			} else if ("byte".equals(preffix)) {
				name = "[B";
			} else if ("short".equals(preffix)) {
				name = "[S";
			} else {
				name = "[L" + preffix + ";";
			}
		}
        return Class.forName(name);
	}

	// Retrieve the method's class from its signature that is received
    // from the Android application in a serialized String object
    private static Class<?> getClassFromSignature(final String methodSignature) throws ClassNotFoundException {
        final String[] matcher = getGroupsFromSignature(methodSignature);
        if (matcher != null) {
            final String className = matcher[1];
            final Class<?> clazz = Class.forName(className);
            return clazz;
        }
        return null;
    }
}
