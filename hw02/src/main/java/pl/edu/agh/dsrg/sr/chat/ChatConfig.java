package pl.edu.agh.dsrg.sr.chat;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ChatConfig {
    private String username;
    private ManagementChannel managementChannel;
    private Map<String, MessageChannel> channels;
    private String currentChannel;

    public ChatConfig() throws Exception {
        managementChannel = new ManagementChannel(this);
        channels = new ConcurrentHashMap<>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ManagementChannel getManagementChannel() {
        return managementChannel;
    }

    public void setManagementChannel(ManagementChannel managementChannel) {
        this.managementChannel = managementChannel;
    }

    public Map<String, MessageChannel> getChannels() {
        return channels;
    }

    public void removeChannel(String chName) {
        synchronized (this) {
            if (channels.containsKey(chName) && channels.get(chName).getUsers().contains(username)) {
                channels.get(chName).removeUser(username);
                channels.get(chName).close();

                if (currentChannel == chName) {
                    currentChannel = null;
                }
            }
        }
    }

    public boolean isChannelSelected() {
        return currentChannel != null;
    }


    public MessageChannel getCurrentChannel() {
        return channels.get(currentChannel) != null ? channels.get(currentChannel) : null;
    }

    public void setCurrentChannel(String currentChannel) {
        this.currentChannel = currentChannel;
    }
}
