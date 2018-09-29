/*
Copyright (C) 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package com.cern.colt.matrix.tdcomplex;

import com.cern.colt.matrix.tdcomplex.impl.DenseDComplexMatrix3D;
import com.cern.colt.matrix.tdcomplex.impl.SparseDComplexMatrix3D;

/**
 * Factory for convenient construction of 3-d matrices holding <tt>complex</tt>
 * cells. Use idioms like <tt>ComplexFactory3D.dense.make(4,4,4)</tt> to
 * construct dense matrices, <tt>ComplexFactory3D.sparse.make(4,4,4)</tt> to
 * construct sparse matrices.
 * 
 * If the factory is used frequently it might be useful to streamline the
 * notation. For example by aliasing:
 * <table>
 * <td class="PRE">
 * 
 * <pre>
 *  ComplexFactory3D F = ComplexFactory3D.dense;
 *  F.make(4,4,4);
 *  F.random(4,4,5);
 *  ...
 * </pre>
 * 
 * </td>
 * </table>
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @version 1.0, 12/10/2007
 */
public class DComplexFactory3D extends com.cern.colt.PersistentObject {
    private static final long serialVersionUID = 1L;

    /**
     * A factory producing dense matrices.
     */
    public static final DComplexFactory3D dense = new DComplexFactory3D();

    /**
     * A factory producing sparse matrices.
     */
    public static final DComplexFactory3D sparse = new DComplexFactory3D();

    /**
     * Makes this class non instantiable, but still let's others inherit from
     * it.
     */
    protected DComplexFactory3D() {
    }

    /**
     * Constructs a matrix with the given cell values. <tt>values</tt> is
     * required to have the form <tt>values[slice][row][column]</tt> and have
     * exactly the same number of slices, rows and columns as the receiver.
     * <p>
     * The values are copied. So subsequent changes in <tt>values</tt> are not
     * reflected in the matrix, and vice-versa.
     * 
     * @param values
     *            the values to be filled into the cells.
     * @return <tt>this</tt> (for convenience only).
     * @throws IllegalArgumentException
     *             if
     *             <tt>values.length != slices() || for any 0 &lt;= slice &lt; slices(): values[slice].length != rows()</tt>
     *             .
     * @throws IllegalArgumentException
     *             if
     *             <tt>for any 0 &lt;= column &lt; columns(): values[slice][row].length != columns()</tt>
     *             .
     */
    public DComplexMatrix3D make(double[][][] values) {
        if (this == sparse) {
            return new SparseDComplexMatrix3D(values);
        } else {
            return new DenseDComplexMatrix3D(values);
        }
    }

    /**
     * Constructs a matrix with the given shape, each cell initialized with
     * zero.
     */
    public DComplexMatrix3D make(int slices, int rows, int columns) {
        if (this == sparse) {
            return new SparseDComplexMatrix3D(slices, rows, columns);
        } else {
            return new DenseDComplexMatrix3D(slices, rows, columns);
        }
    }

    /**
     * Constructs a matrix with the given shape, each cell initialized with the
     * given value.
     */
    public DComplexMatrix3D make(int slices, int rows, int columns, double[] initialValue) {
        return make(slices, rows, columns).assign(initialValue);
    }

    /**
     * Constructs a matrix with uniformly distributed values in <tt>(0,1)</tt>
     * (exclusive).
     */
    public DComplexMatrix3D random(int slices, int rows, int columns) {
        return make(slices, rows, columns).assign(com.cern.jet.math.tdcomplex.DComplexFunctions.random());
    }
}
