package pl.edu.agh.dsrg.sr.chat;


import com.google.protobuf.InvalidProtocolBufferException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageChannel extends ReceiverAdapter {

    private String channelName;
    private ChatConfig chatConfig;
    private ProtocolStack stack;
    private JChannel channel;
    private Set<String> users;

    public MessageChannel(ChatConfig chatConfig, String channelName) {
        if (chatConfig.getChannels().containsKey(channelName)) {
            return;
        }
        this.channelName = channelName;
        this.chatConfig = chatConfig;
        this.users = new ConcurrentSkipListSet<>();

        chatConfig.getChannels().put(channelName, this);
    }

    public void connect() throws Exception {
        channel = new JChannel(false);
        stack = new ProtocolStack();
        stack.addProtocol(new UDP().setValue("mcast_group_addr", InetAddress.getByName("230.0.0.36")))
                .addProtocol(new PING())
                .addProtocol(new MERGE3())
                .addProtocol(new FD_SOCK())
                .addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
                .addProtocol(new VERIFY_SUSPECT())
                .addProtocol(new BARRIER())
                .addProtocol(new NAKACK2())
                .addProtocol(new UNICAST3())
                .addProtocol(new STABLE())
                .addProtocol(new GMS())
                .addProtocol(new UFC())
                .addProtocol(new MFC())
                .addProtocol(new FRAG2())
                .addProtocol(new STATE_TRANSFER())
                .addProtocol(new FLUSH());
        channel.setProtocolStack(stack);
        stack.init();
        channel.connect(channelName);
        channel.setReceiver(this);
        chatConfig.setCurrentChannel(channelName);
    }

    @Override
    public void receive(Message msg) {
        if (channel == null) {
            System.out.print("ERROR");
            return;
        }
        try {
            ChatOperationProtos.ChatMessage message = ChatOperationProtos.ChatMessage.parseFrom(msg.getBuffer());
            System.out.print(channelName + " - " + message);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }


    public String getChannelName() {
        return channelName;
    }

    public void close() {
        channel.close();
    }

    public void removeUser(String nickname) {
        users.remove(nickname);
    }

    public void addUser(String nickname) {
        users.add(nickname);
    }

    public Set<String> getUsers() {
        return users;
    }

    public void sendMessage(String line) {
        if (channel == null || !channel.isConnected()) {
            System.out.print("Can't use this channel, you are'nt connected");
        }
        byte[] msg = ChatOperationProtos.ChatMessage.newBuilder()
                .setMessage("user: " + chatConfig.getUsername() + "  msg: " + line)
                .build()
                .toByteArray();
        try {
            channel.send(new Message(null, null, msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
