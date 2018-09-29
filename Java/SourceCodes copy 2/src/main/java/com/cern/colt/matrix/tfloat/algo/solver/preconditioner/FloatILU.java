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

package com.cern.colt.matrix.tfloat.algo.solver.preconditioner;

import com.cern.colt.matrix.tfloat.FloatMatrix1D;
import com.cern.colt.matrix.tfloat.FloatMatrix2D;
import com.cern.colt.matrix.tfloat.impl.DenseFloatMatrix1D;
import com.cern.colt.matrix.tfloat.impl.SparseRCFloatMatrix2D;

/**
 * ILU(0) preconditioner using a compressed row matrix as internal storage
 */
public class FloatILU implements FloatPreconditioner {

    /**
     * Factorisation matrix
     */
    private SparseRCFloatMatrix2D LU;

    /**
     * Temporary vector for solving the factorised system
     */
    private final FloatMatrix1D y;

    private int[] diagind;

    private final int n;

    /**
     * Sets up the ILU preconditioner
     * 
     * @param n
     *            Problem size (number of rows)
     */
    public FloatILU(int n) {
        this.n = n;
        y = new DenseFloatMatrix1D(n);
    }

    public FloatMatrix1D apply(FloatMatrix1D b, FloatMatrix1D x) {
        if (x == null) {
            x = b.like();
        }

        // Ly = b, y = L\b
        lowerUnitSolve(b, y);

        // Ux = L\b = y
        return upperSolve(y, x);
    }

    public FloatMatrix1D transApply(FloatMatrix1D b, FloatMatrix1D x) {
        if (x == null) {
            x = b.like();
        }

        // U'y = b, y = U'\b
        upperTransSolve(b, y);

        // L'x = U'\b = y
        return loverUnitTransSolve(y, x);
    }

    public void setMatrix(FloatMatrix2D A) {
        if (A.rows() != n) {
            throw new IllegalArgumentException("A.rows() != n");
        }
        LU = new SparseRCFloatMatrix2D(n, n);
        LU.assign(A);
        if (!LU.hasColumnIndexesSorted()) {
            LU.sortColumnIndexes();
        }
        factor();
    }

    private void factor() {

        // Internal CRS matrix storage
        int[] colind = LU.getColumnIndexes();
        int[] rowptr = LU.getRowPointers();
        float[] data = LU.getValues();

        // Find the indexes to the diagonal entries
        diagind = findDiagonalIndexes(n, colind, rowptr);

        // Go down along the main diagonal
        for (int k = 1; k < n; ++k)
            for (int i = rowptr[k]; i < diagind[k]; ++i) {

                // Get the current diagonal entry
                int index = colind[i];
                float LUii = data[diagind[index]];

                if (LUii == 0)
                    throw new RuntimeException("Zero pivot encountered on row " + (i + 1) + " during ILU process");

                // Elimination factor
                float LUki = (data[i] /= LUii);

                // Traverse the sparse row i, reducing on row k
                for (int j = diagind[index] + 1, l = rowptr[k] + 1; j < rowptr[index + 1]; ++j) {

                    while (l < rowptr[k + 1] && colind[l] < colind[j])
                        l++;

                    if (colind[l] == colind[j])
                        data[l] -= LUki * data[j];
                }
            }

    }

    private int[] findDiagonalIndexes(int m, int[] colind, int[] rowptr) {
        int[] diagind = new int[m];

        for (int k = 0; k < m; ++k) {
            diagind[k] = com.cern.colt.Sorting.binarySearchFromTo(colind, k, rowptr[k], rowptr[k + 1] - 1);

            if (diagind[k] < 0)
                throw new RuntimeException("Missing diagonal entry on row " + (k + 1));
        }

        return diagind;
    }

    private FloatMatrix1D lowerUnitSolve(FloatMatrix1D b, FloatMatrix1D x) {
        float[] bd = ((DenseFloatMatrix1D) b).elements();
        float[] xd = ((DenseFloatMatrix1D) x).elements();
        int[] colind = LU.getColumnIndexes();
        int[] rowptr = LU.getRowPointers();
        float[] data = LU.getValues();
        int rows = LU.rows();
        for (int i = 0; i < rows; ++i) {

            // xi = bi - sum[j<i] Lij * xj
            float sum = 0;
            for (int j = rowptr[i]; j < diagind[i]; ++j)
                sum += data[j] * xd[colind[j]];

            xd[i] = bd[i] - sum;
        }

        return x;
    }

    private FloatMatrix1D loverUnitTransSolve(FloatMatrix1D b, FloatMatrix1D x) {
        x.assign(b);
        float[] xd = ((DenseFloatMatrix1D) x).elements();
        int[] colind = LU.getColumnIndexes();
        int[] rowptr = LU.getRowPointers();
        float[] data = LU.getValues();
        int rows = LU.rows();

        for (int i = rows - 1; i >= 0; --i)

            // At this stage, x[i] is known, so move it over to the right hand
            // side for the remaining equations
            for (int j = rowptr[i]; j < diagind[i]; ++j)
                xd[colind[j]] -= data[j] * xd[i];

        return x;
    }

    private FloatMatrix1D upperSolve(FloatMatrix1D b, FloatMatrix1D x) {
        float[] bd = ((DenseFloatMatrix1D) b).elements();
        float[] xd = ((DenseFloatMatrix1D) x).elements();
        int[] colind = LU.getColumnIndexes();
        int[] rowptr = LU.getRowPointers();
        float[] data = LU.getValues();
        int rows = LU.rows();
        for (int i = rows - 1; i >= 0; --i) {

            // xi = (bi - sum[j>i] Uij * xj) / Uii
            float sum = 0;
            for (int j = diagind[i] + 1; j < rowptr[i + 1]; ++j)
                sum += data[j] * xd[colind[j]];

            xd[i] = (bd[i] - sum) / data[diagind[i]];
        }

        return x;
    }

    private FloatMatrix1D upperTransSolve(FloatMatrix1D b, FloatMatrix1D x) {
        x.assign(b);
        float[] xd = ((DenseFloatMatrix1D) x).elements();
        int[] colind = LU.getColumnIndexes();
        int[] rowptr = LU.getRowPointers();
        float[] data = LU.getValues();
        int rows = LU.rows();

        for (int i = 0; i < rows; ++i) {

            // Solve for the current entry
            xd[i] /= data[diagind[i]];

            // Move this known solution over to the right hand side for the
            // remaining equations
            for (int j = diagind[i] + 1; j < rowptr[i + 1]; ++j)
                xd[colind[j]] -= data[j] * xd[i];
        }

        return x;
    }

}
