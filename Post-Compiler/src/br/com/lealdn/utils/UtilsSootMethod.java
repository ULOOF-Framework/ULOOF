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
package br.com.lealdn.utils;

import java.util.ArrayList;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;

public class UtilsSootMethod {

	// Offloadable method containing other offloadable methods
//	public static boolean isComplexMethod(SootMethod targetMethod, ArrayList<SootMethod> offloadMethods) {
//		for (SootMethod method : offloadMethods) {
//			if (targetMethod.retrieveActiveBody().toString().contains(method.getSignature()) && !method.getSignature().equals(targetMethod.getSignature())) {
//				return true;
//			}
//		}
//		return false;
//	}
	
	// Return if the method contains at least one element of the methods list
	public static boolean listContainsClassName(ArrayList<SootClass> listClasses, String className) {
		for (SootClass clazz : listClasses) {
			if (clazz.getName().equals(className)) {
				return true;
			}
		}
		return false;
	}
		
	// Return if the method contains at least one element of the methods list
	public static boolean hasIntenalMethods(SootMethod targetMethod, ArrayList<SootMethod> methods) {
		for (SootMethod method : methods) {
			if (targetMethod.retrieveActiveBody().toString().contains(method.getSignature()) && !method.getSignature().equals(targetMethod.getSignature())) {
				return true;
			}
		}
		return false;
	}
	
	// Return if the method contains at least one element of the methods list
	public static boolean hasNotOffloadClasses(SootMethod targetMethod, ArrayList<SootClass> classes) {
		for (SootClass clazz : classes) {
			try {
//				if (targetMethod.retrieveActiveBody().toString().contains(clazz.toString())) {
//					return true;
//				}
				for(Local l : targetMethod.retrieveActiveBody().getLocals()){
					if(l.getType().toString().equals(clazz.getName())){
						return true;
					}
				}
			} catch (RuntimeException e) {
				// DO NOTHING
			}
		}
		return false;
	}
	
	// Return the list of internal offloadable methods of a given method
//	public static ArrayList<SootMethod> getInternalMethods(SootMethod targetMethod,	ArrayList<SootMethod> methods) {
//		ArrayList<SootMethod> internalMethods = new ArrayList<SootMethod>();
//		for (SootMethod method : methods) {
//			if (targetMethod.retrieveActiveBody().toString().contains(method.getSignature()) && !method.getSignature().equals(targetMethod.getSignature())) {
//				internalMethods.add(method);
//			}
//		}
//		return internalMethods;
//	}

	// Calculate the queue level of a given method
//	public static int calculateLevel(SootMethod targetMethod, ArrayList<SootMethod> offloadMethods, ArrayList<NodeQueue> foundMethods) {
//		int level = 0;
//		ArrayList<SootMethod> internals = getInternalMethods(targetMethod, offloadMethods);
//		for (SootMethod internal : internals) {
//			boolean found = false;
//			for (NodeQueue foundMethod : foundMethods) {
//				if (foundMethod.getMethod().getSignature().equals(internal.getSignature())) {
//					level = Math.max(level, foundMethod.getLevel());
//					found = true;
//					break;
//				}
//			}
//			if(!found){
//				return -1;
//			}
//		}
//		return ++level;
//	}

}
