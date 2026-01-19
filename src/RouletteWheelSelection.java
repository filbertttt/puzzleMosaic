import java.util.Random;
public class RouletteWheelSelection implements SelectionStrategy {
    //Memilih parent menggunakan Roulette Wheel Selection
    @Override
    public Chromosome select(Chromosome[] population, FitnessFunction fitnessFunction, Random random) {
        // Hitung fitness untuk semua kromosom yang belum dihitung
        for (Chromosome chromosome : population) {
            if (!chromosome.isFitnessCalculated()) {
                fitnessFunction.calculateFitness(chromosome);
            }
        }
        
        // Hitung total fitness dengan Improved fitness scaling
        double totalFitness = 0.0;
        double[] probabilities = new double[population.length];
        
        // Find min and max fitness untuk scaling
        double minFitness = Double.MAX_VALUE;
        double maxFitness = Double.MIN_VALUE;
        for (Chromosome chromosome : population) {
            double fit = chromosome.getFitness();
            if (fit < minFitness) minFitness = fit;
            if (fit > maxFitness) maxFitness = fit;
        }
        
        // Linear scaling
        double fitnessRange = maxFitness - minFitness;
        
        if (fitnessRange < 0.001 || maxFitness < 0.001) {
            // uniform selection
            double uniformProb = 1.0 / population.length;
            for (int i = 0; i < probabilities.length; i++) {
                probabilities[i] = uniformProb;
            }
            totalFitness = 1.0;
        } else {
            // Linear scaling
            double c = fitnessRange * 0.1;  
            double scaledMax = maxFitness + c;
            
            for (int i = 0; i < population.length; i++) {
                double fitness = population[i].getFitness();
                // Scaled fitness
                double scaledFitness = scaledMax - fitness + c;
                
                double exponent;
                boolean isFineTuningMode = (minFitness < 15.0 && fitnessRange < 20.0);
                
                if (isFineTuningMode) {
                    if (minFitness < 2.0) {
                        exponent = 4.0;  
                    } else if (minFitness < 5.0) {
                        exponent = 3.5;  
                    } else {
                        exponent = 3.0;  
                    }
                } else if (fitnessRange < 1.0) {
                    exponent = 2.5;  
                } else if (fitnessRange < 5.0) {
                    exponent = 2.0;
                } else if (fitnessRange < 20.0) {
                    exponent = 1.8;
                } else {
                    exponent = 1.6;  
                }
                scaledFitness = Math.pow(scaledFitness, exponent);
                
                probabilities[i] = scaledFitness;
                totalFitness += scaledFitness;
            }
        }
        
        // Normalisasi probabilitas
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= totalFitness;
        }
        
        // Roulette wheel
        double randomValue = random.nextDouble();
        double cumulativeProbability = 0.0;
        
        for (int i = 0; i < population.length; i++) {
            cumulativeProbability += probabilities[i];
            if (randomValue <= cumulativeProbability) {
                return population[i].clone();
            }
        }
        
        // Fallback, kembalikan yang terakhir
        return population[population.length - 1].clone();
    }
}

