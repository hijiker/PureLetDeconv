/*
Copyright (C) 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package com.cern.jet.math.tlong;

/**
 * Only for performance tuning of compute longensive linear algebraic
 * computations. Constructs functions that return one of
 * <ul>
 * <li><tt>a + b*constant</tt>
 * <li><tt>a - b*constant</tt>
 * <li><tt>a + b/constant</tt>
 * <li><tt>a - b/constant</tt>
 * </ul>
 * <tt>a</tt> and <tt>b</tt> are variables, <tt>constant</tt> is fixed, but for
 * performance reasons publicly accessible. Longended to be passed to
 * <tt>matrix.assign(otherMatrix,function)</tt> methods.
 */
public final class LongPlusMultFirst implements com.cern.colt.function.tlong.LongLongFunction {
    /**
     * Public read/write access to avoid frequent object construction.
     */
    public long multiplicator;

    /**
     * Insert the method's description here. Creation date: (8/10/99 19:12:09)
     */
    protected LongPlusMultFirst(final long multiplicator) {
        this.multiplicator = multiplicator;
    }

    /**
     * Returns the result of the function evaluation.
     */
    public final long apply(long a, long b) {
        return a * multiplicator + b;
    }

    /**
     * <tt>a - b*constant</tt>.
     */
    public static LongPlusMultFirst minusMult(final long constant) {
        return new LongPlusMultFirst(-constant);
    }

    /**
     * <tt>a + b*constant</tt>.
     */
    public static LongPlusMultFirst plusMult(final long constant) {
        return new LongPlusMultFirst(constant);
    }
}
