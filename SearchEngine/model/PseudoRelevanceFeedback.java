package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import index.InvertedIndex;
import index.PostingNode;

public class PseudoRelevanceFeedback {
    private InvertedIndex invertedIndex;

    public PseudoRelevanceFeedback() {

    }

    public void setInvertedIndex(InvertedIndex index) {
        this.invertedIndex = index;
    }

    public Map<Integer, Double> PRFWithBIM(List<String> queryTerms, int topK, BIM bim) {
        Map<Integer, Double> initialRanking = bim.hitungRSV(queryTerms, 0, new HashMap<>());

        Set<Integer> topKDocuments = initialRanking.keySet().stream()
                .limit(topK)
                .collect(Collectors.toSet());

        int R = topKDocuments.size();
        Map<String, Integer> rtMap = new HashMap<>();
        for (String term : queryTerms) {
            int rt = 0;
            for (PostingNode node : invertedIndex.getPostingList(term)) {
                int docID = node.getDocID();
                if (topKDocuments.contains(docID)) rt++;
            }
            rtMap.put(term, rt);
        }

        Map<Integer, Double> finalRanking = bim.hitungRSV(queryTerms, R, rtMap);

        return finalRanking;
    }

    public Map<Integer, Double> PRFWithBM11(List<String> queryTerms, int topK, BM11 bm11) {
        Map<Integer, Double> initialRanking = bm11.calculateRSV(queryTerms, 0, new HashMap<>());

        Set<Integer> topKDocuments = initialRanking.keySet().stream()
                .limit(topK)
                .collect(Collectors.toSet());

        int R = topKDocuments.size();
        Map<String, Integer> rtMap = new HashMap<>();
        for (String term : queryTerms) {
            int rt = 0;
            for (PostingNode node : invertedIndex.getPostingList(term)) {
                int docID = node.getDocID();
                if (topKDocuments.contains(docID)) rt++;
            }
            rtMap.put(term, rt);
        }

        Map<Integer, Double> finalRanking = bm11.calculateRSV(queryTerms, R, rtMap);

        return finalRanking;
    }

    public Map<Integer, Double> PRFWithBM25(List<String> queryTerms, int topK, BM25 bm25) {
        Map<Integer, Double> initialRanking = bm25.calculateRSV(queryTerms, 0, new HashMap<>());

        Set<Integer> topKDocuments = initialRanking.keySet().stream()
                .limit(topK)
                .collect(Collectors.toSet());

        int R = topKDocuments.size();
        Map<String, Integer> rtMap = new HashMap<>();
        for (String term : queryTerms) {
            int rt = 0;
            for (PostingNode node : invertedIndex.getPostingList(term)) {
                int docID = node.getDocID();
                if (topKDocuments.contains(docID)) rt++;
            }
            rtMap.put(term, rt);
        }

        Map<Integer, Double> finalRanking = bm25.calculateRSV(queryTerms, R, rtMap);

        return finalRanking;
    }
}
