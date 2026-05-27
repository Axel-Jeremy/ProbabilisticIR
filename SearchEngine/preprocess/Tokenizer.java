package preprocess;

import java.util.ArrayList;
import java.util.List;

/**
 * Kelas Tokenizer berfungsi untuk memproses teks mentah (raw text) menjadi term.
 * Proses ini meliputi pembersihan karakter non-alfabet (seperti tanda baca dan angka), 
 * pemisahan kata, serta penyeragaman teks menjadi huruf kecil (case folding).
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM
 * 
 * @author Axel, Keane, Alex
 */
public class Tokenizer {

    /**
     * Konstruktor default untuk membuat objek Tokenizer.
     */
    public Tokenizer() {
    }

    /**
     * Method utama untuk memproses sebuah teks menjadi daftar token yang bersih.
     * Method ini akan menjalankan proses pemecahan kata (tokenisasi) secara 
     * berurutan, dilanjutkan dengan proses case folding.
     * 
     * @param text Teks mentah (String) yang ingin diproses.
     * @return List berisi term yang sudah dipecah, dibersihkan 
     *         dari tanda baca, dan diubah menjadi huruf kecil.
     */
    public List<String> process(String text) {
        List<String> tokens = tokenize(text);
        tokens = caseFolding(tokens);
        return tokens;
    }

    /**
     * Memecah teks utuh menjadi sekumpulan kata tunggal (token).
     * Pada tahap ini, semua karakter selain huruf alfabet akan dibuang, 
     * dan teks akan dipisahkan berdasarkan spasi atau karakter whitespace lainnya.
     * 
     * @param text Teks yang akan ditokenisasi.
     * @return Hasil list dari kata-kata yang hanya mengandung huruf alfabet.
     */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();

        // ngubah non alphabet jadi spasi
        String cleaned = text.replaceAll("[^a-zA-Z ]", ""); 

        //berguna untuk menghapus semua whitespace baik dari spasi, tab, ataupun newLine
        String[] words = cleaned.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    /**
     * Melakukan proses case folding, yaitu mengubah seluruh huruf di dalam 
     * token menjadi huruf kecil (lowercase). Hal ini penting dalam Information 
     * Retrieval agar pencarian tidak bersifat case-sensitive (contoh: "Buku" 
     * dianggap sama dengan "buku").
     * 
     * @param tokens List kata-kata yang masih memiliki kombinasi huruf besar-kecil.
     * @return List hasil kata yang seluruh hurufnya sudah menjadi huruf kecil.
     */
    private List<String> caseFolding(List<String> tokens) {
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            result.add(token.toLowerCase());
        }
        return result;
    }
}
