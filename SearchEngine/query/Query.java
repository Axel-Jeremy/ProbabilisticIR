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
    private BooleanModel model;

    /**
     * Modul untuk menangani toleransi kesalahan ketik (spell correction).
     */
    private static TolerantModel tolerant;

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
        this.query = query.trim().toLowerCase();
        this.preprocessor = new TextPreprocessor();
        this.model = new BooleanModel();
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
     * Menetapkan objek TolerantRetrieval yang akan digunakan untuk koreksi ejaan
     * kueri.
     * Pengaturan bersifat statik sehingga berlaku untuk semua instance Query.
     * 
     * @param tolerant Objek TolerantRetrieval.
     */
    public void setTolerantModel(TolerantModel tolerant) {
        Query.tolerant = tolerant;
    }

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
        List<String> postfix = shuntingYard(tokens);
        return evaluate(postfix);
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

    /**
     * Mengonversi kueri dari notasi infix (contoh: A AND B) menjadi notasi postfix
     * (contoh: A B AND) menggunakan algoritma Shunting Yard.
     * Pada tahap ini, token yang bukan operator juga akan diproses untuk koreksi
     * ejaan dan stemming sebelum dimasukkan ke dalam antrean hasil (output queue).
     * 
     * @param tokens List token kueri dalam format infix.
     * @return List token kueri yang sudah diubah ke format postfix.
     */
    private List<String> shuntingYard(List<String> tokens) {
        Queue<String> outputQueue = new LinkedList<>();
        Deque<String> operatorStack = new ArrayDeque<>();

        for (String raw : tokens) {
            String token = isOperator(raw) ? raw.toLowerCase() : raw;

            if (isOperator(token)) {
                while (!operatorStack.isEmpty()
                        && isOperator(operatorStack.peek())
                        && ORDER.get(operatorStack.peek()) >= ORDER.get(token)) {
                    outputQueue.add(operatorStack.pop());
                }
                operatorStack.push(token);

            } else if (token.equals("(")) {
                operatorStack.push(token);

            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    outputQueue.add(operatorStack.pop());
                }
                if (!operatorStack.isEmpty())
                    operatorStack.pop();

            } else {
                // Memproses token kata (koreksi typo dan stemming)
                String corrected = tolerant.correct(token);
                List<String> stemmed = preprocessor.process(corrected);
                for (String s : stemmed)
                    outputQueue.add(s);
            }
        }

        while (!operatorStack.isEmpty()) {
            outputQueue.add(operatorStack.pop());
        }

        return new ArrayList<>(outputQueue);
    }

    /**
     * Mengevaluasi kueri boolean dalam format postfix menggunakan struktur data stack.
     * Menggabungkan, mengiris, negasi posting list dokumen berdasarkan operator logika yang ada.
     * 
     * @param postfix List token kueri dalam bentuk postfix.
     * @return List dari PostingNode yang merupakan hasil komputasi keseluruhan kueri.
     */
    private List<PostingNode> evaluate(List<String> postfix) {
        Deque<List<PostingNode>> resultStack = new ArrayDeque<>();

        for (String token : postfix) {
            if (token.equals("not")) {
                if (resultStack.isEmpty())
                    return new ArrayList<>();
                List<PostingNode> operand = resultStack.pop();
                if (operand.isEmpty())
                    return new ArrayList<>();
                resultStack.push(assignPointer(model.negate(operand.getFirst())));

            } else if (token.equals("and")) {
                if (resultStack.size() < 2)
                    return new ArrayList<>();
                List<PostingNode> right = resultStack.pop();
                List<PostingNode> left = resultStack.pop();
                if (right.isEmpty() || left.isEmpty()) {
                    resultStack.push(new ArrayList<>());
                } else {
                    resultStack.push(assignPointer(
                            model.intersect(left.getFirst(), right.getFirst())));
                }

            } else if (token.equals("or")) {
                if (resultStack.size() < 2)
                    return new ArrayList<>();
                List<PostingNode> right = resultStack.pop();
                List<PostingNode> left = resultStack.pop();
                if (right.isEmpty() && left.isEmpty()) {
                    resultStack.push(new ArrayList<>());
                } else if (right.isEmpty()) {
                    resultStack.push(left);
                } else if (left.isEmpty()) {
                    resultStack.push(right);
                } else {
                    resultStack.push(assignPointer(
                            model.union(left.getFirst(), right.getFirst())));
                }

            } else {
                List<PostingNode> posting = invertedIndex.getPostingList(token);
                resultStack.push(posting != null ? posting : new ArrayList<>());
            }
        }

        return resultStack.isEmpty() ? new ArrayList<>() : resultStack.pop();
    }

    /**
     * Memperbarui kembali referensi (skip pointer) pada list hasil sementara 
     * setiap kali selesai melakukan sebuah operasi logika.
     * 
     * @param nodes List PostingNode yang strukturnya ingin disematkan skip pointer.
     * @return List PostingNode yang sama dengan skip pointer yang sudah ter-update.
     */
    private List<PostingNode> assignPointer(List<PostingNode> nodes) {
        return model.assignPointer(nodes);
    }
}