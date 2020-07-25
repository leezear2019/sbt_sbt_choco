package amtf;

import amtf.parser.XCSPParser;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.mis.F;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static java.lang.System.out;
import static org.chocosolver.solver.search.strategy.Search.activityBasedSearch;

public class expAllDiff {


    public static void main(String[] args) {

//        String inputFolder = "G:\\X3Benchmarks\\alldiff\\";
//        String outputFolder = "G:\\X3Benchmarks\\alldiff-result\\";
//        String[] series = new String[]{
//                "Langford-m1-k2",
//                "Langford-m1-k3",
//                "Langford-m1-k4",
////                "Queens-m1-s1",
//                "ColouredQueens-m1-s1",
////                "SchurrLemma-mod-s1",
////                "LatinSquare-m1-gp",
////                "LatinSquare-m1-gs",
////                "LatinSquare-xcsp2-bqwh15-106",
////                "LatinSquare-xcsp2-bqwh18-141",
//        };
        assert args.length == 1;
        Bench_File File_Benchmark = new Bench_File(args[0]);
//        File_Benchmark.Print();
        String inputFolder = File_Benchmark.path_in;
        String outputFolder = File_Benchmark.path_out;
        ArrayList<String> series = File_Benchmark.all;
        XCSPParser parser = new XCSPParser();
        String[] algorithms = new String[]{
                "AC",
                "AC2",
                "ACFast",
                "ACFast2",
                "ACNaive",
                "BC"
        };
        int runNum = 1;
        long node = 0;
        float time, matchingTime, filterTime, numDelValuesP1, numDelValuesP2;
        float IN_SEC = 1000 * 1000 * 1000f;

        for (String s : series) {
            try {
                File csv = new File(outputFolder + s + ".csv");
                BufferedWriter bw = new BufferedWriter(new FileWriter(csv, false));
                bw.write("instance");
                for (int i = 0; i < algorithms.length; i++) {
                    bw.write(",algorithm,node,time,matchingTime,filterTime,numDelValuesP1,numDelValuesP2");
//                    bw.write(",node,time");
                }
                bw.newLine();
                // 读取实例集s下的所有实例文件名
                File[] instances = new File(inputFolder + s).listFiles();
                for (File ins : instances) {
                    out.println(ins.getName());
                    bw.write(ins.getName());
                    for (String algorithm : algorithms) {
                        time = 0f;
                        matchingTime = 0f;
                        filterTime = 0f;
                        numDelValuesP1 = 0f;
                        numDelValuesP2 = 0f;
                        out.println(algorithm + "======>");
                        for (int i = 0; i < runNum; i++) {
                            Measurer.initial();
                            Model model = new Model();
                            try {
                                parser.model(model, ins.getPath(), algorithm);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            IntVar[] decVars = (IntVar[]) model.getHook("decisions");

                            if (decVars == null) {
                                decVars = parser.mvars.values().toArray(new IntVar[parser.mvars.size()]);
                            }
                            Arrays.sort(decVars, Comparator.comparingInt(IntVar::getId));
                            Solver solver = model.getSolver();
                            solver.limitTime("900s");
//                            solver.setSearch(activityBasedSearch(decVars));
                            solver.setSearch(Search.defaultSearch(model));
                            solver.solve();
                            // if (solver.solve()) {
                            // out.printf("solution: ");
                            // for (IntVar v : decVars) {
                            //     out.printf("%d ", v.getValue());
                            // }
                            // out.println();
                            // }
                            node = solver.getNodeCount();
                            time += solver.getTimeCount() / runNum;
                            matchingTime += Measurer.matchingTime / IN_SEC / runNum;
                            filterTime += Measurer.filterTime / IN_SEC / runNum;
                            numDelValuesP1 += Measurer.numDelValuesP1 / runNum;
                            numDelValuesP2 += Measurer.numDelValuesP2 / runNum;
                        }
                        bw.write("," + algorithm + "," + node + "," + time + "," + matchingTime + "," + filterTime + "," + numDelValuesP1 + "," + numDelValuesP2);
//                        bw.write("," + node + "," + time);
                        bw.flush();
                    }
                    bw.newLine();
                }
                bw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
