package pl.edu.agh.dsrg.sr.chat;

import pl.edu.agh.dsrg.sr.chat.ChatConfig;

import java.util.Scanner;

public class ConfigReader {
    public ChatConfig read() throws Exception {
        Scanner scanner = new Scanner(System.in);
        ChatConfig chatConfig = new ChatConfig();

        System.out.print("username: ");
        String user = scanner.next();
        chatConfig.setUsername(user.trim());

        System.out.print("Select chanel: ");

        return chatConfig;
    }
}
