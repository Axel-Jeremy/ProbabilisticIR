package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import index.*;
import sorter.Sorter;

/**
 * Kelas TwoPoissonModel merepresentasikan implementasi dari algoritma pemeringkatan 
 * Two Poisson Model (2PM). 
 * 
 * Model ini merupakan salah satu variasi probabilistik yang membatasi 
 * pengaruh Term Frequency (TF) menggunakan parameter k, serupa dengan BM25. 
 * Perbedaan utamanya adalah pada 2PM, tidak dilakukan normalisasi terhadap panjang 
 * dokumen (mengabaikan parameter b dan panjang rata-rata dokumen)
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM
 * 
 * @author Keane 
 * 
 */
public class TwoPoissonModel {
    
    /**
     * Referensi ke model BIM (Binary Independence Model) untuk mendapatkan bobot term dasar ($w_t$).
     */
    private BIM bim;
    
    /**
     * Parameter tuning k
     * untuk membatasi nilai pengaruh dari Term Frequency (TF).
     */
    private double k;
    
    /**
     * referensi inverted index
     */
    private InvertedIndex invertedIndex;

    /**
     * Referensi ke Sorter untuk melakukan pengurutan skor
     */
    private Sorter sorter;

    /**
     * Konstruktor untuk menginisialisasi model pemeringkatan Two Poisson.
     *
     * @param k Nilai parameter k untuk mengatur besar pengaruh Term Frequency.
     */
    public TwoPoissonModel(double k){
        this.bim = new BIM();
        this.k = k;
        this.sorter = new Sorter();
    }

    /**
     * Menetapkan Inverted Index yang akan digunakan sebagai basis pencarian.
     *
     * @param index Objek InvertedIndex yang sudah terisi dengan dokumen koleksi.
     */
    public void setInvertedIndex(InvertedIndex index) {
        this.invertedIndex = index;
    }

        /**
     * Menghitung nilai Retrieval Status Value (RSV) dokumen berdasarkan kueri yang diberikan.
     * 
     * @param queryTerms Daftar kata (term) yang dicari dalam kueri
     * @param R Total dokumen relevan
     * @param rtMap Map yang memetakan suatu term kueri ke jumlah kemunculannya di dokumen relevan
     * 
     * @return Map yang berisi pasangan ID dokumen dan nilai skor RSV-nya,
     * diurutkan secara descending (dari skor tertinggi ke terendah)
     */
    public Map<Integer, Double> calculateRSV(List<String> queryTerms, int R, Map<String, Integer> rtMap) {
        int N = invertedIndex.getTotalDocuments();
        Map<Integer, Double> score = new HashMap<>();

        for (String term : queryTerms) {
            // Document Frequency dari term
            int Nt = invertedIndex.getDfByTerm(term);
            
            if (Nt == 0) continue;
            
            // Ambil jumlah kemunculan di dokumen relevan
            int rt = rtMap.getOrDefault(term, 0);

            // Hitung bobot probabilistik BIM (wt) dari term
            double wt = bim.hitungWt(Nt, N, rt, R);
            
            for (PostingNode node : invertedIndex.getPostingList(term)) {
                int docID = node.getDocID();
                int ftd = node.getTermFrequency();

                // Perhitungan skor 2PM untuk dokumen saat ini
                double rsv = (ftd * (k + 1) * wt) / (ftd + k); 

                // Akumulasikan nilai rsv tersebut ke dalam total skor dokumen
                score.put(docID, score.getOrDefault(docID, 0.0) + rsv);
            }
        }

        // Kembalikan map dokumen dan skornya yang sudah diurutkan
        return sortDescending(score);
    }

    /**
     * Fungsi pembantu (utility) untuk mengurutkan hasil perhitungan skor dokumen 
     * secara menurun (descending).
     *
     * @param score Map yang berisi ID dokumen sebagai key dan skor RSV sebagai value.
     * @return Map baru (LinkedHashMap) yang urutannya dari skor terbesar hingga terkecil.
     */
    public Map<Integer, Double> sortDescending(Map<Integer, Double> score) {
        return sorter.sortDescending(score);
    }
}