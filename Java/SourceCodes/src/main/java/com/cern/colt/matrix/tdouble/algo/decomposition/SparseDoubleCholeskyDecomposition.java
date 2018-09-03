/*
Copyright (C) 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package com.cern.colt.matrix.tdouble.algo.decomposition;

import com.cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.cern.colt.matrix.tdouble.algo.DoubleProperty;
import com.cern.colt.matrix.tdouble.impl.SparseCCDoubleMatrix2D;
import com.cern.colt.matrix.tdouble.impl.SparseRCDoubleMatrix2D;
import com.edu.emory.mathcs.csparsej.tdouble.Dcs_chol;
import com.edu.emory.mathcs.csparsej.tdouble.Dcs_ipvec;
import com.edu.emory.mathcs.csparsej.tdouble.Dcs_lsolve;
import com.edu.emory.mathcs.csparsej.tdouble.Dcs_ltsolve;
import com.edu.emory.mathcs.csparsej.tdouble.Dcs_pvec;
import com.edu.emory.mathcs.csparsej.tdouble.Dcs_schol;
import com.edu.emory.mathcs.csparsej.tdouble.Dcs_common.Dcs;
import com.edu.emory.mathcs.csparsej.tdouble.Dcs_common.Dcsn;
import com.edu.emory.mathcs.csparsej.tdouble.Dcs_common.Dcss;

/**
 * For a symmetric, positive definite matrix <tt>A</tt>, the Cholesky
 * decomposition is a lower triangular matrix <tt>L</tt> so that <tt>A = L*L'</tt>; If
 * the matrix is not symmetric positive definite, the IllegalArgumentException
 * is thrown.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 */
public class SparseDoubleCholeskyDecomposition {
    private Dcss S;
    private Dcsn N;
    private DoubleMatrix2D L;
    private boolean rcMatrix = false;

    /**
     * Row and column dimension (square matrix).
     */
    private int n;

    /**
     * Constructs and returns a new Cholesky decomposition object for a sparse
     * symmetric and positive definite matrix; The decomposed matrices can be
     * retrieved via instance methods of the returned decomposition object.
     * 
     * @param A
     *            Square, symmetric positive definite matrix .
     * @param order
     *            ordering option (0 or 1); 0: natural ordering, 1: amd(A+A')
     * @throws IllegalArgumentException
     *             if <tt>A</tt> is not square or is not sparse or is not a
     *             symmetric positive definite.
     * @throws IllegalArgumentException
     *             if <tt>order != 0 || order != 1</tt>
     */
    public SparseDoubleCholeskyDecomposition(DoubleMatrix2D A, int order) {
        DoubleProperty.DEFAULT.checkSquare(A);
        DoubleProperty.DEFAULT.checkSparse(A);
        if (order < 0 || order > 1) {
            throw new IllegalArgumentException("order must be equal 0 or 1");
        }
        Dcs dcs;
        if (A instanceof SparseRCDoubleMatrix2D) {
            rcMatrix = true;
            dcs = ((SparseRCDoubleMatrix2D) A).getColumnCompressed().elements();
        } else {
            dcs = (Dcs) A.elements();
        }
        n = A.rows();
        S = Dcs_schol.cs_schol(order, dcs);
        if (S == null) {
            throw new IllegalArgumentException("Exception occured in cs_schol()");
        }
        N = Dcs_chol.cs_chol(dcs, S);
        if (N == null) {
            throw new IllegalArgumentException("Matrix is not symmetric positive definite");
        }
    }

    /**
     * Returns the triangular factor, <tt>L</tt>.
     * 
     * @return <tt>L</tt>
     */
    public DoubleMatrix2D getL() {
        if (L == null) {
            L = new SparseCCDoubleMatrix2D(N.L);
            if (rcMatrix) {
                L = ((SparseCCDoubleMatrix2D) L).getRowCompressed();
            }
        }
        return L.copy();
    }

    /**
     * 
     * Returns the triangular factor, <tt>L'</tt>.
     * 
     * @return <tt>L'</tt>
     */
    public DoubleMatrix2D getLtranspose() {
        if (L == null) {
            L = new SparseCCDoubleMatrix2D(N.L);
            if (rcMatrix) {
                L = ((SparseCCDoubleMatrix2D) L).getRowCompressed();
            }
        }
        if (rcMatrix) {
            return ((SparseRCDoubleMatrix2D) L).getTranspose();
        } else {
            return ((SparseCCDoubleMatrix2D) L).getTranspose();
        }
    }

    /**
     * Returns a copy of the symbolic Cholesky analysis object
     * 
     * @return symbolic Cholesky analysis
     */
    public Dcss getSymbolicAnalysis() {
        Dcss S2 = new Dcss();
        S2.cp = S.cp != null ? S.cp.clone() : null;
        S2.leftmost = S.leftmost != null ? S.leftmost.clone() : null;
        S2.lnz = S.lnz;
        S2.m2 = S.m2;
        S2.parent = S.parent != null ? S.parent.clone() : null;
        S2.pinv = S.pinv != null ? S.pinv.clone() : null;
        S2.q = S.q != null ? S.q.clone() : null;
        S2.unz = S.unz;
        return S2;
    }

    /**
     * Solves <tt>A*x = b</tt>(in-place). Upon return <tt>b</tt> is overridden
     * with the result <tt>x</tt>.
     * 
     * @param b
     *            A vector with of size A.rows();
     * @exception IllegalArgumentException
     *                if <tt>b.size() != A.rows()</tt>.
     */
    public void solve(DoubleMatrix1D b) {
        if (b.size() != n) {
            throw new IllegalArgumentException("b.size() != A.rows()");
        }
        DoubleProperty.DEFAULT.checkDense(b);
        double[] y = new double[n];
        double[] x;
        if (b.isView()) {
            x = (double[]) b.copy().elements();
        } else {
            x = (double[]) b.elements();
        }
        Dcs_ipvec.cs_ipvec(S.pinv, x, y, n); /* y = P*b */
        Dcs_lsolve.cs_lsolve(N.L, y); /* y = L\y */
        Dcs_ltsolve.cs_ltsolve(N.L, y); /* y = L'\y */
        Dcs_pvec.cs_pvec(S.pinv, y, x, n); /* x = P'*y */

        if (b.isView()) {
            b.assign(x);
        }
    }
}
