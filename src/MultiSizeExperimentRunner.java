import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * Kelas MultiSizeExperimentRunner - Menjalankan eksperimen untuk berbagai ukuran puzzle
 * 
 * Fitur:
 * - Membaca puzzle dari file txt
 * - Menjalankan eksperimen untuk berbagai ukuran puzzle
 * - Menggunakan parameter yang disesuaikan dengan ukuran puzzle
 * - Menghasilkan laporan lengkap
 * 
 * @author Kelompok 14
 */
public class MultiSizeExperimentRunner {
    
    /**
     * Menjalankan eksperimen untuk semua puzzle di folder examples
     * 
     * @param examplesFolder Folder yang berisi file puzzle
     * @param baseSeed Seed dasar untuk random
     */
    public static void runExperimentsForAllPuzzles(String examplesFolder, long baseSeed) {
        System.out.println("========================================");
        System.out.println("   MULTI-SIZE PUZZLE EXPERIMENTS");
        System.out.println("========================================\n");
        
        // Dapatkan semua file puzzle
        File folder = new File(examplesFolder);
        File[] files = folder.listFiles((dir, name) -> name.startsWith("puzzle_") && name.endsWith(".txt"));
        
        if (files == null || files.length == 0) {
            System.out.println("No puzzle files found in " + examplesFolder);
            return;
        }
        
        // Urutkan file berdasarkan ukuran puzzle (dari terkecil ke terbesar)
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                try {
                    // Baca ukuran puzzle dari file
                    Puzzle p1 = loadPuzzleFromFile(f1.getPath());
                    Puzzle p2 = loadPuzzleFromFile(f2.getPath());
                    
                    int size1 = p1.getWidth() * p1.getHeight();
                    int size2 = p2.getWidth() * p2.getHeight();
                    
                    // Urutkan berdasarkan ukuran (terkecil dulu)
                    if (size1 != size2) {
                        return Integer.compare(size1, size2);
                    }
                    
                    // Jika ukuran sama, urutkan berdasarkan width
                    if (p1.getWidth() != p2.getWidth()) {
                        return Integer.compare(p1.getWidth(), p2.getWidth());
                    }
                    
                    // Jika width sama, urutkan berdasarkan nama file
                    return f1.getName().compareTo(f2.getName());
                } catch (Exception e) {
                    // Jika error membaca file, urutkan berdasarkan nama
                    return f1.getName().compareTo(f2.getName());
                }
            }
        });
        
        System.out.println("Found " + files.length + " puzzle files:");
        
        List<ExperimentSummary> summaries = new ArrayList<>();
        int seedOffset = 0;
        
        // Jalankan eksperimen untuk setiap puzzle (dari terkecil ke terbesar)
        for (File file : files) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Processing: " + file.getName());
            System.out.println("=".repeat(60));
            
            try {
                Puzzle puzzle = loadPuzzleFromFile(file.getPath());
                System.out.println("\nPuzzle " + puzzle.getWidth() + "x" + puzzle.getHeight() + ":");
                puzzle.print();
                
                // Tentukan parameter berdasarkan ukuran puzzle
                ExperimentConfig config = getConfigForSize(puzzle.getWidth(), puzzle.getHeight());
                
                System.out.println("\nConfiguration:");
                System.out.println("  Population Size: " + config.popSize);
                System.out.println("  Mutation Rate: " + config.mutationRate);
                System.out.println("  Max Generations: " + config.maxGen);
                System.out.println("  Crossover: " + config.crossoverName);
                System.out.println("  Mutation: " + config.mutationName);
                
                // Jalankan eksperimen
                long seed = baseSeed + seedOffset;
                ExperimentResult result = runExperimentForPuzzle(
                    puzzle, file.getName(), config, seed
                );
                
                //Menambahkan hasil eksperimen ke summary
                summaries.add(new ExperimentSummary(
                    file.getName(), puzzle.getWidth(), puzzle.getHeight(),
                    result.solved, result.generations, result.time, result.bestFitness
                ));
                
                seedOffset += 100;
                
            } catch (FileNotFoundException e) {
                System.out.println("Error: File not found - " + file.getName());
            } catch (Exception e) {
                System.out.println("Error processing " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Print summary
        printSummary(summaries);
    }
    
    //return experiment configuration based on puzzle size
    private static ExperimentConfig getConfigForSize(int width, int height) {
        int size = width * height;
        
        //set hyperparameters
        if (size <= 9) { // 3x3
            return new ExperimentConfig(
                50, 0.85, 0.02, 200, 2,
                new RouletteWheelSelection(),
                new SinglePointCrossover(),
                new AdaptiveMutation(0.03, 0.005, 200),
                "Single Point", "Adaptive"
            );
        } else if (size <= 16) { // 4x4
            return new ExperimentConfig(
                    80, 0.85, 0.025, 400, 3,
                new RouletteWheelSelection(),
                new TwoPointCrossover(),
                new AdaptiveMutation(0.04, 0.005, 400),
                "Two Point", "Adaptive"
            );
        } else if (size <= 25) { // 5x5
            return new ExperimentConfig(
                140, 0.85, 0.035, 1500, 5,
                new RouletteWheelSelection(),
                new FitnessGuidedUniformCrossover(0.25),
                new AdaptiveMutation(0.06, 0.005, 1500),
                "Fitness-Guided", "Adaptive"
            );
        } else if (size <= 36) { // 6x6
            return new ExperimentConfig(
                600, 0.86, 0.12, 500, 6,
                new RouletteWheelSelection(),
                new FitnessGuidedUniformCrossover(0.30),
                new AdaptiveMutation(0.12, 0.01, 500),
                "Fitness-Guided", "Adaptive"
            );
        } else if (size <= 49) { // 7x7
            return new ExperimentConfig(
                700, 0.86, 0.125, 600, 8,
                new RouletteWheelSelection(),
                new FitnessGuidedUniformCrossover(0.32),
                new AdaptiveMutation(0.13, 0.01, 600),
                "Fitness-Guided", "Adaptive"
            );
        } else if (size <= 64) { // 8x8
            return new ExperimentConfig(
                800, 0.87, 0.13, 700, 10,
                new RouletteWheelSelection(),
                new FitnessGuidedUniformCrossover(0.35),
                new AdaptiveMutation(0.14, 0.01, 700),
                "Fitness-Guided", "Adaptive"
            );
        } else { // 10x10 atau lebih besar
            return new ExperimentConfig(
                1000, 0.87, 0.14, 800, 12,
                new RouletteWheelSelection(),
                new FitnessGuidedUniformCrossover(0.35),
                new AdaptiveMutation(0.16, 0.01, 800),
                "Fitness-Guided", "Adaptive"
            );
        }
    }
    
    //Jalanin experiment
    private static ExperimentResult runExperimentForPuzzle(
            Puzzle puzzle, String filename, ExperimentConfig config, long seed) {
        
        System.out.println("\n--- Running Experiment (seed: " + seed + ") ---");
        long startTime = System.currentTimeMillis();
        
        ImprovedGeneticAlgorithm ga = new ImprovedGeneticAlgorithm(
            puzzle, config.popSize, config.crossoverRate, config.mutationRate,
            config.maxGen, config.elitism, config.selection, config.crossover,
            config.mutation, seed
        );
        
        Chromosome solution = ga.run();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("\n--- Results ---");
        System.out.println("  Solved: " + (ga.isSolutionFound() ? "YES" : "NO"));
        System.out.println("  Generations: " + ga.getCurrentGeneration());
        System.out.println("  Best Fitness: " + String.format("%.2f", ga.getBestFitness()));
        System.out.println("  Time: " + duration + " ms (~" + (duration / 1000.0) + " seconds)");
        System.out.println("  Final Diversity: " + String.format("%.3f", ga.getCurrentDiversity()));
        
        if (ga.isSolutionFound()) {
            System.out.println("\nSolution:");
            solution.print();
        } else {
            System.out.println("\nBest solution found:");
            solution.print();
        }
        
        return new ExperimentResult(
            ga.isSolutionFound(), ga.getCurrentGeneration(), duration, ga.getBestFitness()
        );
    }
    
    /**
     * Membaca puzzle dari file
     */
    public static Puzzle loadPuzzleFromFile(String filename) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(filename));
        
        int width = scanner.nextInt();
        int height = scanner.nextInt();
        int[][] board = new int[height][width];
        
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                board[i][j] = scanner.nextInt();
            }
        }
        
        scanner.close();
        return new Puzzle(width, height, board);
    }
    
    /**
     * Mencetak summary semua eksperimen
     */
    private static void printSummary(List<ExperimentSummary> summaries) {
        System.out.println("\n\n" + "=".repeat(45));
        System.out.println("   EXPERIMENT SUMMARY");
        System.out.println("=".repeat(45));
        System.out.println();
        System.out.printf("%-25s %8s %12s %10s %12s %10s%n",
            "Puzzle", "Size", "Solved", "Generations", "Time (s)", "Best Fit");
        System.out.println("-".repeat(85));
        
        int totalSolved = 0;
        long totalTime = 0;
        int totalGenerations = 0;
        
        for (ExperimentSummary s : summaries) {
            System.out.printf("%-25s %4dx%-4d %8s %12d %10.2f %12.2f%n",
                s.filename, s.width, s.height,
                s.solved ? "YES" : "NO",
                s.generations,
                s.time / 1000.0,
                s.bestFitness);
            
            if (s.solved) totalSolved++;
            totalTime += s.time;
            totalGenerations += s.generations;
        }
        
        System.out.println("-".repeat(85));
        System.out.printf("Total: %d puzzles, %d solved (%.1f%%), Avg time: %.2f s, Avg gen: %d%n",
            summaries.size(), totalSolved, (totalSolved * 100.0 / summaries.size()),
            totalTime / 1000.0 / summaries.size(),
            totalGenerations / summaries.size());
    }
    
    /**
     * Konfigurasi eksperimen
     */
    private static class ExperimentConfig {
        int popSize;
        double crossoverRate;
        double mutationRate;
        int maxGen;
        int elitism;
        SelectionStrategy selection;
        CrossoverStrategy crossover;
        MutationStrategy mutation;
        String crossoverName;
        String mutationName;
        
        ExperimentConfig(int popSize, double crossRate, double mutRate, int maxGen, int elitism,
                        SelectionStrategy sel, CrossoverStrategy cross, MutationStrategy mut,
                        String crossName, String mutName) {
            this.popSize = popSize;
            this.crossoverRate = crossRate;
            this.mutationRate = mutRate;
            this.maxGen = maxGen;
            this.elitism = elitism;
            this.selection = sel;
            this.crossover = cross;
            this.mutation = mut;
            this.crossoverName = crossName;
            this.mutationName = mutName;
        }
    }
    
    /**
     * Hasil eksperimen
     */
    private static class ExperimentResult {
        boolean solved;
        int generations;
        long time;
        double bestFitness;
        
        ExperimentResult(boolean solved, int generations, long time, double bestFitness) {
            this.solved = solved;
            this.generations = generations;
            this.time = time;
            this.bestFitness = bestFitness;
        }
    }
    
    /**
     * Summary eksperimen
     */
    private static class ExperimentSummary {
        String filename;
        int width;
        int height;
        boolean solved;
        int generations;
        long time;
        double bestFitness;
        
        ExperimentSummary(String filename, int width, int height, boolean solved,
                         int generations, long time, double bestFitness) {
            this.filename = filename;
            this.width = width;
            this.height = height;
            this.solved = solved;
            this.generations = generations;
            this.time = time;
            this.bestFitness = bestFitness;
        }
    }
    
    /**
     * Menjalankan eksperimen untuk satu file puzzle saja
     * 
     * @param filePath Path lengkap ke file puzzle (bisa relatif atau absolut)
     * @param seed Seed untuk random
     */
    public static void runExperimentForSingleFile(String filePath, long seed) {
        System.out.println("========================================");
        System.out.println("   SINGLE PUZZLE EXPERIMENT");
        System.out.println("========================================\n");
        
        File file = new File(filePath);
        
        // Jika file tidak ada, coba cari di folder examples
        if (!file.exists()) {
            file = new File("examples", filePath);
        }
        
        // Jika masih tidak ada, coba dengan nama file langsung
        if (!file.exists() && !filePath.contains(File.separator) && !filePath.contains("/")) {
            file = new File("examples", filePath);
        }
        
        if (!file.exists()) {
            System.out.println("Error: File not found - " + filePath);
            System.out.println("Tried: " + new File(filePath).getAbsolutePath());
            System.out.println("Tried: " + new File("examples", filePath).getAbsolutePath());
            return;
        }
        
        System.out.println("Processing: " + file.getName());
        System.out.println("File path: " + file.getAbsolutePath());
        
        try {
            Puzzle puzzle = loadPuzzleFromFile(file.getPath());
            System.out.println("\nPuzzle " + puzzle.getWidth() + "x" + puzzle.getHeight() + ":");
            puzzle.print();
            
            // Tentukan parameter berdasarkan ukuran puzzle
            ExperimentConfig config = getConfigForSize(puzzle.getWidth(), puzzle.getHeight());
            
            System.out.println("\nConfiguration:");
            System.out.println("  Population Size: " + config.popSize);
            System.out.println("  Crossover Rate: " + config.crossoverRate);
            System.out.println("  Mutation Rate: " + config.mutationRate);
            System.out.println("  Max Generations: " + config.maxGen);
            System.out.println("  Elitism: " + config.elitism);
            System.out.println("  Selection: Roulette Wheel");
            System.out.println("  Crossover: " + config.crossoverName);
            System.out.println("  Mutation: " + config.mutationName);
            
            // Jalankan eksperimen
            System.out.println("\n--- Running Experiment (seed: " + seed + ") ---");
            long startTime = System.currentTimeMillis();
            
            ImprovedGeneticAlgorithm ga = new ImprovedGeneticAlgorithm(
                puzzle, config.popSize, config.crossoverRate, config.mutationRate,
                config.maxGen, config.elitism, config.selection, config.crossover,
                config.mutation, seed
            );
            
            Chromosome solution = ga.run();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n--- Results ---");
            System.out.println("  Solved: " + (ga.isSolutionFound() ? "YES" : "NO"));
            System.out.println("  Generations: " + ga.getCurrentGeneration());
            System.out.println("  Best Fitness: " + String.format("%.2f", ga.getBestFitness()));
            System.out.println("  Time: " + duration + " ms (~" + (duration / 1000.0) + " seconds)");
            System.out.println("  Final Diversity: " + String.format("%.3f", ga.getCurrentDiversity()));
            
            if (ga.isSolutionFound()) {
                System.out.println("\nSolution:");
                solution.print();
            } else {
                System.out.println("\nBest solution found:");
                solution.print();
            }
            
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found - " + file.getPath());
        } catch (Exception e) {
            System.out.println("Error processing " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n========================================");
        System.out.println("   EXPERIMENT COMPLETED");
        System.out.println("========================================");
    }
    
    /**
     * Method utama
     */
    public static void main(String[] args) {
        String examplesFolder = "examples";
        String singleFile = null;
        long baseSeed = 50000L;
        
        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--file") || args[i].equals("-file")) {
                if (i + 1 < args.length) {
                    singleFile = args[++i];
                }
            } else if (args[i].equals("--folder") || args[i].equals("-f")) {
                if (i + 1 < args.length) {
                    examplesFolder = args[++i];
                }
            } else if (args[i].equals("--seed") || args[i].equals("-s")) {
                if (i + 1 < args.length) {
                    baseSeed = Long.parseLong(args[++i]);
                }
            } else if (!args[i].startsWith("-")) {
                // Backward compatibility: first arg = folder, second = seed
                if (i == 0) examplesFolder = args[i];
                if (i == 1) baseSeed = Long.parseLong(args[i]);
            }
        }
        
        // Jika ada parameter --file, jalankan hanya file tersebut
        if (singleFile != null) {
            runExperimentForSingleFile(singleFile, baseSeed);
        } else {
            runExperimentsForAllPuzzles(examplesFolder, baseSeed);
            System.out.println("\n========================================");
            System.out.println("   ALL EXPERIMENTS COMPLETED");
            System.out.println("========================================");
        }
    }
}
