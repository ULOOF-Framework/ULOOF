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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class Interpolator<K extends Comparable<K>, T extends Number> {
    final Map<K, T> rounds;
    final AvlTree<K> keys;
    Smoothable<T> smoother;
    PolynomialSplineFunction spline;

    public Interpolator(Smoothable<T> smoother) {
        this.smoother = smoother;
        this.rounds = new TreeMap<K, T>();
        this.keys = new AvlTree<K>();
    }

    public int size() {
        return this.rounds.size();
    }

    public void addRound(K key, T value) {
        if (this.rounds.containsKey(key)) {
            T previousRound = this.rounds.get(key);
            this.rounds.remove(key);
            value = smoother.smooth(previousRound, value);

            keys.markNodeAsDirty(key);
        }
        else {
            keys.insert(key);
        }
        this.rounds.put(key, value);
    }

    public Double interpolate(K key) {
        if (rounds.containsKey(key)) {
            return rounds.get(key).doubleValue();
        }

        AvlNode<K>[] marginalNodes = keys.findBetween(key);
        if (isDirty(marginalNodes)) {
            this.spline = getSpline(rounds);
            keys.markAllAsClean();
        }
        
        if (spline != null && rounds.size() > 5) {
            try {
                return spline.value(((Double)key).doubleValue());
            } catch(Exception e) {
                return null;
            }
        }
        return null;
    }

    public interface Smoothable<T> {
        public T smooth(T previous, T current);
    }


    private boolean isDirty(AvlNode<K>[] marginalNodes) {
        if (marginalNodes == null) {
            return false;
        }
        if (marginalNodes[0] != null && marginalNodes[0].isDirty()) {
            return true;
        }
        if (marginalNodes[1] != null && marginalNodes[1].isDirty()) {
            return true;
        }
        return false;
    }

    private PolynomialSplineFunction getSpline(Map<K, T> rounds) {
        if (rounds.size() >= 5) {
            final double[] assessments = new double[rounds.size()];
            final double[] times = new double[rounds.size()];
            int i = 0;
            for (Iterator<K> it = rounds.keySet().iterator(); it.hasNext();) {
                final K key = it.next();
                assessments[i] = ((Number)key).doubleValue();
                times[i] = rounds.get(key).doubleValue();
                i++;
            }
            return new AkimaSplineInterpolator().interpolate(assessments, times);
        }
        else {
            return null;
        }
    }
}
