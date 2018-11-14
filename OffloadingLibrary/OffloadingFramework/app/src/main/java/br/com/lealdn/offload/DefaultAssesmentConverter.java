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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultAssesmentConverter implements AssessmentConverter {
    @Override
    public double convertAssesment(Object arg) {
       /* Class<?> clazz = arg.getClass();
        if (clazz == Integer.class || clazz == int.class) {
            return ((Integer)arg).doubleValue();
        }
        if (clazz == Double.class || clazz == double.class) {
            return (Double)arg;
        }
        if (clazz == Long.class || clazz == long.class) {
            return ((Long)arg).doubleValue();
        }
        if (clazz == Short.class || clazz == short.class) {
            return ((Short)arg).doubleValue();
        }
        if (clazz == String.class) {
            return ((String)arg).length();
        }
        if (isClassOrSuperclass(clazz, Collection.class)) {
            return ((Collection<?>)arg).size();
        }*/
        return 0;
    }

    private boolean isClassOrSuperclass(Class<?> clazz, Class<?> superclass) {
        return clazz == superclass || superclass.isAssignableFrom(clazz) || clazz.isInstance(superclass);
    }

    @Override
    public double convertAllArgumentsToAssesment(Object[] o) {
        throw new RuntimeException("Default not implemented, please use the method convertAssesment");
    }

    /*@Override
    public double convertAllArgumentsToAssesment2(ArrayList o) {
        throw new RuntimeException("Not implemented on purpose");
    }

    @Override
    public double convertAllArgumentsToAssesment3(List o) {
        throw new RuntimeException("Not implemented on purpose");
    }*/
}
