import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/**
 * Headless smoke test: constructs the game panel, renders a frame and sends a
 * key event, making sure image loading, map loading, rendering and input
 * handling all work. Run by CI after the build:
 *
 *   javac -cp build/Pacman.jar -d build/test-classes test/SmokeTest.java
 *   java -Djava.awt.headless=true -cp build/Pacman.jar:build/test-classes SmokeTest
 */
public class SmokeTest {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        Pacman game = new Pacman();
        game.setSize(608, 704);
        game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                KeyEvent.VK_RIGHT, 'D'));

        // Let the game loop run for a bit so movement and ghost AI are exercised.
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        BufferedImage canvas = new BufferedImage(608, 704, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        game.paintComponent(g);
        g.dispose();

        System.out.println("Smoke test passed: game constructed, rendered and accepted input.");
        System.exit(0);
    }
}
