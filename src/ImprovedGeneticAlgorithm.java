import java.util.Random;
import java.util.Arrays;
import java.util.Comparator;

public class ImprovedGeneticAlgorithm {
    private Puzzle puzzle;
    private int populationSize;
    @SuppressWarnings("unused")  // disimpan untuk referensi/getter methods
    private double crossoverRate;  // disimpan untuk referensi
    private int maxGenerations;
    @SuppressWarnings("unused")  // disimpan untuk referensi/getter methods
    private double mutationRate;   // disimpan untuk referensi
    @SuppressWarnings("unused")  // disimpan untuk referensi/getter methods
    private int elitismCount;      // disimpan untuk referensi
    private SelectionStrategy selectionStrategy;
    private CrossoverStrategy crossoverStrategy;
    private MutationStrategy mutationStrategy;
    private FitnessFunction fitnessFunction;
    private Heuristics heuristics;
    private Random random;
    private Chromosome[] population;
    private int currentGeneration;
    private Chromosome bestChromosome;
    private double bestFitness;
    private double currentDiversity;
    private double averageFitness;
    private double fitnessStdDev;
    
    // Anti-stagnation variables
    private double previousBestFitness;
    private int stagnationCount;
    private double initialDiversity;
    private int originalElitismCount;  // Simpan elitism original untuk temporary reduction
    
    // Adaptive parameters (IMPROVEMENT)
    private double originalCrossoverRate;  // Simpan crossover rate original untuk adaptive
    private double originalMutationRate;   // Simpan mutation rate original untuk adaptive
    private double improvementRate;        // Track improvement rate untuk adaptive parameters
    
    public ImprovedGeneticAlgorithm(
            Puzzle puzzle,
            int populationSize,
            double crossoverRate,
            double mutationRate,
            int maxGenerations,
            int elitismCount,
            SelectionStrategy selectionStrategy,
            CrossoverStrategy crossoverStrategy,
            MutationStrategy mutationStrategy,
            long seed) {
        
        this.puzzle = puzzle;
        this.populationSize = populationSize;
        this.crossoverRate = crossoverRate;  // Store for reference
        this.mutationRate = mutationRate;    // Store for reference
        this.maxGenerations = maxGenerations;
        this.elitismCount = elitismCount;    // Store for reference
        this.selectionStrategy = selectionStrategy;
        this.crossoverStrategy = crossoverStrategy;
        this.mutationStrategy = mutationStrategy;
        this.random = new Random(seed);
        
        this.fitnessFunction = new FitnessFunction(puzzle);
        this.heuristics = new Heuristics(puzzle);
        this.currentGeneration = 0;
        this.bestFitness = Double.MAX_VALUE;
        this.stagnationCount = 0;
        this.originalElitismCount = elitismCount;  // Simpan untuk temporary reduction
        
        // Save original parameters for adaptive mechanisms (IMPROVEMENT)
        this.originalCrossoverRate = crossoverRate;
        this.originalMutationRate = mutationRate;
        this.improvementRate = 1.0;  // Start with normal rate
    }
    
    //Getter untuk mendapatkan parameter original
    public double getOriginalCrossoverRate() { return originalCrossoverRate; }
    public double getOriginalMutationRate() { return originalMutationRate; }
    public int getOriginalElitismCount() { return originalElitismCount; }
    public double getCurrentCrossoverRate() { 
        return (currentGeneration > 0) ? calculateAdaptiveCrossoverRate() : originalCrossoverRate; 
    }
    
    private void initializePopulation() {
        population = new Chromosome[populationSize];
        
        // Buat populasi acak
        for (int i = 0; i < populationSize; i++) {
            population[i] = new Chromosome(puzzle.getWidth(), puzzle.getHeight(), random);
        }
        
        // Terapkan heuristik pada sebagian populasi
        // Puzzle kompleks (banyak hints) → lebih banyak heuristics
        // Puzzle sederhana (sedikit hints) → lebih sedikit heuristics
        int puzzleSize = puzzle.getWidth() * puzzle.getHeight();
        int hintCount = puzzle.getHintPositions().length;
        double hintDensity = (double) hintCount / puzzleSize;  // Density hints
        
        // Adaptive heuristic percentage: semakin padat hints, semakin banyak heuristics
        // Range: 15% (sparse) hingga 40% (dense)
        double heuristicPercent = 0.15 + (hintDensity * 0.25);  // 15% + (density * 25%)
        heuristicPercent = Math.min(0.40, Math.max(0.15, heuristicPercent));  // Clamp 15-40%
        
        // Untuk puzzle sangat kecil (< 16), batasi heuristics untuk diversity
        if (puzzleSize < 16) {
            heuristicPercent = Math.min(0.25, heuristicPercent);
        }
        
        int applyCount = (int) (population.length * heuristicPercent);
        for (int i = 0; i < applyCount; i++) {
            heuristics.applyHeuristics(population[i]);
        }
        
        // Evaluasi fitness
        evaluatePopulation();
        updateBestChromosome();
        updatePopulationStatistics();
        
        // Simpan initial diversity untuk stagnation detection
        initialDiversity = currentDiversity;
        previousBestFitness = bestFitness;
        
        // Debug: print beberapa fitness awal
        System.out.println("Initial population fitness range:");
        Arrays.sort(population, new Comparator<Chromosome>() {
            @Override
            public int compare(Chromosome c1, Chromosome c2) {
                return Double.compare(c1.getFitness(), c2.getFitness());
            }
        });
        System.out.println("  Best: " + String.format("%.2f", population[0].getFitness()));
        System.out.println("  Worst: " + String.format("%.2f", population[population.length - 1].getFitness()));
        System.out.println("  Median: " + String.format("%.2f", population[population.length / 2].getFitness()));
    }
    
    private void evaluatePopulation() {
        for (Chromosome chromosome : population) {
            fitnessFunction.calculateFitness(chromosome);
        }
    }
    
    private void updateBestChromosome() {
        for (Chromosome chromosome : population) {
            if (chromosome.getFitness() < bestFitness) {
                bestFitness = chromosome.getFitness();
                bestChromosome = chromosome.clone();
            }
        }
    }
    
    private void updatePopulationStatistics() {
        // Hitung diversity lebih jarang untuk populasi besar (setiap 5 generasi)
        // Untuk large population, diversity calculation bisa sangat lambat
        boolean shouldCalculateDiversity = true;
        if (populationSize > 300) {
            shouldCalculateDiversity = (currentGeneration % 5 == 0 || currentGeneration == 0);
        }
        
        if (shouldCalculateDiversity) {
            currentDiversity = DiversityMeasure.calculateDiversity(population);
        }
        // Jika tidak dihitung, gunakan nilai sebelumnya (atau estimasi)
        // Ini acceptable karena diversity tidak berubah drastis dalam beberapa generasi
        
        double totalFitness = 0.0;
        for (Chromosome chromosome : population) {
            totalFitness += chromosome.getFitness();
        }
        averageFitness = totalFitness / population.length;
        fitnessStdDev = DiversityMeasure.calculateFitnessDiversity(population);
    }
    
    private void sortPopulation() {
        Arrays.sort(population, new Comparator<Chromosome>() {
            @Override
            public int compare(Chromosome c1, Chromosome c2) {
                return Double.compare(c1.getFitness(), c2.getFitness());
            }
        });
    }
    
    /**
     * Escape mechanism 1: Mutation boost + Random exploration
     * OPTIMIZED: Untuk large population, lebih agresif
     * Meningkatkan mutation rate sementara untuk eksplorasi lebih banyak
     * TAMBAHAN: Temporary elitism reduction + Random exploration boost
     */
    private void escapeMutationBoost() {
        // OPTIMIZED: Untuk large population, reduction lebih kecil (keep more elite)
        boolean isLargePopulation = (populationSize > maxGenerations);
        double reductionFactor = isLargePopulation ? 0.6 : 0.5;
        int tempElitism = Math.max(1, (int)(originalElitismCount * reductionFactor));
        
        // 2. Mutation boost dengan rate lebih tinggi (OPTIMIZED untuk large pop)
        // OPTIMIZED: Untuk large population, boost lebih tinggi untuk diversity
        double boostMultiplier = isLargePopulation ? 5.0 : 4.0;
        double boostRate = Math.min(0.25, originalMutationRate * boostMultiplier);  // OPTIMIZED: Max 0.25 (dari 0.20)
        for (int i = tempElitism; i < populationSize; i++) {
            mutationStrategy.mutate(population[i], boostRate, random);
        }
        
        // 3. Random exploration boost: OPTIMIZED untuk large population
        // OPTIMIZED: Untuk large population, lebih banyak random exploration (15% vs 10%)
        double explorationPercent = isLargePopulation ? 0.15 : 0.10;
        int randomExplorationCount = Math.max(1, (int)(populationSize * explorationPercent));
        sortPopulation();  // Sort dulu untuk tahu yang terburuk
        for (int i = populationSize - randomExplorationCount; i < populationSize; i++) {
            // Ganti dengan kromosom random baru (TANPA heuristics untuk eksplorasi maksimal)
            population[i] = new Chromosome(puzzle.getWidth(), puzzle.getHeight(), random);
        }
        
        evaluatePopulation();
        updateBestChromosome();
    }
    
    /**
     * Escape mechanism 2: Aggressive diversity injection + Hybrid creation
     * OPTIMIZED: Untuk large population, lebih agresif
     * ENHANCED: Mengganti 60-70% populasi terburuk dengan kromosom baru/hybrid untuk eksplorasi maksimal
     */
    private void escapeDiversityInjection() {
        sortPopulation();  // Sort untuk tahu yang terburuk
        
        // OPTIMIZED: Untuk large population, replace lebih banyak
        boolean isLargePopulation = (populationSize > maxGenerations);
        double replacePercent;
        if (isLargePopulation) {
            // OPTIMIZED: Large population butuh lebih banyak diversity injection (70% vs 65%)
            replacePercent = (bestFitness > 15.0) ? 0.70 : 0.65;
        } else {
            replacePercent = (bestFitness > 15.0) ? 0.65 : 0.55;
        }
        int replaceCount = (int) (populationSize * replacePercent);
        
        for (int i = populationSize - replaceCount; i < populationSize; i++) {
            // 50% random baru, 50% hybrid (kombinasi dari berbagai individu)
            if (random.nextDouble() < 0.5) {
                // Random baru (TANPA heuristics untuk eksplorasi maksimal)
                population[i] = new Chromosome(puzzle.getWidth(), puzzle.getHeight(), random);
            } else {
                // Hybrid chromosome: kombinasi dari 2-3 individu random
                population[i] = createHybridChromosome();
            }
        }
        
        evaluatePopulation();
        updateBestChromosome();
    }
    
    /**
     * Membuat kromosom hybrid dari kombinasi beberapa individu untuk eksplorasi
     * Hybrid = kombinasi random dari 2-3 kromosom yang berbeda
     */
    private Chromosome createHybridChromosome() {
        // Pilih 2-3 kromosom random dari populasi
        int numParents = 2 + random.nextInt(2);  // 2 atau 3 parents
        Chromosome[] parents = new Chromosome[numParents];
        
        for (int i = 0; i < numParents; i++) {
            parents[i] = population[random.nextInt(populationSize)];
        }
        
        // Buat hybrid: untuk setiap posisi, pilih gen dari salah satu parent secara random
        Chromosome hybrid = new Chromosome(puzzle.getWidth(), puzzle.getHeight(), random);
        for (int i = 0; i < hybrid.getLength(); i++) {
            Chromosome selectedParent = parents[random.nextInt(numParents)];
            hybrid.setGene(i, selectedParent.getGene(i));
        }
        
        // Tambah sedikit mutasi untuk eksplorasi lebih lanjut
        for (int i = 0; i < hybrid.getLength(); i++) {
            if (random.nextDouble() < 0.1) {  // 10% chance mutasi
                hybrid.setGene(i, !hybrid.getGene(i));
            }
        }
        
        return hybrid;
    }
    
    /**
     * Escape mechanism 3: Aggressive partial restart + Population expansion
     * Keep HANYA best 1 chromosome, restart 80-90% populasi untuk eksplorasi maksimal
     * TAMBAHAN: Temporary population expansion untuk eksplorasi lebih luas
     */
    private void escapePartialRestart() {
        sortPopulation();  // Sort untuk tahu yang terbaik
        
        // Simpan HANYA best 1 chromosome (bukan elite, untuk eksplorasi maksimal)
        Chromosome savedBest = bestChromosome.clone();
        
        // OPTIMIZED: Untuk large population, replace lebih banyak (90% vs 85%)
        boolean isLargePopulation = (populationSize > maxGenerations);
        double replacePercent = isLargePopulation ? 0.90 : 0.85;
        int replaceCount = (int) (populationSize * replacePercent);
        
        for (int i = 1; i < replaceCount; i++) {  // Mulai dari index 1 (keep best di index 0)
            // 70% random baru, 30% hybrid
            if (random.nextDouble() < 0.7) {
                // Random baru (TANPA heuristics untuk eksplorasi maksimal)
                population[i] = new Chromosome(puzzle.getWidth(), puzzle.getHeight(), random);
            } else {
                // Hybrid chromosome
                population[i] = createHybridChromosome();
            }
        }
        
        // Pastikan best chromosome tetap ada di posisi 0
        population[0] = savedBest;
        
        // OPTIMIZED: Temporary population expansion untuk large population
        // OPTIMIZED: Untuk large population, expansion lebih besar (20% vs 15%)
        double expansionPercent = isLargePopulation ? 0.20 : 0.15;
        int expansionCount = Math.max(1, (int)(populationSize * expansionPercent));
        Chromosome[] expandedPopulation = new Chromosome[populationSize + expansionCount];
        System.arraycopy(population, 0, expandedPopulation, 0, populationSize);
        
        // Tambah individu baru yang benar-benar random
        for (int i = 0; i < expansionCount; i++) {
            expandedPopulation[populationSize + i] = new Chromosome(puzzle.getWidth(), puzzle.getHeight(), random);
        }
        
        // Evaluasi semua
        for (int i = 0; i < expandedPopulation.length; i++) {
            fitnessFunction.calculateFitness(expandedPopulation[i]);
        }
        
        // Pilih yang terbaik untuk populasi normal
        Arrays.sort(expandedPopulation, new Comparator<Chromosome>() {
            @Override
            public int compare(Chromosome c1, Chromosome c2) {
                return Double.compare(c1.getFitness(), c2.getFitness());
            }
        });
        
        // Ambil yang terbaik untuk populasi
        System.arraycopy(expandedPopulation, 0, population, 0, populationSize);
        
        evaluatePopulation();
        updateBestChromosome();
    }
    
    /**
     * Deteksi stagnasi yang lebih cerdas dan sensitif (ENHANCED)
     * Stagnasi jika: fitness tidak membaik DAN (diversity rendah ATAU populasi homogen ATAU improvement rate rendah)
     * 
     * ENHANCED: Lebih sensitif untuk puzzle besar dan konvergensi lambat
     * 
     * @return true jika populasi stagnan
     */
    private boolean isStagnating() {
        // Stagnasi jika:
        // 1. Fitness tidak membaik (tidak ada improvement signifikan)
        // ENHANCED: Threshold lebih sensitif, khususnya untuk fitness besar
        double fitnessThreshold = (bestFitness > 20.0) ? 0.01 : 
                                 (bestFitness > 10.0) ? 0.005 : 0.0001;
        boolean fitnessStagnant = (Math.abs(bestFitness - previousBestFitness) < fitnessThreshold);
        
        // 2. Diversity terlalu rendah (< 30% dari initial diversity, ENHANCED dari 25%)
        // Atau jika diversity rendah RELATIF terhadap fitness (untuk puzzle besar)
        boolean diversityLow = (currentDiversity < initialDiversity * 0.30);
        // ENHANCED: Untuk puzzle besar dengan fitness tinggi, butuh diversity lebih tinggi
        if (bestFitness > 15.0 && currentDiversity < initialDiversity * 0.40) {
            diversityLow = true;  // Lebih sensitif untuk puzzle besar
        }
        
        // 3. Populasi terlalu homogen (std dev fitness < 0.8, ENHANCED dari 0.6)
        // Atau jika std dev rendah RELATIF terhadap fitness (untuk puzzle besar)
        boolean avgFitnessStagnant = (fitnessStdDev < 0.8);
        // ENHANCED: Untuk puzzle besar, perlu std dev lebih tinggi
        if (bestFitness > 15.0 && fitnessStdDev < bestFitness * 0.05) {
            avgFitnessStagnant = true;  // Lebih sensitif untuk puzzle besar
        }
        
        // 4. Improvement rate terlalu rendah (ENHANCED)
        // ENHANCED: Threshold lebih sensitif untuk puzzle besar
        double improvementThreshold = (bestFitness > 15.0) ? 0.002 : 0.001;
        boolean lowImprovementRate = (improvementRate < improvementThreshold && currentGeneration > maxGenerations * 0.05);
        
        // ENHANCED: Stagnasi juga jika tidak ada improvement dalam beberapa generasi berturut-turut
        // Untuk puzzle besar, jika stagnasi count > 5, langsung deteksi stagnasi
        boolean consecutiveNoImprovement = (stagnationCount > 5 && bestFitness > 10.0);
        
        // Stagnasi jika fitness tidak membaik DAN 
        // (diversity rendah ATAU populasi homogen ATAU improvement rate rendah ATAU consecutive no improvement)
        return fitnessStagnant && (diversityLow || avgFitnessStagnant || lowImprovementRate || consecutiveNoImprovement);
    }
    
    /**
     * Early stopping dengan multiple criteria (IMPROVED)
     * Berhenti lebih awal jika sudah jelas tidak akan menemukan solusi
     * 
     * @return true jika harus berhenti lebih awal
     */
    private boolean shouldStopEarly() {
        // ENHANCED: Untuk large population strategy, early stop perlu lebih hati-hati
        // Karena max generation kecil (500), kita perlu lebih banyak kesempatan
        
        // Jangan early stop terlalu cepat
        // ENHANCED: Untuk large population, beri lebih banyak kesempatan (20% dari maxGen)
        double earlyStopThreshold = (populationSize > maxGenerations) ? 0.2 : 0.1;
        if (currentGeneration < maxGenerations * earlyStopThreshold) {
            return false;
        }
        
        // Early stop jika:
        // 1. Fitness sudah sangat baik (sudah dekat solusi)
        if (bestFitness < 0.1 && averageFitness < 1.0) {
            // Sudah sangat dekat, beri kesempatan lebih banyak
            return false;
        }
        
        // 2. Improvement rate sangat rendah DAN diversity sangat rendah (ENHANCED)
        // ENHANCED: Untuk large population, threshold lebih rendah (lebih banyak kesempatan)
        double improvementThreshold = (populationSize > maxGenerations) ? 0.0003 : 0.0005;
        double diversityThreshold = (populationSize > maxGenerations) ? 0.10 : 0.15;
        double generationThreshold = (populationSize > maxGenerations) ? 0.6 : 0.5;
        
        if (improvementRate < improvementThreshold && currentDiversity < initialDiversity * diversityThreshold 
            && currentGeneration > maxGenerations * generationThreshold) {
            return true;  // Kemungkinan besar tidak akan menemukan solusi
        }
        
        // 3. Stagnasi sangat lama tanpa improvement (ENHANCED)
        // ENHANCED: Untuk large population, threshold lebih tinggi (lebih banyak kesempatan)
        double stagnationThreshold = (populationSize > maxGenerations) ? 0.4 : 0.3;
        if (stagnationCount > maxGenerations * stagnationThreshold && bestFitness > 5.0) {
            return true;  // Stagnasi terlalu lama tanpa kemajuan
        }
        
        return false;
    }
    
    /**
     * Menghitung adaptive crossover rate berdasarkan progress dan diversity
     * - Early stage (high diversity): Higher crossover rate untuk eksplorasi
     * - Late stage (low diversity): Lower crossover rate untuk eksploitasi
     */
    private double calculateAdaptiveCrossoverRate() {
        // Base crossover rate
        double adaptiveRate = originalCrossoverRate;
        
        // Factor 1: Berdasarkan diversity (semakin rendah diversity, semakin rendah crossover)
        // Diversity tinggi → lebih banyak crossover untuk eksplorasi
        // Diversity rendah → lebih sedikit crossover untuk eksploitasi
        double diversityFactor = currentDiversity / initialDiversity;
        diversityFactor = Math.max(0.5, Math.min(1.5, diversityFactor));  // Clamp 0.5-1.5
        
        // Factor 2: Berdasarkan progress (semakin dekat solusi, semakin rendah crossover)
        // Progress: normalized fitness improvement
        // ENHANCED: Fine-tuning mode - jika fitness sangat dekat, tingkatkan crossover untuk fine-tuning
        double progressFactor = 1.0;
        if (bestFitness < 15.0) {
            // Fine-tuning mode: tingkatkan crossover untuk fine-tuning
            // Fitness sangat dekat → lebih banyak crossover untuk fine-tuning
            if (bestFitness < 2.0) {
                progressFactor = 1.15;  // Sangat dekat: tingkatkan crossover 15%
            } else if (bestFitness < 5.0) {
                progressFactor = 1.10;  // Dekat: tingkatkan crossover 10%
            } else {
                progressFactor = 1.05;  // Mendekati: tingkatkan crossover 5%
            }
        } else if (bestFitness > 0 && averageFitness > 0) {
            double fitnessRatio = bestFitness / (averageFitness + 1.0);
            // Jika best fitness sudah jauh lebih baik dari average, kurangi crossover
            progressFactor = 0.7 + (0.3 * fitnessRatio);  // Range: 0.7-1.0
        }
        
        // Combine factors
        adaptiveRate = originalCrossoverRate * diversityFactor * progressFactor;
        
        // Clamp: 0.5 - 0.95 (crossover rate tidak boleh terlalu rendah atau terlalu tinggi)
        return Math.max(0.5, Math.min(0.95, adaptiveRate));
    }
    
    /**
     * Menghitung dynamic elitism berdasarkan diversity dan progress
     * - High diversity: Lower elitism untuk eksplorasi
     * - Low diversity: Higher elitism untuk eksploitasi
     * - Stagnation: Temporary reduction untuk eksplorasi
     */
    private int calculateDynamicElitism() {
        int dynamicElitism = originalElitismCount;
        
        // ENHANCED: Untuk large population strategy (pop > maxGen), elitism perlu disesuaikan
        boolean isLargePopulation = (populationSize > maxGenerations);
        
        // Factor 1: Stagnation (priority)
        if (stagnationCount > 20) {
            // Temporary elitism reduction untuk eksplorasi lebih banyak
            // ENHANCED: Untuk large population, reduction lebih kecil (keep more elite)
            double reductionFactor = isLargePopulation ? 0.6 : 0.5;
            dynamicElitism = Math.max(1, (int)(originalElitismCount * reductionFactor));
            return dynamicElitism;
        }
        
        // Factor 2: Berdasarkan diversity (diversity rendah → lebih banyak elitism)
        double diversityRatio = currentDiversity / initialDiversity;
        if (diversityRatio < 0.3) {
            // Diversity sangat rendah: tingkatkan elitism untuk mempertahankan solusi baik
            dynamicElitism = (int) (originalElitismCount * 1.2);
        } else if (diversityRatio > 0.7) {
            // Diversity tinggi: kurangi elitism untuk eksplorasi lebih banyak
            // ENHANCED: Untuk large population, reduction lebih kecil
            double reductionFactor = isLargePopulation ? 0.85 : 0.8;
            dynamicElitism = Math.max(1, (int) (originalElitismCount * reductionFactor));
        }
        
        // Factor 3: Berdasarkan progress (jika sudah dekat solusi, lebih banyak elitism)
        // ENHANCED: Fine-tuning mode lebih agresif untuk puzzle 6x6 dan 7x7
        if (bestFitness < 15.0) {
            // Fine-tuning mode: sangat dekat solusi (6x6 biasanya < 15, 7x7 biasanya < 8)
            // Tingkatkan elitism secara signifikan untuk mempertahankan solusi baik
            int additionalElitism;
            if (bestFitness < 2.0) {
                // Sangat dekat solusi (hampir sempurna): elitism sangat tinggi
                additionalElitism = isLargePopulation ? 8 : 5;
            } else if (bestFitness < 5.0) {
                // Dekat solusi (7x7 biasanya di sini): elitism tinggi
                additionalElitism = isLargePopulation ? 6 : 4;
            } else {
                // Mendekati solusi (6x6 biasanya di sini): elitism moderate
                additionalElitism = isLargePopulation ? 4 : 3;
            }
            int maxElitism = isLargePopulation ? populationSize / 6 : populationSize / 8; // ENHANCED: Max elitism lebih tinggi
            dynamicElitism = Math.min(maxElitism, dynamicElitism + additionalElitism);
        } else if (bestFitness < 5.0 && averageFitness < 10.0) {
            // Sudah dekat solusi: tingkatkan elitism untuk fine-tuning
            // ENHANCED: Untuk large population, max elitism lebih tinggi
            int maxElitism = isLargePopulation ? populationSize / 8 : populationSize / 10;
            dynamicElitism = Math.min(maxElitism, dynamicElitism + 2);
        }
        
        // ENHANCED: Clamp - untuk large population, max elitism lebih tinggi (30% vs 25% untuk fine-tuning)
        int maxElitismPercent = (bestFitness < 15.0) ? (isLargePopulation ? 30 : 25) : (isLargePopulation ? 25 : 20);
        return Math.max(1, Math.min(populationSize * maxElitismPercent / 100, dynamicElitism));
    }
    
    private void createNewGeneration() {
        sortPopulation();
        Chromosome[] newPopulation = new Chromosome[populationSize];
        
        // Dynamic Elitism (IMPROVED - adaptive berdasarkan diversity dan progress)
        int currentElitism = calculateDynamicElitism();
        
        for (int i = 0; i < currentElitism && i < populationSize; i++) {
            newPopulation[i] = population[i].clone();
        }
        
        // Adaptive Crossover Rate (IMPROVED)
        double adaptiveCrossoverRate = calculateAdaptiveCrossoverRate();
        
        // Generate offspring
        for (int i = currentElitism; i < populationSize; i += 2) {
            Chromosome parent1 = selectionStrategy.select(population, fitnessFunction, random);
            Chromosome parent2 = selectionStrategy.select(population, fitnessFunction, random);
            
            Chromosome[] offspring;
            if (random.nextDouble() < adaptiveCrossoverRate) {
                offspring = crossoverStrategy.crossover(parent1, parent2, random);
            } else {
                offspring = new Chromosome[]{parent1.clone(), parent2.clone()};
            }
            
            // Mutation dengan adaptive rate (IMPROVED)
            for (Chromosome child : offspring) {
                if (mutationStrategy instanceof AdaptiveMutation) {
                    ((AdaptiveMutation) mutationStrategy).updateGeneration(currentGeneration);
                    ((AdaptiveMutation) mutationStrategy).updatePopulation(population);
                }
                mutationStrategy.mutate(child, mutationRate, random);
            }
            
            if (i < populationSize) {
                newPopulation[i] = offspring[0];
            }
            if (i + 1 < populationSize) {
                newPopulation[i + 1] = offspring[1];
            }
        }
        
        population = newPopulation;
        evaluatePopulation();
        updateBestChromosome();
        updatePopulationStatistics();
        currentGeneration++;
    }
    
    public Chromosome run() {
        initializePopulation();
        
        System.out.println("\n=== Improved Genetic Algorithm (Pure GA with Enhancements) ===");
        System.out.println("Enhancements: Weighted Fitness, Advanced Heuristics, Anti-Stagnation Mechanisms");
        System.out.println("Generation 0 - Best: " + String.format("%.2f", bestFitness) +
            " - Avg: " + String.format("%.2f", averageFitness) +
            " - Diversity: " + String.format("%.3f", currentDiversity));
        
        int escapeAttempts = 0;
        int maxEscapeAttempts = 5;
        
        while (currentGeneration < maxGenerations && bestFitness > 0.0) {
            createNewGeneration();
            
            // Check improvement dan update improvement rate (IMPROVED)
            if (bestFitness < previousBestFitness - 0.0001) {
                // Ada improvement, hitung improvement rate
                double improvement = previousBestFitness - bestFitness;
                double improvementRatio = (previousBestFitness > 0) ? improvement / previousBestFitness : 0.0;
                
                // Update improvement rate (exponential moving average)
                improvementRate = (improvementRate * 0.7) + (improvementRatio * 0.3);
                
                previousBestFitness = bestFitness;
                stagnationCount = 0;
            } else {
                // Tidak ada improvement, kurangi improvement rate
                improvementRate *= 0.95;
                stagnationCount++;
            }
            
            // Early stopping check (IMPROVED)
            if (shouldStopEarly()) {
                System.out.println("\n[Early Stop] Stopping early due to low progress probability");
                System.out.println("  Best Fitness: " + String.format("%.2f", bestFitness));
                System.out.println("  Improvement Rate: " + String.format("%.6f", improvementRate));
                System.out.println("  Diversity: " + String.format("%.3f", currentDiversity));
                break;
            }
            
            // ENHANCED: Protect best solutions - jangan trigger escape jika sudah sangat dekat solusi
            // Fine-tuning mode: jika fitness sangat dekat (6x6 < 15, 7x7 < 8), jangan gunakan escape yang agresif
            boolean isFineTuningMode = (bestFitness < 15.0);
            
            // Check stagnation dengan deteksi yang lebih cerdas dan sensitif (ENHANCED)
            if (isStagnating() && !isFineTuningMode) {
                // ENHANCED: Escape mechanism lebih agresif dan trigger lebih cepat
                // OPTIMIZED: Untuk large population strategy, trigger lebih cepat karena maxGen lebih kecil relatif
                int triggerInterval = 8;  // OPTIMIZED: Default lebih cepat (dari 10) untuk large pop strategy
                if (maxGenerations > 2000) triggerInterval = 6;   // ENHANCED: Lebih agresif
                if (maxGenerations > 4000) triggerInterval = 5;   // ENHANCED: Sangat agresif
                
                // OPTIMIZED: Untuk large population (pop > maxGen), trigger lebih cepat
                if (populationSize > maxGenerations) {
                    triggerInterval = Math.max(5, triggerInterval - 2);  // OPTIMIZED: Lebih cepat untuk large pop
                }
                
                // ENHANCED: Jika improvement rate rendah atau fitness tinggi, trigger lebih cepat
                if (improvementRate < 0.002) {
                    triggerInterval = Math.max(4, triggerInterval - 3);  // OPTIMIZED: Minimum 4 (dari 5)
                }
                if (bestFitness > 15.0) {
                    triggerInterval = Math.max(5, triggerInterval - 2);  // ENHANCED: Untuk puzzle besar
                }
                
                // ENHANCED: Trigger juga berdasarkan consecutive stagnation
                if (stagnationCount > 8) {  // OPTIMIZED: Threshold lebih rendah (dari 10)
                    triggerInterval = Math.max(3, triggerInterval / 2);  // OPTIMIZED: Lebih agresif, minimum 3
                }
                
                // ENHANCED: Escape mechanism berdasarkan tingkat stagnasi dan kondisi populasi
                // OPTIMIZED: Trigger lebih selektif - hanya jika benar-benar perlu
                boolean shouldTriggerEscape = (stagnationCount % triggerInterval == 0);
                
                // ENHANCED: Juga trigger jika diversity sangat rendah ATAU improvement rate sangat rendah
                if (currentDiversity < initialDiversity * 0.15 || improvementRate < 0.0001) {
                    shouldTriggerEscape = true;  // Force trigger untuk diversity/improvement sangat rendah
                }
                
                // ENHANCED: Skip escape jika baru saja ada improvement kecil (beri kesempatan)
                if (bestFitness < previousBestFitness - 0.001 && stagnationCount < 10) {
                    shouldTriggerEscape = false;  // Skip jika ada improvement kecil baru saja
                }
                
                if (shouldTriggerEscape && escapeAttempts < maxEscapeAttempts) {
                    System.out.println("\n[Stagnation " + stagnationCount + "] Applying EXPLORATION-focused escape mechanism...");
                    System.out.println("  → Improvement Rate: " + String.format("%.6f", improvementRate) + 
                                     ", Diversity: " + String.format("%.3f", currentDiversity) +
                                     " (" + String.format("%.1f", (currentDiversity / initialDiversity * 100)) + "% of initial)");
                    
                    // ENHANCED: Threshold lebih rendah untuk trigger lebih cepat
                    // OPTIMIZED: Untuk fitness hampir solved (fitness < 5), gunakan escape lebih konservatif
                    if (bestFitness < 5.0 && stagnationCount <= 15) {
                        // Near solution: hanya mutation boost (jangan terlalu agresif)
                        escapeMutationBoost();
                        String explorationPercent = (populationSize > maxGenerations) ? "15%" : "10%";
                        System.out.println("  → [Near Solution] Mutation boost + Random exploration (" + explorationPercent + " new random)");
                    } else if (stagnationCount <= 20) {  // ENHANCED: Dari 30 ke 20
                        // Early stagnation: mutation boost + random exploration
                        escapeMutationBoost();
                        String explorationPercent = (populationSize > maxGenerations) ? "15%" : "10%";
                        System.out.println("  → Mutation boost + Random exploration (" + explorationPercent + " new random) + Temp elitism reduction");
                    } else if (stagnationCount <= 40) {  // ENHANCED: Threshold dinaikkan sedikit (dari 35)
                        // Medium stagnation: aggressive diversity injection + hybrid creation
                        escapeDiversityInjection();
                        String replacePercent = (populationSize > maxGenerations) ? "65-70%" : "55-65%";
                        System.out.println("  → Aggressive diversity injection (" + replacePercent + " replaced) + Hybrid chromosomes");
                    } else {
                        // Severe stagnation: aggressive partial restart + population expansion
                        escapePartialRestart();
                        String replacePercent = (populationSize > maxGenerations) ? "90%" : "85%";
                        System.out.println("  → Aggressive partial restart (" + replacePercent + " replaced, keep best 1 only) + Population expansion");
                        escapeAttempts++;
                    }
                    
                    updatePopulationStatistics();
                    System.out.println("  → New diversity: " + String.format("%.3f", currentDiversity) + 
                                     " (was: " + String.format("%.3f", initialDiversity * 0.2) + ")");
                    
                    // Reset stagnation count setelah escape
                    stagnationCount = 0;
                    improvementRate = 0.01;  // Reset improvement rate setelah escape
                }
            }
            
            // Print progress dengan adaptive parameters info (IMPROVED)
            if (currentGeneration % 10 == 0 || bestFitness == 0.0) {
                String stagIndicator = (stagnationCount > 0) ? " [Stag:" + stagnationCount + "]" : "";
                String improvementInfo = String.format(" [ImpRate:%.4f]", improvementRate);
                System.out.println("Gen " + currentGeneration + 
                    " - Best: " + String.format("%.2f", bestFitness) +
                    " - Avg: " + String.format("%.2f", averageFitness) +
                    " - Div: " + String.format("%.3f", currentDiversity) +
                    improvementInfo + stagIndicator);
            }
            
            if (bestFitness == 0.0) {
                System.out.println("\n*** SOLUTION FOUND at generation " + currentGeneration + "! ***");
                break;
            }
        }
        
        System.out.println("\n=== Results ===");
        System.out.println("Final Generation: " + currentGeneration);
        System.out.println("Best Fitness: " + String.format("%.2f", bestFitness));
        System.out.println("Solution Found: " + (bestFitness == 0.0 ? "YES" : "NO"));
        if (escapeAttempts > 0) {
            System.out.println("Escape Attempts: " + escapeAttempts);
        }
        
        return bestChromosome;
    }
    
    public Chromosome getBestChromosome() { return bestChromosome; }
    public double getBestFitness() { return bestFitness; }
    public int getCurrentGeneration() { return currentGeneration; }
    public boolean isSolutionFound() { return bestFitness == 0.0; }
    public double getCurrentDiversity() { return currentDiversity; }
    public double getAverageFitness() { return averageFitness; }
    public double getFitnessStdDev() { return fitnessStdDev; }
    
    // Getters untuk parameter (untuk tracking dan dokumentasi)
    public int getPopulationSize() { return populationSize; }
    public double getCrossoverRate() { return crossoverRate; }  // Returns original rate
    public double getMutationRate() { return mutationRate; }    // Returns original rate
    public int getMaxGenerations() { return maxGenerations; }
    public int getElitismCount() { return elitismCount; }       // Returns original elitism
    public double getImprovementRate() { return improvementRate; }
}

