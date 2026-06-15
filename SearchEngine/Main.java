import index.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

import model.*;
import preprocess.TextPreprocessor;
import reader.DocumentReader;
import query.*;
import evaluator.Evaluator;

/**
 * Kelas Main berfungsi sebagai titik masuk (entry point) utama untuk
 * menjalankan
 * sistem mesin pencari (Information Retrieval).
 * 
 * Kelas ini menggabungkan seluruh komponen sistem, mulai dari pembacaan
 * dokumen,
 * pembuatan indeks (Inverted Index), pemrosesan kueri, hingga pemeringkatan
 * menggunakan
 * berbagai model probabilistik (BIM, Two Poisson, BM11, BM25) beserta Pseudo
 * Relevance Feedback.
 * Kelas ini juga melakukan komputasi evaluasi performa seperti Precision,
 * Recall, dan F1-Score
 * menggunakan ground truth eksternal (folder RES).
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM
 * 
 * @author Axel, Alex, Keane
 */
public class Main {

    /**
     * Path (lokasi) folder yang berisi file-file ground truth (relevance
     * judgements)
     * untuk evaluasi hasil pencarian.
     */
    private static final String RES_FOLDER = "RES";

    /**
     * Path file yang berisi daftar kueri pengujian beserta ID-nya.
     */
    private static final String QUERY_FILE = "query.txt";

    /**
     * Path file yang berisi dataset Cranfield.
     */
    private static final String folderPath = "DataSet";

    /**
     * Metode utama yang akan dijalankan pertama kali saat program dimulai.
     * * @param args Argumen baris perintah (command line arguments), saat ini tidak
     * digunakan.
     */
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        int totalDocs = numberDocument(sc);

        // Membaca seluruh dokumen dari dataset
        Map<Integer, String> documents = readDocuments(folderPath, totalDocs);
        if (documents == null || documents.isEmpty()) {
            System.out.println("Document is empty / wrong path.");
            return;
        }

        // Membangun Inverted Index berdasarkan dokumen yang telah dibaca
        InvertedIndex invertedIndex = buildInvertedIndex(documents, totalDocs);

        // Memuat semua kueri dari query.txt ke dalam struktur Map<Teks Query, QueryID>
        Map<String, Integer> queryLookup = loadQueryLookup(QUERY_FILE);
        System.out.println("Query lookup loaded: " + queryLookup.size() + " queries.");

        //runEvaluation(invertedIndex, queryLookup);

        // Menjalankan sistem interaktif mesin pencari
        run(invertedIndex, sc, queryLookup);
    }

    /**
     * Membaca file kumpulan kueri dan memetakannya ke dalam struktur data Map.
     * Teks kueri akan dinormalisasi (huruf kecil dan spasi berlebih dihapus) agar
     * mudah dicocokkan saat pengguna memasukkan input nanti.
     *
     * @param queryFilePath Lokasi file text yang berisi daftar kueri.
     * @return Map yang berisi pasangan kueri yang sudah dinormalisasi (String)
     *         dan ID kuerinya (Integer).
     */
    private static Map<String, Integer> loadQueryLookup(String queryFilePath) {
        Map<String, Integer> lookup = new LinkedHashMap<>();
        try (
                FileInputStream fileInputStream = new FileInputStream(queryFilePath);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String cleanedLine = line.replace("\r", "").trim();
                if (cleanedLine.isEmpty()) {
                    continue;
                }

                // Mendeteksi pemisah antara ID dan teks kueri, format: "ID\tteks" atau "ID
                // teks"
                int separatorIndex = cleanedLine.indexOf('\t');
                if (separatorIndex < 0) {
                    separatorIndex = cleanedLine.indexOf(' ');
                }

                if (separatorIndex < 0) {
                    continue; // Lewati baris jika tidak ada pemisah yang valid
                }

                // Mengekstrak ID dan teks kueri mentah
                String idString = cleanedLine.substring(0, separatorIndex).trim();
                String rawQueryText = cleanedLine.substring(separatorIndex + 1).trim();

                // Melakukan parsing ID dan menormalisasi teks kueri
                int qid = Integer.parseInt(idString);
                String finalQueryText = rawQueryText.toLowerCase().replaceAll("\\s+", " ");

                lookup.put(finalQueryText, qid);
            }

            // Menampilkan contoh (entry pertama) untuk memastikan proses ekstraksi berhasil
            if (!lookup.isEmpty()) {
                Map.Entry<String, Integer> firstEntry = lookup.entrySet().iterator().next();
                System.out.println("Contoh query: " + firstEntry);
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("[Warning] Gagal membaca file query: " + e.getMessage());
        }

        return lookup;
    }

    /**
     * Meminta input dari pengguna untuk menentukan batasan jumlah dokumen
     * yang akan diindeks dari dataset.
     *
     * @param sc Objek Scanner untuk menerima input dari konsol.
     * @return Total jumlah dokumen yang akan diproses.
     */
    private static int numberDocument(Scanner sc) {
        System.out.print("Enter total documents indexed in dataset (max 1400): ");
        int totalDocs = sc.nextInt();
        sc.nextLine(); // Konsumsi karakter newline yang tersisa
        return totalDocs;
    }

    /**
     * Membaca isi teks dari dokumen-dokumen yang ada di dalam folder dataset.
     *
     * @param folderPath Lokasi folder (direktori) tempat dataset dokumen disimpan.
     * @param totalDocs  Jumlah maksimal dokumen yang akan dibaca.
     * @return Map yang memetakan ID dokumen ke isi konten teksnya.
     */
    private static Map<Integer, String> readDocuments(String folderPath, int totalDocs) {
        System.out.println("Reading document from " + folderPath + " folder...");
        DocumentReader reader = new DocumentReader(folderPath);
        return reader.readAll(totalDocs);
    }

    /**
     * Membangun struktur data Inverted Index yang menampung posting list untuk tiap
     * term.
     * Di dalam proses ini, teks dokumen juga melalui tahap pra-pemrosesan
     * (preprocessing),
     * penghitungan panjang rata-rata dokumen, dan penetapan skip pointer.
     *
     * @param documents Map yang berisi pasangan ID dokumen dan konten teks aslinya.
     * @param totalDocs Jumlah total dokumen yang ada.
     * @return Objek InvertedIndex yang sudah terisi penuh dan siap digunakan.
     */
    private static InvertedIndex buildInvertedIndex(Map<Integer, String> documents, int totalDocs) {
        System.out.println("Building Inverted Index...");
        TextPreprocessor preprocessor = new TextPreprocessor();
        InvertedIndex invertedIndex = new InvertedIndex();

        for (int docID = 1; docID <= totalDocs; docID++) {
            if (!documents.containsKey(docID))
                continue;
            String content = documents.get(docID);

            // Proses pembersihan dan tokenisasi dokumen
            List<String> terms = preprocessor.process(content);
            invertedIndex.setDocumentLength(docID, terms.size());

            // Tambahkan setiap term dokumen ke dalam indeks
            for (String term : terms)
                invertedIndex.addDocument(term, docID);
        }

        // Kalkulasi nilai statistik tambahan setelah indeks terbangun
        invertedIndex.computeAverageDocumentLength();
        // invertedIndex.assignSkipPointer();

        System.out.println("Inverted index has been built successfully.");
        return invertedIndex;
    }

    private static void runEvaluation(InvertedIndex invertedIndex, Map<String, Integer> queryLookup) {
        TextPreprocessor preprocessor = new TextPreprocessor();

        BIM bim = new BIM();
        bim.setInvertedIndex(invertedIndex);

        BM bm11 = new BM(1.5, 1.0);
        bm11.setInvertedIndex(invertedIndex);

        BM bm25 = new BM(1.5, 0.65);
        bm25.setInvertedIndex(invertedIndex);

        TwoPoissonModel twoPoisson = new TwoPoissonModel(1.5);
        twoPoisson.setInvertedIndex(invertedIndex);

        PseudoRelevanceFeedback prf = new PseudoRelevanceFeedback();
        prf.setInvertedIndex(invertedIndex);

        String[] modelNames = { "BIM", "Two Poisson", "BM11", "BM25" };
        double[] totalP = { 0, 0, 0, 0 };
        double[] totalR = { 0, 0, 0, 0 };
        double[] totalF1 = { 0, 0, 0, 0 };
        double[] total11pt = { 0, 0, 0, 0 };
        int count = 0;

        for (Map.Entry<String, Integer> entry : queryLookup.entrySet()) {
            Set<Integer> groundTruth = Evaluator.loadGroundTruth(RES_FOLDER, entry.getValue());
            if (groundTruth.isEmpty())
                continue;

            List<String> queryTerms = preprocessor.process(entry.getKey());
            if (queryTerms.isEmpty())
                continue;

            Map<Integer, Double>[] results = new Map[] {
                    prf.PRF(queryTerms, 10, bim),
                    prf.PRF(queryTerms, 10, twoPoisson),
                    prf.PRF(queryTerms, 10, bm11),
                    prf.PRF(queryTerms, 10, bm25)
            };

            for (int i = 0; i < 4; i++) {
                List<Integer> ranked = Evaluator.toRankedList(results[i]);
                double p = Evaluator.precision(ranked, groundTruth, 10);
                double r = Evaluator.recall(ranked, groundTruth, 10);
                totalP[i] += p;
                totalR[i] += r;
                totalF1[i] += Evaluator.f1Score(p, r);
                total11pt[i] += Evaluator.elevenPointAveragePrecision(ranked, groundTruth);
            }
            count++;
        }

        System.out.println("\n=== Hasil Evaluasi Rata-rata (" + count + " query)===");
        for (int i = 0; i < 4; i++) {
            System.out.printf("%-12s| P@10: %.4f | R@10: %.4f | F1: %.4f | 11-pt AP:%.4f%n",
                    modelNames[i], totalP[i] / count, totalR[i] / count, totalF1[i] / count,
                    total11pt[i] / count);
        }
    }

    /**
     * Loop interaktif utama yang terus menerima kueri pengguna dan memprosesnya
     * dengan berbagai macam model perankingan, lalu menampilkan hasilnya.
     *
     * @param invertedIndex Indeks dokumen yang sudah terbangun.
     * @param sc            Objek Scanner untuk membaca kueri input pengguna.
     * @param queryLookup   Map berisi daftar ground truth kueri untuk proses
     *                      evaluasi performa.
     */
    private static void run(InvertedIndex invertedIndex, Scanner sc, Map<String, Integer> queryLookup) {
        // Inisialisasi model pemeringkatan
        BIM bim = new BIM();
        bim.setInvertedIndex(invertedIndex);

        // Menggunakan kelas dasar BM dengan parameter (k, b) masing-masing untuk BM11
        // dan BM25
        BM bm11 = new BM(1.5, 1);
        bm11.setInvertedIndex(invertedIndex);

        BM bm25 = new BM(1.5, 0.75);
        bm25.setInvertedIndex(invertedIndex);

        TwoPoissonModel twoPoisson = new TwoPoissonModel(1.5);
        twoPoisson.setInvertedIndex(invertedIndex);

        PseudoRelevanceFeedback prf = new PseudoRelevanceFeedback();
        prf.setInvertedIndex(invertedIndex);

        System.out.println("---------------------------------------------");
        System.out.println("           Probabilistic Retrieval           ");
        System.out.println("---------------------------------------------");

        // Loop untuk terus menerima input pencarian hingga pengguna mengetik 'exit'
        while (true) {
            System.out.print("\nEnter query (type 'exit' to cancel): ");
            String input = sc.nextLine().trim();

            Query query = new Query(input);

            if (input.equalsIgnoreCase("exit"))
                break;
            if (input.isEmpty())
                continue;

            List<String> queryTerms = query.process();

            int topK = 10;
            // Jalankan seluruh varian pemodelan menggunakan Pseudo Relevance Feedback
            // (Asumsi Top 10 relevan)
            Map<Integer, Double> resultBIM = prf.PRF(queryTerms, topK, bim);
            Map<Integer, Double> result2PM = prf.PRF(queryTerms, topK, twoPoisson);
            Map<Integer, Double> resultBM11 = prf.PRF(queryTerms, topK, bm11);
            Map<Integer, Double> resultBM25 = prf.PRF(queryTerms, topK, bm25);

            // Lakukan normalisasi input pengguna untuk dicari di lookup table kueri
            // agar bisa dievaluasi dengan ground truth
            String normalizedInput = input.replace("\r", "").trim().toLowerCase().replaceAll("\\s+", " ");
            Integer queryID = queryLookup.get(normalizedInput);

            Set<Integer> groundTruth = (queryID != null)
                    ? Evaluator.loadGroundTruth(RES_FOLDER, queryID)
                    : new HashSet<>();

            // Tampilkan peringkat dan evaluasi performa (Precision, Recall, F1) untuk
            // masing-masing model
            System.out.println("\n--- BIM ---");
            printResultWithEval(resultBIM, groundTruth);

            System.out.println("\n--- Two Poisson ---");
            printResultWithEval(result2PM, groundTruth);

            System.out.println("\n--- BM11 ---");
            printResultWithEval(resultBM11, groundTruth);

            System.out.println("\n--- BM25 ---");
            printResultWithEval(resultBM25, groundTruth);

            if (queryID == null) {
                System.out.println("\n[Info] Query tidak ditemukan di query.txt. evaluasi dilewati.");
            }
        }
    }

    /**
     * Menampilkan maksimal 10 hasil dokumen teratas (Top-10 Ranking) ke layar
     * konsol.
     * Jika ground truth tersedia untuk kueri tersebut, metode ini juga akan
     * mencetak
     * metrik evaluasi sistem seperti Precision@10, Recall@10, dan F1-Score@10.
     *
     * @param result      Map berisi ID dokumen dan skor RSV (sudah diurutkan secara
     *                    menurun).
     * @param groundTruth Set berisi kumpulan ID dokumen yang benar-benar relevan
     *                    (dari data uji).
     */
    private static void printResultWithEval(Map<Integer, Double> result, Set<Integer> groundTruth) {
        if (result == null || result.isEmpty()) {
            System.out.println("--> There is no relevant document.");
            return;
        }

        int rank = 1;
        for (Map.Entry<Integer, Double> entry : result.entrySet()) {
            System.out.printf("Rank %d | DocID: %d | Score: %.4f%n",
                    rank++, entry.getKey(), entry.getValue());
            if (rank > 10)
                break; // limit top 10 dokumen
        }

        // Eksekusi evaluasi performa jika ground truth memiliki data
        if (!groundTruth.isEmpty()) {
            List<Integer> ranked = Evaluator.toRankedList(result);
            double p = Evaluator.precision(ranked, groundTruth, 10);
            double r = Evaluator.recall(ranked, groundTruth, 10);
            double f1 = Evaluator.f1Score(p, r);

            double elevenPtAvg = Evaluator.elevenPointAveragePrecision(ranked, groundTruth);

            System.out.printf("Precision at 10: %.4f | Recall at 10: %.4f | F1 at 10: %.4f%n", p, r, f1);
            System.out.printf("11-Point Average Precision: %.4f%n", elevenPtAvg);
        }
    }
}