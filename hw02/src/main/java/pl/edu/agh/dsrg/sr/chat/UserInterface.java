package pl.edu.agh.dsrg.sr.chat;

import java.util.Map;
import java.util.Scanner;

public class UserInterface {
    private final ChatConfig chatConfig;

    public UserInterface(ChatConfig chatConfig) {
        this.chatConfig = chatConfig;
    }

    public void blockingRead() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();
            try {
                synchronized (chatConfig) {
                    handleLine(line);
                }
            } catch (Exception e) {
                System.out.print("ERROR");
            }
        }
    }

    private void handleLine(String line) throws Exception {
        if (line.startsWith("goto ")) {
            String chName = line.replace("goto", "").trim();
            new MessageChannel(chatConfig, chName).connect();
        } else if (line.startsWith("leave ")) {
            String chName = line.replace("goto", "").trim();
            chatConfig.removeChannel(chName);
        } else if (line.startsWith("list")) {
            listChannels();
        } else if (chatConfig.isChannelSelected()) {
            if (chatConfig.getCurrentChannel() == null) {
                System.out.print("You aren't on any channel. Select one and then send messages.");
            }
            chatConfig.getCurrentChannel().sendMessage(line.trim());
        } else {
            System.out.print("You aren't on any channel. Select one and then send messages.");
            System.out.print("ERROR\n");
        }
    }

    private void listChannels() {
        for (Map.Entry<String, MessageChannel> channel: chatConfig.getChannels().entrySet()) {
            System.out.println("CHANNEL: " + channel.getKey());
            for (String user: channel.getValue().getUsers()) {
                System.out.print(user + ", ");
            }
            System.out.println();
        }
    }
}
