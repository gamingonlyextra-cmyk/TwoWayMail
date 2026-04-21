package controller;

import controller.Message;
import java.sql.*;
import java.util.*;

/**
 * Data Access Object for messaging operations
 * Works with Oracle Database tables: CONVERSATIONS, MESSAGES, USERS
 */
public class MessageDAO {
    
    /**
     * Get all conversations for a user from CONVERSATIONS table
     */
    public List<Map<String, Object>> getConversations(int userId) {
        List<Map<String, Object>> conversations = new ArrayList<>();
        
        try (Connection conn = util.DBConnection.getConnection()) {
            
            System.out.println("DEBUG: MessageDAO.getConversations for user: " + userId);
            
            // Query conversations table
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
                
                Timestamp lastMsgAt = rs.getTimestamp("last_message_at");
                conv.put("lastMessageAt", lastMsgAt != null ? lastMsgAt.toString() : "");
                conv.put("lastMessage", rs.getString("last_message"));
                conversations.add(conv);
                
                System.out.println("DEBUG: Found conversation with user " + rs.getInt("other_user_id"));
            }
            
            System.out.println("DEBUG: MessageDAO found " + conversations.size() + " conversations");
            
        } catch (SQLException e) {
            System.err.println("ERROR in MessageDAO.getConversations: " + e.getMessage());
            e.printStackTrace();
        }
        
        return conversations;
    }
    
    /**
     * Get chat history between two users
     * Returns all messages where either user is the sender
     */
    public List<Message> getChatHistory(int userId1, int userId2) {
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = util.DBConnection.getConnection()) {
            
            System.out.println("DEBUG: MessageDAO.getChatHistory between " + userId1 + " and " + userId2);
            
            // Get ALL messages from both users (not filtered by concern)
            // Messages are sent TO a concern, not to a specific user
            String sql = "SELECT message_id, concern_id, sender_id, message_text, sent_at " +
                        "FROM messages " +
                        "WHERE sender_id = ? OR sender_id = ? " +
                        "ORDER BY sent_at ASC";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            
            System.out.println("DEBUG: Executing query for users: " + userId1 + ", " + userId2);
            
            ResultSet rs = ps.executeQuery();
            int count = 0;
            
            while (rs.next()) {
                Message msg = new Message();
                msg.setMessageId(rs.getInt("message_id"));
                msg.setConcernId(rs.getInt("concern_id"));
                msg.setSenderId(rs.getInt("sender_id"));
                msg.setMessageText(rs.getString("message_text"));
                
                Timestamp sentAt = rs.getTimestamp("sent_at");
                msg.setSentAt(sentAt != null ? sentAt.toString() : "");
                
                messages.add(msg);
                count++;
                
                System.out.println("DEBUG: Message " + count + " - Sender: " + msg.getSenderId() + ", Text: " + msg.getMessageText().substring(0, Math.min(30, msg.getMessageText().length())));
            }
            
            System.out.println("DEBUG: MessageDAO found " + count + " total messages");
            
        } catch (SQLException e) {
            System.err.println("ERROR in MessageDAO.getChatHistory: " + e.getMessage());
            e.printStackTrace();
        }
        
        return messages;
    }
    
    /**
     * Save a message to database
     */
    public boolean saveMessage(int senderId, int concernId, String messageText) {
        try (Connection conn = util.DBConnection.getConnection()) {
            
            System.out.println("DEBUG: MessageDAO.saveMessage from " + senderId + " to concern " + concernId);
            System.out.println("DEBUG: Message text: " + messageText);
            
            String sql = "INSERT INTO messages (message_id, concern_id, sender_id, message_text, sent_at) " +
                        "VALUES (messages_seq.NEXTVAL, ?, ?, ?, SYSDATE)";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, concernId);
            ps.setInt(2, senderId);
            ps.setString(3, messageText);
            
            int rows = ps.executeUpdate();
            System.out.println("DEBUG: MessageDAO inserted " + rows + " row(s)");
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("ERROR in MessageDAO.saveMessage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get user by email (only returns students with role_id = 1)
     */
    public Map<String, Object> getUserByEmail(String email) {
        try (Connection conn = util.DBConnection.getConnection()) {
            
            System.out.println("DEBUG: MessageDAO.getUserByEmail - Looking for: " + email);
            
            String sql = "SELECT user_id, full_name, email FROM users WHERE LOWER(email) = LOWER(?) AND role_id = 1";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("user_id"));
                user.put("fullName", rs.getString("full_name"));
                user.put("email", rs.getString("email"));
                
                System.out.println("DEBUG: MessageDAO found user: " + rs.getString("full_name"));
                return user;
            }
            
            System.out.println("DEBUG: MessageDAO found no user with email: " + email);
            
        } catch (SQLException e) {
            System.err.println("ERROR in MessageDAO.getUserByEmail: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Create or get conversation between two users
     */
    public boolean createOrGetConversation(int userId1, int userId2) {
        try (Connection conn = util.DBConnection.getConnection()) {
            
            int u1 = Math.min(userId1, userId2);
            int u2 = Math.max(userId1, userId2);
            
            System.out.println("DEBUG: MessageDAO.createOrGetConversation between " + u1 + " and " + u2);
            
            // Check if exists
            String checkSql = "SELECT conversation_id FROM conversations WHERE user1_id = ? AND user2_id = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setInt(1, u1);
            checkPs.setInt(2, u2);
            
            ResultSet rs = checkPs.executeQuery();
            if (rs.next()) {
                System.out.println("DEBUG: Conversation already exists with ID: " + rs.getInt("conversation_id"));
                return true;
            }
            
            // Create new
            String insertSql = "INSERT INTO conversations (conversation_id, user1_id, user2_id, last_message_at) " +
                             "VALUES (conversations_seq.NEXTVAL, ?, ?, NULL)";
            PreparedStatement insertPs = conn.prepareStatement(insertSql);
            insertPs.setInt(1, u1);
            insertPs.setInt(2, u2);
            insertPs.executeUpdate();
            
            System.out.println("DEBUG: New conversation created");
            return true;
            
        } catch (SQLException e) {
            System.err.println("ERROR in MessageDAO.createOrGetConversation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}