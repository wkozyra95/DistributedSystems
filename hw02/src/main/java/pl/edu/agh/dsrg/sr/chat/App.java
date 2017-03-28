package pl.edu.agh.dsrg.sr.chat;

import org.jgroups.JChannel;


public class App {

    private static ChatConfig chatConfig;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack","true");
        ConfigReader configReader = new ConfigReader();
        chatConfig = configReader.read();

        new UserInterface(chatConfig).blockingRead();

    }
}
