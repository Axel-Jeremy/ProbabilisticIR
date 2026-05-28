package preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static Set<String> stopWords;
    /**
     * Konstruktor default untuk membuat objek Tokenizer.
     */
    public Tokenizer() {
        Tokenizer.stopWords = loadStopWords();
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

            if (!word.isEmpty() && isNotStopWord(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    public boolean isNotStopWord(String text) {
        return !stopWords.contains(text);
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

    /**
     * Stop word list standar Bahasa Inggris
     */
     private Set<String> loadStopWords() {
        Set<String> sw = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am",
            "an", "and", "any", "are", "aren't", "as", "at", "be", "because",
            "been", "before", "being", "below", "between", "both", "but", "by",
            "can't", "cannot", "could", "couldn't", "did", "didn't", "do",
            "does", "doesn't", "doing", "don't", "down", "during", "each",
            "few", "for", "from", "further", "get", "got", "had", "hadn't",
            "has", "hasn't", "have", "haven't", "having", "he", "he'd",
            "he'll", "he's", "her", "here", "here's", "hers", "herself",
            "him", "himself", "his", "how", "how's", "i", "i'd", "i'll",
            "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's",
            "its", "itself", "let's", "me", "more", "most", "mustn't", "my",
            "myself", "no", "nor", "not", "of", "off", "on", "once", "only",
            "or", "other", "ought", "our", "ours", "ourselves", "out", "over",
            "own", "same", "shan't", "she", "she'd", "she'll", "she's",
            "should", "shouldn't", "so", "some", "such", "than", "that",
            "that's", "the", "their", "theirs", "them", "themselves", "then",
            "there", "there's", "these", "they", "they'd", "they'll",
            "they're", "they've", "this", "those", "through", "to", "too",
            "under", "until", "up", "very", "was", "wasn't", "we", "we'd",
            "we'll", "we're", "we've", "were", "weren't", "what", "what's",
            "when", "when's", "where", "where's", "which", "while", "who",
            "who's", "whom", "why", "why's", "will", "with", "won't",
            "would", "wouldn't", "you", "you'd", "you'll", "you're",
            "you've", "your", "yours", "yourself", "yourselves"
        ));
        return sw;
    }

    
}
