/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.alldifferent.algo;

import amtf.Measurer;
import gnu.trove.map.hash.TIntIntHashMap;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.graphOperations.connectivity.StrongConnectivityNewFinder;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISetIterator;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Algorithm of Alldifferent with AC
 * <p>
 * Uses Zhang algorithm in the paper of IJCAI-18
 * "A Fast Algorithm for Generalized Arc Consistency of the Alldifferent Constraint"
 *
 * @author Jean-Guillaume Fages, Jia'nan Chen
 */
public class AlgoAllDiffACFast {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************
    // 约束的个数
    static public int num = 0;
    // 约束的编号
    private int id;

    private int n, n2;
    private IntVar[] vars;
    private ICause aCause;
    private TIntIntHashMap map;
    private DirectedGraph digraph;
    private int[] matching;
    private BitSet free;
    // distinction为区分集，长度为n2
    // 变量部分（前n位），1b对应的变量属于Γ(A)，0b对应的变量属于Xc-Γ(A)
    // 值部分（后n2-n位），1b对应的值属于A，0b对应的值属于Dc-A
    private BitSet distinction;
    private int[] nodeSCC;
    private StrongConnectivityNewFinder SCCfinder;
    // for augmenting matching (BFS)
    private int[] father;
    private int[] fifo;
    private BitSet in;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public AlgoAllDiffACFast(IntVar[] variables, ICause cause) {
        id = num++;

        this.vars = variables;
        aCause = cause;
        n = vars.length;
        // 存储匹配
        matching = new int[n];
        Arrays.fill(matching, -1);
        map = new TIntIntHashMap();
        IntVar v;
        int ub;
        int idx = n;
        // 统计所有变量论域中不同值的个数
        for (int i = 0; i < n; i++) {
            v = vars[i];
            ub = v.getUB();
            for (int j = v.getLB(); j <= ub; j = v.nextValue(j)) {
                if (!map.containsKey(j)) {
                    map.put(j, idx);
                    idx++;
                }
            }
        }
        n2 = idx;
        // 用Bitset邻接矩阵的有向图，因为没有辅助点，所以是n2，非n2 + 1
        digraph = new DirectedGraph(n2, SetType.BITSET, false);
        // free应该区分匹配点和非匹配点（true表示非匹配点，false表示匹配点）
        free = new BitSet(n2);
        distinction = new BitSet(n2);
        SCCfinder = new StrongConnectivityNewFinder(digraph);
        // 用于回溯增广路径
        father = new int[n2];
        // 使用队列实现非递归广度优先搜索
        fifo = new int[n2];
        // 哪些点在fifo队列中（true表示在，false表示不在）
        in = new BitSet(n2);
    }

    //***********************************************************************************
    // PROPAGATION
    //***********************************************************************************

    public boolean propagate() throws ContradictionException {
//        out.println("vars: ");
//        for (IntVar v : vars) {
//            System.out.println(v.toString());
//        }
//        System.out.println("----------------" + id + " propagate----------------");
        long startTime = System.nanoTime();
        findMaximumMatching();
        Measurer.matchingTime += System.nanoTime() - startTime;

        startTime = System.nanoTime();
        boolean filter = filter();
        Measurer.filterTime += System.nanoTime() - startTime;
        return filter;
    }

    //***********************************************************************************
    // Initialization
    //***********************************************************************************

    private void findMaximumMatching() throws ContradictionException {
        // 每次都重新建图
        for (int i = 0; i < n2; i++) {
            digraph.getSuccOf(i).clear();
            digraph.getPredOf(i).clear();
        }
        free.set(0, n2);
        int k, ub;
        IntVar v;
        for (int i = 0; i < n; i++) {
            v = vars[i];
            ub = v.getUB();
            int mate = matching[i];
            for (k = v.getLB(); k <= ub; k = v.nextValue(k)) {
                int j = map.get(k);
                // 利用之前已经找到的匹配
                if (mate == j) {
                    assert free.get(i) && free.get(j);
                    digraph.addArc(j, i);
                    free.clear(i);
                    free.clear(j);
                } else {
                    digraph.addArc(i, j);
                }
            }
        }
        // 尝试为每个变量都寻找一个匹配，即最大匹配的个数要与变量个数相等，否则回溯
        // 利用匈牙利算法寻找最大匹配
        for (int i = free.nextSetBit(0); i >= 0 && i < n; i = free.nextSetBit(i + 1)) {
            tryToMatch(i);
        }
        // 匹配边是由值指向变量，非匹配边是由变量指向值
        for (int i = 0; i < n; i++) {
            matching[i] = digraph.getPredOf(i).isEmpty() ? -1 : digraph.getPredOf(i).iterator().next();
        }
    }

    private void tryToMatch(int i) throws ContradictionException {
        int mate = augmentPath_BFS(i);
        if (mate != -1) {// 值mate是一个自由点
            free.clear(mate);
            free.clear(i);
            int tmp = mate;
            // 沿着father回溯即是增广路径
            while (tmp != i) {
                // 翻转边的方向
                digraph.removeArc(father[tmp], tmp);
                digraph.addArc(tmp, father[tmp]);
                // 回溯
                tmp = father[tmp];
            }
        } else {//应该是匹配失败，即最大匹配个数与变量个数不相等，需要回溯
            vars[0].instantiateTo(vars[0].getLB() - 1, aCause);
        }
    }

    // 宽度优先搜索寻找增广路径
    private int augmentPath_BFS(int root) {
        // root是一个自由点（变量）。
        // 如果与root相连的值中有自由点，就返回第一个自由点；
        // 如果没有，尝试为匹配变量找一个新的自由点，过程中通过father标记增广路径。
        in.clear();
        int indexFirst = 0, indexLast = 0;
        fifo[indexLast++] = root;
        int x;
        ISetIterator succs;
        while (indexFirst != indexLast) {
            x = fifo[indexFirst++];
            // 如果x是一个变量，那么它的后继就是非匹配的值；
            // 如果x是一个值，那么它的后继只有一个，是与它匹配的变量。
            succs = digraph.getSuccOf(x).iterator();
            while (succs.hasNext()) {
                int y = succs.nextInt();
                if (!in.get(y)) {
                    father[y] = x;
                    fifo[indexLast++] = y;
                    in.set(y);
                    if (free.get(y)) { //自由点（值）
                        return y;
                    }
                }
            }
        }
        return -1;
    }

    //***********************************************************************************
    // PRUNING
    //***********************************************************************************

    //  新函数从自由点出发，区分论文中的四个集合
    private void distinguish() {
        distinction.clear();
        int indexFirst = 0, indexLast = 0;
        // 广度优先搜索，寻找从自由值出发的所有交替路
        ISetIterator predece;
        for (int i = free.nextSetBit(n); i >= n && i < n2; i = free.nextSetBit(i + 1)) {
            // 首先把与自由值相连的变量入队列
            distinction.set(i);
            predece = digraph.getPredOf(i).iterator();
            while (predece.hasNext()) {
                int x = predece.nextInt();
                if (!distinction.get(x)) {
                    fifo[indexLast++] = x;
                    distinction.set(x);
                }
            }
            // 然后，对队列中每个变量的匹配值，把与该值相连的非匹配变量入队
            while (indexFirst != indexLast) {
                int y = fifo[indexFirst++];
                int v = matching[y];
                distinction.set(v);
                predece = digraph.getPredOf(v).iterator();
                while (predece.hasNext()) {
                    int x = predece.nextInt();
                    if (!distinction.get(x)) {
                        fifo[indexLast++] = x;
                        distinction.set(x);
                    }
                }
            }
        }
    }

    private void buildSCC() {
        // 调用重载函数
        SCCfinder.findAllSCC(distinction);
        nodeSCC = SCCfinder.getNodesSCC();
    }

    private boolean filter() throws ContradictionException {
        boolean filter = false;
        // 调用区分函数
        distinguish();
        buildSCC();
        int j, ub;
        IntVar v;
        // 根据变量和取值的所在集合来确定删除方式
        for (int i = 0; i < n; i++) {
            v = vars[i];
            if (!v.isInstantiated()) {
                ub = v.getUB();
                for (int k = v.getLB(); k <= ub; k = v.nextValue(k)) {
                    j = map.get(k);
                    if (distinction.get(i) && !distinction.get(j)) { // 删除第一类边，变量在Γ(A)中，值在Dc-A中
                        ++Measurer.numDelValuesP1;
                        filter |= v.removeValue(k, aCause);
//                        System.out.println("first delete: " + v.getName() + ", " + k);
//                    digraph.removeArc(i, j);
                    } else if (!distinction.get(i) && !distinction.get(j)) { // 删除第二类边，变量在Xc-Γ(A)中，值在Dc-A中
                        if (nodeSCC[i] != nodeSCC[j]) {
                            if (matching[i] == j) {
                                int valNum = v.getDomainSize();
                                filter |= v.instantiateTo(k, aCause);
                                Measurer.numDelValuesP2 += valNum - 1;
//                                System.out.println("instantiate  : " + v.getName() + ", " + k);
                            } else {
                                ++Measurer.numDelValuesP2;
                                filter |= v.removeValue(k, aCause);
//                                System.out.println("second delete: " + v.getName() + ", " + k);
                                // 我觉得不用更新digraph，因为每次调用propagate时都会更新digraph
//                            digraph.removeArc(i, j);
                            }
                        }
                    }
                }
            }
        }
//        for (int i = 0; i < n; i++) {
//            v = vars[i];
//            if (!v.hasEnumeratedDomain()) {
//                ub = v.getUB();
//                for (int k = v.getLB(); k <= ub; k++) {
//                    j = map.get(k);
//                    if (!(digraph.arcExists(i, j) || digraph.arcExists(j, i))) {
//                        filter |= v.removeValue(k, aCause);
//                    }
//                }
//                int lb = v.getLB();
//                for (int k = v.getUB(); k >= lb; k--) {
//                    j = map.get(k);
//                    if (!(digraph.arcExists(i, j) || digraph.arcExists(j, i))) {
//                        filter |= v.removeValue(k, aCause);
//                    }
//                }
//            }
//        }
//        out.println("after vars: ");
//        for (IntVar x : vars) {
//            System.out.println(x.toString());
//        }
        return filter;
    }
}
