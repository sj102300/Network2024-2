package smtp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class OutlineButton extends JButton {
    private final Color background;
    private final Color foreground;

    int paddingWidth = 25, paddingHeight = 5;

    // 생성자: 버튼 배경색과 전경색을 초기화하고, 마우스 이벤트 추가
    public OutlineButton(Color background, Color foreground) {
        this.background = background;
        this.foreground = foreground;
        setText("Outline");

        // 버튼 크기 설정
        Dimension dimension = getPreferredSize();
        int w = (int) dimension.getWidth() + paddingWidth * 2;
        int h = (int) dimension.getHeight() + paddingHeight * 2;

        setPreferredSize(new Dimension(w, h));
        setOpaque(false);
        setBorder(null);
        setBackground(null);
        setForeground(background);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(background);
                setForeground(foreground);
                revalidate();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(null);
                setForeground(background);
                revalidate();
                repaint();
            }
        });
    }

    // 버튼의 커스텀 페인팅 메서드: 버튼 외곽선과 텍스트를 그리는 역할
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Dimension dimension = getPreferredSize();
        int w = (int) dimension.getWidth() - 1;
        int h = (int) dimension.getHeight() - 1;

        // 배경색이 설정된 경우 버튼 배경 그리기
        if (getBackground() != null) {
            g2.setColor(getBackground());
            g2.fillRect(1, 1, w, h);
        }

        // 버튼 외곽선 그리기
        g2.setColor(getForeground());
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(0, 0, w, h);

        // 버튼 텍스트 그리기
        g2.setColor(getForeground());
        g2.setFont(new Font("Arial", 1, 18));

        FontMetrics fontMetrics = g2.getFontMetrics();
        Rectangle rectangle = fontMetrics.getStringBounds(getText(), g2).getBounds();

        g2.drawString(getText(), (w - rectangle.width) / 2, (h - rectangle.height) / 2 + fontMetrics.getAscent());
    }
}