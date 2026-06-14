package evaluator;

import java.io.*;
import java.util.*;

/**
 * Kelas Evaluator berfungsi sebagai alat uji untuk menghitung 
 * metrik performa dari sistem Information Retrieval.
 * 
 * Kelas ini membandingkan hasil dokumen yang ditarik oleh sistem pemeringkatan 
 * dengan data kebenaran aktual (ground truth / relevance judgements) untuk mengukur 
 * tingkat akurasi dan efektivitas model pencarian.
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM
 * 
 * @author Alex
 */
public class Evaluator {

    /**
     * Membaca data ground truth (kebenaran aktual) dari sebuah file teks.
     * Dokumen akan diasumsikan relevan jika nilai relevancescore >= 1, 
     * dan tidak relevan jika nilainya -1 atau 0.
     *
     * @param resFolderPath Lokasi direktori (folder) yang menyimpan file ground truth.
     * @param queryID ID kueri yang sedang dievaluasi, digunakan untuk mencari nama file yang sesuai.
     * @return Set yang berisi kumpulan ID dokumen (DocID) yang benar-benar relevan dengan kueri tersebut.
     */
    public static Set<Integer> loadGroundTruth(String resFolderPath, int queryID) {
        Set<Integer> relevant = new HashSet<>();
        File file = new File(resFolderPath, queryID + ".txt");

        if (!file.exists()) {
            System.out.println("[Warning] Ground truth tidak ditemukan untuk query " + queryID);
            return relevant;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(file),
                    java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("\r", "").trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 3) continue;

                int docID    = Integer.parseInt(parts[1]);
                int relScore = Integer.parseInt(parts[2]);

                // cuma masukkan dokumen yang terbukti relevan ke dalam himpunan
                if (relScore >= 1) {
                    relevant.add(docID);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("[Warning] Gagal membaca ground truth query " + queryID + ": " + e.getMessage());
        }

        return relevant;
    }

    /**
     * Menghitung nilai Precision at K
     * Precision mengukur rasio ketepatan, yaitu seberapa banyak dokumen relevan 
     * yang berhasil ditemukan di antara K-dokumen teratas yang ditarik oleh sistem.
     * 
     * Rumus: 
     * Precision at K = (Jumlah dokumen relevan di Top-K) / K
     *
     * @param retrieved List ID dokumen yang ditarik oleh sistem, diasumsikan sudah terurut dari skor tertinggi.
     * @param relevant Set ID dokumen yang secara aktual relevan (berdasarkan ground truth)
     * @param k Batas jumlah dokumen teratas (Top-K) yang ingin dievaluasi
     * @return Nilai Precision dalam bentuk desimal antara 0 - 1.0
     */
    public static double precision(List<Integer> retrieved, Set<Integer> relevant, int k) {
        if (retrieved == null || retrieved.isEmpty() || k <= 0) return 0.0;
        List<Integer> topK = retrieved.subList(0, Math.min(k, retrieved.size()));
        long tp = topK.stream().filter(relevant::contains).count();
        return (double) tp / topK.size();
    }

    /**
     * Menghitung nilai Recall at K
     * 
     * Rumus:
     * Recall = (Jumlah dokumen relevan di Top-K) / (Total dokumen relevan)
     *
     * @param retrieved List ID dokumen yang ditarik oleh sistem, terurut dari skor tertinggi.
     * @param relevant Set ID dokumen aktual yang relevan (berdasarkan ground truth)
     * @param k Batas jumlah dokumen teratas (Top-K) yang dievaluasi
     * @return Nilai Recall dalam bentuk desimal antara 0 - 1.0
     */
    public static double recall(List<Integer> retrieved, Set<Integer> relevant, int k) {
        if (retrieved == null || retrieved.isEmpty() || relevant == null || relevant.isEmpty()) return 0.0;
        List<Integer> topK = retrieved.subList(0, Math.min(k, retrieved.size()));
        long tp = topK.stream().filter(relevant::contains).count();
        return (double) tp / relevant.size();
    }

    /**
     * Menghitung nilai F1-Score.
     * F1-Score adalah rata-rata harmonik (harmonic mean) dari Precision dan Recall. 
     * 
     * Rumus:
     * F1 = 2 * (Precision * Recall) / (Precision + Recall)
     *
     * @param p Nilai Precision yang telah dihitung
     * @param r Nilai Recall yang telah dihitung
     * @return Nilai F1-Score dalam rentang desimal 0 - 1.0
     */
    public static double f1Score(double p, double r) {
        if (p + r == 0) return 0.0;
        return 2 * p * r / (p + r);
    }

    /**
     * Menghitung 11-point Interpolated Average Precision berdasarkan 
     * slide materi Information Retrieval: Evaluation.
     * @param retrieved List berisi DocID dokumen yang dikembalikan oleh model (terurut dari skor tertinggi).
     * @param relevant Set berisi DocID dokumen yang relevan (ground truth).
     * @return Nilai 11-point Average Precision (dalam desimal 0.0 - 1.0).
     */
    public static double elevenPointAveragePrecision(List<Integer> retrieved, Set<Integer> relevant) {
        // Jika tidak ada dokumen yang relevan sama sekali di ground truth, maka skornya 0
        if (relevant.isEmpty()) {
            return 0.0;
        }

        int totalRelevant = relevant.size();
        List<Double> recalls = new ArrayList<>();
        List<Double> precisions = new ArrayList<>();
        
        int tp = 0; // True Positives

        // Hitung precision dan recall pada setiap level K dokumen yang ditarik
        for (int i = 0; i < retrieved.size(); i++) {
            if (relevant.contains(retrieved.get(i))) {
                tp++;
            }
            double precisionAtK = (double) tp / (i + 1);
            double recallAtK = (double) tp / totalRelevant;
            
            precisions.add(precisionAtK);
            recalls.add(recallAtK);
        }

        // 11 titik standard recall (0.0 - 1.0)
        double[] elevenPoints = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        double sumInterpolatedPrecision = 0.0;

        // 3. Hitung interpolated precision untuk setiap level recall r_j
        for (double r_j : elevenPoints) {
            double maxPrecision = 0.0;
            
            // Gunakan formula interpolasi: P(r_j) = max(P(r')) untuk r' >= r_j
            for (int i = 0; i < recalls.size(); i++) {
                if (recalls.get(i) >= r_j) {
                    if (precisions.get(i) > maxPrecision) {
                        maxPrecision = precisions.get(i);
                    }
                }
            }
            
            // Tambahkan nilai maksimum (interpolated) yang didapat untuk titik r_j ini
            sumInterpolatedPrecision += maxPrecision;
        }

        // 4. Rata-ratakan keseluruhan dari 11 titik (P_11-pt)
        return sumInterpolatedPrecision / 11.0;
    }

    /**
     * Fungsi untuk mengubah struktur data Map (yang memetakan DocID ke Skor RSV) 
     * menjadi sebuah List yang hanya berisi DocID. 
     * 
     * Urutan elemen di dalam List akan sama persis dengan urutan entri di dalam Map.
     * Karena model mengembalikan map yang sudah diurutkan dari skor tertinggi, 
     * maka hasil konversi ini merupakan list ranking yang siap dievaluasi.
     *
     * @param rankedResult Map hasil pemeringkatan dari model
     * @return List yang berisi ID dokumen (DocID) secara berurutan
     */
    public static List<Integer> toRankedList(Map<Integer, Double> rankedResult) {
        if (rankedResult == null) return new ArrayList<>();
        return new ArrayList<>(rankedResult.keySet());
    }
}