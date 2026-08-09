import java.awt.Color;

public class Bishop extends Piece {
    
    public Bishop(Color color) {
        super(color);
    }
    
    @Override
    public String getPieceType() {
        return "bishop";
    }
    
    @Override
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);
        if (rowDiff == colDiff) {
            if (isPathClear(fromRow, fromCol, toRow, toCol, board))
                return board[toRow][toCol] == null || !board[toRow][toCol].getColor().equals(this.color);
        }
        return false;
    }
}
