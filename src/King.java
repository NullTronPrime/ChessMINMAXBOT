import java.awt.Color;

public class King extends Piece {

    private int row, col;
    
    public King(Color color) {
        super(color);
    }
    
    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }
    
    public int getRow() {
        return row;
    }
    
    public int getCol() {
        return col;
    }
    
    @Override
    public String getPieceType() {
        return "king";
    }
    
    @Override
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);
        // Basic king move: one step any direction.
        if (rowDiff <= 1 && colDiff <= 1) {
            return board[toRow][toCol] == null || !board[toRow][toCol].getColor().equals(this.color);
        }
        // Castling is handled at the board level.
        return false;
    }
}
