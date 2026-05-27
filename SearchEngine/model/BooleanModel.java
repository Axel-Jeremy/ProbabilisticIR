package model;

import java.util.ArrayList;
import java.util.List;

import index.*;
/**
 * Kelas BooleanModel bertanggung jawab untuk menangani eksekusi operasi logika 
 * Boolean (AND, OR, NOT) pada himpunan posting list. 
 * Kelas ini mengimplementasikan algoritma penggabungan dan irisan yang efisien, 
 * termasuk pemanfaatan skip pointer untuk mempercepat pencarian.
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM
 * 
 * @author Axel, Alex, Keane
 */
public class BooleanModel {

    /**
     * Referensi static ke struktur InvertedIndex.
     */
    private static InvertedIndex invertedIndex;

    /**
     * Menyimpan nilai ID dokumen maksimal (terbesar) yang ada di dalam koleksi.
     * penting untuk menjalankan operasi negasi.
     */
    private static int maxDocID;

    /**
     * Default Constructor
     */
    public BooleanModel() {
    }

    /**
     * Menetapkan nilai ID dokumen maksimum pada koleksi dokumen saat ini.
     * 
     * * @param maxDocID Nilai integer dari ID dokumen terbesar.
     */
    public void setMaxDocID(int maxDocID) {
        BooleanModel.maxDocID = maxDocID;
    }

    /**
     * Melakukan operasi irisan (AND) antara dua posting list.
     * Proses ini akan mencari dokumen mana saja yang mengandung KEDUA term sekaligus.
     * Menggunakan skip pointer untuk melompati bagian list 
     * yang tidak relevan, sehingga mempercepat proses komputasi.
     * 
     * @param p1 Node pertama dari posting list term A.
     * @param p2 Node pertama dari posting list term B.
     * 
     * @return List hasil yang berisi node dokumen yang ada di p1 dan p2.
     */
    public List<PostingNode> intersect(PostingNode p1, PostingNode p2) {
        List<PostingNode> answer = new ArrayList<>();

        while (p1 != null && p2 != null) {
            int doc1 = p1.getDocID();
            int doc2 = p2.getDocID();

            if (doc1 == doc2) {
                answer.add(new PostingNode(p1.getDocID()));
                p1 = p1.getNext();
                p2 = p2.getNext();
            } else if (doc1 < doc2) {
                if (p1.getSkip() != null && p1.getSkip().getDocID() <= p2.getDocID()) {
                    p1 = p1.getSkip();
                } else {
                    p1 = p1.getNext();
                }
            } else {
                if (p2.getSkip() != null && p2.getSkip().getDocID() <= p1.getDocID()) {
                    p2 = p2.getSkip();
                } else {
                    p2 = p2.getNext();
                }
            }
        }
        return answer;
    }

    /**
     * Melakukan operasi gabungan (OR) antara dua posting list.
     * Proses ini akan menggabungkan semua dokumen yang 
     * mengandung minimal 1 dari kedua term tersebut.
     * 
     * @param p1 Node pertama dari posting list term A.
     * @param p2 Node pertama dari posting list term B.
     * 
     * @return List hasil yang merupakan gabungan terurut dari p1 dan p2.
     */
    public List<PostingNode> union(PostingNode p1, PostingNode p2) {
        List<PostingNode> answer = new ArrayList<>();

        while (p1 != null && p2 != null) {
            int doc1 = p1.getDocID();
            int doc2 = p2.getDocID();

            if (doc1 == doc2) {
                answer.add(new PostingNode(p1.getDocID()));
                p1 = p1.getNext();
                p2 = p2.getNext();
            } else if (doc1 < doc2) {
                answer.add(new PostingNode(p1.getDocID()));
                p1 = p1.getNext();
            } else {
                answer.add(new PostingNode(p2.getDocID()));
                p2 = p2.getNext();
            }
        }
        while (p1 != null) {
            answer.add(new PostingNode(p1.getDocID()));
            p1 = p1.getNext();
        }
        while (p2 != null) {
            answer.add(new PostingNode(p2.getDocID()));
            p2 = p2.getNext();
        }

        return answer;
    }


    /**
     * Melakukan operasi negasi (NOT) terhadap sebuah posting list.
     * Operasi ini akan mengembalikan daftar seluruh dokumen di dalam koleksi 
     * yang tidak mengandung term tersebut.
     * 
     * @param p1 Node pertama dari posting list term yang dinegasikan.
     * @return List dokumen komplemen yang tidak memiliki term tersebut.
     */
    public List<PostingNode> negate(PostingNode p1) {
        List<PostingNode> result = new ArrayList<>();

        int j = p1.getDocID();
        for (int i = 1; i <= maxDocID; i++) {
            if (i != j)
                result.add(new PostingNode(i));
            if (i == j) {
                p1 = p1.getNext();
                if (p1 != null) {
                    j = p1.getDocID();
                } else {
                    j = maxDocID + 1;
                }
            }
        }
        return result;
    }

    /**
     * Menetapkan referensi InvertedIndex yang akan digunakan untuk mengambil 
     * posting list pada operasi jamak (multiple terms).
     * 
     * * @param invertedIndex Objek InvertedIndex yang aktif.
     */
    public void setInvertedIndex(InvertedIndex invertedIndex) {
        BooleanModel.invertedIndex = invertedIndex;
    }

    /**
     * Menautkan ulang referensi (pointer) 'next' dan 'skip' pada sebuah posting list baru.
     * Hal ini diperlukan karena operasi logika (seperti intersect atau union) menghasilkan 
     * objek node baru yang belum memiliki struktur keterhubungan pointer.
     * 
     * @param nodes List dari PostingNode yang baru saja di-generate.
     * 
     * @return List yang sama, namun setiap node di dalamnya sudah terhubung dengan 
     * pointer 'next' dan 'skip'.
     */
    public List<PostingNode> assignPointer(List<PostingNode> nodes) {
        if (nodes == null || nodes.isEmpty())
            return nodes;

        int n = nodes.size();
        int skipInterval = (int) Math.sqrt(n);

        for (int i = 0; i < n - 1; i++) {
            nodes.get(i).setNext(nodes.get(i + 1));
        }
        nodes.get(n - 1).setNext(null);

        for (int i = 0; i < n; i++) {
            int skipTarget = i + skipInterval;
            if (skipTarget < n) {
                nodes.get(i).setSkip(nodes.get(skipTarget));
            } else {
                nodes.get(i).setSkip(null);
            }
        }

        return nodes;
    }

    /**
     * Melakukan irisan (AND) beberapa term sekaligus.
     * 
     * @param terms List string berisi beberapa term yang ingin dicari irisannya.
     * 
     * @return List dari PostingNode hasil irisan seluruh term, atau list kosong jika tidak ada irisan.
     */
    public List<PostingNode> intersects(List<String> terms) {
        terms.sort((a, b) -> invertedIndex.getPostingList(a).size() - invertedIndex.getPostingList(b).size());

        List<PostingNode> res = invertedIndex.getPostingList(terms.removeFirst());
        if (res.isEmpty())
            return new ArrayList<>();

        while (!terms.isEmpty()) {
            if (res.isEmpty())
                return new ArrayList<>();
            List<PostingNode> next = invertedIndex.getPostingList(terms.removeFirst());
            if (next.isEmpty())
                return new ArrayList<>();
            res = intersect(res.getFirst(), next.getFirst());
        }
        return res;
    }

    /**
     * Melakukan gabungan (OR) beberapa term sekaligus.
     * 
     * @param terms List string berisi beberapa term yang ingin digabungkan.
     * 
     * @return List dari PostingNode hasil gabungan seluruh term.
     */
    public List<PostingNode> unions(List<String> terms) {
        List<PostingNode> res = invertedIndex.getPostingList(terms.removeFirst());
        while (!terms.isEmpty()) {
            if (res.isEmpty())
                return new ArrayList<>();
            List<PostingNode> next = invertedIndex.getPostingList(terms.removeFirst());
            if (next.isEmpty())
                return new ArrayList<>();
            res = union(res.getFirst(), next.getFirst());
        }
        return res;
    }
}