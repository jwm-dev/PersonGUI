package src.app.gui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

/**
 * Flat, React-like button UI for all JButtons in the app.
 */
public class FlatButtonUI extends BasicButtonUI {
    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        JButton btn = (JButton) c;
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setMinimumSize(new Dimension(60, 22));
        btn.setPreferredSize(new Dimension(120, 22));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        JButton btn = (JButton) c;
        AbstractButton b = btn;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = UIManager.getColor("Viewer.buttonBackground");
        Color fg = UIManager.getColor("Viewer.buttonForeground");
        if (bg == null) bg = UIManager.getColor("Button.background");
        if (fg == null) fg = UIManager.getColor("Button.foreground");
        boolean hovered = b.getModel().isRollover();
        boolean pressed = b.getModel().isArmed() && b.getModel().isPressed();
        Color hoverBg = blend(bg, Color.WHITE, 0.10f);
        Color pressedBg = blend(bg, Color.BLACK, 0.07f);
        Color useBg = pressed ? pressedBg : (hovered ? hoverBg : bg);
        g2.setColor(useBg);
        g2.fillRect(0, 0, c.getWidth(), c.getHeight());
        // No border at all
        // Draw text
        FontMetrics fm = g2.getFontMetrics();
        String text = b.getText();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        int x = (c.getWidth() - textWidth) / 2;
        int y = (c.getHeight() + textHeight) / 2 - 2;
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
