import java.util.Random;

public interface CrossoverStrategy {
    //Melakukan crossover antara dua parent untuk menghasilkan offspring
    Chromosome[] crossover(Chromosome parent1, Chromosome parent2, Random random);
}

