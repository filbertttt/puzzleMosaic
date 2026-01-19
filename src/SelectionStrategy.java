import java.util.Random;

public interface SelectionStrategy {
    //Memilih parent dari populasi
    Chromosome select(Chromosome[] population, FitnessFunction fitnessFunction, Random random);
}

