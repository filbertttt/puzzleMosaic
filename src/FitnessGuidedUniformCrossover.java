import java.util.Random;
public class FitnessGuidedUniformCrossover implements CrossoverStrategy {
    //Bias strength (0.0 = no bias, 1.0 = maximum bias)
    private double biasStrength;
    
   //Constructor dengan bias strength default
    public FitnessGuidedUniformCrossover() {
        this.biasStrength = 0.3;
    }
    
    //Constructor dengan bias strength custom
    public FitnessGuidedUniformCrossover(double biasStrength) {
        this.biasStrength = Math.max(0.0, Math.min(1.0, biasStrength));
    }
    
    //Melakukan fitness-guided uniform crossover
    @Override
    public Chromosome[] crossover(Chromosome parent1, Chromosome parent2, Random random) {
        int length = parent1.getLength();
        
        //menghitung fitness relatif untuk menentukan bias
        double fitness1 = parent1.getFitness();
        double fitness2 = parent2.getFitness();
        double totalFitness = fitness1 + fitness2;
        
        //menghitung probabilitas memilih dari parent1
        double probParent1;
        if (totalFitness < 0.001) {
            probParent1 = 0.5;
        } else {
            double baseProb = fitness2 / totalFitness;
            probParent1 = 0.5 + (baseProb - 0.5) * biasStrength;
            probParent1 = Math.max(0.3, Math.min(0.7, probParent1));
        }
        
        //membuat gen untuk child1 dan child2
        boolean[] genes1 = new boolean[length];
        boolean[] genes2 = new boolean[length];
        
        //loop untuk setiap posisi, pilih secara acak dengan bias fitness
        for (int i = 0; i < length; i++) {
            if (random.nextDouble() < probParent1) {
                //mengambil dari parent1 untuk child1, parent2 untuk child2
                genes1[i] = parent1.getGene(i);
                genes2[i] = parent2.getGene(i);
            } else {
                //mengambil dari parent2 untuk child1, parent1 untuk child2
                genes1[i] = parent2.getGene(i);
                genes2[i] = parent1.getGene(i);
            }
        }
        
        //membuat kromosom baru
        Chromosome child1 = new Chromosome(parent1.getWidth(), parent1.getHeight(), genes1);
        Chromosome child2 = new Chromosome(parent1.getWidth(), parent1.getHeight(), genes2);
        
        return new Chromosome[]{child1, child2};
    }
}

