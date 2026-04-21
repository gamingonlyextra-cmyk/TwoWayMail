package controller;

import com.google.gson.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.*;
import java.util.*;

@WebServlet("/MessagesServlet")
public class MessagesServlet extends HttpServlet {

    private MessageDAO messageDAO;

    @Override
    public void init() throws ServletException {
        messageDAO = new MessageDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        Integer userId = (Integer) request.getSession().getAttribute("user_id");
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }

        String action = request.getParameter("action");
        System.out.println("DEBUG: GET request from user " + userId);
        System.out.println("DEBUG: Action = " + action);

        try {
            if ("getConversations".equals(action)) {
                System.out.println("DEBUG: Getting conversations for user " + userId);
                List<Map<String, Object>> conversations = messageDAO.getConversations(userId);
                JsonArray arr = new JsonArray();
                
                for (Map<String, Object> conv : conversations) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("conversationId", (Integer) conv.get("conversationId"));
                    obj.addProperty("otherUserId", (Integer) conv.get("otherUserId"));
                    obj.addProperty("fullName", (String) conv.get("fullName"));
                    obj.addProperty("email", (String) conv.get("email"));
                    obj.addProperty("lastMessage", (String) conv.get("lastMessage"));
                    obj.addProperty("lastMessageAt", conv.get("lastMessageAt") != null ? conv.get("lastMessageAt").toString() : "");
                    arr.add(obj);
                }
                
                out.print(arr.toString());

            } else if ("getChatHistory".equals(action)) {
                String otherUserIdParam = request.getParameter("otherUserId");
                System.out.println("DEBUG: Getting chat history with otherUserId = " + otherUserIdParam);
                
                if (otherUserIdParam == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Missing otherUserId\"}");
                    return;
                }
                
                int otherUserId = Integer.parseInt(otherUserIdParam);
                List<Message> messages = messageDAO.getChatHistory(userId, otherUserId);
                JsonArray arr = new JsonArray();
                
                for (Message msg : messages) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("messageId", msg.getMessageId());
                    obj.addProperty("senderId", msg.getSenderId());
                    obj.addProperty("messageText", msg.getMessageText());
                    obj.addProperty("sentAt", msg.getSentAt());
                    arr.add(obj);
                }
                
                out.print(arr.toString());

            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Invalid action\"}");
            }

        } catch (Exception e) {
            System.err.println("ERROR in doGet: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Server error\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        Integer userId = (Integer) request.getSession().getAttribute("user_id");
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"status\":\"error\",\"message\":\"Not logged in\"}");
            return;
        }

        String action = request.getParameter("action");
        System.out.println("DEBUG: POST request from user " + userId);
        System.out.println("DEBUG: Action parameter = " + action);
        
        // Log all parameters
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            System.out.println("  Parameter: " + paramName + " = " + request.getParameter(paramName));
        }

        try {
            if ("sendMessage".equals(action)) {
                String otherUserIdParam = request.getParameter("otherUserId");
                String messageText = request.getParameter("messageText");

                System.out.println("DEBUG: sendMessage - otherUserId=" + otherUserIdParam);

                if (otherUserIdParam == null || messageText == null || messageText.trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"status\":\"error\",\"message\":\"Missing parameters\"}");
                    return;
                }

                int otherUserId = Integer.parseInt(otherUserIdParam);
                boolean saved = messageDAO.saveMessage(userId, 0, messageText.trim());

                JsonObject resp = new JsonObject();
                resp.addProperty("status", saved ? "success" : "error");
                out.print(resp.toString());

            } else if ("startNewChat".equals(action)) {
                String recipientEmail = request.getParameter("recipientEmail");

                System.out.println("DEBUG: startNewChat - recipientEmail=" + recipientEmail);

                if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"status\":\"error\",\"message\":\"Missing email\"}");
                    return;
                }

                Map<String, Object> recipient = messageDAO.getUserByEmail(recipientEmail.trim());

                JsonObject resp = new JsonObject();
                if (recipient == null) {
                    resp.addProperty("status", "error");
                    resp.addProperty("message", "User not found");
                } else {
                    int recipientId = (int) recipient.get("id");
                    if (recipientId == userId) {
                        resp.addProperty("status", "error");
                        resp.addProperty("message", "Cannot chat with yourself");
                    } else {
                        messageDAO.createOrGetConversation(userId, recipientId);
                        resp.addProperty("status", "success");
                        resp.addProperty("recipientId", recipientId);
                        resp.addProperty("recipientName", (String) recipient.get("fullName"));
                    }
                }
                out.print(resp.toString());

            } else {
                System.out.println("DEBUG: Invalid action: " + action);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid action: " + action + "\"}");
            }

        } catch (Exception e) {
            System.err.println("ERROR in doPost: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\",\"message\":\"Server error\"}");
        }
    }
}