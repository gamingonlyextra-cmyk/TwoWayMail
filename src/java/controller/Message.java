package controller;

public class Message {
    private int messageId;
    private int conversationId;
    private int senderId;
    private int recipientId;
    private String messageText;
    private String sentAt;
    private int concernId;

    public Message() {
    }

    public Message(int senderId, int conversationId, String messageText) {
        this.senderId = senderId;
        this.conversationId = conversationId;
        this.messageText = messageText;
    }

    public Message(int senderId, int recipientId, int conversationId, String messageText) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.conversationId = conversationId;
        this.messageText = messageText;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public int getConcernId() {
        return concernId;
    }

    public void setConcernId(int concernId) {
        this.concernId = concernId;
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId=" + messageId +
                ", conversationId=" + conversationId +
                ", senderId=" + senderId +
                ", recipientId=" + recipientId +
                ", messageText='" + messageText + '\'' +
                ", sentAt='" + sentAt + '\'' +
                '}';
    }
}