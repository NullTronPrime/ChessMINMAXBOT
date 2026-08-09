import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.HashMap;
import java.util.Map;

public class ChessBoard extends JFrame {
    // Board configuration and UI components.
    private final int BOARD_SIZE = 8;
    private final int SQUARE_SIZE = 80;
    private final JButton[][] squares = new JButton[BOARD_SIZE][BOARD_SIZE];
    private Piece[][] pieces = new Piece[BOARD_SIZE][BOARD_SIZE];
    private boolean whiteTurn = true;
    private JButton selectedSquare;
    private JLabel turnLabel;
    private JLabel statusLabel;
    private JPanel capturedPiecesPanel;
    
    // Captured pieces list and move undo history.
    private List<Piece> capturedPieces = new ArrayList<>();
    private Stack<Move> moveHistory = new Stack<>();
    
    // Colors and board style.
    private Color lightSquareColor;
    private Color darkSquareColor;
    private final Color highlightColor = new Color(124, 192, 203);
    private final Color validMoveColor = new Color(130, 151, 105);
    private final Color checkColor = new Color(255, 107, 107);
    
    private int boardStyle = 0;    // 0 = Classic, 1 = Alternative.
    private boolean usePngImages = true; // true = use PNG images; false = Unicode fallback.
    private Map<String, ImageIcon> pieceIcons = new HashMap<>();
    
    // Game state.
    private boolean gameOver = false;
    private King whiteKing;
    private King blackKing;
    private List<Point> validMoves = new ArrayList<>();
    
    // En passant target file (0-7) or -1 if none; updated after each move.
    private int enPassantTargetFile = -1;
    // Halfmove clock for the 50-move rule.
    private int halfmoveClock = 0;
    // Position repetition counts for the threefold repetition rule.
    private Map<Long, Integer> positionCounts = new HashMap<>();
    // True while the bot is thinking (blocks user input).
    private boolean botThinking = false;
    private JButton botMoveButton;
    
    // New progress bar for minimax calculation.
    private JProgressBar progressBar;
    
    public ChessBoard() {
        setTitle("Chess Game");
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        updateBoardStyle();
        
        // Top panel: Turn and status labels.
        JPanel topPanel = new JPanel(new BorderLayout());
        turnLabel = new JLabel("White's turn", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 24));
        statusLabel = new JLabel("Game in progress", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        topPanel.add(turnLabel, BorderLayout.NORTH);
        topPanel.add(statusLabel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);
        
        // Captured pieces panel.
        capturedPiecesPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        capturedPiecesPanel.setPreferredSize(new Dimension(SQUARE_SIZE, BOARD_SIZE * SQUARE_SIZE));
        capturedPiecesPanel.setBorder(BorderFactory.createTitledBorder("Captured Pieces"));
        add(capturedPiecesPanel, BorderLayout.EAST);
        
        // Initialize progress bar.
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false); // Hidden when not calculating.
        
        // Load piece icons.
        loadPieceIcons();
        
        // Board coordinate labels.
        JPanel filePanel = new JPanel(new GridLayout(1, BOARD_SIZE));
        for (int i = 0; i < BOARD_SIZE; i++) {
            JLabel fileLabel = new JLabel(String.valueOf((char) ('a' + i)), SwingConstants.CENTER);
            filePanel.add(fileLabel);
        }
        JPanel rankPanel = new JPanel(new GridLayout(BOARD_SIZE, 1));
        for (int i = BOARD_SIZE - 1; i >= 0; i--) {
            JLabel rankLabel = new JLabel(String.valueOf(i + 1), SwingConstants.CENTER);
            rankPanel.add(rankLabel);
        }
        
        // Board grid.
        JPanel gridPanel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                JButton square = new JButton();
                square.setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
                square.setBackground((row + col) % 2 == 0 ? lightSquareColor : darkSquareColor);
                square.setOpaque(true);
                square.setBorderPainted(false);
                square.setFocusPainted(false);
                squares[row][col] = square;
                gridPanel.add(square);
                
                // Place starting pieces.
                if (row == 0 || row == 7) {
                    String pieceName = getPieceName(col);
                    Piece piece = createPiece(pieceName, row == 0 ? Color.BLACK : Color.WHITE);
                    pieces[row][col] = piece;
                    String key = (row == 0 ? "black_" : "white_") + piece.getPieceType();
                    square.setIcon(pieceIcons.get(key));
                    if (piece.getPieceType().equals("king")) {
                        if (row == 0) {
                            blackKing = (King) piece;
                            blackKing.setPosition(row, col);
                        } else {
                            whiteKing = (King) piece;
                            whiteKing.setPosition(row, col);
                        }
                    }
                } else if (row == 1 || row == 6) {
                    String pieceName = "pawn";
                    Piece piece = createPiece(pieceName, row == 1 ? Color.BLACK : Color.WHITE);
                    pieces[row][col] = piece;
                    String key = (row == 1 ? "black_" : "white_") + piece.getPieceType();
                    square.setIcon(pieceIcons.get(key));
                }
                
                final int fRow = row, fCol = col;
                square.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (!gameOver)
                            handleMove(fRow, fCol);
                    }
                });
            }
        }
        JPanel fullBoardPanel = new JPanel(new BorderLayout());
        fullBoardPanel.add(filePanel, BorderLayout.SOUTH);
        fullBoardPanel.add(rankPanel, BorderLayout.WEST);
        fullBoardPanel.add(gridPanel, BorderLayout.CENTER);
        add(fullBoardPanel, BorderLayout.CENTER);
        positionCounts.put(Minimax.computePositionKey(pieces, !whiteTurn, enPassantTargetFile), 1);
        
        // Control panel.
        JPanel controlPanel = new JPanel();
        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> resetGame());
        JButton undoButton = new JButton("Undo Move");
        undoButton.addActionListener(e -> undoMove());
        JButton switchBoardStyleButton = new JButton("Switch Board Style");
        switchBoardStyleButton.addActionListener(e -> {
            boardStyle = (boardStyle + 1) % 2;
            updateBoardStyle();
            resetSquareColors();
        });
        JButton switchPieceStyleButton = new JButton("Switch Piece Style");
        switchPieceStyleButton.addActionListener(e -> {
            usePngImages = !usePngImages;
            loadPieceIcons();
            updateBoardIcons();
            updateCapturedPiecesDisplay();
        });
        botMoveButton = new JButton("Bot Move (Black)");
        botMoveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (botThinking) {
                    return;
                }
                if (whiteTurn) {
                    JOptionPane.showMessageDialog(null, "It is not Black's turn!");
                    return;
                }
                // Start minimax search in a background thread.
                botThinking = true;
                botMoveButton.setEnabled(false);
                MinimaxWorker worker = new MinimaxWorker(7); // Increase depth as desired.
                worker.execute();
            }
        });
        
        controlPanel.add(newGameButton);
        controlPanel.add(undoButton);
        controlPanel.add(switchBoardStyleButton);
        controlPanel.add(switchPieceStyleButton);
        controlPanel.add(botMoveButton);
        // Add progress bar to the control panel.
        controlPanel.add(progressBar);
        add(controlPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    // ---------- Helper Methods (Consolidated) ----------
    
    private void updateBoardStyle() {
        if (boardStyle == 0) {
            lightSquareColor = new Color(240, 217, 181);
            darkSquareColor = new Color(181, 136, 99);
        } else {
            lightSquareColor = new Color(222, 235, 219);
            darkSquareColor = new Color(125, 173, 130);
        }
    }
    
    private void loadPieceIcons() {
        pieceIcons.clear();
        String[] pieceTypes = {"rook", "knight", "bishop", "queen", "king", "pawn"};
        String[] colors = {"white", "black"};
        for (String color : colors) {
            for (String pieceType : pieceTypes) {
                String key = color + "_" + pieceType;
                if (usePngImages) {
                    BufferedImage img = null;
                    try {
                        URL url = getClass().getResource("/" + color.charAt(0) + "_" + pieceType + ".png");
                        if (url != null)
                            img = ImageIO.read(url);
                    } catch (Exception ex) {
                        System.err.println("Error loading classpath image for " + key + ": " + ex.getMessage());
                    }
                    // PNGs ship in src/ but may not be on the classpath; fall back to the filesystem.
                    if (img == null) {
                        File file = new File("src" + File.separator + color.charAt(0) + "_" + pieceType + ".png");
                        if (!file.exists())
                            file = new File(color.charAt(0) + "_" + pieceType + ".png");
                        try {
                            if (file.exists())
                                img = ImageIO.read(file);
                        } catch (Exception ex) {
                            System.err.println("Error loading image file for " + key + ": " + ex.getMessage());
                        }
                    }
                    if (img != null) {
                        Image resized = img.getScaledInstance(SQUARE_SIZE - 10, SQUARE_SIZE - 10, Image.SCALE_SMOOTH);
                        pieceIcons.put(key, new ImageIcon(resized));
                    } else {
                        createFallbackIcon(color, pieceType, key);
                    }
                } else {
                    createFallbackIcon(color, pieceType, key);
                }
            }
        }
    }
    
    private void createFallbackIcon(String color, String pieceType, String key) {
        JLabel temp = new JLabel(getUnicodeChessPiece(color, pieceType));
        temp.setFont(new Font("Arial Unicode MS", Font.BOLD, 36));
        temp.setForeground(color.equals("white") ? Color.WHITE : Color.BLACK);
        temp.setHorizontalAlignment(SwingConstants.CENTER);
        temp.setSize(SQUARE_SIZE, SQUARE_SIZE);
        BufferedImage fb = new BufferedImage(SQUARE_SIZE, SQUARE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = fb.createGraphics();
        g2d.setColor(color.equals("white") ? Color.BLACK : Color.WHITE);
        g2d.fillRect(0, 0, SQUARE_SIZE, SQUARE_SIZE);
        temp.paint(g2d);
        g2d.dispose();
        pieceIcons.put(key, new ImageIcon(fb));
    }
    
    private String getUnicodeChessPiece(String color, String pieceType) {
        if (color.equals("white")) {
            switch(pieceType) {
                case "king": return "♔";
                case "queen": return "♕";
                case "rook": return "♖";
                case "bishop": return "♗";
                case "knight": return "♘";
                case "pawn": return "♙";
                default: return "?";
            }
        } else {
            switch(pieceType) {
                case "king": return "♚";
                case "queen": return "♛";
                case "rook": return "♜";
                case "bishop": return "♝";
                case "knight": return "♞";
                case "pawn": return "♟";
                default: return "?";
            }
        }
    }
    
    private String getPieceName(int col) {
        switch(col) {
            case 0: case 7: return "rook";
            case 1: case 6: return "knight";
            case 2: case 5: return "bishop";
            case 3: return "queen";
            case 4: return "king";
            default: return "";
        }
    }
    
    private Piece createPiece(String pieceName, Color color) {
        switch(pieceName) {
            case "rook": return new Rook(color);
            case "knight": return new Knight(color);
            case "bishop": return new Bishop(color);
            case "queen": return new Queen(color);
            case "king": return new King(color);
            case "pawn": return new Pawn(color);
            default: return null;
        }
    }
    
    private String getPieceIconKey(Piece p) {
        return (p.getColor().equals(Color.WHITE) ? "white_" : "black_") + p.getPieceType();
    }
    
    private void updateBoardIcons() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (pieces[r][c] != null) {
                    String key = getPieceIconKey(pieces[r][c]);
                    squares[r][c].setIcon(pieceIcons.get(key));
                }
            }
        }
    }
    
    private void resetSquareColors() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                squares[i][j].setBackground((i + j) % 2 == 0 ? lightSquareColor : darkSquareColor);
                squares[i][j].setBorder(BorderFactory.createEmptyBorder());
            }
        }
    }
    
    private int getRow(JButton b) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (squares[i][j] == b)
                    return i;
            }
        }
        return -1;
    }
    
    private int getCol(JButton b) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (squares[i][j] == b)
                    return j;
            }
        }
        return -1;
    }
    
    private boolean wouldMoveExposeKing(int fromRow, int fromCol, int toRow, int toCol) {
        Piece moved = pieces[fromRow][fromCol];
        Piece captured = pieces[toRow][toCol];
        pieces[toRow][toCol] = moved;
        pieces[fromRow][fromCol] = null;
        boolean exposed = moved.getColor().equals(Color.WHITE) ? isKingInCheck(true) : isKingInCheck(false);
        pieces[fromRow][fromCol] = moved;
        pieces[toRow][toCol] = captured;
        return exposed;
    }
    
    // If a move is made by the user.
    private void handleMove(int row, int col) {
        if (botThinking)
            return;
        if (selectedSquare == null) {
            if (pieces[row][col] != null && pieces[row][col].getColor().equals(whiteTurn ? Color.WHITE : Color.BLACK)) {
                selectedSquare = squares[row][col];
                highlightSelectedSquare(row, col);
                findAndHighlightValidMoves(row, col);
            }
        } else {
            int fromRow = getRow(selectedSquare);
            int fromCol = getCol(selectedSquare);
            if (fromRow == row && fromCol == col) {
                resetSquareColors();
                selectedSquare = null;
                validMoves.clear();
                return;
            }
            if (pieces[row][col] != null && pieces[row][col].getColor().equals(whiteTurn ? Color.WHITE : Color.BLACK)) {
                resetSquareColors();
                selectedSquare = squares[row][col];
                highlightSelectedSquare(row, col);
                findAndHighlightValidMoves(row, col);
                return;
            }
            boolean valid = false;
            for (Point p : validMoves) {
                if (p.x == row && p.y == col) { valid = true; break; }
            }
            if (valid) {
                Move move = new Move();
                move.fromRow = fromRow;
                move.fromCol = fromCol;
                move.toRow = row;
                move.toCol = col;
                applyMoveToBoard(move);
                selectedSquare = null;
                validMoves.clear();
                resetSquareColors();
            } else {
                System.out.println("Invalid move");
            }
        }
    }
    
    // Fills in move metadata, executes the move on the board, and updates game state.
    private void applyMoveToBoard(Move move) {
        move.movedPiece = pieces[move.fromRow][move.fromCol];
        move.movedPieceHadMoved = move.movedPiece.hasMoved();
        move.epBeforeMove = enPassantTargetFile;
        move.halfmoveBefore = halfmoveClock;
        if (move.movedPiece instanceof Pawn)
            move.preMovedDoubleFlag = ((Pawn) move.movedPiece).getJustMadeDoubleMove();
        boolean wasEnPassant = move.movedPiece instanceof Pawn
                && move.toCol != move.fromCol
                && pieces[move.toRow][move.toCol] == null;
        if (wasEnPassant) {
            move.wasEnPassant = true;
            move.capturedPiece = pieces[move.fromRow][move.toCol];
            if (move.capturedPiece != null) {
                capturedPieces.add(move.capturedPiece);
                updateCapturedPiecesDisplay();
            }
        } else {
            move.capturedPiece = pieces[move.toRow][move.toCol];
            if (move.capturedPiece != null) {
                capturedPieces.add(move.capturedPiece);
                updateCapturedPiecesDisplay();
            }
        }
        if (move.movedPiece instanceof King && Math.abs(move.fromCol - move.toCol) == 2) {
            move.wasCastling = true;
            if (move.toCol > move.fromCol) {
                move.rookFromCol = 7;
                move.rookToCol = move.toCol - 1;
            } else {
                move.rookFromCol = 0;
                move.rookToCol = move.toCol + 1;
            }
            move.rookFromRow = move.fromRow;
            move.rookToRow = move.fromRow;
            move.rookPiece = pieces[move.fromRow][move.rookFromCol];
            move.rookHadMoved = move.rookPiece.hasMoved();
            handleCastling(move.fromRow, move.fromCol, move.toRow, move.toCol);
        } else {
            if (move.movedPiece instanceof Pawn && ((Pawn) move.movedPiece).shouldPromote(move.toRow)) {
                move.wasPromotion = true;
                move.prePromotionPiece = move.movedPiece;
            }
            movePiece(squares[move.fromRow][move.fromCol], squares[move.toRow][move.toCol]);
        }
        completeMove(move);
    }
    
    // Applies draw-rule bookkeeping (50-move rule, threefold repetition), flips the
    // turn, and evaluates check / checkmate / stalemate after a move is executed.
    private void completeMove(Move move) {
        moveHistory.push(move);
        if (move.movedPiece instanceof Pawn || move.capturedPiece != null)
            halfmoveClock = 0;
        else
            halfmoveClock++;
        if (move.movedPiece instanceof Pawn && Math.abs(move.toRow - move.fromRow) == 2)
            enPassantTargetFile = move.fromCol;
        else
            enPassantTargetFile = -1;
        whiteTurn = !whiteTurn;
        updateTurnLabel();
        long key = Minimax.computePositionKey(pieces, !whiteTurn, enPassantTargetFile);
        move.positionKeyAfter = key;
        int count = positionCounts.merge(key, 1, Integer::sum);
        boolean inCheck = isKingInCheck(whiteTurn);
        if (inCheck) {
            statusLabel.setText((whiteTurn ? "White" : "Black") + " is in check!");
            highlightKingInCheck(whiteTurn);
            if (isCheckmate(whiteTurn)) {
                statusLabel.setText("Checkmate! " + (whiteTurn ? "Black" : "White") + " wins!");
                gameOver = true;
            }
        } else {
            statusLabel.setText("Game in progress");
            if (isStalemate(whiteTurn)) {
                statusLabel.setText("Stalemate! Game ends in a draw.");
                gameOver = true;
            } else if (halfmoveClock >= 100) {
                statusLabel.setText("Draw! 50-move rule.");
                gameOver = true;
            } else if (count >= 3) {
                statusLabel.setText("Draw! Threefold repetition.");
                gameOver = true;
            }
        }
    }
    
    private void highlightSelectedSquare(int row, int col) {
        resetSquareColors();
        squares[row][col].setBackground(highlightColor);
    }
    
    private void findAndHighlightValidMoves(int row, int col) {
        validMoves.clear();
        List<Move> legalMoves = Minimax.generateMoves(pieces, !whiteTurn, false, enPassantTargetFile);
        for (Move m : legalMoves) {
            if (m.fromRow == row && m.fromCol == col) {
                validMoves.add(new Point(m.toRow, m.toCol));
                if (pieces[m.toRow][m.toCol] == null)
                    squares[m.toRow][m.toCol].setBackground(validMoveColor);
                else
                    squares[m.toRow][m.toCol].setBorder(BorderFactory.createLineBorder(validMoveColor, 3));
            }
        }
    }
    
    private void checkAndHighlightCastling(int row, int col) {
        if (canCastle(row, col, true)) {
            validMoves.add(new Point(row, col + 2));
            squares[row][col + 2].setBackground(validMoveColor);
        }
        if (canCastle(row, col, false)) {
            validMoves.add(new Point(row, col - 2));
            squares[row][col - 2].setBackground(validMoveColor);
        }
    }
    
    private boolean canCastle(int kingRow, int kingCol, boolean kingSide) {
        King king = (King) pieces[kingRow][kingCol];
        if (king.hasMoved()) return false;
        int rookCol = kingSide ? 7 : 0;
        Piece rook = pieces[kingRow][rookCol];
        if (!(rook instanceof Rook) || ((Rook) rook).hasMoved()) return false;
        int start = kingSide ? kingCol + 1 : 1;
        int end = kingSide ? 6 : kingCol - 1;
        for (int col = start; col <= end; col++) {
            if (pieces[kingRow][col] != null) return false;
        }
        Color opponent = king.getColor().equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
        if (isSquareUnderAttack(kingRow, kingCol, opponent)) return false;
        int mid = kingSide ? kingCol + 1 : kingCol - 1;
        if (isSquareUnderAttack(kingRow, mid, opponent)) return false;
        int dest = kingSide ? kingCol + 2 : kingCol - 2;
        if (isSquareUnderAttack(kingRow, dest, opponent)) return false;
        return true;
    }
    
    private void handleCastling(int fromRow, int fromCol, int toRow, int toCol) {
        movePiece(squares[fromRow][fromCol], squares[toRow][toCol]);
        int rookFromCol = (toCol > fromCol) ? 7 : 0;
        int rookToCol = (toCol > fromCol) ? toCol - 1 : toCol + 1;
        movePiece(squares[fromRow][rookFromCol], squares[fromRow][rookToCol]);
    }
    
    private void movePiece(JButton from, JButton to) {
        int fromRow = getRow(from), fromCol = getCol(from);
        int toRow = getRow(to), toCol = getCol(to);
        Piece piece = pieces[fromRow][fromCol];
        // En passant: diagonal pawn move onto an empty square.
        boolean wasEnPassant = piece instanceof Pawn && fromCol != toCol && pieces[toRow][toCol] == null;
        pieces[toRow][toCol] = piece;
        pieces[fromRow][fromCol] = null;
        to.setIcon(from.getIcon());
        from.setIcon(null);
        if (piece instanceof Rook)
            ((Rook) piece).setHasMoved(true);
        else if (piece instanceof King) {
            ((King) piece).setHasMoved(true);
            if (piece.getColor().equals(Color.WHITE))
                whiteKing.setPosition(toRow, toCol);
            else
                blackKing.setPosition(toRow, toCol);
        } else if (piece instanceof Pawn) {
            ((Pawn) piece).setJustMadeDoubleMove(Math.abs(toRow - fromRow) == 2);
            if (wasEnPassant) {
                pieces[fromRow][toCol] = null;
                squares[fromRow][toCol].setIcon(null);
            }
            if (((Pawn) piece).shouldPromote(toRow))
                promotePawn(toRow, toCol);
        }
    }
    
    private void promotePawn(int row, int col) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        String choice = (String) JOptionPane.showInputDialog(
            this,
            "Choose a piece to promote to:",
            "Pawn Promotion",
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        if (choice != null) {
            Piece newPiece = null;
            Color pieceColor = pieces[row][col].getColor();
            switch (choice) {
                case "Queen": newPiece = new Queen(pieceColor); break;
                case "Rook": newPiece = new Rook(pieceColor); break;
                case "Bishop": newPiece = new Bishop(pieceColor); break;
                case "Knight": newPiece = new Knight(pieceColor); break;
            }
            pieces[row][col] = newPiece;
            String key = (pieceColor.equals(Color.WHITE) ? "white_" : "black_") + newPiece.getPieceType();
            squares[row][col].setIcon(pieceIcons.get(key));
        }
    }
    
    private void undoMove() {
        if (moveHistory.isEmpty() || botThinking) return;
        Move last = moveHistory.pop();
        int fr = last.fromRow, fc = last.fromCol, tr = last.toRow, tc = last.toCol;
        pieces[fr][fc] = last.movedPiece;
        last.movedPiece.setHasMoved(last.movedPieceHadMoved);
        if (last.movedPiece instanceof Pawn)
            ((Pawn) last.movedPiece).setJustMadeDoubleMove(last.preMovedDoubleFlag);
        if (last.wasEnPassant) {
            pieces[tr][tc] = null;
            if (last.capturedPiece != null) {
                pieces[fr][tc] = last.capturedPiece;
                ((Pawn) last.capturedPiece).setJustMadeDoubleMove(true);
            }
        } else {
            pieces[tr][tc] = last.capturedPiece;
        }
        if (last.capturedPiece != null)
            capturedPieces.remove(last.capturedPiece);
        updateCapturedPiecesDisplay();
        if (last.wasCastling) {
            pieces[last.rookFromRow][last.rookFromCol] = last.rookPiece;
            pieces[last.rookToRow][last.rookToCol] = null;
            last.rookPiece.setHasMoved(last.rookHadMoved);
            squares[last.rookFromRow][last.rookFromCol].setIcon(pieceIcons.get(
                (last.rookPiece.getColor().equals(Color.WHITE) ? "white_" : "black_") + last.rookPiece.getPieceType()));
            squares[last.rookToRow][last.rookToCol].setIcon(null);
        }
        if (last.wasPromotion)
            pieces[tr][tc] = last.prePromotionPiece;
        String keyFrom = (pieces[fr][fc].getColor().equals(Color.WHITE) ? "white_" : "black_") + pieces[fr][fc].getPieceType();
        squares[fr][fc].setIcon(pieceIcons.get(keyFrom));
        if (last.wasEnPassant && last.capturedPiece != null) {
            String keyCaptured = (last.capturedPiece.getColor().equals(Color.WHITE) ? "white_" : "black_")
                    + last.capturedPiece.getPieceType();
            squares[fr][tc].setIcon(pieceIcons.get(keyCaptured));
        }
        if (pieces[tr][tc] != null) {
            String keyTo = (pieces[tr][tc].getColor().equals(Color.WHITE) ? "white_" : "black_") + pieces[tr][tc].getPieceType();
            squares[tr][tc].setIcon(pieceIcons.get(keyTo));
        } else {
            squares[tr][tc].setIcon(null);
        }
        // Restore draw-rule bookkeeping state.
        enPassantTargetFile = last.epBeforeMove;
        halfmoveClock = last.halfmoveBefore;
        int c = positionCounts.getOrDefault(last.positionKeyAfter, 0);
        if (c <= 1)
            positionCounts.remove(last.positionKeyAfter);
        else
            positionCounts.put(last.positionKeyAfter, c - 1);
        whiteTurn = !whiteTurn;
        gameOver = false;
        statusLabel.setText("Game in progress");
        updateTurnLabel();
        resetSquareColors();
    }
    
    private boolean isSquareUnderAttack(int row, int col, Color attacker) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (pieces[i][j] != null && pieces[i][j].getColor().equals(attacker))
                    if (pieces[i][j].isValidMove(i, j, row, col, pieces))
                        return true;
            }
        }
        return false;
    }
    
    private boolean isKingInCheck(boolean isWhite) {
        King king = isWhite ? whiteKing : blackKing;
        return isSquareUnderAttack(king.getRow(), king.getCol(),
                king.getColor().equals(Color.WHITE) ? Color.BLACK : Color.WHITE);
    }
    
    private boolean canPlayerMakeAnyMove(boolean isWhite) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (pieces[r][c] != null && pieces[r][c].getColor().equals(isWhite ? Color.WHITE : Color.BLACK))
                    for (int tr = 0; tr < BOARD_SIZE; tr++) {
                        for (int tc = 0; tc < BOARD_SIZE; tc++) {
                            if (pieces[r][c].isValidMove(r, c, tr, tc, pieces))
                                if (!wouldMoveExposeKing(r, c, tr, tc))
                                    return true;
                        }
                    }
            }
        }
        return false;
    }
    
    private boolean isCheckmate(boolean isWhite) {
        if (!isKingInCheck(isWhite))
            return false;
        return Minimax.generateMoves(pieces, !isWhite, false, enPassantTargetFile).isEmpty();
    }
    
    private boolean isStalemate(boolean isWhite) {
        if (isKingInCheck(isWhite))
            return false;
        return Minimax.generateMoves(pieces, !isWhite, false, enPassantTargetFile).isEmpty();
    }
    
    private void highlightKingInCheck(boolean isWhite) {
        King king = isWhite ? whiteKing : blackKing;
        squares[king.getRow()][king.getCol()].setBackground(checkColor);
    }
    
    private void updateTurnLabel() {
        turnLabel.setText((whiteTurn ? "White" : "Black") + "'s turn");
    }
    
    private void updateCapturedPiecesDisplay() {
        capturedPiecesPanel.removeAll();
        for (Piece p : capturedPieces) {
            String key = getPieceIconKey(p);
            ImageIcon icon = pieceIcons.get(key);
            if (icon != null) {
                capturedPiecesPanel.add(new JLabel(icon));
            } else {
                capturedPiecesPanel.add(new JLabel("Missing"));
            }
        }
        capturedPiecesPanel.revalidate();
        capturedPiecesPanel.repaint();
    }
    
    private void resetGame() {
        if (botThinking) return;
        for (int i = 0; i < BOARD_SIZE; i++)
            for (int j = 0; j < BOARD_SIZE; j++) {
                pieces[i][j] = null;
                squares[i][j].setIcon(null);
                squares[i][j].setBackground((i + j) % 2 == 0 ? lightSquareColor : darkSquareColor);
                squares[i][j].setBorder(BorderFactory.createEmptyBorder());
            }
        whiteTurn = true;
        selectedSquare = null;
        validMoves.clear();
        gameOver = false;
        capturedPieces.clear();
        moveHistory.clear();
        enPassantTargetFile = -1;
        halfmoveClock = 0;
        positionCounts.clear();
        botThinking = false;
        if (botMoveButton != null)
            botMoveButton.setEnabled(true);
        updateCapturedPiecesDisplay();
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (r == 0 || r == 7) {
                    String pieceName = getPieceName(c);
                    Piece p = createPiece(pieceName, r == 0 ? Color.BLACK : Color.WHITE);
                    pieces[r][c] = p;
                    String key = (r == 0 ? "black_" : "white_") + p.getPieceType();
                    squares[r][c].setIcon(pieceIcons.get(key));
                    if (p.getPieceType().equals("king")) {
                        if (r == 0) {
                            blackKing = (King) p;
                            blackKing.setPosition(r, c);
                        } else {
                            whiteKing = (King) p;
                            whiteKing.setPosition(r, c);
                        }
                    }
                } else if (r == 1 || r == 6) {
                    String pieceName = "pawn";
                    Piece p = createPiece(pieceName, r == 1 ? Color.BLACK : Color.WHITE);
                    pieces[r][c] = p;
                    String key = (r == 1 ? "black_" : "white_") + p.getPieceType();
                    squares[r][c].setIcon(pieceIcons.get(key));
                }
            }
        }
        updateTurnLabel();
        statusLabel.setText("Game in progress");
        positionCounts.put(Minimax.computePositionKey(pieces, !whiteTurn, enPassantTargetFile), 1);
        resetSquareColors();
    }
    
    // ---------- Minimax SwingWorker ----------
    private class MinimaxWorker extends SwingWorker<Move, Void> {
        private int depth;
        public MinimaxWorker(int depth) {
            this.depth = depth;
        }
        @Override
        protected Move doInBackground() throws Exception {
            // Set progress bar to indeterminate mode.
            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(true);
                progressBar.setVisible(true);
            });
            // Perform minimax search.
            Move bestMove = Minimax.getBestMove(pieces, depth, !whiteTurn, Long.MAX_VALUE, enPassantTargetFile);
            return bestMove;
        }
        @Override
        protected void done() {
            botThinking = false;
            botMoveButton.setEnabled(true);
            try {
                Move bestMove = get();
                if (bestMove != null) {
                    makeMove(bestMove);
                } else {
                    JOptionPane.showMessageDialog(null, "No legal moves found by bot.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
        }
    }
    
    // Applies a move to the current board as selected by the bot.
    private void makeMove(Move bestMove) {
        Move move = new Move();
        move.fromRow = bestMove.fromRow;
        move.fromCol = bestMove.fromCol;
        move.toRow = bestMove.toRow;
        move.toCol = bestMove.toCol;
        applyMoveToBoard(move);
        resetSquareColors();
    }
}
