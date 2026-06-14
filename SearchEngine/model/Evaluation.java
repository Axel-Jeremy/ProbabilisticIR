package model;

import java.io.*;
import java.util.*;

/**
 * Kelas Evaluation untuk menghitung metrik evaluasi Information Retrieval.
 *
 * Method yang digunakan oleh Main:
 * - loadGroundTruth  : membaca file RES/{queryID}.txt
 * - precision        : menghitung Precision at K
 * - recall           : menghitung Recall at K
 * - f1Score          : menghitung F1-Score
 * - toRankedList     : mengubah Map hasil model menjadi List DocID terurut
 */
public class Evaluation {

    /**
     * Membaca ground truth dari file RES/{queryID}.txt.
     * Format per baris: queryID docID relevance
     * Relevansi >= 1 dianggap relevan, -1 dianggap tidak relevan.
     *
     * @param resFolderPath path ke folder RES
     * @param queryID       ID query yang ingin dimuat
     * @return Set<Integer> berisi DocID yang relevan
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
     * Menghitung Precision at K.
     * Precision = dokumen relevan yang ditemukan di top-K / K
     *
     * @param retrieved List DocID terurut dari model
     * @param relevant  Set DocID relevan (ground truth)
     * @param k         Jumlah dokumen teratas yang dievaluasi
     * @return nilai Precision (0.0 - 1.0)
     */
    public static double precision(List<Integer> retrieved, Set<Integer> relevant, int k) {
        if (retrieved == null || retrieved.isEmpty() || k <= 0) return 0.0;
        List<Integer> topK = retrieved.subList(0, Math.min(k, retrieved.size()));
        long tp = topK.stream().filter(relevant::contains).count();
        return (double) tp / topK.size();
    }

    /**
     * Menghitung Recall at K.
     * Recall = dokumen relevan yang ditemukan di top-K / total dokumen relevan
     *
     * @param retrieved List DocID terurut dari model
     * @param relevant  Set DocID relevan (ground truth)
     * @param k         Jumlah dokumen teratas yang dievaluasi
     * @return nilai Recall (0.0 - 1.0)
     */
    public static double recall(List<Integer> retrieved, Set<Integer> relevant, int k) {
        if (retrieved == null || retrieved.isEmpty() || relevant == null || relevant.isEmpty()) return 0.0;
        List<Integer> topK = retrieved.subList(0, Math.min(k, retrieved.size()));
        long tp = topK.stream().filter(relevant::contains).count();
        return (double) tp / relevant.size();
    }

    /**
     * Menghitung F1-Score dari Precision dan Recall.
     * F1 = 2 * Precision * Recall / (Precision + Recall)
     *
     * @param p nilai Precision
     * @param r nilai Recall
     * @return nilai F1-Score (0.0 - 1.0)
     */
    public static double f1Score(double p, double r) {
        if (p + r == 0) return 0.0;
        return 2 * p * r / (p + r);
    }

    /**
     * Mengubah Map<DocID, Score> hasil model menjadi List<DocID> terurut.
     * Urutan sudah sesuai ranking (tertinggi ke terendah) karena model
     * mengembalikan LinkedHashMap.
     *
     * @param rankedResult Map hasil model
     * @return List DocID terurut
     */
    public static List<Integer> toRankedList(Map<Integer, Double> rankedResult) {
        if (rankedResult == null) return new ArrayList<>();
        return new ArrayList<>(rankedResult.keySet());
    }
}