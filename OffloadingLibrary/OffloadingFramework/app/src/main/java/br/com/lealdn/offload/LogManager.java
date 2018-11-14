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

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;


public class LogManager {
	private Set<LogEntry> log = new TreeSet<LogEntry>(new Comparator<LogEntry>() {
		@Override
		public int compare(LogEntry arg0, LogEntry arg1) {
			if (arg1 == null)
				return 0;
			return ((Long)arg1.time).compareTo(arg0.time);
		}
	});
	
	public LogManager() {
	}
	
	public void addToLog(final String methodSignature, final boolean shouldOffload) {
		final LogEntry newLog = new LogEntry(methodSignature, shouldOffload, (long)(System.nanoTime()*0.001));
		this.log.add(newLog);
	}
	
	public Set<LogEntry> getLog() { 
		return this.log;
	}
	
	public class LogEntry {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (int) (time ^ (time >>> 32));
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
			LogEntry other = (LogEntry) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (time != other.time)
				return false;
			return true;
		}
		public LogEntry(final String methodSignature, final boolean shouldOffload, final long time) {
			this.methodSignature = methodSignature;
			this.shouldOffload = shouldOffload;
			this.time = time;
		}

		public final String methodSignature;
		public final boolean shouldOffload;
		public final long time;
		private LogManager getOuterType() {
			return LogManager.this;
		}
	}

}
