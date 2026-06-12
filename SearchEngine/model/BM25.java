package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import index.*;

public class BM25 {
    private BIM bim;
    private double k;
    private double b;
    private InvertedIndex invertedIndex;

    public BM25(double k, double b){
        this.bim = new BIM();
        this.k = k;
        this.b = b;
    }

    public void setInvertedIndex(InvertedIndex index) {
        this.invertedIndex = index;
    }

    public Map<Integer, Double> calculateRSV(List<String> queryTerms, int R, Map<String, Integer> rtMap) {
        int N = invertedIndex.getTotalDocuments();
        Map<Integer, Double> score = new HashMap<>();

        for (String term : queryTerms) {
            int Nt = invertedIndex.getDfByTerm(term);
            if (Nt == 0) continue;
            int rt = rtMap.getOrDefault(term, 0);

            double wt = bim.hitungWt(Nt, N, rt, R);
            
            for (PostingNode node : invertedIndex.getPostingList(term)) {
                int docID = node.getDocID();
                int ftd = node.getTermFrequency();
                int ld = invertedIndex.getDocumentLength(docID);
                double lavg = invertedIndex.getAverageDocumentLength();

                double rsv = (ftd * (k+1)* wt) / (ftd + k * b * ld/lavg + k*(1-b)); 

                score.put(docID, score.getOrDefault(docID, 0.0) + rsv);
            }
        }

        return sortDescending(score);
    }

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
