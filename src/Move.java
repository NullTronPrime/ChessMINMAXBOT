public class Move {
    public int fromRow, fromCol, toRow, toCol;
    public Piece movedPiece;
    public boolean movedPieceHadMoved;
    public Piece capturedPiece; // May be null
    
    // For castling moves
    public boolean wasCastling;
    public int rookFromRow, rookFromCol, rookToRow, rookToCol;
    public Piece rookPiece;
    public boolean rookHadMoved;
    
    // For pawn promotion
    public boolean wasPromotion;
    public Piece prePromotionPiece;
    
    // Default constructor
    public Move() { }
    
    // Minimal constructor to create a basic move.
    public Move(int fromRow, int fromCol, int toRow, int toCol, Piece movedPiece) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.movedPiece = movedPiece;
        // Default values: assume the piece's current moved state; no capture, no special moves.
        this.movedPieceHadMoved = movedPiece.hasMoved();
        this.capturedPiece = null;
        this.wasCastling = false;
        this.wasPromotion = false;
        // Set default rook fields (unused unless castling occurs).
        this.rookFromRow = this.rookFromCol = this.rookToRow = this.rookToCol = 0;
        this.rookPiece = null;
        this.rookHadMoved = false;
        this.prePromotionPiece = null;
    }
    
    // Full constructor to initialize all fields.
    public Move(int fromRow, int fromCol, int toRow, int toCol, Piece movedPiece,
                boolean movedPieceHadMoved, Piece capturedPiece, boolean wasCastling,
                int rookFromRow, int rookFromCol, int rookToRow, int rookToCol, 
                Piece rookPiece, boolean rookHadMoved, boolean wasPromotion, Piece prePromotionPiece) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.movedPiece = movedPiece;
        this.movedPieceHadMoved = movedPieceHadMoved;
        this.capturedPiece = capturedPiece;
        this.wasCastling = wasCastling;
        this.rookFromRow = rookFromRow;
        this.rookFromCol = rookFromCol;
        this.rookToRow = rookToRow;
        this.rookToCol = rookToCol;
        this.rookPiece = rookPiece;
        this.rookHadMoved = rookHadMoved;
        this.wasPromotion = wasPromotion;
        this.prePromotionPiece = prePromotionPiece;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) 
            return true;
        if (!(obj instanceof Move)) 
            return false;
        Move other = (Move) obj;
        return this.fromRow == other.fromRow &&
               this.fromCol == other.fromCol &&
               this.toRow == other.toRow &&
               this.toCol == other.toCol &&
               ((this.movedPiece == null && other.movedPiece == null) || 
                (this.movedPiece != null && this.movedPiece.equals(other.movedPiece))) &&
               this.movedPieceHadMoved == other.movedPieceHadMoved &&
               ((this.capturedPiece == null && other.capturedPiece == null) ||
                (this.capturedPiece != null && this.capturedPiece.equals(other.capturedPiece))) &&
               this.wasCastling == other.wasCastling &&
               this.rookFromRow == other.rookFromRow &&
               this.rookFromCol == other.rookFromCol &&
               this.rookToRow == other.rookToRow &&
               this.rookToCol == other.rookToCol &&
               ((this.rookPiece == null && other.rookPiece == null) || 
                (this.rookPiece != null && this.rookPiece.equals(other.rookPiece))) &&
               this.rookHadMoved == other.rookHadMoved &&
               this.wasPromotion == other.wasPromotion &&
               ((this.prePromotionPiece == null && other.prePromotionPiece == null) ||
                (this.prePromotionPiece != null && this.prePromotionPiece.equals(other.prePromotionPiece)));
    }
    
    @Override
    public int hashCode() {
        int result = fromRow;
        result = 31 * result + fromCol;
        result = 31 * result + toRow;
        result = 31 * result + toCol;
        result = 31 * result + (movedPiece != null ? movedPiece.hashCode() : 0);
        result = 31 * result + (movedPieceHadMoved ? 1 : 0);
        result = 31 * result + (capturedPiece != null ? capturedPiece.hashCode() : 0);
        result = 31 * result + (wasCastling ? 1 : 0);
        result = 31 * result + rookFromRow;
        result = 31 * result + rookFromCol;
        result = 31 * result + rookToRow;
        result = 31 * result + rookToCol;
        result = 31 * result + (rookPiece != null ? rookPiece.hashCode() : 0);
        result = 31 * result + (rookHadMoved ? 1 : 0);
        result = 31 * result + (wasPromotion ? 1 : 0);
        result = 31 * result + (prePromotionPiece != null ? prePromotionPiece.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Move[from=(").append(fromRow).append(", ").append(fromCol)
          .append("), to=(").append(toRow).append(", ").append(toCol).append("), piece=")
          .append(movedPiece);
        if (capturedPiece != null) {
            sb.append(", captures=").append(capturedPiece);
        }
        if (wasCastling) {
            sb.append(", castling: rook from (")
              .append(rookFromRow).append(", ").append(rookFromCol).append(") to (")
              .append(rookToRow).append(", ").append(rookToCol).append(")");
        }
        if (wasPromotion) {
            sb.append(", promotion: prePromotionPiece=").append(prePromotionPiece);
        }
        sb.append("]");
        return sb.toString();
    }
}
