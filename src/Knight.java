import java.awt.Color;

public class Knight extends Piece {
    
    public Knight(Color color) {
        super(color);
    }
    
    @Override
    public String getPieceType() {
        return "knight";
    }
    
    @Override
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);
        if ((rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2))
            return board[toRow][toCol] == null || !board[toRow][toCol].getColor().equals(this.color);
        return false;
    }
}
