/*
Copyright (C) 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package com.cern.colt.matrix.tlong.impl;

import java.io.IOException;

import com.cern.colt.map.tlong.AbstractLongLongMap;
import com.cern.colt.map.tlong.OpenLongLongHashMap;
import com.cern.colt.matrix.io.MatrixInfo;
import com.cern.colt.matrix.io.MatrixSize;
import com.cern.colt.matrix.io.MatrixVectorReader;
import com.cern.colt.matrix.tlong.LongMatrix1D;
import com.cern.colt.matrix.tlong.LongMatrix2D;

/**
 * Sparse hashed 2-d matrix holding <tt>long</tt> elements. First see the <a
 * href="package-summary.html">package summary</a> and javadoc <a
 * href="package-tree.html">tree view</a> to get the broad picture.
 * <p>
 * <b>Implementation:</b>
 * <p>
 * Note that this implementation is not synchronized. Uses a
 * {@link OpenLongLongHashMap}, which is a compact and
 * performant hashing technique.
 * <p>
 * <b>Memory requirements:</b>
 * <p>
 * Cells that
 * <ul>
 * <li>are never set to non-zero values do not use any memory.
 * <li>switch from zero to non-zero state do use memory.
 * <li>switch back from non-zero to zero state also do use memory. However,
 * their memory is automatically reclaimed from time to time. It can also
 * manually be reclaimed by calling {@link #trimToSize()}.
 * </ul>
 * <p>
 * worst case: <tt>memory [bytes] = (1/minLoadFactor) * nonZeros * 13</tt>. <br>
 * best case: <tt>memory [bytes] = (1/maxLoadFactor) * nonZeros * 13</tt>. <br>
 * Where <tt>nonZeros = cardinality()</tt> is the number of non-zero cells.
 * Thus, a 1000 x 1000 matrix with minLoadFactor=0.25 and maxLoadFactor=0.5 and
 * 1000000 non-zero cells consumes between 25 MB and 50 MB. The same 1000 x 1000
 * matrix with 1000 non-zero cells consumes between 25 and 50 KB.
 * <p>
 * <b>Time complexity:</b>
 * <p>
 * This class offers <i>expected</i> time complexity <tt>O(1)</tt> (i.e.
 * constant time) for the basic operations <tt>get</tt>, <tt>getQuick</tt>,
 * <tt>set</tt>, <tt>setQuick</tt> and <tt>size</tt> assuming the hash function
 * disperses the elements properly among the buckets. Otherwise, pathological
 * cases, although highly improbable, can occur, degrading performance to
 * <tt>O(N)</tt> in the worst case. As such this sparse class is expected to
 * have no worse time complexity than its dense counterpart
 * {@link DenseLongMatrix2D}. However, constant factors are considerably larger.
 * <p>
 * Cells are internally addressed in row-major. Performance sensitive
 * applications can exploit this fact. Setting values in a loop row-by-row is
 * quicker than column-by-column, because fewer hash collisions occur. Thus
 * 
 * <pre>
 * for (int row = 0; row &lt; rows; row++) {
 *     for (int column = 0; column &lt; columns; column++) {
 *         matrix.setQuick(row, column, someValue);
 *     }
 * }
 * </pre>
 * 
 * is quicker than
 * 
 * <pre>
 * for (int column = 0; column &lt; columns; column++) {
 *     for (int row = 0; row &lt; rows; row++) {
 *         matrix.setQuick(row, column, someValue);
 *     }
 * }
 * </pre>
 * 
 * @see com.cern.colt.map
 * @see OpenLongLongHashMap
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 */
public class SparseLongMatrix2D extends LongMatrix2D {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /*
     * The elements of the matrix.
     */
    protected AbstractLongLongMap elements;

    /**
     * Constructs a matrix with a copy of the given values. <tt>values</tt> is
     * required to have the form <tt>values[row][column]</tt> and have exactly
     * the same number of columns in every row.
     * <p>
     * The values are copied. So subsequent changes in <tt>values</tt> are not
     * reflected in the matrix, and vice-versa.
     * 
     * @param values
     *            The values to be filled into the new matrix.
     * @throws IllegalArgumentException
     *             if
     *             <tt>for any 1 &lt;= row &lt; values.length: values[row].length != values[row-1].length</tt>
     *             .
     */
    public SparseLongMatrix2D(long[][] values) {
        this(values.length, values.length == 0 ? 0 : values[0].length);
        assign(values);
    }

    /**
     * Constructs a matrix with a given number of rows and columns and default
     * memory usage. All entries are initially <tt>0</tt>.
     * 
     * @param rows
     *            the number of rows the matrix shall have.
     * @param columns
     *            the number of columns the matrix shall have.
     * @throws IllegalArgumentException
     *             if
     *             <tt>rows<0 || columns<0 || (double)columns*rows > Integer.MAX_VALUE</tt>
     *             .
     */
    public SparseLongMatrix2D(int rows, int columns) {
        this(rows, columns, rows * (columns / 1000), 0.2, 0.5);
    }

    /**
     * Constructs a matrix with a given number of rows and columns using memory
     * as specified. All entries are initially <tt>0</tt>. For details related
     * to memory usage see {@link OpenLongLongHashMap}.
     * 
     * @param rows
     *            the number of rows the matrix shall have.
     * @param columns
     *            the number of columns the matrix shall have.
     * @param initialCapacity
     *            the initial capacity of the hash map. If not known, set
     *            <tt>initialCapacity=0</tt> or small.
     * @param minLoadFactor
     *            the minimum load factor of the hash map.
     * @param maxLoadFactor
     *            the maximum load factor of the hash map.
     * @throws IllegalArgumentException
     *             if
     * 
     *             <tt>initialCapacity < 0 || (minLoadFactor < 0.0 || minLoadFactor >= 1.0) || (maxLoadFactor <= 0.0 || maxLoadFactor >= 1.0) || (minLoadFactor >= maxLoadFactor)</tt>
     *             .
     * @throws IllegalArgumentException
     *             if
     *             <tt>rows<0 || columns<0 || (double)columns*rows > Integer.MAX_VALUE</tt>
     *             .
     */
    public SparseLongMatrix2D(int rows, int columns, int initialCapacity, double minLoadFactor, double maxLoadFactor) {
        try {
            setUp(rows, columns);
        } catch (IllegalArgumentException exc) { // we can hold rows*columns>Integer.MAX_VALUE cells !
            if (!"matrix too large".equals(exc.getMessage()))
                throw exc;
        }
        this.elements = new OpenLongLongHashMap(initialCapacity, minLoadFactor, maxLoadFactor);
    }

    /**
     * Constructs a matrix with a copy of the given indexes and a single value.
     * 
     * @param rows
     *            the number of rows the matrix shall have.
     * @param columns
     *            the number of columns the matrix shall have.
     * @param rowIndexes
     *            row indexes
     * @param columnIndexes
     *            column indexes
     * @param value
     *            numerical value
     */
    public SparseLongMatrix2D(int rows, int columns, int[] rowIndexes, int[] columnIndexes, long value) {
        try {
            setUp(rows, columns);
        } catch (IllegalArgumentException exc) { // we can hold rows*columns>Integer.MAX_VALUE cells !
            if (!"matrix too large".equals(exc.getMessage()))
                throw exc;
        }
        this.elements = new OpenLongLongHashMap(rowIndexes.length);
        insert(rowIndexes, columnIndexes, value);
    }

    /**
     * Constructs a matrix with a copy of the given indexes and values.
     * 
     * @param rows
     *            the number of rows the matrix shall have.
     * @param columns
     *            the number of columns the matrix shall have.
     * @param rowIndexes
     *            row indexes
     * @param columnIndexes
     *            column indexes
     * @param values
     *            numerical values
     */
    public SparseLongMatrix2D(int rows, int columns, int[] rowIndexes, int[] columnIndexes, long[] values) {
        try {
            setUp(rows, columns);
        } catch (IllegalArgumentException exc) { // we can hold rows*columns>Integer.MAX_VALUE cells !
            if (!"matrix too large".equals(exc.getMessage()))
                throw exc;
        }
        this.elements = new OpenLongLongHashMap(rowIndexes.length);
        insert(rowIndexes, columnIndexes, values);
    }

    /**
     * Constructs a matrix from MatrixVectorReader.
     * 
     * @param reader
     *            matrix reader
     * @throws IOException
     */
    public SparseLongMatrix2D(MatrixVectorReader reader) throws IOException {
        MatrixInfo info;
        if (reader.hasInfo())
            info = reader.readMatrixInfo();
        else
            info = new MatrixInfo(true, MatrixInfo.MatrixField.Real, MatrixInfo.MatrixSymmetry.General);

        if (info.isPattern())
            throw new UnsupportedOperationException("Pattern matrices are not supported");
        if (info.isDense())
            throw new UnsupportedOperationException("Dense matrices are not supported");
        if (info.isComplex())
            throw new UnsupportedOperationException("Complex matrices are not supported");

        MatrixSize size = reader.readMatrixSize(info);
        try {
            setUp(size.numRows(), size.numColumns());
        } catch (IllegalArgumentException exc) { // we can hold rows*columns>Integer.MAX_VALUE cells !
            if (!"matrix too large".equals(exc.getMessage()))
                throw exc;
        }
        int numEntries = size.numEntries();
        int[] columnIndexes = new int[numEntries];
        int[] rowIndexes = new int[numEntries];
        long[] values = new long[numEntries];
        reader.readCoordinate(rowIndexes, columnIndexes, values);
        if (info.isSymmetric() || info.isSkewSymmetric()) {
            this.elements = new OpenLongLongHashMap(2 * rowIndexes.length);
        } else {
            this.elements = new OpenLongLongHashMap(rowIndexes.length);
        }
        insert(rowIndexes, columnIndexes, values);

        if (info.isSymmetric()) {
            for (int i = 0; i < numEntries; i++) {
                if (rowIndexes[i] != columnIndexes[i]) {
                    set(columnIndexes[i], rowIndexes[i], values[i]);
                }
            }
        } else if (info.isSkewSymmetric()) {
            for (int i = 0; i < numEntries; i++) {
                if (rowIndexes[i] != columnIndexes[i]) {
                    set(columnIndexes[i], rowIndexes[i], -values[i]);
                }
            }
        }
    }

    /**
     * Constructs a view with the given parameters.
     * 
     * @param rows
     *            the number of rows the matrix shall have.
     * @param columns
     *            the number of columns the matrix shall have.
     * @param elements
     *            the cells.
     * @param rowZero
     *            the position of the first element.
     * @param columnZero
     *            the position of the first element.
     * @param rowStride
     *            the number of elements between two rows, i.e.
     *            <tt>index(i+1,j)-index(i,j)</tt>.
     * @param columnStride
     *            the number of elements between two columns, i.e.
     *            <tt>index(i,j+1)-index(i,j)</tt>.
     * @throws IllegalArgumentException
     *             if
     *             <tt>rows<0 || columns<0 || (double)columns*rows > Integer.MAX_VALUE</tt>
     *             or flip's are illegal.
     */
    protected SparseLongMatrix2D(int rows, int columns, AbstractLongLongMap elements, int rowZero, int columnZero,
            int rowStride, int columnStride) {
        try {
            setUp(rows, columns, rowZero, columnZero, rowStride, columnStride);
        } catch (IllegalArgumentException exc) { // we can hold rows*columns>Integer.MAX_VALUE cells !
            if (!"matrix too large".equals(exc.getMessage()))
                throw exc;
        }
        this.elements = elements;
        this.isNoView = false;
    }

    public LongMatrix2D assign(com.cern.colt.function.tlong.LongFunction function) {
        if (this.isNoView && function instanceof com.cern.jet.math.tlong.LongMult) { // x[i] = mult*x[i]
            this.elements.assign(function);
        } else {
            super.assign(function);
        }
        return this;
    }

    public LongMatrix2D assign(long value) {
        // overriden for performance only
        if (this.isNoView && value == 0)
            this.elements.clear();
        else
            super.assign(value);
        return this;
    }

    public LongMatrix2D assign(LongMatrix2D source) {
        // overriden for performance only
        if (!(source instanceof SparseLongMatrix2D)) {
            return super.assign(source);
        }
        SparseLongMatrix2D other = (SparseLongMatrix2D) source;
        if (other == this)
            return this; // nothing to do
        checkShape(other);

        if (this.isNoView && other.isNoView) { // quickest
            this.elements.assign(other.elements);
            return this;
        }
        return super.assign(source);
    }

    public LongMatrix2D assign(final LongMatrix2D y, com.cern.colt.function.tlong.LongLongFunction function) {
        if (!this.isNoView)
            return super.assign(y, function);

        checkShape(y);

        if (function instanceof com.cern.jet.math.tlong.LongPlusMultSecond) { // x[i] = x[i] + alpha*y[i]
            final long alpha = ((com.cern.jet.math.tlong.LongPlusMultSecond) function).multiplicator;
            if (alpha == 0)
                return this; // nothing to do
            y.forEachNonZero(new com.cern.colt.function.tlong.IntIntLongFunction() {
                public long apply(int i, int j, long value) {
                    setQuick(i, j, getQuick(i, j) + alpha * value);
                    return value;
                }
            });
        } else if (function == com.cern.jet.math.tlong.LongFunctions.mult) { // x[i] = x[i] * y[i]
            this.elements.forEachPair(new com.cern.colt.function.tlong.LongLongProcedure() {
                public boolean apply(long key, long value) {
                    int i = (int) (key / columns);
                    int j = (int) (key % columns);
                    long r = value * y.getQuick(i, j);
                    if (r != value)
                        elements.put(key, r);
                    return true;
                }
            });
        } else if (function == com.cern.jet.math.tlong.LongFunctions.div) { // x[i] = x[i] / y[i]
            this.elements.forEachPair(new com.cern.colt.function.tlong.LongLongProcedure() {
                public boolean apply(long key, long value) {
                    int i = (int) (key / columns);
                    int j = (int) (key % columns);
                    long r = value / y.getQuick(i, j);
                    if (r != value)
                        elements.put(key, r);
                    return true;
                }
            });
        } else {
            super.assign(y, function);
        }
        return this;

    }

    /**
     * Assigns the result of a function to each cell;
     * <tt>x[row,col] = function(x[row,col],y[row,col])</tt>, where y is given
     * in the coordinate form with single numerical value.
     * 
     * @param rowIndexes
     *            row indexes of y
     * @param columnIndexes
     *            column indexes of y
     * @param value
     *            numerical value of y
     * @param function
     *            a function object taking as first argument the current cell's
     *            value of <tt>this</tt>, and as second argument the current
     *            cell's value of <tt>y</tt>,
     * @return <tt>this</tt> (for convenience only).
     */
    public SparseLongMatrix2D assign(final int[] rowIndexes, final int[] columnIndexes, final long value,
            final com.cern.colt.function.tlong.LongLongFunction function) {
        int size = rowIndexes.length;
        if (function == com.cern.jet.math.tlong.LongFunctions.plus) { // x[i] = x[i] + y[i]
            for (int i = 0; i < size; i++) {
                long row = rowIndexes[i];
                long column = columnIndexes[i];
                if (row >= rows || column >= columns) {
                    throw new IndexOutOfBoundsException("row: " + row + ", column: " + column);
                }
                long index = rowZero + row * rowStride + columnZero + column * columnStride;
                long elem = elements.get(index);
                long sum = elem + value;
                if (sum != 0) {
                    elements.put(index, sum);
                } else {
                    elements.removeKey(index);
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                long row = rowIndexes[i];
                long column = columnIndexes[i];
                if (row >= rows || column >= columns) {
                    throw new IndexOutOfBoundsException("row: " + row + ", column: " + column);
                }
                long index = rowZero + row * rowStride + columnZero + column * columnStride;
                long elem = elements.get(index);
                long result = function.apply(elem, value);
                if (result != 0) {
                    elements.put(index, result);
                } else {
                    elements.removeKey(index);
                }
            }
        }
        return this;
    }

    /**
     * Assigns the result of a function to each cell;
     * <tt>x[row,col] = function(x[row,col],y[row,col])</tt>, where y is given
     * in the coordinate form.
     * 
     * @param rowIndexes
     *            row indexes of y
     * @param columnIndexes
     *            column indexes of y
     * @param values
     *            numerical values of y
     * @param function
     *            a function object taking as first argument the current cell's
     *            value of <tt>this</tt>, and as second argument the current
     *            cell's value of <tt>y</tt>,
     * @return <tt>this</tt> (for convenience only).
     */
    public SparseLongMatrix2D assign(final int[] rowIndexes, final int[] columnIndexes, final long[] values,
            final com.cern.colt.function.tlong.LongLongFunction function) {
        int size = rowIndexes.length;
        if (function == com.cern.jet.math.tlong.LongFunctions.plus) { // x[i] = x[i] + y[i]
            for (int i = 0; i < size; i++) {
                long value = values[i];
                long row = rowIndexes[i];
                long column = columnIndexes[i];
                if (row >= rows || column >= columns) {
                    throw new IndexOutOfBoundsException("row: " + row + ", column: " + column);
                }
                long index = rowZero + row * rowStride + columnZero + column * columnStride;
                long elem = elements.get(index);
                value += elem;
                if (value != 0) {
                    elements.put(index, value);
                } else {
                    elements.removeKey(index);
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                long value = values[i];
                long row = rowIndexes[i];
                long column = columnIndexes[i];
                if (row >= rows || column >= columns) {
                    throw new IndexOutOfBoundsException("row: " + row + ", column: " + column);
                }
                long index = rowZero + row * rowStride + columnZero + column * columnStride;
                long elem = elements.get(index);
                value = function.apply(elem, value);
                if (value != 0) {
                    elements.put(index, value);
                } else {
                    elements.removeKey(index);
                }
            }
        }
        return this;
    }

    public int cardinality() {
        if (this.isNoView)
            return this.elements.size();
        else
            return super.cardinality();
    }

    /**
     * Returns a new matrix that has the same elements as this matrix, but is in
     * a column-compressed form. This method creates a new object (not a view),
     * so changes in the returned matrix are NOT reflected in this matrix.
     * 
     * @param sortRowIndexes
     *            if true, then row indexes in column compressed matrix are
     *            sorted
     * 
     * @return this matrix in a column-compressed form
     */
    public SparseCCLongMatrix2D getColumnCompressed(boolean sortRowIndexes) {
        int nnz = cardinality();
        long[] keys = elements.keys().elements();
        long[] values = elements.values().elements();
        int[] rowIndexes = new int[nnz];
        int[] columnIndexes = new int[nnz];

        for (int k = 0; k < nnz; k++) {
            long key = keys[k];
            rowIndexes[k] = (int) (key / columns);
            columnIndexes[k] = (int) (key % columns);
        }
        return new SparseCCLongMatrix2D(rows, columns, rowIndexes, columnIndexes, values, false, false, sortRowIndexes);
    }

    /**
     * Returns a new matrix that has the same elements as this matrix, but is in
     * a column-compressed modified form. This method creates a new object (not
     * a view), so changes in the returned matrix are NOT reflected in this
     * matrix.
     * 
     * @return this matrix in a column-compressed modified form
     */
    public SparseCCMLongMatrix2D getColumnCompressedModified() {
        SparseCCMLongMatrix2D A = new SparseCCMLongMatrix2D(rows, columns);
        int nnz = cardinality();
        long[] keys = elements.keys().elements();
        long[] values = elements.values().elements();
        for (int i = 0; i < nnz; i++) {
            int row = (int) (keys[i] / columns);
            int column = (int) (keys[i] % columns);
            A.setQuick(row, column, values[i]);
        }
        return A;
    }

    /**
     * Returns a new matrix that has the same elements as this matrix, but is in
     * a row-compressed form. This method creates a new object (not a view), so
     * changes in the returned matrix are NOT reflected in this matrix.
     * 
     * @param sortColumnIndexes
     *            if true, then column indexes in row compressed matrix are
     *            sorted
     * 
     * @return this matrix in a row-compressed form
     */
    public SparseRCLongMatrix2D getRowCompressed(boolean sortColumnIndexes) {
        int nnz = cardinality();
        long[] keys = elements.keys().elements();
        long[] values = elements.values().elements();
        final int[] rowIndexes = new int[nnz];
        final int[] columnIndexes = new int[nnz];
        for (int k = 0; k < nnz; k++) {
            long key = keys[k];
            rowIndexes[k] = (int) (key / columns);
            columnIndexes[k] = (int) (key % columns);
        }
        return new SparseRCLongMatrix2D(rows, columns, rowIndexes, columnIndexes, values, false, false,
                sortColumnIndexes);
    }

    /**
     * Returns a new matrix that has the same elements as this matrix, but is in
     * a row-compressed modified form. This method creates a new object (not a
     * view), so changes in the returned matrix are NOT reflected in this
     * matrix.
     * 
     * @return this matrix in a row-compressed modified form
     */
    public SparseRCMLongMatrix2D getRowCompressedModified() {
        SparseRCMLongMatrix2D A = new SparseRCMLongMatrix2D(rows, columns);
        int nnz = cardinality();
        long[] keys = elements.keys().elements();
        long[] values = elements.values().elements();
        for (int i = 0; i < nnz; i++) {
            int row = (int) (keys[i] / columns);
            int column = (int) (keys[i] % columns);
            A.setQuick(row, column, values[i]);
        }
        return A;
    }

    public AbstractLongLongMap elements() {
        return elements;
    }

    public void ensureCapacity(int minCapacity) {
        this.elements.ensureCapacity(minCapacity);
    }

    public LongMatrix2D forEachNonZero(final com.cern.colt.function.tlong.IntIntLongFunction function) {
        if (this.isNoView) {
            this.elements.forEachPair(new com.cern.colt.function.tlong.LongLongProcedure() {
                public boolean apply(long key, long value) {
                    int i = (int) (key / columns);
                    int j = (int) (key % columns);
                    long r = function.apply(i, j, value);
                    if (r != value)
                        elements.put(key, r);
                    return true;
                }
            });
        } else {
            super.forEachNonZero(function);
        }
        return this;
    }

    public synchronized long getQuick(int row, int column) {
        return this.elements.get((long) rowZero + (long) row * (long) rowStride + (long) columnZero + (long) column
                * (long) columnStride);
    }

    public long index(int row, int column) {
        return (long) rowZero + (long) row * (long) rowStride + (long) columnZero + (long) column * (long) columnStride;
    }

    public LongMatrix2D like(int rows, int columns) {
        return new SparseLongMatrix2D(rows, columns);
    }

    public LongMatrix1D like1D(int size) {
        return new SparseLongMatrix1D(size);
    }

    public synchronized void setQuick(int row, int column, long value) {
        long index = (long) rowZero + (long) row * (long) rowStride + (long) columnZero + (long) column
                * (long) columnStride;
        if (value == 0)
            this.elements.removeKey(index);
        else
            this.elements.put(index, value);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(rows).append(" x ").append(columns).append(" sparse matrix, nnz = ").append(cardinality())
                .append('\n');
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                long elem = getQuick(r, c);
                if (elem != 0) {
                    builder.append('(').append(r).append(',').append(c).append(')').append('\t').append(elem).append(
                            '\n');
                }
            }
        }
        return builder.toString();
    }

    public void trimToSize() {
        this.elements.trimToSize();
    }

    public LongMatrix1D vectorize() {
        SparseLongMatrix1D v = new SparseLongMatrix1D((int) size());
        int idx = 0;
        for (int c = 0; c < columns; c++) {
            for (int r = 0; r < rows; r++) {
                long elem = getQuick(r, c);
                v.setQuick(idx++, elem);
            }
        }
        return v;
    }

    public LongMatrix1D zMult(LongMatrix1D y, LongMatrix1D z, final long alpha, long beta, final boolean transposeA) {
        int rowsA = rows;
        int columnsA = columns;
        if (transposeA) {
            rowsA = columns;
            columnsA = rows;
        }

        boolean ignore = (z == null);
        if (z == null)
            z = new DenseLongMatrix1D(rowsA);

        if (!(this.isNoView && y instanceof DenseLongMatrix1D && z instanceof DenseLongMatrix1D)) {
            return super.zMult(y, z, alpha, beta, transposeA);
        }

        if (columnsA != y.size() || rowsA > z.size())
            throw new IllegalArgumentException("Incompatible args: "
                    + ((transposeA ? viewDice() : this).toStringShort()) + ", " + y.toStringShort() + ", "
                    + z.toStringShort());

        if (!ignore)
            z.assign(com.cern.jet.math.tlong.LongFunctions.mult(beta));

        DenseLongMatrix1D zz = (DenseLongMatrix1D) z;
        final long[] elementsZ = zz.elements;
        final int strideZ = zz.stride();
        final int zeroZ = (int) z.index(0);

        DenseLongMatrix1D yy = (DenseLongMatrix1D) y;
        final long[] elementsY = yy.elements;
        final int strideY = yy.stride();
        final int zeroY = (int) y.index(0);

        if (elementsY == null || elementsZ == null)
            throw new InternalError();

        this.elements.forEachPair(new com.cern.colt.function.tlong.LongLongProcedure() {
            public boolean apply(long key, long value) {
                int i = (int) (key / columns);
                int j = (int) (key % columns);
                if (transposeA) {
                    int tmp = i;
                    i = j;
                    j = tmp;
                }
                elementsZ[zeroZ + strideZ * i] += alpha * value * elementsY[zeroY + strideY * j];
                return true;
            }
        });

        return z;
    }

    public LongMatrix2D zMult(LongMatrix2D B, LongMatrix2D C, final long alpha, long beta, final boolean transposeA,
            boolean transposeB) {
        if (!(this.isNoView)) {
            return super.zMult(B, C, alpha, beta, transposeA, transposeB);
        }
        if (transposeB)
            B = B.viewDice();
        int rowsA = rows;
        int columnsA = columns;
        if (transposeA) {
            rowsA = columns;
            columnsA = rows;
        }
        int p = B.columns();
        boolean ignore = (C == null);
        if (C == null)
            C = new DenseLongMatrix2D(rowsA, p);

        if (B.rows() != columnsA)
            throw new IllegalArgumentException("Matrix2D inner dimensions must agree:" + toStringShort() + ", "
                    + (transposeB ? B.viewDice() : B).toStringShort());
        if (C.rows() != rowsA || C.columns() != p)
            throw new IllegalArgumentException("Incompatibel result matrix: " + toStringShort() + ", "
                    + (transposeB ? B.viewDice() : B).toStringShort() + ", " + C.toStringShort());
        if (this == C || B == C)
            throw new IllegalArgumentException("Matrices must not be identical");

        if (!ignore)
            C.assign(com.cern.jet.math.tlong.LongFunctions.mult(beta));

        // cache views
        final LongMatrix1D[] Brows = new LongMatrix1D[columnsA];
        for (int i = columnsA; --i >= 0;)
            Brows[i] = B.viewRow(i);
        final LongMatrix1D[] Crows = new LongMatrix1D[rowsA];
        for (int i = rowsA; --i >= 0;)
            Crows[i] = C.viewRow(i);

        final com.cern.jet.math.tlong.LongPlusMultSecond fun = com.cern.jet.math.tlong.LongPlusMultSecond.plusMult(0);

        this.elements.forEachPair(new com.cern.colt.function.tlong.LongLongProcedure() {
            public boolean apply(long key, long value) {
                int i = (int) (key / columns);
                int j = (int) (key % columns);
                fun.multiplicator = value * alpha;
                if (!transposeA)
                    Crows[i].assign(Brows[j], fun);
                else
                    Crows[j].assign(Brows[i], fun);
                return true;
            }
        });

        return C;
    }

    private void insert(int[] rowIndexes, int[] columnIndexes, long value) {
        int size = rowIndexes.length;
        for (int i = 0; i < size; i++) {
            long row = rowIndexes[i];
            long column = columnIndexes[i];
            if (row >= rows || column >= columns) {
                throw new IndexOutOfBoundsException("row: " + row + ", column: " + column);
            }
            if (value != 0) {
                long index = rowZero + row * rowStride + columnZero + column * columnStride;
                long elem = elements.get(index);
                if (elem == 0) {
                    elements.put(index, value);
                } else {
                    long sum = elem + value;
                    if (sum == 0) {
                        elements.removeKey(index);
                    } else {
                        elements.put(index, sum);
                    }
                }
            }
        }
    }

    private void insert(int[] rowIndexes, int[] columnIndexes, long[] values) {
        int size = rowIndexes.length;
        for (int i = 0; i < size; i++) {
            long value = values[i];
            long row = rowIndexes[i];
            long column = columnIndexes[i];
            if (row >= rows || column >= columns) {
                throw new IndexOutOfBoundsException("row: " + row + ", column: " + column);
            }
            if (value != 0) {
                long index = rowZero + row * rowStride + columnZero + column * columnStride;
                long elem = elements.get(index);
                if (elem == 0) {
                    elements.put(index, value);
                } else {
                    long sum = elem + value;
                    if (sum == 0) {
                        elements.removeKey(index);
                    } else {
                        elements.put(index, sum);
                    }
                }
            }
        }
    }

    protected boolean haveSharedCellsRaw(LongMatrix2D other) {
        if (other instanceof SelectedSparseLongMatrix2D) {
            SelectedSparseLongMatrix2D otherMatrix = (SelectedSparseLongMatrix2D) other;
            return this.elements == otherMatrix.elements;
        } else if (other instanceof SparseLongMatrix2D) {
            SparseLongMatrix2D otherMatrix = (SparseLongMatrix2D) other;
            return this.elements == otherMatrix.elements;
        }
        return false;
    }

    protected LongMatrix1D like1D(int size, int offset, int stride) {
        return new SparseLongMatrix1D(size, this.elements, offset, stride);
    }

    protected LongMatrix2D viewSelectionLike(int[] rowOffsets, int[] columnOffsets) {
        return new SelectedSparseLongMatrix2D(this.elements, rowOffsets, columnOffsets, 0);
    }

}
