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
package br.com.lealdn.beans;

import java.util.HashSet;

import soot.SootMethod;

/* 
 * This class models a node in the directed graph that represent
 * the dependencies among methods. If a method call another method,
 * the first one will be the parent and the called one the child.
 * In the node, a list of parents is stored in order to navigate
 * inside the graph, that will be an hash map whose key will be
 * the method's name and the value the NodeGraph object related.
 * The offloadable field is used to marke the method as offloadable
 * or not-offloadable, or unset if it has not been checked.
 */
public class NodeGraph {
	
	public enum ThreeState {
	    TRUE,
	    FALSE,
	    UNSET
	};

	private HashSet<SootMethod> parents;
	private ThreeState offloadable;
	private Boolean visited;
	
	public NodeGraph(){
		parents = new HashSet<SootMethod>();
		visited = false;
	}
	
	public HashSet<SootMethod> getParents() {
		return parents;
	}

	public void setParents(HashSet<SootMethod> parents) {
		this.parents = parents;
	}
	
	public ThreeState getOffloadable() {
		return offloadable;
	}

	public void setOffloadable(ThreeState offloadable) {
		this.offloadable = offloadable;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

}
