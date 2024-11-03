import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class IMAPClient {
    public static void main(String[] args) {
        String host = "imap.naver.com";
        int port = 993;
        String username = "leegh963@naver.com";
        String password = "";

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
            writer.println("a3 SEARCH ALL");
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

            Map<String, Map<String, Object>> emails = new HashMap<>();

            for(int i=0;i<5;i++) {
                writer.println("a4 FETCH " + ids[i] + " (BODY[HEADER] BODY[TEXT])");
                System.out.println("C: " + "a4 FETCH " + ids[i] + " (BODY[HEADER] BODY[TEXT])");

                Map<String, Object> email = new HashMap<>();
                Map<String, String> header = new HashMap<>();
                Map<String, Map<String, String>> body = new HashMap<>();

                int bodyCount = 0;

                while ((response = reader.readLine()) != null) {

                    if(response.contains("From:")){

                        int i1 = response.indexOf("<") + 1;
                        int i2 = response.indexOf(">");

                        // 메일에 괄호가 포함되어 오는 경우와 아닌 경우 분할 <www.naver.com> 과 www.naver.com 구분
                        if(i1 == 0) header.put("From" , response.substring(6));
                        else header.put("From", response.substring(i1 , i2));

                    } else if (response.contains("To:")) {

                        int i1 = response.indexOf("<") + 1;
                        int i2 = response.indexOf(">");

                        // 메일에 괄호가 포함되어 오는 경우와 아닌 경우 분할 <www.naver.com> 과 www.naver.com 구분
                        if (i1 == 0) header.put("To", response.substring(4));
                        else header.put("From", response.substring(i1 , i2));

                    } else if(response.contains("Subject:")){

                        // 제목이 utf-8 로 인코딩 되어 오는 경우와 인코딩 되지 않는 경우 구분
                        if(response.contains("?utf-8") || response.contains("?UTF-8")){
                            int idx1 = response.lastIndexOf("?");
                            int idx2 = response.indexOf("B") + 2; // 인코딩 되는 경우는 형식이 ?인코딩형식?B?인코딩 내용 -> 이 형식이라 B 찾고 그 이후부터 디코딩해서 저장
                            header.put("Subject" ,new String(Base64.getDecoder().decode(response.substring(idx2 , idx1)) , StandardCharsets.UTF_8));
                        }else{
                            header.put("Subject" , response.substring(9));
                        }
                    } else if(response.contains("Date:")){
                        header.put("Date" , response.substring(6));
                    }

                    if (response.startsWith("Content-Type:") && !response.contains("multipart")) {
                        // content-type 이 multipart 인 경우 본문에서 content-type 을 가져옴, 맨 아래에 multipart 와 아닌것 구분해서 예시에 작성
                        String contentType = response.substring(13).trim();
                        StringBuilder contentBuilder = new StringBuilder();

                        // 본문 읽기
                        while ((response = reader.readLine()) != null
                                && !response.startsWith("--")) {
                            if(!response.contains("charset") && !response.contains("Content") && !response.contains("boundary")){
                                contentBuilder.append(response).append("\n");
                            }
                        }

                        // 본문 정보를 body에 저장
                        Map<String, String> bodyPart = new HashMap<>();
                        bodyPart.put("Content-Type", contentType);
                        bodyPart.put("Content", contentBuilder.toString().trim());
                        body.put(String.valueOf(bodyCount++), bodyPart);
                    }


                    // 명령어에 대한 응답이 완료되었는지 확인 (태그가 "OK" 또는 "NO"로 끝나는 경우)
                    if (response.startsWith("a") && (response.contains("OK") || response.contains("NO") || response.contains("BAD"))) {
                        break;
                    }

                    email.put("header", header);
                    email.put("body", body);
                    emails.put(String.valueOf(i), email);

                }

            }
            System.out.println("emails = " + emails);

            sendCommand(writer, reader, "a5 LOGOUT");

            // 소켓 닫기
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

//From: sender@example.com
//To: receiver@example.com
//Subject: Test Multipart Email
//MIME-Version: 1.0
//Content-Type: multipart/alternative; boundary="boundary123" ---> 여기 부분 content-type 은 안가져오고
//
// --boundary123
//Content-Type: text/plain; charset="UTF-8" -> 여기 부분 content-type 을 가져온 후
//Content-Transfer-Encoding: 7bit
//
//This is the plain text version of the email. --> 내용 가져온다
//
//--boundary123
//Content-Type: text/html; charset="UTF-8"
//Content-Transfer-Encoding: 7bit
//
//        <html>
//<body>
//<p>This is the <b>HTML version</b> of the email.</p>
//</body>
//</html>
//
//        --boundary123--



// 평문인 경우

//From: sender@example.com
//To: receiver@example.com
//Subject: Test Single Part Email
//MIME-Version: 1.0
//Content-Type: text/plain; charset="UTF-8" --> 평문인 경우 바로 저장
//Content-Transfer-Encoding: 7bit
//
//This is a simple text email without any attachments or HTML.
