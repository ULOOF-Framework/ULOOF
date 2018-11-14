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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import soot.SootMethod;

public class Utils {

	public static boolean isEmpty(String str) {
		if (str == null || str.isEmpty()) {
			return true;
		}
		return false;
	}

	public static Document stringToXML(String str) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			return builder.parse(new InputSource(new StringReader(str)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<Node> getXMLFields(Document xml, String... fieldName) {
		final List<Node> results = new ArrayList<Node>();
		List<String> fields = new ArrayList<String>();
		for (String field : fieldName) {
			fields.add(field);
		}
		parseXML(xml.getDocumentElement(), results, fields);
		return results;
	}

	public static void parseXML(final Element e, final List<Node> results, List<String> fields) {
		final NodeList children = e.getChildNodes();
		for (int i = 0; i < children.getLength() && !fields.isEmpty(); i++) {
			Node node = children.item(i);
			//System.out.println("node.getNodeName()= "+node.getNodeName());
			if ((node.getNodeType() == Node.ELEMENT_NODE) && (fields.get(0).equals(node.getNodeName()))) {
				fields.remove(0);
				if (fields.isEmpty()) {
					if (node.getAttributes() != null) {
						results.add(node);
					}
					Node tempNode;
					while (null != (tempNode = node.getNextSibling())) {
						if (tempNode.getAttributes() != null) {
							results.add(tempNode);
						}
						node = tempNode;
					}
				} else {
					parseXML((Element) node, results, fields);
				}
			}
		}
	}
	
	
	public static void parseXML2(final Element e, final List<Node> results, List<String> fields) {
		final NodeList children = e.getChildNodes();
		for (int i = 0; i < children.getLength() /*&& !fields.isEmpty()*/; i++) {
			Node node = children.item(i);
			if ((node.getNodeType() == Node.ELEMENT_NODE) && (fields.get(0).equals(node.getNodeName()))) {
				//fields.remove(0);
				if (fields.isEmpty()) {
					if (node.getAttributes() != null) {
						results.add(node);
					}
					Node tempNode;
					while (null != (tempNode = node.getNextSibling())) {
						if (tempNode.getAttributes() != null) {
							results.add(tempNode);
						}
						node = tempNode;
					}
				} else {
					ArrayList<String> fields2 = new ArrayList<String>(){{add("action");}};
					parseXML((Element) node, results, fields2);
				}
			}
		}
	}


	public static boolean isMethodEmpty(SootMethod method) {
		
		String[] body_array = method.retrieveActiveBody().toString().split("\n");
		if (body_array.length < 3) return true;
		body_array = Arrays.copyOfRange(body_array, 2, body_array.length-1);
		
		List<String> base_code = new ArrayList<String>();

		int r = 0;
		if (!method.isStatic()){
			base_code.add(method.getDeclaringClass().toString() + " $r0;");
			base_code.add("$r0 := @this: " + method.getDeclaringClass().toString() + ";");
			r++;
		}
			
		for (int j = 0; j < method.getParameterCount(); j++){
			base_code.add(method.getParameterType(j).toString() + " $r" + r + ";");
			base_code.add("$r" + r + " := @parameter" + j + ": " + method.getParameterType(j).toString() + ";");
			r++;
		}
		
		base_code.add("return;");
		
		for (int i =0; i<body_array.length;i++){
			String line = body_array[i];
			
			for (String code :base_code){
				if (line.trim().equals(code) || line.trim().equals("")){
					body_array = ArrayUtils.removeElement(body_array, line);
					i--;
					break;
				}
			}
		}
		
		if (body_array.length < 1){
			return true;
		}
		
		return false;
	}

}
