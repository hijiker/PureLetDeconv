package com.cern.colt.matrix.tdouble.algo.solver.preconditioner;

import com.cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.cern.colt.matrix.tdouble.DoubleMatrix2D;

public class DoubleIdentity implements DoublePreconditioner {

    public DoubleMatrix1D apply(DoubleMatrix1D b, DoubleMatrix1D x) {
        if (x == null) {
            x = b.like();
        }

        return x.assign(b);
    }

    public DoubleMatrix1D transApply(DoubleMatrix1D b, DoubleMatrix1D x) {
        if (x == null) {
            x = b.like();
        }

        return x.assign(b);
    }

    public void setMatrix(DoubleMatrix2D A) {
        // nothing to do
    }

}
