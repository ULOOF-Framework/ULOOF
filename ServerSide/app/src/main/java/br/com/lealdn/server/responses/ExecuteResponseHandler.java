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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.io.File;

import br.com.lealdn.server.ServerService;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import br.com.lealdn.server.ExecuteMethod;
import br.com.lealdn.server.ResponseHandler;
import br.com.lealdn.server.ServerActivity;
/*
	This is the Handler for Execute Response.
	It receives in the content of the request, obviously in
	textual form, a serialized stream of bytes, that represents
	two Java objects:
		- a String Object, that contains the signature of the
			method that is requested to be executed;
		- a <String, Object> hasmap, that contains the args
			needed by the method's execution in correspondence
			of a key under the form "@arg0", "@arg1", ..
			and that contains the object instance whose the
			method belongs (method needs the fields to execute
			correctly) in correspondence of a key under the
			form "@this" (because for the Android application
			that instance is represented by the this object).

	So, after initializing the Kyro instance, the handler calls
	the executeMethod(), that deserializes the stream, executes
	the method and returns the result and the updated object.
	Once received the result (that includes the result object
	of the execution AND the updated object instance of the
	method's class - fields could be modified during the exec),
	it calls the serializeResult() and attaches the serialized
	stream to the content of a OK response.

 */
public class ExecuteResponseHandler implements ResponseHandler {
  //  private static Kryo kryo = new Kryo();

	@Override
	public Response handle(IHTTPSession session) {

		File stats = new File("stats.txt");
		ServerActivity.debug("Incoming request");
		final int length = Integer.valueOf(session.getHeaders().get("content-length"));
		final byte[] buffer = new byte[length];
		final DataInputStream dataIs = new DataInputStream(session.getInputStream());
        final String clientIp = session.getHeaders().get("http-client-ip");

		try {
			dataIs.readFully(buffer);
		} catch (IOException e) {
			ServerActivity.debug("IO Execption: " + e.getMessage());
		}
		
		final long start = System.nanoTime();

		try {
			//Kryo initialization
            Kryo kryo = ServerActivity.pool.borrow();
            kryo.setAsmEnabled(true);
            kryo.getFieldSerializerConfig().setUseAsm(true);
            kryo.getFieldSerializerConfig().setFieldsAsAccessible(true);
            kryo.setReferences(true);
            initializeSerializers(kryo);

            kryo.setInstantiatorStrategy(new InstantiatorStrategy() {
                @Override
                public ObjectInstantiator newInstantiatorOf(Class type) {
                    try {
                        type.getConstructor();
                        return new Kryo.DefaultInstantiatorStrategy().newInstantiatorOf(type);
                    } catch (NoSuchMethodException | SecurityException e) {
                        return new StdInstantiatorStrategy().newInstantiatorOf(type);
                    }
                }
            });
			final Object result = ExecuteMethod.executeMethod(clientIp, buffer,stats,kryo);
			ServerActivity.debug("Method Executed");
			if (result != null) {

				// do s.th. with kryo here, and afterwards release it

				final ByteArrayOutputStream serialized = ExecuteMethod.serializeResult(result, start,kryo );
				ServerActivity.pool.release(kryo);
				ServerActivity.debug("Ok. Returning");
				return new Response(Response.Status.OK, "application/octet-stream", new ByteArrayInputStream(serialized.toByteArray()));
			}
			ServerActivity.debug("Ok. VOID.");
			return new Response(Response.Status.NO_CONTENT, "application/octet-stream", new ByteArrayInputStream(new byte[]{}));
		} catch (ClassNotFoundException | NoSuchFieldException
				| SecurityException | IllegalArgumentException
				| IllegalAccessException | NoSuchMethodException
				| InvocationTargetException e) {

			ServerActivity.error("Error: " + e.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			ServerActivity.error(sw.toString());

			return new Response(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
		}
	}

    private static void initializeSerializers(Kryo kryo){
        kryo.register( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer() );

    }

    private String toString(Map<String, ? extends Object> map) {
		if (map.size() == 0) {
			return "";
		}
		return unsortedList(map);
	}

	private String unsortedList(Map<String, ? extends Object> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("<ul>");
		for (Map.Entry entry : map.entrySet()) {
			listItem(sb, entry);
		}
		sb.append("</ul>");
		return sb.toString();
	}

	private void listItem(StringBuilder sb, Map.Entry entry) {
		sb.append("<li><code><b>").append(entry.getKey()).
		append("</b> = ").append(entry.getValue()).append("</code></li>");
	}
}
