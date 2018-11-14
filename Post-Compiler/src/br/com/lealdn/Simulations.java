/*******************************************************************************
 * Post-Compiler ULOOF Project 
 * 
 * Copyright (C) 2017-2018  Stefano Secci <stefano.secci@cnam.fr>
 * Copyright (C) 2017-2018  Alessio Diamanti <alessio.diama@gmail.com>
 * Copyright (C) 2017-2018  Alessio Mora	<mora.alessio20@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package br.com.lealdn;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import com.android.dex.ClassData.Method;

import br.com.lealdn.beans.NodeGraph;

public class Simulations implements Runnable {
	static String  args2 = "-pp -android-jars /home/alessio/Android/Sdk/platforms -process-dir ./apk/";
	static String args2b = ".apk -debug -process-dir lib/offFramKryoMod.jar -d sootOutput -allow-phantom-refs -p cg enabled:false -w -force-overwrite";
	
	String apkname = "";
	
	public Simulations(String apkname) {
		this.apkname = apkname;
	}
	
	public static void main(String[] args) {
		System.out.println("---------------REMEBER TO REBUILD WITH ANT FOR EACH MODIFICATION IN MAIN PROJECT-------------");
		ArrayList<String> apknamesList = new ArrayList<String>();
		File[] fileList ;
		File dir = new File("./apk");
		fileList = dir.listFiles();
		for(File f:fileList) {
			System.out.println("apk="+f.getName().replace(".apk", ""));
			apknamesList.add(f.getName().replace(".apk", ""));
		}
		for(int i=277;i<apknamesList.size();i++) {
			String apk1 = apknamesList.get(i);
			//String apk2 = apknamesList.get(i+1);
			Simulations sim1 = new Simulations(apk1);
			//Simulations sim2 = new Simulations(apk2);
			Thread t1 = new Thread(sim1);
			t1.start();
			//Thread t2 = new Thread(sim2);
		//	t2.start();
			try {
				t1.join();
			//	t2.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.gc();
		}
	
	}

	@Override
	public void run() {
		 File buildFile = new File("./build.xml");
    	   Project p = new Project();
    	   p.setUserProperty("ant.file", buildFile.getAbsolutePath());
    	   p.setProperty("apk", this.apkname);
    	   p.init();
    	   ProjectHelper helper = ProjectHelper.getProjectHelper();
    	   p.addReference("ant.projectHelper", helper);
    	   helper.parse(p, buildFile);
    	   DefaultLogger defLogg = new DefaultLogger();
    	   defLogg.setErrorPrintStream(System.err);
    	   defLogg.setOutputPrintStream(System.out);
    	   defLogg.setMessageOutputLevel(Project.MSG_VERBOSE);
    	   p.addBuildListener(defLogg);
    	   p.executeTarget(p.getDefaultTarget());
		
	}
	

}
