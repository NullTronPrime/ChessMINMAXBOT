import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random; // Import Random for Zobrist hashing

public class Minimax {

    // --- Transposition Table Definitions ---
    private static enum EntryType { EXACT, LOWERBOUND, UPPERBOUND }

    private static class TranspositionEntry {
        int depth;
        int value;
        Move bestMove; // Store the best move found at this node
        EntryType type;
        long zobristHash; // Store hash to verify entry validity (optional but good practice)

        TranspositionEntry(long hash, int depth, int value, Move bestMove, EntryType type) {
            this.zobristHash = hash; // Store the hash this entry corresponds to
            this.depth = depth;
            this.value = value;
            this.bestMove = bestMove;
            this.type = type;
        }
    }

    // Transposition table to store evaluated positions (Map Zobrist Hash to Entry)
    private static Map<Long, TranspositionEntry> transpositionTable = new HashMap<>();
    // Zobrist hashing utility
    private static ZobristHashing zobrist = new ZobristHashing();

    // --- Static Evaluation Constants and Piece-Square Tables ---
    // Base piece values
    private static final int PAWN_VALUE   = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE   = 500;
    private static final int QUEEN_VALUE  = 900;
    private static final int KING_VALUE   = 20000; // Effectively infinite for evaluation

    // Evaluation weights/bonuses
    private static final int MOBILITY_WEIGHT = 10; // Weight for mobility score
    private static final int DOUBLED_PAWN_PENALTY = -15; // Penalty per doubled pawn pair in a file
    private static final int ISOLATED_PAWN_PENALTY = -10; // Penalty per isolated pawn
    private static final int PASSED_PAWN_BONUS = 25; // Bonus per passed pawn (scales with rank)
    private static final int CASTLING_BONUS = 50; // Bonus for having castled (or rights intact)
    private static final int CHECKMATE_SCORE = KING_VALUE * 2; // Score for checkmate

    // --- Piece-Square Tables (Remain unchanged from previous version) ---
    // Pawn PST
    private static final int[][] PAWN_TABLE = {
        {  0,   0,   0,   0,   0,   0,   0,   0}, { 50,  50,  50,  50,  50,  50,  50,  50},
        { 10,  10,  20,  30,  30,  20,  10,  10}, {  5,   5,  10,  25,  25,  10,   5,   5},
        {  0,   0,   0,  20,  20,   0,   0,   0}, {  5,  -5, -10,   0,   0, -10,  -5,   5},
        {  5,  10,  10, -20, -20,  10,  10,   5}, {  0,   0,   0,   0,   0,   0,   0,   0}
    };
    // Knight PST
    private static final int[][] KNIGHT_TABLE = {
        {-50, -40, -30, -30, -30, -30, -40, -50}, {-40, -20,   0,   5,   5,   0, -20, -40},
        {-30,   0,  10,  15,  15,  10,   0, -30}, {-30,   5,  15,  20,  20,  15,   5, -30},
        {-30,   0,  15,  20,  20,  15,   0, -30}, {-30,   5,  10,  15,  15,  10,   5, -30},
        {-40, -20,   0,   0,   0,   0, -20, -40}, {-50, -40, -30, -30, -30, -30, -40, -50}
    };
    // Bishop PST
    private static final int[][] BISHOP_TABLE = {
        {-20, -10, -10, -10, -10, -10, -10, -20}, {-10,   0,   0,   0,   0,   0,   0, -10},
        {-10,   0,   5,  10,  10,   5,   0, -10}, {-10,   5,   5,  10,  10,   5,   5, -10},
        {-10,   0,  10,  10,  10,  10,   0, -10}, {-10,  10,  10,  10,  10,  10,  10, -10},
        {-10,   5,   0,   0,   0,   0,   5, -10}, {-20, -10, -10, -10, -10, -10, -10, -20}
    };
    // Rook PST
    private static final int[][] ROOK_TABLE = {
        {  0,   0,   0,   5,   5,   0,   0,   0}, { -5,   0,   0,   0,   0,   0,   0,  -5},
        { -5,   0,   0,   0,   0,   0,   0,  -5}, { -5,   0,   0,   0,   0,   0,   0,  -5},
        { -5,   0,   0,   0,   0,   0,   0,  -5}, { -5,   0,   0,   0,   0,   0,   0,  -5},
        {  5,  10,  10,  10,  10,  10,  10,   5}, {  0,   0,   0,   0,   0,   0,   0,   0}
    };
    // Queen PST
     private static final int[][] QUEEN_TABLE = {
        {-20, -10, -10,  -5,  -5, -10, -10, -20}, {-10,   0,   0,   0,   0,   0,   0, -10},
        {-10,   0,   5,   5,   5,   5,   0, -10}, { -5,   0,   5,   5,   5,   5,   0,  -5},
        {  0,   0,   5,   5,   5,   5,   0,  -5}, {-10,   5,   5,   5,   5,   5,   0, -10},
        {-10,   0,   0,   0,   0,   0,   0, -10}, {-20, -10, -10,  -5,  -5, -10, -10, -20}
    };
    // King Midgame PST
    private static final int[][] KING_MIDGAME_TABLE = {
        {-30, -40, -40, -50, -50, -40, -40, -30}, {-30, -40, -40, -50, -50, -40, -40, -30},
        {-30, -40, -40, -50, -50, -40, -40, -30}, {-30, -40, -40, -50, -50, -40, -40, -30},
        {-20, -30, -30, -40, -40, -30, -30, -20}, {-10, -20, -20, -20, -20, -20, -20, -10},
        { 20,  20,   0,   0,   0,   0,  20,  20}, { 20,  30,  10,   0,   0,  10,  30,  20}
    };
    // King Endgame PST
    private static final int[][] KING_ENDGAME_TABLE = {
        {-50, -40, -30, -20, -20, -30, -40, -50}, {-30, -20, -10,   0,   0, -10, -20, -30},
        {-30, -10,  20,  30,  30,  20, -10, -30}, {-30, -10,  30,  40,  40,  30, -10, -30},
        {-30, -10,  30,  40,  40,  30, -10, -30}, {-30, -10,  20,  30,  30,  20, -10, -30},
        {-30, -30,   0,   0,   0,   0, -30, -30}, {-50, -30, -30, -30, -30, -30, -30, -50}
    };
    // --- End Piece-Square Tables ---

    // Game state needed for evaluation and hashing (passed down or part of a BoardState object)
    // For simplicity, we'll calculate these on the fly or assume they are tracked externally
    // Ideally, a BoardState object would hold pieces, turn, castling rights, en passant target, ply count etc.
    // We'll use helper methods to deduce castling rights and EP target from the board for now.

    // --- Evaluation Function ---

    /**
     * Evaluates the board state. Positive score favors the player whose turn it IS,
     * assuming Negamax structure where score = eval(player) - eval(opponent).
     * @param board The current board state.
     * @param blackTurn True if it's currently Black's turn to move.
     * @return The evaluation score from the perspective of the current player.
     */
    public static int evaluateBoard(Piece[][] board, boolean blackTurn) {
        int whiteMaterial = 0;
        int blackMaterial = 0;
        int whitePositional = 0;
        int blackPositional = 0;
        int whiteMobilityScore = 0;
        int blackMobilityScore = 0;
        int whitePawnStructure = 0;
        int blackPawnStructure = 0;
        int whiteCastleBonus = 0;
        int blackCastleBonus = 0;

        int totalMaterial = 0; // Used to determine game phase

        // Pawn structure tracking
        int[] whitePawnCounts = new int[8]; // Count pawns per file
        int[] blackPawnCounts = new int[8];
        boolean[] whitePawnOnFile = new boolean[8];
        boolean[] blackPawnOnFile = new boolean[8];

        // Determine castling rights from board state (less efficient than tracking)
        BoardStateInfo currentState = getBoardStateInfo(board);

        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                Piece p = board[r][c];
                if (p != null) {
                    int pieceBaseValue = getPieceBaseValue(p);
                    int pieceEvalValue = getPieceValue(p);
                    totalMaterial += pieceBaseValue;

                    boolean isEndgame = isEndgame(totalMaterial);
                    int positionalValue = positionalBonus(p, r, c, isEndgame);

                    if (p.getColor().equals(Color.WHITE)) {
                        whiteMaterial += pieceEvalValue;
                        whitePositional += positionalValue;
                        if (p.getPieceType().equals("pawn")) {
                            whitePawnCounts[c]++;
                            whitePawnOnFile[c] = true;
                        }
                    } else { // Black piece
                        blackMaterial += pieceEvalValue;
                        blackPositional += positionalValue;
                        if (p.getPieceType().equals("pawn")) {
                            blackPawnCounts[c]++;
                            blackPawnOnFile[c] = true;
                        }
                    }
                }
            }
        }

        // Calculate Pawn Structure Penalties/Bonuses
        for (int c = 0; c < 8; c++) {
            // Doubled Pawns
            if (whitePawnCounts[c] > 1) whitePawnStructure += (whitePawnCounts[c] - 1) * DOUBLED_PAWN_PENALTY;
            if (blackPawnCounts[c] > 1) blackPawnStructure += (blackPawnCounts[c] - 1) * DOUBLED_PAWN_PENALTY;

            // Isolated Pawns (no friendly pawn on adjacent files)
            boolean whiteIsolated = whitePawnOnFile[c] && (c == 0 || !whitePawnOnFile[c - 1]) && (c == 7 || !whitePawnOnFile[c + 1]);
            boolean blackIsolated = blackPawnOnFile[c] && (c == 0 || !blackPawnOnFile[c - 1]) && (c == 7 || !blackPawnOnFile[c + 1]);
            if (whiteIsolated) whitePawnStructure += ISOLATED_PAWN_PENALTY;
            if (blackIsolated) blackPawnStructure += ISOLATED_PAWN_PENALTY;
        }
        // Passed Pawns (needs check for opposing pawns in front on same/adjacent files)
        whitePawnStructure += calculatePassedPawnBonus(board, Color.WHITE, blackPawnOnFile);
        blackPawnStructure += calculatePassedPawnBonus(board, Color.BLACK, whitePawnOnFile);


        // Calculate Mobility (Expensive - consider simplifying or caching)
        // Note: generateMoves filters for legality, giving a true mobility count.
        // whiteMobilityScore = generateMoves(board, false, false).size() * MOBILITY_WEIGHT;
        // blackMobilityScore = generateMoves(board, true, false).size() * MOBILITY_WEIGHT;
        // Using a simpler heuristic for performance: sum potential moves per piece
        whiteMobilityScore = calculateSimpleMobility(board, Color.WHITE) * MOBILITY_WEIGHT / 2; // Scaled down
        blackMobilityScore = calculateSimpleMobility(board, Color.BLACK) * MOBILITY_WEIGHT / 2;


        // Add castling bonus if rights are intact
        if (currentState.whiteKingsideCastle || currentState.whiteQueensideCastle) whiteCastleBonus = CASTLING_BONUS;
        if (currentState.blackKingsideCastle || currentState.blackQueensideCastle) blackCastleBonus = CASTLING_BONUS;


        // Combine scores
        int whiteScore = whiteMaterial + whitePositional + whiteMobilityScore + whitePawnStructure + whiteCastleBonus;
        int blackScore = blackMaterial + blackPositional + blackMobilityScore + blackPawnStructure + blackCastleBonus;

        // Negamax requires score relative to the current player
        int evaluation = blackScore - whiteScore; // Black's advantage over White
        return blackTurn ? evaluation : -evaluation; // Return score for current player
    }

    /** Calculates bonus for passed pawns for a given color. */
    private static int calculatePassedPawnBonus(Piece[][] board, Color color, boolean[] opponentPawnsOnFile) {
        int bonus = 0;
        int direction = color.equals(Color.WHITE) ? -1 : 1;
        int startRank = color.equals(Color.WHITE) ? 6 : 1;
        int promotionRank = color.equals(Color.WHITE) ? 0 : 7;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.getPieceType().equals("pawn") && p.getColor().equals(color)) {
                    boolean isPassed = true;
                    // Check files in front (same file, adjacent files) for opponent pawns
                    for (int checkCol = Math.max(0, c - 1); checkCol <= Math.min(7, c + 1); checkCol++) {
                        for (int checkRow = r + direction; ; checkRow += direction) {
                             if (checkRow < 0 || checkRow > 7) break; // Off board
                             Piece ahead = board[checkRow][checkCol];
                             if (ahead != null && ahead.getPieceType().equals("pawn") && ahead.getColor() != color) {
                                 isPassed = false; // Opponent pawn blocks path
                                 break;
                             }
                        }
                        if (!isPassed) break; // No need to check other files if blocked
                    }

                    if (isPassed) {
                        // Scale bonus by how far advanced the pawn is
                        int rankDistance = Math.abs(r - startRank);
                        bonus += PASSED_PAWN_BONUS * (rankDistance + 1); // Simple scaling
                    }
                }
            }
        }
        return bonus;
    }

    /** Calculates a simple mobility score (sum of potential moves, not fully legal). */
    private static int calculateSimpleMobility(Piece[][] board, Color color) {
        int mobility = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.getColor().equals(color)) {
                    // Very basic: Add fixed value per piece type (crude estimate)
                    switch(p.getPieceType()){
                        case "pawn": mobility += 2; break; // Forward moves
                        case "knight": mobility += 4; break; // Average moves
                        case "bishop": mobility += 6; break; // Diagonal range
                        case "rook": mobility += 7; break; // Rank/file range
                        case "queen": mobility += 10; break; // Max range
                        case "king": mobility += 3; break; // Close range
                    }
                    // A better simple heuristic would count empty/opponent squares reachable
                }
            }
        }
        return mobility;
    }


    /** Determines if the game is likely in an endgame phase based on material. */
    private static boolean isEndgame(int totalMaterial) {
        // Threshold based on remaining major/minor pieces
        return totalMaterial < (ROOK_VALUE * 2 + KNIGHT_VALUE + BISHOP_VALUE + QUEEN_VALUE); // Example threshold
    }

    /** Gets the base value of a piece, used for game phase detection. */
    public static int getPieceBaseValue(Piece piece) {
         if (piece == null) return 0;
         switch (piece.getPieceType()) {
             case "pawn":   return PAWN_VALUE; case "knight": return KNIGHT_VALUE;
             case "bishop": return BISHOP_VALUE; case "rook":   return ROOK_VALUE;
             case "queen":  return QUEEN_VALUE; case "king":   return 0; // King value irrelevant for phase
             default:       return 0;
         }
     }

    /** Gets the evaluation value of a piece. */
    public static int getPieceValue(Piece piece) {
        if (piece == null) return 0;
        switch (piece.getPieceType()) {
            case "pawn":   return PAWN_VALUE; case "knight": return KNIGHT_VALUE;
            case "bishop": return BISHOP_VALUE; case "rook":   return ROOK_VALUE;
            case "queen":  return QUEEN_VALUE; case "king":   return KING_VALUE;
            default:       return 0;
        }
    }

    /** Gets the positional bonus for a piece based on its type, location, and game phase. */
    public static int positionalBonus(Piece piece, int row, int col, boolean isEndgame) {
        if (piece == null) return 0;
        int[][] table = null;
        switch (piece.getPieceType()) {
            case "pawn":   table = PAWN_TABLE; break;
            case "knight": table = KNIGHT_TABLE; break;
            case "bishop": table = BISHOP_TABLE; break;
            case "rook":   table = ROOK_TABLE; break;
            case "queen":  table = QUEEN_TABLE; break;
            case "king":   table = isEndgame ? KING_ENDGAME_TABLE : KING_MIDGAME_TABLE; break;
        }
        if (table == null) return 0;
        return piece.getColor().equals(Color.WHITE) ? table[row][col] : table[7 - row][col];
    }

    // --- Negamax Search with Alpha-Beta Pruning ---

    /**
     * Principal Variation Search (PVS), a variant of Negamax with alpha-beta.
     * @param board Current board state.
     * @param depth Remaining search depth.
     * @param alpha Lower bound for the score (from perspective of current player).
     * @param beta Upper bound for the score (from perspective of current player).
     * @param color 1 if the current player is maximizing (Black), -1 if minimizing (White).
     * @param boardState Current board state info (hash, castling, EP).
     * @param initialMaxDepth The maximum depth set for the entire search (used for checkmate scoring).
     * @return The score of the best move found from this position for the 'color' player.
     */
     public static int negamax(Piece[][] board, int depth, int alpha, int beta, int color, BoardStateInfo boardState, int initialMaxDepth) {
        int alphaOrig = alpha; // Store original alpha value
        long currentHash = boardState.zobristHash; // Use hash from state object

        // --- Transposition Table Lookup ---
        TranspositionEntry entry = transpositionTable.get(currentHash);
        // Check hash match and sufficient depth
        if (entry != null && entry.zobristHash == currentHash && entry.depth >= depth) {
            switch (entry.type) {
                case EXACT: return entry.value;
                case LOWERBOUND: alpha = Math.max(alpha, entry.value); break;
                case UPPERBOUND: beta = Math.min(beta, entry.value); break;
            }
            if (alpha >= beta) {
                return entry.value; // Pruning based on TT entry
            }
        }

        // --- Base Case: Depth Limit or Terminal Node ---
        if (depth == 0) {
            // Evaluate quiescent state instead of direct evaluation at depth 0
            return quiescenceSearch(board, alpha, beta, color, boardState);
        }

        // --- Recursive Search ---
        boolean isBlackTurn = (color == 1);
        List<Move> moves = generateMoves(board, isBlackTurn, true); // Generate and order moves
        if (moves.isEmpty()) {
            // Checkmate or Stalemate
            if (isKingInCheck(board, isBlackTurn ? Color.BLACK : Color.WHITE)) {
                 // Checkmate! Score is highly negative relative to the player who got mated.
                 // Add ply count to prefer faster mates for the opponent.
                return -CHECKMATE_SCORE + (initialMaxDepth - depth); // Score relative to current player
            } else {
                return 0; // Stalemate is a draw
            }
        }

        int bestValue = Integer.MIN_VALUE; // Score relative to current player
        Move bestMove = null;

        // --- Principal Variation Search Logic ---
        // 1. Search the first move (potentially the best move from TT or move ordering) with a full window.
        Move firstMove = moves.get(0);
        Piece[][] firstNewBoard = copyBoard(board);
        // Apply move and get the *next* board state (including updated hash, EP, castling)
        BoardStateInfo firstNextState = applyMove(firstNewBoard, firstMove, boardState);
        bestValue = -negamax(firstNewBoard, depth - 1, -beta, -alpha, -color, firstNextState, initialMaxDepth);
        bestMove = firstMove;
        alpha = Math.max(alpha, bestValue);

        // 2. Search remaining moves with a null window (zero window search).
        for (int i = 1; i < moves.size(); i++) {
            if (alpha >= beta) break; // Alpha-beta cutoff

            Move move = moves.get(i);
            Piece[][] newBoard = copyBoard(board);
            BoardStateInfo nextState = applyMove(newBoard, move, boardState);

            // Zero Window Search: Assume the move is worse than the current best (alpha)
            int score = -negamax(newBoard, depth - 1, -alpha - 1, -alpha, -color, nextState, initialMaxDepth);

            // If the zero window search failed high (score > alpha) and within beta,
            // it means this move might be better than the current PV. Re-search with full window.
            if (score > alpha && score < beta) {
                score = -negamax(newBoard, depth - 1, -beta, -alpha, -color, nextState, initialMaxDepth);
            }

            // Update best value and best move if a better move is found
            if (score > bestValue) {
                bestValue = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, bestValue); // Update alpha
        }


        // --- Transposition Table Store ---
        EntryType entryType;
        if (bestValue <= alphaOrig) {
            entryType = EntryType.UPPERBOUND;
        } else if (bestValue >= beta) {
            entryType = EntryType.LOWERBOUND;
        } else {
            entryType = EntryType.EXACT;
        }
        // Store entry with current hash, depth, score, best move found, and type
        transpositionTable.put(currentHash, new TranspositionEntry(currentHash, depth, bestValue, bestMove, entryType));

        return bestValue;
    }

    /**
     * Quiescence search to stabilize evaluation by exploring captures and checks beyond the fixed depth.
     * @param board Current board state.
     * @param alpha Lower bound.
     * @param beta Upper bound.
     * @param color 1 for maximizing player (Black), -1 for minimizing (White).
     * @param boardState Current board state info (hash, castling, EP).
     * @return Stable evaluation score.
     */
    public static int quiescenceSearch(Piece[][] board, int alpha, int beta, int color, BoardStateInfo boardState) {
        long currentHash = boardState.zobristHash;

        // --- Transposition Table Lookup ---
        TranspositionEntry entry = transpositionTable.get(currentHash);
         if (entry != null && entry.zobristHash == currentHash && entry.depth >= 0) { // Depth 0 for quiescence nodes
             switch (entry.type) {
                 case EXACT: return entry.value;
                 case LOWERBOUND: alpha = Math.max(alpha, entry.value); break;
                 case UPPERBOUND: beta = Math.min(beta, entry.value); break;
             }
             if (alpha >= beta) {
                 return entry.value;
             }
         }

        // --- Stand-Pat Score ---
        boolean isBlackTurn = (color == 1);
        int standPat = evaluateBoard(board, isBlackTurn); // Evaluate from current player's perspective

        // --- Beta Cutoff (Fail High) ---
        if (standPat >= beta) {
             // Store TT entry (LOWERBOUND)
             transpositionTable.put(currentHash, new TranspositionEntry(currentHash, 0, standPat, null, EntryType.LOWERBOUND));
            return beta;
        }

        // --- Delta Pruning (Optional Optimization) ---
        // If standPat + biggest_possible_gain < alpha, prune (e.g., standPat + QueenValue < alpha)
        // if (standPat + QUEEN_VALUE < alpha) { return alpha; }


        // --- Update Alpha (Lower Bound) ---
        alpha = Math.max(alpha, standPat);

        // --- Generate and Explore "Interesting" Moves (Captures and Promotions primarily) ---
        // Checks can sometimes be included but can lead to infinite loops if not careful
        List<Move> interestingMoves = generateMoves(board, isBlackTurn, true); // Order moves
        // Keep only captures and promotions for standard quiescence
        interestingMoves.removeIf(move -> !move.wasPromotion && board[move.toRow][move.toCol] == null && !isEnPassantCapture(board, move));

        int bestValue = standPat; // Initialize with stand-pat score

        for (Move move : interestingMoves) {
            Piece[][] newBoard = copyBoard(board);
            BoardStateInfo nextState = applyMove(newBoard, move, boardState);

            // Recursively call quiescence search for the opponent
            int score = -quiescenceSearch(newBoard, -beta, -alpha, -color, nextState);

             // Update best value
             bestValue = Math.max(bestValue, score);

            // --- Alpha Update and Beta Cutoff ---
            alpha = Math.max(alpha, bestValue);
            if (alpha >= beta) {
                 // Store TT entry (LOWERBOUND)
                 transpositionTable.put(currentHash, new TranspositionEntry(currentHash, 0, bestValue, move, EntryType.LOWERBOUND));
                return beta; // Prune (Fail High)
            }
        }

         // --- Store Final Result in TT ---
         EntryType entryType = (bestValue <= standPat) ? EntryType.UPPERBOUND : EntryType.EXACT;
         transpositionTable.put(currentHash, new TranspositionEntry(currentHash, 0, bestValue, null, entryType));

        return bestValue; // Return the best score found
    }

     /** Helper to check if a move is an en passant capture */
     private static boolean isEnPassantCapture(Piece[][] board, Move move) {
         Piece moved = board[move.fromRow][move.fromCol];
         return moved != null && moved.getPieceType().equals("pawn") &&
                move.fromCol != move.toCol && board[move.toRow][move.toCol] == null;
     }


    /**
     * Finds the best move for the specified player using iterative deepening Negamax.
     * @param board The current board state.
     * @param maxSearchDepth The maximum depth for iterative deepening.
     * @param blackTurn True if calculating for Black, false for White.
     * @return The best Move found.
     */
    public static Move getBestMove(Piece[][] board, int maxSearchDepth, boolean blackTurn) {
        transpositionTable.clear(); // Clear TT for new search
        zobrist.initialize(); // Ensure Zobrist keys are ready

        // Calculate initial board state (including hash)
        BoardStateInfo initialState = getBoardStateInfo(board);
        initialState.zobristHash = zobrist.calculateBoardHash(board, initialState, blackTurn);

        Move bestMoveOverall = null;
        // Initialize bestEval based on whose turn (Black maximizes, White minimizes negamax score)
        int bestEvalOverall = blackTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int color = blackTurn ? 1 : -1; // 1 for Black (maximizer), -1 for White (minimizer)

        String player = blackTurn ? "Black" : "White";
        System.out.printf("Starting Minimax Search (%s)... Max Depth: %d\n", player, maxSearchDepth);

        // Iterative Deepening Loop
        for (int depth = 1; depth <= maxSearchDepth; depth++) {
            long startTime = System.currentTimeMillis();

            // Negamax call for the current depth
            int currentEval = negamax(board, depth, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, color, initialState, depth); // Pass current depth as initialMaxDepth for this iteration

            // Retrieve the best move for this depth from the TT (stored by negamax)
            TranspositionEntry rootEntry = transpositionTable.get(initialState.zobristHash);
            // Ensure entry exists, matches hash, has a move, and is from this depth or deeper
            Move currentBestMove = (rootEntry != null && rootEntry.zobristHash == initialState.zobristHash && rootEntry.bestMove != null && rootEntry.depth >= depth)
                                   ? rootEntry.bestMove : null;

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Update overall best move if a valid move was found for this depth
            if (currentBestMove != null) {
                 bestMoveOverall = currentBestMove;
                 bestEvalOverall = currentEval; // Store the evaluation corresponding to this move/depth
                 System.out.printf("Depth %d: Best Move=%s, Eval=%d (%s perspective), Time=%.3f s\n",
                        depth, formatMove(bestMoveOverall), currentEval, player, duration / 1000.0);
            } else {
                 // Log if TT didn't yield a move for this depth
                 System.out.printf("Depth %d: No best move found in TT for this depth (Eval=%d), Time=%.3f s\n",
                        depth, currentEval, duration / 1000.0);
                 // Rely on the best move from the previous depth if available
                 if (bestMoveOverall == null && depth == 1) { // If no move found even at depth 1
                     List<Move> legalMoves = generateMoves(board, blackTurn, false);
                     if (!legalMoves.isEmpty()) {
                         bestMoveOverall = legalMoves.get(0); // Fallback: pick the first legal move
                         System.out.println("  -> Fallback: picked first legal move: " + formatMove(bestMoveOverall));
                     } else {
                         System.out.println("  -> No legal moves available!");
                         return null; // No moves possible
                     }
                 } else if (bestMoveOverall != null){
                     System.out.println("  -> Using best move from previous depth: " + formatMove(bestMoveOverall));
                 }
            }

            // Optional: Add time limit check here
        }

        System.out.printf("Search Complete. Final Best Move (%s): %s | Final Eval: %d\n",
                          player, formatMove(bestMoveOverall), bestEvalOverall);
        return bestMoveOverall;
    }


    // --- Board and Move Utility Methods ---

    /** Creates a deep copy of the board state. */
    public static Piece[][] copyBoard(Piece[][] board) {
        Piece[][] newBoard = new Piece[board.length][board[0].length];
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                if (board[r][c] != null) {
                     try {
                         newBoard[r][c] = createPieceCopy(board[r][c]);
                     } catch (Exception e) {
                         System.err.println("FATAL: Piece cloning failed: " + e.getMessage());
                         return null; // Indicate failure
                     }
                } else {
                    newBoard[r][c] = null;
                }
            }
        }
        return newBoard;
    }

     /** Helper to create a copy of a piece. */
     private static Piece createPieceCopy(Piece original) {
         if (original == null) return null;
         Piece copy = null; Color color = original.getColor();
         switch (original.getPieceType()) {
             case "pawn":   copy = new Pawn(color); break; case "knight": copy = new Knight(color); break;
             case "bishop": copy = new Bishop(color); break; case "rook":   copy = new Rook(color); break;
             case "queen":  copy = new Queen(color); break; case "king":   copy = new King(color); break;
             default: throw new IllegalArgumentException("Unknown piece type: " + original.getPieceType());
         }
         copy.setHasMoved(original.hasMoved());
         if (original instanceof Pawn && copy instanceof Pawn) {
              ((Pawn) copy).setJustMadeDoubleMove(((Pawn) original).getJustMadeDoubleMove());
         }
         if (original instanceof King && copy instanceof King) {
             ((King) copy).setPosition(((King) original).getRow(), ((King) original).getCol());
         }
         return copy;
     }


    /**
     * Applies a move to the board state and calculates the *next* BoardStateInfo
     * including the incrementally updated Zobrist hash.
     * @param board The board state (will be modified).
     * @param move The move to apply.
     * @param previousState The BoardStateInfo *before* the move.
     * @return BoardStateInfo representing the state *after* the move.
     */
    public static BoardStateInfo applyMove(Piece[][] board, Move move, BoardStateInfo previousState) {
        long currentHash = previousState.zobristHash;
        Piece movedPiece = board[move.fromRow][move.fromCol];

        // Store previous state info needed for hash updates
        int prevEnPassantTargetFile = previousState.enPassantTargetFile;
        int prevCastlingRights = previousState.getCastlingRightsMask();

        // --- Create new state object to modify ---
        BoardStateInfo nextState = new BoardStateInfo(previousState); // Copy constructor or similar needed
        nextState.enPassantTargetFile = -1; // Reset EP target by default, set later if needed

        // 1. --- Handle Castling Rights Changes ---
        // XOR out the old castling rights key
        currentHash ^= zobrist.getCastlingHash(prevCastlingRights);

        // Update castling rights based on the move
        nextState.updateCastlingRights(move, board); // Modifies nextState's rights flags

        // XOR in the new castling rights key
        currentHash ^= zobrist.getCastlingHash(nextState.getCastlingRightsMask());


        // 2. --- Handle En Passant Target Changes ---
        // XOR out the old EP target key (if one existed)
        if (prevEnPassantTargetFile != -1) {
            currentHash ^= zobrist.getEnPassantHash(prevEnPassantTargetFile);
        }

        // Determine if this move creates a new EP target (double pawn push)
        if (movedPiece.getPieceType().equals("pawn") && Math.abs(move.toRow - move.fromRow) == 2) {
            nextState.enPassantTargetFile = move.toCol; // Target file is the destination column
            // XOR in the new EP target key
            currentHash ^= zobrist.getEnPassantHash(nextState.enPassantTargetFile);
        }
        // Else: No new EP target created by this move, nextState.enPassantTargetFile remains -1, no key XORed in.


        // 3. --- Update Piece Positions and Hashes ---

        // XOR out moved piece from source square
        currentHash ^= zobrist.getPieceHash(movedPiece, move.fromRow, move.fromCol);

        // Handle Capture (including standard and en passant)
        Piece capturedPiece = board[move.toRow][move.toCol]; // Standard capture target
        boolean isEnPassant = false;
        if (movedPiece.getPieceType().equals("pawn") && move.fromCol != move.toCol && capturedPiece == null) {
            // En Passant capture
            int capturedPawnRow = move.fromRow;
            int capturedPawnCol = move.toCol;
            capturedPiece = board[capturedPawnRow][capturedPawnCol]; // Actual captured pawn
            if (capturedPiece != null) {
                 currentHash ^= zobrist.getPieceHash(capturedPiece, capturedPawnRow, capturedPawnCol); // XOR out captured EP pawn
                 board[capturedPawnRow][capturedPawnCol] = null; // Remove from board
                 move.capturedPiece = capturedPiece; // Ensure move object knows about capture
                 isEnPassant = true;
            }
        } else if (capturedPiece != null) {
            // Standard capture
            currentHash ^= zobrist.getPieceHash(capturedPiece, move.toRow, move.toCol); // XOR out captured piece
            // Check if captured rook affects castling rights
            nextState.updateCastlingRightsForCapture(move.toRow, move.toCol);
        }

        // Move the piece on the board
        board[move.toRow][move.toCol] = movedPiece;
        board[move.fromRow][move.fromCol] = null;
        // XOR in moved piece at destination square
        currentHash ^= zobrist.getPieceHash(movedPiece, move.toRow, move.toCol);


        // 4. --- Handle Special Moves (Castling Rook, Promotion) ---

        // Castling: Move the rook as well
        if (move.wasCastling) {
            Piece rook = board[move.rookFromRow][move.rookFromCol]; // Rook should be at its original position
            if (rook != null) {
                currentHash ^= zobrist.getPieceHash(rook, move.rookFromRow, move.rookFromCol); // XOR out rook at source
                board[move.rookToRow][move.rookToCol] = rook;
                board[move.rookFromRow][move.rookFromCol] = null;
                rook.setHasMoved(true);
                currentHash ^= zobrist.getPieceHash(rook, move.rookToRow, move.rookToCol); // XOR in rook at destination
            }
        }

        // Promotion: Replace pawn with new piece
        if (move.wasPromotion) {
            Color promoColor = movedPiece.getColor();
            currentHash ^= zobrist.getPieceHash(movedPiece, move.toRow, move.toCol); // XOR out the pawn hash

            Piece promotedPiece = new Queen(promoColor); // Assume Queen
            promotedPiece.setHasMoved(true);
            board[move.toRow][move.toCol] = promotedPiece;

            currentHash ^= zobrist.getPieceHash(promotedPiece, move.toRow, move.toCol); // XOR in the new piece hash
            movedPiece = promotedPiece; // Update reference
        }

        // 5. --- Update Piece State (hasMoved, King position, Pawn double move) ---
        if (!move.wasCastling) { // King/Rook hasMoved updated by castling rights logic
             movedPiece.setHasMoved(true);
        }
        if (movedPiece instanceof King) {
             ((King) movedPiece).setPosition(move.toRow, move.toCol);
        }
        // Reset double move flag for all pawns *except* the one that might have just moved
        boolean justMadeDouble = movedPiece instanceof Pawn && Math.abs(move.toRow - move.fromRow) == 2;
        for(int r=0; r<8; r++){
             for(int c=0; c<8; c++){
                 Piece p = board[r][c];
                 if(p instanceof Pawn){
                     if(p == movedPiece){
                         ((Pawn)p).setJustMadeDoubleMove(justMadeDouble);
                     } else if (p.getColor() == movedPiece.getColor()) {
                         // Reset flag for other friendly pawns only
                         ((Pawn)p).setJustMadeDoubleMove(false);
                     }
                 }
             }
         }


        // 6. --- Toggle Side to Move Hash ---
        currentHash ^= zobrist.getSideToMoveHash();

        // 7. --- Finalize Next State ---
        nextState.zobristHash = currentHash;
        // nextState already has updated castling and EP target file

        return nextState;
    }


    // --- Move Generation (Largely unchanged, uses helpers below) ---

    /** Generates legal moves for the specified player. */
    public static List<Move> generateMoves(Piece[][] board, boolean blackTurn, boolean ordered) {
        List<Move> pseudoMoves = new ArrayList<>();
        Color playerColor = blackTurn ? Color.BLACK : Color.WHITE;
        BoardStateInfo currentState = getBoardStateInfo(board); // Get EP target, castling rights

        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                Piece p = board[r][c];
                if (p != null && p.getColor().equals(playerColor)) {
                    generatePieceMoves(board, r, c, p, pseudoMoves);
                    if (p instanceof King && !p.hasMoved()) {
                        addCastlingMoves(board, r, c, (King)p, pseudoMoves, currentState);
                    }
                    if (p instanceof Pawn) {
                        addEnPassantMoves(board, r, c, (Pawn)p, pseudoMoves, currentState.enPassantTargetFile);
                    }
                }
            }
        }
        List<Move> legalMoves = filterLegalMoves(board, pseudoMoves, blackTurn);
        if (ordered) { orderMoves(board, legalMoves); }
        return legalMoves;
    }

    /** Generates standard moves for a single piece. */
    private static void generatePieceMoves(Piece[][] board, int r, int c, Piece p, List<Move> moves) {
        for (int tr = 0; tr < board.length; tr++) {
            for (int tc = 0; tc < board[r].length; tc++) {
                if (p.isValidMove(r, c, tr, tc, board)) {
                    Move move = new Move(r, c, tr, tc, p);
                    move.capturedPiece = board[tr][tc];
                    if (p.getPieceType().equals("pawn") && (tr == 0 || tr == 7)) {
                        move.wasPromotion = true;
                    }
                    moves.add(move);
                }
            }
        }
    }

    /** Adds potential castling moves. */
    private static void addCastlingMoves(Piece[][] board, int r, int c, King king, List<Move> moves, BoardStateInfo state) {
        if (isKingInCheck(board, king.getColor())) return;
        Color opponentColor = king.getColor().equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
        boolean isWhite = king.getColor().equals(Color.WHITE);

        // Kingside
        if ((isWhite && state.whiteKingsideCastle) || (!isWhite && state.blackKingsideCastle)) {
            if (board[r][c + 1] == null && board[r][c + 2] == null) {
                if (!isSquareUnderAttack(board, r, c + 1, opponentColor) && !isSquareUnderAttack(board, r, c + 2, opponentColor)) {
                    Move castling = new Move(r, c, r, c + 2, king); castling.wasCastling = true;
                    castling.rookFromRow = r; castling.rookFromCol = 7; castling.rookToRow = r; castling.rookToCol = c + 1;
                    // We don't need to store rookPiece/rookHadMoved if applyMove deduces it
                    moves.add(castling);
                }
            }
        }
        // Queenside
        if ((isWhite && state.whiteQueensideCastle) || (!isWhite && state.blackQueensideCastle)) {
            if (board[r][c - 1] == null && board[r][c - 2] == null && board[r][c - 3] == null) {
                 if (!isSquareUnderAttack(board, r, c - 1, opponentColor) && !isSquareUnderAttack(board, r, c - 2, opponentColor)) {
                    Move castling = new Move(r, c, r, c - 2, king); castling.wasCastling = true;
                    castling.rookFromRow = r; castling.rookFromCol = 0; castling.rookToRow = r; castling.rookToCol = c - 1;
                    moves.add(castling);
                 }
            }
        }
    }

     /** Adds potential en passant capture moves. */
     private static void addEnPassantMoves(Piece[][] board, int r, int c, Pawn pawn, List<Move> moves, int enPassantTargetFile) {
         if (enPassantTargetFile == -1) return; // No EP target available

         int direction = pawn.getColor().equals(Color.WHITE) ? -1 : 1;
         int targetRow = r + direction;
         // Target row must be valid (White on row 2, Black on row 5)
         if (!((pawn.getColor().equals(Color.WHITE) && r == 3 && targetRow == 2) || (pawn.getColor().equals(Color.BLACK) && r == 4 && targetRow == 5))) {
              return;
         }

         // Check capture to the left (target column is EP file)
         if (c > 0 && (c - 1) == enPassantTargetFile) {
             Piece adjacentPiece = board[r][c - 1]; // Pawn to be captured is adjacent
             if (adjacentPiece instanceof Pawn && adjacentPiece.getColor() != pawn.getColor()) {
                  Move enPassant = new Move(r, c, targetRow, c - 1, pawn);
                  enPassant.capturedPiece = adjacentPiece; // Mark the captured piece
                  moves.add(enPassant);
             }
         }
         // Check capture to the right
         if (c < 7 && (c + 1) == enPassantTargetFile) {
              Piece adjacentPiece = board[r][c + 1];
              if (adjacentPiece instanceof Pawn && adjacentPiece.getColor() != pawn.getColor()) {
                   Move enPassant = new Move(r, c, targetRow, c + 1, pawn);
                   enPassant.capturedPiece = adjacentPiece;
                   moves.add(enPassant);
              }
         }
     }


    /** Filters pseudo-legal moves for legality (king safety). */
    private static List<Move> filterLegalMoves(Piece[][] board, List<Move> pseudoMoves, boolean blackTurn) {
        List<Move> legalMoves = new ArrayList<>();
        Color kingColor = blackTurn ? Color.BLACK : Color.WHITE;
        BoardStateInfo initialInfo = getBoardStateInfo(board); // Needed for applyMove

        for (Move move : pseudoMoves) {
            Piece[][] tempBoard = copyBoard(board);
            if (tempBoard == null) continue;
            // Apply move temporarily - use a version that doesn't need full state if possible,
            // but applyMoveForCheckTest needs updating to handle state correctly or applyMove used carefully.
            // For simplicity, we might re-use applyMove but discard the returned state.
            // Using applyMoveForCheckTest which updates king position internally.
            applyMoveForCheckTest(tempBoard, move); // This needs to be robust

            int[] kingPos = findKing(tempBoard, kingColor);
            if (kingPos != null && !isKingInCheck(tempBoard, kingColor)) {
                legalMoves.add(move);
            }
        }
        return legalMoves;
    }

    /** Simplified applyMove for check testing - needs careful state handling or replacement. */
     private static void applyMoveForCheckTest(Piece[][] board, Move move) {
         Piece pieceToMove = board[move.fromRow][move.fromCol];
         if (pieceToMove == null) return;

         // Handle en passant capture target removal
         if (pieceToMove.getPieceType().equals("pawn") && move.fromCol != move.toCol && board[move.toRow][move.toCol] == null) {
             board[move.fromRow][move.toCol] = null;
         }
         // Move the piece
         board[move.toRow][move.toCol] = pieceToMove;
         board[move.fromRow][move.fromCol] = null;
         // Handle castling rook move
         if (move.wasCastling) {
             Piece rook = board[move.rookFromRow][move.rookFromCol]; // Find rook at source
             if (rook != null) {
                 board[move.rookToRow][move.rookToCol] = rook;
                 board[move.rookFromRow][move.rookFromCol] = null;
             }
         }
         // Handle promotion
         if (move.wasPromotion) {
             board[move.toRow][move.toCol] = new Queen(pieceToMove.getColor());
             if (pieceToMove instanceof King) pieceToMove = board[move.toRow][move.toCol];
         }
         // Update King's internal position
         if (pieceToMove instanceof King) {
             ((King) pieceToMove).setPosition(move.toRow, move.toCol);
         }
     }

    /** Checks if the king of the specified color is currently under attack. */
    static boolean isKingInCheck(Piece[][] board, Color kingColor) {
        int[] kingPos = findKing(board, kingColor);
        if (kingPos == null) return false;
        return isSquareUnderAttack(board, kingPos[0], kingPos[1], kingColor.equals(Color.WHITE) ? Color.BLACK : Color.WHITE);
    }

    /** Checks if a given square is attacked by any piece of the attackerColor. */
    private static boolean isSquareUnderAttack(Piece[][] board, int targetRow, int targetCol, Color attackerColor) {
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                Piece p = board[r][c];
                if (p != null && p.getColor().equals(attackerColor)) {
                    // Assumes Piece class has isValidAttack method (Pawn overrides it)
                    if (p.isValidAttack(r, c, targetRow, targetCol, board)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Finds the position (row, col) of the king for the given color. */
    private static int[] findKing(Piece[][] board, Color color) {
         for (int r = 0; r < board.length; r++) { // Search board directly
             for (int c = 0; c < board[r].length; c++) {
                 Piece p = board[r][c];
                 if (p instanceof King && p.getColor().equals(color)) {
                     // Update internal position for consistency if found via search
                     ((King)p).setPosition(r, c);
                     return new int[]{r, c};
                 }
             }
         }
         System.err.println("CRITICAL WARNING: King of color " + color + " not found!");
         return null;
    }

    /** Checks if the given move results in a check to the opponent's king. */
     private static boolean isCheck(Piece[][] board, Move move) {
         Piece[][] tempBoard = copyBoard(board);
         if (tempBoard == null) return false;
         applyMoveForCheckTest(tempBoard, move);
         Color opponentColor = move.movedPiece.getColor().equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
         return isKingInCheck(tempBoard, opponentColor);
     }

    // --- Move Ordering ---

    /** Calculates a heuristic priority score for a move. */
    private static int getMovePriority(Piece[][] board, Move move) {
        int priority = 0;
        Piece moved = board[move.fromRow][move.fromCol]; // Piece being moved
        if (moved == null) return Integer.MIN_VALUE; // Should not happen

        Piece captured = null; // Piece captured (if any)
        boolean isEnPassant = moved.getPieceType().equals("pawn") && move.fromCol != move.toCol && board[move.toRow][move.toCol] == null;
        if (isEnPassant) {
            captured = board[move.fromRow][move.toCol]; // EP captured pawn is adjacent
        } else {
            captured = board[move.toRow][move.toCol]; // Standard capture target
        }

        // 1. Checkmate bonus (highest priority)
        Piece[][] tempBoardCheckmate = copyBoard(board);
        if (tempBoardCheckmate != null) {
            applyMoveForCheckTest(tempBoardCheckmate, move);
            Color opponentColor = moved.getColor().equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
            if (isKingInCheck(tempBoardCheckmate, opponentColor)) {
                 List<Move> opponentReplies = generateMoves(tempBoardCheckmate, !moved.getColor().equals(Color.WHITE), false);
                 if (opponentReplies.isEmpty()) return 100000; // Checkmate!
            }
        }

        // 2. Promotion bonus
        if (move.wasPromotion) priority += 9000; // Assume Queen promotion value

        // 3. Capture bonus (MVV-LVA)
        if (captured != null && captured.getColor() != moved.getColor()) {
             priority += 1000 + (getPieceValue(captured) * 10) - getPieceValue(moved);
        }

        // 4. Check bonus
        if (isCheck(board, move)) priority += 500;

        // 5. Castling bonus
        if (move.wasCastling) priority += 300;

        // 6. Positional bonus change (heuristic)
        boolean endgame = isEndgame(0); // Crude phase estimate
        int currentPosBonus = positionalBonus(moved, move.fromRow, move.fromCol, endgame);
        int targetPosBonus = positionalBonus(moved, move.toRow, move.toCol, endgame);
        priority += (targetPosBonus - currentPosBonus);

        return priority;
    }

    /** Sorts moves in descending order based on priority. */
    private static void orderMoves(Piece[][] board, List<Move> moves) {
        moves.sort((m1, m2) -> {
            if (m1 == null && m2 == null) return 0; if (m1 == null) return 1; if (m2 == null) return -1;
            return Integer.compare(getMovePriority(board, m2), getMovePriority(board, m1));
        });
    }

    // --- Helper for Formatting ---
    private static String formatMove(Move move) {
        if (move == null) return "null";
        String from = "" + (char)('a' + move.fromCol) + (8 - move.fromRow);
        String to = "" + (char)('a' + move.toCol) + (8 - move.toRow);
        String promotion = move.wasPromotion ? "q" : "";
        return String.format("%s%s%s", from, to, promotion);
    }

    // --- Zobrist Hashing Inner Class (Fully Implemented) ---
    private static class ZobristHashing {
        // Piece type indices (0-11)
        private static final int WHITE_PAWN = 0, WHITE_KNIGHT = 1, WHITE_BISHOP = 2, WHITE_ROOK = 3, WHITE_QUEEN = 4, WHITE_KING = 5;
        private static final int BLACK_PAWN = 6, BLACK_KNIGHT = 7, BLACK_BISHOP = 8, BLACK_ROOK = 9, BLACK_QUEEN = 10, BLACK_KING = 11;

        private long[][] pieceKeys = new long[12][64]; // [PieceType][Square]
        private long sideToMoveKey;                    // Key for black to move
        private long[] castlingKeys = new long[16];    // Keys for each castling rights combination
        private long[] enPassantKeys = new long[9];    // Keys for EP target file (0-7), 8 for none

        private boolean initialized = false;
        private Random random = new Random(987654321L); // Fixed seed

        public ZobristHashing() { /* Init deferred */ }

        /** Initializes random keys. */
        public synchronized void initialize() { // Synchronized for potential multi-threading later
            if (initialized) return;
            for (int i = 0; i < 12; i++) for (int j = 0; j < 64; j++) pieceKeys[i][j] = random.nextLong();
            sideToMoveKey = random.nextLong();
            for (int i = 0; i < 16; i++) castlingKeys[i] = random.nextLong();
            for (int i = 0; i < 9; i++) enPassantKeys[i] = random.nextLong();
            initialized = true;
            System.out.println("Zobrist Hashing Initialized.");
        }

        /** Calculates the full Zobrist hash for a given board state. */
        public long calculateBoardHash(Piece[][] board, BoardStateInfo state, boolean blackTurn) {
            if (!initialized) initialize();
            long hash = 0;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    Piece p = board[r][c];
                    if (p != null) hash ^= getPieceHash(p, r, c);
                }
            }
            if (blackTurn) hash ^= sideToMoveKey;
            hash ^= getCastlingHash(state.getCastlingRightsMask());
            hash ^= getEnPassantHash(state.enPassantTargetFile);
            return hash;
        }

        /** Gets the key for a piece at a square. */
        public long getPieceHash(Piece piece, int row, int col) {
             if (!initialized) initialize(); if (piece == null) return 0;
             int pieceIndex = getPieceIndex(piece); int squareIndex = row * 8 + col;
             if (pieceIndex < 0 || squareIndex < 0 || squareIndex >= 64) return 0; // Error
             return pieceKeys[pieceIndex][squareIndex];
        }

        /** Gets the key for toggling the side to move. */
         public long getSideToMoveHash() { if (!initialized) initialize(); return sideToMoveKey; }

        /** Gets the key for a specific castling rights mask (0-15). */
        public long getCastlingHash(int castlingMask) {
            if (!initialized) initialize();
            if (castlingMask < 0 || castlingMask >= 16) return 0; // Error
            return castlingKeys[castlingMask];
        }

        /** Gets the key for an en passant target file (-1 for none, 0-7 for file). */
        public long getEnPassantHash(int enPassantFile) {
            if (!initialized) initialize();
            int index = (enPassantFile == -1) ? 8 : enPassantFile; // Map -1 to index 8
            if (index < 0 || index >= 9) return 0; // Error
            return enPassantKeys[index];
        }

        /** Maps a Piece object to its index (0-11). */
        private int getPieceIndex(Piece piece) {
            boolean isWhite = piece.getColor().equals(Color.WHITE);
            switch (piece.getPieceType()) {
                case "pawn":   return isWhite ? WHITE_PAWN : BLACK_PAWN;
                case "knight": return isWhite ? WHITE_KNIGHT : BLACK_KNIGHT;
                case "bishop": return isWhite ? WHITE_BISHOP : BLACK_BISHOP;
                case "rook":   return isWhite ? WHITE_ROOK : BLACK_ROOK;
                case "queen":  return isWhite ? WHITE_QUEEN : BLACK_QUEEN;
                case "king":   return isWhite ? WHITE_KING : BLACK_KING;
                default: return -1; // Error
            }
        }
    }

    // --- Board State Info Helper Class ---
    // Holds state needed for hashing and move generation beyond just piece positions
    private static class BoardStateInfo {
        long zobristHash;
        boolean whiteKingsideCastle;
        boolean whiteQueensideCastle;
        boolean blackKingsideCastle;
        boolean blackQueensideCastle;
        int enPassantTargetFile; // Column index (0-7) or -1 if none

        // Default constructor (assumes starting position state)
        public BoardStateInfo() {
            this.whiteKingsideCastle = true; this.whiteQueensideCastle = true;
            this.blackKingsideCastle = true; this.blackQueensideCastle = true;
            this.enPassantTargetFile = -1;
            this.zobristHash = 0; // Calculated separately
        }

        // Copy constructor
        public BoardStateInfo(BoardStateInfo other) {
            this.zobristHash = other.zobristHash;
            this.whiteKingsideCastle = other.whiteKingsideCastle;
            this.whiteQueensideCastle = other.whiteQueensideCastle;
            this.blackKingsideCastle = other.blackKingsideCastle;
            this.blackQueensideCastle = other.blackQueensideCastle;
            this.enPassantTargetFile = other.enPassantTargetFile;
        }

        /** Gets castling rights as a bitmask (0-15). */
        public int getCastlingRightsMask() {
            return (whiteKingsideCastle ? 8 : 0) | (whiteQueensideCastle ? 4 : 0) |
                   (blackKingsideCastle ? 2 : 0) | (blackQueensideCastle ? 1 : 0);
        }

        /** Updates castling rights based on a move involving king or rook start squares. */
        public void updateCastlingRights(Move move, Piece[][] board) {
            Piece moved = board[move.toRow][move.toCol]; // Piece after move
            if (moved == null) return; // Should not happen if called correctly

            // King moves
            if (moved.getPieceType().equals("king")) {
                if (moved.getColor().equals(Color.WHITE)) {
                    whiteKingsideCastle = false; whiteQueensideCastle = false;
                } else {
                    blackKingsideCastle = false; blackQueensideCastle = false;
                }
            }
            // Rook moves from starting square
            else if (moved.getPieceType().equals("rook")) {
                if (moved.getColor().equals(Color.WHITE)) {
                    if (move.fromRow == 7 && move.fromCol == 7) whiteKingsideCastle = false; // H1
                    if (move.fromRow == 7 && move.fromCol == 0) whiteQueensideCastle = false; // A1
                } else {
                    if (move.fromRow == 0 && move.fromCol == 7) blackKingsideCastle = false; // H8
                    if (move.fromRow == 0 && move.fromCol == 0) blackQueensideCastle = false; // A8
                }
            }
        }

         /** Updates castling rights if a rook is captured on its starting square. */
         public void updateCastlingRightsForCapture(int capturedRow, int capturedCol) {
             // If a rook is captured on its starting square, rights are lost
             if (capturedRow == 7) { // White's back rank
                 if (capturedCol == 7) whiteKingsideCastle = false; // H1
                 if (capturedCol == 0) whiteQueensideCastle = false; // A1
             } else if (capturedRow == 0) { // Black's back rank
                 if (capturedCol == 7) blackKingsideCastle = false; // H8
                 if (capturedCol == 0) blackQueensideCastle = false; // A8
             }
         }
    }

    /** Helper to get current BoardStateInfo (castling, EP) from board. Less efficient than tracking. */
    private static BoardStateInfo getBoardStateInfo(Piece[][] board) {
         BoardStateInfo info = new BoardStateInfo(); // Starts with all rights true, EP -1

         // Check White King/Rook positions and moved status
         Piece wKing = board[7][4]; Piece wRookH = board[7][7]; Piece wRookA = board[7][0];
         if (!(wKing instanceof King && wKing.getColor().equals(Color.WHITE) && !wKing.hasMoved())) {
             info.whiteKingsideCastle = false; info.whiteQueensideCastle = false;
         } else {
             if (!(wRookH instanceof Rook && wRookH.getColor().equals(Color.WHITE) && !wRookH.hasMoved())) info.whiteKingsideCastle = false;
             if (!(wRookA instanceof Rook && wRookA.getColor().equals(Color.WHITE) && !wRookA.hasMoved())) info.whiteQueensideCastle = false;
         }

         // Check Black King/Rook positions and moved status
         Piece bKing = board[0][4]; Piece bRookH = board[0][7]; Piece bRookA = board[0][0];
          if (!(bKing instanceof King && bKing.getColor().equals(Color.BLACK) && !bKing.hasMoved())) {
             info.blackKingsideCastle = false; info.blackQueensideCastle = false;
         } else {
             if (!(bRookH instanceof Rook && bRookH.getColor().equals(Color.BLACK) && !bRookH.hasMoved())) info.blackKingsideCastle = false;
             if (!(bRookA instanceof Rook && bRookA.getColor().equals(Color.BLACK) && !bRookA.hasMoved())) info.blackQueensideCastle = false;
         }

         // Find En Passant Target (requires knowing the *previous* move, hard to deduce)
         // For now, we assume it's tracked externally or reset each turn in applyMove.
         // A proper implementation needs the previous move's info.
         info.enPassantTargetFile = -1; // Cannot reliably deduce from board state alone

         return info;
     }


     // --- Add isValidAttack method to Piece base class or specific pieces ---
     // This is needed for isSquareUnderAttack to work correctly.
     // Add these methods to your actual Piece.java and Pawn.java files:

     /* In Piece.java:
     public abstract class Piece {
         // ... existing fields and methods ...

         // Checks if the piece can attack a square (geometrically)
         // Pawns override this. For others, it's the same as isValidMove.
         public boolean isValidAttack(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
             return isValidMove(fromRow, fromCol, toRow, toCol, board);
         }
     }
     */

     /* In Pawn.java:
     public class Pawn extends Piece {
         // ... existing fields and methods ...

         @Override
         public boolean isValidAttack(int fromRow, int fromCol, int toRow, int toCol, Piece[][] board) {
             int direction = color.equals(Color.WHITE) ? -1 : 1;
             // Pawns attack diagonally one square forward
             return Math.abs(toCol - fromCol) == 1 && toRow - fromRow == direction;
             // Note: This *only* checks the attack geometry, not occupancy.
         }
     }
     */
}
