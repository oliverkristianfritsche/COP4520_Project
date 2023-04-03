import java.util.*;
import java.io.*;

public class Evaluate {
    static Random rng = new Random();

    public static void main(String[] args) throws IOException, InterruptedException {
        int[] n = {1, (int)(1e1), (int)(1e2), (int)(1e3), (int)(1e4), (int)(1e5), (int)(1e6), (int)(1e7)};
        nVersusTimeInsert(n);
        nVersusHeightInsert(n);
    }

    public static void nVersusTimeInsert(int[] n) throws IOException, InterruptedException {
        PrintWriter out = new PrintWriter(new FileWriter("evaluation/data/nVersusTime.txt"));
        ParallelTreap<Integer> t = new ParallelTreap<>();
        long[] time = new long[n.length];
        for (int i = 0; i < n.length; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < n[i]; j++) {
                t.insert(t.treapRoot, j);
            }
            long end = System.currentTimeMillis();
            time[i] = end - start;
        }

        for (int i = 0; i < n.length; i++) {
            out.println(n[i] + " " + time[i]);
        }
        out.close();
    }

    public static void nVersusHeightInsert(int[] n) throws IOException, InterruptedException {
        PrintWriter out = new PrintWriter(new FileWriter("evaluation/data/nVersusHeight.txt"));
        int[] height = new int[n.length];
        ParallelTreap<Integer> t = new ParallelTreap<>();
        for (int i = 0; i < n.length; i++) {
            for (int j = 0; j < n[i]; j++) {
                t.insert(t.treapRoot, j);
            }
            height[i] = t.height(t.treapRoot);
        }
        
        for (int i = 0; i < n.length; i++) {
            out.println(n[i] + " " + height[i]);
        }
        out.close();
    }

    
}