import java.util.Random;

/**
 * Kelas TournamentSelection - Implementasi Tournament Selection
 * 
 * Cara kerja:
 * 1. Pilih k individu secara acak dari populasi
 * 2. Pilih yang terbaik (fitness terkecil) dari kandidat tersebut
 * 
 * Parameter: tournamentSize (ukuran turnamen, default = 3)
 * 
 * @author Kelompok 14
 */
public class TournamentSelection implements SelectionStrategy {
    /** Ukuran turnamen (jumlah kandidat yang dipilih) */
    private int tournamentSize;
    
    /**
     * Konstruktor dengan ukuran turnamen default = 3
     */
    public TournamentSelection() {
        this.tournamentSize = 3;
    }
    
    /**
     * Konstruktor dengan ukuran turnamen yang dapat disesuaikan
     * 
     * @param tournamentSize Ukuran turnamen
     */
    public TournamentSelection(int tournamentSize) {
        this.tournamentSize = tournamentSize;
    }
    
    /**
     * Memilih parent menggunakan Tournament Selection
     * 
     * @param population Populasi kromosom
     * @param fitnessFunction Fitness function
     * @param random Random number generator
     * @return Kromosom yang terpilih
     */
    @Override
    public Chromosome select(Chromosome[] population, FitnessFunction fitnessFunction, Random random) {
        // Hitung fitness untuk semua kromosom yang belum dihitung
        for (Chromosome chromosome : population) {
            if (!chromosome.isFitnessCalculated()) {
                fitnessFunction.calculateFitness(chromosome);
            }
        }
        
        // Pilih k kandidat secara acak
        Chromosome[] candidates = new Chromosome[tournamentSize];
        for (int i = 0; i < tournamentSize; i++) {
            int randomIndex = random.nextInt(population.length);
            candidates[i] = population[randomIndex];
        }
        
        // Pilih yang terbaik (fitness terkecil)
        Chromosome best = candidates[0];
        for (int i = 1; i < tournamentSize; i++) {
            if (candidates[i].getFitness() < best.getFitness()) {
                best = candidates[i];
            }
        }
        
        return best.clone();
    }
}

