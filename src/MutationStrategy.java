import java.util.Random;

public interface MutationStrategy {
    //Melakukan mutasi pada kromosom
    void mutate(Chromosome chromosome, double mutationRate, Random random);
}

