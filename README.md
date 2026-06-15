# Probabilistic Information Retrieval System

Sistem *Information Retrieval* ini dibangun menggunakan bahasa pemrograman **Java**. Proyek ini mengimplementasikan model pencarian berbasis probabilistik yang menggabungkan beberapa model pemeringkatan dokumen serta teknik *Pseudo Relevance Feedback* untuk meningkatkan kualitas hasil pencarian.

Proyek ini dibuat untuk memenuhi tugas mata kuliah *Information Retrieval* sarjana Informatika di Universitas Katolik Parahyangan.

## Cara Run (Gunakan CMD atau PowerShell)

```bash
cd SearchEngine
javac *.java 
java Main.java
```

## Fitur Utama

Sistem ini terdiri dari beberapa modul utama:

1. **Document Reader:** Membaca dan mengekstraksi teks dari kumpulan dokumen dataset Cranfield (hingga 1.400 dokumen).

2. **Text Preprocessing:**
   - **Tokenizer:** Memecah teks menjadi token, membuang karakter non-alfabet dan *stop words*, serta melakukan *case folding*.
   - **Stemmer:** Mengembalikan kata ke bentuk dasarnya menggunakan algoritma **Porter Stemmer**.
   - **TextPreprocessor:** Pembungkus (*wrapper*) yang menyatukan Tokenizer dan Stemmer dalam satu alur pemrosesan.

3. **Inverted Index:** Struktur data utama yang memetakan setiap term ke *postings list* berisi DocID dan term frequency, dilengkapi statistik seperti rata-rata panjang dokumen.

4. **Model Pemeringkatan Probabilistik:**
   - **BIM (Binary Independence Model):** Menghitung skor relevansi dokumen berdasarkan bobot term w_t dari estimasi probabilitas p_t dan u_t.
   - **BM11 & BM25:** Varian BM dengan parameter k (pengaruh term frequency) dan b (normalisasi panjang dokumen). BM11 menggunakan b=1, BM25 menggunakan b=0.75.
   - **Two Poisson Model:** Model probabilistik yang memisahkan distribusi kemunculan term pada dokumen relevan dan tidak relevan.

5. **Pseudo Relevance Feedback (PRF):** Teknik *relevance feedback* otomatis yang mengasumsikan Top-K dokumen hasil ranking awal sebagai relevan, lalu menggunakannya untuk memperbarui bobot term dan menghasilkan ranking akhir yang lebih akurat.

6. **Evaluator:** Modul evaluasi performa sistem menggunakan *ground truth* dari dataset Cranfield (folder RES), dengan metrik:
   - **Precision at 10:** Rasio dokumen relevan di antara 10 hasil teratas.
   - **Recall at 10:** Rasio dokumen relevan yang berhasil ditemukan dari total dokumen relevan.
   - **F1-Score at 10:** Rata-rata harmonik dari Precision dan Recall.
   - **11-point Average Precision:** Rata-rata precision interpolasi pada 11 titik recall standar (0.0, 0.1, ..., 1.0).

## Dataset

Sistem ini menggunakan **Cranfield Collection**, salah satu benchmark klasik dalam bidang Information Retrieval:
- **1.400 dokumen** ilmiah di bidang aerodinamika
- **225 query** standar
- **Relevance judgment** untuk setiap pasangan query-dokumen dengan skor -1 (tidak relevan) hingga 4 (sangat relevan)

## Contoh Penggunaan

Setelah program berjalan, masukkan jumlah dokumen yang ingin diindeks, lalu ketik query pencarian.

```
Enter total documents indexed in dataset (max 1400): 1400
Building Inverted Index...

Enter query (type 'exit' to cancel): what similarity laws must be obeyed when constructing aeroelastic models of heated high speed aircraft .

--- BIM ---
Rank 1 | DocID: 51  | Score: 12.3456
Rank 2 | DocID: 102 | Score: 11.2341
...
Precision at 10: 0.3000 | Recall at 10: 0.6000 | F1 at 10: 0.4000
11-Point Average Precision: 0.4523
```

> **Catatan:** Untuk mendapatkan hasil evaluasi, query yang dimasukkan harus **sama persis** (termasuk tanda baca) dengan query yang ada di `query.txt`. Jika query tidak ditemukan, sistem tetap menampilkan hasil ranking tanpa metrik evaluasi.
