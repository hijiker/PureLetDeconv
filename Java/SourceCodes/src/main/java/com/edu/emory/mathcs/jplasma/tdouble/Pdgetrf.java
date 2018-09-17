/* ***** BEGIN LICENSE BLOCK *****
 * -- Innovative Computing Laboratory
 * -- Electrical Engineering and Computer Science Department
 * -- University of Tennessee
 * -- (C) Copyright 2008
 *
 * Redistribution  and  use  in  source and binary forms, with or without
 * modification,  are  permitted  provided  that the following conditions
 * are met:
 *
 * * Redistributions  of  source  code  must  retain  the above copyright
 *   notice,  this  list  of  conditions  and  the  following  disclaimer.
 * * Redistributions  in  binary  form must reproduce the above copyright
 *   notice,  this list of conditions and the following disclaimer in the
 *   documentation  and/or other materials provided with the distribution.
 * * Neither  the  name of the University of Tennessee, Knoxville nor the
 *   names of its contributors may be used to endorse or promote products
 *   derived from this software without specific prior written permission.
 *
 * THIS  SOFTWARE  IS  PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS''  AND  ANY  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A  PARTICULAR  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL,  EXEMPLARY,  OR  CONSEQUENTIAL  DAMAGES  (INCLUDING,  BUT NOT
 * LIMITED  TO,  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA,  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY  OF  LIABILITY,  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF  THIS  SOFTWARE,  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ***** END LICENSE BLOCK ***** */

package com.edu.emory.mathcs.jplasma.tdouble;

import com.edu.emory.mathcs.jplasma.tdouble.Dcommon;
import com.edu.emory.mathcs.jplasma.tdouble.DcoreBLAS;
import org.netlib.util.intW;

class Pdgetrf {

    private Pdgetrf() {

    }

    /*////////////////////////////////////////////////////////////////////////////////////////
     *  Parallel tile LU factorization
     */
    protected static void plasma_pDGETRF(int M, int N, double[] A, int A_offset, int NB, int NBNBSIZE, int IBNBSIZE,
            int IB, int MT, int NT, double[] L, int L_offset, int[] IPIV, int IPIV_offset, intW INFO, int cores_num,
            int my_core_id) {
        int[] progress = com.edu.emory.mathcs.jplasma.tdouble.Dcommon.plasma_aux.progress;
        int k, m, n;
        int next_k;
        int next_m;
        int next_n;

        k = 0;
        n = my_core_id;
        while (n >= NT) {
            k++;
            n = n - NT + k;
        }
        m = k;

        while (k < Math.min(MT, NT) && n < NT) {
            next_n = n;
            next_m = m;
            next_k = k;

            next_m++;
            if (next_m == MT) {
                next_n += cores_num;
                while (next_n >= NT && next_k < Math.min(MT, NT)) {
                    next_k++;
                    next_n = next_n - NT + next_k;
                }
                next_m = next_k;
            }

            if (n == k) {
                if (m == k) {
                    while (progress[(k) + MT * (k)] != k - 1)
                        com.edu.emory.mathcs.jplasma.tdouble.Dcommon.delay();
                    DcoreBLAS.core_DGETRF(k == MT - 1 ? M - k * NB : NB, k == NT - 1 ? N - k * NB : NB, IB, A, A_offset
                            + NBNBSIZE * (k) + NBNBSIZE * MT * (k), NB, L, L_offset + IBNBSIZE * (k) + IBNBSIZE * MT
                            * (k), NB, IPIV, IPIV_offset + NB * (k) + NB * MT * (k), INFO);
                    if (INFO.val != 0)
                        INFO.val += NB * k;
                    progress[(k) + MT * (k)] = k;
                } else {
                    while (progress[(m) + MT * (k)] != k - 1)
                        com.edu.emory.mathcs.jplasma.tdouble.Dcommon.delay();
                    DcoreBLAS.core_DTSTRF(m == MT - 1 ? M - m * NB : NB, k == NT - 1 ? N - k * NB : NB, IB, NB, A,
                            A_offset + NBNBSIZE * (k) + NBNBSIZE * MT * (k), NB, A, A_offset + NBNBSIZE * (m)
                                    + NBNBSIZE * MT * (k), NB, L, L_offset + IBNBSIZE * (m) + IBNBSIZE * MT * (k), IB,
                            IPIV, IPIV_offset + NB * (m) + NB * MT * (k), INFO);
                    if (INFO.val != 0)
                        INFO.val += NB * k;
                    progress[(m) + MT * (k)] = k;
                }
            } else {
                if (m == k) {
                    while (progress[(k) + MT * (k)] != k)
                        com.edu.emory.mathcs.jplasma.tdouble.Dcommon.delay();
                    while (progress[(k) + MT * (n)] != k - 1)
                        com.edu.emory.mathcs.jplasma.tdouble.Dcommon.delay();
                    DcoreBLAS.core_DGESSM(k == MT - 1 ? M - k * NB : NB, n == NT - 1 ? N - n * NB : NB, NB, IB, IPIV,
                            IPIV_offset + NB * (k) + NB * MT * (k), A, A_offset + NBNBSIZE * (k) + NBNBSIZE * MT * (k),
                            NB, A, A_offset + NBNBSIZE * (k) + NBNBSIZE * MT * (n), NB);
                } else {
                    while (progress[(m) + MT * (k)] != k)
                        com.edu.emory.mathcs.jplasma.tdouble.Dcommon.delay();
                    while (progress[(m) + MT * (n)] != k - 1)
                        com.edu.emory.mathcs.jplasma.tdouble.Dcommon.delay();
                    DcoreBLAS.core_DSSSSM(NB, m == MT - 1 ? M - m * NB : NB, n == NT - 1 ? N - n * NB : NB, IB, NB,
                            IPIV, IPIV_offset + NB * (m) + NB * MT * (k), L, L_offset + IBNBSIZE * (m) + IBNBSIZE * MT
                                    * (k), IB, A, A_offset + NBNBSIZE * (m) + NBNBSIZE * MT * (k), NB, A, A_offset
                                    + NBNBSIZE * (k) + NBNBSIZE * MT * (n), NB, A,
                            NBNBSIZE * (m) + NBNBSIZE * MT * (n), NB);
                    progress[(m) + MT * (n)] = k;
                }
            }
            n = next_n;
            m = next_m;
            k = next_k;
        }
    }

}
