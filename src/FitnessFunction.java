public class FitnessFunction {
    private Puzzle puzzle;
    
    //Cache untuk hint weights
    private double[] hintWeights;
    
    //Status weights
    private boolean weightsCalculated;
    
    //Constructor
    public FitnessFunction(Puzzle puzzle) {
        this.puzzle = puzzle;
        this.weightsCalculated = false;
    }
    
    //Menghitung weight untuk setiap hint berdasarkan kompleksitas
    private void calculateHintWeights() {
        if (weightsCalculated) {
            return;
        }
        
        int[][] hints = puzzle.getHintPositions();
        hintWeights = new double[hints.length];
        
        // Hitung weight berdasarkan jumlah hints yang overlapping (hints di daerah padat mendapat weight lebih tinggi)
        for (int i = 0; i < hints.length; i++) {
            int row = hints[i][0];
            int col = hints[i][1];
            
            // Hitung berapa banyak hints yang overlapping dengan hint ini
            int overlappingCount = 0;
            for (int j = 0; j < hints.length; j++) {
                if (i != j) {
                    int otherRow = hints[j][0];
                    int otherCol = hints[j][1];
                    
                    // Cek apakah overlapping (area 3x3 mereka berpotongan)
                    if (Math.abs(row - otherRow) <= 2 && Math.abs(col - otherCol) <= 2) {
                        overlappingCount++;
                    }
                }
            }
            
            // Hints dengan lebih banyak overlapping mendapat weight lebih tinggi
            hintWeights[i] = 1.0 + (overlappingCount * 0.2);
        }
        
        weightsCalculated = true;
    }
    
    //Menghitung nilai fitness untuk kromosom
    public double calculateFitness(Chromosome chromosome) {
        // Jika fitness sudah dihitung, kembalikan nilai yang sudah ada
        if (chromosome.isFitnessCalculated()) {
            return chromosome.getFitness();
        }
        
        // Hitung hint weights jika belum dihitung
        calculateHintWeights();
        
        double totalError = 0.0;
        int correctHints = 0;
        
        // Dapatkan semua posisi petunjuk
        int[][] hints = puzzle.getHintPositions();
        
        // Untuk setiap petunjuk, hitung error local dengan weight dan quadratic penalty
        for (int i = 0; i < hints.length; i++) {
            int row = hints[i][0];
            int col = hints[i][1];
            int expectedValue = hints[i][2]; // N = nilai petunjuk
            
            // Hitung jumlah sel hitam di area 3x3 sekitar petunjuk
            int actualBlackCount = chromosome.countBlackNeighbors(row, col);
            
            // Hitung error local: selisih absolut antara expected dan actual
            int localError = Math.abs(expectedValue - actualBlackCount);
            
            // Apply weight (hints di daerah padat mendapat weight lebih tinggi)
            double weightedError = localError * hintWeights[i];
            
            // Quadratic penalty untuk error besar
            if (localError > 1) {
                weightedError *= (1.0 + (localError - 1) * 0.6);
            }
            
            totalError += weightedError;
            
            // Hitung jumlah hints yang sudah benar
            if (localError == 0) {
                correctHints++;
            }
        }
        
        // Penalty untuk hints yang overlapping dan tidak konsisten
        double fitnessThreshold = (hints.length > 15) ? 100.0 : 50.0;
        if (totalError < fitnessThreshold) {
            double inconsistencyPenalty = calculateInconsistencyPenalty(chromosome, hints);
            totalError += inconsistencyPenalty;
        }
        
        // Bonus untuk hints yang benar
        double bonus = correctHints * 0.01;
        if (correctHints > hints.length * 0.5) {
            bonus += (correctHints - hints.length * 0.5) * 0.02;
        }
        totalError = Math.max(0.0, totalError - bonus);
        
        chromosome.setFitness(totalError);
        
        return totalError;
    }
    
    private double calculateInconsistencyPenalty(Chromosome chromosome, int[][] hints) {
        int[] actualCounts = new int[hints.length];
        int[] errors = new int[hints.length];
        
        //loop untuk menghitung jumlah sel hitam di area 3x3 sekitar petunjuk
        for (int i = 0; i < hints.length; i++) {
            int row = hints[i][0];
            int col = hints[i][1];
            int expected = hints[i][2];
            actualCounts[i] = chromosome.countBlackNeighbors(row, col);
            errors[i] = Math.abs(expected - actualCounts[i]);
        }
        
        double penalty = 0.0;
        
        //loop untuk menghitung penalty untuk hints yang overlapping dan tidak konsisten
        for (int i = 0; i < hints.length; i++) {
            int row1 = hints[i][0];
            int col1 = hints[i][1];
            int error1 = errors[i];
            
            for (int j = i + 1; j < hints.length; j++) {
                int row2 = hints[j][0];
                int col2 = hints[j][1];
                
                // Cek apakah overlapping (area 3x3 mereka berpotongan)
                if (Math.abs(row1 - row2) <= 2 && Math.abs(col1 - col2) <= 2) {
                    int error2 = errors[j];
                    
                    if (error1 > 1 && error2 > 1) {
                        int errorDiff = Math.abs(error1 - error2);
                        penalty += errorDiff * 0.15;
                    }
                    if (error1 + error2 > 4) {
                        penalty += (error1 + error2 - 4) * 0.1;
                    }
                }
            }
        }
        
        return penalty;
    }
    
    //Mengecek apakah kromosomnya adalah solusi yang valid
    public boolean isSolution(Chromosome chromosome) {
        return calculateFitness(chromosome) == 0.0;
    }
}

