import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class SmtpClient {
    public static void main(String[] args) {
        String smtpServer = "smtp.gmail.com";
        int port = 587;
        String fromEmail = "kt52488872@gmail.com";
        String toEmail1 = "leegh963@naver.com";
        String username = "kt52488872@gmail.com";
        String password = ""; // 앱 비밀번호나 일반 비밀번호
        String subject = "Test Email from Java";
        String body = "Hello, this is a test email sent via raw SMTP commands in Java.";


        try {
            // SMTP 서버 연결 및 스트림 생성
            Socket socket = new Socket(smtpServer, port);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 서버 응답 확인하는 헬퍼 메서드
            String response = reader.readLine();
            System.out.println("Server: " + response);

            // EHLO 명령어 전송
            writer.println("EHLO " + smtpServer);
            readResponse(reader);

            // TLS 시작 (STARTTLS)
            writer.println("STARTTLS");
            readResponse(reader);

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
                    smtpServer,
                    port,
                    true
            );
            sslSocket.startHandshake(); // SSL 핸드셰이크 시작하여 TLS 연결 설정
            PrintWriter sslWriter = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream(), "UTF-8"), true);
            BufferedReader sslReader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), "UTF-8"));

            // TLS 이후 다시 EHLO 명령어
            sslWriter.println("EHLO " + smtpServer);
            readResponse(sslReader);

            // 인증 (AUTH LOGIN)
            sslWriter.println("AUTH LOGIN");
            readResponse(sslReader);

            // 사용자 이름과 비밀번호를 Base64로 인코딩하여 전송
            sslWriter.println(Base64.getEncoder().encodeToString(username.getBytes()));
            readResponse(sslReader);
            sslWriter.println(Base64.getEncoder().encodeToString(password.getBytes()));
            readResponse(sslReader);

            // 메일 전송 명령어 (MAIL FROM, RCPT TO, DATA)
            sslWriter.println("MAIL FROM:<" + fromEmail + ">");
            readResponse(sslReader);

            sslWriter.println("RCPT TO:<" + toEmail1 + ">");
            readResponse(sslReader);

//            sslWriter.println("RCPT TO:<" + toEmail2 + ">");
//            readResponse(sslReader);

            sslWriter.println("DATA");
            readResponse(sslReader);

            // 메일 본문 작성
            sslWriter.println("Subject: " + subject);
            sslWriter.println("From: " + fromEmail);
            sslWriter.println("To: " + toEmail1);
            sslWriter.println();
            sslWriter.print(body);
            sslWriter.print("\r\n");
            sslWriter.print(".");
            sslWriter.print("\r\n");
            sslWriter.flush();
            readResponse(sslReader);

            // QUIT 명령어로 연결 종료
            sslWriter.println("QUIT");
            readResponse(sslReader);

            sslWriter.close();
            sslReader.close();
            sslSocket.close();

            System.out.println("Email sent successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    // SMTP 응답 읽기 헬퍼 메서드
    private static void readResponse(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("Server: " + line);
            if (line.charAt(3) == ' ') {
                break;
            }
        }
    }
}
