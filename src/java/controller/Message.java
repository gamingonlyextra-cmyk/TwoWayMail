package controller;

public class Message {
    private int messageId;
    private int concernId;
    private int senderId;
    private String messageText;
    private String sentAt;
    
    // Constructors
    public Message() {
    }
    
    public Message(int senderId, int concernId, String messageText) {
        this.senderId = senderId;
        this.concernId = concernId;
        this.messageText = messageText;
    }
    
    // Getters and Setters
    public int getMessageId() {
        return messageId;
    }
    
    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }
    
    public int getConcernId() {
        return concernId;
    }
    
    public void setConcernId(int concernId) {
        this.concernId = concernId;
    }
    
    public int getSenderId() {
        return senderId;
    }
    
    public void setSenderId(int senderId) {
        this.senderId = senderId;
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
}