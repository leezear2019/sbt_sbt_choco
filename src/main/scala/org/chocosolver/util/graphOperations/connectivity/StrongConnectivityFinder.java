/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.util.graphOperations.connectivity;

import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;

import java.util.BitSet;
import java.util.Iterator;

public class StrongConnectivityFinder  {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	// input
	private DirectedGraph graph;
	private BitSet restriction;
	private int n;
	// output
	private int[] sccFirstNode, nextNode, nodeSCC;
	private int nbSCC;

	// util
	private int[] stack, p, inf, nodeOfDfsNum, dfsNumOfNode;
	private Iterator<Integer>[] iterator;
	private BitSet inStack;

	//***********************************************************************************
	// CONSTRUCTOR
	//***********************************************************************************

	public StrongConnectivityFinder(DirectedGraph graph) {
		this.graph = graph;
		this.n = graph.getNbMaxNodes();
		//
		stack = new int[n];
		p = new int[n];
		inf = new int[n];
		nodeOfDfsNum = new int[n];
		dfsNumOfNode = new int[n];
		inStack = new BitSet(n);
		restriction = new BitSet(n);
		// SCC的第一个点
		sccFirstNode = new int[n];
		nextNode = new int[n];
		// node属于哪一个SCC
		nodeSCC = new int[n];
		// nbSCC是强连通分量的个数
		nbSCC = 0;
		//noinspection unchecked
		iterator = new Iterator[n];
	}

	//***********************************************************************************
	// ALGORITHM
	//***********************************************************************************

	public void findAllSCC() {
		ISet nodes = graph.getNodes();
        // 目前猜想restriction应该是标记哪些点还需要寻找强联通分量
		// 根据我的观察，每次寻找强联通分量结束之后，restriction就已经是全0了
        for (int i = 0; i < n; i++) {
			restriction.set(i, nodes.contains(i));
		}
//		System.out.println(restriction.toString());
		findAllSCCOf(restriction);
	}

	public void findAllSCCOf(BitSet restriction) {
		inStack.clear();
		for (int i = 0; i < n; i++) {
			dfsNumOfNode[i] = 0;
			// inf初始化为n+2是为了下面
			inf[i] = n + 2;
			nextNode[i] = -1;
			sccFirstNode[i] = -1;
			nodeSCC[i] = -1;
		}
		nbSCC = 0;
		// 先寻找只包含单个点的强连通分量
		findSingletons(restriction);
		int first = restriction.nextSetBit(0);
		while (first >= 0) {
			findSCC(first, restriction, stack, p, inf, nodeOfDfsNum, dfsNumOfNode, inStack);
			first = restriction.nextSetBit(first);
		}
	}

	private void findSingletons(BitSet restriction) {
		ISet nodes = graph.getNodes();
		// 找不到下一个为1b的bit时，nextSetBit返回-1
		for (int i = restriction.nextSetBit(0); i >= 0; i = restriction.nextSetBit(i + 1)) {
		    // 按照我的想法，前面findAllSCC()中已经把restriction全部置为0，所以这里就不用再判断nodes.contain了
			if (nodes.contains(i) && graph.getPredOf(i).size() * graph.getSuccOf(i).size() == 0) {
				nodeSCC[i] = nbSCC;
				sccFirstNode[nbSCC++] = i;
				restriction.clear(i);
			}
		}
	}

	// 非递归版tarjan算法
    // dfsNumOfNode(博客算法里面的Dfn)指每个点的深度优先搜索序号
    // nodeOfDfsNum指每个深度优先搜索序号对应的点
    // inf（博客算法里面的Low）指每个点所在强联通分量对应的深搜子树的根节点的深搜序号
	// p指深搜序号的前驱（父亲）序号
	private void findSCC(int start, BitSet restriction, int[] stack, int[] p, int[] inf, int[] nodeOfDfsNum, int[] dfsNumOfNode, BitSet inStack) {
		int nb = restriction.cardinality();
		// trivial case
		if (nb == 1) {
			nodeSCC[start] = nbSCC;
			sccFirstNode[nbSCC++] = start;
			restriction.clear(start);
			return;
		}
		//initialization
		int stackIdx = 0;
		// i和k（博客算法里面的index）指深度优先搜索的序号
		int k = 0;
		int i = k;
		dfsNumOfNode[start] = k;
		nodeOfDfsNum[k] = start;
		// 栈里面存的是深搜序号（博客的栈里面存的是点）
		stack[stackIdx++] = i;
		inStack.set(i);
		p[k] = k;
		iterator[k] = graph.getSuccOf(start).iterator();
		// j指点
		int j;
		// algo
		while (true) {
			if (iterator[i].hasNext()) {
				j = iterator[i].next();
				if (restriction.get(j)) {
					if (dfsNumOfNode[j] == 0 && j != start) { // 点j没有被访问过
						k++;
						nodeOfDfsNum[k] = j;
						dfsNumOfNode[j] = k;
						p[k] = i;
						i = k;
						// 非递归版，所以要转到j的后继点
						iterator[i] = graph.getSuccOf(j).iterator();
						stack[stackIdx++] = i;
						inStack.set(i);
						inf[i] = i;
					} else if (inStack.get(dfsNumOfNode[j])) {// 点j被访问过且还在栈中
						inf[i] = Math.min(inf[i], dfsNumOfNode[j]);
					}
				}
			} else {
				if (i == 0) {
					break;
				}
				// 因为inf被初始化为n+2，所以需要>=（博客上是==）。
				// 每个点的dfs和深搜序号i是相等的(博客上是判断dfn == low)
				if (inf[i] >= i) {
					int y, z;
					do {
						z = stack[--stackIdx];
						inStack.clear(z);
						y = nodeOfDfsNum[z];
						restriction.clear(y);
						sccAdd(y);
					} while (z != i);
					nbSCC++;
				}
				// p[i]起回溯作用（对应递归回到上一层）
				inf[p[i]] = Math.min(inf[p[i]], inf[i]);
				i = p[i];
			}
		}
		if (inStack.cardinality() > 0) {
			int y;
			do {
				y = nodeOfDfsNum[stack[--stackIdx]];
				restriction.clear(y);
				sccAdd(y);
			} while (y != start);
			nbSCC++;
		}
	}

	private void sccAdd(int y) {
		nodeSCC[y] = nbSCC;
		// 向后挪，并更新第一个点
		nextNode[y] = sccFirstNode[nbSCC];
		sccFirstNode[nbSCC] = y;
	}

	//***********************************************************************************
	// ACCESSORS
	//***********************************************************************************

	public int getNbSCC() {
		return nbSCC;
	}

	public int[] getNodesSCC() {
		return nodeSCC;
	}

	public int getSCCFirstNode(int i) {
		return sccFirstNode[i];
	}

	public int getNextNode(int j) {
		return nextNode[j];
	}

}
