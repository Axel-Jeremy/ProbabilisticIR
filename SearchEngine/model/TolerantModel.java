package model;

import java.util.Set;

import index.*;
/**
 * Kelas TolerantModel menangani fitur toleransi kesalahan ketik (spell correction) 
 * pada kueri pencarian. Kelas ini menggunakan algoritma Levenshtein Distance (Edit Distance) 
 * untuk mencari kata kandidat yang paling mirip dari himpunan kosa kata (vocabulary) 
 * yang ada di dalam indeks.
 * 
* Sumber: Membuat sendiri dengan bantuan LLM
 * 
 * @author Keane
 */
public class TolerantModel{

    /**
     * Referensi ke struktur data InvertedIndex yang digunakan untuk mengambil 
     * kosa kata asli (raw vocabulary).
     */
    private static InvertedIndex invertedIndex;

    /**
     * Batas maksimal nilai edit distance (jarak pengubahan) yang dapat ditoleransi.
     * Kata hanya akan dikoreksi jika jaraknya dengan kata di vocabulary <= 2.
     */
    private static int threshold = 2;

    /**
     * Default constructor
     */
    public TolerantModel() {
    }

    /**
     * Menetapkan objek InvertedIndex yang akan digunakan sebagai basis acuan 
     * kosa kata dalam proses pencarian kandidat perbaikan kata.
     * 
     * @param invertedIndex Objek InvertedIndex yang berisi data teks terindeks.
     */
    public void setInvertedIndex(InvertedIndex invertedIndex) {
        TolerantModel.invertedIndex = invertedIndex;
    }

    /**
     * Mengoreksi kata yang diduga typo (salah ketik) dengan mencari kandidat kata 
     * terdekat dari raw vocabulary yang ada di InvertedIndex.
     * 
     * @param rawTerm Kata mentah dari kueri pengguna yang akan dicek.
     * @return Kata yang sudah dikoreksi jika ditemukan kandidat dengan jarak <= threshold. 
     *         Mengembalikan kata aslinya jika kata tersebut sudah benar atau tidak ada 
     *         kandidat kata yang memenuhi syarat batas (threshold).
     */
    public String correct(String rawTerm) {
        Set<String> rawVocab = invertedIndex.getRawVocabulary();

        if (rawVocab.contains(rawTerm)) { // kalo ada di raw vocab berarti term gausah dikoreksi
            return rawTerm;
        }

        String bestCandidate = rawTerm;
        int minDistance = Integer.MAX_VALUE;

        for (String candidate : rawVocab) {
            if (Math.abs(candidate.length() - rawTerm.length()) > threshold) {
                continue;
            }

            int distance = editDistance(rawTerm, candidate);
            if (distance < minDistance) {
                minDistance = distance;
                bestCandidate = candidate;
            }
            if (minDistance == 0)
                break;
        }

        // Kembalikan kata kandidat terbaik jika jaraknya masih masuk akal (<= threshold),
        // jika tidak, kembalikan kata inputannya (rawTerm) karena kemungkinan bukan typo biasa.
        return minDistance <= threshold ? bestCandidate : rawTerm;
    }

    /**
     * Menghitung nilai Levenshtein Distance (Edit Distance) antara dua buah string 
     * menggunakan pendekatan Dynamic Programming.
     * Edit distance adalah jumlah minimum operasi penyisipan (insert), penghapusan (delete), 
     * atau penggantian (replace) karakter yang diperlukan untuk mengubah string s1 menjadi s2.
     * 
     * @param s1 String pertama (biasanya kata kueri).
     * @param s2 String kedua (biasanya kata kandidat dari vocabulary).
     * @return Nilai integer yang merepresentasikan jumlah langkah/operasi minimal 
     *         untuk menyamakan kedua string.
     */
    private int editDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {

                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    int insert = dp[i][j - 1];
                    int delete = dp[i - 1][j];
                    int replace = dp[i - 1][j - 1];

                    dp[i][j] = 1 + Math.min(insert,
                            Math.min(delete, replace));
                }
            }
        }
        // Hasil akhir berada di pojok kanan bawah matriks
        return dp[m][n];
    }
}
