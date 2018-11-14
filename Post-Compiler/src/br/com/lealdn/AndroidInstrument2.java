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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import com.googlecode.dex2jar.tools.Dex2jarCmd;

import br.com.lealdn.beans.NodeGraph;
import br.com.lealdn.beans.NodeGraph.ThreeState;
import br.com.lealdn.utils.UtilsSoot;
import soot.Body;
import soot.Local;
import soot.Modifier;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.ApkHandler;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.options.Options;

/*
 * This is the main class of the offloading framewrok's post-compiler.
 */

public class AndroidInstrument2 /*implements Runnable */{
	private String[] args ;
	public AndroidInstrument2(String[] args) {
		this.args = args;
		
	}
	
	/*
	 * This directed graph is used during the method check
	 * to represent the dependency between methods:
	 * a method that calls another method will be its parent
	 * in the graph. This is useful because methods from
	 * un-offloadable classes make methods that call them
	 * un-offloadable too.
	 * */
	static HashMap<String, NodeGraph> graph = new HashMap<String, NodeGraph>();
	
	public static void main(String[] args) {
	//public void run() {
		graph = new HashMap<String, NodeGraph>();
		
		//Setting general properties
		String apkFilePath = args[4];
		String operatingSystem = System.getProperty("os.name");
		String[] apkStrip;
		if (operatingSystem.contains("Mac OS X") || operatingSystem.contains("Linux")){
			apkStrip = apkFilePath.split("/");
		}else{
			apkStrip = apkFilePath.split("\\\\");
		}
	    String filename = apkStrip[apkStrip.length-1].replace(".apk", "");
	    System.out.println(filename);
		makeMainDexTxt(filename);
	    	
		// Soot options setting 
			
		//prefer Android APK files// -src-prec apk
		Options.v().set_src_prec(Options.src_prec_apk);
			
		//output as APK, too//-f J
		Options.v().set_output_format(Options.output_format_dex);
		//Options.v().set_output_format(Options.output_format_class);
		Options.v().set_process_multiple_dex(true);
		Options.v().set_whole_program(true);
		//Options.v().set_output_format(Options.output_format_jasmin);
			
		// Options.v().set_ignore_resolution_errors(true);
	    Options.v().parse(args);
	    Scene.v().releaseActiveHierarchy();
	  
	    Scene.v().loadNecessaryClasses(); //loads all the classes
	    
	    /* Now starts the analysis */
	    
	    
	    /* CheckMethods constructor initializes the object loading from .txt files the 
	     * names of the classes, of the methods and of the libraries that could certainly 
	     * not be offloaded (e.g. Android libraries that manage the input/output).
	     *  
	     */
	    CheckMethods checkMethods = new CheckMethods(Scene.v().getClasses(), apkFilePath);

	    /*
	     * offloadClasses, List of classes whose methods could be offloadable
	     * notOffloadClasse, List of classes whose methods could NOT be offloadable
	     */
		ArrayList<SootMethod> offloadMethods;
	    ArrayList<SootClass> offloadClasses = new ArrayList<SootClass>(); 
	    ArrayList<SootClass> notOffloadClasses = new ArrayList<SootClass>(); 
	    int numberOffloadableClasses = 0;
	    long duration = 0;
	    
	    /*
	     * Retrieving the Main Activity of the Android application
	     */
	    final String mainActivity;
	    if (checkMethods.checkMainActivity()) {
	    	mainActivity = checkMethods.getActivityMain();
	        long startTime = System.nanoTime();
	        
	        //Here offloadClasses and notOffloadClasses are still empty
		    numberOffloadableClasses = analyzeClasses(checkMethods, offloadClasses, notOffloadClasses);
		   
		    /*
		     * analyzeMehods3 returns the list of the offloadable methods.
		     * To do that, it firstly creates the directed graph to represent dependencies of method,
		     * and then it marks the methods inside the graph as offloadable or not-offloadable.
		     * Finally it propagates the not-offloability of a method to its parents.
		     */
		    offloadMethods = analyzeMethods(checkMethods, offloadClasses, notOffloadClasses); 
		        
		    long endTime = System.nanoTime();
		    duration = (endTime - startTime)/1000000;
		      
		    System.out.println("Time: " + duration + "ms");
		    
		    /*
		     * addOffloadingCalls inserts framework bootstrap in onCreate() method as soon as 
		     * the application is created
		     */
		    addOffloadingCalls(checkMethods.getApplication(),checkMethods.getProvider());
	    } else {
	        System.out.println("Error during checking the main activity");
	        offloadMethods = null;
	        mainActivity = null;
	    }
	        
	    /* 
	     * Check for unserializable fields,
	     * e.g.methods that belong to a class that has unserializable fields could not be offloaded
	    */
        List<SootMethod> toRemove = SerializationCheck(offloadMethods);
        
        checkMethods.unserMethod = toRemove.size();
        System.out.println("offloadMethods.sixe= "+ offloadMethods.size());
    	//System.out.println("toRemove.size()= "+toRemove.size());
        
        /*
         * Setting the methods not-offloadable due to the serialization
         * check inside the graph
         */
        for(SootMethod met : toRemove) {
        	//System.out.println("REMOVING "+met.getName()+" due to  UNSERIALIZATION");
        	offloadMethods.remove(met);
        	NodeGraph metNode = graph.get(met.getSignature());
        	metNode.setOffloadable(ThreeState.FALSE);
        }
        
        System.out.println("offloadMethods.size= "+ offloadMethods.size());
        System.out.println("Performing the dependency checking ...");
        /*
         * The graph is scanned once more to mark the new not-offloadable methods
         */
     	for(Entry<String, NodeGraph> node : graph.entrySet()){
     		NodeGraph nodeValue = node.getValue();
     		if(ThreeState.FALSE.equals(nodeValue.getOffloadable())) {
     			checkMethods.scanMethodsDependency(graph, nodeValue);
     		}
     	}
     	System.out.println("Finished the dependency checking.");
     	
     	ArrayList<SootMethod> offloadMethods2 = new ArrayList<SootMethod>();
     	//Retrieving only the signatures of offloadable methods and saving them in offloadMethods2
       	Iterator<Entry<String, NodeGraph>> mapIterator = graph.entrySet().iterator();
        while (mapIterator.hasNext()) {
             Entry<String, NodeGraph> node = mapIterator.next();
             try{
 	            if(ThreeState.TRUE.equals(node.getValue().getOffloadable())) {
 	            	offloadMethods2.add(Scene.v().getMethod(node.getKey()));
 	    		}
 	        } catch(RuntimeException e){
 	    		//e.printStackTrace();
 	    	}
         }
     	
         System.out.println("offloadMethods2.size= "+ offloadMethods2.size());
         
         int serCheckFiltered = offloadMethods.size()-offloadMethods2.size();
         
         //Removing methods that is not useful to offload
         List<SootMethod> offloadMethods3 = cleanUselessOffloading(offloadMethods2,checkMethods);//offloadMethods2.subList(0,offloadMethods2.size()/4);
         System.out.println("offloadMethods3.size= "+ offloadMethods3.size());
    
       
         //------------------------------------HERE SUBLIST IF YOU WANT TO REDUCE METHODS TO OFFLOAD----------------------------------
         List<SootMethod> offloadMethods4 = offloadMethods3;
         //---------------------------------------------------------------------------------------------------------------------------
         
         /*
          * Scanning all the methods to modify their body inflating the logic to invoke the remote call.
          * The original method's body is copied to another method called offloadCopy_methodName() and
          * in the original one is inflated the lofic that calls the offloading framework to:
          * - Check if offloading is enabled;
          * - Check if it is convenient to offload the current method;
          * If so, the logic inflated has to send to the offloading server the parameters of the methods 
          * and the object itself. They are stored an hashmap (that is serialized and sent).
          * Then the inflated logic wait for the result of the execution and propagate the modification
          * to the object fields.
          */
         for (SootMethod method : offloadMethods4){      			
        	 UtilsSoot.modifyMethodToOffload(method);
         }
         System.out.println("Methods to Offload modified");
         
         /*
          * AS useful information, the list of offloadable methods modified is 
          * stored in a .txt file. 
          */
         try{
	      	File f = new File(filename + "_methodlist.txt");
	          PrintWriter writer = new PrintWriter(f, "UTF-8");
	          //List<SootMethod> offloadMethods2 = offloadMethods.subList(2,(offloadMethods.size()/2)-4);
	          for(SootMethod m : offloadMethods4){
	          	writer.println(m.getSignature());
	         }
	          writer.close();
	          PrintWriter writer2 = new PrintWriter(new BufferedWriter(new FileWriter("resultsNew3.txt", true)));
	          int[] classesStatistics = checkMethods.getClassesStatistics();
	          int[] methodsStatistics = checkMethods.getMethodsStatistics();
	          writer2.println(filename+";"+classesStatistics[0]+";"+classesStatistics[1]+";"+classesStatistics[2]+";"+classesStatistics[3]+";"+classesStatistics[4]+";"+classesStatistics[5]+";"+numberOffloadableClasses+";"+methodsStatistics[0]+";"+methodsStatistics[1]+";"+methodsStatistics[2]+";"+methodsStatistics[3]+";"+methodsStatistics[4]+";"+methodsStatistics[5]+";"+methodsStatistics[6]+";"+methodsStatistics[7]+";"+methodsStatistics[8]+";"+serCheckFiltered+";"+offloadMethods3.size());
	          writer2.close();
	      } catch (IOException e) {
	         e.printStackTrace();
	      } 
	      
         System.out.println("cm.methodsNumber= "+checkMethods.methodsNumber);
	     
         PackManager.v().setNumDexes(2);
         PackManager.v().writeOutput(CheckMethods.arrayMaindexClasses,checkMethods.getFolder_workspace() + "apk"+ File.separator +filename+".apk");
	
         List<File> fileList = new ArrayList<File>();
         List<File> fileList2 = new ArrayList<File>();
         HashMap<String, String> paths = new HashMap<String, String>();
         //paths.put("AndroidManifest.xml", "");
         paths.put("assets/unoffloadable.txt", "assets/unoffloadable.txt");
         File newManifest = new File("AndroidManifest.xml");
         File assestsFolder = new File("assets/unoffloadable.txt");
		
         fileList2.add(newManifest);
         fileList.add(assestsFolder);
		
         try {
        	 /*
        	  * Adding to the manifest file of the application the needed permissions
        	  */
        	 manage_permission("sootOutput"+ File.separator + filename+".apk");
        	 ApkHandler apkH = new ApkHandler(new File("sootOutput" + File.separator +filename + ".apk"));
        	 apkH.addFilesToApk(fileList2);
        	 apkH.addFilesToApk(fileList, paths);
         } catch (IOException e) {
        	 // TODO Auto-generated catch block
        	 e.printStackTrace();
         } catch (XmlPullParserException e) {
        	 // TODO Auto-generated catch block
        	 e.printStackTrace();
         }
       
        
		 System.out.println("Now signing the app");
		 
		 /*
		  * Here, the new APK is signed. Remember to provide your own keyphrase and
		  * to update the key path.
		  */
		 signApk(filename);
		 System.out.println("Time: " + duration + "ms");
	
	}
	

	private static boolean isPrimitiveType(SootClass clazz){
		
		if(clazz.getName().equals("java.lang.Integer") || clazz.getName().equals("java.lang.Double") || clazz.getName().equals("java.lang.Float") 
				|| clazz.getName().equals("java.lang.Boolean") || clazz.getName().equals("java.lang.Long") || clazz.getName().equals("java.lang.Character") 
				|| clazz.getName().equals("java.lang.Short") || clazz.getName().equals("java.lang.Byte") || clazz.getName().equals("int")
				|| clazz.getName().equals("double") || clazz.getName().equals("float") || clazz.getName().equals("long") || clazz.getName().equals("char") 
				|| clazz.getName().equals("short") || clazz.getName().equals("short") || clazz.getName().equals("java.lang.String")) { /*String is not strictly primitive, but for our purpose yes*/
			return true;
		}else {
			return false;
		}
	}
	
	private static boolean isSerializableRic(SootClass clazz, SootClass caller,ArrayList<String> metCalledOn) {
		boolean result = true;
		boolean alreadyEncontered= false;
		for(soot.SootField filed : clazz.getFields()) {
			String fieldTypeString = filed.getType().toString();
			if(fieldTypeString.contains("[]")) {
				fieldTypeString = fieldTypeString.replace("[]", "");
			}
			SootClass filedClass = Scene.v().getSootClassUnsafe(fieldTypeString);
			if(metCalledOn.contains(filedClass.getName())) {
				alreadyEncontered = true;
			}
			if(!alreadyEncontered && !isPrimitiveType(filedClass) /*&& filedClass.isApplicationClass()*/) { // if the filed is a class go deeper
				boolean isFieldSerializable = isSerializable(filedClass,new ArrayList<String>());
				if(isFieldSerializable && !filedClass.toString().equals(clazz.toString()) && !filedClass.toString().equals(caller.toString())) {
					metCalledOn.add(filedClass.getName());
					result = isSerializableRic(filedClass,clazz,metCalledOn);
					metCalledOn.remove(filedClass.getName());
				}else {
					result = false;
					break;
				}
			}else { //else stop the recursion
				//return true;
			}
		}
		return result;
	}
	
	
		private static ArrayList<SootMethod> SerializationCheck (List<SootMethod> offloadMethods) {
			ArrayList<SootMethod> toRemove = new ArrayList<>();
	        for(SootMethod met : offloadMethods) {
	        	boolean checkStatic = true;
	        	boolean checkRetClass = true;
	        	boolean checkArguments = true;
	        	SootClass clazz = met.getDeclaringClass();
	        	boolean clazzIsSerializable = isSerializable(clazz,new ArrayList<String>());
	        	if(!clazzIsSerializable) {// method's class has at least one field not serializable
	        		checkStatic = false;
	        		checkRetClass= false;
    				checkArguments= false;
	        		toRemove.add(met);
	        	}else { // deep check
	        	//	System.out.println("Inixio ric");
	        		for(soot.SootField outerfield : clazz.getFields()) { //select each field
	        			String outerfieldTypeString = outerfield.getType().toString();
	        			if(outerfieldTypeString.contains("[]")) {
	        				outerfieldTypeString = outerfieldTypeString.replace("[]", "");
	        			}
	        			SootClass outerFieldClass = Scene.v().getSootClass(outerfieldTypeString);
	        			if(!isPrimitiveType(outerFieldClass)) {
	        				ArrayList<String> metCalledOn = new ArrayList<String>();
	        				metCalledOn.add(outerFieldClass.getName());
	   						boolean filedDeepSerializeble =  isSerializableRic(outerFieldClass,clazz,metCalledOn);
	   						metCalledOn.remove(outerFieldClass.getName());
	   						if(!filedDeepSerializeble) { // classi di var d'istanza giu nella gerarchia non sono serializzabili
	   						toRemove.add(met);
       					 	checkStatic = false;
       					 	checkRetClass= false;
	        				checkArguments= false;
       					 	break; 
       				 		}
	   					}
	        		}
	        	}
	        	if(checkStatic) {
		        	Set<SootField> staticFileds = UtilsSoot.getStaticFieldsFromMethod(met, new HashSet<SootMethod>());
		        	for(SootField statFiled : staticFileds) {
		        		boolean stop = false;
		        		for(String str : CheckMethods.array_libraries){
		        			if(statFiled.getType().toString().startsWith(str)) {
		        				toRemove.add(met);
		        				checkRetClass= false;
		        				checkArguments= false;
		        				stop = true;
		        				break;
		        			}
		        			
		        		}
		        		if(stop) {
	        				break;
	        			}
		        	}
		        }
	        	if(checkRetClass) {
	        		SootClass retClass = Scene.v().getSootClass(met.getReturnType().toString());
	        		boolean isretClassSerializable = isSerializable(retClass,new ArrayList<String>());
		        	if(!isretClassSerializable) {// la classe del metodo ha var d'istanza non serializzabili
		        		toRemove.add(met);
		        		checkArguments= false;
		        	}
	        	}
	        	if(checkArguments) {
	        		for(Type argType: met.getParameterTypes()) {
	        			SootClass argClass = Scene.v().getSootClass(argType.toString());
	        			boolean isretClassSerializable = isSerializable(argClass,new ArrayList<String>());
			        	if(!isretClassSerializable) {// la classe del metodo ha var d'istanza non serializzabili
			        		toRemove.add(met);
			        	}
	        		}
	        	}
	        }
				
	        return toRemove;
		}
		
		
		private static boolean isSerializable(SootClass clazz,ArrayList<String> metCalledOn) { // return false if clazz has fields not serializable by name checking
		
			if(clazz.isInnerClass() || 	clazz.getName().contains("$")) { // inner classes are not serializables as suggested at https://docs.oracle.com/javase/6/docs/platform/serialization/spec/serial-arch.html#7182
				//System.out.println("ECCCCCC");
				/*try {
					System.in.read();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				return false;
			}
			
			if(isPrimitiveType(clazz)) {
				return true;
			}
		//	System.out.println("clazz= "+clazz);
			boolean result = true;
			for(soot.SootField fil : clazz.getFields()) { //select each field
				Type filType = fil.getType();
				String filTypeString = filType.toString();
				if(filTypeString.contains("[]")) {
					filTypeString=filTypeString.replace("[]", "");
				}
				SootClass clau = Scene.v().getSootClassUnsafe(filTypeString);
				for(String str : CheckMethods.array_libraries){ // Check classe diretta
					if(clau.getName().startsWith(str)) {
						result= false;
						break;
					}
				}
				SootClass superClass = null;
				try {
					superClass = clau.getSuperclass();
				}catch(RuntimeException ex) {
					superClass = null;
				}
				while(result && superClass!=null && !superClass.getName().equals("java.lang.Object") && !superClass.getName().equals(clazz.getName())) {
					metCalledOn.add(superClass.getName());
						
					for(String str : CheckMethods.array_libraries){
						if(superClass.getName().startsWith(str)) {// Check direct class
							result= false;
							break;
						}
					}
					if(result) { // then checks fields
						for(SootField fil2: superClass.getFields()) {
							boolean alreadyEncontered = false;
							Type filType2 = fil2.getType();
							String filType2String = filType2.toString();
							if(filType2String.contains("[]")) {
								filType2String = filType2String.replace("[]", "");
							}
							SootClass clau2 = Scene.v().getSootClassUnsafe(filType2String);
							if(metCalledOn.contains(clau2.getName())) {
								alreadyEncontered = true;
							}
							if(!alreadyEncontered) {
								/*First test clau2 name to see if it is between array_libraries*/
								for(String str : CheckMethods.array_libraries){
									if(clau2.getName().startsWith(str)) {// Check classe diretta
										result= false;
										break;
									}
								}
								if(!result) {
									break;
								}
								/* if clau2 name is ok now test its fields */
								metCalledOn.add(clau2.getName());
								metCalledOn.add(clazz.getName());
								boolean resS =isSerializable(clau2,metCalledOn);
								metCalledOn.remove(clau2.getName());
								metCalledOn.remove(clazz.getName());
								if(!resS) {
									result= false;
									break;
								}
							}
						}
					}
					try {
						superClass = superClass.getSuperclass();
					}catch(RuntimeException ex) {
						System.out.println("Primitive type");
						superClass=null;
					}
				}
				
				/*if(result) {
					Chain<SootClass> interfaces = clau.getInterfaces();
					if(!interfaces.isEmpty()) {
						for(SootClass inter : interfaces) {
							boolean interRes= isSerializable(inter, metCalledOn);
							if(!interRes) {
								result = false;
								break;
							}
						}
					}
				}*/
				if(!result) {
					break;
				}
			}
			return result;
		}
	
	private static void manage_permission(String filename) throws IOException, XmlPullParserException{
		File apkFile = new File(filename);
        ProcessManifest pm = new ProcessManifest(apkFile);
      
        AXmlHandler axmlh = pm.getAXml();
        List<AXmlNode> axmlnL = axmlh.getNodesWithTag("uses-permission");
        List<AXmlNode> axmlapp = axmlh.getNodesWithTag("application");
        //List<AXmlNode> axmlnMinSDK = axmlh.getNodesWithTag("uses-sdk");
        
    	boolean exStorFound = false;
    	boolean internetFound = false;
    	boolean accCorseLocFound = false;
    	boolean accNetStateFound = false;
    	boolean accWifiStateFound = false;
    	boolean batStatsFound = false;
    	String nameSpace = "http://schemas.android.com/apk/res/android";
    
    	Iterator<AXmlNode> axmlnLIt = axmlnL.iterator();
    	Iterator<AXmlNode> axmlAppIt = axmlapp.iterator();
    	//pm.getManifest().removeChild(axmlMinSDK.next());
    	
    	while(axmlAppIt.hasNext()) {
    		AXmlNode itNode = axmlAppIt.next();
    		//int type = itNode.getAttribute("debbugable").getType();
    		
    		Map<String, AXmlAttribute<?>> map = itNode.getAttributes();
    		  for (Map.Entry<String, AXmlAttribute<?>> entry : map.entrySet()) {
    	            System.out.println("INTECEPTTTTT: key: " + entry.getKey() + " value: "
    	                    + entry.getValue()+"type="+entry.getValue().getType()+"for method ");
    	        }
    		  @SuppressWarnings("unchecked")
			AXmlAttribute<Object> attr = (AXmlAttribute<Object>) itNode.getAttribute("debuggable");
    		  if(attr == null){
    			  itNode.addAttribute(new AXmlAttribute<String>("debuggable","true",  nameSpace));
    			  attr = (AXmlAttribute<Object>) itNode.getAttribute("debuggable");
    		  }
    		//System.out.println("Valueeee= "+attr.getValue());
    		attr.setValue(new Boolean(true));
    	//	AXmlNode debuggable = new AXmlNode("application", null, axmlh.getDocument().getRootNode());
    		//debuggable.addAttribute(new AXmlAttribute<String>("debuggable","true",  nameSpace));
    	}
    	while(axmlnLIt.hasNext()){
    		AXmlNode itNode = axmlnLIt.next();
    		exStorFound = check_permission(itNode, "android.permission.WRITE_EXTERNAL_STORAGE",exStorFound);
    		internetFound = check_permission(itNode, "android.permission.INTERNET",internetFound);
    		accCorseLocFound = check_permission(itNode, "android.permission.ACCESS_COARSE_LOCATION",accCorseLocFound);
    		accNetStateFound = check_permission(itNode, "android.permission.ACCESS_NETWORK_STATE",accNetStateFound);
    		accWifiStateFound = check_permission(itNode, "android.permission.ACCESS_WIFI_STATE",accWifiStateFound);
    		batStatsFound = check_permission(itNode, "android.permission.BATTERY_STATS",batStatsFound);
    	}
    	
    	if(!exStorFound){
    		System.out.println("*** Adding \"android.permission.WRITE_EXTERNAL_STORAGE\" permission...");
    		add_Permission(axmlh,"android.permission.WRITE_EXTERNAL_STORAGE",nameSpace);
    	}
    	if(!internetFound){
    		System.out.println("*** Adding \"android.permission.INTERNET\" permission...");
    		add_Permission(axmlh,"android.permission.INTERNET",nameSpace);
    	}
    	if(!accCorseLocFound){
    		System.out.println("*** Adding \"android.permission.ACCESS_COARSE_LOCATION\" permission...");
    		add_Permission(axmlh,"android.permission.ACCESS_COARSE_LOCATION",nameSpace);
    	}
    	if(!accNetStateFound){
    		System.out.println("*** Adding \"android.permission.ACCESS_NETWORK_STATE\" permission...");
    		add_Permission(axmlh,"android.permission.ACCESS_NETWORK_STATE",nameSpace);
    	}
    	if(!accWifiStateFound){
    		System.out.println("*** Adding \"android.permission.ACCESS_WIFI_STATE\" permission...");
    		add_Permission(axmlh,"android.permission.ACCESS_WIFI_STATE",nameSpace);
    	}
    	if(!batStatsFound){
    		System.out.println("*** Adding \"android.permission.BATTERY_STATS\" permission...");
    		add_Permission(axmlh,"android.permission.BATTERY_STATS",nameSpace);
    	}
    	//while(axmlMinSDK.hasNext()) {
    /*		AXmlNode internet = new AXmlNode("uses-sdk", null, axmlh.getDocument().getRootNode());
    		internet.addAttribute(new AXmlAttribute<String>("minSdkVersion","21",  nameSpace)); 
    		internet.addAttribute(new AXmlAttribute<String>("targetSdkVersion","25",  nameSpace)); */
    		
    	//	axmlMinSDK.
    	//	axmlMinSDK.next().getAttribute("minSDK")
    	//	System.out.println(axmlMinSDK.next().getAttribute("minSDK"));
    		
    	//}
    	
    	
    	
    	byte[] axmlBA = pm.getOutput();
    	//System.out.println(axmlh.toString());
    	String manifestString = pm.toString();
    	FileOutputStream fileOuputStream = new FileOutputStream("AndroidManifest.xml"); 
    	fileOuputStream.write(axmlBA);
    	fileOuputStream.close();
	}
	
	private static void add_Permission(AXmlHandler axmlh, String permission, String nameSpace)
	{
		AXmlNode internet = new AXmlNode("uses-permission", null, axmlh.getDocument().getRootNode());
		internet.addAttribute(new AXmlAttribute<String>("name",permission,  nameSpace)); 
	}
	
	private static boolean check_permission(AXmlNode itNode, String permission, boolean found)
	{
		if((itNode.getAttribute("name").getValue().equals(permission)) && !found) found = true;
		return found;
	}
	 /*
	  * addOffloadingCalls inserts framework bootstrap in onCreate() method
	  */
	 public static void addOffloadingCalls(String mainActivity) { 
			SootMethod onCreateMethod = null;
			SootClass mainClass = Scene.v().getSootClass(mainActivity);
			for(SootMethod m : mainClass.getMethods()){
				System.out.println(m.getName());
				boolean bundle_param = m.getParameterTypes().contains(RefType.v("android.os.Bundle"));
				boolean return_param = m.getReturnType().equals(VoidType.v());
				if((m.getParameterTypes().size() == 1) && bundle_param && return_param && !m.isPhantom()) {
					onCreateMethod = m; //onCreate() method found
					break;
				}
			}
			
			Body body = onCreateMethod.retrieveActiveBody();
			final JimpleBody jbody = (JimpleBody) body;
			Stmt suc = jbody.getFirstNonIdentityStmt();
			
			SootMethod initializeOffManagerMethod = Scene.v().getMethod("<br.com.lealdn.offload.OffloadingManager: void initialize(android.app.Activity)>");
			SootMethod checkConnectionMethod = Scene.v().getMethod("<br.com.lealdn.offload.Intercept: boolean checkConnection()>");
			
			final List<Value> args = new ArrayList<>();
			for(Local l : body.getLocals()){
				if(l.getType().toString().equals(mainClass.toString())){
					args.add(l);
					break;
				}
			}
			final Unit invokeInitOffMan = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(initializeOffManagerMethod.makeRef(), args));
			final Unit invokeCheckConn = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(checkConnectionMethod.makeRef()));
			body.getUnits().insertBefore(invokeInitOffMan, suc);
			body.getUnits().insertBefore(invokeCheckConn, suc);
			System.out.println(body);
			/*try {
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
	 
	 
	 /*
	  * addOffloadingCalls inserts framework bootstrap in onCreate() method
	  */
	 public static void addOffloadingCalls(String mainActivity, String provider) { 
		 		System.out.println("actm="+mainActivity);
				SootClass mainClass = Scene.v().getSootClass(mainActivity);
			//	mainClass.getSuperclass().getName().equals
			
				SootMethod hookMethod = mainClass.getMethodByNameUnsafe("attachBaseContext");
				if(hookMethod == null) {
					hookMethod  = mainClass.getMethodByNameUnsafe("onCreate");
				}
				
				SootClass superClass = mainClass;
				while(hookMethod == null) { // add the framework startup in the first onCreateMethod found on the hierarchy. ASSUMPTION
					superClass = superClass.getSuperclass();
					//System.out.println("superClass= "+superClass);
					if(superClass.getName().contains("MultiDexApplication")) {
						hookMethod = superClass.getMethodByNameUnsafe("attachBaseContext");
					}else {
						hookMethod = superClass.getMethodByNameUnsafe("onCreate");
					}
				}
				Body body = hookMethod.retrieveActiveBody();
				System.out.println(body);
				/*try {
					System.in.read();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}*/
				final JimpleBody jbody = (JimpleBody) body;
				Unit bootHook = null;
				//OffloadingManager.initialize(arg0, arg1);
				SootMethod initializeOffManagerMethod = Scene.v().getMethod("<br.com.lealdn.offload.OffloadingManager: void initialize(android.content.Context,java.lang.String)>");
				//SootMethod checkConnectionMethod = Scene.v().getMethod("<br.com.lealdn.offload.Intercept: boolean checkConnection()>");
				ArrayList<Type> list = new ArrayList<Type>();
				list.add(superClass.getType());
				//OffloadingManager.i
				final List<Value> args = new ArrayList<>();
				for(Local l : body.getLocals()){
					if(l.getType().toString().equals(superClass.toString())){
						args.add(l);
						break;
					}
				}
				args.add(StringConstant.v(CheckMethods.packageName));
				if(hookMethod.getName().contains("attachBaseContext")) {
					PatchingChain<Unit> units = jbody.getUnits();
					bootHook = units.getLast();
				}else {
					bootHook = jbody.getFirstNonIdentityStmt();
				}
				
				System.out.println("args.get(0).getType()"+args.get(0).getType());
				final Unit invokeInitOffMan = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(initializeOffManagerMethod.makeRef(), args));
				//final Unit invokeCheckConn = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(checkConnectionMethod.makeRef()));
				body.getUnits().insertBefore(invokeInitOffMan, bootHook);
				//body.getUnits().insertBefore(invokeCheckConn, bootHook);

				//System.out.println(body);
		//}
	}


    
    private static int analyzeClasses(CheckMethods cm, ArrayList<SootClass> offloadClasses, ArrayList<SootClass> notOffloadClasses) {
        ArrayList<SootClass> offloadClasses_old;
		ArrayList<SootClass> notOffloadClasses_old = new ArrayList<SootClass>();
        ArrayList<SootClass> notOffloadClasses_new = new ArrayList<SootClass>();
        
        /* 
         * A first coarse check is done, marking as not-offloadable class whose methods
         * are considered not-offloadable a priori because the belong to known classes.
         */
    	System.out.println("Performing native classes checking ...");
    	cm.initialScanClasses(offloadClasses, notOffloadClasses_old);
    	System.out.println("Finished native classes checking.");
    	
    	int count = 1;
    	
    	//----------- After initial Scan Classes
    	//----------- PRINT
    	System.out.println("---------- After Initial Scan Classes - analyzeClasses ---------");
    	for(SootClass clazz : offloadClasses) {
    		if(clazz.getName().contains("java.net"))
    			System.out.println("----------"+ clazz.getName()+ "---------");
		}
    	/*System.out.println("cm.classesNumber"+cm.classesNumber);
    	System.out.println("cm.classesAndroid"+cm.classesAndroid);
    	    	System.out.println("cm.kryoPrefix"+cm.kryoPrefix);
    	    	System.out.println("cm.appclass"+cm.appclass);
    	    	System.out.println("cm.internal_class"+cm.internal_class);
    	    	System.out.println("cm.notOffloadClass"+cm.notOffloadClass);
    	    	System.out.println("cm.notOffloadSuperclass"+cm.notOffloadSuperclass);
    	    	System.out.println("cm.notOffloadIntefaces"+cm.notOffloadIntefaces);
    	    	System.out.println("cm.countTemp"+cm.countTemp);
    	    	System.out.println("cm.runtimeex"+cm.runtimeex);*/
  
		do {
			offloadClasses_old = offloadClasses;
			offloadClasses = new ArrayList<SootClass>();
    		System.out.println("Performing the " + count + " classes dependency checking ...");
    		cm.scanClassesDependency(offloadClasses, offloadClasses_old, notOffloadClasses_old, notOffloadClasses_new);
    		System.out.println("Finished the " + count++ + " classes dependency checking.");
    		notOffloadClasses_old = notOffloadClasses_new;
    		
    		for(SootClass clazz : notOffloadClasses_new) {
				//System.out.println(clazz.getName()+" NOT OFFLOADABLE");
    			//---------------PRINT-------------
    			//if(clazz.getName().contains("InetAddress")) {
    				//System.out.println(clazz.getName()+" NOT OFFLOADABLE");
    			//}
    			//--------------------------------
    			notOffloadClasses.add(clazz);
    		}
    		notOffloadClasses_new = new ArrayList<SootClass>();
    	} while(!notOffloadClasses_old.isEmpty()); // stay in the loop until new not-offloadable methods are found
		
		//----------print
		for(SootClass clazz : offloadClasses) {
				if(clazz.getName().contains("InetAddress"))
				System.out.println(clazz.getName()+" OFFLOAD CLASS analyzeClasses");
		
		}
		//---- print 
		return offloadClasses.size();
	}
    
    private static void printStatistics(CheckMethods cm, int offloadableClassesNumber, int offloadableMethodsNumber) {
    	int[] classesStatistics = cm.getClassesStatistics();
    	System.out.println();
        System.out.println(classesStatistics[0] + " total classes to analyze.");
        System.out.println("Found " + offloadableClassesNumber + " offloadable classes.");
        System.out.println(classesStatistics[1] + " classes rejected because Android classes.");
        System.out.println(classesStatistics[2] + " classes rejected because of internal usage classes.");
        System.out.println(classesStatistics[3] + " classes rejected because extends no-offloadable classes.");
        System.out.println(classesStatistics[4] + " classes rejected because extends no-offloadable superclasses.");
        System.out.println(classesStatistics[5] + " classes rejected because extends no-offloadable interfaces.");
        System.out.println(classesStatistics[6] + " classes rejected because kryo prefix.");
        System.out.println(classesStatistics[7] + " classes rejected because app classLIP.");
        System.out.println(classesStatistics[7] + " classes rejected because native");
    	
    	int[] methodsStatistics = cm.getMethodsStatistics();
    	System.out.println();
        System.out.println(methodsStatistics[0] + " total methods to analyze.");
        System.out.println("Found " + offloadableMethodsNumber + " offloadable methods.");
        System.out.println(methodsStatistics[1] + " methods rejected because of internal usage methods.");
        System.out.println(methodsStatistics[2] + " methods rejected because belong to Android classes.");
        System.out.println(methodsStatistics[3] + " methods rejected because belong to no-offloadable classes.");
        System.out.println(methodsStatistics[4] + " methods rejected because contains into their body calls to no-offloadable classes.");
        System.out.println(methodsStatistics[5] + " methods rejected because contains into their body calls to no-offloadable methods.");
        System.out.println(methodsStatistics[6] + " methods rejected because syntetich.");
        System.out.println(methodsStatistics[7] + " methods rejected because belongs to contentProvMethod.");
        System.out.println(methodsStatistics[8] + " methods rejected because getter method.");
        System.out.println(methodsStatistics[9] + " methods rejected because useless to be offloaded.");
        
        System.out.println(methodsStatistics[10] + " methods rejected because requires serialization of unserializeble obj.");
       // System.out.println(methodsStatistics[9] + " methods rejected because setter method.");
	}
    
    /*
     * analyzeMehods3 returns the list of the offloadable methods.
     * To do that, it firstly creates the directed graph to represent dependencies of method,
     * and then it marks the methods inside the graph as offloadable or not-offloadable.
     * Finally it propagates the not-offloability of a method to its parents.
     */
 	private static ArrayList<SootMethod> analyzeMethods(CheckMethods cm, ArrayList<SootClass> offloadClasses, ArrayList<SootClass> notOffloadClasses) {
       	
 		/*
 		 * initialScanMethods3 creates the graph that represents the dependencies among methods
 		 * considering only the methods that does not belong to the offloading library class
 		 * and marking the methods that belong to not-offloadable classes as not-offloadable
 		 */
     	System.out.println("Performing syntax and internal methods checking ...");
     	cm.initialScanMethods3(graph, offloadClasses, notOffloadClasses);
     	System.out.println("Finished syntax and internal methods checking.");
     	
     	//-------------STAMPA AGGIUNTA ALESSIO MORA---------------
	    System.out.println(" OFFLOAD CLASSES 2");
	    for(SootClass clazz : offloadClasses)
	    	if (clazz.getName().startsWith("java.net.InetAddress"))
	    		System.out.println("---- "+clazz.getName());
	    //-------------------------------------------------------- 	
     	
	    /*
	     * Here all the graph is scanned, in order to propagate the not-offloadability of one method
	     * to its parent using recursion exploiting scanMethodsDependency3().
	     */
     	
     	System.out.println("Performing the dependency checking ...");
     	for(Entry<String, NodeGraph> node : graph.entrySet()){
     		NodeGraph nodeValue = node.getValue();
     	
     		if(ThreeState.FALSE.equals(nodeValue.getOffloadable())) {
     			//------------------- PRINT ADDED by alessio mora -----------------------
     			/*if( node.getKey().contains("getAllByName") ) {
     				System.out.println("^^^^^^^^^^ getAllByName: "+node.getKey());
     				for(SootMethod parent: node.getValue().getParents()) {
     					System.out.println("^^^for^^^^ : "+node.getKey() + " --PARENT: "+parent.getName());
     				}
     			}*/
     				
     			
     			for(SootMethod parent: node.getValue().getParents()) {
     				if( (parent.getName().contains("connectAnd")) || (parent.getName().contains("getByName")) 
     						//|| (parent.getName().contains("getAllByName")) 
     						     						)
     					
     					System.out.println("^^^^^^^^^^ CAUSA: "+node.getKey() + " --PARENT declaring class: "+parent.getDeclaringClass()+ " "+parent.getName());
     				
     					
     			}
     			
     			//-----------------------------------------------------------------------
     			cm.scanMethodsDependency(graph, nodeValue);
     		}
     	}
     	System.out.println("Finished the dependency checking.");
     	
     	//***************************************************************************
     	//------------ FORCING TO MARK SOCKET METHODS AS OFFLOADABLE -----------------
     	/*for(Entry<String, NodeGraph> node : graph.entrySet()){
     		if(node.getKey().contains("connectAndSend")) {
     		if(   (node.getKey().contains("moraa.example.com.socketclientsplitted.SocketUtility: void connect()")) || 
     				(node.getKey().contains("moraa.example.com.socketclientsplitted.SocketUtility: void sendMessage()"))   ) {
     				
     			
					System.out.println("FOUND, key "+node.getKey());
					node.getValue().setOffloadable(ThreeState.TRUE);
					node.getValue().setVisited(true);
     		}
     	
     	}*/
     	//---------------------------------------------------------------------------
        
     	// Retrieving only the signatures of offloadable methods
     	ArrayList<SootMethod> offloadMethods = new ArrayList<SootMethod>();
     	Iterator<Entry<String, NodeGraph>> mapIterator = graph.entrySet().iterator();
         while (mapIterator.hasNext()) {
             Entry<String, NodeGraph> node = mapIterator.next();
             try{
 	            if(ThreeState.TRUE.equals(node.getValue().getOffloadable())) {
 	    			offloadMethods.add(Scene.v().getMethod(node.getKey()));
 	    		}
 	        } catch(RuntimeException e){
 	    		e.printStackTrace();
 	    	}
         }
     	return offloadMethods;
 	}
 	
 	public static void deleteFolder(File folder) {
 	    File[] files = folder.listFiles();
 	    if(files!=null) { //some JVMs return null for empty dirs
 	        for(File f: files) {
 	            if(f.isDirectory()) {
 	                deleteFolder(f);
 	            } else {
 	                f.delete();
 	            }
 	        }
 	    }
 	    folder.delete();
 	}
 	
 	public static void deleteOldDexes() {
 		 // List<File> fileList = new ArrayList<File>();
 			File[] fileList ;
 			File dir = new File("./");
 			FilenameFilter filter = new FilenameFilter() {
 			      public boolean accept(File dir, String name) {
 			          return name.contains(".dex");
 			      }
 			  };
 			 fileList = dir.listFiles(filter);
 			for(File f:fileList) {
 				f.delete();
 			}
 	}
 	
 	private static void clearFromOldRun() {
 		File[] fileList ;
			File dir = new File("./temp");
			 fileList = dir.listFiles();
			for(File f:fileList) {
				f.delete();
			}
 	}
 	
 	public static void makeMainDexTxt(String filename) {
 		clearFromOldRun();
 		
 		
 		System.out.println("Now running dex2jar on the .apk");
 		 Dex2jarCmd.main("-o", "./temp/temp.jar" ,"apk/"+filename+".apk","--force");
 		 
 		/*String argsPreBuild = "--disable-annotation-resolution-workaround ./temp/temp.jar ./temp/temp.jar classIn";
	    String [] argsMultiDex = argsMultiDexString.split("\\s+");
	    com.android.multidex.MainDexListBuilder.main(argsMultiDex);
 		
 		 */

 		//Find the absolute path of the project -> for project external calls 
		String[] workspace_path_array = System.getProperty("user.dir").toString().split("\\\\");
    	String postcompiler_path = "";
    	System.out.println("workspace_path_array.length-1= "+workspace_path_array.length);
    	for (int i=0; i<workspace_path_array.length-1; i++) {
    		postcompiler_path += workspace_path_array[i] + java.io.File.separator;
    	}
    	System.out.println("System.getProperty(\"user.dir\")= "+System.getProperty("user.dir"));
    	postcompiler_path += System.getProperty("user.dir");
    	System.out.println("postcompiler_path= "+postcompiler_path);
    	///media/alessiodiama/Dati/ULOOFLAST/AndroidInstrumentTest/config/mainDexClasses.rules
    	String tempFile = postcompiler_path+java.io.File.separator+"temp"+java.io.File.separator+"temp.jar";
   	 	String outFile = postcompiler_path+java.io.File.separator+"temp"+java.io.File.separator+"out.jar";
   	 	String shrinkedAndroid = postcompiler_path+java.io.File.separator+"lib"+java.io.File.separator+"shrinkedAndroid.jar";
   	 	String rulesFile = postcompiler_path+java.io.File.separator+"config"+java.io.File.separator+"mainDexClasses.rules";
	     try { 
	    	 System.out.println("Now proguard runs");
			 String argsS = "-injars " + tempFile + " -dontwarn -dontnote -forceprocessing -outjars "+ outFile +" -dontoptimize -dontobfuscate -dontpreverify -libraryjars "+ shrinkedAndroid +" -include " + rulesFile ;
	         String [] argsProguard = argsS.split("\\s+");
	         proguard.ProGuard.main(argsProguard);
	     }catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }
	     System.out.println("Now building the mainDexList thanks to an Android build-tool");
	     String argsMultiDexString = "--disable-annotation-resolution-workaround " + outFile + " " + tempFile + " ClassInMainDex";
	     //String argsMultiDexString = "--disable-annotation-resolution-workaround ./temp/out.jar ./temp/temp.jar ClassInMainDex";
	     String [] argsMultiDex = argsMultiDexString.split("\\s+");
	     com.android.multidex.MainDexListBuilder.main(argsMultiDex);
 	}
 	
 	private static  List<SootMethod> cleanUselessOffloading( List<SootMethod> offloadMethods,CheckMethods cm){ // remove getter and setter method as is useless to offload them
 		ArrayList<SootMethod> offloadMethodsNew = new ArrayList<SootMethod>();
 		 for(SootMethod met : offloadMethods) {
 			if( Modifier.isSynthetic(met.getModifiers())) {
 				cm.synthetic_method++;
 			}else  if(CheckMethods.isGetter((JimpleBody)met.retrieveActiveBody())) {
 				cm.getterMethod++;
 			}
 			if(met.getName().startsWith("set") || met.getName().equals("toString") || met.getName().equals("hashCode") 
 					 || met.getDeclaringClass().getName().startsWith("org.apache.commons.math3") || met.getDeclaringClass().getName().startsWith("com.applovin") || met.getDeclaringClass().getName().startsWith("com.smaato") || met.getDeclaringClass().getName().startsWith("com.mopub")
 					|| met.getDeclaringClass().getName().startsWith("com.inmobi") /*|| met.getDeclaringClass().getName().startsWith("sun.reflect")*/ || met.getDeclaringClass().getName().startsWith("com.millennialmedia")
 					|| met.getDeclaringClass().getName().startsWith("pl.droidsonroids") || met.getDeclaringClass().getName().startsWith("com.google.ads") || met.getDeclaringClass().getName().startsWith("com.foursquare")
 					|| met.getDeclaringClass().getName().startsWith("io.fabric") /*|| met.getDeclaringClass().getName().startsWith("com.facebook")*/ ||
 					 met.getDeclaringClass().getName().startsWith("com.squareup") || met.getDeclaringClass().getName().startsWith("com.esotericsoftware.kryo") 
 					|| met.getDeclaringClass().getName().startsWith("okhttp3") || met.getDeclaringClass().getName().startsWith("com.flurry") || met.getDeclaringClass().getName().startsWith("com.crashlytics") 
 					|| met.getDeclaringClass().getName().startsWith("com.comscore") || met.getDeclaringClass().getName().startsWith("sun.reflect")
 				 || (met.getDeclaringClass().getName().startsWith("org.apache.http")) || met.getDeclaringClass().getName().startsWith("org.apache.log4j") 
 				|| (met.getDeclaringClass().getName().startsWith("org.apache.commons.jexl2")) || (met.getDeclaringClass().getName().startsWith("com.google.gson"))
 				|| (met.getDeclaringClass().getName().startsWith("org.objectweb.asm")) || (met.getDeclaringClass().getName().startsWith("org.objenesis"))|| met.getDeclaringClass().getName().startsWith("rx.f$a") || met.getDeclaringClass().getName().startsWith("rx.internal.util.l")
 				|| (met.getName().startsWith("invokeMethodQuietly"))) {
 				
 				 cm.uselessOffload++;
 			 }else {
 				offloadMethodsNew.add(met);
 			 }
 		 }
 		 return offloadMethodsNew;
 	}



	private static void signApk(String apk){
		
		//Here set your own keyphrase and change its path
		String keyphrase = "";
		try {
			String command = "jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore /home/alessio/keystore/release-key.keystore -storepass " 
									+ keyphrase + " " + "./sootOutput/" +apk +".apk "+"key0";

			
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			Process pc = Runtime.getRuntime().exec(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(pc.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {}			
			pc.waitFor();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	
 	
}
