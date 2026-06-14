package index;

/**
 * Kelas PostingNode merepresentasikan sebuah node (simpul) di dalam sebuah posting list.
 * Struktur data ini digunakan pada Inverted Index dalam sistem temu kembali 
 * informasi (Information Retrieval) untuk menyimpan daftar dokumen yang mengandung 
 * suatu kata (term) tertentu. Kelas ini juga mendukung implementasi skip pointer
 * untuk mempercepat proses pencarian atau irisan (intersection).
 * 
 * Sumber: Membuat sendiri
 * 
 * @author Axel 
 * 
 */
public class PostingNode {
    /**
     * ID dokumen tempat suatu kata ditemukan.
     */
    private int docID;

    /**
     * frekuensi kemunculan term, nilainya selalu 1 saat node pertama kali dibuat
     */
    private int termFrequency;

    /**
     * Pointer yang menunjuk ke node berikutnya dalam posting list.
     */
    private PostingNode next;

    /**
     * Pointer lompatan (skip pointer) untuk mempercepat iterasi pencarian.
     */
    private PostingNode skip;

    /**
     * Constructor
     * Secara default, pointer 'next' dan 'skip' akan diinisialisasi dengan nilai null.
     * 
     * @param docID ID unik dari dokumen yang akan disimpan dalam node ini.
     */
    public PostingNode(int docID) {
        this.docID = docID;
        this.termFrequency = 1;
        this.next = null;
        this.skip = null;
    }

    public int getDocID() {
        return docID;
    }

    public void setDocID(int docID) {
        this.docID = docID;
    }

    public PostingNode getNext() {
        return next;
    }

    public void setNext(PostingNode next) {
        this.next = next;
    }

    public PostingNode getSkip() {
        return skip;
    }

    public void setSkip(PostingNode skip) {
        this.skip = skip;
    }
    
    public int getTermFrequency() {
        return termFrequency;
    }

    public void incrementTermFrequency() {
        this.termFrequency++;
    }
}