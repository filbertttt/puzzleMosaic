import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Otomatisasi pencarian parameter terbaik untuk Mosaic Puzzle 6x6.
 * Didesain khusus untuk dijalankan di macOS terminal.
 */
public class ParameterTuner {

    public static void main(String[] args) {
        // 1. Identifikasi File Puzzle (Menyesuaikan dengan struktur folder Anda)
        String fileName = "puzzle_7x7_medium.txt";
        File puzzleFile = new File("examples/" + fileName);

        // Fallback jika program dijalankan dari luar folder 'code'
        if (!puzzleFile.exists()) {
            puzzleFile = new File("src/examples/" + fileName);
        }

        if (!puzzleFile.exists()) {
            System.out.println("❌ Error: File " + fileName + " tidak ditemukan!");
            System.out.println("Lokasi saat ini: " + System.getProperty("user.dir"));
            System.out.println("Pastikan folder 'examples' berada di folder yang sama dengan tempat Anda menjalankan terminal.");
            return;
        }

        // 2. Definisi Parameter yang akan diuji (Grid Search)
        // Kita gunakan nilai yang lebih tinggi karena 6x6 tergolong sulit
        int[] popSizes = {600, 1000, 1500};
        double[] mutRates = {0.12, 0.15, 0.20}; // Initial mutation rate
        int[] elitismValues = {10, 30, 60};     // Jumlah elit yang dijaga
        int maxGen = 1000;                      // Batas generasi lebih panjang
        long seed = 50000L;

        List<TuningResult> results = new ArrayList<>();

        System.out.println("=================================================");
        System.out.println("   AUTOMATED PARAMETER TUNING - MOSAIC 6x6");
        System.out.println("=================================================");
        System.out.println("Target File: " + puzzleFile.getPath());
        System.out.println("Total Kombinasi: " + (popSizes.length * mutRates.length * elitismValues.length));

        try {
            Puzzle puzzle = MultiSizeExperimentRunner.loadPuzzleFromFile(puzzleFile.getPath());

            for (int pop : popSizes) {
                for (double mut : mutRates) {
                    for (int elit : elitismValues) {
                        
                        System.out.printf("Testing -> Pop: %d, Mut: %.2f, Elit: %d | ", pop, mut, elit);
                        
                        // Inisialisasi Strategi
                        SelectionStrategy selection = new RouletteWheelSelection();
                        CrossoverStrategy crossover = new FitnessGuidedUniformCrossover(0.40); // Bias ditingkatkan ke 0.4
                        MutationStrategy mutation = new AdaptiveMutation(mut, 0.01, maxGen);

                        long startTime = System.currentTimeMillis();
                        
                        // Menjalankan Algoritma Genetika
                        ImprovedGeneticAlgorithm ga = new ImprovedGeneticAlgorithm(
                            puzzle, pop, 0.86, mut, maxGen, elit, 
                            selection, crossover, mutation, seed
                        );

                        ga.run();
                        long duration = System.currentTimeMillis() - startTime;

                        // Simpan Hasil
                        TuningResult res = new TuningResult(
                            pop, mut, elit, ga.isSolutionFound(), 
                            ga.getCurrentGeneration(), duration, ga.getBestFitness()
                        );
                        results.add(res);
                        
                        if(res.solved) {
                            System.out.println("✅ SOLVED!");
                        } else {
                            System.out.println("❌ FAILED (Best Fit: " + String.format("%.2f", res.fitness) + ")");
                        }
                    }
                }
            }

            // Tampilkan Laporan Akhir
            printFinalReport(results);
            saveToCSV(results);

        } catch (Exception e) {
            System.err.println("Terjadi kesalahan saat tuning: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printFinalReport(List<TuningResult> results) {
        System.out.println("\n\n" + "=".repeat(75));
        System.out.println("                     FINAL TUNING SUMMARY");
        System.out.println("=".repeat(75));
        System.out.printf("%-10s %-10s %-10s %-10s %-10s %-12s %-10s%n", 
                          "PopSize", "MutRate", "Elit", "Solved", "Gen", "Time(ms)", "Fitness");
        System.out.println("-".repeat(75));

        for (TuningResult r : results) {
            System.out.printf("%-10d %-10.2f %-10d %-10s %-10d %-12d %-10.2f%n", 
                              r.pop, r.mut, r.elit, r.solved ? "YES" : "NO", r.gen, r.time, r.fitness);
        }
    }

    private static void saveToCSV(List<TuningResult> results) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("tuning_results2.csv"))) {
            writer.println("PopSize,MutationRate,Elitism,Solved,Generations,TimeMS,BestFitness");
            for (TuningResult r : results) {
                writer.printf("%d,%.2f,%d,%b,%d,%d,%.2f%n", 
                              r.pop, r.mut, r.elit, r.solved, r.gen, r.time, r.fitness);
            }
            System.out.println("\n✅ Berhasil! Hasil lengkap disimpan di: tuning_results.csv");
        } catch (IOException e) {
            System.out.println("❌ Gagal menyimpan file CSV.");
        }
    }

    // Class helper untuk menyimpan hasil tiap iterasi
    private static class TuningResult {
        int pop, elit, gen;
        double mut, fitness;
        boolean solved;
        long time;

        TuningResult(int p, double m, int e, boolean s, int g, long t, double f) {
            this.pop = p; this.mut = m; this.elit = e; 
            this.solved = s; this.gen = g; this.time = t; this.fitness = f;
        }
    }
}
