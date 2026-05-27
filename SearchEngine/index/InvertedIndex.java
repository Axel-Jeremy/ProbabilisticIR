package index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Kelas InvertedIndex merepresentasikan struktur data yang
 * digunakan dalam sistem Information Retrieval untuk memetakan sebuah kata 
 * (term) ke daftar dokumen (posting list) yang mengandung kata tersebut.
 * Kelas ini juga mendukung penambahan skip pointer untuk mempercepat proses pencarian.
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM
 * 
 * @author Axel, Keane, Alex
 */

public class InvertedIndex {

    /**
     * Map yang menyimpan struktur utama inverted index.
     * Key berupa kata (String) dan Value berupa list of PostingNode.
     */
    private Map<String, List<PostingNode>> postingList;

    /**
     * Set yang menyimpan kosa kata asli (raw vocabulary) sebelum diproses,
     */
    private Set<String> rawVocabulary = new HashSet<>();

    /**
     * Menyimpan ID dokumen tertinggi yang pernah dimasukkan ke dalam indeks.
     */
    private int maxDocID;

    /**
     * Default constructor
     */
    public InvertedIndex() {
        this.postingList = new HashMap<>();
        maxDocID = 0;
    }

    /**
     * Mengambil daftar posting (posting list) untuk sebuah term tertentu.
     * 
     * @param term kata yang ingin dicari posting list-nya.
     * @return List yang berisi objek PostingNode. Jika kata tidak ditemukan, 
     *         mengembalikan arraylist kosong.
     */
    public List<PostingNode> getPostingList(String term) {
        return postingList.getOrDefault(term, new ArrayList<>());
    }

    /**
     * Mengambil ID dokumen terbesar (maksimal) yang tersimpan di dalam indeks.
     * 
     * @return Nilai integer dari ID dokumen terbesar.
     */
    public int getMaxDocID() {
        return maxDocID;
    }

    /**
     * Mengambil semua terms yang telah dimasukkan ke indeks.
     * 
     * @return Set berupa kumpulan string term yang menjadi key pada postingList.
     */
    public Set<String> getAllTerms() {
        return postingList.keySet();
    }

    /**
     * Mengecek apakah sebuah dokumen ada di dalam posting list suatu term.
     * 
     * @param term Kata yang dicari.
     * @param targetDocId ID dokumen yang ingin dicek.
     * @return true jika dokumen sudah ada di dalam posting list kata tersebut, 
     *         false jika sebaliknya.
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
     * Menambahkan kata mentah (raw term) ke dalam kumpulan kosa kata.
     * 
     * @param rawTerm Kata mentah yang akan ditambahkan.
     */
    public void addRawTerm(String rawTerm) {
        rawVocabulary.add(rawTerm.toLowerCase()); //case folding sebelum masukin raw term
    }

    /**
     * Mengambil seluruh himpunan kosa kata mentah (raw vocabulary).
     * 
     * @return Set yang berisi raw terms.
     */
    public Set<String> getRawVocabulary() {
        return rawVocabulary;
    }

    /**
     * Menambahkan dokumen (berdasarkan ID) ke dalam posting list suatu kata.
     * Jika kata belum ada di indeks, akan dibuatkan list baru. Jika sudah ada, 
     * node baru akan ditautkan di akhir list.
     * 
     * @param term Kata dari dokumen yang diindeks.
     * @param docID ID dari dokumen yang mengandung kata tersebut.
     */
    public void addDocument(String term, int docID) {
        if (!postingList.containsKey(term)) {
            List<PostingNode> newList = new LinkedList<>();
            newList.add(new PostingNode(docID));
            postingList.put(term, newList);
        } else {
            if (!isDocIdExist(term, docID)) {
                PostingNode newNode = new PostingNode(docID);
                postingList.get(term).getLast().setNext(newNode);
                postingList.get(term).add(newNode);
            }
        }
        // Perbarui nilai maxDocID jika docID baru lebih besar
        maxDocID = Math.max(maxDocID, docID);
    }

    /**
     * Set skip pointer pada setiap node di semua posting list.
     * Skip pointer berguna untuk mengoptimalkan proses irisan (intersection) 
     * saat memproses kueri. Jarak lompatan dihitung menggunakan akar kuadrat 
     * dari panjang posting list.
     */
    public void assignSkipPointer() {
        for (String term : postingList.keySet()) {
            // Hitung panjang posting list term
            int length = postingList.get(term).size();

            if (length < 3)
                continue; // skip pointer tidak berguna untuk list pendek

            // Rumus standar jarak skip
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

}
