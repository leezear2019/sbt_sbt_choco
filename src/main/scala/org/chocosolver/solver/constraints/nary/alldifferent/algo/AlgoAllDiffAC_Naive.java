package org.chocosolver.solver.constraints.nary.alldifferent.algo;

import amtf.Measurer;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.hash.TIntIntHashMap;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.NaiveBitSet;
import org.chocosolver.util.objects.SparseSet;

import java.util.Arrays;

/**
 * Algorithm of Alldifferent with AC
 * <p>
 * Uses Zhang algorithm in the paper of IJCAI-18
 * "A Fast Algorithm for Generalized Arc Consistency of the Alldifferent Constraint"
 * <p>
 * We try to use the bit to speed up.
 * <p>
 * <p>
 *
 * @author Jean-Guillaume Fages, Zhe Li, Jia'nan Chen
 */
public abstract class AlgoAllDiffAC_Naive {


    public AlgoAllDiffAC_Naive(IntVar[] variables, ICause cause) {
    }

    public abstract boolean propagate() throws ContradictionException;


}