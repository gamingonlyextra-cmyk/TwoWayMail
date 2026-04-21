-- This SQL script creates the tables for the messaging system in TwoWayMail without duplicate constraints.

CREATE TABLE Users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE Messages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    content TEXT NOT NULL,
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES Users(id),
    FOREIGN KEY (receiver_id) REFERENCES Users(id)
);

CREATE TABLE Conversations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user1_id INT NOT NULL,
    user2_id INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user1_id) REFERENCES Users(id),
    FOREIGN KEY (user2_id) REFERENCES Users(id)
);

-- Ensure to add indexes to optimize performance without enforcing unique constraints if needed.

CREATE INDEX idx_sender ON Messages(sender_id);
CREATE INDEX idx_receiver ON Messages(receiver_id);
CREATE INDEX idx_user1 ON Conversations(user1_id);
CREATE INDEX idx_user2 ON Conversations(user2_id);
