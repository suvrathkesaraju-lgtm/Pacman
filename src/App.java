import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pacman");
            Pacman game = new Pacman();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            setWindowIcon(frame);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }

    private static void setWindowIcon(JFrame frame) {
        java.net.URL url = App.class.getResource("/pacmanRight.png");
        if (url != null) {
            Image icon = new ImageIcon(url).getImage();
            frame.setIconImage(icon);
        }
    }
}
