package smtp;

// SMTPClient UI

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

public class SmtpClientUI {
    // GUI 구성 요소와 이메일 관련 데이터
    private JTextField fromEmailField;     // 보내는 사람 이메일 입력 필드
    private JTextField userNameField;        // 보내는 사람 계정 이름 입력 필드
    private JPasswordField passwordField;    // 비밀번호 입력 필드
    private JTextField toEmailFiled;       // 수신자 이메일 입력 필드
    private JTextField subjectField;         // 이메일 제목 입력 필드
    private JTextArea bodyArea;           // 이메일 본문 입력 필드
    private ArrayList<File> attachments;     // 첨부 파일 리스트
    JFrame frame;
    JPanel inputPanel;

    // 생성자: GUI 설정 및 초기화
    public SmtpClientUI() {
        frame = new JFrame("Email Sender"); // 메인 프레임 생성
        frame.setSize(500, 600); // 프레임 크기 설정
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 종료 시 프로그램 종료
        frame.setLayout(new BorderLayout(10, 10)); // 레이아웃 설정

        inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(); // 레이아웃 제어를 위한 설정
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5); // 여백 설정

        // 보내는 사람 이메일 입력 라벨 및 필드
        JLabel fromEmailLabel = new JLabel("From Email:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(fromEmailLabel, gbc);

        // 보내는 사람 이메일을 입력할 텍스트 필드
        fromEmailField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(fromEmailField, gbc);

        // 계정 이름 입력 라벨 및 필드
        JLabel userNameLabel = new JLabel("User Name:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(userNameLabel, gbc);

        // 계정 이름을 입력할 텍스트 필드
        userNameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(userNameField, gbc);

        // 비밀번호 입력 라벨 및 필드
        JLabel passwordLabel = new JLabel("Password:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(passwordLabel, gbc);

        // 비밀번호를 입력할 텍스트 필드
        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        inputPanel.add(passwordField, gbc);

        // 수신자 이메일 입력 라벨 및 필드
        JLabel toEmailLabel = new JLabel("To Emails:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(toEmailLabel, gbc);

        // 수신자 이메일을 입력할 텍스트 필드
        toEmailFiled = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 3;
        inputPanel.add(toEmailFiled, gbc);

        // 제목 입력 라벨 및 필드
        JLabel subjectLabel = new JLabel("Subject:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        inputPanel.add(subjectLabel, gbc);

        // 이메일 제목을 입력할 텍스트 필드
        subjectField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 4;
        inputPanel.add(subjectField, gbc);

        // 본문 입력 라벨 및 텍스트 영역
        JLabel bodyLabel = new JLabel("Message:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        inputPanel.add(bodyLabel, gbc);

        // 이메일 본문을 입력할 텍스트 영역
        bodyArea = new JTextArea(10, 20);
        bodyArea.setLineWrap(true); // 줄 바꿈 설정
        bodyArea.setWrapStyleWord(true);

        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        JScrollPane bodyScrollPane = new JScrollPane(bodyArea);
        inputPanel.add(bodyScrollPane, gbc);

        // 레이아웃 초기화
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 첨부 파일 패널 및 설정
        OutlineButton attachButton = new OutlineButton(new Color(0, 175, 115), Color.WHITE);
        attachButton.setText("Attach Files");
        attachButton.setPreferredSize(new Dimension(120, 30));
        attachButton.setFont(new Font("Arial", Font.BOLD, 10));

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        inputPanel.add(attachButton, gbc);

        attachButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser(); // 파일 선택 다이얼로그
                fileChooser.setMultiSelectionEnabled(true); // 다중 파일 선택 가능

                // 파일 선택 다이얼로그를 열고 사용자가 파일을 선택하면 파일을 가져오는 작업
                if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {

                    // 사용자가 선택한 파일들을 가져옴
                    File[] selectedFiles = fileChooser.getSelectedFiles();

                    // 선택된 각 파일에 대해 파일 패널 생성
                    for (File selectedFile : selectedFiles) {
                        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                        JLabel fileLabel = new JLabel(selectedFile.getName());
                        fileLabel.putClientProperty("file", selectedFile);

                        // 삭제 버튼 생성 및 설정
                        OutlineButton removeButton = new OutlineButton(new Color(0, 175, 115), Color.WHITE);
                        removeButton.setText("X");
                        removeButton.setPreferredSize(new Dimension(20, 20));
                        removeButton.setBorder(BorderFactory.createLineBorder(new Color(0, 175, 115), 2));
                        removeButton.setFont(new Font("Arial", Font.BOLD, 10));
                        removeButton.setOpaque(true);

                        // 삭제 버튼 클릭 시 파일 패널 제거
                        removeButton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                inputPanel.remove(filePanel);
                                inputPanel.revalidate();
                                inputPanel.repaint();
                            }
                        });

                        // 파일 패널에 파일명 라벨과 삭제 버튼 추가
                        filePanel.add(fileLabel);
                        filePanel.add(removeButton);
                        gbc.gridx = 0;
                        gbc.gridy = GridBagConstraints.RELATIVE;
                        gbc.gridwidth = 2;
                        inputPanel.add(filePanel, gbc);
                    }

                    inputPanel.revalidate();
                    inputPanel.repaint();
                }
            }
        });

        // 이메일 전송 버튼
        JButton sendButton = new JButton("Send");
        sendButton.setFocusPainted(false);
        sendButton.setBackground(new Color(0, 175, 115));
        sendButton.setForeground(Color.WHITE);
        sendButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        sendButton.setOpaque(true);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendEmailInfo(); // 이메일 전송 메서드 호출
            }
        });

        frame.add(inputPanel, BorderLayout.CENTER);
        frame.add(sendButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    // 이메일 즉시 전송 메서드
    private void sendEmailInfo() {
        // SMTP 서버 및 포트 설정
        String smtpServer = "smtp.naver.com";
        int port = 587;

        // UI에서 보내는 사람 정보 및 로그인 정보 가져오기
        String fromEmail = fromEmailField.getText();
        String username = userNameField.getText();
        String password = new String(passwordField.getPassword());

        // UI에서 수신자, 제목, 메시지 내용 가져오기
        String[] toEmails = toEmailFiled.getText().split("\\s*,\\s*"); // ','로 분리하여 배열로 변환
        String subject = subjectField.getText();
        String body = bodyArea.getText();

        // UI에서 첨부파일들 가져오기
        attachments = new ArrayList<>();
        for (Component component : inputPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                for (Component innerComponent : panel.getComponents()) {
                    if (innerComponent instanceof JLabel) {
                        JLabel label = (JLabel) innerComponent;
                        attachments.add((File) label.getClientProperty("file"));
                    }
                }
            }
        }

        // SmtpClient 클래스의 sendEmail 메서드를 호출하여 이메일을 전송
        try {
            SmtpClient.sendEmail(smtpServer, port, fromEmail, toEmails, username, password, subject, body, attachments);
            JOptionPane.showMessageDialog(frame, "Email sent successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error sending email: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}