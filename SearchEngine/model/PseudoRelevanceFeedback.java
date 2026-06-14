package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import index.InvertedIndex;
import index.PostingNode;

/**
 * Kelas PseudoRelevanceFeedback mengimplementasikan teknik umpan balik relevansi semu

 * Teknik ini mengasumsikan bahwa sejumlah 'K' dokumen teratas (Top-K) yang dikembalikan 
 * dari pencarian awal (initial retrieval) adalah dokumen yang relevan. 
 * 
 * Informasi dari dokumen Top-K tersebut kemudian digunakan untuk memperbarui parameter probabilitas,
 * sehingga dapat melakukan pencarian kedua (final retrieval) yang diharapkan memberikan 
 * hasil yang lebih akurat.
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM
 * https://stackoverflow.com/questions/42878542/how-to-fetch-first-10-key-value-hashmap-in-java 
 * 
 * @author Keane 
 * 
 */
public class PseudoRelevanceFeedback {
    
    /**
     * Referensi ke struktur data Inverted Index yang menyimpan koleksi dokumen 
     * beserta metrik frekuensinya.
     */
    private InvertedIndex invertedIndex;

    /**
     * Konstruktor default
     */
    public PseudoRelevanceFeedback() {
    }

    /**
     * Menetapkan objek Inverted Index yang akan digunakan dalam proses pencarian
     * dan ekstraksi dokumen Top-K.
     *
     * @param index Objek InvertedIndex yang sudah terisi
     */
    public void setInvertedIndex(InvertedIndex index) {
        this.invertedIndex = index;
    }

    /**
     * Melakukan proses Pseudo Relevance Feedback menggunakan Binary Independence Model (BIM).
     *
     * @param queryTerms Daftar kata (term) dari kueri pencarian awal.
     * @param topK Jumlah dokumen teratas dari pencarian pertama yang akan diasumsikan relevan.
     * @param bim Instansiasi dari model pemeringkatan BIM.
     * 
     * @return Map berisi ID dokumen dan skor akhirnya (RSV), yang sudah dihitung ulang 
     * berdasarkan informasi umpan balik, diurutkan secara menurun.
     */
    public Map<Integer, Double> PRFWithBIM(List<String> queryTerms, int topK, BIM bim) {
        // Pencarian awal tanpa asumsikan ada dokumen yang relevan (R = 0)
        Map<Integer, Double> initialRanking = bim.hitungRSV(queryTerms, 0, new HashMap<>());

        // Ambil ID dari K-dokumen teratas yang diasumsikan relevan
        Set<Integer> topKDocuments = initialRanking.keySet().stream()
                .limit(topK)
                .collect(Collectors.toSet());

        int R = topKDocuments.size(); // Total dokumen relevan (diasumsikan = K)
        Map<String, Integer> rtMap = new HashMap<>(); // Menyimpan jumlah dokumen relevan yang mengandung term tertentu
        
        for (String term : queryTerms) {
            int rt = 0;
            for (PostingNode node : invertedIndex.getPostingList(term)) {
                int docID = node.getDocID();
                // kalo dokumen yang mengandung term ada di dalam set Top-K, tambah nilai rt
                if (topKDocuments.contains(docID)) rt++;
            }
            rtMap.put(term, rt);
        }

        // pencarian final menggunakan parameter PRF (R dan rt) yang telah diperbarui
        Map<Integer, Double> finalRanking = bim.hitungRSV(queryTerms, R, rtMap);

        return finalRanking;
    }

/**
     * Melakukan proses Pseudo Relevance Feedback menggunakan BM.
     *
     * @param queryTerms Daftar kata (term) dari kueri pencarian awal.
     * @param topK Jumlah dokumen teratas dari pencarian pertama yang akan diasumsikan relevan.
     * @param bm Instansiasi dari model pemeringkatan bm (bm11/bm25).
     * 
     * @return Map berisi ID dokumen dan skor akhirnya (RSV), yang sudah dihitung ulang 
     * berdasarkan informasi umpan balik, diurutkan secara menurun.
     */
    public Map<Integer, Double> PRFWithBM(List<String> queryTerms, int topK, BM bm) {
         // Pencarian awal tanpa asumsikan ada dokumen yang relevan (R = 0)
        Map<Integer, Double> initialRanking = bm.calculateRSV(queryTerms, 0, new HashMap<>());

        // Ambil Top-K dokumen
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

        Map<Integer, Double> finalRanking = bm.calculateRSV(queryTerms, R, rtMap);

        return finalRanking;
    }

/**
     * Melakukan proses Pseudo Relevance Feedback menggunakan Two Poisson Model.
     *
     * @param queryTerms Daftar kata (term) dari kueri pencarian awal.
     * @param topK Jumlah dokumen teratas dari pencarian pertama yang akan diasumsikan relevan.
     * @param tpm Instansiasi dari model two poisson.
     * 
     * @return Map berisi ID dokumen dan skor akhirnya (RSV), yang sudah dihitung ulang 
     * berdasarkan informasi umpan balik, diurutkan secara menurun.
     */
    public Map<Integer, Double> PRFWith2PM(List<String> queryTerms, int topK, TwoPoissonModel tpm) {
        Map<Integer, Double> initialRanking = tpm.calculateRSV(queryTerms, 0, new HashMap<>());

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

        Map<Integer, Double> finalRanking = tpm.calculateRSV(queryTerms, R, rtMap);

        return finalRanking;
    }
}