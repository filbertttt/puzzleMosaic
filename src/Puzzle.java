public class Puzzle {
    private int width;
    private int height;
    private int[][] board;
    
    //Constructor
    public Puzzle(int width, int height, int[][] hints) {
        this.width = width;
        this.height = height;
        this.board = new int[height][width];
        
        // Copy hints ke board
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                this.board[i][j] = hints[i][j];
            }
        }
    }
    //Getter untuk lebar papan
    public int getWidth() {
        return width;
    }
    
    //Getter untuk tinggi papan
    public int getHeight() {
        return height;
    }

    //Getter untuk nilai pada posisi (row, col)
    public int getValue(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) {
            return -1;
        }
        return board[row][col];
    }

    //Return true if position (row, col) is a number (0-9)
    public boolean isHint(int row, int col) {
        return getValue(row, col) >= 0;
    }
    
    //Return all numbers (0-9) positions
    public int[][] getHintPositions() {
        int count = 0;
        
        //Count number of numbers (0-9)
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (board[i][j] >= 0) {
                    count++;
                }
            }
        }
        
        //Create array of numbers (0-9) positions
        int[][] hints = new int[count][3];
        int index = 0;
        
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (board[i][j] >= 0) {
                    hints[index][0] = i;  // row
                    hints[index][1] = j;  // col
                    hints[index][2] = board[i][j];  // value
                    index++;
                }
            }
        }
        
        return hints;
    }
    
    //Return number of valid neighbors for position (row, col)
    public int getNeighborCount(int row, int col) {
        int count = 0;
        
        //Iterate through 3x3 area
        for (int i = row - 1; i <= row + 1; i++) {
            for (int j = col - 1; j <= col + 1; j++) {
                if (i >= 0 && i < height && j >= 0 && j < width) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    //Print puzzle board
    public void print() {
        System.out.println("Puzzle Board (" + width + "x" + height + "):");
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (board[i][j] == -1) {
                    System.out.print("  ");
                } else {
                    System.out.print(board[i][j] + " ");
                }
            }
            System.out.println();
        }
    }
}

