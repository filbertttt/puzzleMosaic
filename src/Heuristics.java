import java.util.Random;
public class Heuristics {
    private Puzzle puzzle;
    
    //Constructor
    public Heuristics(Puzzle puzzle) {
        this.puzzle = puzzle;
    }
    
    //Menerapkan semua heuristik pada kromosom
    public void applyHeuristics(Chromosome chromosome) {
        // Basic heuristics (iterative untuk memastikan semua constraint terpenuhi)
        boolean changed = true;
        int iterations = 0;
        while (changed && iterations < 10) {  //untuk menghindari loop
            changed = false;
            
            // Apply basic patterns
            changed |= applyPattern0(chromosome);
            changed |= applyPattern9(chromosome);
            changed |= applyEdgeCornerPattern(chromosome);
            
            // Apply advanced heuristics
            changed |= applyConstraintPropagation(chromosome);
            changed |= applyPatternCompletion(chromosome);
            
            iterations++;
        }
    }
    
    //Pola 0: Jika angka = 0, semua tetangga (termasuk sel itu sendiri) pasti putih
    private boolean applyPattern0(Chromosome chromosome) {
        boolean changed = false;
        int[][] hints = puzzle.getHintPositions();
        
        for (int i = 0; i < hints.length; i++) {
            int row = hints[i][0];
            int col = hints[i][1];
            int value = hints[i][2];
            
            if (value == 0) {
                // Semua sel di area 3x3 harus putih
                for (int r = row - 1; r <= row + 1; r++) {
                    for (int c = col - 1; c <= col + 1; c++) {
                        if (r >= 0 && r < puzzle.getHeight() && 
                            c >= 0 && c < puzzle.getWidth()) {
                            if (chromosome.getCell(r, c)) {  // Jika hitam, ubah jadi putih
                                chromosome.setCell(r, c, false);
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
        return changed;
    }
    
    //Pola 9: Jika angka = 9 dan di tengah (memiliki 9 tetangga), semua tetangga pasti hitam
    private boolean applyPattern9(Chromosome chromosome) {
        boolean changed = false;
        int[][] hints = puzzle.getHintPositions();
        
        for (int i = 0; i < hints.length; i++) {
            int row = hints[i][0];
            int col = hints[i][1];
            int value = hints[i][2];
            
            if (value == 9) {
                int neighborCount = puzzle.getNeighborCount(row, col);
                
                // Jika memiliki 9 tetangga (di tengah), semua pasti hitam
                if (neighborCount == 9) {
                    for (int r = row - 1; r <= row + 1; r++) {
                        for (int c = col - 1; c <= col + 1; c++) {
                            if (r >= 0 && r < puzzle.getHeight() && 
                                c >= 0 && c < puzzle.getWidth()) {
                                if (!chromosome.getCell(r, c)) {  // Jika putih, ubah jadi hitam
                                    chromosome.setCell(r, c, true);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return changed;
    }
    
    //Pola Sudut/Tepi: Jika angka = jumlah maksimal tetangga yang dimiliki, semua tetangga pasti hitam
    private boolean applyEdgeCornerPattern(Chromosome chromosome) {
        boolean changed = false;
        int[][] hints = puzzle.getHintPositions();
        
        for (int i = 0; i < hints.length; i++) {
            int row = hints[i][0];
            int col = hints[i][1];
            int value = hints[i][2];
            
            int neighborCount = puzzle.getNeighborCount(row, col);
            
            // Jika nilai petunjuk sama dengan jumlah maksimal tetangga,
            // semua tetangga pasti hitam
            if (value == neighborCount) {
                for (int r = row - 1; r <= row + 1; r++) {
                    for (int c = col - 1; c <= col + 1; c++) {
                        if (r >= 0 && r < puzzle.getHeight() && 
                            c >= 0 && c < puzzle.getWidth()) {
                            if (!chromosome.getCell(r, c)) {  // Jika putih, ubah jadi hitam
                                chromosome.setCell(r, c, true);
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
        return changed;
    }
    
    //Constraint Propagation: Jika suatu hint sudah terpenuhi, pastikan sel-sel di luar area 3x3 tidak melanggar
    //Jika suatu hint masih kurang/lebih, propagasikan constraint ke tetangga
    private boolean applyConstraintPropagation(Chromosome chromosome) {
        boolean changed = false;
        int[][] hints = puzzle.getHintPositions();
        
        for (int i = 0; i < hints.length; i++) {
            int row = hints[i][0];
            int col = hints[i][1];
            int expectedValue = hints[i][2];
            
            // Hitung current count
            int currentCount = chromosome.countBlackNeighbors(row, col);
            int neighborCount = puzzle.getNeighborCount(row, col);
            
            if (currentCount == expectedValue) {
                // Hint sudah terpenuhi, tidak perlu perubahan
                continue;
            }
            
            // Hitung sel yang belum ditentukan (tidak ada hint yang memaksa)
            int remainingCells = neighborCount;
            int remainingNeeded = expectedValue - currentCount;
            
            // Jika remainingNeeded == remainingCells, semua sisa harus hitam
            if (remainingNeeded == remainingCells && remainingCells > 0) {
                for (int r = row - 1; r <= row + 1; r++) {
                    for (int c = col - 1; c <= col + 1; c++) {
                        if (r >= 0 && r < puzzle.getHeight() && 
                            c >= 0 && c < puzzle.getWidth() &&
                            !puzzle.isHint(r, c)) {  // Hanya sel non-hint
                            if (!chromosome.getCell(r, c)) {
                                chromosome.setCell(r, c, true);
                                changed = true;
                            }
                        }
                    }
                }
            }
            
            // Jika remainingNeeded == 0 tapi masih ada hitam, ubah yang tidak perlu jadi putih
            if (remainingNeeded == 0 && currentCount > expectedValue) {
                // Sudah terlalu banyak hitam, tapi ini sudah ditangani oleh fitness function
            }
        }
        
        return changed;
    }
    
    //Pattern Completion: Jika suatu hint sudah sebagian terpenuhi, coba selesaikan sisanya secara greedy
    //Fokus pada hints yang hampir terpenuhi (error kecil)
    private boolean applyPatternCompletion(Chromosome chromosome) {
        boolean changed = false;
        int[][] hints = puzzle.getHintPositions();
        
        for (int i = 0; i < hints.length; i++) {
            int row = hints[i][0];
            int col = hints[i][1];
            int expectedValue = hints[i][2];
            
            int currentCount = chromosome.countBlackNeighbors(row, col);
            int error = Math.abs(expectedValue - currentCount);
            
            // Fokus pada hints dengan error kecil (1 atau 2)
            if (error <= 1 && error > 0) {
                int remainingCells = 0;
                
                // Hitung sel yang bisa diubah (non-hint cells)
                for (int r = row - 1; r <= row + 1; r++) {
                    for (int c = col - 1; c <= col + 1; c++) {
                        if (r >= 0 && r < puzzle.getHeight() && 
                            c >= 0 && c < puzzle.getWidth() &&
                            !puzzle.isHint(r, c)) {
                            remainingCells++;
                        }
                    }
                }
                
                // Jika hanya ada 1 sel yang bisa diubah dan itu cukup untuk memperbaiki error
                if (remainingCells == 1 && error == 1) {
                    for (int r = row - 1; r <= row + 1; r++) {
                        for (int c = col - 1; c <= col + 1; c++) {
                            if (r >= 0 && r < puzzle.getHeight() && 
                                c >= 0 && c < puzzle.getWidth() &&
                                !puzzle.isHint(r, c)) {
                                // Ubah sel ini untuk memperbaiki error
                                boolean shouldBeBlack = (currentCount < expectedValue);
                                if (chromosome.getCell(r, c) != shouldBeBlack) {
                                    chromosome.setCell(r, c, shouldBeBlack);
                                    changed = true;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return changed;
    }
    
    //Menerapkan heuristik pada populasi awal
    public void applyToPopulation(Chromosome[] population, Random random) {
        // Terapkan heuristik pada sebagian populasi
        // untuk menjaga diversity
        int applyCount = (int) (population.length * 0.3);
        
        for (int i = 0; i < applyCount; i++) {
            applyHeuristics(population[i]);
        }
    }
}

