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

package br.com.lealdn.server.responses;

import java.util.Date;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import br.com.lealdn.server.ResponseHandler;
import br.com.lealdn.server.ServerActivity;
/*
	This is the Handler for Ping Response
	Just returns an HTTP OK response
 */
public class PingResponseHandler implements ResponseHandler {
	@Override
	public Response handle(IHTTPSession session) {
		ServerActivity.debug("PING REQUEST: " + new Date());
		System.out.println("Ping");
		return new Response(Response.Status.OK, "text/plain", "ACK");
	}

}
