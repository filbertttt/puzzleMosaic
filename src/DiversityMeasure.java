public class DiversityMeasure {
    
    //Menghitung diversity populasi menggunakan Hamming Distance
    public static double calculateDiversity(Chromosome[] population) {
        if (population.length < 2) {
            return 0.0;
        }
        
        int maxSamples = 300; //untuk menghindari O(n²)
        boolean useSampling = (population.length > 100); //untuk populasi besar
        
        long totalHammingDistance = 0;
        long totalComparisons = 0;
        
        if (useSampling && population.length > 1) { //untuk populasi besar
            // Sample-based diversity
            java.util.Random random = new java.util.Random();
            int samples = Math.min(maxSamples, population.length * (population.length - 1) / 2);
            
            for (int s = 0; s < samples; s++) {
                int i = random.nextInt(population.length);
                int j = random.nextInt(population.length);
                if (i != j) {
                    int hammingDistance = calculateHammingDistance(population[i], population[j]);
                    totalHammingDistance += hammingDistance;
                    totalComparisons++;
                }
            }
        } else {
            // Hitung Hamming Distance antara semua pasangan kromosom (untuk populasi kecil)
            for (int i = 0; i < population.length; i++) {
                for (int j = i + 1; j < population.length; j++) {
                    int hammingDistance = calculateHammingDistance(population[i], population[j]);
                    totalHammingDistance += hammingDistance;
                    totalComparisons++;
                }
            }
        }
        
        // Normalisasi: diversity = rata-rata hamming distance / panjang kromosom
        if (totalComparisons == 0 || population[0].getLength() == 0) {
            return 0.0;
        }
        
        double avgHammingDistance = (double) totalHammingDistance / totalComparisons;
        double maxPossibleDistance = population[0].getLength();
        
        return avgHammingDistance / maxPossibleDistance;
    }
    
    //Menghitung Hamming Distance antara dua kromosom
    private static int calculateHammingDistance(Chromosome c1, Chromosome c2) {
        int distance = 0;
        for (int i = 0; i < c1.getLength(); i++) {
            if (c1.getGene(i) != c2.getGene(i)) {
                distance++;
            }
        }
        return distance;
    }
    
    //Menghitung fitness diversity (standar deviasi fitness)
    public static double calculateFitnessDiversity(Chromosome[] population) {
        if (population.length == 0) {
            return 0.0;
        }
        
        // Hitung rata-rata fitness
        double meanFitness = 0.0;
        for (Chromosome chromosome : population) {
            meanFitness += chromosome.getFitness();
        }
        meanFitness /= population.length;
        
        // Hitung varians
        double variance = 0.0;
        for (Chromosome chromosome : population) {
            double diff = chromosome.getFitness() - meanFitness;
            variance += diff * diff;
        }
        variance /= population.length;
        
        // Standar deviasi
        return Math.sqrt(variance);
    }
}

