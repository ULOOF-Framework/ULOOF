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

package br.com.lealdn.algorithmtest;

import android.net.Uri;
import android.support.v7.app.ActionBarActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.EditText;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;
import android.content.res.Resources.NotFoundException;
import android.widget.Toast;

import com.esotericsoftware.kryo.Kryo;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import br.com.lealdn.offload.ConnectionUtils;
import br.com.lealdn.offload.Intercept;
import br.com.lealdn.offload.OffloadingManager;

public class MainActivity extends ActionBarActivity {
    private final static Logger log = Logger.getLogger(MainActivity.class);

    final Handler handler = new Handler();
    Button button1, button2, button3, button4;
    Button buttonFibDummy;
    TextView tv1, tv2, tv3, tv4, tv5, tv6, delay_view/*, tv7*/;
    ImageView iv1;
    EditText ipaddr_text;

    private int batteryLevel = 0;
    private static boolean running = false;
    private static final int PROGRESS = 0x1;

    private int mProgressStatus = 0;
    private ProgressBar mProgress;
    private Handler mHandler = new Handler();
    private GifDrawable gifFromResource;
    Counter c = new Counter();

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batteryLevel = level;
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //c.reset();
        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.hideSoftInputFromWindow(editTextField.getWindowToken(), 0);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        OffloadingManager.initialize(this,"br.com.lealdn.algorithmtest");
        //ConnectionUtils.setIp("132.227.125.120");
        ConnectionUtils.setPort(8080);
        Intercept.setAlpha(0.993);

        setContentView(R.layout.activity_main);

       // this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
        tv1 = (TextView) findViewById(R.id.textView1);
        tv2 = (TextView) findViewById(R.id.textView2);
        tv3 = (TextView) findViewById(R.id.textView3);
        tv4 = (TextView) findViewById(R.id.textView4);
        tv5 = (TextView) findViewById(R.id.textView5);
        tv6 = (TextView) findViewById(R.id.textView6);
        //tv7 = (TextView) findViewById(R.id.textView7);
        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        GifImageView gifImageView = (GifImageView) findViewById(R.id.loading);
        iv1 = (ImageView) findViewById(R.id.imageView2);
        ipaddr_text = (EditText) findViewById(R.id.remote_ip);
        delay_view = (TextView) findViewById(R.id.delay_text);

        //mProgress.setVisibility(LinearLayout.INVISIBLE);
        try {
            gifFromResource = new GifDrawable(getResources(), R.drawable.loading);
            gifFromResource.stop();
            gifImageView.setImageDrawable(gifFromResource);
        } catch (NotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //final TextView provatext = (TextView) findViewById(R.id.textView0);

        tv1.setFreezesText(true);

        //final int[] numbers = new int[]{5, 10, 20, 25, 30, 35, 40, 22, 18, 41};
        final int[] numbers = new int[]{5, 10, 20, 25, 30, 32, 22, 18, 29, 26, 31, 28, 22, 29, 23, 24, 32, 28, 21, 27, 30};
        //final int[] numbers = new int[]{5, 10, 20, 25, 13, 20, 23, 21, 29, 22, 31, 19, 26, 32, 28, 24, 33, 30, 35, 27, 22, 32};

        setListener(button1, numbers, 0);
        setListener(button2, numbers, 1);
        setListener(button3, numbers, 2);
        setListener(button4, numbers, 3);
        //ipaddr_text.setOnClickListener(new View.OnClickListener() {

                                        /*  @Override
                                          public void onClick(View v) {
                                              String curr_text = ipaddr_text.getText().toString();
                                              log.debug("Clicked remote_ip: " + curr_text);
                                              if (curr_text.contains("Please specify server address")){
                                                  log.debug("clearing server msg");
                                                  ipaddr_text.setText(curr_text.replace("Please specify server address",""));
                                              }
                                              ipaddr_text.setText("");
                                          }
                                      });*/

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void doTest(final int num, final int iteration, int type) throws Throwable {
        final int res;
        final Map<Object, Object> sendArgs = new HashMap<Object, Object>();
        final String sig = "<br.com.lealdn.algorithmtest.Algorithms: java.lang.Integer fibonacciRecusion1(Object[])>";


      //  long tshould = System.currentTimeMillis();
      //
        //  final boolean should = Intercept.shouldOffload(sig, sendArgs);
        //final long overhead = System.currentTimeMillis();


           // final Object result = Intercept.sendAndSerialize(sig, sendArgs);
        switch (type) {
            case 1:

                sendArgs.put("@this", this);
                Object[] aobj = new Object[1];
                aobj[0] = new Integer(num);
                sendArgs.put("@arg0", aobj);
               // final boolean should = Intercept.shouldOffload(sig, sendArgs);
               // if(!true){
                ////    res = Algorithms.fibonacciRecusion1(aobj);
                   // break;
               // }else{
                  //  try {
                        res = (int)Intercept.sendAndSerialize(sig, sendArgs);
                   // } catch (Throwable throwable) {
                   //     throwable.printStackTrace();
                   // }
                    break;
                //}


            case 2:
                ArrayList arrlist = new ArrayList();
                arrlist.add(new Integer(num));
                res = Algorithms.fibonacciRecusion2(arrlist);
                break;
            case 3:
                List list = new ArrayList();
                list.add(new Integer(num));
                res = Algorithms.fibonacciRecusion3(list);
                break;
            default:
                res = Algorithms.fibonacciRecusion(new Integer(num));
                c.increase();
                //log.debug("Print string: " + c.getString() + ", count: " + c.getCount());
                c.reset();
                //log.debug("Print string: " + c.getString() + ", count: " + c.getCount());
                break;
        }
        handler.post(new Runnable() {
            public void run() {
                //tv1.setText(iteration + " % "); //+ "\n offloaded : " + Intercept.checkOffloading());
                if (Intercept.checkOffloading()) {
                    //tv2.setText(tv2.getText() + "\nFib(" + num + ") = " + res + " offloaded");
                    iv1.setImageResource(R.drawable.ic_cloud_queue_green_a400_18dp);
                } else {

                    iv1.setImageResource(R.drawable.ic_cloud_queue_black_24dp);
                }
                if (Intercept.checkOffloading()) {
                    tv2.setText(tv2.getText() + "\nFib(" + num + ") = " + res + " offloaded");
                } else {
                    tv2.setText(tv2.getText() + "\nFib(" + num + ") = " + res);
                }
                tv3.setText("Local execution: " + Intercept.checkNumLoal());
                tv4.setText("Offloaded: " + Intercept.checkNumOffload());
                tv5.setText("Execution time: " + Intercept.getextime() + "ms");
//                delay_view.setText("Latency: " + (int)Intercept.getRTT() + "ms");
                double energy = Intercept.getEnergy_cons() * 1000;
                //log.debug("Energy consumption for gui :" + energy);
                DecimalFormat df = new DecimalFormat("#.##");
                tv6.setText("Energy Consumption: " + df.format(energy) + "mw");
                //tv7.setText("RTT: " + Intercept.getRTT() + "ms");
                //mProgress.setProgress(iteration + 1);
            }
        });
    }

    private void setListener(final Button b, final int[] numbers, final int type){
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        handler.post(new Runnable() {
                            public void run() {
                                setButtons(false);
                                b.setText("Checking server");
                            }
                        });
                        if (Intercept.checkConnection()) {
                            handler.post(new Runnable() {
                                public void run() {
                                    b.setText("Calc...");
                                    tv1.setText("");
                                }
                            });
                            running = true;
                            log.debug("Starting test.");
                            log.debug("---------- BATTERY LEVEL: " + batteryLevel);
                            for (int j = 0; j < 5; j++) {
                                log.debug("Test#" + j);
                                log.debug("===============================");
                                for (int i = 0; i < numbers.length; i++) {
                                    final int node = numbers[i];
                                    log.debug("---- BATTERY LEVEL: " + batteryLevel);
                                    log.debug("-----pos: " + (i) + " from " + numbers.length);
                                    log.debug("------------- url: " + node);
                                    gifFromResource.start();
                                    try {
                                        doTest(node, j, type);
                                    } catch (Throwable throwable) {
                                        throwable.printStackTrace();
                                    }
                                }
                            }

                            log.debug("---------- TEST FINISHED. BATTERY LEVEL: " + batteryLevel);
                            running = false;
                            resetInterface(b);
                            gifFromResource.stop();
                        } else {
                            handler.post(new Runnable() {
                                public void run() {
                                    b.setEnabled(true);
                                    //b.setText("Fibonacci");
                                    ipaddr_text.setText("Server is temporarily unavilable");
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }

    private void resetInterface(final Button b) {
        handler.post(new Runnable() {
            public void run() {
                tv1.setText("Finished. \nPress Calculate button for restarting calculation"); //+ "\n offloaded : " + Intercept.checkOffloading());
                //b.setEnabled(true);
                setButtons(true);
            }
        });
    }

    private void setButtons(boolean enable){
        if (enable){
            button1.setText("Fibonacci");
            button1.setEnabled(true);
            button2.setText("Obj[]");
            button2.setEnabled(true);
            button3.setText("ArrList");
            button3.setEnabled(true);
            button3.setText("List");
            button4.setEnabled(true);
        }else {
            button1.setEnabled(false);
            button2.setEnabled(false);
            button3.setEnabled(false);
            button4.setEnabled(false);
        }
    }

    private void doTestFibDummy(int num, final int iteration) {
        //byte[] arr = this.createBA(1024 * 50 * num);
        //final int res = Algorithms.fibonacciRecusionBigObject(num, arr);
        String arr = this.createSA(1024 * 50 * num);
        //final int res = Algorithms.fibonacciRecusionBigObject(num, arr);
        final int res = 0;
        arr = null;
        handler.post(new Runnable() {
            public void run() {
                tv1.setText(iteration + " : " + String.valueOf(res));
            }
        });
    }

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    private String createSA(int size) {
        final byte muster = (byte) 0x2;
        baos.reset();
        for (int i = 0; i < size; i++) {
            baos.write(muster);
        }
        try {
            baos.close();
        } catch (IOException e) {
        }

        return baos.toString();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            if(!running){
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }else{
                Context context = getApplicationContext();
                CharSequence text = "Wait for the end of the test";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
       // unregisterReceiver(this.mBatInfoReceiver);
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }
}
