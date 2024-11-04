import javax.swing.*;
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

                    if(Content.contains("<") || Content.contains("</")) {
                        JLabel contentLabel = new JLabel(Content);
                        panel.add(contentLabel);
                    }
                    else{
//                        byte[] decodedBytes = decoder.decode(Content);
//                        String decodedHtml = new String(decodedBytes);
//
//                        JLabel decodedContentLabel = new JLabel(decodedHtml);
//                        panel.add(decodedContentLabel);
                    }

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
