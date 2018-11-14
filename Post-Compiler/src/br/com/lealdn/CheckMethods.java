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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import br.com.lealdn.beans.Activity;
import br.com.lealdn.beans.NodeGraph;
import br.com.lealdn.beans.NodeGraph.ThreeState;
import br.com.lealdn.utils.Utils;
import br.com.lealdn.utils.UtilsSootMethod;
import net.dongliu.apk.parser.ApkParser;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;
import soot.Modifier;

/* 
 * This class provides methods to perform class analysis and method analysis,
 * in order to determine if a method could be considered offloadable
 */

public class CheckMethods {

	static final String ANDROID_PREFIX = "android.";
	static final String GOOGLE_PREFIX = "com.google.android.gms.";
	static final String KRYO_PREFIX = "com.esotericsoftware.kryo.";
	static final String FOLDER_CONF_FILE = "keywords" + java.io.File.separator;
	static final String METHODS_LIFECYCLE = FOLDER_CONF_FILE + "native_methods_lifecycle";
	static final String METHODS_GUI = FOLDER_CONF_FILE + "native_methods_gui";
	static final String METHODS_INOUT = FOLDER_CONF_FILE + "native_methods_inout";
	static final String METHODS_EVENTS = FOLDER_CONF_FILE + "native_methods_events";
	static final String OBJECTS = FOLDER_CONF_FILE + "native_objects";
	static final String LIBRARIES = FOLDER_CONF_FILE + "native_libraries";
	static final String FIRST_DEX_CLASSES = FOLDER_CONF_FILE + "ClassInMainDex.txt";
	static final String FIRST_DEX_FRAMEWORK_CLASSES = FOLDER_CONF_FILE + "frameworkClass.txt";

	static ArrayList<ArrayList<String>> array_methods = new ArrayList<ArrayList<String>>();
	static ArrayList<String> array_objects = new ArrayList<String>();
	static ArrayList<String> array_libraries = new ArrayList<String>();
	static ArrayList<String> arrayMaindexClasses = new ArrayList<String>();
	Chain<SootClass> classes = null;
	
	private Document manifest_xml = null;
	private String apkFile = null;
	private ApkParser apkParser = null;
	private String activityMain = "";
	

	private String provider = "";
	private String application = "";
	static String packageName = "";
	private String folder_workspace;
	
	// Variables for Classes statistics
	int classesNumber; //total number of classes found
	int classesAndroid; //number of Android classes
	int kryoPrefix;
	int appclass;
	int internal_class; //number of internal classes
	int notOffloadClass; //number of classes inside a package that cannot be offloaded
	int notOffloadSuperclass; //number of classes that extend a not-offloadable class
	int notOffloadIntefaces;
	int runtimeex;
	
	// Variables for Methods statistics
	int methodsNumber; //total number of methods found
	int internal_method; //number of internal methods
	int synthetic_method;// number of synthetic method
	int contentProvMethod;
	int belongToAndroidClass; //number of methods that belong to Android class
	int belongToNotOffloadClass; //number of methods that belong to not-offloadable classes
	int hasNotOffloadClasses; //number of methods that contains calls to not-offloadable classes
	int hasNotOffloadMethods; //number of methods that contains calls to not-offloadable methods
	int getterMethod;
	int uselessOffload;
	int unserMethod;
	int countTemp;
	int nativeMethod;
	int noBodyMethod;
	int innerClasses;
	int transientFields;
	int androidUnoff;
	int unoffObj;

	
    /* CheckMethods constructor initializes the object loading from .txt files the 
     * names of the classes, of the methods and of the libraries that could certainly 
     * not be offloaded (e.g. Android libraries that manage the input/output).
     * It also initializs the classes field that contains all the classes in the apk.
     *  
     */
	public CheckMethods(Chain<SootClass> classes, String apkFile) {
		this.classes = classes;
		this.apkFile = apkFile;
		try {
			apkParser = loadApkConfiguration();
			
		} catch (IOException e) {
			e.printStackTrace(); // load apk configuration error
		}
		// Find the project workspace
		String workspace_path = System.getProperty("user.dir");//.toString().split("\\\\");
		/*for (int i=0; i<workspace_path_array.length-1; i++) {
			folder += workspace_path_array[i] + java.io.File.separator;
		}*/
		setFolder_workspace(workspace_path + java.io.File.separator);
		array_methods.add(getConfigFile(getFolder_workspace() + METHODS_LIFECYCLE));
		array_methods.add(getConfigFile(getFolder_workspace() + METHODS_GUI));
		array_methods.add(getConfigFile(getFolder_workspace() + METHODS_INOUT));
		array_methods.add(getConfigFile(getFolder_workspace() + METHODS_EVENTS));
		System.out.println("array_methods.size()= "+array_methods.size());
		array_objects.addAll(getConfigFile(getFolder_workspace() + OBJECTS));
		System.out.println("array_objects.size()= "+array_objects.size());
		array_libraries.addAll(getConfigFile(getFolder_workspace() + LIBRARIES));
		System.out.println("array_libraries.size()= "+array_libraries.size());
		/*try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		arrayMaindexClasses.addAll(getConfigFileN(getFolder_workspace() + FIRST_DEX_FRAMEWORK_CLASSES));
		arrayMaindexClasses.addAll(getConfigFile(getFolder_workspace() + FIRST_DEX_CLASSES));
		arrayMaindexClasses.add("com.upsight.android.internal.persistence.ContentProvider");
		
		 System.out.println("arrayMaindexClasses= "+arrayMaindexClasses.size());
		// statistics initialization
		classesNumber = 0;
		classesAndroid = 0;
		kryoPrefix=0;
		appclass=0;
		internal_class = 0;
		synthetic_method = 0;
		contentProvMethod = 0;
		notOffloadClass = 0;
		notOffloadSuperclass = 0;
		notOffloadIntefaces =0;
		methodsNumber = 0;
		internal_method = 0;
		belongToAndroidClass = 0;
		belongToNotOffloadClass = 0;
		hasNotOffloadClasses = 0;
		hasNotOffloadMethods = 0;
		getterMethod = 0;
		uselessOffload=0;
		unserMethod=0;
		runtimeex=0;
		countTemp=0;
		nativeMethod=0;
		noBodyMethod=0;
		innerClasses=0;
		transientFields=0;
		androidUnoff=0;
		unoffObj=0;
		}

	public String getActivityMain() {
		return activityMain;
	}
	
	public String getApplication() {
		return application;
	}

	public String getProvider() {
		return provider;
	}

	private ArrayList<String> getConfigFile(String filename) {
		Scanner s = null;
		try {
			s = new Scanner(new File(filename));
			s.useDelimiter("\r\n");
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		}
		ArrayList<String> list = new ArrayList<String>();
		while (s.hasNext()) {
			list.add(s.next());
		}
		s.close();
		return list;
	}
	
	private ArrayList<String> getConfigFileN(String filename) {
		Scanner s = null;
		try {
			s = new Scanner(new File(filename));
			s.useDelimiter("\n");
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		}
		ArrayList<String> list = new ArrayList<String>();
		while (s.hasNext()) {
			String temp = s.next();
			temp = temp.replace(".class", "");
			list.add(temp);
		}
		s.close();
		return list;
	}

	public boolean checkMainActivity() {
		List<Activity> listActivity = getActivityList();
		for(Activity activity : listActivity){
			if(activity.isMain()){
				activityMain = activity.getName();
				return true;
			}
		}
//		activityMain = "com.ubercab.client.feature.launch.LauncherActivity";
//		return true;
		return false;
	}
	
	/*
	 * Checking the class names in order to mark as not-offladable the following classes:
	 * - Classes that are inner classes, the classes that have transient fields;
	 * - Classes whose names match with the ones in configuration .txt files;
	 * - The application.package.name.Application class;
	 * Oherwise the current analyzed class is marked as offloadable here.
	 */
	public void initialScanClasses(ArrayList<SootClass> offloadClasses, ArrayList<SootClass> notOffloadClasses) {
		for (final SootClass clazz : classes) {
			

			if(clazz.getName().contains("java.net.InetAddress")) {
				System.out.println("-------InitialScanClasses-------------");
				System.out.println(clazz.getName());
			}
				
			//--------------------------
			if(!clazz.getName().startsWith("br.com.lealdn.offload.") /*&& !clazz.getName().startsWith(KRYO_PREFIX)*/){
				classesNumber++;
				/*if (clazz.getName().startsWith(ANDROID_PREFIX)) {
					classesAndroid++;
					notOffloadClasses.add(clazz); 
				} else*/ if (clazz.isInnerClass() || 	clazz.getName().contains("$")){/* Inner classes in general are not seializables as suggested in https://docs.oracle.com/javase/6/docs/platform/serialization/spec/serial-arch.html#7182 */
					notOffloadClasses.add(clazz); 
					innerClasses++;
				} else if(clazz.getName().equals(application)) {
					notOffloadClasses.add(clazz);
					appclass++;
				} else {
					countTemp++;
					try {
						/*if (internalClass(clazz.getName())) { //anonymous inner class
							notOffloadClasses.add(clazz);
						} else */
						if(hasTransientFiedls(clazz)){
							transientFields++;
							notOffloadClasses.add(clazz);
						}else
						if(containsArrayLibOrArrayObj(clazz)) {
							//-----------PRINT ADDED----------
							if(clazz.getName().contains("java.net.InetAddress"))
								System.out.println("-------InitialScanClasses: containsArrayLibOrArrayObj: "+containsArrayLibOrArrayObj(clazz));
							//--------------------------------
							notOffloadClasses.add(clazz);
						}else {
							offloadClasses.add(clazz);
						}
					} catch(RuntimeException e) {
						runtimeex++;
						offloadClasses.add(clazz);
					}
				}
			}
		}
	}
	
	/*
	 * Check if a class has transient fields. If so, it will be marked as not-offloadable.
	 */
	private boolean hasTransientFiedls(SootClass clazz) {
		boolean result = false;
		for(SootField fil :clazz.getFields()) {
			if(Modifier.isTransient(fil.getModifiers())) { 
				return true;
			}
		}
		return result;
	}
	
	private boolean internalClass(String className) {
		if(className.matches("^.+?\\$\\d{1,10}$")) { //anonymous inner class "classname$number"
			internal_class++;
			return true;
		}
		return false;
	}
	
	
	
	
	/*
	 * Check if the classes used by the application are among the ones
	 * whose methods could not be considered offloaded checking whether
	 * or not their names match with class names in configuration
	 * .txt files.
	 * 
	 */
	private boolean containsArrayLibOrArrayObj(SootClass clazz) {
		/*for (SootMethod method: clazz.getMethods()){
			if (Modifier.isNative(method.getModifiers())) {
				nativeMethod++;
				return true;}
		}*/
		//-------------ADDED
		String controlPrint = "";
		//----------------
		
		// Libraries check both into class and superclass names
		for (String keyword : array_libraries) {
			if (clazz.getName().startsWith(keyword)) {
				androidUnoff++;
				controlPrint = "array_libraries";
				if(clazz.getName().contains("java.net.InetAddress"))
					System.out.println("---settato true: "+controlPrint);
				return true; // non-offlodable keyword
			} else 
			if (clazz.getSuperclass().getName().startsWith(keyword)) {
				notOffloadSuperclass++;
				controlPrint = "superclass";
				if(clazz.getName().contains("java.net.InetAddress"))
					System.out.println("---settato true: "+controlPrint);
				return true; // non-offlodable keyword
			}
		}
		
		for (String keyword : array_objects) {
			if (clazz.getPackageName().startsWith(keyword)) {
				unoffObj++;
				controlPrint = "array_objects";
				if(clazz.getName().contains("java.net.InetAddress"))
					System.out.println("---settato true: "+controlPrint);
				return true; // non-offlodable keyboard
			} else if (clazz.getSuperclass().getName().startsWith(keyword)) {/* + ";") || clazz.getSuperclass().getName().startsWith(keyword + " ") || clazz.getSuperclass().getName().startsWith(keyword + ")")) {
				*/notOffloadSuperclass++;
				controlPrint = "superclass starts with";
				if(clazz.getName().contains("java.net.InetAddress"))
					System.out.println("---settato true: "+controlPrint);
				return true; // non-offlodable keyboard
			}
		}
		 /* 
		  * Check if the class implements interfaces that are considered not-offladable.
		  * java.io.Serializable is considered offloadable, even if it is contained in
		  * the java.io package because it is basically only a marker.
		  * 		  
		  */
		Chain<SootClass> interfaces = clazz.getInterfaces();
		Iterator<SootClass> itr = interfaces.iterator();
		while(itr.hasNext()) {
			SootClass current = itr.next();
			for (String keyword : array_libraries) {
				if( (current.getName().startsWith(keyword)) && ( !current.getName().equals("java.io.Serializable")) ){
					notOffloadIntefaces++;
					controlPrint = "interfaces lib";
					if(clazz.getName().contains("java.net.InetAddress"))
						System.out.println("---settato true: "+controlPrint+ " "+current.getName());
					return true; // non-offlodable keyboard
				}
			}
			for (String keyword : array_objects) {
				if( (current.getName().startsWith(keyword)) && ( !current.getName().equals("java.io.Serializable")) ){
					notOffloadIntefaces++;
					controlPrint = "interfaces objects";
					if(clazz.getName().contains("java.net.InetAddress"))
						System.out.println("---settato true: "+controlPrint+ " "+current.getName());
					return true; // non-offlodable keyboard
				}
			}
		}
		
		return false;
	}
	
	/*S
     * Checks if a method contains at least one not-offloadable method in its body. 
     * In this case the method is marked as not-offloadable as well.
     * 
     */
	public void scanClassesDependency(ArrayList<SootClass> offloadClasses_new, ArrayList<SootClass> offloadClasses_old, ArrayList<SootClass> notOffloadClasses_old, ArrayList<SootClass> notOffloadClasses_new) {
		for (final SootClass clazz : offloadClasses_old) {
			try {
				if (UtilsSootMethod.listContainsClassName(notOffloadClasses_old, clazz.getSuperclass().getName())) {
					notOffloadSuperclass++;
					notOffloadClasses_new.add(clazz);
				} else {
					offloadClasses_new.add(clazz);
				}
			} catch(RuntimeException e) {
				offloadClasses_new.add(clazz);
			}
		}
	}

	
	/*
	 * initialScanMethods3 creates the graph that represents the dependencies among methods
	 * considering only the methods that does not belong to the offloading library class
	 * and marking the methods that belong to not-offloadable classes as not-offloadable
	 */
	
	public void initialScanMethods3(HashMap<String, NodeGraph> graph, ArrayList<SootClass> offloadClasses, ArrayList<SootClass> notOffloadClasses) {
		
		//Scanning all the classes in the application
		for (final SootClass clazz : classes) {
			
			/* We do not want to offload offLib methods but at the same time mark them as un-offloadable: we skip those methods 
			 * to not include them into nodeGraph view.
			 * Methods that are considered not-offloadable are marked setting the flag_offloadable field of
			 * the graph node (see NodeGraph class) as FALSE.
			 * */
			
			//----------------------------------------------
			//if (clazz.getName().startsWith("java.io"))
	    	//	System.out.println("+++++ "+clazz.getName());
			//------------------------------------------------
			if (!clazz.getName().startsWith("br.com.lealdn.offload.") && !clazz.getName().startsWith("de.javakaffee.kryoserializers") && 
					!clazz.getName().startsWith("com.esotericsoftware") && !clazz.getName().startsWith("de.mindpipe.android.logging") && !clazz.isPhantom() /*&& !clazz.getName().startsWith("android.")*/) {
					for (SootMethod method : clazz.getMethods()) {
						try{
							methodsNumber++;
							method.retrieveActiveBody();//useful to throw exception for those method with no body so that to simply skip them
							if(method.isPrivate() && clazz.isApplicationClass()) {
								method.setModifiers(method.getModifiers() + ~Modifier.PRIVATE + Modifier.PUBLIC/*& ~Modifier.FINAL*/);
							}
							ThreeState flag_offloadable ;
							
						/* 
						 * Following if-elseif chain marks methods as not offloadable based on these checks:
						 * - Native methods marked as not-offloadable a priori;	
						 * - Creation of main activity should not be offload;
						 * - Methods whose name contain <clinit> or <init> (syntaxCheck()) should not be offloaded;
						 * - Internal methods (interalMethods()) should not be offloaded;
						 * - If a method belongs to a class marked as not-offloadable, it could not be offloaded 
						 * - If a method name is among "black-lists" of not-offloadable methos is marked as not-offloadable (containsMethods_Objects()).
						 * 
						 * Otherwise, the method is considered offloadable.
						 */
						
						if(Modifier.isNative(method.getModifiers())) {
							nativeMethod++;
							flag_offloadable = ThreeState.FALSE;
						}else if(method.getReturnType().toString().equals(application) || method.getReturnType().toString().equals(activityMain)) { 
							System.out.println("App returned!!!!1**-*-*-*-*********");
							internal_method++;
							flag_offloadable = ThreeState.FALSE;
						}else if(clazz.getName().equals(provider)){
							flag_offloadable = ThreeState.FALSE;
							contentProvMethod++;
						}else if (syntaxCheck(method)) {
							internal_method++;
							flag_offloadable = ThreeState.FALSE;
							//flag_offloadable = ThreeState.TRUE;
						} else if (internalMethod(method)) {
							internal_method++;
							flag_offloadable = ThreeState.FALSE;
						/*} else if (method.getDeclaringClass().getName().startsWith(ANDROID_PREFIX) || 
								method.getDeclaringClass().getName().startsWith(GOOGLE_PREFIX)) {
							belongToAndroidClass++;
							flag_offloadable = ThreeState.FALSE;
						*/} else if (!offloadClasses.contains(method.getDeclaringClass())) { //must be 0 anyway some method pass class level filter. TODO clarify why
							
							belongToNotOffloadClass++;
							flag_offloadable = ThreeState.FALSE;
							
							//----------------------------------------------
							if (method.getName().startsWith("getByName") && (method.getDeclaringClass().getName().contains("InetAddress")))
					    		System.out.println("--------CONTAINS GETDECLARING CLASS " +flag_offloadable);
							//----------------------------------------------
							
							
						} else if (containsMethods_Objects(method)) {
							
							hasNotOffloadMethods++;
							flag_offloadable = ThreeState.FALSE;
							
							//----------------------------------------------
							if (method.getName().startsWith("getByName") && (method.getDeclaringClass().getName().contains("InetAddress")))
					    		System.out.println("--------CONTAINS GETDECLARING CLASS " +flag_offloadable);
							//----------------------------------------------
								
						}else{
							flag_offloadable = ThreeState.TRUE;
						}
						
						
						//The graph is an hash-map whose key is the signature of the method, and its value is a NodeGraph instance object
						NodeGraph node;
						//already present the method signature into the hashmap -> update it with offloading and visited information 
						if(graph.containsKey(method.getSignature())){
							node = graph.get(method.getSignature()); 
						} else {
							node = new NodeGraph(); // the method is new and I create a new object into the hashmap
						}
						//The flag_offloadable field of the NodeGraph object is set according to the else-if chain result value 
						node.setOffloadable(flag_offloadable);
						//----------------------------------------------
						if (method.getName().startsWith("getByName") && (method.getDeclaringClass().getName().contains("InetAddress")))
				    		System.out.println("!!!!!!!!  " +flag_offloadable);
						//----------------------------------------------
						//The NodeGraph object is added to the hash-map that represents the graph
						graph.put(method.getSignature(), node);
						
						//In each node (that represents one method), there is stored the list of the parents of that method
						
						//Retrieving the list of children of the current method
						ArrayList<SootMethodRef> methodChildren = checkMethodChildren(method);
				        for (SootMethodRef child: methodChildren) {
				        	
				       		
				        	//Already present the child into the hashmap -> update the list of its parent with the current method 
							if(graph.containsKey(child.getSignature())){
								node = graph.get(child.getSignature()); 
								node.getParents().add(method);
							} else {
								//The child method is not present into the hashmap -> create a new node, adding the current method as parent
								NodeGraph childNode = new NodeGraph(); 
								childNode.getParents().add(method);
								childNode.setOffloadable(ThreeState.UNSET);
								graph.put(child.getSignature(), childNode);
							}
				        }
					
				} catch( soot.custom.exception.NoSourceMethodException e) {
					noBodyMethod++;
				}
						
				} //for to scan all methods of the current class
			}
		}//for to scan all class
	}
	
	public static boolean isGetter(JimpleBody jbo) {
		if(jbo.getMethod().getName().startsWith("get")) {
			return true;
		}
		/* First try to catch other getter method */
		String[] splitted = jbo.toString().split(";");
		String[] split1 = splitted[1].split("\\s+");//split2[1] = getted object
		String[] split2,split3,split4;
		if(splitted.length > 2) {
			split2= splitted[2].split("\\s+");//split3[2]= returned object
		}else {
			return false;
		}
		String toCompare1 ;
		if(split2.length > 2) {
			toCompare1= split2[2];
		}else {
			return false;
		}
		String toCompare2;
		if(split1.length > 1) {
			toCompare2 = split1[1];
		}else {
			return false;
		}
		if (toCompare1.equals(toCompare2) && split2[1].equals("return"))
			return true;

		if(splitted.length==6) { /* Second try to catch getter */
			split3 = splitted[3].split("\\s+");
			split4 = splitted[4].split("\\s+");
			if(split3.length > 1) {
				toCompare1= split3[1];
			}else {
				return false;
			}
			if(split4.length > 2) {
				toCompare2 = split4[2];
			}else {
				return false;
			}
			if(toCompare1.equals(toCompare2)  && split4[1].equals("return")) {
				//System.out.println(jbo);
			}
			return 	toCompare1.equals(toCompare2)  && split4[1].equals("return");
		}
		return false;
	}
	
	//Retrieving the list of children of the passed method
	private ArrayList<SootMethodRef> checkMethodChildren(SootMethod method) {
		ArrayList<SootMethodRef> methodChildren = new ArrayList<SootMethodRef>();
		try {
			Iterator<Unit> statements = method.retrieveActiveBody().getUnits().snapshotIterator();
			while (statements.hasNext()) {
				Stmt stmt = (Stmt) statements.next();
				if (stmt.containsInvokeExpr() && !methodChildren.contains(stmt.getInvokeExpr().getMethodRef())) {
		        	methodChildren.add(stmt.getInvokeExpr().getMethodRef());
		        }
		    }
		} catch(RuntimeException e){
			// DO NOTHING
		}
		return methodChildren;
	}

/*
	public void scanMethodsDependency(List<SootMethod> listMethods, ArrayList<SootMethod> offloadMethods, ArrayList<SootMethod> notOffloadMethods, ArrayList<SootMethod> notOffloadMethods_temp, ArrayList<SootClass> notOffloadClasses) {
		
		for (final SootMethod method : listMethods) {
			if(!notOffloadMethods.contains(method)) {
				if (UtilsSootMethod.hasIntenalMethods(method, notOffloadMethods)) {
					hasNotOffloadMethods++;
//					System.out.println("Method " + method + " labeled as NOT offloadable");
					notOffloadMethods_temp.add(method);
				} else {
//					System.out.println("Method " + method + " labeled as offloadable");
					offloadMethods.add(method);
				}
			}
		}
	}*/
	
	public void scanMethodsCallGraph(HashMap<SootMethod, Boolean> methods, ArrayList<SootMethod> notOffloadMethods_old, ArrayList<SootMethod> notOffloadMethods_new) {
        Scene.v().setEntryPoints(notOffloadMethods_old);
		PackManager.v().runPacks();
		CallGraph appCallGraph = Scene.v().getCallGraph();
		for(SootMethod method : notOffloadMethods_old) {
			Iterator<Edge> ite = appCallGraph.edgesInto(method);
			while (ite.hasNext()) {
				SootMethod caller = ite.next().src();
				System.out.println(method + " may be called by " + caller);
				if(methods.get(method) == true) {
					notOffloadMethods_new.add(caller);
					methods.put(method, false);
				}
			}
		}
	}
	
	

	
	/*---- ORIGINAL ------------------------------------------------------------------------------
	/*
	 * Recursive check of parents method of a not-offloadable method,
	 * in order to mark them as not-offloadable too.
	 *
	 */
	
	public void scanMethodsDependency(HashMap<String, NodeGraph> graph, NodeGraph nodeGraph) {
		if(!nodeGraph.isVisited()){
			nodeGraph.setVisited(true);
			if(nodeGraph.getOffloadable().equals(ThreeState.TRUE)){
				hasNotOffloadMethods++;
			}
			nodeGraph.setOffloadable(ThreeState.FALSE);
			
			for (SootMethod parent : nodeGraph.getParents()) {
				NodeGraph parentNode = graph.get(parent.getSignature());

				scanMethodsDependency(graph, parentNode);
			}
		}
	}

	
	public List<Activity> getActivityList() {
		packageName = manifest_xml.getDocumentElement().getAttribute("package"); //The package name
		System.out.println("packageName= "+packageName);
		String activityMain = "";
		//String application = "";
		List<Activity> listActivity = new ArrayList<Activity>();
		List<org.w3c.dom.Node> nodeApplication = (List<org.w3c.dom.Node>) Utils.getXMLFields(manifest_xml, "application");
		List<org.w3c.dom.Node> nodeProvider = (List<org.w3c.dom.Node>) Utils.getXMLFields(manifest_xml,"application", "provider");
		
		List<org.w3c.dom.Node> nodes = (List<org.w3c.dom.Node>) Utils.getXMLFields(manifest_xml, "application", "activity");
		
		if(nodeProvider.size()==1) {
			provider = nodeProvider.get(0).getAttributes().getNamedItem("android:name").getNodeValue();
		}else if(nodeProvider.size()!=0){
			int maxPri = 0, indexMax = 0;
			for(int i=0;i<nodeProvider.size();i++) {
				Node node = nodeProvider.get(i);
				if(node.getAttributes().getNamedItem("android:initOrder")!=null && Integer.parseInt(node.getAttributes().getNamedItem("android:initOrder").getNodeValue()) > maxPri){
					maxPri =Integer.parseInt(node.getAttributes().getNamedItem("android:initOrder").getNodeValue());
					indexMax = i;
				}
			}
		provider = nodeProvider.get(indexMax).getAttributes().getNamedItem("android:name").getNodeValue();			
		}
		System.out.println("provider="+provider);
		//array_libraries.add(provider); // add provider class to library class in order to not offload these classes
		
		if(nodeApplication.get(0).getAttributes().getNamedItem("android:name") != null){
			
			application = nodeApplication.get(0).getAttributes().getNamedItem("android:name").getNodeValue();
			if(application.startsWith(".")) {
				application = packageName+application;
			}
			System.out.println("application="+application);
		}else {
			application = "android.app.Application";
			System.out.println("activityMain="+application);
		}
	
		//List<org.w3c.dom.Node> nodes2 = (List<org.w3c.dom.Node>) Utils.getXMLFields(manifest_xml, "manifest");
	//	NamedNodeMap nodeMapA = nodes2.get(0).getAttributes();
	//	String packagE = nodeMapA.getNamedItem("package").getNodeValue();
		
		for (org.w3c.dom.Node node : nodes){
			//System.out.println("node.getNodeName())= "+node.getNodeName());
			NamedNodeMap nodeMap = (NamedNodeMap) ((org.w3c.dom.Node) node).getAttributes();
			String activityName;
			try{
				//nodeMap.get
				if(nodeMap.getNamedItem("android:targetActivity") != null) {
					activityName = nodeMap.getNamedItem("android:targetActivity").getNodeValue();
				} else if(nodeMap.getNamedItem("android:name") != null){
					activityName = nodeMap.getNamedItem("android:name").getNodeValue();
				} else{
					activityName = nodeMap.getNamedItem("name").getNodeValue();
				}
				if(!Utils.isEmpty(activityName)) {
					Activity activity = new Activity();
					activity.setName(activityName);
					
					//For each node I search the main (<action android:name="android.intent.action.MAIN" />)
					if(Utils.isEmpty(activityMain)){ 
						@SuppressWarnings("serial")
						ArrayList<String> fields = new ArrayList<String>(){{add("intent-filter");add("action");}};
						List<org.w3c.dom.Node> results = new ArrayList<org.w3c.dom.Node>();
						Utils.parseXML2((Element) node, results, fields);
						for(org.w3c.dom.Node res : results){
							String nodeValue = null;
							try {
								nodeValue = res.getAttributes().getNamedItem("android:name").getNodeValue();
							} catch (NullPointerException e){
								// DO NOTHING
							}
							if("android.intent.action.MAIN".equals(nodeValue)) {
								activity.setMain(true);
								if(activityName.startsWith(".")) {
									activity.setName(packageName+activityName);
								}
								activityMain = activityName;
								System.out.println("activityName= "+activity.getName());
								//System.in.read();
								break;
							}
						}
					}
					listActivity.add(activity);
				}
			} catch (NullPointerException e) {
				// DO NOTHING -> probably decompiling problem
			//} catch (IOException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			} 
		}
		return listActivity;
	}

	private ApkParser loadApkConfiguration() throws IOException {
		apkParser = new ApkParser(apkFile);
		manifest_xml = Utils.stringToXML(apkParser.getManifestXml());
		return apkParser;
	}

	//Check if the method name is <init> or <clinit> that usually could not be offloaded
	private boolean syntaxCheck(SootMethod method) {
//		Pattern p = Pattern.compile("[~@#%^&:*;<>,/}{)( ]");
//		return p.matcher(method.getName()).find();
		//if(method.getName().contains("<clinit>") ){
		if(method.getName().contains("<init>") || method.getName().contains("<clinit>")){
			return true;
		}
		return false;
	}
	
	//Check if the method is an internal method
	private boolean internalMethod(SootMethod method) {
		for (Type param : method.getParameterTypes()) {
			if ((activityMain).equals(param.toString())|| (application).equals(param.toString())) {
				return true;
			}
		}
		return false;
	}

	private boolean containsMethods_Objects(SootMethod method) {
		
		try {
			if (Utils.isMethodEmpty(method)) {
				return true;
			}
		} catch (RuntimeException e) {
			return true; // No body detected
		}
		String method_code = method.retrieveActiveBody().toString();

		// Methods Check
		for (ArrayList<String> array : array_methods) {
			for (String keyword : array) {
				if (method_code.contains(keyword)) {
					//System.out.println("Method keyword found: " + keyword);
					//System.out.println(method.getName()+" NOT OFFLOADABLE BECAUSE OF KEYWORD");
					return true; // non-offlodable keyboard
				}
			}
		}

		// Libraries Check both int the method body 
		for (String keyword : array_libraries) {
			if (method_code.contains(keyword + ".")) {
				//System.out.println(method.getName()+" NOT OFFLOADABLE BECAUSE OF KEYWORD");

//				System.out.println("Object keyword found: " + keyword);
				return true; // non-offlodable keyboard
			}
			
		}
		
		// Objects Check
		for (String keyword : array_objects) {
			if (method_code.contains(keyword + ";") || method_code.contains(keyword + " ") || method_code.contains(keyword + ")")) {
//						System.out.println("Object keyword found: " + keyword);
				//System.out.println(method.getName()+" NOT OFFLOADABLE BECAUSE OF KEYWORD");

				return true; // non-offlodable keyboard
			}
		}

		return false;
	}
	
	public int[] getClassesStatistics(){
		int[] classesStatistics = new int[6];
		classesStatistics[0] = classesNumber;
		classesStatistics[1] = androidUnoff;
		classesStatistics[2] = unoffObj;
		classesStatistics[3] = notOffloadSuperclass;
		classesStatistics[4] = notOffloadIntefaces;
		classesStatistics[5] = transientFields;
		return classesStatistics;
	}
	
	public int[] getMethodsStatistics(){
		int[] methodsStatistics = new int[9];
		methodsStatistics[0] = methodsNumber;
		methodsStatistics[1] = nativeMethod;
		methodsStatistics[2] = internal_method;
		methodsStatistics[3] = contentProvMethod;
		methodsStatistics[4] = belongToNotOffloadClass;
		methodsStatistics[5] = hasNotOffloadMethods;
		methodsStatistics[6] = synthetic_method;
		methodsStatistics[7] = getterMethod;
		methodsStatistics[8] = uselessOffload;
		return methodsStatistics;
	}
	
	public String getFolder_workspace() {
		return folder_workspace;
	}

	public void setFolder_workspace(String folder_workspace) {
		this.folder_workspace = folder_workspace;
	}
	
	
	/*
     * Checks if a method can be considered offloadable or not-offloadable, using 4 steps: 
     * 
     * - syntax check, that checks if the method name is ok or if contains "strange" chars (internal method) 
     * - internal method check, that checks if the "MainActivity" class is passed as parameters (internal method) 
     * - contains local methods, that check if the method contains at least one of a blacklist word, related to methods that must run locally
     * - dependency check, that checks if a method contains at least one not-offloadable method in its body
     * 
     * If all the previous checks are false -> method can be offloaded
     * 
     */
	/*public void initialScanMethods(ArrayList<SootClass> offloadClasses, ArrayList<SootClass> notOffloadClasses, ArrayList<SootMethod> offloadMethods, ArrayList<SootMethod> notOffloadMethods) {
		for (final SootClass clazz : classes) {
			if (!clazz.getName().startsWith("br.com.lealdn.offload.") && !clazz.getName().equals(activityMain) && !"BuildConfig".equals(clazz.getShortName()) && !clazz.getShortName().equals("R")) {
				for (final SootMethod method : clazz.getMethods()) {
					methodsNumber++;
					if (syntaxCheck(method)) {
						internal_method++;
//						System.out.println("Method " + method + " labeled as NOT offlodable");
						notOffloadMethods.add(method);
					} else if (internalMethod(method)) {
						internal_method++;
//						System.out.println("Method " + method + " labeled as NOT offlodable");
						notOffloadMethods.add(method);
					} else if (!offloadClasses.contains(clazz)) {
						belongToNotOffloadClass++;
//						System.out.println("Method " + method + " labeled as NOT offlodable");
						notOffloadMethods.add(method);
					} else if (UtilsSootMethod.hasNotOffloadClasses(method, notOffloadClasses)) {
						hasNotOffloadClasses++;
//						System.out.println("Method " + method + " labeled as NOT offlodable");
						notOffloadMethods.add(method);
					} else if (containsMethods_Objects(method)) {
						hasNotOffloadMethods++;
//						System.out.println("Method " + method + " labeled as NOT offlodable");
						notOffloadMethods.add(method);
					} else {
//						System.out.println("Method " + method + " labeled as offlodable");
						offloadMethods.add(method);
					}
					
//						if (!offloadClasses.contains(clazz) || syntaxCheck(method) || internalMethod(method) ||  UtilsSootMethod.hasIntenalClasses(method, notOffloadClasses) || containsMethods_Objects(method)) {
////							System.out.println("Method " + method + " labeled as NOT offlodable");
//							notOffloadMethods.add(method);
//						} else {
////							System.out.println("Method " + method + " labeled as offlodable");
//							offloadMethods.add(method);
//						}
				}
			}
		}
	}
	
	public void initialScanMethods2(HashMap<SootMethod, Boolean> methods, ArrayList<SootClass> offloadClasses, ArrayList<SootClass> notOffloadClasses, ArrayList<SootMethod> notOffloadMethods) {
		for (final SootClass clazz : classes) {
			if (!clazz.getName().startsWith("br.com.lealdn.offload.") && !clazz.getName().equals(activityMain) && !"BuildConfig".equals(clazz.getShortName()) && !clazz.getShortName().equals("R")) {
				for (final SootMethod method : clazz.getMethods()) {
					methodsNumber++;
					boolean flag_offloadable;
					if (syntaxCheck(method)) {
						internal_method++;
						notOffloadMethods.add(method);
						flag_offloadable = false;
					} else if (internalMethod(method)) {
						internal_method++;
						notOffloadMethods.add(method);
						flag_offloadable = false;
					} else if (!offloadClasses.contains(clazz)) {
						belongToNotOffloadClass++;
						notOffloadMethods.add(method);
						flag_offloadable = false;
					} else if (UtilsSootMethod.hasNotOffloadClasses(method, notOffloadClasses)) {
						hasNotOffloadClasses++;
						notOffloadMethods.add(method);
						flag_offloadable = false;
					} else if (containsMethods_Objects(method)) {
						hasNotOffloadMethods++;
						notOffloadMethods.add(method);
						flag_offloadable = false;
					} else {
						flag_offloadable = true;
					}
					methods.put(method, flag_offloadable);
				}
			}
		}
	}*/
}
