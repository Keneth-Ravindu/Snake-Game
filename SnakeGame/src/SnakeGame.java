import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiBaseUrl = "http://127.0.0.1:8000";

    private String aiAdvice = "AI feedback not loaded yet.";
    private String rivalComment = "";
    private boolean scoreSent = false;

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
    private final int hudHeight = 120;
    private final int tileSize = 25;

    private String playerName = "Player";
    private boolean waitingForName = true;
    private String nameInput = "";
    private int transitionAlpha = 255;
    private boolean transitioningToGame = false;

    private Tile snakeHead;
    private ArrayList<Tile> snakeBody = new ArrayList<>();

    private Tile rivalHead;
    private ArrayList<Tile> rivalBody = new ArrayList<>();
    private int rivalVelocityX = -1;
    private int rivalVelocityY = 0;

    private Tile food;
    private FoodType foodType;

    private ArrayList<Tile> obstacles = new ArrayList<>();
    private ArrayList<Particle> particles = new ArrayList<>();

    private Random random = new Random();
    private Timer gameLoop;

    private boolean paused = false;

    private int velocityX = 1;
    private int velocityY = 0;

    private int score = 0;
    private int rivalScore = 0;
    private int level = 1;
    private int speed = 100;

    private boolean gameOver = false;
    private int animationTick = 0;

    private int poisonTimer = 0;
    private final int poisonLifetime = 35;

    private String lossReason = "";
    private String bestMove = "N/A";
    private String riskLevel = "Low";

    public SnakeGame(int boardWidth, int boardHeight) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;

        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(new Color(8, 12, 10));
        setFocusable(true);
        addKeyListener(this);

        snakeHead = new Tile(5, 5);
        rivalHead = new Tile(cols() - 6, rows() - 6);
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

    private boolean collision(Tile a, Tile b) {
        return a.x == b.x && a.y == b.y;
    }

    private boolean isTileOccupied(Tile tile) {
        if (isWallTile(tile))
            return true;
        if (collision(tile, snakeHead))
            return true;
        if (collision(tile, rivalHead))
            return true;
        if (food != null && collision(tile, food))
            return true;

        for (Tile part : snakeBody) {
            if (collision(tile, part))
                return true;
        }

        for (Tile part : rivalBody) {
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

    private void move() {
        if (waitingForName)
            return;

        if (transitioningToGame) {
            transitionAlpha -= 15;

            if (transitionAlpha <= 0) {
                transitionAlpha = 0;
                transitioningToGame = false;
            }
        }

        if (gameOver || paused)
            return;

        animationTick++;

        movePlayerBody();
        moveRivalBody();

        snakeHead.x += velocityX;
        snakeHead.y += velocityY;

        updateRivalDirection();
        rivalHead.x += rivalVelocityX;
        rivalHead.y += rivalVelocityY;

        checkCollisions();

        if (!gameOver && collision(snakeHead, food)) {
            handlePlayerFoodEffect();
            createFoodParticles(food.x, food.y);

            if (!gameOver) {
                placeFood();
            }
        }

        if (!gameOver && collision(rivalHead, food)) {
            if (foodType != FoodType.POISON) {
                handleRivalFoodEffect();
                createFoodParticles(food.x, food.y);
                placeFood();
            } else {
                updateRivalDirection();
            }
        }

        updateParticles();
        updatePoisonTimer();
    }

    private void movePlayerBody() {
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
    }

    private void moveRivalBody() {
        for (int i = rivalBody.size() - 1; i >= 0; i--) {
            Tile part = rivalBody.get(i);

            if (i == 0) {
                part.x = rivalHead.x;
                part.y = rivalHead.y;
            } else {
                Tile previous = rivalBody.get(i - 1);
                part.x = previous.x;
                part.y = previous.y;
            }
        }
    }

    private void updateRivalDirection() {
        int[][] moves = {
                { 1, 0 },
                { -1, 0 },
                { 0, 1 },
                { 0, -1 }
        };

        int bestDx = 0;
        int bestDy = 0;
        int bestScore = Integer.MAX_VALUE;
        boolean foundMove = false;

        for (int[] move : moves) {
            int dx = move[0];
            int dy = move[1];

            Tile nextTile = new Tile(rivalHead.x + dx, rivalHead.y + dy);

            if (!isSafeForRival(nextTile)) {
                continue;
            }

            int moveScore;

            if (foodType == FoodType.POISON) {
                moveScore = -(Math.abs(nextTile.x - food.x) +
                        Math.abs(nextTile.y - food.y));
            } else {
                moveScore = Math.abs(nextTile.x - food.x) +
                        Math.abs(nextTile.y - food.y);
            }

            if (moveScore < bestScore) {
                bestScore = moveScore;
                bestDx = dx;
                bestDy = dy;
                foundMove = true;
            }
        }

        if (foundMove) {
            rivalVelocityX = bestDx;
            rivalVelocityY = bestDy;
        } else {
            resetRival();
        }
    }

    private boolean isSafeForRival(Tile tile) {
        if (isWallTile(tile))
            return false;
        if (collision(tile, snakeHead))
            return false;

        for (Tile obstacle : obstacles) {
            if (collision(tile, obstacle))
                return false;
        }

        for (Tile part : rivalBody) {
            if (collision(tile, part))
                return false;
        }

        return true;
    }

    private void handlePlayerFoodEffect() {
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
                endGame("You lost because you ate poison food.");
                return;
        }

        updateLevel();
    }

    private void handleRivalFoodEffect() {
        if (foodType == FoodType.POISON) {
            rivalScore = Math.max(0, rivalScore - 1);
            return;
        }

        rivalBody.add(new Tile(food.x, food.y));
        rivalScore++;

        if (foodType == FoodType.GOLDEN) {
            rivalBody.add(new Tile(food.x, food.y));
            rivalScore += 4;
        }
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

    private void checkCollisions() {
        if (isWallTile(snakeHead)) {
            endGame("You lost because you turned into a wall.");
            return;
        }

        for (Tile part : snakeBody) {
            if (collision(snakeHead, part)) {
                endGame("You lost because you crashed into your own body.");
                return;
            }
        }

        for (Tile obstacle : obstacles) {
            if (collision(snakeHead, obstacle)) {
                endGame("You lost because you hit an obstacle.");
                return;
            }
        }

        if (collision(snakeHead, rivalHead)) {
            endGame("You lost because you crashed into the rival snake.");
            return;
        }

        for (Tile part : rivalBody) {
            if (collision(snakeHead, part)) {
                endGame("You lost because the rival snake blocked your path.");
                return;
            }
        }

        for (Tile obstacle : obstacles) {
            if (collision(rivalHead, obstacle)) {
                resetRival();
                break;
            }
        }
    }

    private void resetRival() {
        rivalHead = new Tile(cols() - 6, rows() - 6);
        rivalBody.clear();
        rivalVelocityX = -1;
        rivalVelocityY = 0;
    }

    private void endGame(String reason) {
        gameOver = true;
        paused = false;
        lossReason = reason;
        bestMove = calculateBestMove();
        riskLevel = calculateRiskLevel();

        gameLoop.stop();

        if (!scoreSent) {
            scoreSent = true;
            sendScoreToBackend();
            getAiAnalysisFromBackend();
        }
    }

    private void sendScoreToBackend() {
        String json = String.format(
                """
                        {
                            "player_name": "%s",
                            "score": %d,
                            "level": %d,
                            "rival_score": %d,
                            "loss_reason": "%s",
                            "best_move": "%s",
                            "risk_level": "%s"
                        }
                        """,
                escapeJson(playerName),
                score,
                level,
                rivalScore,
                escapeJson(lossReason),
                escapeJson(bestMove),
                escapeJson(riskLevel));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/scores"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .exceptionally(error -> {
                    System.out.println("Could not send score: " + error.getMessage());
                    return null;
                });
    }

    private void getAiAnalysisFromBackend() {
        String json = String.format(
                """
                        {
                            "score": %d,
                            "level": %d,
                            "rival_score": %d,
                            "loss_reason": "%s",
                            "best_move": "%s",
                            "risk_level": "%s"
                        }
                        """,
                score,
                level,
                rivalScore,
                escapeJson(lossReason),
                escapeJson(bestMove),
                escapeJson(riskLevel));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/analysis"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    aiAdvice = extractJsonValue(response, "advice");
                    rivalComment = extractJsonValue(response, "rival_comment");
                    repaint();
                })
                .exceptionally(error -> {
                    aiAdvice = "Backend not connected. Run FastAPI first.";
                    rivalComment = "";
                    repaint();
                    return null;
                });
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);

        if (start == -1)
            return "";

        start += search.length();
        int end = json.indexOf("\"", start);

        if (end == -1)
            return "";

        return json.substring(start, end);
    }

    private String calculateBestMove() {
        String[] moveNames = { "RIGHT", "LEFT", "DOWN", "UP" };
        int[][] moves = {
                { 1, 0 },
                { -1, 0 },
                { 0, 1 },
                { 0, -1 }
        };

        String best = "N/A";
        int bestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < moves.length; i++) {
            Tile next = new Tile(
                    snakeHead.x + moves[i][0],
                    snakeHead.y + moves[i][1]);

            if (!isSafeForPlayer(next)) {
                continue;
            }

            int distance = Math.abs(next.x - food.x) + Math.abs(next.y - food.y);

            if (distance < bestDistance) {
                bestDistance = distance;
                best = moveNames[i];
            }
        }

        return best;
    }

    private boolean isSafeForPlayer(Tile tile) {
        if (isWallTile(tile))
            return false;

        for (Tile part : snakeBody) {
            if (collision(tile, part))
                return false;
        }

        for (Tile part : rivalBody) {
            if (collision(tile, part))
                return false;
        }

        if (collision(tile, rivalHead))
            return false;

        for (Tile obstacle : obstacles) {
            if (collision(tile, obstacle))
                return false;
        }

        return true;
    }

    private String calculateRiskLevel() {
        int dangerCount = 0;

        int[][] moves = {
                { 1, 0 },
                { -1, 0 },
                { 0, 1 },
                { 0, -1 }
        };

        for (int[] move : moves) {
            Tile next = new Tile(snakeHead.x + move[0], snakeHead.y + move[1]);

            if (!isSafeForPlayer(next)) {
                dangerCount++;
            }
        }

        if (dangerCount >= 3)
            return "High";
        if (dangerCount == 2)
            return "Medium";
        return "Low";
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

    private void restartGame() {
        snakeHead = new Tile(5, 5);
        snakeBody.clear();

        rivalHead = new Tile(cols() - 6, rows() - 6);
        rivalBody.clear();
        rivalVelocityX = -1;
        rivalVelocityY = 0;

        obstacles.clear();
        particles.clear();

        velocityX = 1;
        velocityY = 0;

        score = 0;
        rivalScore = 0;
        level = 1;
        speed = 100;
        gameOver = false;
        paused = false;
        poisonTimer = 0;

        lossReason = "";
        bestMove = "N/A";
        riskLevel = "Low";

        scoreSent = false;
        aiAdvice = "AI feedback not loaded yet.";
        rivalComment = "";

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
        if (waitingForName) {
            handleNameInput(e);
            return;
        }

        if (gameOver && e.getKeyCode() == KeyEvent.VK_SPACE) {
            restartGame();
            return;
        }

        if (!gameOver && e.getKeyCode() == KeyEvent.VK_P) {
            paused = !paused;

            if (paused) {
                gameLoop.stop();
            } else {
                gameLoop.start();
            }

            repaint();
            return;
        }

        if (paused || gameOver)
            return;

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

    private void handleNameInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (!nameInput.trim().isEmpty()) {
                playerName = nameInput.trim();
            } else {
                playerName = "Player";
            }

            waitingForName = false;
            transitioningToGame = true;
            transitionAlpha = 255;
            repaint();
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && nameInput.length() > 0) {
            nameInput = nameInput.substring(0, nameInput.length() - 1);
            repaint();
            return;
        }

        char keyChar = e.getKeyChar();

        if (Character.isLetterOrDigit(keyChar) || keyChar == ' ' || keyChar == '_' || keyChar == '-') {
            if (nameInput.length() < 14) {
                nameInput += keyChar;
                repaint();
            }
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

        if (waitingForName) {
            drawNameScreen(g);
            return;
        }

        drawBackground(g);
        drawHud(g);
        drawGrid(g);
        drawBorderWalls(g);
        drawFood(g);
        drawObstacles(g);
        drawParticles(g);
        drawSnake(g);
        drawRivalSnake(g);
        drawVignette(g);

        if (transitioningToGame) {
            g.setColor(new Color(0, 0, 0, transitionAlpha));
            g.fillRect(0, 0, boardWidth, boardHeight);
        }

        if (paused && !gameOver) {
            drawPauseScreen(g);
        }

        if (gameOver) {
            drawGameOver(g);
        }
    }

    private void drawNameScreen(Graphics2D g) {
        GradientPaint bg = new GradientPaint(
                0, 0, new Color(5, 15, 10),
                boardWidth, boardHeight, new Color(0, 0, 0));

        g.setPaint(bg);
        g.fillRect(0, 0, boardWidth, boardHeight);

        for (int i = 0; i < 55; i++) {
            int x = (i * 83 + animationTick * 2) % boardWidth;
            int y = (i * 51) % boardHeight;

            g.setColor(new Color(0, 255, 130, 25));
            g.fillOval(x, y, 3, 3);
        }

        g.setColor(new Color(0, 0, 0, 130));
        g.fillRoundRect(75, 145, 450, 380, 35, 35);

        g.setColor(new Color(0, 255, 130));
        g.setFont(new Font("Arial", Font.BOLD, 46));
        g.drawString("NEON SERPENT", 120, 220);

        g.setColor(new Color(180, 180, 180));
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Enter your player name to begin", 165, 260);

        int boxX = 130;
        int boxY = 310;
        int boxW = 340;
        int boxH = 58;

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(boxX + 5, boxY + 6, boxW, boxH, 22, 22);

        g.setColor(new Color(15, 30, 22, 245));
        g.fillRoundRect(boxX, boxY, boxW, boxH, 22, 22);

        g.setColor(new Color(0, 230, 110));
        g.drawRoundRect(boxX, boxY, boxW, boxH, 22, 22);

        g.setFont(new Font("Arial", Font.BOLD, 22));

        if (nameInput.isEmpty()) {
            g.setColor(new Color(130, 130, 130));
            g.drawString("Player Name", boxX + 25, boxY + 37);
        } else {
            g.setColor(Color.WHITE);
            g.drawString(nameInput, boxX + 25, boxY + 37);
        }

        if ((animationTick / 8) % 2 == 0) {
            int cursorX = boxX + 25 + g.getFontMetrics().stringWidth(nameInput.isEmpty() ? "" : nameInput);
            g.setColor(new Color(0, 255, 130));
            g.drawLine(cursorX + 3, boxY + 17, cursorX + 3, boxY + 42);
        }

        g.setColor(new Color(255, 215, 80));
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Press ENTER to Start", 200, 425);

        g.setColor(new Color(120, 255, 170));
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.drawString("Arrow Keys = Move   |   P = Pause   |   SPACE = Restart", 112, 465);
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
            int y = hudHeight + ((i * 47) % Math.max(1, boardHeight - hudHeight));
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
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("NEON SERPENT", 25, 32);

        drawHudCard(g, 220, 10, 120, 32, "You: " + score, Color.WHITE);
        drawHudCard(g, 350, 10, 130, 32, "AI: " + rivalScore, new Color(90, 190, 255));
        drawHudCard(g, 495, 10, 90, 32, "Lv: " + level, new Color(255, 215, 80));

        g.setColor(new Color(190, 190, 190));
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Controls: Arrow Keys = Move | P = Pause/Resume | SPACE = Restart", 75, 64);

        g.setColor(new Color(180, 180, 180));
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Player: " + playerName, 25, 88);

        g.setFont(new Font("Arial", Font.BOLD, 13));

        g.setColor(new Color(255, 75, 75));
        g.drawString("Red +1", 150, 88);

        g.setColor(new Color(255, 210, 60));
        g.drawString("Gold +5", 225, 88);

        g.setColor(new Color(180, 90, 255));
        g.drawString("Purple clears obstacles", 315, 88);

        g.setColor(new Color(80, 255, 80));
        g.drawString("Green poison", 500, 88);

        if (paused) {
            g.setColor(new Color(255, 215, 80));
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("PAUSED", 520, 112);
        }
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
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.drawString(text, x + 18, y + 21);
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
            drawSnakeBlock(g, part.x, part.y, false, false);
        }

        drawSnakeBlock(g, snakeHead.x, snakeHead.y, true, false);
    }

    private void drawRivalSnake(Graphics2D g) {
        for (Tile part : rivalBody) {
            drawSnakeBlock(g, part.x, part.y, false, true);
        }

        drawSnakeBlock(g, rivalHead.x, rivalHead.y, true, true);
    }

    private void drawSnakeBlock(Graphics2D g, int tileX, int tileY, boolean isHead, boolean isRival) {
        int x = tileX * tileSize;
        int y = screenY(tileY);

        g.setColor(new Color(0, 0, 0, 130));
        g.fillRoundRect(x + 4, y + 5, tileSize - 3, tileSize - 3, 14, 14);

        Color top;
        Color bottom;

        if (isRival) {
            top = isHead ? new Color(95, 210, 255) : new Color(40, 160, 230);
            bottom = isHead ? new Color(20, 80, 180) : new Color(15, 65, 145);
        } else {
            top = isHead ? new Color(80, 255, 160) : new Color(20, 210, 100);
            bottom = isHead ? new Color(0, 150, 75) : new Color(0, 115, 60);
        }

        GradientPaint snakeGradient = new GradientPaint(
                x, y, top,
                x, y + tileSize, bottom);

        g.setPaint(snakeGradient);
        g.fillRoundRect(x + 1, y + 1, tileSize - 3, tileSize - 3, 14, 14);

        g.setColor(new Color(220, 255, 255, 120));
        g.drawLine(x + 6, y + 5, x + tileSize - 7, y + 5);

        g.setColor(new Color(0, 40, 70, 150));
        g.drawLine(x + 5, y + tileSize - 5, x + tileSize - 6, y + tileSize - 5);

        if (isHead) {
            drawEyes(g, x, y, isRival);
        }
    }

    private void drawEyes(Graphics2D g, int headX, int headY, boolean isRival) {
        g.setColor(Color.WHITE);

        int dx = isRival ? rivalVelocityX : velocityX;
        int dy = isRival ? rivalVelocityY : velocityY;

        if (dx == 1) {
            g.fillOval(headX + 15, headY + 6, 6, 6);
            g.fillOval(headX + 15, headY + 15, 6, 6);
            g.setColor(Color.BLACK);
            g.fillOval(headX + 17, headY + 8, 2, 2);
            g.fillOval(headX + 17, headY + 17, 2, 2);
        } else if (dx == -1) {
            g.fillOval(headX + 4, headY + 6, 6, 6);
            g.fillOval(headX + 4, headY + 15, 6, 6);
            g.setColor(Color.BLACK);
            g.fillOval(headX + 6, headY + 8, 2, 2);
            g.fillOval(headX + 6, headY + 17, 2, 2);
        } else if (dy == -1) {
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

    private void drawPauseScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 165));
        g.fillRect(0, 0, boardWidth, boardHeight);

        int panelX = 135;
        int panelY = 285;
        int panelW = 330;
        int panelH = 130;

        g.setColor(new Color(8, 12, 10, 240));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 30, 30);

        g.setColor(new Color(255, 215, 80));
        g.setFont(new Font("Arial", Font.BOLD, 42));
        g.drawString("PAUSED", panelX + 77, panelY + 55);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Press P to Resume", panelX + 92, panelY + 92);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, boardWidth, boardHeight);

        int panelY = boardHeight - 210;
        int panelH = 210;

        g.setColor(new Color(8, 12, 10, 245));
        g.fillRoundRect(0, panelY, boardWidth, panelH + 30, 35, 35);

        g.setColor(new Color(255, 70, 70));
        g.setFont(new Font("Arial", Font.BOLD, 34));
        g.drawString("GAME OVER", 30, panelY + 45);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score + "   Level: " + level + "   AI: " + rivalScore, 30, panelY + 80);

        g.setColor(new Color(220, 220, 220));
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString(lossReason, 30, panelY + 112);

        g.setColor(new Color(255, 215, 80));
        g.drawString("Best Move: " + bestMove + "     Risk: " + riskLevel, 30, panelY + 140);

        g.setColor(new Color(120, 255, 170));
        g.drawString("Advice: " + aiAdvice, 30, panelY + 165);

        g.setColor(new Color(90, 190, 255));
        g.drawString(rivalComment, 30, panelY + 188);

        g.setColor(new Color(0, 230, 110));
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Press SPACE to restart", 380, panelY + 45);
    }
}