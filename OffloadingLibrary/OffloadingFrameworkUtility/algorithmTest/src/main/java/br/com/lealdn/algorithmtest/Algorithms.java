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

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import br.com.lealdn.offload.AssessmentConverter;
import br.com.lealdn.offload.LogManager;
import br.com.lealdn.offload.MethodAssessmentConverter;
import br.com.lealdn.offload.OffloadCandidate;

public class Algorithms {

    static int variable = 10;
    //static Float var = new Float(3);

    @OffloadCandidate
    //public static int fibonacciRecusionBigObject(Integer number, byte[] dummy){
    public int fibonacciRecusionBigObject(int number, String dummy){
        if(number == 1 || number == 2){
            return 1;
        } 
        return fibonacciRecusionBigObject(number-1) + fibonacciRecusionBigObject(number -2); //tail recursion
    }

    public static int fibonacciRecusionBigObject(int number){
        //variable = 1;
        if(number == 1 || number == 2){
            return 1;
        } 
        return fibonacciRecusionBigObject(number-1) + fibonacciRecusionBigObject(number -2); //tail recursion
    }
        
    @OffloadCandidate
    public static Integer fibonacciRecusion(Integer n){
        //var++;
        variable++;

        int number = n.intValue();
        if(number == 1 || number == 2){
            return new Integer(1);
        }
        return fibonacciRecusion(new Integer(number-1)) + fibonacciRecusion(new Integer(number -2)); //tail recursion
    }

    public static Integer fibonacciRecusion1(Object[] aobj){
        Integer n = (Integer) aobj[0];
        int number = n.intValue();
        if(number == 1 || number == 2){
            return new Integer(1);
        }
        Object[] par1 = new Object[1];
        Object[] par2 = new Object[1];
        par1[0] = new Integer(number - 1);
        par2[0] = new Integer(number - 2);
        return fibonacciRecusion1(par1) + fibonacciRecusion1(par2); //tail recursion
    }

    public static Integer fibonacciRecusion2(ArrayList arrlist){
        Integer n = (Integer) arrlist.get(0);
        int number = n.intValue();
        if(number == 1 || number == 2){
            return new Integer(1);
        }
        ArrayList par1 = new ArrayList();
        ArrayList par2 = new ArrayList();
        par1.add(new Integer(number - 1));
        par2.add(new Integer(number - 2));
        return fibonacciRecusion2(par1) + fibonacciRecusion2(par2); //tail recursion
    }

    public static Integer fibonacciRecusion3(List list){
        Integer n = (Integer) list.get(0);
        int number = n.intValue();
        if(number == 1 || number == 2){
            return new Integer(1);
        }
        List par1 = new ArrayList();
        List par2 = new ArrayList();
        par1.add(new Integer(number - 1));
        par2.add(new Integer(number - 2));
        return fibonacciRecusion3(par1) + fibonacciRecusion3(par2); //tail recursion
    }
    
    @OffloadCandidate
    @MethodAssessmentConverter(converter=BoyerMooreAssessment.class)
    public static int search(String text, String pattern) {
        int[] badChar = prepareBadChar(pattern);
        int[] s = prepareGoodSuffix(pattern);
        int m = pattern.length();

        int i = 0;
        while(i <= text.length()-m) {
            int j = m-1;
                while(j >= 0 && pattern.charAt(j) == text.charAt(i+j)) {
                j--;
            }
            if (j < 0) {
                return i;
            }
            else {
                i += Math.max(s[j+1], j-badChar[text.charAt(i+j)-97]);
            }
        }

        return -1;
    }

    public static int[] prepareGoodSuffix(String pattern) {
        int m = pattern.length();
        int[] s = new int[m+1];
        for (int i = 0; i < s.length; i++) {
            s[i] = 0;
        }

        int[] f = new int[m+1];
        int i = m, j = m+1;

        f[i] = j;
        while(i > 0) {
            while(j <= m && pattern.charAt(i-1) != pattern.charAt(j-1)) {
                if (s[j] == 0) {
                    s[j] = j - i;
                }
                j = f[j];
            }
            i--;
            j--;
            f[i] = j;
        }

        j = s[0];
        for (int k = 0; k <= m; k++) {
            if (s[k] == 0) {
                s[k] = j;
            }
            if (j == k) {
                j = f[j];
            }
        }

        return s;
    }

    public static int[] prepareBadChar(String pattern) {
        int[] badChar = new int[26];
        for (int i = 0; i < badChar.length; i++) {
            badChar[i] = -1;
        }

        for (int i = 0; i < pattern.length(); i++) {
            badChar[pattern.charAt(i)-97] = i;
        }

        return badChar;
    }

    public static class BoyerMooreAssessment implements AssessmentConverter {

        @Override
        public double convertAssesment(Object o) {
            String s = o.toString();
            double d = s.length();
            return d;
        }

        @Override
        public double convertAllArgumentsToAssesment(Object[] o) {
            if (o != null && o.length == 2) {
                final String text = (String)o[0];
                final String pattern = (String)o[1];

                return text.length() * pattern.length();
            }

            throw new RuntimeException("Arg is null");
        }

    }
}
