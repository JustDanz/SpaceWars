package com.mycompany.spacewars;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class SpaceWars extends JPanel implements ActionListener {
    private static Image backgroundImage;
    private Timer timer;
    private int playerX = 275;
    private final int playerY = 600;
    private int backgroundY = 0;
    private int score = 0;
    private boolean gameRunning = false;
    private long lastBulletTime = 0;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private Image playerImage, enemyImage, bulletImage, enemyBulletImage, explosionImage;
    private ArrayList<Point> bullets = new ArrayList<>();
    private ArrayList<Point> enemyBullets = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private ArrayList<Explosion> explosions = new ArrayList<>();
    private Random random = new Random();
    private JFrame frame;
    private long lastEnemyFireTime = 0; // To control enemy firing

    public SpaceWars() {
        // Load images
        playerImage = new ImageIcon("src/assets/player.png").getImage()
                .getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        enemyImage = new ImageIcon("src/assets/enemy.png").getImage()
                .getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        bulletImage = new ImageIcon("src/assets/bullet.png").getImage()
                .getScaledInstance(40, 60, Image.SCALE_SMOOTH);
        enemyBulletImage = new ImageIcon("src/assets/bullet_1.png").getImage()
                .getScaledInstance(40, 60, Image.SCALE_SMOOTH);
        backgroundImage = new ImageIcon("src/assets/background.png").getImage();
        explosionImage = new ImageIcon("src/assets/ledak.png").getImage()
                .getScaledInstance(100, 100, Image.SCALE_SMOOTH);

        timer = new Timer(16, this);
        setFocusable(true);

        // Key listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_LEFT) {
                    moveLeft = true;
                } else if (key == KeyEvent.VK_RIGHT) {
                    moveRight = true;
                } else if (key == KeyEvent.VK_SPACE) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastBulletTime > 100) {
                        bullets.add(new Point(playerX + 30, playerY));
                        lastBulletTime = currentTime;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_LEFT) {
                    moveLeft = false;
                } else if (key == KeyEvent.VK_RIGHT) {
                    moveRight = false;
                }
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw background, player, bullets, enemies
        g.drawImage(backgroundImage, 0, backgroundY, getWidth(), getHeight(), null);
        g.drawImage(backgroundImage, 0, backgroundY - getHeight(), getWidth(), getHeight(), null);
        g.drawImage(playerImage, playerX, playerY, null);

        for (Point bullet : bullets) {
            g.drawImage(bulletImage, bullet.x, bullet.y, null);
        }

        for (Point enemyBullet : enemyBullets) {
            g.drawImage(enemyBulletImage, enemyBullet.x, enemyBullet.y, null);
        }

        for (Enemy enemy : enemies) {
            g.drawImage(enemyImage, enemy.x, enemy.y, null);
        }

        for (Explosion explosion : explosions) {
            g.drawImage(explosionImage, explosion.x, explosion.y, null);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 30);

        // Draw copyright text in the bottom left corner
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Copyright by Justdan", 10, getHeight() - 10);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;

        // Player movement
        if (moveLeft && playerX > 0) {
            playerX -= 10;
        } else if (moveRight && playerX < getWidth() - 100) {
            playerX += 10;
        }

        // Move bullets
        ArrayList<Point> toRemoveBullets = new ArrayList<>();
        for (Point bullet : bullets) {
            bullet.y -= 8;
            if (bullet.y < 0) {
                toRemoveBullets.add(bullet);
            }
        }
        bullets.removeAll(toRemoveBullets);

        // Move enemy bullets
        ArrayList<Point> toRemoveEnemyBullets = new ArrayList<>();
        for (Point enemyBullet : enemyBullets) {
            enemyBullet.y += 6;
            if (enemyBullet.y > getHeight()) {
                toRemoveEnemyBullets.add(enemyBullet);
            }
            if (enemyBullet.x >= playerX && enemyBullet.x <= playerX + 100 &&
                enemyBullet.y >= playerY && enemyBullet.y <= playerY + 100) {
                gameOver();
            }
        }
        enemyBullets.removeAll(toRemoveEnemyBullets);

        // Check collisions between bullets and enemies
        ArrayList<Enemy> toRemoveEnemies = new ArrayList<>();
        ArrayList<Explosion> toRemoveExplosions = new ArrayList<>();
        for (Point bullet : bullets) {
            for (Enemy enemy : enemies) {
                if (bullet.x >= enemy.x && bullet.x <= enemy.x + 100 &&
                    bullet.y >= enemy.y && bullet.y <= enemy.y + 100) {
                    toRemoveBullets.add(bullet);
                    toRemoveEnemies.add(enemy);
                    score += 100;
                    // Create explosion effect
                    explosions.add(new Explosion(enemy.x, enemy.y));
                    break;
                }
            }
        }
        bullets.removeAll(toRemoveBullets);
        enemies.removeAll(toRemoveEnemies);

        // Remove explosions after a brief period
        for (Explosion explosion : explosions) {
            if (System.currentTimeMillis() - explosion.time > 500) {
                toRemoveExplosions.add(explosion);
            }
        }
        explosions.removeAll(toRemoveExplosions);

        // Spawn new enemies
        if (random.nextDouble() < 0.02 && enemies.size() < 3) {
            int enemyX = random.nextInt(getWidth() - 100);
            enemies.add(new Enemy(enemyX, -100));
        }

        // Move enemies
        ArrayList<Enemy> toRemoveEnemiesList = new ArrayList<>();
        for (Enemy enemy : enemies) {
            enemy.y += 3;
            if (enemy.y > getHeight()) {
                toRemoveEnemiesList.add(enemy);
            }
            // Check collision with player
            if (Math.abs(playerX - enemy.x) < 50 && Math.abs(playerY - enemy.y) < 50) {
                gameOver();
            }
        }
        enemies.removeAll(toRemoveEnemiesList);

        // Spawn enemy bullets every 2 seconds
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEnemyFireTime > 2500) {
            for (Enemy enemy : enemies) {
                enemyBullets.add(new Point(enemy.x + 25, enemy.y + 50));
            }
            lastEnemyFireTime = currentTime;
        }

        // Move background
        backgroundY += 3;
        if (backgroundY >= getHeight()) {
            backgroundY = 0;
        }

        repaint();
    }

    private void gameOver() {
        gameRunning = false;
        // Close the current game window
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        topFrame.dispose(); // Close current game window
        
        // Show Game Over message and redirect to the main menu
        JOptionPane.showMessageDialog(this, "Game Over! Your Score: " + score);
        showMainMenu();  // Optionally, you can call a method to show the main menu here
    }

    public void startGame() {
        gameRunning = true;
        timer.start();
    }

    private void showMainMenu() {
        frame = new JFrame("SpaceWars Main Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        // Using JPanel with GridBagLayout to center the button
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(e -> {
            frame.dispose();
            JFrame gameFrame = new JFrame("SpaceWars");
            SpaceWars gamePanel = new SpaceWars();
            gameFrame.add(gamePanel);
            gameFrame.setSize(600, 800);
            gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            gameFrame.setLocationRelativeTo(null); // Center the window
            gameFrame.setVisible(true);
            gamePanel.startGame();
        });

        panel.add(startButton, gbc); // Add the start button centered in the panel

        frame.add(panel);
        frame.setLocationRelativeTo(null); // Center the frame on the screen
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SpaceWars().showMainMenu());
    }
}

    class Enemy {
        int x, y;

        public Enemy(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    class Explosion {
        int x, y;
        long time;

        public Explosion(int x, int y) {
            this.x = x;
            this.y = y;
            this.time = System.currentTimeMillis();
        }
    }
