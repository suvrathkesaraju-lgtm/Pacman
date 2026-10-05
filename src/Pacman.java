import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import javax.swing.*;

public class Pacman extends JPanel implements ActionListener, KeyListener {

    /** A wall tile, food pellet, power pellet, ghost or pacman. */
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;        // image currently drawn
        Image normalImage;  // image when in normal state (not frightened)
        int startX;
        int startY;
        int velocityX = 0;
        int velocityY = 0;
        char direction = LEFT;      // current heading
        char nextDirection = LEFT;  // queued heading (pacman turns ahead of time)
        boolean frightened = false; // ghosts only

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.normalImage = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void reset() {
            x = startX;
            y = startY;
            velocityX = 0;
            velocityY = 0;
            direction = LEFT;
            nextDirection = LEFT;
            frightened = false;
            image = normalImage;
        }
    }

    // X = wall, space = food, 0 = tunnel (no food), * = power pellet,
    // P = pacman, b/o/p/r = blue/orange/pink/red ghost
    private final String[] tileMap = {
            "XXXXXXXXXXXXXXXXXXX",
            "X                 X",
            "X XX XXX X XXX XX X",
            "X *             * X",
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "000X X       X X000",
            "XXXX X XXrXX X XXXX",
            "0       bpo       0",
            "XXXX X XXXXX X XXXX",
            "000X X       X X000",
            "XXXX X XXXXX X XXXX",
            "X *      X      * X",
            "X XX XXX X XXX XX X",
            "X  X     P     X  X",
            "XX X X XXXXX X X XX",
            "X    X   X   X    X",
            "X XXXXXX X XXXXXX X",
            "X                 X",
            "XXXXXXXXXXXXXXXXXXX"
    };

    private static final char UP = 'U', DOWN = 'D', LEFT = 'L', RIGHT = 'R';

    private static final int FPS = 60;
    private static final int PACMAN_SPEED = 2;      // px per frame
    private static final int GHOST_SPEED = 2;       // px per frame
    private static final int FRIGHTENED_SPEED = 1;  // px per frame
    private static final int FRIGHTENED_FRAMES = 6 * FPS;
    private static final int START_LIVES = 3;

    private final int rowCount = 21;
    private final int columnCount = 19;
    private final int tileSize = 32;
    private final int boardWidth = columnCount * tileSize;
    private final int boardHeight = rowCount * tileSize;
    private final int hudHeight = tileSize; // strip above the maze for score/lives

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;
    private Image scaredGhostImage;
    private Image powerFoodImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanRightImage;
    private Image pacmanLeftImage;

    private final HashSet<Block> walls = new HashSet<>();
    private final HashSet<Block> foods = new HashSet<>();
    private final HashSet<Block> powerFoods = new HashSet<>();
    private final ArrayList<Block> ghosts = new ArrayList<>();
    private Block pacman;

    private int score = 0;
    private int lives = START_LIVES;
    private int frightenedFrames = 0; // remaining frames of fright
    private int ghostCombo = 0;       // consecutive ghosts eaten while frightened
    private boolean awaitingInput = true; // true until the player presses a direction key
    private boolean gameOver = false;
    private boolean won = false;
    private boolean paused = false;

    private final Random random = new Random();
    private final Timer timer;

    Pacman() {
        setPreferredSize(new Dimension(boardWidth, boardHeight + hudHeight));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        loadImages();
        loadMap();

        timer = new Timer(1000 / FPS, this);
        timer.start();
    }

    // ---------------------------------------------------------------- setup

    private void loadImages() {
        wallImage = loadImage("wall.png");
        blueGhostImage = loadImage("blueGhost.png");
        orangeGhostImage = loadImage("orangeGhost.png");
        pinkGhostImage = loadImage("pinkGhost.png");
        redGhostImage = loadImage("redGhost.png");
        scaredGhostImage = loadImage("scaredGhost.png");
        powerFoodImage = loadImage("powerFood.png");
        pacmanUpImage = loadImage("pacmanUp.png");
        pacmanDownImage = loadImage("pacmanDown.png");
        pacmanLeftImage = loadImage("pacmanLeft.png");
        pacmanRightImage = loadImage("pacmanRight.png");
    }

    private Image loadImage(String name) {
        java.net.URL url = getClass().getResource("/" + name);
        if (url == null) {
            throw new IllegalStateException("Missing image resource: " + name
                    + " (make sure src/*.png is on the classpath)");
        }
        return new ImageIcon(url).getImage();
    }

    private void loadMap() {
        walls.clear();
        foods.clear();
        powerFoods.clear();
        ghosts.clear();

        for (int r = 0; r < rowCount; r++) {
            String row = tileMap[r];
            for (int c = 0; c < columnCount; c++) {
                char tile = row.charAt(c);
                int x = c * tileSize;
                int y = r * tileSize;

                switch (tile) {
                    case 'X' -> walls.add(new Block(wallImage, x, y, tileSize, tileSize));
                    case 'b' -> ghosts.add(new Block(blueGhostImage, x, y, tileSize, tileSize));
                    case 'o' -> ghosts.add(new Block(orangeGhostImage, x, y, tileSize, tileSize));
                    case 'p' -> ghosts.add(new Block(pinkGhostImage, x, y, tileSize, tileSize));
                    case 'r' -> ghosts.add(new Block(redGhostImage, x, y, tileSize, tileSize));
                    case 'P' -> pacman = new Block(pacmanLeftImage, x, y, tileSize, tileSize);
                    case '*' -> powerFoods.add(new Block(powerFoodImage, x, y, tileSize, tileSize));
                    case ' ' -> foods.add(new Block(null, x + 14, y + 14, 4, 4));
                    case '0' -> { /* tunnel: empty tile with no food */ }
                    default -> throw new IllegalStateException("Unknown tile '" + tile + "' at row " + r + ", column " + c);
                }
            }
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_P && !gameOver && !won && !awaitingInput) {
            paused = !paused;
            return;
        }

        char dir = switch (e.getKeyCode()) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> UP;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> DOWN;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> LEFT;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> RIGHT;
            default -> 0;
        };

        if (dir != 0) {
            pacman.nextDirection = dir;
            awaitingInput = false;
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE && (gameOver || won)) {
            restart();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // ---------------------------------------------------------------- loop

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !won && !awaitingInput && !paused) {
            update();
        }
        repaint();
    }

    private void update() {
        if (frightenedFrames > 0) {
            frightenedFrames--;
            if (frightenedFrames == 0) {
                for (Block ghost : ghosts) {
                    ghost.frightened = false;
                }
                ghostCombo = 0;
            }
        }

        movePacman();
        eatFood();
        if (foods.isEmpty() && powerFoods.isEmpty()) {
            won = true;
            return;
        }

        for (Block ghost : ghosts) {
            moveGhost(ghost);
        }
        checkGhostCollisions();
    }

    private void restart() {
        score = 0;
        lives = START_LIVES;
        frightenedFrames = 0;
        ghostCombo = 0;
        gameOver = false;
        won = false;
        paused = false;
        awaitingInput = true;
        loadMap();
    }

    // ---------------------------------------------------------------- pacman

    private void movePacman() {
        // Try to turn into the queued direction so corners can be pre-pressed.
        if (pacman.nextDirection != pacman.direction
                || (pacman.velocityX == 0 && pacman.velocityY == 0)) {
            int[] v = velocityFor(pacman.nextDirection, PACMAN_SPEED);
            if (canMove(pacman, v[0], v[1])) {
                pacman.direction = pacman.nextDirection;
                pacman.velocityX = v[0];
                pacman.velocityY = v[1];
            }
        }

        if (!canMove(pacman, pacman.velocityX, pacman.velocityY)) {
            return; // blocked against a wall, keep trying the queued direction
        }

        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;
        wrapAround(pacman);
        pacman.image = imageForDirection(pacman.direction);
    }

    private void eatFood() {
        Iterator<Block> it = foods.iterator();
        while (it.hasNext()) {
            Block food = it.next();
            if (intersects(pacman, food)) {
                it.remove();
                score += 10;
            }
        }

        Iterator<Block> pit = powerFoods.iterator();
        while (pit.hasNext()) {
            Block power = pit.next();
            if (intersects(pacman, power)) {
                pit.remove();
                score += 50;
                frightenGhosts();
            }
        }
    }

    // ---------------------------------------------------------------- ghosts

    private void frightenGhosts() {
        frightenedFrames = FRIGHTENED_FRAMES;
        ghostCombo = 0;
        for (Block ghost : ghosts) {
            ghost.frightened = true;
            reverseGhost(ghost);
        }
    }

    private void reverseGhost(Block ghost) {
        char opposite = oppositeOf(ghost.direction);
        int[] v = velocityFor(opposite, ghost.frightened ? FRIGHTENED_SPEED : GHOST_SPEED);
        ghost.direction = opposite;
        ghost.velocityX = v[0];
        ghost.velocityY = v[1];
    }

    private void moveGhost(Block ghost) {
        ghost.image = ghost.frightened ? scaredGhostImage : ghost.normalImage;

        // Decide a heading whenever the ghost is centred on a tile.
        if (ghost.x % tileSize == 0 && ghost.y % tileSize == 0) {
            chooseGhostDirection(ghost);
        }
        if (!canMove(ghost, ghost.velocityX, ghost.velocityY)) {
            chooseGhostDirection(ghost); // safety net
        }
        if (!canMove(ghost, ghost.velocityX, ghost.velocityY)) {
            return;
        }

        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        wrapAround(ghost);
    }

    private void chooseGhostDirection(Block ghost) {
        if (ghost.velocityX == 0 && ghost.velocityY == 0 && ghost.direction != LEFT) {
            ghost.direction = LEFT; // keep opposite-of logic consistent after a reset
        }
        int col = Math.floorDiv(ghost.x, tileSize);
        int row = Math.floorDiv(ghost.y, tileSize);

        char[] candidates = {UP, DOWN, LEFT, RIGHT};
        ArrayList<Character> options = new ArrayList<>();
        for (char dir : candidates) {
            if (dir != oppositeOf(ghost.direction) && isOpen(row, col, dir)) {
                options.add(dir);
            }
        }
        if (options.isEmpty()) { // dead end, turn back
            char back = oppositeOf(ghost.direction);
            if (isOpen(row, col, back)) {
                options.add(back);
            }
        }
        if (options.isEmpty()) {
            return;
        }

        char chosen = options.get(random.nextInt(options.size()));
        // Sometimes chase pacman instead of wandering (or flee when frightened).
        boolean chase = ghost.frightened
                ? random.nextDouble() < 0.6
                : random.nextDouble() < 0.35;
        if (chase) {
            int bestDistance = ghost.frightened ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            for (char option : options) {
                int[] tile = neighborTile(row, col, option);
                int cx = tile[1] * tileSize + tileSize / 2;
                int cy = tile[0] * tileSize + tileSize / 2;
                int dx = cx - (pacman.x + pacman.width / 2);
                int dy = cy - (pacman.y + pacman.height / 2);
                int distance = dx * dx + dy * dy;
                if (ghost.frightened ? distance > bestDistance : distance < bestDistance) {
                    bestDistance = distance;
                    chosen = option;
                }
            }
        }

        int speed = ghost.frightened ? FRIGHTENED_SPEED : GHOST_SPEED;
        int[] v = velocityFor(chosen, speed);
        ghost.direction = chosen;
        ghost.velocityX = v[0];
        ghost.velocityY = v[1];
    }

    private void checkGhostCollisions() {
        for (Block ghost : ghosts) {
            if (!intersects(pacman, ghost)) {
                continue;
            }
            if (ghost.frightened) {
                score += 200 << Math.min(ghostCombo, 3); // 200, 400, 800, 1600
                ghostCombo++;
                ghost.reset(); // eaten, sent back home (comes back normal)
            } else {
                lives--;
                if (lives <= 0) {
                    gameOver = true;
                } else {
                    resetPositions();
                    awaitingInput = true;
                }
                return;
            }
        }
    }

    private void resetPositions() {
        pacman.reset();
        frightenedFrames = 0;
        ghostCombo = 0;
        for (Block ghost : ghosts) {
            ghost.reset();
        }
    }

    // ---------------------------------------------------------------- helpers

    /** True when the block can move by (dx, dy) without hitting a wall. */
    private boolean canMove(Block block, int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return false;
        }
        int nx = block.x + dx;
        int ny = block.y + dy;
        for (Block wall : walls) {
            if (nx < wall.x + wall.width && nx + block.width > wall.x
                    && ny < wall.y + wall.height && ny + block.height > wall.y) {
                return false;
            }
        }
        return true;
    }

    private boolean intersects(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x
                && a.y < b.y + b.height && a.y + a.height > b.y;
    }

    /** Teleport between the left and right tunnel entrances. */
    private void wrapAround(Block block) {
        if (block.x + block.width / 2 <= 0) {
            block.x += boardWidth;
        } else if (block.x + block.width / 2 >= boardWidth) {
            block.x -= boardWidth;
        }
    }

    private boolean isOpen(int row, int col, char dir) {
        int[] tile = neighborTile(row, col, dir);
        row = tile[0];
        col = tile[1];
        if (row < 0 || row >= rowCount) {
            return false;
        }
        if (col < 0) {
            col = columnCount - 1; // tunnels connect both edges
        }
        if (col >= columnCount) {
            col = 0;
        }
        return tileMap[row].charAt(col) != 'X';
    }

    private int[] neighborTile(int row, int col, char dir) {
        return switch (dir) {
            case UP -> new int[]{row - 1, col};
            case DOWN -> new int[]{row + 1, col};
            case LEFT -> new int[]{row, col - 1};
            default -> new int[]{row, col + 1};
        };
    }

    private Image imageForDirection(char dir) {
        return switch (dir) {
            case UP -> pacmanUpImage;
            case DOWN -> pacmanDownImage;
            case LEFT -> pacmanLeftImage;
            default -> pacmanRightImage;
        };
    }

    private static char oppositeOf(char dir) {
        return switch (dir) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            default -> LEFT;
        };
    }

    private static int[] velocityFor(char dir, int speed) {
        return switch (dir) {
            case UP -> new int[]{0, -speed};
            case DOWN -> new int[]{0, speed};
            case LEFT -> new int[]{-speed, 0};
            default -> new int[]{speed, 0};
        };
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawHud(g);
        int oy = hudHeight; // maze sits below the HUD strip

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y + oy, wall.width, wall.height, null);
        }

        g.setColor(new Color(255, 228, 196));
        for (Block food : foods) {
            g.fillOval(food.x, food.y + oy, food.width, food.height);
        }

        for (Block power : powerFoods) {
            g.drawImage(power.image, power.x, power.y + oy, power.width, power.height, null);
        }

        for (Block ghost : ghosts) {
            g.drawImage(ghost.image, ghost.x, ghost.y + oy, ghost.width, ghost.height, null);
        }

        g.drawImage(pacman.image, pacman.x, pacman.y + oy, pacman.width, pacman.height, null);

        if (gameOver || won || awaitingInput || paused) {
            drawOverlay(g, oy);
        }
    }

    private void drawHud(Graphics g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 22);

        for (int i = 0; i < lives; i++) {
            g.drawImage(pacmanRightImage, boardWidth - 32 - i * 30, 4, 24, 24, null);
        }
    }

    private void drawOverlay(Graphics g, int oy) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, oy + boardHeight / 2 - 60, boardWidth, 120);

        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        g.setColor(gameOver ? Color.RED : paused ? Color.CYAN : Color.YELLOW);
        String title = won ? "YOU WIN!" : gameOver ? "GAME OVER" : paused ? "PAUSED" : "GET READY!";
        drawCentered(g, title, oy + boardHeight / 2 - 8);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        String sub;
        if (gameOver || won) {
            sub = "Score: " + score + "   -   Press SPACE to play again";
        } else if (paused) {
            sub = "Press P to resume";
        } else {
            sub = "Press an arrow key or WASD to move   -   P to pause";
        }
        drawCentered(g, sub, oy + boardHeight / 2 + 28);
    }

    private void drawCentered(Graphics g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (boardWidth - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }
}
