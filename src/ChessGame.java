import javax.swing.SwingUtilities;

public class ChessGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessBoard());
    }
}
