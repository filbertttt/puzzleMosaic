import java.util.Random;

public class AdaptiveMutation implements MutationStrategy {
    //Mutation rate awal (untuk eksplorasi)
    private double initialMutationRate;
    
    //Mutation rate akhir (untuk eksploitasi)
    private double finalMutationRate;
    
    //Generasi saat ini
    private int currentGeneration;
    
    //Maksimal generasi
    private int maxGenerations;
    
    //Populasi saat ini (untuk menghitung diversity)
    private Chromosome[] population;
    
    //Constructor
    public AdaptiveMutation(double initialMutationRate, double finalMutationRate, int maxGenerations) {
        this.initialMutationRate = initialMutationRate;
        this.finalMutationRate = finalMutationRate;
        this.maxGenerations = maxGenerations;
        this.currentGeneration = 0;
    }
    
    //Update generasi saat ini
    public void updateGeneration(int generation) {
        this.currentGeneration = Math.max(0, generation);
        // Pastikan tidak melebihi max generations
        if (this.currentGeneration > maxGenerations) {
            this.currentGeneration = maxGenerations;
        }
    }
    
    //Update populasi saat ini
    public void updatePopulation(Chromosome[] population) {
        this.population = population;
    }
    
    //Menghitung mutation rate adaptif berdasarkan generasi, diversity, dan progress
    private double calculateAdaptiveMutationRate() {
        // Faktor 1: Berdasarkan generasi
        double progress = (double) currentGeneration / maxGenerations;
        double generationFactor = Math.pow(1.0 - progress, 0.7);
        
        // Faktor 2: Berdasarkan diversity 
        // jika diversity rendah, tingkatkan mutation
        double diversityFactor = 1.0;
        if (population != null && population.length > 1) {
            double diversity = DiversityMeasure.calculateDiversity(population);
            double fitnessStdDev = DiversityMeasure.calculateFitnessDiversity(population);
            
            // Jika diversity rendah, tingkatkan mutation rate lebih agresif
            if (diversity < 0.15) {
                diversityFactor = 1.0 + (0.15 - diversity) * 4.0;
            } else if (diversity < 0.25) {
                diversityFactor = 1.0 + (0.25 - diversity) * 3.5;
            } else if (diversity < 0.4) {
                diversityFactor = 1.0 + (0.4 - diversity) * 1.5;
            }
            
            // Jika fitness std dev rendah (populasi homogen), tingkatkan mutation lebih agresif
            if (fitnessStdDev < 0.2) {
                diversityFactor *= 1.5;
            } else if (fitnessStdDev < 0.3) {
                diversityFactor *= 1.3;
            }
        }
        
        // Faktor 3: Berdasarkan progress
        // Jika fitness sangat dekat, kurangi mutation
        double progressFactor = 1.0;
        
        // Fine-tuning mode detection
        // Jika fitness sangat dekat, kurangi mutation  
        boolean isFineTuningMode = false;
        double currentBestFitness = Double.MAX_VALUE;
        if (population != null && population.length > 0) {
            for (Chromosome c : population) {
                if (c.getFitness() < currentBestFitness) {
                    currentBestFitness = c.getFitness();
                }
            }
            isFineTuningMode = (currentBestFitness < 15.0);
        }
        
        if (isFineTuningMode) {
            if (currentBestFitness < 2.0) {
                progressFactor = 0.5;
            } else if (currentBestFitness < 5.0) {
                progressFactor = 0.65;
            } else {
                progressFactor = 0.75;
            }
        } else if (progress < 0.4) {
            progressFactor = 1.0 + (0.4 - progress) * 0.8;
        } else if (progress > 0.7) {
            progressFactor = 1.0 - (progress - 0.7) * 0.4;
        }
        
        // Faktor 4 - Stagnation-based boost
        // Jika populasi homogen (diversity sangat rendah), naikkan mutation rate
        if (population != null && population.length > 1) {
            double diversity = DiversityMeasure.calculateDiversity(population);
            if (diversity < 0.15) {
                progressFactor *= 1.6;
            } else if (diversity < 0.25) {
                progressFactor *= 1.4;
            } else if (diversity < 0.35) {
                progressFactor *= 1.15;
            }
        }
        
        // Gabungkan semua faktor untuk mendapatkan mutation rate adaptif
        double adaptiveRate = initialMutationRate * generationFactor * diversityFactor * progressFactor;
        
        // Pastikan tidak melebihi batas
        adaptiveRate = Math.max(finalMutationRate, Math.min(initialMutationRate * 2.5, adaptiveRate));
        
        return adaptiveRate;
    }
    
    //Melakukan mutation dengan rate adaptif
    @Override
    public void mutate(Chromosome chromosome, double mutationRate, Random random) {
        double adaptiveRate = calculateAdaptiveMutationRate();
        
        // Lakukan mutation dengan rate adaptif
        for (int i = 0; i < chromosome.getLength(); i++) {
            if (random.nextDouble() < adaptiveRate) {
                boolean currentValue = chromosome.getGene(i);
                chromosome.setGene(i, !currentValue);
            }
        }
    }
    
    //Mendapatkan mutation rate saat ini
    public double getCurrentMutationRate() {
        return calculateAdaptiveMutationRate();
    }
}

