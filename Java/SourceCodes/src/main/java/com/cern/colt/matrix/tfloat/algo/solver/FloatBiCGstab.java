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
/*
 * Derived from public domain software at http://www.netlib.org/templates
 */

package com.cern.colt.matrix.tfloat.algo.solver;

import com.cern.colt.matrix.tfloat.FloatMatrix1D;
import com.cern.colt.matrix.tfloat.FloatMatrix2D;
import com.cern.jet.math.tfloat.FloatFunctions;

/**
 * BiCG stablized solver. BiCGstab solves the unsymmetric linear system
 * <code>Ax = b</code> using the Preconditioned BiConjugate Gradient Stabilized
 * method
 * 
 * @author Templates
 */
public class FloatBiCGstab extends AbstractFloatIterativeSolver {

    /**
     * Vectors for use in the iterative solution process
     */
    private FloatMatrix1D p, s, phat, shat, t, v, temp, r, rtilde;

    /**
     * Constructor for BiCGstab. Uses the given vector as template for creating
     * scratch vectors. Typically, the solution or the right hand side vector
     * can be passed, and the template is not modified
     * 
     * @param template
     *            Vector to use as template for the work vectors needed in the
     *            solution process
     */
    public FloatBiCGstab(FloatMatrix1D template) {
        p = template.copy();
        s = template.copy();
        phat = template.copy();
        shat = template.copy();
        t = template.copy();
        v = template.copy();
        temp = template.copy();
        r = template.copy();
        rtilde = template.copy();
    }

    public FloatMatrix1D solve(FloatMatrix2D A, FloatMatrix1D b, FloatMatrix1D x)
            throws IterativeSolverFloatNotConvergedException {
        checkSizes(A, b, x);

        float rho_1 = 1, rho_2 = 1, alpha = 1, beta = 1, omega = 1;

        A.zMult(x, r.assign(b), -1, 1, false);
        rtilde.assign(r);

        for (iter.setFirst(); !iter.converged(r, x); iter.next()) {
            rho_1 = rtilde.zDotProduct(r);

            if (rho_1 == 0)
                throw new IterativeSolverFloatNotConvergedException(FloatNotConvergedException.Reason.Breakdown, "rho",
                        iter);

            if (omega == 0)
                throw new IterativeSolverFloatNotConvergedException(FloatNotConvergedException.Reason.Breakdown,
                        "omega", iter);

            if (iter.isFirst())
                p.assign(r);
            else {
                beta = (rho_1 / rho_2) * (alpha / omega);

                // temp = p - omega * v
                temp.assign(v, FloatFunctions.multSecond(-omega)).assign(p, FloatFunctions.plus);
                // p = r + beta * temp = r + beta * (p - omega * v)
                p.assign(r).assign(temp, FloatFunctions.plusMultSecond(beta));
            }

            M.apply(p, phat);
            A.zMult(phat, v);
            alpha = rho_1 / rtilde.zDotProduct(v);
            s.assign(r).assign(v, FloatFunctions.plusMultSecond(-alpha));

            if (iter.converged(s, x))
                return x.assign(phat, FloatFunctions.plusMultSecond(alpha));
            ;

            M.apply(s, shat);
            A.zMult(shat, t);
            omega = t.zDotProduct(s) / t.zDotProduct(t);
            x.assign(phat, FloatFunctions.plusMultSecond(alpha));
            x.assign(shat, FloatFunctions.plusMultSecond(omega));
            r.assign(s).assign(t, FloatFunctions.plusMultSecond(-omega));

            rho_2 = rho_1;
        }

        return x;
    }

}
