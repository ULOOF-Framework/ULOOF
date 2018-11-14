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

package br.com.lealdn.offload.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import android.os.Process;
import android.util.Log;
import br.com.lealdn.offload.OffloadingManager;

public class Utils {
    final static Pattern methodSignaturePattern = Pattern.compile("<([0-9a-zA-Z.$#]+): ([0-9a-zA-Z.$#\\[\\]]+) ([0-9a-zA-Z.$#_<>]+)\\(([0-9a-zA-Z.$#,\\[\\]]*)\\)>");
    final static Pattern fieldSignaturePattern = Pattern.compile("<([0-9a-zA-Z.$#-]+): ([0-9a-zA-Z.$#\\[\\]]+) ([0-9a-zA-Z.$#]+)>");
    private static Method methodReadProcFile;
    private final static Logger log = Logger.getLogger(Utils.class);

    /* These are stolen from Process.java which hides these constants. */
    public static final int PROC_SPACE_TERM = (int)' ';
    public static final int PROC_OUT_LONG = 0x2000;
    public static final int INDEX_USER_TIME = 0;
    public static final int INDEX_SYS_TIME = 1;
    public static final int INDEX_TOTAL_TIME = 2;
    
    private static final int[] PROCESS_TOTAL_STATS_FORMAT = new int[] {
        PROC_SPACE_TERM,
        PROC_SPACE_TERM|PROC_OUT_LONG,
        PROC_SPACE_TERM|PROC_OUT_LONG,
        PROC_SPACE_TERM|PROC_OUT_LONG,
        PROC_SPACE_TERM|PROC_OUT_LONG,
        PROC_SPACE_TERM|PROC_OUT_LONG,
        PROC_SPACE_TERM|PROC_OUT_LONG,
        PROC_SPACE_TERM|PROC_OUT_LONG,
    };


    private static final int[] PROCESS_STATS_FORMAT = new int[] {
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM,
        PROC_SPACE_TERM|PROC_OUT_LONG,                  // 13: utime
        PROC_SPACE_TERM|PROC_OUT_LONG                   // 14: stime
    };


    private static final int[] READ_LONG_FORMAT = new int[] {
        PROC_SPACE_TERM|PROC_OUT_LONG
    };


    private static long[] readBuf;
    
    
    static {
        try {
            methodReadProcFile = Process.class.getMethod("readProcFile", String.class,
                    int[].class, String[].class, long[].class, float[].class);
        } catch(NoSuchMethodException e) {
            Log.w("OFFLOADING", "Could not access readProcFile method");
        }
    }


    private static String[] getGroupsFromSignature(final String methodSignature) throws ClassNotFoundException {
        final Matcher matcher = methodSignaturePattern.matcher(methodSignature);
       // System.out.println("matcher[getGruoupFromSig]="+matcher);
        if (matcher != null && matcher.find()) {
            final List<String> groups = new ArrayList<String>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                groups.add(matcher.group(i));
            }
            return groups.toArray(new String[groups.size()]);
        }
        return null;
    }

    public static Method getMethodFromSignature(final Class<?> rootClass, final String methodSignature) throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        final String[] matcher = getGroupsFromSignature(methodSignature);
        if (matcher != null) {
          //  System.out.println("Matcher not null");
            final String methodName = matcher[3];
            final String args = matcher[4];
            System.out.println("methodName= "+methodName);
          //  System.out.println("args.length()= "+args.length());
            final List<Class<?>> argsClassList = new ArrayList<Class<?>>();
           // System.out.println("args.length()= "+args.length());
            if (args.length() > 0) {
                for (final String argClass : args.split(",")) {
                  //  System.out.println("argClass= "+argClass);
                    final Class<?> clazz = getClassForName(argClass);
                   // System.out.println("clazz= "+clazz);
                    Log.w("GETCLASS","CLAZZ: "+clazz.toString()+", METHOD: "+methodSignature);
                    argsClassList.add(clazz);
                }
            }
            Method met =null;
            try{
               /* Method[] metArr =rootClass.getDeclaredMethods();
                for(Method me : metArr){
                    System.out.println("me.getName()= "+me.getName());
                }*/
              // rootClass.get

            met= rootClass.getDeclaredMethod(methodName, argsClassList.toArray(new Class[argsClassList.size()]));
            }catch(NoSuchMethodException e){
                e.printStackTrace();
               // System.out.println("EEEEEEEE");
            }
           // System.out.println(met.getName());
            return met;
        }
        return null;
    }

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
         //   System.out.println("GetClassForName Booooooollllleeeeeaaannnnnnn");
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
       // System.out.println("ASDASSSSSSSSSSSSSSSS"+ Class.forName(name));
        return Class.forName(name);
    }

    public static Class<?> getClassFromSignature(final String methodSignature) throws ClassNotFoundException {
        final String[] matcher = getGroupsFromSignature(methodSignature);
       // System.out.println("matcher[getClassFromSig] ="+matcher);
        if (matcher != null) {
            final String className = matcher[1];
            final Class<?> clazz = Class.forName(className);
            return clazz;
        }
        return null;
    }

    public static double calculateBandwidth(final long startTime, final long endTime, final long size) {
        //log.debug("s: " + size + " | " + (endTime - startTime));
        return (double)size / (((endTime - startTime)));
    }
    
    public static int getSmallestCpuUsage() {
        String tempString = executeTop();

        final int usage = Integer.parseInt(tempString.split(",")[0].split(" ")[1].trim().split("%")[0]);
        if (isAllCpuOnline()) {
            return usage/getNumberOfProcessors();
        }
        else {
            return 0;
        }
    }
    
    public static Double getCpuUsage() {
        long[] cpuStat = new long[7];
        if (getUsrSysTotalTime(cpuStat)) {
            return (cpuStat[INDEX_SYS_TIME]+cpuStat[INDEX_USER_TIME]) / (double) cpuStat[INDEX_TOTAL_TIME];
        }
        return null;
    }
    
    
    /* times should contain seven elements.  times[INDEX_USER_TIME] will be filled
     * with the total user time, times[INDEX_SYS_TIME] will be filled
     * with the total sys time, and times[INDEX_TOTAL_TIME] will have the total
     * time (including idle cycles).  Returns true on success.
     */
    public static boolean getUsrSysTotalTime(long[] times) {
        if(methodReadProcFile == null) return false;
        try {
            if((Boolean)methodReadProcFile.invoke(
                    null, "/proc/stat",
                    PROCESS_TOTAL_STATS_FORMAT, null, times, null)) {
                long usr = times[0] + times[1];
                long sys = times[2] + times[5] + times[6];
                long total = usr + sys + times[3] + times[4];
                times[INDEX_USER_TIME] = usr;
                times[INDEX_SYS_TIME] = sys;
                times[INDEX_TOTAL_TIME] = total;
                return true;
            }
        } catch(IllegalAccessException e) {
            Log.w("OFFLOADING", "Failed to get total cpu usage");
        } catch(InvocationTargetException e) {
            Log.w("OFFLOADING", "Exception thrown while getting total cpu usage");
        }
        return false;
    }


    /* times should contain two elements.  times[INDEX_USER_TIME] will be filled
    * with the user time for this pid and times[INDEX_SYS_TIME] will be filled
    * with the sys time for this pid.  Returns true on sucess.
    */
    public static boolean getPidUsrSysTime(int pid, long[] times) {
    if(methodReadProcFile == null) return false;
    try {
      return (Boolean)methodReadProcFile.invoke(
          null, "/proc/" + pid + "/stat",
          PROCESS_STATS_FORMAT, null, times, null);
    } catch(IllegalAccessException e) {
      Log.w("OFFLOADING", "Failed to get pid cpu usage");
    } catch(InvocationTargetException e) {
      Log.w("OFFLOADING", "Exception thrown while getting pid cpu usage");
    }
        return false;
    }

    /* Returns -1 on failure. */
    public static long readLongFromFile(String file) {
    if(methodReadProcFile == null) return -1;
        try {
          if((Boolean)methodReadProcFile.invoke(
              null, file, READ_LONG_FORMAT, null, readBuf, null)) {
              return readBuf[0];
          }
        } catch(IllegalAccessException e) {
          Log.w("OFFLOADING", "Failed to get pid cpu usage");
        } catch(InvocationTargetException e) {
          Log.w("OFFLOADING", "Exception thrown while getting pid cpu usage");
        }
        return -1L;
    }

    public static long readTcpActivityFromSelf() {
        final int uid = OffloadingManager.getUid();

        return readLongFromFile("/proc/uid_stat/"+ uid + "/tcp_rcv") + readLongFromFile("/proc/uid_stat/"+ uid + "/tcp_snd");
    }

    private static String executeTop() {
        return executeCommand("top -n 1");
    }
    
    private static int getNumberOfProcessors() {
        final int[] n = getBoundsPresentCpus();
        return n[1]-n[0]+1;
    }
    
    private static int[] getBoundsPresentCpus() {
        final String[] presentBounds = executeCommand("cat /sys/devices/system/cpu/present").split("-");    
        return new int[] { Integer.parseInt(presentBounds[0]), Integer.parseInt(presentBounds[1]) };
    }
    
    private static boolean isAllCpuOnline() {
        final int[] bounds = getBoundsPresentCpus();
        for (int i = bounds[0]; i < bounds[1]; i++) {
            final String cpuOnline = executeCommand("cat /sys/devices/system/cpu/cpu1/online").trim();
            if (!"1".equals(cpuOnline)) {
                return false;
            }
        }
        return true;
    }
    
    private static String executeCommand(final String command) {
        java.lang.Process p = null;
        BufferedReader in = null;
        String returnString = null;
        try {
            p = Runtime.getRuntime().exec(command);
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (returnString == null || returnString.contentEquals("")) {
                returnString = in.readLine();
            }
        } catch (IOException e) {
            Log.e("executeTop", "error in getting first line of top");
            e.printStackTrace();
        } finally {
            try {
                in.close();
                p.destroy();
            } catch (IOException e) {
                Log.e("executeTop",
                        "error in closing and destroying top process");
                e.printStackTrace();
            }
        }
        return returnString;
    }
}
