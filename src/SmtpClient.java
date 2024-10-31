import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class SmtpClient {
    // SMTP 서버 설정을 담는 클래스 추가
    static class SmtpConfig {
        String server;
        int port;
        boolean useTLS;

        SmtpConfig(String server, int port, boolean useTLS) {
            this.server = server;
            this.port = port;
            this.useTLS = useTLS;
        }
    }

    // 이메일 서비스별 SMTP 설정
    private static SmtpConfig getSmtpConfig(String emailDomain) {
        switch (emailDomain.toLowerCase()) {
            case "gmail.com":
                return new SmtpConfig("smtp.gmail.com", 587, true);
            case "outlook.com":
            case "hotmail.com":
                return new SmtpConfig("smtp.office365.com", 587, true);
            case "naver.com":
                return new SmtpConfig("smtp.naver.com", 587, true);
            case "yahoo.com":
                return new SmtpConfig("smtp.mail.yahoo.com", 587, true);
            case "daum.net":
                return new SmtpConfig("smtp.daum.net", 465, true);
            default:
                throw new IllegalArgumentException("Unsupported email domain: " + emailDomain);
        }
    }

    public static void main(String[] args) {
        String fromEmail = "swingaaa@naver.com";
        String toEmail1 = "mjung0811@naver.com";

        // 이메일 도메인 추출 (@ 이후 부분)
        String emailDomain = fromEmail.substring(fromEmail.indexOf("@") + 1);

        // 해당 도메인의 SMTP 설정 가져오기
        SmtpConfig config = getSmtpConfig(emailDomain);

        String username = fromEmail;
        String password = "ehrekf1!@"; // 앱 비밀번호나 일반 비밀번호
        String subject = "Test Email from Java";
        String body = "Hello, this is a test email sent via raw SMTP commands in Java.";

        try {
            // SMTP 서버 연결 및 스트림 생성
            Socket socket = new Socket(config.server, config.port);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 서버 응답 확인하는 헬퍼 메서드
            String response = reader.readLine();
            System.out.println("Server: " + response);

            // EHLO 명령어 전송
            writer.println("EHLO " + config.server);
            readResponse(reader);

            if(config.useTLS) {
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
                        config.server,
                        config.port,
                        true
                );
                sslSocket.startHandshake();
                PrintWriter sslWriter = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream(), "UTF-8"), true);
                BufferedReader sslReader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), "UTF-8"));

                // TLS 이후 다시 EHLO 명령어
                sslWriter.println("EHLO " + config.server);
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
            }
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