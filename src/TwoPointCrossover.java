import java.util.Random;

public class TwoPointCrossover implements CrossoverStrategy {
    //wo point crossover
    @Override
    public Chromosome[] crossover(Chromosome parent1, Chromosome parent2, Random random) {
        int length = parent1.getLength();
        
        // Pilih dua titik potong secara acak
        int point1 = random.nextInt(length - 1) + 1;
        int point2 = random.nextInt(length - 1) + 1;
        
        // Pastikan point1 < point2
        if (point1 > point2) {
            int temp = point1;
            point1 = point2;
            point2 = temp;
        }
        
        // Jika point1 == point2, tambahkan 1 ke point2
        if (point1 == point2) {
            point2 = Math.min(point2 + 1, length - 1);
        }
        
        // Buat gen untuk child1 dan child2
        boolean[] genes1 = new boolean[length];
        boolean[] genes2 = new boolean[length];
        
        // Child1: parent1[0..point1] + parent2[point1..point2] + parent1[point2..end]
        // Child2: parent2[0..point1] + parent1[point1..point2] + parent2[point2..end]
        for (int i = 0; i < length; i++) {
            if (i < point1) {
                genes1[i] = parent1.getGene(i);
                genes2[i] = parent2.getGene(i);
            } else if (i < point2) {
                genes1[i] = parent2.getGene(i);
                genes2[i] = parent1.getGene(i);
            } else {
                genes1[i] = parent1.getGene(i);
                genes2[i] = parent2.getGene(i);
            }
        }
        
        // Buat kromosom baru
        Chromosome child1 = new Chromosome(parent1.getWidth(), parent1.getHeight(), genes1);
        Chromosome child2 = new Chromosome(parent1.getWidth(), parent1.getHeight(), genes2);
        
        return new Chromosome[]{child1, child2};
    }
}

