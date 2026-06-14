import index.*;
import java.io.*;
import java.util.*;
import model.*;
import preprocess.TextPreprocessor;
import reader.DocumentReader;

/**
 * Kelas Main untuk menjalankan mesin pencari (Information Retrieval)
 * dengan evaluasi Precision & Recall menggunakan ground truth eksternal (RES).
 */
public class Main {

    private static final String RES_FOLDER   = "../RES";
    private static final String QUERY_FILE   = "../query.txt";

    public static void main(String[] args) {
        String folderPath = "DataSet";

        Scanner sc = new Scanner(System.in);
        int totalDocs = numberDocument(sc);

        Map<Integer, String> documents = readDocuments(folderPath, totalDocs);
        if (documents == null || documents.isEmpty()) {
            System.out.println("Document is empty / wrong path.");
            return;
        }

        InvertedIndex invertedIndex = buildInvertedIndex(documents, totalDocs);

        // Load semua query dari query.txt → Map<QueryID, teks query>
        Map<String, Integer> queryLookup = loadQueryLookup(QUERY_FILE);
        System.out.println("Query lookup loaded: " + queryLookup.size() + " queries.");

        run(invertedIndex, sc, queryLookup);
    }

    /**
     * Membaca query.txt dan membangun Map<teks query lowercase, queryID>.
     * Digunakan untuk exact match saat user mengetik query.
     */
    private static Map<String, Integer> loadQueryLookup(String queryFilePath) {
        Map<String, Integer> lookup = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(
                new java.io.InputStreamReader(
                    new java.io.FileInputStream(queryFilePath),
                    java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("\r", "").trim();
                line = line.replaceAll("\\s+", " ");
                if (line.isEmpty()) continue;
                // Format: "ID\tteks query" atau "ID spasi teks query"
                int tabIndex = line.indexOf('\t');
                if (tabIndex < 0) tabIndex = line.indexOf(' ');
                if (tabIndex < 0) continue;
                int qid      = Integer.parseInt(line.substring(0, tabIndex).trim());
                String qtext = line.substring(tabIndex + 1).trim().toLowerCase();
                qtext = qtext.replaceAll("\\s+", " ");
                lookup.put(qtext, qid);
            }
            if (!lookup.isEmpty()) {
                System.out.println("Contoh query: " + lookup.entrySet().iterator().next());
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("[Warning] Gagal membaca query.txt: " + e.getMessage());
        }
        return lookup;
    }

    private static int numberDocument(Scanner sc) {
        System.out.print("Enter total documents indexed in dataset (max 1400): ");
        int totalDocs = sc.nextInt();
        sc.nextLine();
        return totalDocs;
    }

    private static Map<Integer, String> readDocuments(String folderPath, int totalDocs) {
        System.out.println("Reading document from " + folderPath + " folder...");
        DocumentReader reader = new DocumentReader(folderPath);
        return reader.readAll(totalDocs);
    }

    private static InvertedIndex buildInvertedIndex(Map<Integer, String> documents, int totalDocs) {
        System.out.println("Building Inverted Index...");
        TextPreprocessor preprocessor = new TextPreprocessor();
        InvertedIndex invertedIndex = new InvertedIndex();

        for (int docID = 1; docID <= totalDocs; docID++) {
            if (!documents.containsKey(docID)) continue;
            String content = documents.get(docID);

            // Mengambil dan menyimpan raw terms (tanpa stemming) ke vocabulary
            // List<String> rawTerms = preprocessor.getRawTerms(content);
            // for (String raw : rawTerms) {
            //     invertedIndex.addRawTerm(raw);
            // }

            List<String> terms = preprocessor.process(content);
            invertedIndex.setDocumentLength(docID, terms.size());
            for (String term : terms) invertedIndex.addDocument(term, docID);
        }

        invertedIndex.computeAverageDocumentLength();
        invertedIndex.assignSkipPointer();
        System.out.println("Inverted index has been built successfully.");
        return invertedIndex;
    }

    private static void run(InvertedIndex invertedIndex, Scanner sc, Map<String, Integer> queryLookup) {
        BIM bim = new BIM();
        bim.setInvertedIndex(invertedIndex);

        BM bm11 = new BM(1.5, 1);
        bm11.setInvertedIndex(invertedIndex);

        BM bm25 = new BM(1.5, 0.75);
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

            if (input.equalsIgnoreCase("exit")) break;
            if (input.isEmpty()) continue;

            List<String> queryTerms = preprocessor.process(input);

            // Jalankan semua model
            Map<Integer, Double> resultBIM  = prf.PRFWithBIM(queryTerms, 10, bim);
            Map<Integer, Double> result2PM  = prf.PRFWith2PM(queryTerms, 10, twoPoisson);
            Map<Integer, Double> resultBM11 = prf.PRFWithBM11(queryTerms, 10, bm11);
            Map<Integer, Double> resultBM25 = prf.PRFWithBM25(queryTerms, 10, bm25);

            // Cari Query ID dan ground truth
            String normalizedInput = input.replace("\r", "").trim().toLowerCase().replaceAll("\\s+", " ");
            Integer queryID = queryLookup.get(normalizedInput);
            Set<Integer> groundTruth = (queryID != null)
                ? Evaluation.loadGroundTruth(RES_FOLDER, queryID)
                : new HashSet<>();

            // Tampilkan ranking + evaluasi per model
            System.out.println("\n--- BIM ---");
            printResultWithEval(resultBIM, groundTruth);

            System.out.println("\n--- Two Poisson ---");
            printResultWithEval(result2PM, groundTruth);

            System.out.println("\n--- BM11 ---");
            printResultWithEval(resultBM11, groundTruth);

            System.out.println("\n--- BM25 ---");
            printResultWithEval(resultBM25, groundTruth);

            if (queryID == null) {
                System.out.println("\n[Info] Query tidak ditemukan di query.txt — evaluasi dilewati.");
            }
        }
    }

    private static void printResult(Map<Integer, Double> result) {
        if (result == null || result.isEmpty()) {
            System.out.println("--> There is no relevant document.");
            return;
        }
        int rank = 1;
        for (Map.Entry<Integer, Double> entry : result.entrySet()) {
            System.out.printf("Rank %d | DocID: %d | Score: %.4f%n",
                    rank++, entry.getKey(), entry.getValue());
            if (rank > 10) break;
        }
    }

    private static void printResultWithEval(Map<Integer, Double> result, Set<Integer> groundTruth) {
        if (result == null || result.isEmpty()) {
            System.out.println("--> There is no relevant document.");
            return;
        }
        int rank = 1;
        for (Map.Entry<Integer, Double> entry : result.entrySet()) {
            System.out.printf("Rank %d | DocID: %d | Score: %.4f%n",
                    rank++, entry.getKey(), entry.getValue());
            if (rank > 10) break;
        }
        if (!groundTruth.isEmpty()) {
            List<Integer> ranked = Evaluation.toRankedList(result);
            double p  = Evaluation.precision(ranked, groundTruth, 10);
            double r  = Evaluation.recall(ranked, groundTruth, 10);
            double f1 = Evaluation.f1Score(p, r);
            System.out.printf("Precision at 10: %.4f | Recall at 10: %.4f | F1 at 10: %.4f%n", p, r, f1);
        }
        
    }
}