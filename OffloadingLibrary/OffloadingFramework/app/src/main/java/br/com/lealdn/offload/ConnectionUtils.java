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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import android.util.Log;

public class ConnectionUtils {
	private static String ip = "132.227.79.200";
	private static int port = 8080;

    public static void setIp(String ip) {
        ConnectionUtils.ip = ip;
    }

    public static String getIp() {
        return ip;
    }
    public static void setPort(int port) {
        ConnectionUtils.port = port;
    }

    public static int getPort() {
        return port;
    }

	private final static Logger log = Logger.getLogger(ConnectionUtils.class);

	public static Long pingServer() {
		String url = "http://"+ getIp() +":"+String.valueOf(getPort())+"/ping";
    	try {
            final HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 1500);
    		final HttpClient httpclient = new DefaultHttpClient(httpParams);
    		final HttpGet httpGet = new HttpGet(url);
    		final long startTime = System.nanoTime();
    		final HttpResponse response = httpclient.execute(httpGet);
    		final long endTime = System.nanoTime();
    		log.debug("Pinging server..");

    		switch(response.getStatusLine().getStatusCode()) {
    		case 200:
                final long rtt = (long)((endTime - startTime));
                log.debug("RTT: " + rtt);
    			return rtt;
    		default:
    			return null;
    		}
    	} catch(Exception e) {
    		log.error(e.getMessage());
    		return null;
    	}
	}
}
