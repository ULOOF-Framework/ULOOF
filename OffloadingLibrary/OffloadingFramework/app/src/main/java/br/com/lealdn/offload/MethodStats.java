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

/**
 * Created by uloofx on 01/02/18.
 */

public class MethodStats {
    private String signature;
    private double meanLocalExecTime;
    private double meanRemoteExecTime;
    private int executionTimesLocal;
    private int executionTimesRemote;
    private boolean bigvariation ;

    public MethodStats(String signature, double meanLocalExecTime, double meanRemoteExecTime,int executionTimesLocal, int executionTimesRemote){
        this.signature = signature;
        this.meanLocalExecTime = meanLocalExecTime;
        this.meanRemoteExecTime = meanRemoteExecTime;
        this.executionTimesLocal = executionTimesLocal;
        this.executionTimesRemote = executionTimesRemote;
        this.bigvariation = false;
    }

    public void setMeanLocalexecTime(double meanLocalExecTime){
       /* if(((this.meanLocalExecTime/meanLocalExecTime)>=2) || ((this.meanLocalExecTime/meanLocalExecTime)<0.2)){
            this.bigvariation=true;
        }*/
        this.meanLocalExecTime = meanLocalExecTime;
    }
    public void setMeanRemoteExecTime(double meanRemoteExecTime){
       /* if(((this.meanRemoteExecTime/meanRemoteExecTime)>=2) || ((this.meanRemoteExecTime/meanRemoteExecTime)<0.2)){
            this.bigvariation=true;
        }*/
        this.meanRemoteExecTime = meanRemoteExecTime;
    }

    public void setexEcutionTimesLocal(int executionTimesLocal){
        this.executionTimesLocal = executionTimesLocal;
    }

    public void setexEcutionTimesRemote(int executionTimesRemote){
        this.executionTimesRemote = executionTimesRemote;
    }

    public void setBigVariation(boolean bigvaraition){
        this.bigvariation = bigvaraition;
    }


    public int getExecutionTimesLocal(){
        return this.executionTimesLocal;
    }
    public double getMeanLocalexecTime(){
        return this.meanLocalExecTime;
    }
    public double getMeanRemoteExecTime(){
        return this.meanRemoteExecTime;
    }
    public int getExecutionTimesRemote(){
        return this.executionTimesRemote;
    }
    public boolean getBigVariation(){
        return this.bigvariation;
    }

    public void resetStats(){
        this.meanLocalExecTime = 0.0;
        this.meanRemoteExecTime = 0.0;
        this.executionTimesLocal = 0;
        this.executionTimesRemote = 0;
        this.bigvariation = false;
    }

}
