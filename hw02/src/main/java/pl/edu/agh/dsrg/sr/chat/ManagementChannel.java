package pl.edu.agh.dsrg.sr.chat;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos;

import java.io.*;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ManagementChannel extends ReceiverAdapter {
    private static final String CHANNEL_NAME = "ChatManagement321321";
    private final ProtocolStack stack;
    private final JChannel channel;
    private final ChatConfig chatConfig;

    public ManagementChannel(ChatConfig chatConfig) throws Exception {
        this.chatConfig = chatConfig;
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
        channel.setReceiver(this);
        channel.connect(CHANNEL_NAME);
        channel.getState(null, 10000);
    }

    @Override
    public void receive(Message msg) {
        try {
            ChatOperationProtos.ChatAction action = ChatOperationProtos.ChatAction.parseFrom(msg.getBuffer());
            if (chatConfig.getChannels().containsKey(action.getChannel()) && action.getAction() == ChatOperationProtos.ChatAction.ActionType.LEAVE) {
                chatConfig.getChannels().get(action.getChannel()).removeUser(action.getNickname());
            } else if (!chatConfig.getChannels().containsKey(action.getChannel()) && action.getAction() == ChatOperationProtos.ChatAction.ActionType.JOIN) {
                new MessageChannel(chatConfig, action.getChannel()).addUser(action.getNickname());
            } else if (chatConfig.getChannels().containsKey(action.getAction()) && action.getAction() == ChatOperationProtos.ChatAction.ActionType.JOIN) {
                chatConfig.getChannels().get(action.getChannel()).addUser(action.getNickname());
            }

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void getState(OutputStream output) throws Exception {
        output.write(createState());
        output.close();
    }

    private byte[] createState() {
        ChatOperationProtos.ChatState.Builder builder = ChatOperationProtos.ChatState.newBuilder();
        synchronized (chatConfig) {
            for (Map.Entry<String, MessageChannel> ch : chatConfig.getChannels().entrySet()) {
                for (String user : ch.getValue().getUsers()) {
                    ChatOperationProtos.ChatAction action = ChatOperationProtos.ChatAction.newBuilder()
                            .setNickname(user)
                            .setAction(ChatOperationProtos.ChatAction.ActionType.JOIN)
                            .setChannel(ch.getValue().getChannelName())
                            .build();

                    builder.addState(action);
                }
            }
        }

        return builder.build().toByteArray();
    }

    @Override
    public void setState(InputStream input) {
        try {
            readState(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readState(InputStream inputByte) throws IOException {
        synchronized (chatConfig) {
            List<ChatOperationProtos.ChatAction> list = ChatOperationProtos.ChatState.parseFrom(inputByte).getStateList();
            for (ChatOperationProtos.ChatAction action : list) {
                new MessageChannel(chatConfig, action.getChannel());
                chatConfig.getChannels().get(action.getChannel()).addUser(action.getNickname());
            }
        }
    }
}
