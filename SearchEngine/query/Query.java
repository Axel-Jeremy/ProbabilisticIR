package query;

import java.util.List;

import preprocess.TextPreprocessor;

/**
 * Kelas Query merepresentasikan input pencarian (kueri) dari pengguna 
 * di dalam sistem Information Retrieval. 
 * Kelas ini bertugas untuk menyimpan kueri mentah dan memprosesnya 
 * menjadi daftar kata (term) yang siap dicocokkan dengan indeks dokumen.
 * 
 * Sumber: Membuat sendiri
 * 
 * @author Axel
 */
public class Query {

    /**
     * Teks kueri mentah (raw string) yang dimasukkan oleh pengguna.
     */
    private String query;

    /**
     * Objek pemroses teks yang digunakan untuk melakukan pra-pemrosesan (preprocessing) 
     * pada kueri mentah, melakukan tokenizing, stopword removal, dan stemming.
     */
    private TextPreprocessor preprocessor;

    /**
     * Konstruktor untuk membuat instance objek Query baru berdasarkan input pengguna.
     * * @param query String kueri mentah yang dimasukkan oleh pengguna.
     */
    public Query(String query) {
        this.query = query;
        this.preprocessor = new TextPreprocessor();
    }

    /**
     * Memproses teks kueri mentah menjadi daftar term (kata) yang sudah dibersihkan.
     * Fungsi ini akan meneruskan string kueri ke objek TextPreprocessor
     * 
     * * @return List yang berisi sekumpulan string (term) hasil preprocess.
     */
    public List<String> process() {
        return this.preprocessor.process(query);
    }
}