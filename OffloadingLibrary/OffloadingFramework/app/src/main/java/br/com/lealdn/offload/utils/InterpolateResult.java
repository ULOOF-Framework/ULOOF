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

package br.com.lealdn.offload.utils;

/**
 * Created by youf3 on 28/09/2016.
 */

public class InterpolateResult {
    Double running_time, transfer_bytes, CPU_ticks, energy;
    Integer result_size;
    boolean is_local;


    public InterpolateResult(Double running_time, Double transfer_bytes, Double CPU_ticks, Integer result_size, boolean is_local){
        this.running_time = running_time;
        this.transfer_bytes = transfer_bytes;
        this.CPU_ticks = CPU_ticks;
        this.result_size = result_size;
        this.is_local = is_local;
        this.energy = null;
    }

    public boolean success(){
        if (is_local){
            //System.out.println("(LOCAL)this.running_time ="+this.running_time);
           // System.out.println("(LOCAL)this.transfer_bytes ="+this.transfer_bytes);
            if (this.running_time != null && this.transfer_bytes != null) return true;
        }
        else{
           // System.out.println("(REMOTE)this.running_time ="+this.running_time);
            if (this.running_time != null) return true;
        }

        return false;
    }

    public Integer getResult_size(){
        return this.result_size;
    }

    public Double getRunning_time(){
        return this.running_time;
    }

    public Double getTransfer_bytes(){
        return this.transfer_bytes;
    }

    public Double getCPU_ticks(){
        return this.CPU_ticks;
    }

    public void setEnergy(Double energy){this.energy = energy;}

    public Double getEnergy(){return this.energy;}
}
