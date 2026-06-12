package model;

import java.util.*;
import index.InvertedIndex;
import index.PostingNode;

public class BIM {

    private InvertedIndex invertedIndex;

    public void setInvertedIndex(InvertedIndex index) {
        this.invertedIndex = index;
    }

    // Estimasi pt dan ut, lalu hitung wt
    // Kalau R=0 → Skenario 1 (tanpa relevance judgements)
    // Kalau R>0 → Skenario 2 (dengan relevance judgements / PRF)
    public double hitungWt(int Nt, int N, int rt, int R) {
        double pt = (R == 0) ? 0.5 : (rt + 0.5) / (R + 1);
        double ut = (R == 0) ? (double) Nt / N : (Nt - rt + 0.5) / (N - R + 1);
        return Math.log10(pt / ut);
    }

    // Hitung RSV untuk semua dokumen
    public Map<Integer, Double> hitungRSV(List<String> queryTerms, int R, Map<String, Integer> rtMap) {
        int N = invertedIndex.getTotalDocuments();
        Map<Integer, Double> score = new HashMap<>();

        for (String term : queryTerms) {
            int Nt = invertedIndex.getDfByTerm(term);
            if (Nt == 0)
                continue;

            int rt = rtMap.getOrDefault(term, 0);
            double wt = hitungWt(Nt, N, rt, R);

            for (PostingNode node : invertedIndex.getPostingList(term)) {
                int docID = node.getDocID();
                score.put(docID, score.getOrDefault(docID, 0.0) + wt);
            }
        }

        return sortDescending(score);
    }

    // Utility sorting
    public Map<Integer, Double> sortDescending(Map<Integer, Double> score) {
        List<Map.Entry<Integer, Double>> entries = new ArrayList<>(score.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        Map<Integer, Double> hasil = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> e : entries) {
            hasil.put(e.getKey(), e.getValue());
        }
        return hasil;
    }
}