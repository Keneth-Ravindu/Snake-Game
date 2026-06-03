import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {

    private class Tile {
        int x, y;

        Tile(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private class Particle {
        int x, y, life;

        Particle(int x, int y) {
            this.x = x;
            this.y = y;
            this.life = 25;
        }
    }

    private enum FoodType {
        NORMAL, GOLDEN, PURPLE, POISON
    }

    private final int boardWidth;
    private final int boardHeight;
    private final int hudHeight = 95;
    private final int tileSize = 25;

    private Tile snakeHead;
    private ArrayList<Tile> snakeBody = new ArrayList<>();

    private Tile food;
    private FoodType foodType;

    private ArrayList<Tile> obstacles = new ArrayList<>();
    private ArrayList<Particle> particles = new ArrayList<>();

    private Random random = new Random();
    private Timer gameLoop;

    private int velocityX = 1;
    private int velocityY = 0;

    private int score = 0;
    private int level = 1;
    private int speed = 100;

    private boolean gameOver = false;
    private int animationTick = 0;

    private int poisonTimer = 0;
    private final int poisonLifetime = 35;

    public SnakeGame(int boardWidth, int boardHeight) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;

        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(new Color(8, 12, 10));
        setFocusable(true);
        addKeyListener(this);

        snakeHead = new Tile(5, 5);
        food = new Tile(10, 10);

        placeFood();
        placeObstacles(8);

        gameLoop = new Timer(speed, this);
        gameLoop.start();
    }

    private int cols() {
        return boardWidth / tileSize;
    }

    private int rows() {
        return (boardHeight - hudHeight) / tileSize;
    }

    private int screenY(int tileY) {
        return tileY * tileSize + hudHeight;
    }

    private boolean isWallTile(Tile tile) {
        return tile.x <= 0 || tile.x >= cols() - 1 ||
                tile.y <= 0 || tile.y >= rows() - 1;
    }

    private boolean isTileOccupied(Tile tile) {
        if (isWallTile(tile))
            return true;
        if (collision(tile, snakeHead))
            return true;
        if (food != null && collision(tile, food))
            return true;

        for (Tile part : snakeBody) {
            if (collision(tile, part))
                return true;
        }

        for (Tile obstacle : obstacles) {
            if (collision(tile, obstacle))
                return true;
        }

        return false;
    }

    private void placeFood() {
        Tile newFood;

        do {
            newFood = new Tile(
                    random.nextInt(cols() - 2) + 1,
                    random.nextInt(rows() - 2) + 1);
        } while (isTileOccupied(newFood));

        food.x = newFood.x;
        food.y = newFood.y;

        int chance = random.nextInt(100);

        if (chance < 65) {
            foodType = FoodType.NORMAL;
            poisonTimer = 0;
        } else if (chance < 80) {
            foodType = FoodType.GOLDEN;
            poisonTimer = 0;
        } else if (chance < 92) {
            foodType = FoodType.PURPLE;
            poisonTimer = 0;
        } else {
            foodType = FoodType.POISON;
            poisonTimer = poisonLifetime;
        }
    }

    private void placeObstacles(int count) {
        obstacles.clear();

        for (int i = 0; i < count; i++) {
            addObstacle();
        }
    }

    private void addObstacle() {
        Tile obstacle;

        do {
            obstacle = new Tile(
                    random.nextInt(cols() - 2) + 1,
                    random.nextInt(rows() - 2) + 1);
        } while (isTileOccupied(obstacle));

        obstacles.add(obstacle);
    }

    private boolean collision(Tile a, Tile b) {
        return a.x == b.x && a.y == b.y;
    }

    private void move() {
        if (gameOver)
            return;

        animationTick++;

        for (int i = snakeBody.size() - 1; i >= 0; i--) {
            Tile part = snakeBody.get(i);

            if (i == 0) {
                part.x = snakeHead.x;
                part.y = snakeHead.y;
            } else {
                Tile previous = snakeBody.get(i - 1);
                part.x = previous.x;
                part.y = previous.y;
            }
        }

        snakeHead.x += velocityX;
        snakeHead.y += velocityY;

        checkCollisions();

        if (!gameOver && collision(snakeHead, food)) {
            handleFoodEffect();
            createFoodParticles(food.x, food.y);

            if (!gameOver) {
                placeFood();
            }
        }

        updateParticles();
        updatePoisonTimer();
    }

    private void handleFoodEffect() {
        switch (foodType) {
            case NORMAL:
                snakeBody.add(new Tile(food.x, food.y));
                score += 1;
                break;

            case GOLDEN:
                snakeBody.add(new Tile(food.x, food.y));
                snakeBody.add(new Tile(food.x, food.y));
                score += 5;
                break;

            case PURPLE:
                score += 2;
                removeObstacles(2);
                break;

            case POISON:
                gameOver = true;
                gameLoop.stop();
                return;
        }

        updateLevel();
    }

    private void removeObstacles(int amount) {
        for (int i = 0; i < amount && !obstacles.isEmpty(); i++) {
            obstacles.remove(obstacles.size() - 1);
        }
    }

    private void updateLevel() {
        int newLevel = (score / 5) + 1;

        if (newLevel > level) {
            level = newLevel;

            speed = Math.max(45, speed - 8);
            gameLoop.setDelay(speed);

            addObstacle();
            addObstacle();
        }
    }

    private void updatePoisonTimer() {
        if (foodType == FoodType.POISON) {
            poisonTimer--;

            if (poisonTimer <= 0) {
                placeFood();
            }
        }
    }

    private void createFoodParticles(int tileX, int tileY) {
        for (int i = 0; i < 14; i++) {
            particles.add(new Particle(
                    tileX * tileSize + random.nextInt(tileSize),
                    screenY(tileY) + random.nextInt(tileSize)));
        }
    }

    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.y -= 1;
            p.life--;

            if (p.life <= 0) {
                particles.remove(i);
            }
        }
    }

    private void checkCollisions() {
        if (snakeHead.x <= 0 ||
                snakeHead.x >= cols() - 1 ||
                snakeHead.y <= 0 ||
                snakeHead.y >= rows() - 1) {
            gameOver = true;
        }

        for (Tile part : snakeBody) {
            if (collision(snakeHead, part)) {
                gameOver = true;
                break;
            }
        }

        for (Tile obstacle : obstacles) {
            if (collision(snakeHead, obstacle)) {
                gameOver = true;
                break;
            }
        }

        if (gameOver) {
            gameLoop.stop();
        }
    }

    private void restartGame() {
        snakeHead = new Tile(5, 5);
        snakeBody.clear();
        obstacles.clear();
        particles.clear();

        velocityX = 1;
        velocityY = 0;

        score = 0;
        level = 1;
        speed = 100;
        gameOver = false;
        poisonTimer = 0;

        placeFood();
        placeObstacles(8);

        gameLoop.setDelay(speed);
        gameLoop.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver && e.getKeyCode() == KeyEvent.VK_SPACE) {
            restartGame();
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_UP && velocityY != 1) {
            velocityX = 0;
            velocityY = -1;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && velocityY != -1) {
            velocityX = 0;
            velocityY = 1;
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT && velocityX != 1) {
            velocityX = -1;
            velocityY = 0;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && velocityX != -1) {
            velocityX = 1;
            velocityY = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw((Graphics2D) g);
    }

    private void draw(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g);
        drawHud(g);
        drawGrid(g);
        drawBorderWalls(g);
        drawFood(g);
        drawObstacles(g);
        drawParticles(g);
        drawSnake(g);
        drawVignette(g);

        if (gameOver) {
            drawGameOver(g);
        }
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint bg = new GradientPaint(
                0, 0, new Color(18, 32, 22),
                boardWidth, boardHeight, new Color(3, 7, 5));

        g.setPaint(bg);
        g.fillRect(0, 0, boardWidth, boardHeight);

        g.setColor(new Color(255, 255, 255, 10));
        for (int i = 0; i < 45; i++) {
            int x = (i * 79 + animationTick * 2) % boardWidth;
            int y = hudHeight + ((i * 47) % (boardHeight - hudHeight));
            g.fillOval(x, y, 2, 2);
        }
    }

    private void drawHud(Graphics2D g) {
        GradientPaint hud = new GradientPaint(
                0, 0, new Color(10, 18, 14),
                boardWidth, hudHeight, new Color(3, 6, 5));

        g.setPaint(hud);
        g.fillRect(0, 0, boardWidth, hudHeight);

        g.setColor(new Color(0, 255, 130));
        g.setFont(new Font("Arial", Font.BOLD, 25));
        g.drawString("Venom Grid", 25, 32);

        drawHudCard(g, 230, 10, 145, 32, "Score: " + score, Color.WHITE);
        drawHudCard(g, 405, 10, 125, 32, "Level: " + level, new Color(255, 215, 80));

        g.setColor(new Color(190, 190, 190));
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Controls: Arrow Keys = Move   |   SPACE = Restart after Game Over", 45, 68);

        g.setFont(new Font("Arial", Font.BOLD, 13));

        g.setColor(new Color(255, 75, 75));
        g.drawString("Red +1", 55, 88);

        g.setColor(new Color(255, 210, 60));
        g.drawString("Gold +5", 130, 88);

        g.setColor(new Color(180, 90, 255));
        g.drawString("Purple clears obstacles", 220, 88);

        g.setColor(new Color(80, 255, 80));
        g.drawString("Green poison", 410, 88);
    }

    private void drawHudCard(Graphics2D g, int x, int y, int w, int h, String text, Color textColor) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(x + 3, y + 4, w, h, 18, 18);

        GradientPaint card = new GradientPaint(
                x, y, new Color(35, 55, 42),
                x, y + h, new Color(12, 22, 16));

        g.setPaint(card);
        g.fillRoundRect(x, y, w, h, 18, 18);

        g.setColor(new Color(90, 255, 160, 80));
        g.drawRoundRect(x, y, w, h, 18, 18);

        g.setColor(textColor);
        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.drawString(text, x + 25, y + 22);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 14));

        for (int x = 0; x < boardWidth; x += tileSize) {
            g.drawLine(x, hudHeight, x, boardHeight);
        }

        for (int y = hudHeight; y < boardHeight; y += tileSize) {
            g.drawLine(0, y, boardWidth, y);
        }
    }

    private void drawBorderWalls(Graphics2D g) {
        for (int x = 0; x < cols(); x++) {
            drawWallBlock(g, x, 0);
            drawWallBlock(g, x, rows() - 1);
        }

        for (int y = 0; y < rows(); y++) {
            drawWallBlock(g, 0, y);
            drawWallBlock(g, cols() - 1, y);
        }
    }

    private void drawWallBlock(Graphics2D g, int x, int y) {
        int px = x * tileSize;
        int py = screenY(y);

        g.setColor(new Color(0, 0, 0, 130));
        g.fillRoundRect(px + 4, py + 5, tileSize - 4, tileSize - 4, 8, 8);

        GradientPaint wall = new GradientPaint(
                px, py, new Color(165, 120, 70),
                px, py + tileSize, new Color(70, 45, 25));

        g.setPaint(wall);
        g.fillRoundRect(px + 2, py + 2, tileSize - 4, tileSize - 4, 8, 8);

        g.setColor(new Color(220, 170, 100, 150));
        g.drawLine(px + 5, py + 5, px + tileSize - 6, py + 5);

        g.setColor(new Color(40, 25, 15, 180));
        g.drawLine(px + 5, py + tileSize - 5, px + tileSize - 6, py + tileSize - 5);
    }

    private void drawFood(Graphics2D g) {
        int pulse = (int) (Math.sin(animationTick * 0.3) * 4);
        int size = tileSize - 6 + pulse;

        int x = food.x * tileSize + (tileSize - size) / 2;
        int y = screenY(food.y) + (tileSize - size) / 2;

        Color glow;
        Color main;

        switch (foodType) {
            case GOLDEN:
                glow = new Color(255, 215, 0, 100);
                main = new Color(255, 200, 40);
                break;
            case PURPLE:
                glow = new Color(185, 90, 255, 100);
                main = new Color(170, 70, 255);
                break;
            case POISON:
                glow = new Color(80, 255, 80, 100);
                main = new Color(50, 210, 50);
                break;
            default:
                glow = new Color(255, 60, 60, 100);
                main = new Color(255, 45, 45);
                break;
        }

        g.setColor(glow);
        g.fillOval(x - 10, y - 10, size + 20, size + 20);

        GradientPaint foodGradient = new GradientPaint(
                x, y, Color.WHITE,
                x + size, y + size, main);

        g.setPaint(foodGradient);
        g.fillOval(x, y, size, size);

        g.setColor(new Color(255, 255, 255, 180));
        g.fillOval(x + 5, y + 4, 6, 6);

        if (foodType == FoodType.POISON) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 13));
            g.drawString("X", x + 7, y + 16);
        }
    }

    private void drawObstacles(Graphics2D g) {
        for (Tile obstacle : obstacles) {
            int x = obstacle.x * tileSize;
            int y = screenY(obstacle.y);

            g.setColor(new Color(0, 0, 0, 130));
            g.fillRoundRect(x + 5, y + 6, tileSize - 5, tileSize - 5, 10, 10);

            GradientPaint rock = new GradientPaint(
                    x, y, new Color(130, 130, 145),
                    x, y + tileSize, new Color(45, 45, 55));

            g.setPaint(rock);
            g.fillRoundRect(x + 3, y + 3, tileSize - 6, tileSize - 6, 10, 10);

            g.setColor(new Color(220, 220, 230, 90));
            g.drawLine(x + 7, y + 7, x + 17, y + 6);

            g.setColor(new Color(20, 20, 25, 130));
            g.drawLine(x + 8, y + 18, x + 18, y + 20);
        }
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            int alpha = Math.min(255, p.life * 10);
            g.setColor(new Color(255, 120, 80, alpha));
            g.fillOval(p.x, p.y, 6, 6);

            g.setColor(new Color(255, 255, 255, alpha / 2));
            g.fillOval(p.x + 1, p.y + 1, 2, 2);
        }
    }

    private void drawSnake(Graphics2D g) {
        for (Tile part : snakeBody) {
            drawSnakeBlock(g, part.x, part.y, false);
        }

        drawSnakeBlock(g, snakeHead.x, snakeHead.y, true);
    }

    private void drawSnakeBlock(Graphics2D g, int tileX, int tileY, boolean isHead) {
        int x = tileX * tileSize;
        int y = screenY(tileY);

        g.setColor(new Color(0, 0, 0, 130));
        g.fillRoundRect(x + 4, y + 5, tileSize - 3, tileSize - 3, 14, 14);

        Color top = isHead ? new Color(80, 255, 160) : new Color(20, 210, 100);
        Color bottom = isHead ? new Color(0, 150, 75) : new Color(0, 115, 60);

        GradientPaint snakeGradient = new GradientPaint(
                x, y, top,
                x, y + tileSize, bottom);

        g.setPaint(snakeGradient);
        g.fillRoundRect(x + 1, y + 1, tileSize - 3, tileSize - 3, 14, 14);

        g.setColor(new Color(180, 255, 210, 120));
        g.drawLine(x + 6, y + 5, x + tileSize - 7, y + 5);

        g.setColor(new Color(0, 70, 35, 160));
        g.drawLine(x + 5, y + tileSize - 5, x + tileSize - 6, y + tileSize - 5);

        if (isHead) {
            drawSnakeEyes(g, x, y);
        }
    }

    private void drawSnakeEyes(Graphics2D g, int headX, int headY) {
        g.setColor(Color.WHITE);

        if (velocityX == 1) {
            g.fillOval(headX + 15, headY + 6, 6, 6);
            g.fillOval(headX + 15, headY + 15, 6, 6);
            g.setColor(Color.BLACK);
            g.fillOval(headX + 17, headY + 8, 2, 2);
            g.fillOval(headX + 17, headY + 17, 2, 2);
        } else if (velocityX == -1) {
            g.fillOval(headX + 4, headY + 6, 6, 6);
            g.fillOval(headX + 4, headY + 15, 6, 6);
            g.setColor(Color.BLACK);
            g.fillOval(headX + 6, headY + 8, 2, 2);
            g.fillOval(headX + 6, headY + 17, 2, 2);
        } else if (velocityY == -1) {
            g.fillOval(headX + 6, headY + 4, 6, 6);
            g.fillOval(headX + 15, headY + 4, 6, 6);
            g.setColor(Color.BLACK);
            g.fillOval(headX + 8, headY + 6, 2, 2);
            g.fillOval(headX + 17, headY + 6, 2, 2);
        } else {
            g.fillOval(headX + 6, headY + 15, 6, 6);
            g.fillOval(headX + 15, headY + 15, 6, 6);
            g.setColor(Color.BLACK);
            g.fillOval(headX + 8, headY + 17, 2, 2);
            g.fillOval(headX + 17, headY + 17, 2, 2);
        }
    }

    private void drawVignette(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRect(0, boardHeight - 20, boardWidth, 20);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, boardWidth, boardHeight);

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(115, 250, 370, 190, 30, 30);

        g.setColor(new Color(255, 70, 70));
        g.setFont(new Font("Arial", Font.BOLD, 52));
        g.drawString("GAME OVER", 145, 315);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 25));
        g.drawString("Final Score: " + score, 215, 370);

        g.setFont(new Font("Arial", Font.PLAIN, 21));
        g.drawString("Press SPACE to Restart", 195, 415);
    }
}