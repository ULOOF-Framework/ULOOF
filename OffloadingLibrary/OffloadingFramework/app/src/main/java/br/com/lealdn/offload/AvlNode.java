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

public class AvlNode<T> {
    private T element;      // The data in the node
    private AvlNode<T> left;         // Left child
    private AvlNode<T> right;        // Right child
    private int height;       // Height
    private boolean dirty;

    AvlNode(T theElement) {
        this(theElement, null, null);
    }

    AvlNode(T theElement, AvlNode<T> lt, AvlNode<T> rt) {
        this.element = theElement;
        this.left = lt;
        this.right = rt;
        this.height = 0;
        this.dirty = true;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void setLeft(AvlNode<T> left) {
        this.left = left;
    }
    public AvlNode<T> getLeft() {
        return left;
    }
    public void setRight(AvlNode<T> right) {
        this.right = right;
    }
    public AvlNode<T> getRight() {
        return right;
    }

    public T getElement() {
        return element;
    }

    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }
}
