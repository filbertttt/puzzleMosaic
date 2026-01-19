import java.util.Random;

/**
 * Encoding Strategy:
 * Setiap bit = satu sel pada papan
 * Bit 1 (true) = sel terisi (hitam)
 * Bit 0 (false) = sel kosong (putih)
 * Panjang kromosom = width × height
 */
public class Chromosome {
    //Representasi status setiap sel (true = hitam, false = putih)
    private boolean[] genes;
    
    //Ukuran papan
    private int width;
    private int height;
    
    //Nilai fitness (0 = solusi sempurna)
    private double fitness;
    
    //Status fitness
    private boolean fitnessCalculated;
    
    //Constructor untuk membuat kromosom baru dengan gen acak
    public Chromosome(int width, int height, Random random) {
        this.width = width;
        this.height = height;
        this.genes = new boolean[width * height];
        this.fitness = Double.MAX_VALUE;
        this.fitnessCalculated = false;
        
        // Inisialisasi acak
        for (int i = 0; i < genes.length; i++) {
            genes[i] = random.nextBoolean();
        }
    }
    
    //Constructor untuk membuat kromosom baru dengan gen yang sudah ditentukan
    public Chromosome(int width, int height, boolean[] genes) {
        this.width = width;
        this.height = height;
        this.genes = genes.clone(); // Clone untuk menghindari referensi yang sama
        this.fitness = Double.MAX_VALUE;
        this.fitnessCalculated = false;
    }
    
    //Mendapatkan status sel pada posisi tertentu
    public boolean getCell(int row, int col) {
        int index = row * width + col;
        return genes[index];
    }
    
    //Mengatur status sel pada posisi tertentu
    public void setCell(int row, int col, boolean value) {
        int index = row * width + col;
        genes[index] = value;
        fitnessCalculated = false; // Fitness perlu dihitung ulang
    }
    
    //Mendapatkan gen pada index tertentu
    public boolean getGene(int index) {
        return genes[index];
    }
    
    //Mengatur gen pada index tertentu
    public void setGene(int index, boolean value) {
        genes[index] = value;
        fitnessCalculated = false;
    }
    
    //Mendapatkan panjang kromosom (jumlah gen)
    public int getLength() {
        return genes.length;
    }
    
    //Mendapatkan lebar papan
    public int getWidth() {
        return width;
    }
    
    //Mendapatkan tinggi papan
    public int getHeight() {
        return height;
    }
    
    //Mendapatkan array gen (clone untuk keamanan)
    public boolean[] getGenes() {
        return genes.clone();
    }
    
    //Mendapatkan nilai fitness
    public double getFitness() {
        return fitness;
    }
    
    //Mengatur nilai fitness
    public void setFitness(double fitness) {
        this.fitness = fitness;
        this.fitnessCalculated = true;
    }
    
    //Mengecek apakah fitness sudah dihitung
    public boolean isFitnessCalculated() {
        return fitnessCalculated;
    }
    
    //Membuat salinan kromosom ini
    public Chromosome clone() {
        Chromosome clone = new Chromosome(width, height, genes);
        clone.fitness = this.fitness;
        clone.fitnessCalculated = this.fitnessCalculated;
        return clone;
    }
    
    //Menghitung jumlah sel hitam di sekitar posisi tertentu (area 3x3)
    public int countBlackNeighbors(int row, int col) {
        int count = 0;
        
        // Iterasi melalui area 3x3
        for (int i = row - 1; i <= row + 1; i++) {
            for (int j = col - 1; j <= col + 1; j++) {
                if (i >= 0 && i < height && j >= 0 && j < width) {
                    if (getCell(i, j)) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    //Mencetak kromosom sebagai papan permainan
    public void print() {
        System.out.println("Chromosome (fitness: " + fitness + "):");
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                System.out.print(getCell(i, j) ? "1 " : "0 ");
            }
            System.out.println();
        }
    }
}

