package model;

import java.util.*;
import index.InvertedIndex;
import index.PostingNode;
import sorter.Sorter;

/**
 * Kelas BIM merepresentasikan implementasi dari Binary Independence Model.
 * Model probabilistik ini digunakan dalam sistem Information Retrieval untuk 
 * menghitung estimasi relevansi dokumen terhadap suatu kueri dan memberikan peringkat 
 * (ranking) berdasarkan skor Retrieval Status Value (RSV).
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM
 * 
 * @author Keane, Axel
 */
public class BIM {
    
    //constructor
    public BIM(){
        this.sorter = new Sorter();
    }

    /**
     * Referensi ke struktur data InvertedIndex
     */
    private InvertedIndex invertedIndex;

    /**
     * Referensi ke Sorter untuk melakukan pengurutan skor
     */
    private Sorter sorter;

    /**
     * Menetapkan objek InvertedIndex yang akan dievaluasi oleh model BIM ini.
     * * @param index Objek InvertedIndex yang sudah terisi dengan dokumen koleksi.
     */
    public void setInvertedIndex(InvertedIndex index) {
        this.invertedIndex = index;
    }

    /**
     * Menghitung bobot term (weight term) berdasarkan estimasi probabilitas term muncul di 
     * dokumen relevan dan term muncul di dokumen tidak relevan.
     * 
     * @param Nt Jumlah dokumen yang mengandung term tersebut (Document Frequency).
     * @param N Jumlah total keseluruhan dokumen di dalam koleksi.
     * @param rt banyaknya dokumen yang mengandung term t yang dinilai relevan.
     * @param R banyaknya dokumen yang dinilai relevan.
     * 
     * @return Nilai bobot term dalam tipe data double.
     */
    public double hitungWt(int Nt, int N, int rt, int R) {
        double pt = (R == 0) ? 0.5 : (rt + 0.5) / (R + 1);
        double ut = (R == 0) ? (double) Nt / N : (Nt - rt + 0.5) / (N - R + 1);
        return Math.log10(pt / ut);
    }

    /**
     * Menghitung nilai Retrieval Status Value (RSV) untuk semua dokumen.
     * RSV dihitung dengan menjumlahkan bobot term (wt) dari setiap term kueri 
     * yang ditemukan di dalam dokumen.
     * 
     * @param queryTerms Daftar term (kata) yang dimasukkan pengguna dalam kueri.
     * @param R Total jumlah dokumen yang dinilai relevan (digunakan untuk perhitungan bobot).
     * @param rtMap Map yang memetakan suatu term kueri ke jumlah kemunculannya di set dokumen relevan (rt).
     *
     * @return Map yang berisi pasangan ID dokumen dan nilai skor RSV-nya. Hasil map ini 
     * sudah diurutkan dari skor tertinggi ke terendah (descending).
     */
    public Map<Integer, Double> hitungRSV(List<String> queryTerms, int R, Map<String, Integer> rtMap) {
        int N = invertedIndex.getTotalDocuments();
        Map<Integer, Double> score = new HashMap<>();

        for (String term : queryTerms) {
            int Nt = invertedIndex.getDfByTerm(term);
            if (Nt == 0)
                continue; // kalo term ga ada di koleksi dokumen sama sekali

            int rt = rtMap.getOrDefault(term, 0);
            double wt = hitungWt(Nt, N, rt, R);

            for (PostingNode node : invertedIndex.getPostingList(term)) {
                int docID = node.getDocID();
                // Tambahkan bobot term ke total skor (RSV) dokumen
                score.put(docID, score.getOrDefault(docID, 0.0) + wt);
            }
        }

        return sortDescending(score);
    }

    /**
     * Fungsi untuk mengurutkan hasil perhitungan skor dokumen 
     * secara menurun (descending) agar dokumen yang paling relevan berada di urutan teratas.
     * 
     * @param score Map yang berisi ID dokumen sebagai key dan skor RSV sebagai value.
     * 
     * @return Map baru yang sudah terurut dari nilai skor tertinggi hingga terendah.
     */
    public Map<Integer, Double> sortDescending(Map<Integer, Double> score) {
        return sorter.sortDescending(score);
    }
}