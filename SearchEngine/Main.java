import java.util.*;

import index.*;
import model.*;
import preprocess.TextPreprocessor;

import reader.DocumentReader;

/**
 * Kelas Main digunakan untuk menjalankan
 * aplikasi mesin pencari (Information Retrieval). Kelas ini mengatur alur
 * mulai dari pembacaan dokumen, pembangunan indeks, hingga interaksi
 * kueri dengan pengguna.
 */
public class Main {
    public static void main(String[] args) {
        String folderPath = "../DataSet";

        Scanner sc = new Scanner(System.in);
        int totalDocs = numberDocument(sc);

        // baca semua dokumen dari direktori
        Map<Integer, String> documents = readDocuments(folderPath, totalDocs);

        if (documents == null || documents.isEmpty()) {
            System.out.println("Document is empty / wrong path.");
            return;
        }

        // bangun inverted index
        InvertedIndex invertedIndex = buildInvertedIndex(documents, totalDocs);

        // // Contoh penggunaan term frequency
        // System.out.println("\n=== Contoh Term Frequency ===");
        // invertedIndex.printPostingWithTF("system");

        // jalankan search engine
        run(invertedIndex, sc);
    }

    /**
     * Method untuk meminta input dari pengguna mengenai jumlah dokumen
     * yang ingin diindeks dari dalam dataset.
     * 
     * @param sc Objek Scanner yang digunakan untuk membaca input dari console.
     * 
     * @return Nilai integer yang merepresentasikan total dokumen yang akan
     *         diproses.
     */
    private static int numberDocument(Scanner sc) {
        System.out.print("Enter total documents indexed in dataset (max 1400): ");
        int totalDocs = sc.nextInt();
        sc.nextLine();

        return totalDocs;
    }

    /**
     * Membaca kumpulan dokumen teks dari direktori yang telah ditentukan.
     * 
     * @param folderPath Path atau lokasi folder yang menyimpan dokumen.
     * @param totalDocs  Jumlah dokumen maksimal yang akan dicoba untuk dibaca.
     * 
     * @return Map yang memetakan ID dokumen (Integer) dengan isi teksnya (String).
     */
    private static Map<Integer, String> readDocuments(String folderPath, int totalDocs) {
        System.out.println("Reading document from " + folderPath + " folder...");
        DocumentReader reader = new DocumentReader(folderPath);
        return reader.readAll(totalDocs);
    }

    /**
     * Membangun Inverted Index berdasarkan dokumen-dokumen yang telah dibaca.
     * Proses ini mencakup tokenisasi, case folding, stemming, penyimpanan raw terms
     * untuk koreksi ejaan, serta inisiasi skip pointer.
     * 
     * @param documents Map yang berisi kumpulan ID dokumen beserta teksnya.
     * @param totalDocs Jumlah dokumen untuk batas iterasi (memastikan urutan
     *                  posting list).
     * 
     * @return Objek InvertedIndex yang sudah terisi dan siap digunakan.
     */
    private static InvertedIndex buildInvertedIndex(Map<Integer, String> documents, int totalDocs) {
        System.out.println("Building Inverted Index...");

        TextPreprocessor preprocessor = new TextPreprocessor();
        InvertedIndex invertedIndex = new InvertedIndex();

        // Iterasi berurutan dari 1 agar ID di dalam posting list terurut dengan benar
        for (int docID = 1; docID <= totalDocs; docID++) {
            if (!documents.containsKey(docID))
                continue;

            String content = documents.get(docID);

            // Mengambil dan menyimpan raw terms (tanpa stemming) ke vocabulary
            List<String> rawTerms = preprocessor.getRawTerms(content);
            for (String raw : rawTerms) {
                invertedIndex.addRawTerm(raw);
            }

            // Memproses teks secara penuh (termasuk stemming) untuk dipindahkan ke posting
            // list
            List<String> terms = preprocessor.process(content);
            invertedIndex.setDocumentLength(docID, terms.size());
            for (String term : terms) {
                invertedIndex.addDocument(term, docID);
            }
        }

        // Panggil setelah seluruh looping (seluruh dokumen) selesai
        invertedIndex.computeAverageDocumentLength();

        // Pasang skip pointer setelah struktur indeks untuk semua dokumen selesai
        invertedIndex.assignSkipPointer();
        System.out.println("Inverted index has been built succesfully.");

        return invertedIndex;
    }

    /**
     * Menginisialisasi komponen pencarian (Boolean Model & Tolerant Retrieval)
     * dan menjalankan perulangan (loop) untuk terus menerima kueri dari pengguna.
     * 
     * @param invertedIndex Objek InvertedIndex yang digunakan sebagai rujukan
     *                      pencarian.
     */
    private static void run(InvertedIndex invertedIndex, Scanner sc) {
        // // Menginisialisasi Boolean Model untuk evaluasi logical operator
        // BooleanModel model = new BooleanModel();
        // model.setInvertedIndex(invertedIndex);
        // model.setMaxDocID(invertedIndex.getMaxDocID());

        // // Menginisialisasi Tolerant Retrieval untuk fitur koreksi ejaan (typo)
        // TolerantModel tolerant = new TolerantModel();
        // tolerant.setInvertedIndex(invertedIndex);

        BIM bim = new BIM();
        bim.setInvertedIndex(invertedIndex);

        BM11 bm11 = new BM11(1.5);
        bm11.setInvertedIndex(invertedIndex);

        BM25 bm25 = new BM25(1.5, 0.75);
        bm25.setInvertedIndex(invertedIndex);

        TwoPoissonModel twoPoisson = new TwoPoissonModel(1.5);
        twoPoisson.setInvertedIndex(invertedIndex);

        PseudoRelevanceFeedback prf = new PseudoRelevanceFeedback();
        prf.setInvertedIndex(invertedIndex);

        TextPreprocessor preprocessor = new TextPreprocessor();

        System.out.println("---------------------------------------------");
        System.out.println("         Boolean Tolerant Retrieval          ");
        System.out.println("---------------------------------------------");

        while (true) {
            System.out.print("\nEnter query (type 'exit' to cancel): ");
            String input = sc.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                break;
            }
            if (input.isEmpty()) {
                continue;
            }

            // Query query = new Query(input);
            // query.setInvertedIndex(invertedIndex);
            // query.setTolerantModel(tolerant);
            // query.setModel(model);

            // List<PostingNode> result = query.process();

            List<String> queryTerms = preprocessor.process(input);

            System.out.println("\n--- BIM ---");
            printResult(prf.PRFWithBIM(queryTerms, 10, bim));

            System.out.println("\n--- Two Poisson ---");
            printResult(prf.PRFWith2PM(queryTerms, 10, twoPoisson));

            System.out.println("\n--- BM11 ---");
            printResult(prf.PRFWithBM11(queryTerms, 10, bm11));

            System.out.println("\n--- BM25 ---");
            printResult(prf.PRFWithBM25(queryTerms, 10, bm25));
        }
    }

    /**
     * Formatting dan mencetak daftar dokumen yang relevan (hasil pencarian) ke
     * console.
     * 
     * * @param result List dari PostingNode yang merepresentasikan ID dokumen hasil
     * evaluasi kueri.
     */
    private static void printResult(Map<Integer, Double> result) {
        if (result == null || result.isEmpty()) {
            System.out.println("--> There is no relevant document.");
            return;
        }
        int rank = 1;
        for (Map.Entry<Integer, Double> entry : result.entrySet()) {
            System.out.printf("Rank %d | DocID: %d | Score: %.4f%n",
                    rank++, entry.getKey(), entry.getValue());
            if (rank > 10)
                break;
        }
    }
}