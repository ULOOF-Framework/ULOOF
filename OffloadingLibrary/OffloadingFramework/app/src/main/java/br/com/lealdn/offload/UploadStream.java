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

import java.io.IOException;
import java.io.OutputStream;

public class UploadStream extends OutputStream {
	private volatile long transferred = 0;
	private volatile long lastTime = 0;
	private OutputStream out;

	public UploadStream(OutputStream out) {
		this();
		this.out = out;
	}
	
	public UploadStream() {
		this.transferred = 0;
	}
	
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}

	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		this.transferred += len;
		//System.out.println(this.transferred/1024+" KB");
		this.lastTime = System.nanoTime();
	}

	public void write(int b) throws IOException	{
		out.write(b);
		this.transferred++;
		this.lastTime = System.nanoTime();
	}
	
	public long getLastTime() {
		return this.lastTime;
	}

    public long getTransferred() {
        return this.transferred;
    }
}
