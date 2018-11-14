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

import com.esotericsoftware.minlog.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;

/*
	Class that just extend the NanoHTTPD and override the serve
	method for our scope.
 */
public class ServerService extends NanoHTTPD {
	public static ExecutorService executors;


	public ServerService() {
		super(8080);


	}

	@Override 
	public Response serve(IHTTPSession session) {
		final String path = session.getUri();
		Log.TRACE();
		// Responses could have only two values: Ping and Execute,
		// Ping is just used to check if the server is up and reachable
		for (final Responses response : Responses.values()) {
			if (path.equals(response.path)) {

				return response.handler.handle(session);
			}
		}
		
		return new Response("NoResponse");
	}
}
