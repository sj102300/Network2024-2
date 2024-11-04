import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class EmailViewer {
    public static void main(String[] args) {
        // 예제 이메일 데이터를 포함하는 HashMap
        Map<String, Map<String, Object>> emails = IMAPClient.getUnreadEmails();

        // JFrame 설정
        JFrame frame = new JFrame("Email Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // JPanel 설정
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // HashMap의 각 항목을 처리
        for (Map.Entry<String, Map<String, Object>> email : emails.entrySet()) {
            String emailId = email.getKey();
            Map<String, Object> response = email.getValue();

            Map<String, String> header = (Map<String, String>) response.get("header");
            Map<String, Object> body = (Map<String, Object>) response.get("body");

            System.out.println(header.toString());
            panel.add(new JLabel("ID: " + emailId));
            panel.add(new JLabel("From: " + header.get("From")));
            panel.add(new JLabel("To: " + header.get("To")));
            panel.add(new JLabel("Subject: " + header.get("Subject")));
            panel.add(new JLabel("Content: "));

            Base64.Decoder decoder = Base64.getDecoder();

            for (Map.Entry<String, Object> oneBody : body.entrySet()) {

                String bodyId = oneBody.getKey();
                Map<String, String> bodyContent = (Map<String, String>) oneBody.getValue();

                if (bodyContent.get("Content-Type").contains("text/html")) {
                    String Content = bodyContent.get("Content");

                    // Base64 디코딩 (필요한 경우)
                    if (!Content.contains("<") && !Content.contains("</")) {
                        Content = Content.replaceAll("\\s", "");
                        byte[] decodedBytes = decoder.decode(Content);
                        Content = new String(decodedBytes, StandardCharsets.UTF_8);
                    }

                    // JTextArea를 사용하여 텍스트를 출력
                    JTextArea textArea = new JTextArea(Content);
                    textArea.setLineWrap(true);  // 줄바꿈 설정
                    textArea.setWrapStyleWord(true);  // 단어 단위로 줄바꿈
                    textArea.setEditable(false);  // 편집 불가능하게 설정

                    // 패널에 추가
                    panel.add(new JScrollPane(textArea));  // 스크롤 가능하게 설정

                }
            }

            // 각 이메일 사이에 10픽셀 공백 추가
            panel.add(Box.createVerticalStrut(10));
        }
        // JScrollPane으로 스크롤 가능하도록 설정
        JScrollPane scrollPane = new JScrollPane(panel);
        frame.add(scrollPane);

        // JFrame 표시
        frame.setVisible(true);
    }
}