package imap;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class IMAPClient {

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

    public static Map<String, Map<String, Object>>  getUnreadEmails() {

        String host = "imap.naver.com";
        int port = 993;
        String username = "ye6194@naver.com";
        String password = "";

        Map<String, Map<String, Object>> emails = new HashMap<>();

        try {
            // SSL 소켓 생성 및 연결

            Socket socket = new Socket(host, port);

            SSLContext sslContext = SSLContext.getInstance("TLS");

            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
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
            sendCommand(writer, reader, "a1 LOGIN " + "\"" + username + "\"" + " " + "\"" + password + "\"");

            // 2. 받은 편지함 선택
            sendCommand(writer, reader, "a2 SELECT INBOX");

            // 3. 메일 목록 가져오기
            writer.println("a3 SEARCH UNSEEN");
            System.out.println("C: " + "a3 SEARCH UNSEEN");

            // 검색 결과에서 메일 ID 목록을 추출하고, 각 ID에 대해 메일을 FETCH
            String response;
            String[] ids = new String[5];
            while ((response = reader.readLine()) != null) {
                System.out.println("S: " + response);
                if (response.startsWith("* SEARCH")) {
                    String[] tmp = response.split(" ");
                    // 첫 번째 요소는 "* SEARCH"이므로 그 이후가 메일 ID
                    int idx = 0;
                    for (int i = tmp.length - 1; i >= 2 && i >= tmp.length - 5; i--) {
                        ids[idx++] = tmp[i];
                    }
                }
                if (response.startsWith("a3 OK")) {
                    break;
                }
            }

            for (int i = 0; i < 5; i++) {
                writer.println("a4 FETCH " + ids[i] + " (BODY[HEADER] BODY[TEXT])");
                System.out.println("C: " + "a4 FETCH " + ids[i] + " (BODY[HEADER] BODY[TEXT])");

                Map<String, Object> email = new HashMap<>();
                Map<String, String> header = new HashMap<>();
                Map<String, Map<String, String>> body = new HashMap<>();

                int bodyCount = 0;

                while ((response = reader.readLine()) != null) {

                    if (response.contains("From:")) {

                        int i1 = response.indexOf("<") + 1;
                        int i2 = response.indexOf(">");

                        // 메일에 괄호가 포함되어 오는 경우와 아닌 경우 분할 <www.naver.com> 과 www.naver.com 구분
                        if (i1 == 0) header.put("From", response.substring(6));
                        else header.put("From", response.substring(i1, i2));

                    } else if (response.contains("To:")) {

                        int i1 = response.indexOf("<") + 1;
                        int i2 = response.indexOf(">");

                        // 메일에 괄호가 포함되어 오는 경우와 아닌 경우 분할 <www.naver.com> 과 www.naver.com 구분
                        if (i1 == 0) header.put("To", response.substring(4));
                        else header.put("To", response.substring(i1, i2));

                    } else if (response.contains("Subject:")) {

                        if(response.contains("=?UTF-8?B?") || response.contains("=?utf-8?B?")) {
                            String title = response.substring(19, response.length() - 2);
                            title = decodeBase64(title);
                            header.put("Subject", title);
                        }
                        else if(response.contains("=?UTF-8?Q?") || response.contains("=?utf-8?Q?")) {
                            String title = response.substring(19, response.length() - 2);
                            title = decodeQuotedPrintable(title);
                            header.put("Subject", title);
                        }
                        else{
                            String title = response.substring(9);
                            if(title.isEmpty()){
                                title = "제목 없음";
                            }
                            header.put("Subject", response.substring(9));
                        }
                    } else if (response.contains("Date:")) {
                        header.put("Date", response.substring(6));
                    }

                    if (response.startsWith("Content-Type:") && !response.contains("multipart")) {
                        // content-type 이 multipart 인 경우 본문에서 content-type 을 가져옴, 맨 아래에 multipart 와 아닌것 구분해서 예시에 작성
                        String contentType = response.substring(13).trim();
                        StringBuilder contentBuilder = new StringBuilder();

                        // 본문 읽기
                        while ((response = reader.readLine()) != null
                                && !response.startsWith("--")) {

                            if (response.startsWith("a") && (response.contains("OK") || response.contains("NO") || response.contains("BAD"))) {
                                break;
                            }

                            if (!response.contains("charset") && !response.contains("Content") && !response.contains("boundary") && !response.contains("BODY")) {
                                contentBuilder.append(response).append("\n");
                            }
                        }

                        // 본문 정보를 body에 저장
                        Map<String, String> bodyPart = new HashMap<>();
                        bodyPart.put("Content-Type", contentType);
                        bodyPart.put("Content", contentBuilder.toString().trim());
                        body.put(String.valueOf(bodyCount++), bodyPart);
                    }

                    email.put("header", header);
                    email.put("body", body);
                    emails.put(String.valueOf(i), email);

                    if (response.startsWith("a") && (response.contains("OK") || response.contains("NO") || response.contains("BAD"))) {
                        break;
                    }

                }

            }

            sendCommand(writer, reader, "a5 LOGOUT");

            // 소켓 닫기
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return emails;
    }

    private static String decodeBase64(String input) {
        byte[] decodedBytes = Base64.getDecoder().decode(input);
        return new String(decodedBytes);
    }

    private static String decodeQuotedPrintable(String input) throws UnsupportedEncodingException {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '=' && i + 2 < input.length()) {
                String hex = input.substring(i + 1, i + 3);
                int value = Integer.parseInt(hex, 16);
                decoded.append((char) value);
                i += 2;
            } else if (c == '_') {
                decoded.append(' ');
            } else {
                decoded.append(c);
            }
        }
        return decoded.toString();
    }

}