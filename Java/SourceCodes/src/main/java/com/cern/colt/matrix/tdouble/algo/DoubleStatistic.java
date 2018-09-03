/*
Copyright (C) 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package com.cern.colt.matrix.tdouble.algo;

import com.hep.aida.tdouble.bin.DynamicDoubleBin1D;

import java.util.concurrent.Future;

import com.cern.colt.function.tdouble.DoubleDoubleFunction;
import com.cern.colt.matrix.tdouble.DoubleFactory1D;
import com.cern.colt.matrix.tdouble.DoubleFactory2D;
import com.cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.cern.colt.matrix.tdouble.DoubleMatrix3D;
import com.cern.jet.math.tdouble.DoubleFunctions;
import com.cern.jet.random.tdouble.engine.DoubleRandomEngine;
import com.edu.emory.mathcs.utils.pc.ConcurrencyUtils;

/**
 * Basic statistics operations on matrices. Computation of covariance,
 * correlation, distance matrix. Random sampling views. Conversion to histograms
 * with and without OLAP cube operators. Conversion to bins with retrieval of
 * statistical bin measures. Also see {@link com.cern.jet.stat} and
 * {@link com.hep.aida.tdouble.bin}, in particular
 * {@link DynamicDoubleBin1D}.
 * <p>
 * Examples:
 * <table border="1" cellspacing="0" dwcopytype="CopyTableRow">
 * <tr valign="top" align="center">
 * <td><tt>A</tt></td>
 * <td><tt>covariance(A)</tt></td>
 * <td><tt>correlation(covariance(A))</tt></td>
 * <td><tt>distance(A,EUCLID)</tt></td>
 * </tr>
 * <tr valign="top">
 * <td><tt> 4&nbsp;x&nbsp;3&nbsp;matrix<br>
 1&nbsp;&nbsp;2&nbsp;&nbsp;&nbsp;3<br>
 2&nbsp;&nbsp;4&nbsp;&nbsp;&nbsp;6<br>
 3&nbsp;&nbsp;6&nbsp;&nbsp;&nbsp;9<br>
 4&nbsp;-8&nbsp;-10 </tt></td>
 * <td><tt> 3&nbsp;x&nbsp;3&nbsp;matrix<br>
 &nbsp;1.25&nbsp;-3.5&nbsp;-4.5<br>
 -3.5&nbsp;&nbsp;29&nbsp;&nbsp;&nbsp;39&nbsp;&nbsp;<br>
 -4.5&nbsp;&nbsp;39&nbsp;&nbsp;&nbsp;52.5 </tt></td>
 * <td><tt> 3&nbsp;x&nbsp;3&nbsp;matrix<br>
 &nbsp;1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;-0.581318&nbsp;-0.555492<br>
 -0.581318&nbsp;&nbsp;1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;0.999507<br>
 -0.555492&nbsp;&nbsp;0.999507&nbsp;&nbsp;1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 
 </tt></td>
 * <td><tt> 3&nbsp;x&nbsp;3&nbsp;matrix<br>
 &nbsp;0&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;12.569805&nbsp;15.874508<br>
 12.569805&nbsp;&nbsp;0&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;4.242641<br>
 15.874508&nbsp;&nbsp;4.242641&nbsp;&nbsp;0&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 
 </tt> <tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; </tt></td>
 * </tr>
 * </table>
 * 
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 */
public class DoubleStatistic extends Object {
    private static final DoubleFunctions F = DoubleFunctions.functions;

    /**
     * Euclidean distance function; <tt>Sqrt(Sum( (x[i]-y[i])^2 ))</tt>.
     */
    public static final VectorVectorFunction EUCLID = new VectorVectorFunction() {
        public final double apply(DoubleMatrix1D a, DoubleMatrix1D b) {
            return Math.sqrt(a.aggregate(b, DoubleFunctions.plus, DoubleFunctions.chain(DoubleFunctions.square,
                    DoubleFunctions.minus)));
        }
    };

    /**
     * Bray-Curtis distance function;
     * <tt>Sum( abs(x[i]-y[i]) )  /  Sum( x[i]+y[i] )</tt>.
     */
    public static final VectorVectorFunction BRAY_CURTIS = new VectorVectorFunction() {
        public final double apply(DoubleMatrix1D a, DoubleMatrix1D b) {
            return a.aggregate(b, DoubleFunctions.plus, DoubleFunctions.chain(DoubleFunctions.abs,
                    DoubleFunctions.minus))
                    / a.aggregate(b, DoubleFunctions.plus, DoubleFunctions.plus);
        }
    };

    /**
     * Canberra distance function;
     * <tt>Sum( abs(x[i]-y[i]) / abs(x[i]+y[i]) )</tt>.
     */
    public static final VectorVectorFunction CANBERRA = new VectorVectorFunction() {
        DoubleDoubleFunction fun = new DoubleDoubleFunction() {
            public final double apply(double a, double b) {
                return Math.abs(a - b) / Math.abs(a + b);
            }
        };

        public final double apply(DoubleMatrix1D a, DoubleMatrix1D b) {
            return a.aggregate(b, DoubleFunctions.plus, fun);
        }
    };

    /**
     * Maximum distance function; <tt>Max( abs(x[i]-y[i]) )</tt>.
     */
    public static final VectorVectorFunction MAXIMUM = new VectorVectorFunction() {
        public final double apply(DoubleMatrix1D a, DoubleMatrix1D b) {
            return a.aggregate(b, DoubleFunctions.max, DoubleFunctions
                    .chain(DoubleFunctions.abs, DoubleFunctions.minus));
        }
    };

    /**
     * Manhattan distance function; <tt>Sum( abs(x[i]-y[i]) )</tt>.
     */
    public static final VectorVectorFunction MANHATTAN = new VectorVectorFunction() {
        public final double apply(DoubleMatrix1D a, DoubleMatrix1D b) {
            return a.aggregate(b, DoubleFunctions.plus, DoubleFunctions.chain(DoubleFunctions.abs,
                    DoubleFunctions.minus));
        }
    };

    /**
     * Interface that represents a function object: a function that takes two
     * argument vectors and returns a single value.
     */
    public interface VectorVectorFunction {
        /**
         * Applies a function to two argument vectors.
         * 
         * @param x
         *            the first argument vector passed to the function.
         * @param y
         *            the second argument vector passed to the function.
         * @return the result of the function.
         */
        abstract public double apply(DoubleMatrix1D x,
                                     DoubleMatrix1D y);
    }

    /**
     * Makes this class non instantiable, but still let's others inherit from
     * it.
     */
    protected DoubleStatistic() {
    }

    /**
     * Applies the given aggregation functions to each column and stores the
     * results in a the result matrix. If matrix has shape <tt>m x n</tt>, then
     * result must have shape <tt>aggr.length x n</tt>. Tip: To do aggregations
     * on rows use dice views (transpositions), as in
     * <tt>aggregate(matrix.viewDice(),aggr,result.viewDice())</tt>.
     * 
     * @param matrix
     *            any matrix; a column holds the values of a given variable.
     * @param aggr
     *            the aggregation functions to be applied to each column.
     * @param result
     *            the matrix to hold the aggregation results.
     * @return <tt>result</tt> (for convenience only).
     * @see DoubleFormatter
     * @see com.hep.aida.tdouble.bin.DoubleBinFunction1D
     * @see com.hep.aida.tdouble.bin.DoubleBinFunctions1D
     */
    public static DoubleMatrix2D aggregate(DoubleMatrix2D matrix, com.hep.aida.tdouble.bin.DoubleBinFunction1D[] aggr,
            DoubleMatrix2D result) {
        DynamicDoubleBin1D bin = new DynamicDoubleBin1D();
        double[] elements = new double[matrix.rows()];
        com.cern.colt.list.tdouble.DoubleArrayList values = new com.cern.colt.list.tdouble.DoubleArrayList(elements);
        for (int column = matrix.columns(); --column >= 0;) {
            matrix.viewColumn(column).toArray(elements); // copy column into
            // values
            bin.clear();
            bin.addAllOf(values);
            for (int i = aggr.length; --i >= 0;) {
                result.set(i, column, aggr[i].apply(bin));
            }
        }
        return result;
    }

    /**
     * Fills all cell values of the given vector into a bin from which
     * statistics measures can be retrieved efficiently. Cells values are
     * copied. <br>
     * Tip: Use <tt>System.out.println(bin(vector))</tt> to print most measures
     * computed by the bin. Example:
     * <table>
     * <td class="PRE">
     * 
     * <pre>
     * 	 Size: 20000
     * 	 Sum: 299858.02350278624
     * 	 SumOfSquares: 5399184.154095971
     * 	 Min: 0.8639113139711261
     * 	 Max: 59.75331890541892
     * 	 Mean: 14.992901175139313
     * 	 RMS: 16.43043540825375
     * 	 Variance: 45.17438077634358
     * 	 Standard deviation: 6.721188940681818
     * 	 Standard error: 0.04752598277592142
     * 	 Geometric mean: 13.516615397064466
     * 	 Product: Infinity
     * 	 Harmonic mean: 11.995174297952191
     * 	 Sum of inversions: 1667.337172700724
     * 	 Skew: 0.8922838940067878
     * 	 Kurtosis: 1.1915828121825598
     * 	 Sum of powers(3): 1.1345828465808412E8
     * 	 Sum of powers(4): 2.7251055344494686E9
     * 	 Sum of powers(5): 7.367125643433887E10
     * 	 Sum of powers(6): 2.215370909100143E12
     * 	 Moment(0,0): 1.0
     * 	 Moment(1,0): 14.992901175139313
     * 	 Moment(2,0): 269.95920770479853
     * 	 Moment(3,0): 5672.914232904206
     * 	 Moment(4,0): 136255.27672247344
     * 	 Moment(5,0): 3683562.8217169433
     * 	 Moment(6,0): 1.1076854545500715E8
     * 	 Moment(0,mean()): 1.0
     * 	 Moment(1,mean()): -2.0806734113421045E-14
     * 	 Moment(2,mean()): 45.172122057305664
     * 	 Moment(3,mean()): 270.92018671421
     * 	 Moment(4,mean()): 8553.8664869067
     * 	 Moment(5,mean()): 153357.41712233616
     * 	 Moment(6,mean()): 4273757.570142922
     * 	 25%, 50% and 75% Quantiles: 10.030074811938091, 13.977982089912224,
     * 	 18.86124362967137
     * 	 quantileInverse(mean): 0.559163335012079
     * 	 Distinct elements &amp; frequencies not printed (too many).
     * 
     * </pre>
     * 
     * </td>
     * </table>
     * 
     * @param vector
     *            the vector to analyze.
     * @return a bin holding the statistics measures of the vector.
     */
    public static DynamicDoubleBin1D bin(DoubleMatrix1D vector) {
        DynamicDoubleBin1D bin = new DynamicDoubleBin1D();
        bin.addAllOf(DoubleFactory1D.dense.toList(vector));
        return bin;
    }

    /**
     * Modifies the given covariance matrix to be a correlation matrix
     * (in-place). The correlation matrix is a square, symmetric matrix
     * consisting of nothing but correlation coefficients. The rows and the
     * columns represent the variables, the cells represent correlation
     * coefficients. The diagonal cells (i.e. the correlation between a variable
     * and itself) will equal 1, for the simple reason that the correlation
     * coefficient of a variable with itself equals 1. The correlation of two
     * column vectors x and y is given by
     * <tt>corr(x,y) = cov(x,y) / (stdDev(x)*stdDev(y))</tt> (Pearson's
     * correlation coefficient). A correlation coefficient varies between -1
     * (for a perfect negative relationship) to +1 (for a perfect positive
     * relationship). See the <A
     * HREF="http://www.cquest.utoronto.ca/geog/ggr270y/notes/not05efg.html">
     * math definition</A> and <A HREF="http://www.stat.berkeley.edu/users/stark/SticiGui/Text/gloss.htm#correlation_coef"
     * > another def</A>. Compares two column vectors at a time. Use dice views
     * to compare two row vectors at a time.
     * 
     * @param covariance
     *            a covariance matrix, as, for example, returned by method
     *            {@link #covariance(DoubleMatrix2D)}.
     * @return the modified covariance, now correlation matrix (for convenience
     *         only).
     */
    public static DoubleMatrix2D correlation(DoubleMatrix2D covariance) {
        for (int i = covariance.columns(); --i >= 0;) {
            for (int j = i; --j >= 0;) {
                double stdDev1 = Math.sqrt(covariance.getQuick(i, i));
                double stdDev2 = Math.sqrt(covariance.getQuick(j, j));
                double cov = covariance.getQuick(i, j);
                double corr = cov / (stdDev1 * stdDev2);

                covariance.setQuick(i, j, corr);
                covariance.setQuick(j, i, corr); // symmetric
            }
        }
        for (int i = covariance.columns(); --i >= 0;)
            covariance.setQuick(i, i, 1);

        return covariance;
    }

    /**
     * Constructs and returns the covariance matrix of the given matrix. The
     * covariance matrix is a square, symmetric matrix consisting of nothing but
     * covariance coefficients. The rows and the columns represent the
     * variables, the cells represent covariance coefficients. The diagonal
     * cells (i.e. the covariance between a variable and itself) will equal the
     * variances. The covariance of two column vectors x and y is given by
     * <tt>cov(x,y) = (1/n) * Sum((x[i]-mean(x)) * (y[i]-mean(y)))</tt>. See the
     * <A HREF="http://www.cquest.utoronto.ca/geog/ggr270y/notes/not05efg.html">
     * math definition</A>. Compares two column vectors at a time. Use dice
     * views to compare two row vectors at a time.
     * 
     * @param matrix
     *            any matrix; a column holds the values of a given variable.
     * @return the covariance matrix (<tt>n x n, n=matrix.columns</tt>).
     */
    public static DoubleMatrix2D covariance(DoubleMatrix2D matrix) {
        int rows = matrix.rows();
        int columns = matrix.columns();
        DoubleMatrix2D covariance = new com.cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D(columns, columns);

        double[] sums = new double[columns];
        DoubleMatrix1D[] cols = new DoubleMatrix1D[columns];
        for (int i = columns; --i >= 0;) {
            cols[i] = matrix.viewColumn(i);
            sums[i] = cols[i].zSum();
        }

        for (int i = columns; --i >= 0;) {
            for (int j = i + 1; --j >= 0;) {
                double sumOfProducts = cols[i].zDotProduct(cols[j]);
                double cov = (sumOfProducts - sums[i] * sums[j] / rows) / rows;
                covariance.setQuick(i, j, cov);
                covariance.setQuick(j, i, cov); // symmetric
            }
        }
        return covariance;
    }

    /**
     * 2-d OLAP cube operator; Fills all cells of the given vectors into the
     * given histogram. If you use com.hep.aida.ref.Converter.toString(histo) on the
     * result, the OLAP cube of x-"column" vs. y-"column" , summing the weights
     * "column" will be printed. For example, aggregate sales by product by
     * region.
     * <p>
     * Computes the distinct values of x and y, yielding histogram axes that
     * capture one distinct value per bin. Then fills the histogram.
     * <p>
     * Example output:
     * <table>
     * <td class="PRE">
     * 
     * <pre>
     * 	 Cube:
     * 	    Entries=5000, ExtraEntries=0
     * 	    MeanX=4.9838, RmsX=NaN
     * 	    MeanY=2.5304, RmsY=NaN
     * 	    xAxis: Min=0, Max=10, Bins=11
     * 	    yAxis: Min=0, Max=5, Bins=6
     * 	 Heights:
     * 	       | X
     * 	       | 0   1   2   3   4   5   6   7   8   9   10  | Sum 
     * 	 ----------------------------------------------------------
     * 	 Y 5   |  30  53  51  52  57  39  65  61  55  49  22 |  534
     * 	   4   |  43 106 112  96  92  94 107  98  98 110  47 | 1003
     * 	   3   |  39 134  87  93 102 103 110  90 114  98  51 | 1021
     * 	   2   |  44  81 113  96 101  86 109  83 111  93  42 |  959
     * 	   1   |  54  94 103  99 115  92  98  97 103  90  44 |  989
     * 	   0   |  24  54  52  44  42  56  46  47  56  53  20 |  494
     * 	 ----------------------------------------------------------
     * 	   Sum | 234 522 518 480 509 470 535 476 537 493 226 |
     * 
     * </pre>
     * 
     * </td>
     * </table>
     * 
     * @return the histogram containing the cube.
     * @throws IllegalArgumentException
     *             if
     *             <tt>x.size() != y.size() || y.size() != weights.size()</tt>.
     */
    public static com.hep.aida.tdouble.DoubleIHistogram2D cube(DoubleMatrix1D x, DoubleMatrix1D y, DoubleMatrix1D weights) {
        if (x.size() != y.size() || y.size() != weights.size())
            throw new IllegalArgumentException("vectors must have same size");

        double epsilon = 1.0E-5f;
        com.cern.colt.list.tdouble.DoubleArrayList distinct = new com.cern.colt.list.tdouble.DoubleArrayList();
        double[] vals = new double[(int) x.size()];
        com.cern.colt.list.tdouble.DoubleArrayList sorted = new com.cern.colt.list.tdouble.DoubleArrayList(vals);

        // compute distinct values of x
        x.toArray(vals); // copy x into vals
        sorted.sort();
        com.cern.jet.stat.tdouble.DoubleDescriptive.frequencies(sorted, distinct, null);
        // since bins are right-open [from,to) we need an additional dummy bin
        // so that the last distinct value does not fall into the overflow bin
        if (distinct.size() > 0)
            distinct.add(distinct.get(distinct.size() - 1) + epsilon);
        distinct.trimToSize();
        com.hep.aida.tdouble.DoubleIAxis xaxis = new com.hep.aida.tdouble.ref.DoubleVariableAxis(distinct.elements());

        // compute distinct values of y
        y.toArray(vals);
        sorted.sort();
        com.cern.jet.stat.tdouble.DoubleDescriptive.frequencies(sorted, distinct, null);
        // since bins are right-open [from,to) we need an additional dummy bin
        // so that the last distinct value does not fall into the overflow bin
        if (distinct.size() > 0)
            distinct.add(distinct.get(distinct.size() - 1) + epsilon);
        distinct.trimToSize();
        com.hep.aida.tdouble.DoubleIAxis yaxis = new com.hep.aida.tdouble.ref.DoubleVariableAxis(distinct.elements());

        com.hep.aida.tdouble.DoubleIHistogram2D histo = new com.hep.aida.tdouble.ref.DoubleHistogram2D("Cube", xaxis, yaxis);
        return histogram(histo, x, y, weights);
    }

    /**
     * 3-d OLAP cube operator; Fills all cells of the given vectors into the
     * given histogram. If you use com.hep.aida.ref.Converter.toString(histo) on the
     * result, the OLAP cube of x-"column" vs. y-"column" vs. z-"column",
     * summing the weights "column" will be printed. For example, aggregate
     * sales by product by region by time.
     * <p>
     * Computes the distinct values of x and y and z, yielding histogram axes
     * that capture one distinct value per bin. Then fills the histogram.
     * 
     * @return the histogram containing the cube.
     * @throws IllegalArgumentException
     *             if
     *             <tt>x.size() != y.size() || x.size() != z.size() || x.size() != weights.size()</tt>
     *             .
     */
    public static com.hep.aida.tdouble.DoubleIHistogram3D cube(DoubleMatrix1D x, DoubleMatrix1D y, DoubleMatrix1D z,
            DoubleMatrix1D weights) {
        if (x.size() != y.size() || x.size() != z.size() || x.size() != weights.size())
            throw new IllegalArgumentException("vectors must have same size");

        double epsilon = 1.0E-5f;
        com.cern.colt.list.tdouble.DoubleArrayList distinct = new com.cern.colt.list.tdouble.DoubleArrayList();
        double[] vals = new double[(int) x.size()];
        com.cern.colt.list.tdouble.DoubleArrayList sorted = new com.cern.colt.list.tdouble.DoubleArrayList(vals);

        // compute distinct values of x
        x.toArray(vals); // copy x into vals
        sorted.sort();
        com.cern.jet.stat.tdouble.DoubleDescriptive.frequencies(sorted, distinct, null);
        // since bins are right-open [from,to) we need an additional dummy bin
        // so that the last distinct value does not fall into the overflow bin
        if (distinct.size() > 0)
            distinct.add(distinct.get(distinct.size() - 1) + epsilon);
        distinct.trimToSize();
        com.hep.aida.tdouble.DoubleIAxis xaxis = new com.hep.aida.tdouble.ref.DoubleVariableAxis(distinct.elements());

        // compute distinct values of y
        y.toArray(vals);
        sorted.sort();
        com.cern.jet.stat.tdouble.DoubleDescriptive.frequencies(sorted, distinct, null);
        // since bins are right-open [from,to) we need an additional dummy bin
        // so that the last distinct value does not fall into the overflow bin
        if (distinct.size() > 0)
            distinct.add(distinct.get(distinct.size() - 1) + epsilon);
        distinct.trimToSize();
        com.hep.aida.tdouble.DoubleIAxis yaxis = new com.hep.aida.tdouble.ref.DoubleVariableAxis(distinct.elements());

        // compute distinct values of z
        z.toArray(vals);
        sorted.sort();
        com.cern.jet.stat.tdouble.DoubleDescriptive.frequencies(sorted, distinct, null);
        // since bins are right-open [from,to) we need an additional dummy bin
        // so that the last distinct value does not fall into the overflow bin
        if (distinct.size() > 0)
            distinct.add(distinct.get(distinct.size() - 1) + epsilon);
        distinct.trimToSize();
        com.hep.aida.tdouble.DoubleIAxis zaxis = new com.hep.aida.tdouble.ref.DoubleVariableAxis(distinct.elements());

        com.hep.aida.tdouble.DoubleIHistogram3D histo = new com.hep.aida.tdouble.ref.DoubleHistogram3D("Cube", xaxis, yaxis,
                zaxis);
        return histogram(histo, x, y, z, weights);
    }

    /**
     * Demonstrates usage of this class.
     */
    public static void demo1() {
        double[][] values = { { 1, 2, 3 }, { 2, 4, 6 }, { 3, 6, 9 }, { 4, -8, -10 } };
        DoubleFactory2D factory = DoubleFactory2D.dense;
        DoubleMatrix2D A = factory.make(values);
        System.out.println("\n\nmatrix=" + A);
        System.out.println("\ncovar1=" + covariance(A));
        // System.out.println(correlation(covariance(A)));
        // System.out.println(distance(A,EUCLID));

        // System.out.println(com.cern.colt.matrixpattern.Converting.toHTML(A.toString()));
        // System.out.println(com.cern.colt.matrixpattern.Converting.toHTML(covariance(A).toString()));
        // System.out.println(com.cern.colt.matrixpattern.Converting.toHTML(correlation(covariance(A)).toString()));
        // System.out.println(com.cern.colt.matrixpattern.Converting.toHTML(distance(A,EUCLID).toString()));
    }

    /**
     * Demonstrates usage of this class.
     */
    public static void demo2(int rows, int columns, boolean print) {
        System.out.println("\n\ninitializing...");
        DoubleFactory2D factory = DoubleFactory2D.dense;
        DoubleMatrix2D A = factory.ascending(rows, columns);
        // double value = 1;
        // DoubleMatrix2D A = factory.make(rows,columns);
        // A.assign(value);

        System.out.println("benchmarking correlation...");

        com.cern.colt.Timer timer = new com.cern.colt.Timer().start();
        DoubleMatrix2D corr = correlation(covariance(A));
        timer.stop().display();

        if (print) {
            System.out.println("printing result...");
            System.out.println(corr);
        }
        System.out.println("done.");
    }

    /**
     * Demonstrates usage of this class.
     */
    public static void demo3(VectorVectorFunction norm) {
        double[][] values = { { -0.9611052f, -0.25421095f }, { 0.4308269f, -0.69932648f },
                { -1.2071029f, 0.62030596f }, { 1.5345166f, 0.02135884f }, { -1.1341542f, 0.20388430f } };

        System.out.println("\n\ninitializing...");
        DoubleFactory2D factory = DoubleFactory2D.dense;
        DoubleMatrix2D A = factory.make(values).viewDice();

        System.out.println("\nA=" + A.viewDice());
        System.out.println("\ndist=" + distance(A, norm).viewDice());
    }

    /**
     * Constructs and returns the distance matrix of the given matrix. The
     * distance matrix is a square, symmetric matrix consisting of nothing but
     * distance coefficients. The rows and the columns represent the variables,
     * the cells represent distance coefficients. The diagonal cells (i.e. the
     * distance between a variable and itself) will be zero. Compares two column
     * vectors at a time. Use dice views to compare two row vectors at a time.
     * 
     * @param matrix
     *            any matrix; a column holds the values of a given variable
     *            (vector).
     * @param distanceFunction
     *            (EUCLID, CANBERRA, ..., or any user defined distance function
     *            operating on two vectors).
     * @return the distance matrix (<tt>n x n, n=matrix.columns</tt>).
     */
    public static DoubleMatrix2D distance(DoubleMatrix2D matrix, VectorVectorFunction distanceFunction) {
        int columns = matrix.columns();
        DoubleMatrix2D distance = new com.cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D(columns, columns);

        // cache views
        DoubleMatrix1D[] cols = new DoubleMatrix1D[columns];
        for (int i = columns; --i >= 0;) {
            cols[i] = matrix.viewColumn(i);
        }

        // work out all permutations
        for (int i = columns; --i >= 0;) {
            for (int j = i; --j >= 0;) {
                double d = distanceFunction.apply(cols[i], cols[j]);
                distance.setQuick(i, j, d);
                distance.setQuick(j, i, d); // symmetric
            }
        }
        return distance;
    }

    /**
     * Fills all cells of the given vector into the given histogram.
     * 
     * @return <tt>histo</tt> (for convenience only).
     */
    public static com.hep.aida.tdouble.DoubleIHistogram1D histogram(com.hep.aida.tdouble.DoubleIHistogram1D histo,
            DoubleMatrix1D vector) {
        for (int i = (int) vector.size(); --i >= 0;) {
            histo.fill(vector.getQuick(i));
        }
        return histo;
    }

    /**
     * Fills all cells of the given matrix into the given histogram.
     * 
     * @return <tt>histo</tt> (for convenience only).
     */
    public static com.hep.aida.tdouble.DoubleIHistogram1D histogram(final com.hep.aida.tdouble.DoubleIHistogram1D histo,
            final DoubleMatrix2D matrix) {
        histo.fill_2D((double[]) matrix.elements(), matrix.rows(), matrix.columns(), (int) matrix.index(0, 0), matrix
                .rowStride(), matrix.columnStride());
        return histo;
    }

    /**
     * Splits the given matrix into m x n pieces and computes 1D histogram of
     * each piece.
     * 
     * @return <tt>histo</tt> (for convenience only).
     */
    public static com.hep.aida.tdouble.DoubleIHistogram1D[][] histogram(
            final com.hep.aida.tdouble.DoubleIHistogram1D[][] histo, final DoubleMatrix2D matrix, final int m, final int n) {
        int rows = matrix.rows();
        int columns = matrix.columns();
        if (m >= rows) {
            throw new IllegalArgumentException("Parameter m must be smaller than the number of rows in the matrix");
        }
        if (n >= columns) {
            throw new IllegalArgumentException("Parameter n must be smaller than the number of columns in the matrix");
        }
        final int row_size = rows / m;
        final int col_size = columns / n;
        final int[] height = new int[m];
        final int[] width = new int[n];
        for (int r = 0; r < m - 1; r++) {
            height[r] = row_size;
        }
        height[m - 1] = rows - (m - 1) * row_size;
        for (int c = 0; c < n - 1; c++) {
            width[c] = col_size;
        }
        width[n - 1] = columns - (n - 1) * col_size;

        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && (rows * columns >= ConcurrencyUtils.getThreadsBeginN_2D())) {
            nthreads = Math.min(nthreads, m);
            Future<?>[] futures = new Future[nthreads];
            int k = m / nthreads;
            for (int j = 0; j < nthreads; j++) {
                final int firstRow = j * k;
                final int lastRow = (j == nthreads - 1) ? m : firstRow + k;
                futures[j] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        DoubleMatrix2D view = null;
                        for (int r = firstRow; r < lastRow; r++) {
                            for (int c = 0; c < n; c++) {
                                view = matrix.viewPart(r * row_size, c * col_size, height[r], width[c]);
                                histo[r][c].fill_2D((double[]) view.elements(), view.rows(), view.columns(), (int) view
                                        .index(0, 0), view.rowStride(), view.columnStride());
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            DoubleMatrix2D view = null;
            for (int r = 0; r < m; r++) {
                for (int c = 0; c < n; c++) {
                    view = matrix.viewPart(r * row_size, c * col_size, height[r], width[c]);
                    histo[r][c].fill_2D((double[]) view.elements(), view.rows(), view.columns(),
                            (int) view.index(0, 0), view.rowStride(), view.columnStride());
                }
            }
        }
        return histo;
    }

    /**
     * Fills all cells of the given vectors into the given histogram.
     * 
     * @return <tt>histo</tt> (for convenience only).
     * @throws IllegalArgumentException
     *             if <tt>x.size() != y.size()</tt>.
     */
    public static com.hep.aida.tdouble.DoubleIHistogram2D histogram(com.hep.aida.tdouble.DoubleIHistogram2D histo,
            DoubleMatrix1D x, DoubleMatrix1D y) {
        if (x.size() != y.size())
            throw new IllegalArgumentException("vectors must have same size");
        for (int i = (int) x.size(); --i >= 0;) {
            histo.fill(x.getQuick(i), y.getQuick(i));
        }
        return histo;
    }

    /**
     * Fills all cells of the given vectors into the given histogram.
     * 
     * @return <tt>histo</tt> (for convenience only).
     * @throws IllegalArgumentException
     *             if
     *             <tt>x.size() != y.size() || y.size() != weights.size()</tt>.
     */
    public static com.hep.aida.tdouble.DoubleIHistogram2D histogram(com.hep.aida.tdouble.DoubleIHistogram2D histo,
            DoubleMatrix1D x, DoubleMatrix1D y, DoubleMatrix1D weights) {
        if (x.size() != y.size() || y.size() != weights.size())
            throw new IllegalArgumentException("vectors must have same size");
        for (int i = (int) x.size(); --i >= 0;) {
            histo.fill(x.getQuick(i), y.getQuick(i), weights.getQuick(i));
        }
        return histo;
    }

    /**
     * Fills all cells of the given vectors into the given histogram.
     * 
     * @return <tt>histo</tt> (for convenience only).
     * @throws IllegalArgumentException
     *             if
     *             <tt>x.size() != y.size() || x.size() != z.size() || x.size() != weights.size()</tt>
     *             .
     */
    public static com.hep.aida.tdouble.DoubleIHistogram3D histogram(com.hep.aida.tdouble.DoubleIHistogram3D histo,
            DoubleMatrix1D x, DoubleMatrix1D y, DoubleMatrix1D z, DoubleMatrix1D weights) {
        if (x.size() != y.size() || x.size() != z.size() || x.size() != weights.size())
            throw new IllegalArgumentException("vectors must have same size");
        for (int i = (int) x.size(); --i >= 0;) {
            histo.fill(x.getQuick(i), y.getQuick(i), z.getQuick(i), weights.getQuick(i));
        }
        return histo;
    }

    /**
     * Benchmarks covariance computation.
     */
    public static void main(String[] args) {
        int rows = Integer.parseInt(args[0]);
        int columns = Integer.parseInt(args[1]);
        boolean print = args[2].equals("print");
        demo2(rows, columns, print);
    }

    /**
     * Constructs and returns a sampling view with a size of
     * <tt>round(matrix.size() * fraction)</tt>. Samples "without replacement"
     * from the uniform distribution.
     * 
     * @param matrix
     *            any matrix.
     * @param fraction
     *            the percentage to be included in the view.
     * @param randomGenerator
     *            a uniform random number generator; set this parameter to
     *            <tt>null</tt> to use a default generator seeded with the
     *            current time.
     * @return the sampling view.
     * @throws IllegalArgumentException
     *             if
     *             <tt>! (0 <= rowFraction <= 1 && 0 <= columnFraction <= 1)</tt>
     *             .
     * @see com.cern.jet.random.tdouble.sampling.DoubleRandomSampler
     */
    public static DoubleMatrix1D viewSample(DoubleMatrix1D matrix, double fraction, DoubleRandomEngine randomGenerator) {
        // check preconditions and allow for a little tolerance
        double epsilon = 1e-05f;
        if (fraction < 0 - epsilon || fraction > 1 + epsilon)
            throw new IllegalArgumentException();
        if (fraction < 0)
            fraction = 0;
        if (fraction > 1)
            fraction = 1;

        // random generator seeded with current time
        if (randomGenerator == null)
            randomGenerator = new com.cern.jet.random.tdouble.engine.DoubleMersenneTwister((int) System.currentTimeMillis());

        int ncolumns = (int) Math.round(matrix.size() * fraction);
        int max = ncolumns;
        long[] selected = new long[max]; // sampler works on long's, not
        // int's

        // sample
        int n = ncolumns;
        int N = (int) matrix.size();
        com.cern.jet.random.tdouble.sampling.DoubleRandomSampler.sample(n, N, n, 0, selected, 0, randomGenerator);
        int[] selectedCols = new int[n];
        for (int i = 0; i < n; i++)
            selectedCols[i] = (int) selected[i];

        return matrix.viewSelection(selectedCols);
    }

    /**
     * Constructs and returns a sampling view with
     * <tt>round(matrix.rows() * rowFraction)</tt> rows and
     * <tt>round(matrix.columns() * columnFraction)</tt> columns. Samples
     * "without replacement". Rows and columns are randomly chosen from the
     * uniform distribution. Examples:
     * <table border="1" cellspacing="0">
     * <tr valign="top" align="center">
     * <td><div align="left"><tt>matrix</tt></div></td>
     * <td><div align="left"><tt>rowFraction=0.2<br>
     columnFraction=0.2</tt></div></td>
     * <td><div align="left"><tt>rowFraction=0.2<br>
     columnFraction=1.0 </tt></div></td>
     * <td><div align="left"><tt>rowFraction=1.0<br>
     columnFraction=0.2 </tt></div></td>
     * </tr>
     * <tr valign="top">
     * <td><tt> 10&nbsp;x&nbsp;10&nbsp;matrix<br>
     &nbsp;1&nbsp;&nbsp;2&nbsp;&nbsp;3&nbsp;&nbsp;4&nbsp;&nbsp;5&nbsp;&nbsp;6&nbsp;&nbsp;7&nbsp;&nbsp;8&nbsp;&nbsp;9&nbsp;&nbsp;10<br>
     11&nbsp;12&nbsp;13&nbsp;14&nbsp;15&nbsp;16&nbsp;17&nbsp;18&nbsp;19&nbsp;&nbsp;20<br>
     21&nbsp;22&nbsp;23&nbsp;24&nbsp;25&nbsp;26&nbsp;27&nbsp;28&nbsp;29&nbsp;&nbsp;30<br>
     31&nbsp;32&nbsp;33&nbsp;34&nbsp;35&nbsp;36&nbsp;37&nbsp;38&nbsp;39&nbsp;&nbsp;40<br>
     41&nbsp;42&nbsp;43&nbsp;44&nbsp;45&nbsp;46&nbsp;47&nbsp;48&nbsp;49&nbsp;&nbsp;50<br>
     51&nbsp;52&nbsp;53&nbsp;54&nbsp;55&nbsp;56&nbsp;57&nbsp;58&nbsp;59&nbsp;&nbsp;60<br>
     61&nbsp;62&nbsp;63&nbsp;64&nbsp;65&nbsp;66&nbsp;67&nbsp;68&nbsp;69&nbsp;&nbsp;70<br>
     71&nbsp;72&nbsp;73&nbsp;74&nbsp;75&nbsp;76&nbsp;77&nbsp;78&nbsp;79&nbsp;&nbsp;80<br>
     81&nbsp;82&nbsp;83&nbsp;84&nbsp;85&nbsp;86&nbsp;87&nbsp;88&nbsp;89&nbsp;&nbsp;90<br>
     91&nbsp;92&nbsp;93&nbsp;94&nbsp;95&nbsp;96&nbsp;97&nbsp;98&nbsp;99&nbsp;100 
     </tt></td>
     * <td><tt> 2&nbsp;x&nbsp;2&nbsp;matrix<br>
     43&nbsp;50<br>
     53&nbsp;60 </tt></td>
     * <td><tt> 2&nbsp;x&nbsp;10&nbsp;matrix<br>
     41&nbsp;42&nbsp;43&nbsp;44&nbsp;45&nbsp;46&nbsp;47&nbsp;48&nbsp;49&nbsp;&nbsp;50<br>
     91&nbsp;92&nbsp;93&nbsp;94&nbsp;95&nbsp;96&nbsp;97&nbsp;98&nbsp;99&nbsp;100 
     </tt></td>
     * <td><tt> 10&nbsp;x&nbsp;2&nbsp;matrix<br>
     &nbsp;4&nbsp;&nbsp;8<br>
     14&nbsp;18<br>
     24&nbsp;28<br>
     34&nbsp;38<br>
     44&nbsp;48<br>
     54&nbsp;58<br>
     64&nbsp;68<br>
     74&nbsp;78<br>
     84&nbsp;88<br>
     94&nbsp;98 </tt></td>
     * </tr>
     * </table>
     * 
     * @param matrix
     *            any matrix.
     * @param rowFraction
     *            the percentage of rows to be included in the view.
     * @param columnFraction
     *            the percentage of columns to be included in the view.
     * @param randomGenerator
     *            a uniform random number generator; set this parameter to
     *            <tt>null</tt> to use a default generator seeded with the
     *            current time.
     * @return the sampling view.
     * @throws IllegalArgumentException
     *             if
     *             <tt>! (0 <= rowFraction <= 1 && 0 <= columnFraction <= 1)</tt>
     *             .
     * @see com.cern.jet.random.tdouble.sampling.DoubleRandomSampler
     */
    public static DoubleMatrix2D viewSample(DoubleMatrix2D matrix, double rowFraction, double columnFraction,
            DoubleRandomEngine randomGenerator) {
        // check preconditions and allow for a little tolerance
        double epsilon = 1e-05f;
        if (rowFraction < 0 - epsilon || rowFraction > 1 + epsilon)
            throw new IllegalArgumentException();
        if (rowFraction < 0)
            rowFraction = 0;
        if (rowFraction > 1)
            rowFraction = 1;

        if (columnFraction < 0 - epsilon || columnFraction > 1 + epsilon)
            throw new IllegalArgumentException();
        if (columnFraction < 0)
            columnFraction = 0;
        if (columnFraction > 1)
            columnFraction = 1;

        // random generator seeded with current time
        if (randomGenerator == null)
            randomGenerator = new com.cern.jet.random.tdouble.engine.DoubleMersenneTwister((int) System.currentTimeMillis());

        int nrows = (int) Math.round(matrix.rows() * rowFraction);
        int ncolumns = (int) Math.round(matrix.columns() * columnFraction);
        int max = Math.max(nrows, ncolumns);
        long[] selected = new long[max]; // sampler works on long's, not
        // int's

        // sample rows
        int n = nrows;
        int N = matrix.rows();
        com.cern.jet.random.tdouble.sampling.DoubleRandomSampler.sample(n, N, n, 0, selected, 0, randomGenerator);
        int[] selectedRows = new int[n];
        for (int i = 0; i < n; i++)
            selectedRows[i] = (int) selected[i];

        // sample columns
        n = ncolumns;
        N = matrix.columns();
        com.cern.jet.random.tdouble.sampling.DoubleRandomSampler.sample(n, N, n, 0, selected, 0, randomGenerator);
        int[] selectedCols = new int[n];
        for (int i = 0; i < n; i++)
            selectedCols[i] = (int) selected[i];

        return matrix.viewSelection(selectedRows, selectedCols);
    }

    /**
     * Constructs and returns a sampling view with
     * <tt>round(matrix.slices() * sliceFraction)</tt> slices and
     * <tt>round(matrix.rows() * rowFraction)</tt> rows and
     * <tt>round(matrix.columns() * columnFraction)</tt> columns. Samples
     * "without replacement". Slices, rows and columns are randomly chosen from
     * the uniform distribution.
     * 
     * @param matrix
     *            any matrix.
     * @param sliceFraction
     *            the percentage of slices to be included in the view.
     * @param rowFraction
     *            the percentage of rows to be included in the view.
     * @param columnFraction
     *            the percentage of columns to be included in the view.
     * @param randomGenerator
     *            a uniform random number generator; set this parameter to
     *            <tt>null</tt> to use a default generator seeded with the
     *            current time.
     * @return the sampling view.
     * @throws IllegalArgumentException
     *             if
     *             <tt>! (0 <= sliceFraction <= 1 && 0 <= rowFraction <= 1 && 0 <= columnFraction <= 1)</tt>
     *             .
     * @see com.cern.jet.random.tdouble.sampling.DoubleRandomSampler
     */
    public static DoubleMatrix3D viewSample(DoubleMatrix3D matrix, double sliceFraction, double rowFraction,
            double columnFraction, DoubleRandomEngine randomGenerator) {
        // check preconditions and allow for a little tolerance
        double epsilon = 1e-05f;
        if (sliceFraction < 0 - epsilon || sliceFraction > 1 + epsilon)
            throw new IllegalArgumentException();
        if (sliceFraction < 0)
            sliceFraction = 0;
        if (sliceFraction > 1)
            sliceFraction = 1;

        if (rowFraction < 0 - epsilon || rowFraction > 1 + epsilon)
            throw new IllegalArgumentException();
        if (rowFraction < 0)
            rowFraction = 0;
        if (rowFraction > 1)
            rowFraction = 1;

        if (columnFraction < 0 - epsilon || columnFraction > 1 + epsilon)
            throw new IllegalArgumentException();
        if (columnFraction < 0)
            columnFraction = 0;
        if (columnFraction > 1)
            columnFraction = 1;

        // random generator seeded with current time
        if (randomGenerator == null)
            randomGenerator = new com.cern.jet.random.tdouble.engine.DoubleMersenneTwister((int) System.currentTimeMillis());

        int nslices = (int) Math.round(matrix.slices() * sliceFraction);
        int nrows = (int) Math.round(matrix.rows() * rowFraction);
        int ncolumns = (int) Math.round(matrix.columns() * columnFraction);
        int max = Math.max(nslices, Math.max(nrows, ncolumns));
        long[] selected = new long[max]; // sampler works on long's, not
        // int's

        // sample slices
        int n = nslices;
        int N = matrix.slices();
        com.cern.jet.random.tdouble.sampling.DoubleRandomSampler.sample(n, N, n, 0, selected, 0, randomGenerator);
        int[] selectedSlices = new int[n];
        for (int i = 0; i < n; i++)
            selectedSlices[i] = (int) selected[i];

        // sample rows
        n = nrows;
        N = matrix.rows();
        com.cern.jet.random.tdouble.sampling.DoubleRandomSampler.sample(n, N, n, 0, selected, 0, randomGenerator);
        int[] selectedRows = new int[n];
        for (int i = 0; i < n; i++)
            selectedRows[i] = (int) selected[i];

        // sample columns
        n = ncolumns;
        N = matrix.columns();
        com.cern.jet.random.tdouble.sampling.DoubleRandomSampler.sample(n, N, n, 0, selected, 0, randomGenerator);
        int[] selectedCols = new int[n];
        for (int i = 0; i < n; i++)
            selectedCols[i] = (int) selected[i];

        return matrix.viewSelection(selectedSlices, selectedRows, selectedCols);
    }
}
