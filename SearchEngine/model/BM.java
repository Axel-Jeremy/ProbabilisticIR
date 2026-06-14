package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import index.*;

/**
 * Kelas BM merepresentasikan implementasi dari algoritma pemeringkatan BM25/BM11.
 * Algoritma ini merupakan salah satu fungsi ranking (pemeringkatan) yang paling banyak 
 * digunakan dalam Information Retrieval. 
 * 
 * BM11 memanfaatkan perhitungan bobot dari model BIM untuk menghitung 
 * Retrieval Status Value (RSV) akhir sebuah dokumen.
 * 
 * BM25 memperluas Binary Independence Model (BIM) 
 * dengan mempertimbangkan pengaruh Term Frequency (TF) serta normalisasi panjang dokumen.
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM
 * 
 * @author Keane 
 */
public class BM {
    
    /**
     * Referensi ke model BIM (Binary Independence Model)
     */
    private BIM bim;
    
    /**
     * Parameter tuning k
     * untuk membatasi nilai pengaruh dari Term Frequency (TF).
     */
    private double k;
    
    /**
     * Parameter tuning b.
     * Digunakan untuk mengontrol seberapa besar pengaruh normalisasi panjang dokumen.
     * Jika b = 1, normalisasi panjang dokumen bekerja sepenuhnya (ekuivalen dengan BM11).
     * Jika b = 0.75, normalisasi panjang dokumen bekerja sebagian (ekuivalen dengan BM25).
     * Jika b = 0, tidak ada normalisasi panjang dokumen sama sekali (ekuivalen dengan BM15).
     */
    private double b;
    
    /**
     * referensi inverted index
     */
    private InvertedIndex invertedIndex;

    /**
     * Konstruktor untuk menginisialisasi model pemeringkatan BM.
     *
     * @param k Nilai parameter $k$ untuk mengatur saturasi Term Frequency.
     * @param b Nilai parameter $b$ untuk mengatur normalisasi panjang dokumen.
     */
    public BM(double k, double b){
        this.bim = new BIM();
        this.k = k;
        this.b = b;
    }

    /**
     * Menetapkan Inverted Index yang akan digunakan sebagai basis pencarian.
     *
     * @param index Objek InvertedIndex yang sudah terisi dengan data korpus dokumen.
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
            
            // skip kalo term tidak ditemukan di dokumen manapun
            if (Nt == 0) continue;
            
            // Ambil nilai rt dari relevance map
            int rt = rtMap.getOrDefault(term, 0);

            // Hitung bobot dasar (wt) menggunakan model BIM
            double wt = bim.hitungWt(Nt, N, rt, R);
            
            for (PostingNode node : invertedIndex.getPostingList(term)) {
                int docID = node.getDocID();
                int ftd = node.getTermFrequency();
                int ld = invertedIndex.getDocumentLength(docID);
                double lavg = invertedIndex.getAverageDocumentLength();

                // Perhitungan skor untuk satu term dalam dokumen tertentu
                double rsv = (ftd * (k + 1) * wt) / (ftd + k * b * ld / lavg + k * (1 - b)); 

                // Akumulasikan ke total skor RSV dokumen tersebut
                score.put(docID, score.getOrDefault(docID, 0.0) + rsv);
            }
        }

        // Kembalikan map skor yang sudah diurutkan dari nilai tertinggi
        return sortDescending(score);
    }

    /**
     * method untuk mengurutkan skor dokumen secara menurun
     * Dokumen dengan probabilitas / skor relevansi tertinggi akan diletakkan di bagian atas.
     *
     * @param score Map pasangan ID dokumen dan nilai skor RSV-nya.
     * @return Map baru (LinkedHashMap) yang urutannya dari skor terbesar hingga terkecil.
     */
    public Map<Integer, Double> sortDescending(Map<Integer, Double> score) {
        List<Map.Entry<Integer, Double>> entries = new ArrayList<>(score.entrySet());
        
        // Mengurutkan koleksi dengan membandingkan nilai b terhadap a (descending order)
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        Map<Integer, Double> hasil = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> e : entries) {
            hasil.put(e.getKey(), e.getValue());
        }
        return hasil;
    }
}