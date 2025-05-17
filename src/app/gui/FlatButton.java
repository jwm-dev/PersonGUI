package src.app.gui;

import javax.swing.*;
import java.awt.*;

public class FlatButton extends JButton {
    private float animPhase = 0f;
    private boolean hovered = false;
    private javax.swing.Timer timer;

    public FlatButton(String text) {
        super(text);
        setUI(new FlatButtonUI()); // Explicitly set the UI delegate
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        Font f = getFont();
        if (f != null) setFont(f.deriveFont(Font.BOLD, 13f));
        setMargin(new Insets(0, 0, 0, 0));
        setMinimumSize(new Dimension(60, 22));
        setPreferredSize(new Dimension(120, 22));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                hovered = true;
                startAnim();
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                hovered = false;
                startAnim();
            }
        });
    }

    private void startAnim() {
        if (timer == null) {
            timer = new javax.swing.Timer(16, _ -> {
                animPhase += 0.08f;
                repaint();
                if (!hovered && animPhase > 0.99f) {
                    animPhase = 0f;
                    timer.stop();
                }
            });
        }
        if (!timer.isRunning()) timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = UIManager.getColor("Viewer.buttonBackground");
        Color fg = UIManager.getColor("Viewer.buttonForeground");
        if (bg == null) bg = UIManager.getColor("Button.background");
        if (fg == null) fg = UIManager.getColor("Button.foreground");
        boolean pressed = getModel().isArmed() && getModel().isPressed();
        float anim = Math.min(1f, animPhase);
        Color hoverBg = blend(bg, Color.WHITE, 0.10f + 0.10f * anim);
        Color pressedBg = blend(bg, Color.BLACK, 0.07f + 0.10f * anim);
        Color useBg = pressed ? pressedBg : (hovered ? hoverBg : bg);
        g2.setColor(useBg);
        g2.fillRect(0, 0, getWidth(), getHeight());
        // Draw text
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() + textHeight) / 2 - 2;
        g2.setColor(fg != null ? fg : Color.BLACK);
        g2.drawString(text, x, y);
        g2.dispose();
    }

    private static Color blend(Color c1, Color c2, float ratio) {
        float ir = 1.0f - ratio;
        int r = (int) (c1.getRed() * ir + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * ir + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * ir + c2.getBlue() * ratio);
        return new Color(r, g, b, 255);
    }
}
