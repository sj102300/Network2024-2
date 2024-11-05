package imap;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailViewer {
    public static void main(String[] args) throws UnsupportedEncodingException {
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

            for (Map.Entry<String, Object> oneBody : body.entrySet()) {

                String bodyId = oneBody.getKey();
                Map<String, String> bodyContent = (Map<String, String>) oneBody.getValue();

                String contentType = bodyContent.get("Content-Type");
                String encodingType = bodyContent.get("Content-Transfer-Encoding");
                String content = bodyContent.get("Content");

                System.out.println(contentType + " " + encodingType);

                if(encodingType.isEmpty()) {
                    if (contentType.contains("text/html")) {
                        if(!(content.contains("<") || content.contains("</"))){
                            content = decodeBase64(content);
                        }
                    }
                }
                else if(encodingType.contains("base64")){
                    content = decodeBase64(content);
                    System.out.println("here");
                }
                else if(encodingType.equals("quoted-printable")){
                    content = decodeQuotedPrintable(content);
                }
                else{
                    content = "지원하지 않는 형식입니다.";
                }

                // JTextArea를 사용하여 텍스트를 출력
                JTextArea textArea = new JTextArea(content);
                textArea.setLineWrap(true);  // 줄바꿈 설정
                textArea.setWrapStyleWord(true);  // 단어 단위로 줄바꿈
                textArea.setEditable(false);  // 편집 불가능하게 설정

                // 패널에 추가
                panel.add(new JScrollPane(textArea));  // 스크롤 가능하게 설정
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


    private static String decodeBase64(String content) {

        try {
            content = content.replaceAll("\\s", "");
            // 문자열 길이를 4의 배수로 맞추기 위해 패딩 추가
            while (content.length() % 4 != 0) {
                content += "=";
            }

            byte[] decodedBytes = Base64.getDecoder().decode(content);
            content = new String(decodedBytes);

        } catch (IllegalArgumentException e) {
//            System.err.println("Invalid Base64 content: " + e.getMessage());
        }

        return content;
    }

    private static String decodeQuotedPrintable(String encoded) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int length = encoded.length();
        for (int i = 0; i < length; i++) {
            char ch = encoded.charAt(i);

            if (ch == '=') {
                if (i + 2 < length) {
                    String hex = encoded.substring(i + 1, i + 3);
                    try {
                        byte decodedByte = (byte) Integer.parseInt(hex, 16);
                        byteArrayOutputStream.write(decodedByte);
                        i += 2; // Skip the next two characters
                    } catch (NumberFormatException e) {
                        byteArrayOutputStream.write(ch);
                    }
                } else {
                    byteArrayOutputStream.write(ch);
                }
            } else {
                byteArrayOutputStream.write(ch);
            }
        }

        // Convert the collected bytes to a UTF-8 string
        return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
    }


}
