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

import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
/*
	This is the Main Activity of the Offloading Server.
	It basically just starts ServerService.
 */
@SuppressLint("NewApi")
public class ServerActivity extends ActionBarActivity {
	public static final String LOGPREFIX = "SERVER";
	public static KryoPool pool;
	
	public static void debug(String msg) {
		Log.d(LOGPREFIX, msg);
	}
	public static void error(String msg) {
		Log.e(LOGPREFIX, msg);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		KryoFactory factory = new KryoFactory() {
			public Kryo create () {
				Kryo kryo = new Kryo();
				return kryo;
			}
		};
		// Simple pool, you might also activate SoftReferences to fight OOMEs.
		pool = new KryoPool.Builder(factory).build();

		try {
			ServerService ss = new ServerService();
			ss.start();
		} catch(Exception ex) {
		    Log.e("OFFLOADING", ex.getMessage());
		}


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.server, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
