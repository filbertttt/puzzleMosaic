import java.util.Random;

public class SinglePointCrossover implements CrossoverStrategy {
    
    //Melakukan single point crossover
    @Override
    public Chromosome[] crossover(Chromosome parent1, Chromosome parent2, Random random) {
        int length = parent1.getLength();
        
        // Pilih titik potong secara acak (antara 1 dan length-1)
        int crossoverPoint = random.nextInt(length - 1) + 1;
        
        // Buat gen untuk child1 dan child2
        boolean[] genes1 = new boolean[length];
        boolean[] genes2 = new boolean[length];
        
        // Child1: parent1 sebelum titik, parent2 setelah titik
        for (int i = 0; i < crossoverPoint; i++) {
            genes1[i] = parent1.getGene(i);
            genes2[i] = parent2.getGene(i);
        }
        
        // Child2: parent2 sebelum titik, parent1 setelah titik
        for (int i = crossoverPoint; i < length; i++) {
            genes1[i] = parent2.getGene(i);
            genes2[i] = parent1.getGene(i);
        }
        
        // Buat kromosom baru
        Chromosome child1 = new Chromosome(parent1.getWidth(), parent1.getHeight(), genes1);
        Chromosome child2 = new Chromosome(parent1.getWidth(), parent1.getHeight(), genes2);
        
        return new Chromosome[]{child1, child2};
    }
}

