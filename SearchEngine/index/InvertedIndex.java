package index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kelas InvertedIndex merepresentasikan struktur data yang
 * digunakan dalam sistem Information Retrieval untuk memetakan sebuah kata 
 * (term) ke daftar dokumen (posting list) yang mengandung kata tersebut.
 * 
 * Kelas ini juga mendukung penambahan skip pointer untuk mempercepat proses pencarian,
 * serta menyimpan metrik tambahan untuk perhitungan skor seperti TF-IDF atau BM25.
 * 
 * Sumber: Dibuat sendiri dengan bantuan LLM
 * 
 * @author Axel, Keane, Alex
 */
public class InvertedIndex {

    /**
     * Map yang menyimpan struktur utama inverted index.
     * Key berupa kata, value berupa list dari PostingNode.
     */
    private Map<String, List<PostingNode>> postingList;

    /**
     * Map yang menyimpan document frequency (DF) untuk setiap kata.
     * Key berupa kata dan value berupa jumlah dokumen yang mengandung kata tersebut.
     */
    private Map<String, Integer> postingLength;

    /**
     * Map yang menyimpan panjang dari setiap dokumen (jumlah kata dalam dokumen tersebut).
     * Key berupa ID dokumen (Integer) dan Value berupa panjang dokumen (Integer).
     */
    private Map<Integer, Integer> documentLengths;

    /**
     * Rata-rata panjang dokumen dalam seluruh koleksi (avgdl).
     */
    private double avgDocLength;

    /**
     * Total keseluruhan kata (terms) yang terdapat di dalam seluruh dokumen koleksi.
     */
    private long totalTermsInCollection;

    /**
     * Set yang menyimpan kosa kata asli (raw vocabulary) sebelum diproses.
     */
    // private Set<String> rawVocabulary = new HashSet<>();

    /**
     * Menyimpan ID dokumen tertinggi yang pernah dimasukkan ke dalam indeks.
     */
    private int maxDocID;

    /**
     * Konstruktor bawaan (default) untuk menginisialisasi InvertedIndex.
     */
    public InvertedIndex() {
        this.postingList = new HashMap<>();
        this.postingLength = new HashMap<>();
        this.documentLengths = new HashMap<>();
        this.maxDocID = 0;
        this.totalTermsInCollection = 0;
        this.avgDocLength = 0.0;
    }

    /**
     * Mengambil daftar posting (posting list) untuk sebuah term tertentu.
     * @param term Kata yang ingin dicari posting list-nya.
     * 
     * @return List yang berisi objek PostingNode. Jika kata tidak ditemukan, 
     * akan mengembalikan ArrayList kosong.
     */
    public List<PostingNode> getPostingList(String term) {
        return postingList.getOrDefault(term, new ArrayList<>());
    }

    /**
     * Mengambil ID dokumen terbesar (maksimal) yang tersimpan di dalam indeks.
     * * @return Nilai integer dari ID dokumen terbesar.
     */
    public int getMaxDocID() {
        return maxDocID;
    }

    /**
     * Mengambil semua terms (kata) yang telah dimasukkan ke dalam indeks.
     * * @return Set berupa kumpulan string term yang menjadi key pada postingList.
     */
    public Set<String> getAllTerms() {
        return postingList.keySet();
    }

    /**
     * Mengecek apakah sebuah dokumen ada di dalam posting list suatu term.
     * @param term Kata yang dicari.
     * @param targetDocId ID dokumen yang ingin dicek.
     * @return true jika dokumen sudah ada di dalam posting list kata tersebut, 
     * false jika sebaliknya.
     */
    public boolean isDocIdExist(String term, int targetDocId) {
        List<PostingNode> nodes = postingList.get(term);

        if (nodes == null)
            return false;

        for (PostingNode node : nodes) {
            if (node.getDocID() == targetDocId)
                return true;
        }
        return false;
    }

    /**
     * Mengambil nilai Document Frequency (DF) untuk sebuah term.
     * @param text Term atau kata yang ingin dicari DF-nya.
     * 
     * @return Jumlah dokumen yang mengandung kata tersebut. Mengembalikan 0 jika term tidak ada.
     */
    public int getDfByTerm(String text) {
        return postingLength.getOrDefault(text, 0);
    }

    /**
     * Menyimpan informasi panjang sebuah dokumen dan menambahkan jumlah kata tersebut 
     * ke dalam total kata keseluruhan koleksi.
     * 
     * @param docID ID dari dokumen yang sedang diproses.
     * @param length Jumlah kata (panjang) dari dokumen tersebut.
     */
    public void setDocumentLength(int docID, int length) {
        documentLengths.put(docID, length);
        totalTermsInCollection += length;
    }

    /**
     * Menghitung rata-rata panjang dokumen dari seluruh koleksi yang ada.
     * Method ini idealnya dipanggil setelah semua dokumen selesai diindeks.
     */
    public void computeAverageDocumentLength() {
        if (!documentLengths.isEmpty()) {
            this.avgDocLength = (double) totalTermsInCollection / getTotalDocuments();
        }
    }

    /**
     * Mengambil panjang (jumlah kata) dari sebuah dokumen berdasarkan ID-nya.
     * @param docID ID dokumen yang ingin dicari panjangnya.
     * 
     * @return Panjang dokumen tersebut, atau 0 jika dokumen tidak ditemukan.
     */
    public int getDocumentLength(int docID) {
        return documentLengths.getOrDefault(docID, 0);
    }

    /**
     * Mengambil nilai rata-rata panjang dokumen (average document length) di dalam koleksi.
     * * @return Nilai rata-rata panjang dokumen dalam bentuk double.
     */
    public double getAverageDocumentLength() {
        return this.avgDocLength;
    }
    
    /**
     * Mengambil total jumlah dokumen (N) yang ada di dalam koleksi.
     * @return Jumlah keseluruhan dokumen.
     */
    public int getTotalDocuments() {
        return documentLengths.size(); // Total dokumen N
    }

    /**
     * Menambahkan dokumen (berdasarkan ID) ke dalam posting list suatu kata.
     * Jika kata belum ada di indeks, akan dibuatkan list baru. Jika sudah ada, 
     * Term Frequency (TF) akan ditambahkan, atau node baru akan ditautkan di akhir list.
     * 
     * * @param term Kata dari dokumen yang diindeks.
     * @param docID ID dari dokumen yang mengandung kata tersebut.
     */
    public void addDocument(String term, int docID) {
        if (!postingList.containsKey(term)) {
            List<PostingNode> newList = new LinkedList<>();
            newList.add(new PostingNode(docID));
            postingList.put(term, newList);
            postingLength.put(term, 1);

        } else {
            // Jika docID sudah ada, tambahkan nilai TF
            if (isDocIdExist(term, docID)) {
                for (PostingNode node : postingList.get(term)) {
                    if (node.getDocID() == docID) {
                        node.incrementTermFrequency();
                        break;
                    }
                }
            } 
            else {
                // Jika docID belum ada, buat posting node baru di akhir list
                PostingNode newNode = new PostingNode(docID);
                postingList.get(term).getLast().setNext(newNode);
                postingList.get(term).add(newNode);
                postingLength.replace(term, postingLength.get(term) + 1);
            }
        }
        // Perbarui nilai maxDocID jika docID baru lebih besar
        maxDocID = Math.max(maxDocID, docID);
    }

    /**
     * Menetapkan skip pointer pada setiap node di semua posting list.
     * Skip pointer berguna untuk mengoptimalkan proses irisan (intersection) 
     * saat memproses kueri pencarian. Jarak lompatan dihitung menggunakan akar kuadrat 
     * dari panjang posting list.
     */
    public void assignSkipPointer() {
        for (String term : postingList.keySet()) {
            // Hitung panjang posting list untuk term saat ini
            int length = postingList.get(term).size();

            if (length < 3)
                continue; //terlalu pendek

            // Rumus standar skip interval
            int skipInterval = (int) Math.sqrt(length);

            PostingNode[] nodes = new PostingNode[length];
            PostingNode current = postingList.get(term).getFirst();
            for (int i = 0; i < length; i++) {
                nodes[i] = current;
                current = current.getNext();
            }

            for (int i = 0; i + skipInterval < length; i += skipInterval) {
                nodes[i].setSkip(nodes[i + skipInterval]);
            }
        }
    }

    /**
     * Menampilkan daftar dokumen di posting list beserta nilai term frequency (TF)
     *
     * @param term Kata yang ingin dilihat detail posting list-nya.
     */
    public void printPostingWithTF(String term) {
        List<PostingNode> postings = getPostingList(term);

        if (postings.isEmpty()) {
            System.out.println("Term tidak ditemukan.");
            return;
        }

        System.out.println("Posting list untuk term: " + term);

        for (PostingNode node : postings) {
            System.out.println(
                    "DocID: " + node.getDocID() +
                    " | TF: " + node.getTermFrequency());
        }
    }
}


    // /**
    //  * Menambahkan kata mentah (raw term) ke dalam kumpulan kosa kata.
    //  * Kata akan diubah menjadi huruf kecil (case folding) terlebih dahulu.
    //  * * @param rawTerm Kata mentah yang akan ditambahkan.
    //  */
    // public void addRawTerm(String rawTerm) {
    //     rawVocabulary.add(rawTerm.toLowerCase()); // case folding sebelum memasukkan raw term
    // }

    // /**
    //  * Mengambil seluruh himpunan kosa kata mentah (raw vocabulary).
    //  * * @return Set yang berisi raw terms.
    //  */
    // public Set<String> getRawVocabulary() {
    //     return rawVocabulary;
    // }