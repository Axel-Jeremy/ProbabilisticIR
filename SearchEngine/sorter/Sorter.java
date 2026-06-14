package sorter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kelas Sorter merupakan kelas yang berfungsi 
 * untuk menangani operasi pengurutan relevansi skor dokumen.
 * 
 * Sumber: Membuat sendiri
 * 
 * @author Axel 
 */

public class Sorter {
    
    //default constructor
    public Sorter(){
    }

    /**
     * Fungsi untuk mengurutkan hasil perhitungan skor dokumen 
     * secara menurun (descending) agar dokumen yang paling relevan berada di urutan teratas.
     * 
     * @param score Map yang berisi ID dokumen sebagai key dan skor RSV sebagai value.
     * 
     * @return Map baru yang sudah terurut dari nilai skor tertinggi hingga terendah.
     */
    public Map<Integer, Double> sortDescending(Map<Integer, Double> score) {
        List<Map.Entry<Integer, Double>> entries = new ArrayList<>(score.entrySet());
        // Mengurutkan secara descending membandingkan value dari b dengan a
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        Map<Integer, Double> hasil = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> e : entries) {
            hasil.put(e.getKey(), e.getValue());
        }
        return hasil;
    }
}
