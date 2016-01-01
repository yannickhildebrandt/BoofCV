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

package boofcv.alg.geo.pose;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;
import org.ejml.equation.Equation;

import java.util.List;

/**
 * A mindless port of the original author's code with no thought for performance or understanding of what's
 * going on.
 *
 * @author Peter Abeles
 */
public class PnPDirectLeastSquaresNaive {

	FastQueue<Se3_F64> solutions = new FastQueue<Se3_F64>(Se3_F64.class,true);

	Equation eq = new Equation();

	public PnPDirectLeastSquaresNaive() {
		eq.process("macro leftMultVec(v) = [v' zeros(1,6); zeros(1,3) v' zeros(1,3); zeros(1,6) v']");
	}

	/**
	 * Compute all possible camera positions from the observations and known locations of points
	 *
	 * @param worldPts Known location of features in 3D world coordinates
	 * @param observed Observed location of features in normalized camera coordinates
	 */
	public void process(List<Point3D_F64> worldPts , List<Point2D_F64> observed )
	{
		if( worldPts.size() != observed.size() )
			throw new IllegalArgumentException("The two lists must have the same size");

		// setup internal data structures
		solutions.reset();

		int flag = 0;
		int N = observed.size();

		// build coeff matrix
		// An intermediate matrix, the inverse of what is called "H" in the paper
		// (see eq. 25)
		eq.process("H=zeros(3,3)");
		for (int i = 0; i < N; i++) {
			alias("z",observed.get(i));
			eq.process("H = H - eye(3) + z*z'");
		}

		eq.process("A=zeros(3,9)");
		for (int i = 0; i < N; i++) {
			alias("z",observed.get(i));
			alias("v",worldPts.get(i));
			// A = A + (z(:,i)*z(:,i)' - eye(3)) * LeftMultVec(p(:,i));
			eq.process("A = A + (z*z' - eye(3)) * leftMultVec(v)");
		}
		eq.process("A = H\\A");

		eq.process("D = zeros(9,9)");
		for (int i = 0; i < N; i++) {
			alias("z",observed.get(i));
			alias("v",worldPts.get(i));
			// D = D + (LeftMultVec(p(:,i)) + A)' * (eye(3) - z(:,i)*z(:,i)') * (LeftMultVec(p(:,i)) + A);
			eq.process("D = D + (leftMultVec(v) + A)'*(eye(3) - z*z')*(leftMultVec(v)+A)");
		}


	}


	void alias( String name , Point2D_F64 p ) {
		DenseMatrix64F m = new DenseMatrix64F(3,1);
		m.data[0] = p.x;
		m.data[1] = p.y;
		m.data[2] = 1;

		eq.alias(name,m);
	}

	void alias( String name , Point3D_F64 p ) {
		DenseMatrix64F m = new DenseMatrix64F(3,1);
		m.data[0] = p.x;
		m.data[1] = p.y;
		m.data[2] = p.z;

		eq.alias(name,m);
	}

	public List<Se3_F64> getWorldToCamera() {
		return solutions.toList();
	}
}
