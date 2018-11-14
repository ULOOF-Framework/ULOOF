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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.Modifier;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.JimpleMethodSource;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;

/*
 * This class provides utility methods to work with Soot library,
 * in particular to modify the offloadable methods' body, basically
 *
 */

public class UtilsSoot {

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
	
	public static void modifyMethodToOffload(final SootMethod method) {
		boolean isNative = Modifier.isNative(method.getModifiers());
		method.getDeclaringClass().setApplicationClass();
		final Body body ;
		List<Local> argLocalList = new ArrayList<Local>();
		if(!isNative) {
			body = method.retrieveActiveBody();
		}else {
			method.setModifiers(method.getModifiers() - Modifier.NATIVE);
			List<Type> lst = new ArrayList<Type>();
			lst.add(VoidType.v());
			//SootMethod met = method.getDeclaringClass().getMethodByNameUnsafe("nativeComputeRoute");
			//met.setDeclaringClass(method.getDeclaringClass());
			//met.setDeclared(true);
			JimpleBody bodyA = Jimple.v().newBody(method);
			
			for(int i=0;i<method.getParameterCount();i++) {
				String name = "l"+i;
				Local arg = Jimple.v().newLocal(name, method.getParameterType(i));
				bodyA.getLocals().add(arg);
				bodyA.getUnits().add(Jimple.v().newIdentityStmt(arg, Jimple.v().newParameterRef(method.getParameterType(i), i)));
				argLocalList.add(arg);
			}
			String name = "l"+5;
			Local arg = Jimple.v().newLocal(name, method.getParameterType(4));
			bodyA.getLocals().add(arg);
			bodyA.getUnits().add(Jimple.v().newAssignStmt(arg, IntConstant.v(5)));
			method.setActiveBody(bodyA);
			//System.out.println("Body"+method.getActiveBody());
			body= method.getActiveBody();
		}
		
		final Set<SootField> staticFields = getStaticFieldsFromMethod(method, new HashSet<SootMethod>());
		turnFieldsPublic(staticFields);
		
		/* Copy the method body to a new method and remove body of original
		* method, except parameter init
		* Append two nop(marks for jump statement) and put the cursor in
		* between */
		
		final SootMethod copyMethod = copyMethod(method, isNative, argLocalList);

		Unit cursor;
		cursor = stripAllUnitsFromMethod(body, method);
	
		final Unit noop = Jimple.v().newNopStmt();
		final Unit noop2 = Jimple.v().newNopStmt();
		body.getUnits().add(noop);
		body.getUnits().add(noop2);
		cursor = noop;
		
		/* 
		 * Adding an hashmap local variable to the original body and get the
		 * hashmap pointers assigned to the variables
		 * puts the cursor at the hashmap initialization
		 * */
		
		final Map<String, Object> mapResult = addMapToBody(body, cursor);
		final Local mapLocal = (Local) mapResult.get("mapLocal");
		final Local startTimeLocal = createStartTimeLocal(body);
		cursor = (Unit) mapResult.get("cursor");

		Random r = new Random(System.currentTimeMillis());
		for (final SootField f : staticFields) {
			cursor = addLocalForFieldAndAddToMap(f, body, mapLocal, cursor, r);
		}
		
		if ((method.getModifiers() & Modifier.STATIC) == 0) {
			cursor = addThisRefToMap(mapLocal, body, cursor);
		}
		//System.out.println("*****************"+body);
		cursor = addArgsRefToMap(mapLocal, body, method, cursor);
		
		
		// Add shared variables to hashmap
		
		cursor = invokeConditionToOffloadIfNecessary(method, mapLocal, startTimeLocal, body, cursor);
		final Unit[] cursorAndTrapStmt = invokeInterceptorWithNeededLocals(method, mapLocal, body, cursor);
		cursor = cursorAndTrapStmt[0];
		addCallsToInstrumentingMethod(method, body, cursor, copyMethod, startTimeLocal, mapLocal, cursorAndTrapStmt[1]);

		body.getUnits().remove(noop);
		body.getUnits().remove(noop2);
		
		Unit lastStmt = body.getUnits().getLast();
		final JimpleBody jbody = (JimpleBody) body;
		final Unit firstStmt = jbody.getFirstNonIdentityStmt();
		
		InvokeExpr invokeExpr = null;
		if ((copyMethod.getModifiers() & Modifier.STATIC) > 0) {
			invokeExpr = Jimple.v().newStaticInvokeExpr(copyMethod.makeRef(), body.getParameterLocals());
		} else {
			invokeExpr = Jimple.v().newVirtualInvokeExpr(body.getThisLocal(), copyMethod.makeRef(),
					body.getParameterLocals());
		}

		//final Unit lastStmt = body.getUnits().getPredOf(body.getUnits().getLast());

		Unit assignOrInvoke = null;
		Unit retStmt = null;
		if (!(copyMethod.getReturnType() instanceof VoidType)) {
			final Local ret = Jimple.v().newLocal("$copyMethodReturnLocal2", copyMethod.getReturnType());
			body.getLocals().add(ret);

			assignOrInvoke = Jimple.v().newAssignStmt(ret, invokeExpr);
			retStmt = Jimple.v().newReturnStmt(ret);
		} else {
			assignOrInvoke = Jimple.v().newInvokeStmt(invokeExpr);
			retStmt = Jimple.v().newReturnVoidStmt();
		}
		body.getUnits().add(retStmt);
		
		body.getUnits().insertAfter(assignOrInvoke, lastStmt);
		lastStmt=body.getUnits().getPredOf(body.getUnits().getLast());;
		
		final Local enabeled = Jimple.v().newLocal("enabeled", BooleanType.v());
		body.getLocals().add(enabeled);
		final Unit assignEnabeld = Jimple.v().newAssignStmt(enabeled, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<br.com.lealdn.offload.OffloadingManager: boolean getEnabled()>").makeRef()));
		body.getUnits().insertBefore(assignEnabeld, firstStmt);
		final Unit ifstmnt = Jimple.v().newIfStmt(Jimple.v().newEqExpr(enabeled,IntConstant.v(0)),lastStmt);
		
		body.getUnits().insertAfter(ifstmnt, assignEnabeld);
		/*if(method.getName().equals("closeQuietly")) {
			System.out.println((JimpleBody)body);
			try {
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		*/
		
			System.out.println((JimpleBody)body);
			System.out.println(copyMethod.retrieveActiveBody());
			try {
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	

	// Remove all units(statements) that is not related to parameter
	// initialization
	private static Unit stripAllUnitsFromMethod(final Body body, SootMethod method) {
		final JimpleBody jbody = (JimpleBody) body;
		List<Value> paramRefs = jbody.getParameterRefs();		
		List<Unit> unitsToRemove = new ArrayList<Unit>();
		Unit suc = jbody.getFirstNonIdentityStmt();
		Unit last = suc;
//		ArrayList<Unit> tempUnits = new ArrayList<Unit>();
		while (suc != null) {
			if (!paramRefs.contains(suc)/* && (tempUnits.isEmpty() || !isSharedInit(suc, method))*/) { // Not a parameter initialization
				unitsToRemove.add(suc);
			}
			suc = body.getUnits().getSuccOf(suc);
		}

		for (Unit toRemove : unitsToRemove) {
			body.getUnits().remove(toRemove);
		}

		return last;
	}


	private static SootMethod copyMethod(final SootMethod method,boolean isNative, List<Local> argLocalList) {
		
			final SootMethod copy;
			//method.getName().
			//method.i
			//if(!method.getName().equals("<init>") && !method.getName().equals("<clinit>")) {
			copy = new SootMethod("$offloadCopy_" + method.getName(), method.getParameterTypes(),
					method.getReturnType(), method.getModifiers());
			/*if(Modifier.isNative(method.getModifiers())){
				copy.setModifiers(copy.getModifiers()+ ~Modifier.NATIVE);
			}*/
			final JimpleBody copyBody = Jimple.v().newBody(copy);
			copy.setActiveBody(copyBody);
			//System.out.println("");
			//if(!Modifier.isNative(method.getModifiers())){
			final Body methodBody = method.retrieveActiveBody();
			method.getDeclaringClass().addMethod(copy);
			copyBody.importBodyContentsFrom(methodBody);
			if(isNative) {
			//	final SootMethod sendAndSerialize = Scene.v().getMethod("<br.com.lealdn.offload.Intercept: java.lang.Object sendAndSerialize(java.lang.String,java.util.Map)>");
				final StaticInvokeExpr stmt = Jimple.v().newStaticInvokeExpr(method.makeRef(), argLocalList);
				copy.retrieveActiveBody().getUnits().add(Jimple.v().newInvokeStmt(stmt));
				copy.retrieveActiveBody().getUnits().add(Jimple.v().newReturnVoidStmt());
			
			}
			if(!isNative) {
				for (final Unit unit : copyBody.getUnits()) {
					cloneUnitAndSwapMethodsIfApplies(unit, method, copy);
				}
			}
			//}
			return copy;
	}
	
	private static void cloneUnitAndSwapMethodsIfApplies(final Unit unit, final SootMethod originalMethod,
			final SootMethod copyMethod) {
		final Unit copyUnit = unit;

		if (copyUnit instanceof JAssignStmt) {
			final JAssignStmt assign = (JAssignStmt) copyUnit;
			if (assign.getRightOp() instanceof InvokeExpr) {
				swapMethods(assign.getInvokeExpr(), originalMethod, copyMethod);
			}
		} else if (copyUnit instanceof JInvokeStmt) {
			final InvokeStmt invoke = (JInvokeStmt) copyUnit;
			swapMethods(invoke.getInvokeExpr(), originalMethod, copyMethod);
		}
	}

	private static void swapMethods(final InvokeExpr invokeExpr, final SootMethod fromMethod,
			final SootMethod toMethod) {
		if (invokeExpr != null && invokeExpr.getMethod().equals(fromMethod)) {
			invokeExpr.setMethodRef(toMethod.makeRef());
		}
	}
	
	/*
	private static void addCallsToInstrumentingMethod2(final SootMethod method, final Body body, final Unit cursor,
			final SootMethod copyMethod, final Local startTimeLocal, final Local mapLocal,
			final Unit sendAndSerializeStmt,Unit afterIf) {
		InvokeExpr invokeExpr = null;
		if ((copyMethod.getModifiers() & Modifier.STATIC) > 0) {
			invokeExpr = Jimple.v().newStaticInvokeExpr(copyMethod.makeRef(), body.getParameterLocals());
		} else {
			invokeExpr = Jimple.v().newVirtualInvokeExpr(body.getThisLocal(), copyMethod.makeRef(),
					body.getParameterLocals());
		}

		final Unit lastStmt = body.getUnits().getPredOf(body.getUnits().getLast());

		Unit assignOrInvoke = null;
		Unit retStmt = null;
		if (!(copyMethod.getReturnType() instanceof VoidType)) {
			final Local ret = Jimple.v().newLocal("$copyMethodReturnLocal", copyMethod.getReturnType());
			body.getLocals().add(ret);

			assignOrInvoke = Jimple.v().newAssignStmt(ret, invokeExpr);
			retStmt = Jimple.v().newReturnStmt(ret);
		} else {
			assignOrInvoke = Jimple.v().newInvokeStmt(invokeExpr);
			retStmt = Jimple.v().newReturnVoidStmt();
		}

		final Local cpuTicksCountLocal = Jimple.v().newLocal("cpuTicksCount", LongType.v());
		body.getLocals().add(cpuTicksCountLocal);

		body.getUnits().add(Jimple.v().newAssignStmt(cpuTicksCountLocal, Jimple.v().newStaticInvokeExpr(
				Scene.v().getMethod("<br.com.lealdn.offload.Intercept: long getCurrentCPUTickCount()>").makeRef())));

		final Local rxTxCountLocal = Jimple.v().newLocal("rxTxCount", soot.ArrayType.v(LongType.v(), 1));
		body.getLocals().add(rxTxCountLocal);

		body.getUnits().add(Jimple.v().newAssignStmt(rxTxCountLocal, Jimple.v().newStaticInvokeExpr(
				Scene.v().getMethod("<br.com.lealdn.offload.Intercept: long[] getRxTxCount()>").makeRef())));

		body.getUnits().add(assignOrInvoke);

		final SootMethod instrumentingMethod = Scene.v().getMethod(
				"<br.com.lealdn.offload.Intercept: void updateMethodRuntime(java.lang.String,long,java.util.Map,long[],long)>");
		final List<Value> args = new ArrayList<>();
		args.add(StringConstant.v(method.getSignature()));
		args.add(startTimeLocal);
		args.add(mapLocal);
		//args.add(rxTxCountLocal);
		//args.add(cpuTicksCountLocal);

		final Unit invokeInstrument = Jimple.v()
				.newInvokeStmt(Jimple.v().newStaticInvokeExpr(instrumentingMethod.makeRef(), args));

		body.getUnits().add(invokeInstrument);
		body.getUnits().add(retStmt);

		body.getTraps().clear();
		//body.getTraps().add(Jimple.v().newTrap(Scene.v().loadClassAndSupport("java.lang.Throwable"),
		//		sendAndSerializeStmt, lastStmt, lastStmt));
		
		
		
		final Local enabeled = Jimple.v().newLocal("enabeled", BooleanType.v());
		body.getLocals().add(enabeled);
		final Unit assignEnabeld = Jimple.v().newAssignStmt(enabeled, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<br.com.lealdn.offload.OffloadingManager: boolean getEnabled()>").makeRef()));
		body.getUnits().insertBefore(assignEnabeld, afterIf);
		final Unit ifstmnt = Jimple.v().newIfStmt(Jimple.v().newEqExpr(enabeled,IntConstant.v(0)),assignOrInvoke);
		final Unit ifstmnt2 = Jimple.v().newIfStmt(Jimple.v().newEqExpr(enabeled,IntConstant.v(0)),body.getUnits().getLast());

		body.getUnits().insertBefore(ifstmnt, afterIf);
		body.getUnits().insertAfter(ifstmnt2, assignOrInvoke);
	}*/
	
	private static void addCallsToInstrumentingMethod(final SootMethod method, final Body body, final Unit cursor,
			final SootMethod copyMethod, final Local startTimeLocal, final Local mapLocal,
			final Unit sendAndSerializeStmt) {
		InvokeExpr invokeExpr = null;
		if ((copyMethod.getModifiers() & Modifier.STATIC) > 0) {
			invokeExpr = Jimple.v().newStaticInvokeExpr(copyMethod.makeRef(), body.getParameterLocals());
		} else {
			invokeExpr = Jimple.v().newVirtualInvokeExpr(body.getThisLocal(), copyMethod.makeRef(),
					body.getParameterLocals());
		}

		final Unit lastStmt = body.getUnits().getPredOf(body.getUnits().getLast());

		Unit assignOrInvoke = null;
		Unit retStmt = null;
		if (!(copyMethod.getReturnType() instanceof VoidType)) {
			final Local ret = Jimple.v().newLocal("$copyMethodReturnLocal", copyMethod.getReturnType());
			body.getLocals().add(ret);

			assignOrInvoke = Jimple.v().newAssignStmt(ret, invokeExpr);
			retStmt = Jimple.v().newReturnStmt(ret);
		} else {
			assignOrInvoke = Jimple.v().newInvokeStmt(invokeExpr);
			retStmt = Jimple.v().newReturnVoidStmt();
		}
		body.getUnits().add(assignOrInvoke);
		final Local cpuTicksCountLocal = Jimple.v().newLocal("cpuTicksCount", LongType.v());
		body.getLocals().add(cpuTicksCountLocal);
		Unit predAssignOrInvoke = body.getUnits().getPredOf(body.getUnits().getPredOf(assignOrInvoke));

		Unit un = Jimple.v().newAssignStmt(cpuTicksCountLocal, Jimple.v().newStaticInvokeExpr(
				Scene.v().getMethod("<br.com.lealdn.offload.Intercept: long getCurrentCPUTickCount()>").makeRef()));
		body.getUnits().insertBefore(un, predAssignOrInvoke);

		final Local rxTxCountLocal = Jimple.v().newLocal("rxTxCount", soot.ArrayType.v(LongType.v(), 1));
		body.getLocals().add(rxTxCountLocal);

		body.getUnits().insertBefore(Jimple.v().newAssignStmt(rxTxCountLocal, Jimple.v().newStaticInvokeExpr(
				Scene.v().getMethod("<br.com.lealdn.offload.Intercept: long[] getRxTxCount()>").makeRef())),un);

		
		final Local finishTimeLocal = Jimple.v().newLocal("finishTimeLocal", LongType.v());
		body.getLocals().add(finishTimeLocal);

		final Unit assignStartTimeLocal = Jimple.v().newAssignStmt(finishTimeLocal, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.System: long nanoTime()>").makeRef()));

		body.getUnits().insertAfter(assignStartTimeLocal, assignOrInvoke);

		final SootMethod instrumentingMethod = Scene.v().getMethod(
				"<br.com.lealdn.offload.Intercept: void updateMethodRuntime(java.lang.String,long,long,java.util.Map,long[],long)>");
		final List<Value> args = new ArrayList<>();
		args.add(StringConstant.v(method.getSignature()));
		args.add(startTimeLocal);
		args.add(finishTimeLocal);
		args.add(mapLocal);
		args.add(rxTxCountLocal);
		args.add(cpuTicksCountLocal);

		final Unit invokeInstrument = Jimple.v()
				.newInvokeStmt(Jimple.v().newStaticInvokeExpr(instrumentingMethod.makeRef(), args));

		body.getUnits().add(invokeInstrument);
		body.getUnits().add(retStmt);

		body.getTraps().clear();
		//body.getTraps().add(Jimple.v().newTrap(Scene.v().loadClassAndSupport("java.lang.Throwable"),
		//		sendAndSerializeStmt, lastStmt, lastStmt));
	}

	private static Local createStartTimeLocal(final Body body, Unit cursor) {
		
		
		Scene.v().loadClassAndSupport("java.lang.Long");

		final SootMethod longConstructor = Scene.v().getMethod("<java.lang.Long: void <init>(long)>");

		final Local startTimeLocalObj = Jimple.v().newLocal("startTimeLocalObj", RefType.v("java.lang.Long"));
		body.getLocals().add(startTimeLocalObj);
		
		final AssignStmt assignStmt = Jimple.v().newAssignStmt(startTimeLocalObj,
				Jimple.v().newNewExpr(RefType.v("java.lang.Long")));
		body.getUnits().insertAfter(assignStmt, cursor);
		
		final InvokeStmt invokeStmt = Jimple.v()
				.newInvokeStmt(Jimple.v().newSpecialInvokeExpr(startTimeLocalObj, longConstructor.makeRef(),LongConstant.v(0)));

		/*final SootMethod hashMapConstructor = Scene.v().getMethod("<java.util.HashMap: void <init>()>");

		final Local mapLocal = Jimple.v().newLocal("mapToSerialize", RefType.v("java.util.HashMap"));
		body.getLocals().add(mapLocal);
		final AssignStmt assignStmt = Jimple.v().newAssignStmt(mapLocal,
				Jimple.v().newNewExpr(RefType.v("java.util.HashMap")));
		body.getUnits().insertAfter(assignStmt, cursor);
		final InvokeStmt invokeStmt = Jimple.v()
				.newInvokeStmt(Jimple.v().newSpecialInvokeExpr(mapLocal, hashMapConstructor.makeRef()));

		body.getUnits().insertAfter(invokeStmt, assignStmt);
		*/

		//java.lang.Long asd = new java.lang.Long(2L);
		//final Local startTimeLocal = Jimple.v().newLocal("startTimeLocal", LongType.v());
		//final Unit assign = Jimple.v().newAssignStmt(startTimeLocal,LongConstant.v(0));
		//body.getLocals().add(startTimeLocal);
		
		body.getUnits().insertAfter(invokeStmt, assignStmt);

		return startTimeLocalObj;
	}
	
	
	
	private static Local createStartTimeLocal(final Body body) {
		final Local startTimeLocal = Jimple.v().newLocal("startTimeLocal", LongType.v());
		body.getLocals().add(startTimeLocal);

		return startTimeLocal;
	}

	public static void turnMethodPublic(final SootMethod method) {
		method.getDeclaringClass().setApplicationClass();
		method.setModifiers(method.getModifiers() & ~Modifier.PRIVATE);
	}

	public static Unit addThisRefToMap(final Local mapLocal, final Body body, final Unit cursor) {
		/*if(body.getMethod().getName().equals("calcSequenceOffset") && method.getDeclaringClass().getName().startsWith("com.xiaomi.hm.health.k.e")) {
			System.out.println(((JimpleBody)body).getThisLocal());
			try {
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		*/
		@SuppressWarnings("serial")
		final InvokeStmt invStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(mapLocal, getMapPutMethod().makeRef(), new ArrayList<Value>() {
					{
						add(StringConstant.v("@this"));
						add(((JimpleBody) body).getThisLocal());
					}
				}));

		body.getUnits().insertAfter(invStmt, cursor);
		return invStmt;
	}

	public static Unit addArgsRefToMap(final Local mapLocal, final Body body, final SootMethod method, Unit cursor) {
		for (int i = 0; i < method.getParameterCount(); i++) {
			if(method.getName().contains("closeQuietly")) {
				//System.out.println("AAAAAAAAAAAAAAAAAAAAA");
			}
			final Local localParam = ((JimpleBody) body).getParameterLocal(i);
			final List<Value> arg = new ArrayList<>();
			final Map<String, Object> retAutobox = UtilsSoot.autoboxParam(localParam, cursor, body);
			arg.add(StringConstant.v("@arg" + i));
			arg.add((Local) retAutobox.get("returnRes"));
			/*if(method.getDeclaringClass().getName().equals("a.a.cg")) {
				System.out.println("method= "+method);
				for(Value val: arg) {
					System.out.println(val);
				}
				try {
					System.in.read();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}*/
			@SuppressWarnings("unchecked")
			List<Unit> cursorList = (List<Unit>) retAutobox.get("cursor");

			final InvokeStmt invStmt = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(mapLocal, getMapPutMethod().makeRef(), arg));
			body.getUnits().insertAfter(invStmt, cursorList.get(cursorList.size() - 1));
			cursor = invStmt;
		}

		return cursor;
	}
	

	public static Unit[] invokeConditionToOffloadIfNecessary2(final SootMethod method, final Local mapLocal,
			final Local startTimeLocal, final Body body, final Unit cursor) {
		final Local shouldOffload = Jimple.v().newLocal("localShouldOffload", BooleanType.v());
		body.getLocals().add(shouldOffload);

		final List<Value> args = new ArrayList<>();
		args.add(StringConstant.v(method.getSignature()));
		args.add(mapLocal);

		Scene.v().forceResolve("br.com.lealdn.offload.Intercept", SootClass.SIGNATURES);
		final Unit assign = Jimple.v().newAssignStmt(shouldOffload,Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<br.com.lealdn.offload.Intercept: boolean shouldOffload(java.lang.String,java.util.Map)>").makeRef(), args));

		body.getUnits().insertAfter(assign, cursor);

		final Unit sucOfAssign = body.getUnits().getSuccOf(assign);
		final Unit assignStartTimeLocal = Jimple.v().newAssignStmt(startTimeLocal, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.System: long currentTimeMillis()>").makeRef()));

		body.getUnits().insertBefore(assignStartTimeLocal, sucOfAssign);

		final Unit ifstmt = Jimple.v().newIfStmt(Jimple.v().newEqExpr(shouldOffload, IntConstant.v(0)),	assignStartTimeLocal);

		body.getUnits().insertAfter(ifstmt, assign);

		return new Unit [] {ifstmt,assignStartTimeLocal};
	}
	
	// insert localshouldoffload local to the body. Insert "if
	// localshouldoffload is 1, record current time" code to the body
	public static Unit invokeConditionToOffloadIfNecessary(final SootMethod method, final Local mapLocal,
			final Local startTimeLocal, final Body body, final Unit cursor) {
		
		/* Let soot load needed Thread class */
		
		Scene.v().loadClassAndSupport("java.lang.Thread");
		
		/* Needed locals declaration */
		
		final Local shouldOffload = Jimple.v().newLocal("localShouldOffload", BooleanType.v());
		final Local decEngineLocal = Jimple.v().newLocal("localDecEngine", RefType.v("br.com.lealdn.offload.DecisionEngine"));
		final Local threadLocal = Jimple.v().newLocal("localThread", RefType.v("java.lang.Thread"));

		body.getLocals().add(shouldOffload);
		body.getLocals().add(decEngineLocal);
		body.getLocals().add(threadLocal);
		
		/* Local variables for needed objects */

		final AssignStmt assignStmtDec = Jimple.v().newAssignStmt(decEngineLocal,
				Jimple.v().newNewExpr(RefType.v("br.com.lealdn.offload.DecisionEngine")));
		final AssignStmt assignStmtThread = Jimple.v().newAssignStmt(threadLocal,
				Jimple.v().newNewExpr(RefType.v("java.lang.Thread")));
		body.getUnits().insertAfter(assignStmtDec, cursor);
		body.getUnits().insertAfter(assignStmtThread, assignStmtDec);
		
		/* DecisionEngine instance creation */
		
		final List<Value> args = new ArrayList<>();
		args.add(StringConstant.v(method.getSignature()));
		args.add(mapLocal);
		Scene.v().forceResolve("br.com.lealdn.offload.Intercept", SootClass.SIGNATURES);// not sure if useful
		ArrayList<Type> argu = new ArrayList<Type>();
		argu.add(Scene.v().getSootClass("java.lang.String").getType());
		argu.add(Scene.v().getSootClass("java.util.Map").getType());
		SootMethodRef cref = Scene.v().makeConstructorRef(Scene.v().getSootClass("br.com.lealdn.offload.DecisionEngine"),argu );
		SpecialInvokeExpr constructorInvokeExpr = Jimple.v().newSpecialInvokeExpr(decEngineLocal,cref,args);
		Unit initStmt = Jimple.v().newInvokeStmt(constructorInvokeExpr);
		
		/* Thread instance creation */

		final ArrayList<Value> args2 = new ArrayList<>();
		args2.add(decEngineLocal);
		ArrayList<Type> argu2 = new ArrayList<Type>();
		argu2.add(Scene.v().getSootClass("java.lang.Runnable").getType());
		SootMethodRef cref2 = Scene.v().makeConstructorRef(Scene.v().getSootClass("java.lang.Thread"),argu2);
		SpecialInvokeExpr constructorInvokeExpr2 = Jimple.v().newSpecialInvokeExpr(threadLocal,cref2,args2);
		Unit initStmt2 = Jimple.v().newInvokeStmt(constructorInvokeExpr2);
		
		//-----------------------MODIFIED-------------------------------
		//SootMethodRef systemOut = Scene.v().getSootClass("void java.io.PrintStream").getMethodByName("out").makeRef();
		//SpecialInvokeExpr systemOutInvkeExpr = Jimple.v().newSpecialInvokeExpr(threadLocal, systemOut);
		
		/* star() method expr creation */
		
		SootMethodRef threadStartRef = Scene.v().getSootClass("java.lang.Thread").getMethodByName("start").makeRef();
		SpecialInvokeExpr startInvkeExpr = Jimple.v().newSpecialInvokeExpr(threadLocal, threadStartRef);
		Unit startStmt = Jimple.v().newInvokeStmt(startInvkeExpr);
		
	/* join() method expr creation */
		
		SootMethodRef threadjoinRef = Scene.v().getSootClass("java.lang.Thread").getMethod("join", new ArrayList<Type>()).makeRef();
		SpecialInvokeExpr joinInvkeExpr = Jimple.v().newSpecialInvokeExpr(threadLocal, threadjoinRef);
		Unit joinStmt = Jimple.v().newInvokeStmt(joinInvkeExpr);

		/* getAnswer() method expr creation */

		SootMethodRef getAnswerRef = Scene.v().getSootClass("br.com.lealdn.offload.DecisionEngine").getMethodByName("getAnswer").makeRef();
		SpecialInvokeExpr getAnswInvokeExpr = Jimple.v().newSpecialInvokeExpr(decEngineLocal, getAnswerRef);
		final Unit assignshouldOffLocal = Jimple.v().newAssignStmt(shouldOffload,getAnswInvokeExpr);
		
		/* Insertion in code of created constructors/method calls */
		
		body.getUnits().insertAfter(initStmt, assignStmtThread);
		body.getUnits().insertAfter(initStmt2, initStmt);
		body.getUnits().insertAfter(startStmt, initStmt2);
		body.getUnits().insertAfter(joinStmt, startStmt);
		body.getUnits().insertAfter(assignshouldOffLocal, joinStmt);
		
		/* if statement placement. This is the if related to shouldOffload */
		
		final Unit sucOfAssign = body.getUnits().getSuccOf(assignshouldOffLocal);
		final Unit assignStartTimeLocal = Jimple.v().newAssignStmt(startTimeLocal, Jimple.v().newStaticInvokeExpr(Scene.v().getMethod("<java.lang.System: long nanoTime()>").makeRef()));
		body.getUnits().insertBefore(assignStartTimeLocal, sucOfAssign);
		final Unit ifstmt = Jimple.v().newIfStmt(Jimple.v().newEqExpr(shouldOffload, IntConstant.v(0)),	assignStartTimeLocal);
		body.getUnits().insertAfter(ifstmt, assignshouldOffLocal);

		return ifstmt;
	}

	private static Type boxPrimitiveType(Type type) {
		if (type instanceof RefLikeType) {
			return type;
		}

		if (type instanceof IntType) {
			return RefType.v("java.lang.Integer");
		} else if (type instanceof FloatType) {
			return RefType.v("java.lang.Float");
		} else if (type instanceof DoubleType) {
			return RefType.v("java.lang.Double");
		} else if (type instanceof LongType) {
			return RefType.v("java.lang.Long");
		} else if (type instanceof CharType) {
			return RefType.v("java.lang.Character");
		} else if (type instanceof ByteType) {
			return RefType.v("java.lang.Byte");
		} else if (type instanceof ShortType) {
			return RefType.v("java.lang.Short");
		} else if (type instanceof BooleanType) {
			return RefType.v("java.lang.Boolean");
		}

		return null;
	}

	// convert primitive local variable to wrapper variable with initialization
	// so it can get boxed
	private static List<Unit> createPrimitiveBoxingMethod(final Local primitive, final Local boxedPrimitive, Unit cursor, final Body body) {
//		for (SootClass clazz : Scene.v().getClasses()) {
//			if (clazz.getName().toLowerCase().contains("kryo")) {
//				for (SootMethod m : clazz.getMethods()) {
//					System.out.println(m);
//				}
//			}
//		}
		
		List<Unit> invokeStmtList = new ArrayList<Unit>();
		try {
			// Primitive or String
			String methodsig= "<"+boxedPrimitive.getType().toString()+": void <init>("+primitive.getType().toString()+")>";
			final SootMethod constructor = Scene.v().getMethod(methodsig);
			final AssignStmt assignStmt = Jimple.v().newAssignStmt(boxedPrimitive, Jimple.v().newNewExpr((RefType)boxedPrimitive.getType()));
		    body.getUnits().insertAfter(assignStmt, cursor);
			
		    List<Value> argsForConstructor = new ArrayList<Value>();
		    argsForConstructor.add(primitive);
		    final InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(boxedPrimitive, constructor.makeRef(), argsForConstructor));
			body.getUnits().insertAfter(invokeStmt, assignStmt);
			invokeStmtList.add(invokeStmt);
		} catch (RuntimeException e) {
			// Object, Object[], List, etc.
			final AssignStmt assignStmt = Jimple.v().newAssignStmt(boxedPrimitive, primitive);
			body.getUnits().insertAfter(assignStmt, cursor);
			invokeStmtList.add(assignStmt);

//			CORRECT FOR OBJECT (NOT LIST OR ARRAY)
//				String methodsig1 = "<" + boxedPrimitive.getType().toString() + ": void <init>()>";
//				final SootMethod constructor1 = Scene.v().getMethod(methodsig1);
//				final AssignStmt assignStmt = Jimple.v().newAssignStmt(boxedPrimitive, Jimple.v().newNewExpr((RefType)boxedPrimitive.getType()));
//			    body.getUnits().insertAfter(assignStmt, cursor);
//				
//			    final InvokeStmt invokeStmt1 = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(boxedPrimitive, constructor1.makeRef(), new ArrayList<Value>()));
//				String methodsig2 = "<" + boxedPrimitive.getType().toString() + ": " + primitive.getType().toString() + " clone()>";
//				final SootMethod constructor2 = Scene.v().getMethod(methodsig2);
//				List<Value> argsForConstructor = new ArrayList<Value>();
//				final InvokeStmt invokeStmt2 = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(primitive, constructor2.makeRef(), argsForConstructor));
//
//				body.getUnits().insertAfter(invokeStmt1, assignStmt);
//				body.getUnits().insertAfter(invokeStmt2, invokeStmt1);
//				invokeStmtList.add(invokeStmt1);
//				body.getUnits().insertAfter(invokeStmt2, assignStmt);
//				invokeStmtList.add(invokeStmt2);
		}
		// returning invoke statement which initialize wrapper object
		return invokeStmtList;
	}

	// TODO: Complete function
	private static SootMethod getPrimitiveMethodForBoxedLocal(final RefType type) {
		switch (type.getClassName()) {
		case "java.lang.Integer":
			return Scene.v().getMethod("<java.lang.Integer: int intValue()>");
		case "java.lang.Double":
			return Scene.v().getMethod("<java.lang.Double: double doubleValue()>");
		case "java.lang.Float":
			return Scene.v().getMethod("<java.lang.Float: float floatValue()>");
		case "java.lang.Boolean":
			return Scene.v().getMethod("<java.lang.Boolean: boolean booleanValue()>");
		case "java.lang.Long":
			return Scene.v().getMethod("<java.lang.Long: long longValue()>");
		case "java.lang.Character":
			return Scene.v().getMethod("<java.lang.Character: char charValue()>");
		case "java.lang.Short":
			return Scene.v().getMethod("<java.lang.Short: short shortValue()>");
		case "java.lang.Byte":
			return Scene.v().getMethod("<java.lang.Byte: byte byteValue()>");
		}

		return null;
	}

	private static Map<String, Object> autoboxParam(final Local local, Unit cursor, final Body body) {
		Map<String, Object> ret = new HashMap<String, Object>();
		if (local.getType() instanceof RefLikeType || local.getType() instanceof RefType) {
			ret.put("returnRes", local);
			List<Unit> invokeStmtList = new ArrayList<Unit>();
			invokeStmtList.add(cursor);
			ret.put("cursor", invokeStmtList);
			return ret;
		}

		final Local returnPrimitive = Jimple.v().newLocal("autoboxing-" + local.getName(),
				boxPrimitiveType(local.getType()));
		body.getLocals().add(returnPrimitive);
		List<Unit> cursorList = createPrimitiveBoxingMethod(local, returnPrimitive, cursor, body);

		Map<String, Object> autoboxResult = new HashMap<String, Object>();
		autoboxResult.put("returnRes", returnPrimitive);
		autoboxResult.put("cursor", cursorList);
		return autoboxResult;
	}

	private static Map<String, Object> autoboxPrimitiveIfNecessary(final SootMethod returnMethod,
			final Local sendAndSerializeLocal, final Body body, Unit cursor) {
		final Map<String, Object> autoboxResult = new HashMap<String, Object>();
		final Type castedTypeFromReturn = boxPrimitiveType(returnMethod.getReturnType());
		final Local functionResult = Jimple.v().newLocal("functionResult", castedTypeFromReturn);

		body.getLocals().add(functionResult);

		final AssignStmt castStmt = Jimple.v().newAssignStmt(functionResult,
				Jimple.v().newCastExpr(sendAndSerializeLocal, castedTypeFromReturn));

		body.getUnits().insertAfter(castStmt, cursor);
		
		if (returnMethod.getReturnType() instanceof RefLikeType) {
			autoboxResult.put("returnRes", functionResult);
			autoboxResult.put("cursor", castStmt);
		
		} else {
			final Local returnPrimitive = Jimple.v().newLocal("returnPrimitive", returnMethod.getReturnType());
			body.getLocals().add(returnPrimitive);

			AssignStmt assignStmt = Jimple.v().newAssignStmt(returnPrimitive, Jimple.v().newVirtualInvokeExpr(functionResult, getPrimitiveMethodForBoxedLocal((RefType) functionResult.getType()).makeRef()));
			body.getUnits().insertAfter(assignStmt, castStmt);
			
			autoboxResult.put("returnRes", returnPrimitive);
			autoboxResult.put("cursor", assignStmt);
		}

		return autoboxResult;
	}

	public static Unit[] invokeInterceptorWithNeededLocals(final SootMethod method, final Local mapLocal, final Body body, Unit cursor) {
		final boolean hasReturn = !(method.getReturnType() instanceof VoidType);
		
		final SootMethod sendAndSerialize = Scene.v().getMethod("<br.com.lealdn.offload.Intercept: java.lang.Object sendAndSerialize(java.lang.String,java.util.Map)>");
		final Local returnRes = Jimple.v().newLocal("returnFromIntercept", sendAndSerialize.getReturnType());
		body.getLocals().add(returnRes);

		final List<Value> args = new ArrayList<>();
//		Local objInstance;
//		if(Modifier.isStatic(method.getModifiers())) { //if method is static the object reference is null because it's invocated on the client at runtime  
//			args.add(NullConstant.v());
//		} else { //if method is not static the object reference I send this instance
//			objInstance= Jimple.v().newLocal("ThisInstance", RefType.v(method.getDeclaringClass()));
//			args.add(objInstance);
//		}
		args.add(StringConstant.v(method.getSignature()));
		args.add(mapLocal);

		final AssignStmt stmt = Jimple.v().newAssignStmt(returnRes,
				Jimple.v().newStaticInvokeExpr(sendAndSerialize.makeRef(), args));

		body.getUnits().insertAfter(stmt, cursor);

		if (hasReturn) {
			final Map<String, Object> autoboxResult = UtilsSoot.autoboxPrimitiveIfNecessary(method, returnRes, body,
					stmt);
			final Local returnVariable = (Local) autoboxResult.get("returnRes");
			final Unit cursorSendAndSerialize = (Unit) autoboxResult.get("cursor");

			final ReturnStmt retStmt = Jimple.v().newReturnStmt(returnVariable);
			body.getUnits().insertAfter(retStmt, cursorSendAndSerialize);
			return new Unit[] { retStmt, stmt };
		} else {
			final Unit retStmt = Jimple.v().newReturnVoidStmt();
			body.getUnits().insertAfter(retStmt, stmt);

			return new Unit[] { retStmt, stmt };
		}
	}

	// adds a hashmap local variable to body and initialize it at cursor
	@SuppressWarnings("serial")
	public static Map<String, Object> addMapToBody(final Body body, Unit cursor) {
		Scene.v().loadClassAndSupport("java.util.HashMap");

		final SootMethod hashMapConstructor = Scene.v().getMethod("<java.util.HashMap: void <init>()>");

		final Local mapLocal = Jimple.v().newLocal("mapToSerialize", RefType.v("java.util.HashMap"));
		body.getLocals().add(mapLocal);
		final AssignStmt assignStmt = Jimple.v().newAssignStmt(mapLocal,
				Jimple.v().newNewExpr(RefType.v("java.util.HashMap")));
		body.getUnits().insertAfter(assignStmt, cursor);
		final InvokeStmt invokeStmt = Jimple.v()
				.newInvokeStmt(Jimple.v().newSpecialInvokeExpr(mapLocal, hashMapConstructor.makeRef()));

		body.getUnits().insertAfter(invokeStmt, assignStmt);

		return new HashMap<String, Object>() {
			{
				put("mapLocal", mapLocal);
				put("cursor", invokeStmt);
			}
		};
	}

	public static SootMethod getMapPutMethod() {
		return Scene.v().getMethod("<java.util.HashMap: java.lang.Object put(java.lang.Object,java.lang.Object)>");
	}

	private static Unit addLocalForFieldAndAddToMap(SootField field, Body body, Local mapLocal, Unit cursor, Random r) {
		final Local local = Jimple.v().newLocal("local" + String.valueOf(field.getSignature().hashCode()) + /*System.currentTimeMillis()*/ + r.nextInt(), field.getType());
		
		body.getLocals().add(local);
		final AssignStmt staticFieldRef = Jimple.v().newAssignStmt(local,Jimple.v().newStaticFieldRef(field.makeRef()));
		body.getUnits().insertAfter(staticFieldRef, cursor);
		final SootMethod putMethod = getMapPutMethod();
		
		final List<Value> arg = new ArrayList<>();
		final Map<String, Object> retAutobox = UtilsSoot.autoboxParam(local, staticFieldRef, body);
		arg.add(StringConstant.v("field-" + field.getSignature()));
		arg.add((Local) retAutobox.get("returnRes"));
		@SuppressWarnings("unchecked")
		List<Unit> cursorList = (List<Unit>) retAutobox.get("cursor");

		final InvokeStmt invStmt = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(mapLocal, putMethod.makeRef(), arg));
		body.getUnits().insertAfter(invStmt, cursorList.get(0));
		cursor = invStmt;
		return cursor;
	}

	public static void turnFieldsPublic(final Set<SootField> fields) {
		for (final SootField field : fields) {
			field.getDeclaringClass().setApplicationClass();
			//System.out.println("mod prima "+field.getModifiers());
			field.setModifiers(/*field.getModifiers() &*/ Modifier.PUBLIC + Modifier.STATIC /*+ ~Modifier.FINAL*/);
			
			/*System.out.println("mod dopo "+field.getModifiers());
			try {
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
	}
	
	public static StaticFieldRef checkForStaticFieldRef(final Value value) {
		if (value instanceof StaticFieldRef) {
			return (StaticFieldRef) value;
		}
		return null;
	}

	public static SootMethod checkForMethod(final Value value) {
		if (value instanceof JStaticInvokeExpr) {
			return ((JStaticInvokeExpr) value).getMethod();
		} else if (value instanceof JSpecialInvokeExpr) {
			return ((JSpecialInvokeExpr) value).getMethod();
		} else if (value instanceof JVirtualInvokeExpr) {
			return ((JVirtualInvokeExpr) value).getMethod();
		}
		return null;
	}

	public static Set<SootField> getStaticFieldsFromMethod(final SootMethod method, Set<SootMethod> visited) {
		if (visited.contains(method) || method.isJavaLibraryMethod() || method.getDeclaringClass().getPackageName().startsWith("android."))
			return new HashSet<>();
		visited.add(method);
		method.getDeclaringClass().setApplicationClass();
		
		final Set<SootField> res = new HashSet<>();
		try {
			final Body body = method.retrieveActiveBody();
				
			Unit u = null;
	
			for (final Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext(); u = i.next()) {
				if (u instanceof JAssignStmt) {
					final JAssignStmt assignStmt = (JAssignStmt) u;
					final StaticFieldRef refRight = checkForStaticFieldRef(assignStmt.getRightOp());
//					final StaticFieldRef refLeft = checkForStaticFieldRef(assignStmt.getLeftOp());
					if ((refRight != null && !isStaticFieldAnnotated(refRight.getField()))) {
						res.add(refRight.getField());
//					} else if ((refLeft != null && !isStaticFieldAnnotated(refLeft.getField()))) {
//						res.add(refLeft.getField());
					} else {
						final SootMethod bfsMethod = checkForMethod(assignStmt.getRightOp());
						if ((bfsMethod != null) && !"br.com.lealdn.offload.Intercept".equals(bfsMethod.getDeclaringClass().toString())) {
							res.addAll(getStaticFieldsFromMethod(bfsMethod, visited));
						}
					}
				} else if (u instanceof JInvokeStmt) {
					final JInvokeStmt invokeStmt = (JInvokeStmt) u;
					final SootMethod bfsMethod = checkForMethod(invokeStmt.getInvokeExpr());
					if ((bfsMethod != null) && !"br.com.lealdn.offload.Intercept".equals(bfsMethod.getDeclaringClass().toString())) {
						res.addAll(getStaticFieldsFromMethod(bfsMethod, visited));
					}
				}
			}
		} catch(RuntimeException e) {
			// Soot was not able to retrieve the method body
			// DO NOTHING
			System.out.println("");
		}

		return res;
	}

	public static boolean isAnnotated(final SootMethod m) {
		return isAnnotatedWith(m.getTags(), "Lbr/com/lealdn/offload/OffloadCandidate;");
	}

	public static boolean isStaticFieldAnnotated(final SootField field) {
		return isAnnotatedWith(field.getTags(), "Lbr/com/lealdn/offload/IgnoreStaticField;");
	}

	private static boolean isAnnotatedWith(final List<Tag> tags, final String annotation) {
		for (final Tag tag : tags) {
			try {
				VisibilityAnnotationTag vtag = (VisibilityAnnotationTag) tag;
				AnnotationTag atag = vtag.getAnnotations().get(0);
				if (atag.getType().equals(annotation)) {
					return true;
				}
			} catch (Exception e) {
			}
		}

		return false;
	}
	
}
