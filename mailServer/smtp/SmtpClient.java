package smtp;

// 기존 SMTPClient 코드 -> file 부분만 조금 변경

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

public class SmtpClient {

    // 이메일 전송 메서드 -> 기존의 하드코딩된 변수들을 ui 입력 정보에서 받아온다.
    public static void sendEmail(String smtpServer, int port, String fromEmail, String[] toEmails,
                                 String username, String password, String subject, String body, List<File> attachments) {
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

            // TLS 를 이용하는 sslContext 를 생성, sslContext 는 ssl/tls 의 설정들을 담는다
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // sslContext.init 을 이용해 ssl 설정
            // keyManager 는 나를 상대방에게 인증시킬 때 사용
            // trustManager 는 상대방의 인증을 확인할 때 사용
            // 마지막 secureRandom 은 암호키 생성에 무작위성 부여
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }}, new java.security.SecureRandom());

            // sslContext 를 이용해 socketFactory 를 생성하고
            // 여기서 createSocket 을 이용해 기존 socket 을 ssl wrapping 한다.
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                    socket,
                    smtpServer,
                    port,
                    true);
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

            // 수신자 추가
            for (String toEmail : toEmails) {
                sslWriter.println("RCPT TO:<" + toEmail + ">");
                readResponse(sslReader);
            }

            // 이메일의 본문 시작임을 알림
            sslWriter.println("DATA");
            readResponse(sslReader);

            // 메일 본문을 구성해서 보낸다
            sslWriter.print(makeBody(subject, fromEmail, toEmails, body, attachments));
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
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    // 이메일 본문 구성 메서드 (본문과 첨부 파일 포함) -> body와 attachments도 받아와서 사용
    private static String makeBody(String subject, String fromEmail, String[] toEmails, String body, List<File> attachments) {
        StringBuilder stringBuilder = new StringBuilder();

        // 제목 , From , to 적어주고
        stringBuilder.append("Subject: ").append(subject).append("\n");
        stringBuilder.append("From: ").append(fromEmail).append("\n");
        stringBuilder.append("To: ");
        for (String toEmail : toEmails) {
            stringBuilder.append(toEmail).append(", ");
        }
        stringBuilder.append("\n");

        // 헤더 부분 , multipart 사용을 위해 boundary 지정 (multipart 의 경우 boundary 를 이용해 본문을 구분)
        stringBuilder.append("MIME-Version: 1.0").append("\n");
        stringBuilder.append("Content-Type: multipart/mixed; boundary=\"simple_boundary\"\n\n");

        // 여기서 부터 body , 본문 내용 추가
        stringBuilder.append("--simple_boundary\n");
        stringBuilder.append("Content-Type: text/plain; charset=\"utf-8\"\n\n");
        stringBuilder.append(body).append("\n\n");


        // 첨부 파일 추가 (여러 개의 첨부 파일 처리)
        if (attachments != null && !attachments.isEmpty()) {
            for (File attachment : attachments) {
                try {
                    String fileContent = Base64.getEncoder().encodeToString(Files.readAllBytes(attachment.toPath()));
                    stringBuilder.append("--simple_boundary\n");
                    stringBuilder.append("Content-Type: application/octet-stream; name=\"").append(attachment.getName()).append("\"\n");
                    stringBuilder.append("Content-Transfer-Encoding: base64\n");
                    stringBuilder.append("Content-Disposition: attachment; filename=\"").append(attachment.getName()).append("\"\n\n");
                    stringBuilder.append(fileContent).append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 이 boundary 를 통해 MIME 본문이 끝났음을 알려준다
        stringBuilder.append("--simple_boundary--\n");

        // <CRLF>.<CRLF> 를 이용해 메시지의 전송이 끝남을 나타낸다.
        stringBuilder.append("\r\n.\r\n");

        return stringBuilder.toString();
    }

    // SMTP 응답 읽기 메서드
    private static void readResponse(BufferedReader reader) throws IOException {
        String line;

        // SMTP 응답의 형태는 3자리 숫자 코드 + ' ' (공백) 혹은 '-' 으로 온다.
        // 3자리 코드 뒤에 '-' 이 아닌 공백인 경우는 응답의 마지막 줄이라는 뜻이므로 break
        while ((line = reader.readLine()) != null) {
            System.out.println("Server: " + line);
            if (line.charAt(3) == ' ') {
                break;
            }
        }
    }
}
