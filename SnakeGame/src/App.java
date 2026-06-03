import javax.swing.*;

public class App {
    public static void main(String[] args) {
        try {
            int boardWidth = 600;
            int boardHeight = 700;

            JFrame frame = new JFrame("Venom Grid");

            SnakeGame snakeGame = new SnakeGame(boardWidth, boardHeight);
            frame.add(snakeGame);
            frame.pack();

            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

        } catch (Exception e) {
            System.out.println("Failed to start the Snake Game.");
            e.printStackTrace();
        }
    }
}