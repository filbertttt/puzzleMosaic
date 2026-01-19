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
        // Menggunakan linear scaling untuk memperbaiki selection pressure
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
        
        // Linear scaling: transform fitness untuk selection pressure yang lebih baik
        // Jika fitness range kecil, gunakan linear scaling
        // Jika fitness range besar, gunakan inverse fitness
        double fitnessRange = maxFitness - minFitness;
        
        if (fitnessRange < 0.001 || maxFitness < 0.001) {
            // Semua fitness hampir sama atau sudah dekat solusi: uniform selection
            double uniformProb = 1.0 / population.length;
            for (int i = 0; i < probabilities.length; i++) {
                probabilities[i] = uniformProb;
            }
            totalFitness = 1.0;
        } else {
            // Linear scaling: f_scaled = maxFitness - fitness + c
            // c adalah offset kecil untuk memastikan semua memiliki probabilitas positif
            double c = fitnessRange * 0.1;  // 10% dari range sebagai offset
            double scaledMax = maxFitness + c;
            
            for (int i = 0; i < population.length; i++) {
                double fitness = population[i].getFitness();
                // Scaled fitness: semakin kecil fitness, semakin besar scaled value
                double scaledFitness = scaledMax - fitness + c;
                
                // Exponential scaling untuk selection pressure yang lebih agresif
                // Adaptive exponent berdasarkan fitness range untuk balance yang lebih baik
                // Lebih agresif untuk fitness range besar (puzzle besar)
                // Fine-tuning mode - jika fitness sangat dekat, tingkatkan selection pressure
                double exponent;
                boolean isFineTuningMode = (minFitness < 15.0 && fitnessRange < 20.0);
                
                if (isFineTuningMode) {
                    // Fine-tuning mode: sangat agresif selection pressure untuk fine-tuning
                    if (minFitness < 2.0) {
                        exponent = 4.0;  // Sangat agresif untuk solusi hampir sempurna
                    } else if (minFitness < 5.0) {
                        exponent = 3.5;  // Agresif untuk solusi dekat
                    } else {
                        exponent = 3.0;  // Agresif untuk solusi mendekati
                    }
                } else if (fitnessRange < 1.0) {
                    // Range kecil: gunakan exponential yang lebih agresif
                    exponent = 2.5;  // Exponential untuk selection pressure tinggi
                } else if (fitnessRange < 5.0) {
                    // Range sedang: moderate exponential
                    exponent = 2.0;
                } else if (fitnessRange < 20.0) {
                    // Range besar (puzzle sedang): lebih agresif (dari 1.5 ke 1.8)
                    exponent = 1.8;
                } else {
                    // Range sangat besar (puzzle besar): tetap agresif (dari 1.5 ke 1.6)
                    exponent = 1.6;  // Tetap agresif untuk puzzle besar
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
        
        // Roulette wheel: pilih berdasarkan probabilitas
        double randomValue = random.nextDouble();
        double cumulativeProbability = 0.0;
        
        for (int i = 0; i < population.length; i++) {
            cumulativeProbability += probabilities[i];
            if (randomValue <= cumulativeProbability) {
                return population[i].clone();
            }
        }
        
        // Fallback: kembalikan yang terakhir (seharusnya tidak terjadi)
        return population[population.length - 1].clone();
    }
}

