package query;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import index.*;
import model.*;
import preprocess.TextPreprocessor;
/**
 * Kelas Query bertanggung jawab untuk memproses kueri pencarian boolean dari pengguna.
 * Kelas ini menangani proses tokenisasi kueri, pengubahan struktur kueri dari infix
 * menjadi postfix menggunakan algoritma Shunting Yard, serta evaluasi kueri tersebut
 * terhadap Inverted Index untuk mendapatkan daftar dokumen yang relevan.
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM,
 * https://www.geeksforgeeks.org/java/java-program-to-implement-shunting-yard-algorithm/
 * https://www.youtube.com/watch?v=Wz85Hiwi5MY
 * https://www.youtube.com/watch?v=bebqXO8H4eA
 * 
 * @author Axel, Alex, Keane
 * 
 */
public class Query {

    /**
     * Teks kueri mentah yang dimasukkan oleh pengguna.
     */
    private String query;

    /**
     * Objek untuk preprocess teks (tokenizing dan stemming).
     */
    private TextPreprocessor preprocessor;

    /**
     * Model boolean yang menangani operasi logika.
     */
    // private BooleanModel model;

    // /**
    //  * Modul untuk menangani toleransi kesalahan ketik (spell correction).
    //  */
    // private static TolerantModel tolerant;

    /**
     * Referensi statik Inverted Index tempat pencarian dilakukan.
     */
    private static InvertedIndex invertedIndex;

    /**
     * Map yang menyimpan urutan prioritas dari operator boolean.
     * Semakin besar nilainya, semakin tinggi prioritasnya (NOT > AND > OR).
     */
    private static final Map<String, Integer> ORDER = new HashMap<>();
    static {
        ORDER.put("or", 1);
        ORDER.put("and", 2);
        ORDER.put("not", 3);
    }

    /**
     * Konstruktor untuk membuat objek Query baru.
     * Menginisialisasi kueri dengan menghapus spasi ekstra dan mengubahnya ke huruf
     * kecil, serta menyiapkan modul-modul pendukung.
     * 
     * @param query String kueri boolean yang dimasukkan pengguna.
     */
    public Query(String query) {

    }

    /**
     * Menetapkan objek BooleanModel yang akan digunakan untuk operasi logika.
     * 
     * @param model Objek BooleanModel.
     */
    // public void setModel(BooleanModel model) {
    // this.model = model;
    // }

    /**
     * Menetapkan InvertedIndex yang menjadi basis data pencarian dokumen.
     * Pengaturan bersifat statik sehingga berlaku untuk semua instance Query.
     * 
     * @param invertedIndex Objek InvertedIndex yang sudah berisi indeks kata.
     */
    public void setInvertedIndex(InvertedIndex invertedIndex) {
        Query.invertedIndex = invertedIndex;
    }

    /**
     * Mengecek apakah sebuah string adalah operator boolean (AND, OR, NOT).
     * 
     * @param kata String token yang akan dicek.
     * @return true jika token merupakan operator terdaftar, false jika sebaliknya.
     */
    public boolean isOperator(String kata) {
        return ORDER.containsKey(kata.toLowerCase());
    }

    /**
     * Alur utama untuk mengeksekusi kueri.
     * Langkah-langkahnya:
     * - split token
     * - konversi ke postfix
     * - evaluasi hasil.
     * 
     * @return List objek PostingNode yang merepresentasikan dokumen hasil
     *         pencarian.
     */
    public List<PostingNode> process() {
        List<String> tokens = splitQuery();
        // List<String> postfix = shuntingYard(tokens);
        // return evaluate(postfix);
        return null;
    }

    /**
     * Memisahkan teks kueri mentah menjadi sekumpulan token terpisah.
     * Metode ini memperhatikan spasi dan memisahkan secara eksplisit tanda 
     * kurung "(" dan ")".
     * 
     * @return List berisi token-token kueri yang sudah terpisah (termasuk kurung 
     *         dan operator).
     */
    public List<String> splitQuery() {
        List<String> token = new ArrayList<>();
        String temp = "";

        for (char c : query.toCharArray()) {
            if (c == '(' || c == ')') {
                if (temp.length() > 0) {
                    token.add(temp);
                    temp = "";
                }

                token.add(c + "");
            }

            else if (c == ' ') {
                if (temp.length() > 0) {
                    token.add(temp);
                    temp = "";
                }
            }

            else {
                temp = temp + c;
            }
        }
        if (temp.length() > 0) {
            token.add(temp);
        }

        return token;
    }

        // /**
    //  * Menetapkan objek TolerantRetrieval yang akan digunakan untuk koreksi ejaan
    //  * kueri.
    //  * Pengaturan bersifat statik sehingga berlaku untuk semua instance Query.
    //  * 
    //  * @param tolerant Objek TolerantRetrieval.
    //  */
    // public void setTolerantModel(TolerantModel tolerant) {
    //     Query.tolerant = tolerant;
    // }


    /**
     * Memperbarui kembali referensi (skip pointer) pada list hasil sementara 
     * setiap kali selesai melakukan sebuah operasi logika.
     * 
     * @param nodes List PostingNode yang strukturnya ingin disematkan skip pointer.
     * @return List PostingNode yang sama dengan skip pointer yang sudah ter-update.
     */
    // private List<PostingNode> assignPointer(List<PostingNode> nodes) {
    //     return model.assignPointer(nodes);
    // }
}