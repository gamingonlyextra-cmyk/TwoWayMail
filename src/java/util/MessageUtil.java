package util;

import javax.mail.*;
import javax.mail.internet.*;
import java.sql.*;
import java.util.*;

public class MessageUtil {
    
    // Email configuration
    private static final String FROM_EMAIL = "gonzagafernando077@gmail.com";
    private static final String FROM_PASSWORD = "njomfunsxxdwmwzo";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    
    /**
     * Save a message to database
     * Maps to MESSAGES table: MESSAGE_ID, CONCERN_ID, SENDER_ID, MESSAGE_TEXT, SENT_AT
     */
    public static boolean saveMessage(int senderId, int concernId, String messageText) {
        try (Connection conn = DBConnection.getConnection()) {
            
            System.out.println("DEBUG: Saving message from " + senderId + " to concern " + concernId);
            
            // Insert message
            String insertMsgSql = "INSERT INTO messages (message_id, concern_id, sender_id, message_text, sent_at) " +
                                 "VALUES (messages_seq.NEXTVAL, ?, ?, ?, SYSDATE)";
            PreparedStatement ps = conn.prepareStatement(insertMsgSql);
            ps.setInt(1, concernId);
            ps.setInt(2, senderId);
            ps.setString(3, messageText);
            ps.executeUpdate();
            
            System.out.println("DEBUG: Message inserted into database");
            return true;
            
        } catch (SQLException e) {
            System.err.println("ERROR saving message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Send email notification
     */
    public static boolean sendMessageNotificationEmail(String recipientEmail, String senderName, String messagePreview) {
        try {
            String subject = "New Message from " + senderName + " - SCMS";
            
            // Limit preview to first 100 characters
            String preview = messagePreview.length() > 100 ? 
                           messagePreview.substring(0, 100) + "..." : 
                           messagePreview;
            
            String htmlBody = "<html>" +
                    "<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>" +
                    "<div style='background-color: white; padding: 30px; border-radius: 8px; max-width: 500px; margin: 0 auto;'>" +
                    "<h2 style='color: #333;'>New Message Received</h2>" +
                    "<p style='color: #666;'><strong>From:</strong> " + senderName + "</p>" +
                    "<div style='background-color: #f9f9f9; padding: 15px; border-left: 4px solid #007bff; margin: 20px 0;'>" +
                    "<p style='color: #333; margin: 0;'>" + preview + "</p>" +
                    "</div>" +
                    "<p style='color: #666;'><a href='messages.jsp' style='color: #007bff; text-decoration: none; font-weight: bold;'>View Full Message</a></p>" +
                    "<hr style='border: none; border-top: 1px solid #ddd; margin-top: 30px;'/>" +
                    "<p style='color: #999; font-size: 12px;'>SCMS System</p>" +
                    "</div>" +
                    "</body>" +
                    "</html>";
            
            return sendHtmlEmail(recipientEmail, subject, htmlBody);
            
        } catch (Exception e) {
            System.err.println("ERROR sending message notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Helper method to send HTML email
     */
    private static boolean sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");
            
            Transport.send(message);
            System.out.println("Message notification email sent to: " + toEmail);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("ERROR Failed to send message notification email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get all conversations for a user
     * CONVERSATIONS table has: CONVERSATION_ID, USER1_ID, USER2_ID, LAST_MESSAGE_AT
     */
    public static List<Map<String, Object>> getConversations(int userId) {
        List<Map<String, Object>> conversations = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection()) {
            
            System.out.println("DEBUG: Getting conversations for user: " + userId);
            
            // Get all conversations where user is involved
            String sql = "SELECT c.conversation_id, " +
                        "CASE WHEN c.user1_id = ? THEN c.user2_id ELSE c.user1_id END as other_user_id, " +
                        "u.full_name, u.email, c.last_message_at, " +
                        "(SELECT m.message_text FROM messages m WHERE m.sender_id = ? ORDER BY m.sent_at DESC FETCH FIRST 1 ROWS ONLY) as last_message " +
                        "FROM conversations c " +
                        "JOIN users u ON (c.user1_id = ? AND u.user_id = c.user2_id) OR (c.user2_id = ? AND u.user_id = c.user1_id) " +
                        "WHERE c.user1_id = ? OR c.user2_id = ? " +
                        "ORDER BY c.last_message_at DESC NULLS LAST";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ps.setInt(4, userId);
            ps.setInt(5, userId);
            ps.setInt(6, userId);
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> conv = new HashMap<>();
                conv.put("conversationId", rs.getInt("conversation_id"));
                conv.put("otherUserId", rs.getInt("other_user_id"));
                conv.put("fullName", rs.getString("full_name"));
                conv.put("email", rs.getString("email"));
                conv.put("lastMessageAt", rs.getTimestamp("last_message_at"));
                conv.put("lastMessage", rs.getString("last_message"));
                conversations.add(conv);
                System.out.println("DEBUG: Found conversation with " + rs.getString("full_name"));
            }
            
            System.out.println("DEBUG: Total conversations found: " + conversations.size());
            
        } catch (SQLException e) {
            System.err.println("ERROR fetching conversations: " + e.getMessage());
            e.printStackTrace();
        }
        
        return conversations;
    }
    
    /**
     * Get message history between two users
     * Uses MESSAGES table: MESSAGE_ID, CONCERN_ID, SENDER_ID, MESSAGE_TEXT, SENT_AT
     */
    public static List<Map<String, Object>> getMessageHistory(int userId1, int userId2) {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection()) {
            
            System.out.println("DEBUG: Getting message history between " + userId1 + " and " + userId2);
            
            String sql = "SELECT m.message_id, m.concern_id, m.sender_id, m.message_text, m.sent_at, " +
                        "u.full_name, u.email " +
                        "FROM messages m " +
                        "JOIN users u ON m.sender_id = u.user_id " +
                        "WHERE m.sender_id = ? OR m.sender_id = ? " +
                        "ORDER BY m.sent_at ASC";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("messageId", rs.getInt("message_id"));
                msg.put("concernId", rs.getInt("concern_id"));
                msg.put("senderId", rs.getInt("sender_id"));
                msg.put("messageText", rs.getString("message_text"));
                msg.put("sentAt", rs.getTimestamp("sent_at"));
                msg.put("senderName", rs.getString("full_name"));
                msg.put("senderEmail", rs.getString("email"));
                messages.add(msg);
            }
            
            System.out.println("DEBUG: Found " + messages.size() + " messages");
            
        } catch (SQLException e) {
            System.err.println("ERROR fetching message history: " + e.getMessage());
            e.printStackTrace();
        }
        
        return messages;
    }
    
    /**
     * Get user info by email - Only returns students (role_id = 1)
     */
    public static Map<String, Object> getUserByEmail(String email) {
        try (Connection conn = DBConnection.getConnection()) {
            
            System.out.println("DEBUG: getUserByEmail - Looking for: " + email);
            
            // Use LOWER for case-insensitive search
            String sql = "SELECT user_id, full_name, email, role_id FROM users WHERE LOWER(email) = LOWER(?) AND role_id = 1";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int userId = rs.getInt("user_id");
                String fullName = rs.getString("full_name");
                String dbEmail = rs.getString("email");
                int roleId = rs.getInt("role_id");
                
                System.out.println("DEBUG: User found - ID: " + userId + ", Name: " + fullName + 
                                 ", Email: " + dbEmail + ", Role: " + roleId);
                
                Map<String, Object> user = new HashMap<>();
                user.put("userId", userId);
                user.put("fullName", fullName);
                user.put("email", dbEmail);
                return user;
            }
            
            System.out.println("DEBUG: No student user found with email: " + email);
            
        } catch (SQLException e) {
            System.err.println("ERROR fetching user by email: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Create or get conversation between two users
     * CONVERSATIONS table: CONVERSATION_ID, USER1_ID, USER2_ID, LAST_MESSAGE_AT
     */
    public static boolean createOrGetConversation(int userId1, int userId2) {
        try (Connection conn = DBConnection.getConnection()) {
            
            // Ensure userId1 < userId2 for consistency
            int u1 = Math.min(userId1, userId2);
            int u2 = Math.max(userId1, userId2);
            
            System.out.println("DEBUG: Creating/Getting conversation between " + u1 + " and " + u2);
            
            // Check if conversation exists
            String checkSql = "SELECT conversation_id FROM conversations WHERE user1_id = ? AND user2_id = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, u1);
            checkPs.setInt(2, u2);
            
            ResultSet rs = checkPs.executeQuery();
            if (rs.next()) {
                System.out.println("DEBUG: Conversation already exists");
                return true;
            }
            
            // Create new conversation
            String insertSql = "INSERT INTO conversations (conversation_id, user1_id, user2_id, last_message_at) " +
                             "VALUES (conversations_seq.NEXTVAL, ?, ?, NULL)";
            PreparedStatement insertPs = conn.prepareStatement(insertSql);
            insertPs.setInt(1, u1);
            insertPs.setInt(2, u2);
            insertPs.executeUpdate();
            
            System.out.println("DEBUG: New conversation created");
            return true;
            
        } catch (SQLException e) {
            System.err.println("ERROR creating conversation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete all messages and conversations for a user (called on account deletion)
     */
    public static boolean deleteUserMessages(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            
            System.out.println("DEBUG: Deleting all messages for user: " + userId);
            
            // Delete messages where user is sender
            String deleteMsgSql = "DELETE FROM messages WHERE sender_id = ?";
            PreparedStatement deleteMsgPs = conn.prepareStatement(deleteMsgSql);
            deleteMsgPs.setInt(1, userId);
            int msgDeleted = deleteMsgPs.executeUpdate();
            
            System.out.println("DEBUG: Deleted " + msgDeleted + " messages");
            
            // Delete conversations where user is participant
            String deleteConvSql = "DELETE FROM conversations WHERE user1_id = ? OR user2_id = ?";
            PreparedStatement deleteConvPs = conn.prepareStatement(deleteConvSql);
            deleteConvPs.setInt(1, userId);
            deleteConvPs.setInt(2, userId);
            int convDeleted = deleteConvPs.executeUpdate();
            
            System.out.println("DEBUG: Deleted " + convDeleted + " conversations");
            return true;
            
        } catch (SQLException e) {
            System.err.println("ERROR deleting user messages: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Update last message timestamp in conversation
     */
    public static boolean updateConversationTimestamp(int userId1, int userId2) {
        try (Connection conn = DBConnection.getConnection()) {
            
            int u1 = Math.min(userId1, userId2);
            int u2 = Math.max(userId1, userId2);
            
            System.out.println("DEBUG: Updating conversation timestamp for " + u1 + " and " + u2);
            
            String sql = "UPDATE conversations SET last_message_at = SYSDATE WHERE user1_id = ? AND user2_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, u1);
            ps.setInt(2, u2);
            ps.executeUpdate();
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("ERROR updating conversation timestamp: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}