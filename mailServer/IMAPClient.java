import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class IMAPClient {
    public static void main(String[] args) {
        String host = "imap.naver.com";
        int port = 993;
        String username = "leegh963@naver.com";
        String password = "QWC999FSWD6Z";

        try {
            // SSL 소켓 생성 및 연결

            Socket socket = new Socket(host, port);

            SSLContext sslContext = SSLContext.getInstance("TLS");

            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) { }

                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }}, new java.security.SecureRandom());


            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                    socket,
                    host,
                    port,
                    true
            );
            sslSocket.startHandshake(); // SSL 핸드셰이크 시작하여 TLS 연결 설정
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));

            // 초기 응답 확인
            System.out.println("Server: " + reader.readLine());

            // 1. 로그인
            sendCommand(writer, reader, "a1 LOGIN "  + "\"" + username + "\"" + " "  + "\"" + password + "\"");

            // 2. 받은 편지함 선택
            sendCommand(writer, reader, "a2 SELECT INBOX");

            // 3. 메일 목록 가져오기
            int messageCount = getMessageCount(writer, reader);
            if (messageCount > 0) {
                // 4. 가장 최근 메일 가져오기 (메일 ID가 가장 큰 번호가 가장 최근 메일)
                sendCommand(writer, reader, "a3 FETCH " + messageCount + " (BODY[HEADER.FIELDS (SUBJECT FROM DATE)])");
            } else {
                System.out.println("No emails found in INBOX.");
            }

            // 4. 로그아웃
            sendCommand(writer, reader, "a4 LOGOUT");

            // 소켓 닫기
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int getMessageCount(PrintWriter writer, BufferedReader reader) throws Exception {
        writer.println("a2 STATUS INBOX (MESSAGES)");
        System.out.println("C: a2 STATUS INBOX (MESSAGES)");

        String response;
        int messageCount = 0;

        while ((response = reader.readLine()) != null) {
            System.out.println("S: " + response);
            if (response.contains("MESSAGES")) {
                // "MESSAGES n"에서 n 값을 추출
                String[] parts = response.split(" ");
                messageCount = Integer.parseInt(parts[4]);
            }
            if (response.startsWith("a2 OK")) {
                break;
            }
        }
        return messageCount;
    }

    private static void sendCommand(PrintWriter writer, BufferedReader reader, String command) throws Exception {
        writer.println(command);
        System.out.println("C: " + command);

        String response;
        while ((response = reader.readLine()) != null) {
            System.out.println("S: " + response);
            // 명령어에 대한 응답이 완료되었는지 확인 (태그가 "OK" 또는 "NO"로 끝나는 경우)
            if (response.startsWith("a") && (response.contains("OK") || response.contains("NO") || response.contains("BAD"))) {
                break;
            }
        }
    }
}
