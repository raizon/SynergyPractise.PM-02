import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * RuTube Video Catcher — приложение для предметной области «Видеохостинг».
 * Производственная практика ПМ.02 «Ревьюирование программных модулей».
 * Студент: Левицкий М.Ю., группа ОКИП-22209МО
 *
 * Приложение демонстрирует:
 * - Работу с графикой (Graphics2D, градиенты, сглаживание)
 * - Анимацию (перемещение объектов, таймер)
 * - Обработку событий мыши (MouseAdapter)
 * - Использование коллекций (ArrayList)
 * - ООП (вложенные классы, наследование JPanel)
 */
public class RuTubeApp extends JFrame {

    public RuTubeApp() {
        setTitle("RuTube Video Catcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RuTubeApp::new);
    }

    /**
     * Класс, представляющий падающий объект — кнопку «Play».
     */
    static class PlayButton {
        double x, y;          // позиция центра
        double speed;          // скорость падения (пикселей/кадр)
        int size;              // размер кнопки
        boolean caught;        // поймана ли кнопка
        float alpha;           // прозрачность (для анимации исчезновения)
        Color color;           // цвет кнопки

        private static final Color[] COLORS = {
                new Color(0xE5, 0x1A, 0x2C),  // красный (RuTube)
                new Color(0xFF, 0x45, 0x00),  // оранжево-красный
                new Color(0xCC, 0x00, 0x33),  // тёмно-красный
                new Color(0xFF, 0x66, 0x33),  // оранжевый
        };

        PlayButton(double x, double y, double speed, int size) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = size;
            this.caught = false;
            this.alpha = 1.0f;
            this.color = COLORS[new Random().nextInt(COLORS.length)];
        }

        void update() {
            if (caught) {
                alpha -= 0.05f;
            } else {
                y += speed;
            }
        }

        boolean contains(int mx, int my) {
            double dx = mx - x;
            double dy = my - y;
            return dx * dx + dy * dy <= (size / 2.0) * (size / 2.0);
        }

        boolean isExpired() {
            return alpha <= 0;
        }

        void draw(Graphics2D g2) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alpha)));

            int r = size / 2;

            // Круг с градиентом
            GradientPaint gp = new GradientPaint(
                    (float) (x - r), (float) (y - r), color.brighter(),
                    (float) (x + r), (float) (y + r), color.darker()
            );
            g2.setPaint(gp);
            g2.fillOval((int) x - r, (int) y - r, size, size);

            // Обводка
            g2.setColor(new Color(255, 255, 255, 80));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval((int) x - r, (int) y - r, size, size);

            // Треугольник «Play»
            int triSize = size / 3;
            int cx = (int) x + triSize / 6;  // смещение вправо для центровки
            int cy = (int) y;
            int[] xp = {cx - triSize / 2, cx - triSize / 2, cx + triSize / 2};
            int[] yp = {cy - triSize / 2, cy + triSize / 2, cy};
            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillPolygon(xp, yp, 3);

            g2.setComposite(old);
        }
    }

    /**
     * Игровая панель с анимацией и обработкой событий.
     */
    static class GamePanel extends JPanel implements ActionListener {
        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;
        private static final int SPAWN_INTERVAL = 40;  // кадров между появлениями
        private static final Color BG_TOP = new Color(0x1A, 0x1A, 0x2E);
        private static final Color BG_BOTTOM = new Color(0x0D, 0x0D, 0x1A);

        private final ArrayList<PlayButton> buttons = new ArrayList<>();
        private final Random random = new Random();
        private final Timer timer;

        private int score = 0;
        private int missed = 0;
        private int frameCount = 0;
        private boolean gameOver = false;

        GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(Color.BLACK);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (gameOver) {
                        // Перезапуск игры
                        score = 0;
                        missed = 0;
                        buttons.clear();
                        gameOver = false;
                        return;
                    }
                    for (PlayButton btn : buttons) {
                        if (!btn.caught && btn.contains(e.getX(), e.getY())) {
                            btn.caught = true;
                            score++;
                            break;
                        }
                    }
                }
            });

            timer = new Timer(25, this);  // ~40 FPS
            timer.start();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver) {
                repaint();
                return;
            }

            frameCount++;

            // Создание новых кнопок
            if (frameCount % SPAWN_INTERVAL == 0) {
                int size = 40 + random.nextInt(30);  // 40-70 px
                double x = size + random.nextInt(WIDTH - size * 2);
                double speed = 1.5 + random.nextDouble() * 2.5;
                buttons.add(new PlayButton(x, -size, speed, size));
            }

            // Обновление позиций
            Iterator<PlayButton> it = buttons.iterator();
            while (it.hasNext()) {
                PlayButton btn = it.next();
                btn.update();

                if (btn.isExpired()) {
                    it.remove();
                } else if (!btn.caught && btn.y > HEIGHT + btn.size) {
                    missed++;
                    it.remove();
                    if (missed >= 10) {
                        gameOver = true;
                    }
                }
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Фон — тёмный градиент
            GradientPaint bg = new GradientPaint(0, 0, BG_TOP, 0, HEIGHT, BG_BOTTOM);
            g2.setPaint(bg);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // Декоративные линии (сетка)
            g2.setColor(new Color(255, 255, 255, 8));
            for (int i = 0; i < WIDTH; i += 40) {
                g2.drawLine(i, 0, i, HEIGHT);
            }
            for (int i = 0; i < HEIGHT; i += 40) {
                g2.drawLine(0, i, WIDTH, i);
            }

            // Логотип RuTube
            drawLogo(g2);

            // Кнопки Play
            for (PlayButton btn : buttons) {
                btn.draw(g2);
            }

            // Счёт
            drawHUD(g2);

            // Game Over
            if (gameOver) {
                drawGameOver(g2);
            }
        }

        private void drawLogo(Graphics2D g2) {
            // «RUTUBE» в верхней части
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            String text = "RUTUBE";
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int x = (WIDTH - textWidth) / 2;
            int y = 40;

            // Красная подложка
            int pad = 8;
            g2.setColor(new Color(0xE5, 0x1A, 0x2C));
            g2.fillRoundRect(x - pad * 2, y - fm.getAscent() - pad,
                    textWidth + pad * 4, fm.getHeight() + pad * 2, 10, 10);

            // Белый текст
            g2.setColor(Color.WHITE);
            g2.drawString(text, x, y);

            // Подзаголовок
            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            String sub = "Video Catcher — ловите кнопки Play!";
            int subW = g2.getFontMetrics().stringWidth(sub);
            g2.setColor(new Color(255, 255, 255, 120));
            g2.drawString(sub, (WIDTH - subW) / 2, y + 25);
        }

        private void drawHUD(Graphics2D g2) {
            g2.setFont(new Font("Arial", Font.BOLD, 18));

            // Счёт (слева)
            g2.setColor(new Color(0x4C, 0xAF, 0x50));
            g2.drawString("Поймано: " + score, 20, HEIGHT - 20);

            // Пропущено (справа)
            g2.setColor(missed >= 7 ? new Color(0xFF, 0x44, 0x44) : new Color(0xFF, 0xA7, 0x26));
            String missText = "Пропущено: " + missed + " / 10";
            int missW = g2.getFontMetrics().stringWidth(missText);
            g2.drawString(missText, WIDTH - missW - 20, HEIGHT - 20);
        }

        private void drawGameOver(Graphics2D g2) {
            // Затемнение
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // Текст Game Over
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.setColor(new Color(0xE5, 0x1A, 0x2C));
            String go = "GAME OVER";
            int goW = g2.getFontMetrics().stringWidth(go);
            g2.drawString(go, (WIDTH - goW) / 2, HEIGHT / 2 - 30);

            // Итоговый счёт
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.setColor(Color.WHITE);
            String sc = "Ваш счёт: " + score;
            int scW = g2.getFontMetrics().stringWidth(sc);
            g2.drawString(sc, (WIDTH - scW) / 2, HEIGHT / 2 + 20);

            // Подсказка
            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            g2.setColor(new Color(255, 255, 255, 150));
            String hint = "Нажмите для перезапуска";
            int hW = g2.getFontMetrics().stringWidth(hint);
            g2.drawString(hint, (WIDTH - hW) / 2, HEIGHT / 2 + 55);
        }
    }
}
