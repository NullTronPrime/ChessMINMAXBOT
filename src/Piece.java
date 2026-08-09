import java.awt.Color;

/**
 * Abstract base class for all chess pieces.
 */
public abstract class Piece {
    protected Color color;
    protected boolean hasMoved = false; // Track if the piece has moved (for castling, pawn double moves)

    /**
     * Constructor for a Piece.
     * @param color The color of the piece (Color.WHITE or Color.BLACK).
     */
    public Piece(Color color) {
        this.color = color;
    }

    /**
     * Gets the color of the piece.
     * @return The Color of the piece.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Checks if the piece has moved from its starting position.
     * @return True if the piece has moved, false otherwise.
     */
    public boolean hasMoved() {
        return hasMoved;
    }

    /**
     * Sets the moved status of the piece.
     * @param moved True to mark the piece as moved, false otherwise.
     */
    public void setHasMoved(boolean moved) {
        this.hasMoved = moved;
    }

    /**
     * Gets the type of the piece (e.g., "pawn", "rook", "king").
     * Must be implemented by subclasses.
     * @return A string representing the piece type.
     */
    public abstract String getPieceType();

    /**
     * Checks if a move from (fromRow, fromCol) to (toRow, toCol) is valid
     * according to the rules of this piece type, ignoring king safety.
     * Must be implemented by subclasses.
     * @param fromRow Starting row (0-7).
     * @param fromCol Starting column (0-7).
     * @param toRow Destination row (0-7).
     * @param toCol Destination column (0-7).
     * @param board The current board state (Piece[][]).
     * @return True if the move geometry and basic rules are valid, false otherwise.
     */
    public abstract boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board);

    /**
     * Checks if the piece at (fromRow, fromCol) can attack the square (toRow, toCol),
     * based purely on the piece's attack pattern geometry.
     * This is often the same as isValidMove, but pawns have different move/attack patterns.
     *
     * @param fromRow Starting row of the attacking piece (0-7).
     * @param fromCol Starting column of the attacking piece (0-7).
     * @param toRow Destination row being attacked (0-7).
     * @param toCol Destination column being attacked (0-7).
     * @param board The current board state (Piece[][]).
     * @return True if the piece type can attack the target square from its position, false otherwise.
     */
    public boolean isValidAttack(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        // Default implementation: For most pieces (Rook, Knight, Bishop, Queen, King),
        // the squares they can attack are the same squares they can legally move to
        // (ignoring whether the target square is empty or occupied by friendly piece).
        // The Pawn class MUST override this method.
        return isValidMove(fromRow, fromCol, toRow, toCol, board);
    }


    /**
     * Utility method for sliding pieces (Rook, Bishop, Queen) to check if the path
     * between the start and end squares is clear of other pieces.
     * Assumes the move is strictly horizontal, vertical, or diagonal.
     *
     * @param fromRow Starting row.
     * @param fromCol Starting column.
     * @param toRow Destination row.
     * @param toCol Destination column.
     * @param board The current board state.
     * @return True if the path is clear (excluding start/end squares), false otherwise.
     */
    protected boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
        int rowDir = Integer.compare(toRow, fromRow); // 0, 1, or -1
        int colDir = Integer.compare(toCol, fromCol); // 0, 1, or -1

        int r = fromRow + rowDir;
        int c = fromCol + colDir;

        // Iterate along the path until the destination square is reached
        while (r != toRow || c != toCol) {
            // Check bounds (shouldn't be necessary if isValidMove was called first, but safe)
            if (r < 0 || r >= board.length || c < 0 || c >= board[0].length) {
                return false; // Path went off board - indicates invalid move geometry
            }
            // If any square along the path is occupied
            if (board[r][c] != null) {
                return false; // Path is blocked
            }
            // Move to the next square along the path
            r += rowDir;
            c += colDir;
        }

        // If the loop completes without finding obstructions, the path is clear
        return true;
    }

    // Optional: Override equals and hashCode if pieces need to be compared or used in Sets/Maps
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Piece piece = (Piece) obj;
        return hasMoved == piece.hasMoved &&
               java.util.Objects.equals(color, piece.color) &&
               java.util.Objects.equals(getPieceType(), piece.getPieceType()); // Compare type too
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(color, hasMoved, getPieceType());
    }

    @Override
    public String toString() {
        return (color.equals(Color.WHITE) ? "W" : "B") + "_" + getPieceType();
    }
}
