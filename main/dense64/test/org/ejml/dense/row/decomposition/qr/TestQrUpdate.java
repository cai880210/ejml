/*
 * Copyright (c) 2009-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.dense.row.decomposition.qr;

import org.ejml.data.DMatrixRow_F64;
import org.ejml.dense.row.CommonOps_R64;
import org.ejml.dense.row.MatrixFeatures_R64;
import org.ejml.dense.row.RandomMatrices_R64;
import org.ejml.dense.row.mult.SubmatrixOps_R64;
import org.ejml.interfaces.decomposition.QRDecomposition;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestQrUpdate {

    Random rand = new Random(0x345345);

    /**
     * Adds a row to a matrix at various points and updates the QR decomposition.
     * This is then checked by multiplying Q by R and seeing if the augmented A matrix
     * is the result
     */
    @Test
    public void testInsertRow() {
        int n = 3;

        for( int m = 3; m < 6; m++ ) {
            for( int insert = 0; insert < m; insert++ ) {
                checkInsert(m, n, insert);
            }
        }
    }

    @Test
    public void testRemoveRow() {
        int n = 3;

        for( int m = 4; m < 6; m++ ) {
            for( int remove = 0; remove < m; remove++ ) {
                checkRemove(m, n, remove);
            }
        }
    }

    private void checkRemove(int m, int n, int remove) {
        DMatrixRow_F64 A = RandomMatrices_R64.createRandom(m,n,rand);
        DMatrixRow_F64 Q = RandomMatrices_R64.createRandom(m,m,rand);
        DMatrixRow_F64 R = new DMatrixRow_F64(m,n);

        // compute what the A matrix would look like without the row
        DMatrixRow_F64 A_e = RandomMatrices_R64.createRandom(m-1,n,rand);
        SubmatrixOps_R64.setSubMatrix(A,A_e,0,0,0,0,remove,n);
        SubmatrixOps_R64.setSubMatrix(A,A_e,remove+1,0,remove,0,m-remove-1,n);

        QRDecomposition decomp = new QRDecompositionHouseholderColumn_R64();

        // now compute the results by removing elements from A
        decomp.decompose(A);
        Q.reshape(m,m, false);
        decomp.getQ(Q,false);
        decomp.getR(R,false);

        QrUpdate_R64 update = new QrUpdate_R64(m,n);

        update.deleteRow(Q,R,remove,true);

        assertTrue(MatrixFeatures_R64.isOrthogonal(update.getU_tran(),1e-6));

        DMatrixRow_F64 A_r = RandomMatrices_R64.createRandom(m-1,n,rand);
        CommonOps_R64.mult(Q,R,A_r);


        // see if the augmented A matrix is correct extracted from the adjusted Q and R matrices
        assertTrue(MatrixFeatures_R64.isIdentical(A_e,A_r,1e-6));
    }

    private void checkInsert(int m, int n, int insert) {
        DMatrixRow_F64 A = RandomMatrices_R64.createRandom(m,n,rand);
        DMatrixRow_F64 Q = RandomMatrices_R64.createRandom(m+1,m+1,rand);
        DMatrixRow_F64 R = new DMatrixRow_F64(m+1,n);

        // the row that is to be inserted
        double row[] = new double[]{1,2,3};

        // create the modified A
        DMatrixRow_F64 A_e = RandomMatrices_R64.createRandom(m+1,n,rand);
        SubmatrixOps_R64.setSubMatrix(A,A_e,0,0,0,0,insert,n);
        System.arraycopy(row, 0, A_e.data, insert * n, n);
        SubmatrixOps_R64.setSubMatrix(A,A_e,insert,0,insert+1,0,m-insert,n);

        QRDecomposition decomp = new QRDecompositionHouseholderColumn_R64();

        decomp.decompose(A);
        Q.reshape(m,m, false);
        decomp.getQ(Q,false);
        R.reshape(m,n,false);
        decomp.getR(R,false);

        DMatrixRow_F64 Qmod = createQMod(Q,insert);

        QrUpdate_R64 update = new QrUpdate_R64(m+1,n);

        update.addRow(Q,R,row,insert,true);

        DMatrixRow_F64 Z = new DMatrixRow_F64(m+1,m+1);
        CommonOps_R64.multTransB(Qmod,update.getU_tran(),Z);
        // see if the U matrix has the expected features
        assertTrue(MatrixFeatures_R64.isOrthogonal(Z,1e-6));

        // see if the process that updates Q from U is valid
        assertTrue(MatrixFeatures_R64.isIdentical(Q,Z,1e-6));

        DMatrixRow_F64 A_r = RandomMatrices_R64.createRandom(m+1,n,rand);
        CommonOps_R64.mult(Q,R,A_r);

        // see if the augmented A matrix is correct extracted from the adjusted Q and R matrices
        assertTrue(MatrixFeatures_R64.isIdentical(A_e,A_r,1e-6));
    }

    public static DMatrixRow_F64 createQMod(DMatrixRow_F64 Q , int insertRow ) {
        DMatrixRow_F64 Qmod = new DMatrixRow_F64(Q.numRows+1,Q.numCols+1);

        SubmatrixOps_R64.setSubMatrix(Q,Qmod,0,0,0,1,insertRow,Q.numCols);
        Qmod.set(insertRow,0,1);
        SubmatrixOps_R64.setSubMatrix(Q,Qmod,insertRow,0,insertRow+1,1,Q.numRows-insertRow,Q.numCols);

        return Qmod;
    }
}