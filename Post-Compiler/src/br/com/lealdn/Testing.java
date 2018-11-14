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

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.apache.tools.ant.*;


public class Testing {

    public Testing() {
        System.out.println("ok");
    }

    public static void main(String[] args) {
    	File fil = new File("/home/alessio/ULOOF/AndroidInstrumentTest2/Airbnb_methodlist.txt");
    	System.out.println(fil.getAbsolutePath());
    }
   /* 	OkHttpClient client = new OkHttpClient();

    	MediaType mediaType = MediaType.parse("application/json");
    	RequestBody body = RequestBody.create(mediaType, "{\n \"username\": \"sdn-bec3\",\n    \"password\": \"SalVa!!\",\n \"service\": \"bec3.com\"\n}");
    	Request request = new Request.Builder()
    	  .url("https://bec3.com/api/login")
    	  .post(body)
    	  .addHeader("Content-Type", "application/json")
    	  .addHeader("Cache-Control", "no-cache")
    	  .addHeader("Postman-Token", "0c6c7464-5891-4423-ac75-ee1ababfb378")
    	  .build();
    	Response response = null;
    	try {
    		response = client.newCall(request).execute();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} 
    	
    	try {
			System.out.println(""+response.body().string());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	
    	
    	OkHttpClient client2 = new OkHttpClient();

    	MediaType mediaType2 = MediaType.parse("application/octet-stream");
    	RequestBody body2 = RequestBody.create(mediaType2, "{\n \"username\": \"sdn-bec3\",\n    \"password\": \"SalVa!!!\",\n \"service\": \"bec3.com\"\n}");
    	Request request2 = new Request.Builder()
    	  .url("https://bec3.com/api/feature")
    	  .get()
    	  .addHeader("Cache-Control", "no-cache")
    	  .addHeader("Postman-Token", "39ddc674-576e-4157-a6fc-1c3b4fd44692")
    	  .build();
    	Response response2= null ;
    	try {
    		response2= client.newCall(request2).execute();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
    	try {
			System.out.println(response2.body().string());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
   
    }

	public int doNothing() {
        System.currentTimeMillis();
        int a = 1;
        System.out.println(a);
        try {
            System.currentTimeMillis();
        } catch(Exception e) {
            System.out.println("Oops");
        }
        return 0;
	}*/

}
