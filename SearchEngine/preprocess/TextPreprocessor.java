package preprocess;

import java.util.ArrayList;
import java.util.List;

/**
 * Kelas TextPreprocessor bertindak sebagai wrapper dalam tahapan 
 * \text preprocessing. Kelas ini menggabungkan fungsionalitas dari 
 * kelas Tokenizer (pemecahan kata dan case folding) dan kelas Stemmer (pemotongan 
 * imbuhan) untuk menghasilkan daftar kata dasar yang siap untuk diindeks.
 * 
 * Sumber: Membuat sendiri
 * 
 * @author Axel
 */
public class TextPreprocessor {

    /**
     * Objek yang digunakan untuk mengubah kata menjadi bentuk dasarnya (root word).
     */
    private Stemmer stemmer;

    /**
     * Objek yang digunakan untuk memecah teks utuh menjadi token atau daftar kata tunggal.
     */
    private Tokenizer tokenizer;

    /**
     * Konstruktor menginisialisasi objek TextPreprocessor.
     * Secara otomatis juga akan membuat instance baru untuk objek stemmer dan tokenizer.
     */
    public TextPreprocessor() {
        this.stemmer = new Stemmer();
        this.tokenizer = new Tokenizer();
    }

    /**
     * Method utama untuk memproses sebuah string teks secara menyeluruh.
     * Alur proses:
     * - Tokenisasi teks mentah
     * - Stemming hasil tokenisasi
     * 
     * @param text Teks string mentah yang ingin diproses.
     * @return List berupa kata-kata (String) yang sudah dilakukan tokenisasi dan stemming.
     */
    public List<String> process(String text) {
        List<String> result = tokenize(text);
        result = stem(result);
        return result;
    }
    
    /**
     * Melakukan proses Porter Stemming pada kumpulan daftar term.
     * 
     * @param terms List kata-kata yang sudah ditokenisasi.
     * @return List kata-kata dasar (root words) hasil stemming.
     */
    private List<String> stem(List<String> terms) {
        List<String> res = new ArrayList<>();
        for (String term : terms) {
            String stemmedWord = stemmer.porterStemmer(term);
            if (stemmedWord != null)
                res.add(stemmedWord);
        }
        return res;
    }

    /**
     * Memanggil modul Tokenizer untuk memecah teks input menjadi token.
     * 
     * @param tokens Teks (berupa string utuh) yang akan dipecah menjadi kata-kata.
     * @return List berisi token (kata-kata) yang sudah dipisah dan di-case-fold.
     */
    private List<String> tokenize(String tokens) {
        List<String> res = new ArrayList<>();
        List<String> terms = tokenizer.process(tokens);
        if (terms == null)
            return res;
        while (!terms.isEmpty()) {
            res.add(terms.removeFirst());
        }
        return res;
    }
}
