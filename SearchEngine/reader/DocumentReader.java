package reader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Kelas DocumentReader berfungsi untuk membaca kumpulan dokumen teks dari 
 * sebuah direktori atau folder tertentu. Dokumen yang dibaca kemudian 
 * disimpan ke dalam struktur data Map untuk diproses lebih lanjut.
 * 
 * Sumber: Membuat sendiri dengan bantuan LLM
 * 
 * @author Axel, Keane
 */
public class DocumentReader {
    /**
     * Menyimpan path atau lokasi direktori tempat dokumen berada.
     */
    private String folderPath;

    /**
     * Constructor
     * 
     * @param folderPath Lokasi atau path dari folder yang menyimpan file teks 
     *                   (dalam proyek ini: Dataset).
     */
    public DocumentReader(String folderPath) {
        this.folderPath = folderPath;
    }

    /**
     * Membaca semua dokumen berformat .txt (dari 1.txt hingga n.txt) 
     * yang ada di dalam folder folderPath.
     * 
     * @return Sebuah Map yang memetakan ID dokumen (Integer) sebagai key 
     *         dan isi teks dari dokumen tersebut (String) sebagai value.
     */
    public Map<Integer, String> readAll(int n) {
        Map<Integer, String> documents = new HashMap<>();

        for (int i = 1; i <= n; i++) {
            String filePath = "" + folderPath + "/" + i + ".txt";
            String content = readFile(filePath);

            // Jika file berhasil dibaca dan isinya tidak null, masukkan ke dalam Map
            if (content != null) {
                documents.put(i, content);
            }
        }

        System.out.println("Successfully read " + documents.size() + " document(s).");
        return documents;
    }

    /**
     * Membaca isi teks dari satu file secara spesifik berdasarkan path-nya.
     * 
     * @param filePath Path lengkap menuju file yang akan dibaca 
     * @param docID    ID unik dokumen yang sedang dibaca
     * @return String yang berisi seluruh teks di dalam file dokumen. 
     *         Mengembalikan nilai null jika file tidak ditemukan atau 
     *         jika terjadi kesalahan saat proses membaca file.
     */
    private String readFile(String filePath) {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(" ");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filePath);
            return null;
        } catch (IOException e) {
            System.err.println("Read file failed: " + filePath + " --> " + e.getMessage());
            return null;
        }

        return content.toString().trim();
    }
}