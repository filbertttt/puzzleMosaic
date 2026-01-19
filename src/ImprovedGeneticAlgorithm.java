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
    
    // Adaptive parameters
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
        this.crossoverRate = crossoverRate;  
        this.mutationRate = mutationRate;    
        this.maxGenerations = maxGenerations;
        this.elitismCount = elitismCount;   
        this.selectionStrategy = selectionStrategy;
        this.crossoverStrategy = crossoverStrategy;
        this.mutationStrategy = mutationStrategy;
        this.random = new Random(seed);
        
        this.fitnessFunction = new FitnessFunction(puzzle);
        this.heuristics = new Heuristics(puzzle);
        this.currentGeneration = 0;
        this.bestFitness = Double.MAX_VALUE;
        this.stagnationCount = 0;
        this.originalElitismCount = elitismCount;  
        
        this.originalCrossoverRate = crossoverRate;
        this.originalMutationRate = mutationRate;
        this.improvementRate = 1.0;  
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
        int puzzleSize = puzzle.getWidth() * puzzle.getHeight();
        int hintCount = puzzle.getHintPositions().length;
        double hintDensity = (double) hintCount / puzzleSize;  
        
        // Adaptive heuristic percentage: semakin padat hints, semakin banyak heuristics
        double heuristicPercent = 0.15 + (hintDensity * 0.25);  
        heuristicPercent = Math.min(0.40, Math.max(0.15, heuristicPercent));  
        
        //batasi heuristics untuk diversity, jika puzzle kecil
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
        
        // Print fitness awal
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
        // Hitung diversity, untuk large population, hitung lebih jarang (setiap 5 generasi)
        boolean shouldCalculateDiversity = true;
        if (populationSize > 300) {
            shouldCalculateDiversity = (currentGeneration % 5 == 0 || currentGeneration == 0);
        }
        
        if (shouldCalculateDiversity) {
            currentDiversity = DiversityMeasure.calculateDiversity(population);
        }

        //menghitung rata-rata fitness
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
    
    //Escape mechanism 1: Mutation boost + Random exploration
    private void escapeMutationBoost() {
        //untuk large population, reduction lebih kecil
        boolean isLargePopulation = (populationSize > maxGenerations);
        double reductionFactor = isLargePopulation ? 0.6 : 0.5;
        int tempElitism = Math.max(1, (int)(originalElitismCount * reductionFactor));
        
        //Mutation boost untuk large population
        double boostMultiplier = isLargePopulation ? 5.0 : 4.0;
        double boostRate = Math.min(0.25, originalMutationRate * boostMultiplier);
        for (int i = tempElitism; i < populationSize; i++) {
            mutationStrategy.mutate(population[i], boostRate, random);
        }
        
        //Random exploration boost untuk large population
        double explorationPercent = isLargePopulation ? 0.15 : 0.10;
        int randomExplorationCount = Math.max(1, (int)(populationSize * explorationPercent));
        sortPopulation();  //untuk tahu yang terburuk
        for (int i = populationSize - randomExplorationCount; i < populationSize; i++) {
            // Ganti dengan kromosom random baru
            population[i] = new Chromosome(puzzle.getWidth(), puzzle.getHeight(), random);
        }
        
        evaluatePopulation();
        updateBestChromosome();
    }
    
    //Escape mechanism 2: Aggressive diversity injection + Hybrid creation
    private void escapeDiversityInjection() {
        sortPopulation();  //untuk tahu yang terburuk
        
        //untuk large population, replace lebih banyak
        boolean isLargePopulation = (populationSize > maxGenerations);
        double replacePercent;
        if (isLargePopulation) {
            //Large population butuh lebih banyak diversity injection
            replacePercent = (bestFitness > 15.0) ? 0.70 : 0.65;
        } else {
            replacePercent = (bestFitness > 15.0) ? 0.65 : 0.55;
        }
        int replaceCount = (int) (populationSize * replacePercent);
        
        for (int i = populationSize - replaceCount; i < populationSize; i++) {
            if (random.nextDouble() < 0.5) {
                // Random baru
                population[i] = new Chromosome(puzzle.getWidth(), puzzle.getHeight(), random);
            } else {
                // Hybrid chromosome
                population[i] = createHybridChromosome();
            }
        }
        
        evaluatePopulation();
        updateBestChromosome();
    }
    
    //Membuat kromosom hybrid dari kombinasi beberapa individu
    private Chromosome createHybridChromosome() {
        // Pilih 2-3 kromosom random dari populasi
        int numParents = 2 + random.nextInt(2);  
        Chromosome[] parents = new Chromosome[numParents];
        
        for (int i = 0; i < numParents; i++) {
            parents[i] = population[random.nextInt(populationSize)];
        }
        
        // Buat hybrid, untuk setiap posisi, pilih gen dari salah satu parent secara random
        Chromosome hybrid = new Chromosome(puzzle.getWidth(), puzzle.getHeight(), random);
        for (int i = 0; i < hybrid.getLength(); i++) {
            Chromosome selectedParent = parents[random.nextInt(numParents)];
            hybrid.setGene(i, selectedParent.getGene(i));
        }
        
        // Tambah sedikit mutasi
        for (int i = 0; i < hybrid.getLength(); i++) {
            if (random.nextDouble() < 0.1) {  
                hybrid.setGene(i, !hybrid.getGene(i));
            }
        }
        
        return hybrid;
    }
    
    //Escape mechanism 3: Aggressive partial restart + Population expansion
    private void escapePartialRestart() {
        sortPopulation();  //untuk tahu yang terbaik
        
        // Simpan best chromosome
        Chromosome savedBest = bestChromosome.clone();
        
        //untuk large population, replace lebih banyak
        boolean isLargePopulation = (populationSize > maxGenerations);
        double replacePercent = isLargePopulation ? 0.90 : 0.85;
        int replaceCount = (int) (populationSize * replacePercent);
        
        for (int i = 1; i < replaceCount; i++) { 
            if (random.nextDouble() < 0.7) {
                // Random baru
                population[i] = new Chromosome(puzzle.getWidth(), puzzle.getHeight(), random);
            } else {
                // Hybrid chromosome
                population[i] = createHybridChromosome();
            }
        }
        
        // Pastikan best chromosome tetap di index 0
        population[0] = savedBest;
        
        // Temporary population expansion untuk large population
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
        
        // Ambil yang terbaik
        System.arraycopy(expandedPopulation, 0, population, 0, populationSize);
        
        evaluatePopulation();
        updateBestChromosome();
    }
    
    //Deteksi stagnasi
    private boolean isStagnating() {
        // Stagnasi jika fitness tidak membaik
        double fitnessThreshold = (bestFitness > 20.0) ? 0.01 : 
                                 (bestFitness > 10.0) ? 0.005 : 0.0001;
        boolean fitnessStagnant = (Math.abs(bestFitness - previousBestFitness) < fitnessThreshold);
        
        // Diversity terlalu rendah
        boolean diversityLow = (currentDiversity < initialDiversity * 0.30);
        if (bestFitness > 15.0 && currentDiversity < initialDiversity * 0.40) {
            diversityLow = true;  
        }
        
        // Populasi terlalu homogen
        boolean avgFitnessStagnant = (fitnessStdDev < 0.8);
        if (bestFitness > 15.0 && fitnessStdDev < bestFitness * 0.05) {
            avgFitnessStagnant = true;  
        }
        
        // Improvement rate terlalu rendah
        double improvementThreshold = (bestFitness > 15.0) ? 0.002 : 0.001;
        boolean lowImprovementRate = (improvementRate < improvementThreshold && currentGeneration > maxGenerations * 0.05);
        
        // Stagnasi juga jika tidak ada improvement dalam beberapa generasi berturut-turut
        boolean consecutiveNoImprovement = (stagnationCount > 5 && bestFitness > 10.0);
        return fitnessStagnant && (diversityLow || avgFitnessStagnant || lowImprovementRate || consecutiveNoImprovement);
    }
    
    //Early stopping dengan multiple criteria
    private boolean shouldStopEarly() {
        double earlyStopThreshold = (populationSize > maxGenerations) ? 0.2 : 0.1;
        if (currentGeneration < maxGenerations * earlyStopThreshold) {
            return false;
        }
        
        // Early stop jika fitness sudah sangat baik
        if (bestFitness < 0.1 && averageFitness < 1.0) {
            return false;
        }
        
        // Improvement rate sangat rendah dan diversity sangat rendah
        double improvementThreshold = (populationSize > maxGenerations) ? 0.0003 : 0.0005;
        double diversityThreshold = (populationSize > maxGenerations) ? 0.10 : 0.15;
        double generationThreshold = (populationSize > maxGenerations) ? 0.6 : 0.5;
        
        if (improvementRate < improvementThreshold && currentDiversity < initialDiversity * diversityThreshold 
            && currentGeneration > maxGenerations * generationThreshold) {
            return true;  
        }
        
        // Stagnasi sangat lama tanpa improvement
        double stagnationThreshold = (populationSize > maxGenerations) ? 0.4 : 0.3;
        if (stagnationCount > maxGenerations * stagnationThreshold && bestFitness > 5.0) {
            return true;  
        }
        
        return false;
    }
    
    //Menghitung adaptive crossover rate berdasarkan progress dan diversity
    private double calculateAdaptiveCrossoverRate() {
        // Base crossover rate
        double adaptiveRate = originalCrossoverRate;
        
        // Factor 1: Berdasarkan diversity (semakin rendah diversity, semakin rendah crossover)
        double diversityFactor = currentDiversity / initialDiversity;
        diversityFactor = Math.max(0.5, Math.min(1.5, diversityFactor));  
        
        // Factor 2: Berdasarkan progress (semakin dekat solusi, semakin rendah crossover)
        double progressFactor = 1.0;
        if (bestFitness < 15.0) {
            if (bestFitness < 2.0) {
                progressFactor = 1.15;  
            } else if (bestFitness < 5.0) {
                progressFactor = 1.10;  
            } else {
                progressFactor = 1.05;  
            }
        } else if (bestFitness > 0 && averageFitness > 0) {
            double fitnessRatio = bestFitness / (averageFitness + 1.0);
            progressFactor = 0.7 + (0.3 * fitnessRatio);  
        }
        
        adaptiveRate = originalCrossoverRate * diversityFactor * progressFactor;
        
        return Math.max(0.5, Math.min(0.95, adaptiveRate));
    }
    
    //Menghitung dynamic elitism berdasarkan diversity dan progress
    private int calculateDynamicElitism() {
        int dynamicElitism = originalElitismCount;
        
        // Untuk large population strategy (pop > maxGen), elitism perlu disesuaikan
        boolean isLargePopulation = (populationSize > maxGenerations);
        
        // Factor 1: Stagnation 
        if (stagnationCount > 20) {
            // Temporary elitism reduction
            // Untuk large population, reduction lebih kecil
            double reductionFactor = isLargePopulation ? 0.6 : 0.5;
            dynamicElitism = Math.max(1, (int)(originalElitismCount * reductionFactor));
            return dynamicElitism;
        }
        
        // Factor 2: Berdasarkan diversity
        double diversityRatio = currentDiversity / initialDiversity;
        if (diversityRatio < 0.3) {
            // Diversity rendah: tingkatkan elitism
            dynamicElitism = (int) (originalElitismCount * 1.2);
        } else if (diversityRatio > 0.7) {
            // Diversity tinggi: kurangi elitism
            // Untuk large population, reduction lebih kecil
            double reductionFactor = isLargePopulation ? 0.85 : 0.8;
            dynamicElitism = Math.max(1, (int) (originalElitismCount * reductionFactor));
        }
        
        // Factor 3: Berdasarkan progress
        if (bestFitness < 15.0) {
            int additionalElitism;
            if (bestFitness < 2.0) {
                // Sangat dekat solusi: elitism sangat tinggi
                additionalElitism = isLargePopulation ? 8 : 5;
            } else if (bestFitness < 5.0) {
                // Dekat solusi: elitism tinggi
                additionalElitism = isLargePopulation ? 6 : 4;
            } else {
                // Mendekati solusi: elitism moderate
                additionalElitism = isLargePopulation ? 4 : 3;
            }
            int maxElitism = isLargePopulation ? populationSize / 6 : populationSize / 8; 
            dynamicElitism = Math.min(maxElitism, dynamicElitism + additionalElitism);
        } else if (bestFitness < 5.0 && averageFitness < 10.0) {
            // Sudah dekat solusi: tingkatkan elitism
            // Untuk large population, max elitism lebih tinggi
            int maxElitism = isLargePopulation ? populationSize / 8 : populationSize / 10;
            dynamicElitism = Math.min(maxElitism, dynamicElitism + 2);
        }
        
        // Untuk large population, max elitism lebih tinggi
        int maxElitismPercent = (bestFitness < 15.0) ? (isLargePopulation ? 30 : 25) : (isLargePopulation ? 25 : 20);
        return Math.max(1, Math.min(populationSize * maxElitismPercent / 100, dynamicElitism));
    }
    
    private void createNewGeneration() {
        sortPopulation();
        Chromosome[] newPopulation = new Chromosome[populationSize];
        
        // Dynamic Elitism
        int currentElitism = calculateDynamicElitism();
        
        for (int i = 0; i < currentElitism && i < populationSize; i++) {
            newPopulation[i] = population[i].clone();
        }
        
        // Adaptive Crossover Rate
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
            
            // Mutation dengan adaptive rate
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
            
            // Check improvement dan update improvement rate
            if (bestFitness < previousBestFitness - 0.0001) {
                // Ada improvement, hitung improvement rate
                double improvement = previousBestFitness - bestFitness;
                double improvementRatio = (previousBestFitness > 0) ? improvement / previousBestFitness : 0.0;
                
                // Update improvement rate
                improvementRate = (improvementRate * 0.7) + (improvementRatio * 0.3);
                
                previousBestFitness = bestFitness;
                stagnationCount = 0;
            } else {
                // Tidak ada improvement, kurangi improvement rate
                improvementRate *= 0.95;
                stagnationCount++;
            }
            
            // Early stopping check
            if (shouldStopEarly()) {
                System.out.println("\n[Early Stop] Stopping early due to low progress probability");
                System.out.println("  Best Fitness: " + String.format("%.2f", bestFitness));
                System.out.println("  Improvement Rate: " + String.format("%.6f", improvementRate));
                System.out.println("  Diversity: " + String.format("%.3f", currentDiversity));
                break;
            }
            
            // Protect best solutions
            boolean isFineTuningMode = (bestFitness < 15.0);
            
            // Check stagnation
            if (isStagnating() && !isFineTuningMode) {
                int triggerInterval = 8;  
                if (maxGenerations > 2000) triggerInterval = 6;  
                if (maxGenerations > 4000) triggerInterval = 5;  
                
                if (populationSize > maxGenerations) {
                    triggerInterval = Math.max(5, triggerInterval - 2);  
                }
                
                if (improvementRate < 0.002) {
                    triggerInterval = Math.max(4, triggerInterval - 3);  
                }
                if (bestFitness > 15.0) {
                    triggerInterval = Math.max(5, triggerInterval - 2);  
                }
                
                if (stagnationCount > 8) {  
                    triggerInterval = Math.max(3, triggerInterval / 2);  
                }
                
                boolean shouldTriggerEscape = (stagnationCount % triggerInterval == 0);
                
                if (currentDiversity < initialDiversity * 0.15 || improvementRate < 0.0001) {
                    shouldTriggerEscape = true;  
                }
                
                if (bestFitness < previousBestFitness - 0.001 && stagnationCount < 10) {
                    shouldTriggerEscape = false;  
                }
                
                if (shouldTriggerEscape && escapeAttempts < maxEscapeAttempts) {
                    System.out.println("\n[Stagnation " + stagnationCount + "] Applying Exploration focused escape mechanism...");
                    if (bestFitness < 5.0 && stagnationCount <= 15) {
                        // Near solution: hanya mutation boost
                        escapeMutationBoost();
                    } else if (stagnationCount <= 20) {  
                        // Early stagnation: mutation boost + random exploration
                        escapeMutationBoost();
                    } else if (stagnationCount <= 40) {  
                        // Medium stagnation: aggressive diversity injection + hybrid creation
                        escapeDiversityInjection();
                    } else {
                        // Severe stagnation: aggressive partial restart + population expansion
                        escapePartialRestart();
                        escapeAttempts++;
                    }
                    
                    updatePopulationStatistics();
                    
                    // Reset stagnation count setelah escape
                    stagnationCount = 0;
                    improvementRate = 0.01;  // Reset improvement rate setelah escape
                }
            }
            
            // Print progress dengan adaptive parameters info
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
    
    // Getters untuk parameter
    public int getPopulationSize() { return populationSize; }
    public double getCrossoverRate() { return crossoverRate; }  
    public double getMutationRate() { return mutationRate; }    
    public int getMaxGenerations() { return maxGenerations; }
    public int getElitismCount() { return elitismCount; }       
    public double getImprovementRate() { return improvementRate; }
}

