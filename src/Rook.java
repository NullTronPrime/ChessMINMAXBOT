import java.awt.Color;

public class Rook extends Piece {
    
    public Rook(Color color) {
        super(color);
    }
    
    @Override
    public String getPieceType() {
        return "rook";
    }
    
    @Override
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        if (fromRow == toRow || fromCol == toCol) {
            if (isPathClear(fromRow, fromCol, toRow, toCol, board))
                return board[toRow][toCol] == null || !board[toRow][toCol].getColor().equals(this.color);
        }
        return false;
    }
}
