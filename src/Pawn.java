import java.awt.Color;

public class Pawn extends Piece {

    // Flag to indicate if the pawn just moved two squares forward
    private boolean justMadeDoubleMove = false;

    /**
     * Constructor for the Pawn piece.
     * @param color The color of the piece.
     */
    public Pawn(Color color) {
        super(color);
    }

    /**
     * Sets the flag indicating whether the pawn just made a double move.
     * Used for en passant logic.
     * @param flag True if the pawn just moved two squares, false otherwise.
     */
    public void setJustMadeDoubleMove(boolean flag) {
        this.justMadeDoubleMove = flag;
    }

    /**
     * Gets the flag indicating whether the pawn just made a double move.
     * Used for en passant logic by the opponent.
     * @return True if the pawn just moved two squares, false otherwise.
     */
    public boolean getJustMadeDoubleMove() { // Added this getter method
        return this.justMadeDoubleMove;
    }


    /**
     * Checks if the pawn has reached the opposite side of the board and should be promoted.
     * @param toRow The destination row.
     * @return True if the pawn should be promoted, false otherwise.
     */
    public boolean shouldPromote(int toRow) {
        return (color.equals(Color.WHITE) && toRow == 0) ||
               (color.equals(Color.BLACK) && toRow == 7);
    }

    @Override
    public String getPieceType() {
        return "pawn";
    }

    /**
     * Checks if a move is valid for a pawn, excluding king safety checks.
     * Handles single forward move, double initial move, and standard diagonal captures.
     * En passant validity is checked during move generation in Minimax, not here.
     * @param fromRow Starting row.
     * @param fromCol Starting column.
     * @param toRow Destination row.
     * @param toCol Destination column.
     * @param board The current board state.
     * @return True if the move follows pawn movement/capture rules, false otherwise.
     */
    @Override
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        int direction = color.equals(Color.WHITE) ? -1 : 1; // White moves -1 row, Black moves +1 row

        // --- Standard Forward Move (1 square) ---
        if (fromCol == toCol && toRow - fromRow == direction && board[toRow][toCol] == null) {
            return true;
        }

        // --- Initial Double Forward Move (2 squares) ---
        if (fromCol == toCol && // Must be in the same column
           ((color.equals(Color.WHITE) && fromRow == 6) || (color.equals(Color.BLACK) && fromRow == 1)) && // Must be on starting row
            toRow - fromRow == 2 * direction && // Must move two squares
            board[fromRow + direction][toCol] == null && // Square in between must be empty
            board[toRow][toCol] == null) { // Destination square must be empty
            return true;
        }

        // --- Standard Diagonal Capture ---
        if (Math.abs(toCol - fromCol) == 1 && // Must move one column diagonally
            toRow - fromRow == direction && // Must move one row forward
            board[toRow][toCol] != null && // Destination must have a piece
            !board[toRow][toCol].getColor().equals(this.color)) { // Piece must be opponent's color
            return true;
        }

        // Note: En passant is handled separately in move generation (Minimax.addEnPassantMoves)
        // because it depends on the opponent's *last* move state (justMadeDoubleMove flag).
        // This isValidMove only checks the basic geometry and occupancy for standard moves.

        return false; // Move is not a valid standard pawn move/capture
    }
}
