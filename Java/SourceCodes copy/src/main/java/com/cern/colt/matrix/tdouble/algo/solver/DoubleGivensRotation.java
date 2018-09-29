/*
 * Copyright (C) 2003-2006 Bjørn-Ove Heimsund
 * 
 * This file is part of MTJ.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package com.cern.colt.matrix.tdouble.algo.solver;

import com.cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 * Givens plane rotation
 */
public class DoubleGivensRotation {

    /**
     * Cosine and sine of the rotation angle. c = x / sqrt(x^2 + y^2), and s =
     * -y / sqrt(x^2 + y^2)
     */
    private final double c, s;

    /**
     * Constructs a Givens plane rotation for a given 2-vector
     * 
     * @param x
     *            First component of the vector
     * @param y
     *            Second component of the vector
     */
    public DoubleGivensRotation(double x, double y) {
        double roe = Math.abs(x) > Math.abs(y) ? x : y;

        double scale = Math.abs(x) + Math.abs(y);
        if (scale != 0) {
            double xs = x / scale;
            double ys = y / scale;
            double r = scale * Math.sqrt(xs * xs + ys * ys);
            if (roe < 0)
                r *= -1;
            c = x / r;
            s = y / r;
        } else {
            c = 1;
            s = 0;
        }
    }

    /**
     * Applies the Givens rotation to two elements in a matrix column
     * 
     * @param H
     *            Matrix to apply to
     * @param column
     *            Column index
     * @param i1
     *            Row index of first element
     * @param i2
     *            Row index of second element
     */
    public void apply(DoubleMatrix2D H, int column, int i1, int i2) {
        double temp = c * H.getQuick(i1, column) + s * H.getQuick(i2, column);
        H.setQuick(i2, column, -s * H.getQuick(i1, column) + c * H.getQuick(i2, column));
        H.setQuick(i1, column, temp);
    }

    /**
     * Applies the Givens rotation to two elements of a vector
     * 
     * @param x
     *            Vector to apply to
     * @param i1
     *            Index of first element
     * @param i2
     *            Index of second element
     */
    public void apply(DoubleMatrix1D x, int i1, int i2) {
        double temp = c * x.getQuick(i1) + s * x.getQuick(i2);
        x.setQuick(i2, -s * x.getQuick(i1) + c * x.getQuick(i2));
        x.setQuick(i1, temp);
    }

}
