/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
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

package boofcv.alg.geo;

import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;

/**
 * Performs math on points in homogenous coordinates
 * @author Peter Abeles
 */
public class HomogenousMath2D_F64 {

	/**
	 * A = A + diag([x,y,z])
	 *
	 * @param A 3x3 matrix
	 * @param x scalar
	 * @param y scalar
	 * @double z scalr
	 */
	public static void addDiag(DenseMatrix64F A , double x , double y , double z ) {
		A.data[0] += x;
		A.data[4] += y;
		A.data[8] += z;
	}

	/**
	 * A = A + &alpha;*p*p<sup>T</sup>
	 *
	 * @param A 3x3 matrix
	 * @param p 2D point in homogenous coordinates
	 */
	public static void addOuter(DenseMatrix64F A , double alpha , Point2D_F64 p) {
		A.data[0] += alpha*p.x*p.x;
		A.data[1] += alpha*p.x*p.y;
		A.data[2] += alpha*p.x;
		A.data[3] += alpha*p.y*p.x;
		A.data[4] += alpha*p.y*p.y;
		A.data[5] += alpha*p.y;
		A.data[6] += alpha*p.x;
		A.data[7] += alpha*p.y;
		A.data[8] += alpha;
	}
}
