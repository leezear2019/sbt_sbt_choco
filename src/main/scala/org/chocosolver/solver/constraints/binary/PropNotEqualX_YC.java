/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.binary;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.learn.ExplanationForSignedClause;
import org.chocosolver.solver.learn.Implications;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.ValueSortedMap;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableRangeSet;

/**
 * A specific <code>Propagator</code> extension defining filtering algorithm for:
 * <br/>
 * <b>X =/= Y + C</b>
 * <br>where <i>X</i> and <i>Y</i> are <code>Variable</code> objects and <i>C</i> a constant.
 * <br>
 * This <code>Propagator</code> defines the <code>propagate</code> and <code>awakeOnInst</code> methods. The other ones
 * throw <code>UnsupportedOperationException</code>.
 * <br/>
 * <br/>
 * <i>Based on Choco-2.1.1</i>
 *
 * @author Xavier Lorca
 * @author Charles Prud'homme
 * @author Arnaud Malapert
 * @version 0.01, june 2010
 * @since 0.01
 */
public class PropNotEqualX_YC extends Propagator<IntVar> {

    private IntVar x;
    private IntVar y;
    private int cste;

    @SuppressWarnings({"unchecked"})
    public PropNotEqualX_YC(IntVar[] vars, int c) {
        super(vars, PropagatorPriority.BINARY, false);
        this.x = vars[0];
        this.y = vars[1];
        this.cste = c;
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        //Principle : if v0 is instantiated and v1 is enumerated, then awakeOnInst(0) performs all needed pruning
        //Otherwise, we must check if we can remove the value from v1 when the bounds has changed.
        if (vars[vIdx].hasEnumeratedDomain()) {
            return IntEventType.instantiation();
        }
        return IntEventType.boundAndInst();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        // typical case: A=[1,4], B=[1,4] (bounded domains)
        // A instantiated to 3 => nothing can be done on B
        // then B dec supp to 3 => 3 can also be removed du to A = 3.
        // this is why propagation is not incremental
        if (x.isInstantiated()) {
            if (y.removeValue(x.getValue() - this.cste, this) || !y.contains(x.getValue() - cste)) {
                this.setPassive();
            }
        } else if (y.isInstantiated()) {
            if (x.removeValue(y.getValue() + this.cste, this) || !x.contains(y.getValue() + cste)) {
                this.setPassive();
            }
        } else if (x.getUB() < (y.getLB() + cste) || (y.getUB() + cste) < x.getLB()) {
            setPassive();
        }
    }

    @Override
    public ESat isEntailed() {
        if ((x.getUB() < y.getLB() + this.cste) ||
                (y.getUB() < x.getLB() - this.cste))
            return ESat.TRUE;
        else if (x.isInstantiated()
                && y.isInstantiated()
                && x.getValue() == y.getValue() + this.cste)
            return ESat.FALSE;
        else
            return ESat.UNDEFINED;
    }

    @Override
    public void explain(ExplanationForSignedClause explanation,
                        ValueSortedMap<IntVar> front,
                        Implications ig, int p) {
        boolean isPivot;
        int m;
        IntIterableRangeSet set0, set1;
        if (isPivot = (ig.getIntVarAt(p) == vars[0])) {
            assert explanation.getSet(vars[1]).size() == 1;
            m = explanation.getSet(vars[1]).min();
            set0 = explanation.getRootSet(vars[0]);
            set1 = explanation.getRootSet(vars[1]);
            set0.remove(m + cste);
            set1.remove(m);
        } else {
            assert explanation.getSet(vars[0]).size() == 1;
            m = explanation.getSet(vars[0]).min();
            set1 = explanation.getRootSet(vars[1]);
            set0 = explanation.getRootSet(vars[0]);
            set0.remove(m);
            set1.remove(m - cste);
        }
        explanation.addLiteral(vars[0], set0, isPivot);
        explanation.addLiteral(vars[1], set1, !isPivot);
    }

    @Override
    public String toString() {
        return "prop(" + vars[0].getName() + ".NEQ." + vars[1].getName() + "+" + cste + ")";
    }

}
