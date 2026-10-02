import javax.swing.JFrame;
public class App {
    public static void main(String[] args) {
        //Set Window Size
        int rowCount =21;
        int columnCount = 19;
        int tileSize = 32;
        int boardWidth = columnCount *tileSize;
        int boardHeight = rowCount * tileSize;
        //Create window
        JFrame frame = new JFrame("Pacman");
        frame.setVisible(true);
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Pacman pacmangame = new Pacman();
        frame.add(pacmangame);
        frame.pack();

    }
}
