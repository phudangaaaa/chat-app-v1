-- Reset Database - Clean slate for chat application
-- WARNING: This will DELETE ALL DATA and recreate the database

-- Drop existing database if exists
DROP DATABASE IF EXISTS chat_app;

-- Create fresh database
CREATE DATABASE chat_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chat_app;

-- ============================================
-- TABLE: users
-- ============================================
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    status_message VARCHAR(255) DEFAULT '',
    user_status ENUM('ONLINE', 'OFFLINE', 'AWAY', 'BUSY') DEFAULT 'OFFLINE',
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_user_status (user_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: friend_requests
-- ============================================
CREATE TABLE friend_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    request_status ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_request (sender_id, receiver_id),
    INDEX idx_receiver_status (receiver_id, request_status),
    INDEX idx_sender (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: friends
-- ============================================
CREATE TABLE friends (
    friendship_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_friendship (user_id, friend_id),
    INDEX idx_user_friends (user_id),
    INDEX idx_friend (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: chat_groups
-- ============================================
CREATE TABLE chat_groups (
    group_id INT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(100) NOT NULL,
    group_description VARCHAR(500),
    creator_id INT NOT NULL,
    group_avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_creator (creator_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: group_members
-- ============================================
CREATE TABLE group_members (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,
    user_id INT NOT NULL,
    member_role ENUM('ADMIN', 'MEMBER') DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES chat_groups(group_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_group_member (group_id, user_id),
    INDEX idx_group_members (group_id),
    INDEX idx_user_groups (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: messages (FIXED FOR GROUP CHAT)
-- ============================================
CREATE TABLE messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NULL,  -- NULL for group messages
    group_id INT NULL,     -- NULL for private messages
    message_type ENUM('TEXT', 'IMAGE', 'FILE', 'VIDEO', 'AUDIO') DEFAULT 'TEXT',
    message_content TEXT NOT NULL,
    file_url VARCHAR(500),
    file_name VARCHAR(255),
    file_size BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES chat_groups(group_id) ON DELETE CASCADE,
    INDEX idx_private_messages (sender_id, receiver_id, sent_at),
    INDEX idx_group_messages (group_id, sent_at DESC),
    INDEX idx_sender_group (sender_id, group_id),
    INDEX idx_sent_at (sent_at),
    CHECK ((receiver_id IS NOT NULL AND group_id IS NULL) OR
           (receiver_id IS NULL AND group_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: calls
-- ============================================
CREATE TABLE calls (
    call_id INT AUTO_INCREMENT PRIMARY KEY,
    caller_id INT NOT NULL,
    receiver_id INT NOT NULL,
    call_type ENUM('VOICE', 'VIDEO') NOT NULL,
    call_status ENUM('RINGING', 'ACCEPTED', 'REJECTED', 'ENDED', 'MISSED') DEFAULT 'RINGING',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    duration INT DEFAULT 0,
    FOREIGN KEY (caller_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_caller (caller_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_call_status (call_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TABLE: sessions
-- ============================================
CREATE TABLE sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id INT NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_sessions (user_id),
    INDEX idx_active_sessions (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- INSERT TEST DATA
-- ============================================

-- Insert users (password for all: admin123)
INSERT INTO users (username, email, password_hash, full_name, user_status) VALUES
('admin', 'admin@chatapp.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Administrator', 'ONLINE'),
('alice', 'alice@chatapp.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alice Johnson', 'ONLINE'),
('bob', 'bob@chatapp.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bob Smith', 'OFFLINE'),
('charlie', 'charlie@chatapp.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Charlie Brown', 'AWAY');

-- Make them friends with each other
INSERT INTO friends (user_id, friend_id) VALUES
(1, 2), (2, 1),  -- admin <-> alice
(1, 3), (3, 1),  -- admin <-> bob
(1, 4), (4, 1),  -- admin <-> charlie
(2, 3), (3, 2),  -- alice <-> bob
(2, 4), (4, 2);  -- alice <-> charlie

-- Create test groups
INSERT INTO chat_groups (group_name, group_description, creator_id) VALUES
('Team Project', 'Project discussion group', 1),
('Friends Hangout', 'Just friends chatting', 2);

-- Add members to groups
-- Group 1: Team Project (admin, alice, bob)
INSERT INTO group_members (group_id, user_id, member_role) VALUES
(1, 1, 'ADMIN'),
(1, 2, 'MEMBER'),
(1, 3, 'MEMBER');

-- Group 2: Friends Hangout (alice, bob, charlie)
INSERT INTO group_members (group_id, user_id, member_role) VALUES
(2, 2, 'ADMIN'),
(2, 3, 'MEMBER'),
(2, 4, 'MEMBER');

-- Insert test group messages
INSERT INTO messages (sender_id, group_id, message_type, message_content) VALUES
-- Team Project messages
(1, 1, 'TEXT', 'Welcome to the Team Project group! 👋'),
(2, 1, 'TEXT', 'Hi everyone! Excited to work together!'),
(3, 1, 'TEXT', 'Hello team! What are we working on?'),
(1, 1, 'TEXT', 'We need to finish the chat application features'),
(2, 1, 'TEXT', 'I can help with the frontend'),
(3, 1, 'TEXT', 'I will work on the backend'),

-- Friends Hangout messages
(2, 2, 'TEXT', 'Hey friends! Welcome to our hangout 🎉'),
(3, 2, 'TEXT', 'Thanks for creating this, Alice!'),
(4, 2, 'TEXT', 'Nice to have a place to chat together'),
(2, 2, 'TEXT', 'Who wants to play games this weekend?'),
(3, 2, 'TEXT', 'I am in! What games?'),
(4, 2, 'TEXT', 'Count me in too!');

-- Insert some private messages
INSERT INTO messages (sender_id, receiver_id, message_type, message_content) VALUES
(1, 2, 'TEXT', 'Hi Alice, how are you?'),
(2, 1, 'TEXT', 'Hi admin, I am good! How about you?'),
(1, 3, 'TEXT', 'Bob, did you see the new group messages?'),
(3, 1, 'TEXT', 'Yes, checking them now!');

-- ============================================
-- VERIFICATION
-- ============================================

SELECT '========================================' AS '';
SELECT 'DATABASE CREATED SUCCESSFULLY!' AS '';
SELECT '========================================' AS '';

SELECT 'Users created:' AS Info, COUNT(*) AS Count FROM users;
SELECT 'Friendships created:' AS Info, COUNT(*) AS Count FROM friends;
SELECT 'Groups created:' AS Info, COUNT(*) AS Count FROM chat_groups;
SELECT 'Group members added:' AS Info, COUNT(*) AS Count FROM group_members;
SELECT 'Group messages created:' AS Info, COUNT(*) AS Count FROM messages WHERE group_id IS NOT NULL;
SELECT 'Private messages created:' AS Info, COUNT(*) AS Count FROM messages WHERE receiver_id IS NOT NULL;

SELECT '========================================' AS '';
SELECT 'GROUPS OVERVIEW' AS '';
SELECT '========================================' AS '';

SELECT
    g.group_id,
    g.group_name,
    g.group_description,
    c.username AS creator,
    COUNT(DISTINCT gm.user_id) AS members,
    COUNT(DISTINCT m.message_id) AS messages
FROM chat_groups g
LEFT JOIN users c ON g.creator_id = c.user_id
LEFT JOIN group_members gm ON g.group_id = gm.group_id
LEFT JOIN messages m ON g.group_id = m.group_id
GROUP BY g.group_id, g.group_name, g.group_description, c.username;

SELECT '========================================' AS '';
SELECT 'TEST CREDENTIALS' AS '';
SELECT '========================================' AS '';
SELECT 'Username: admin, alice, bob, or charlie' AS '';
SELECT 'Password: admin123 (for all users)' AS '';
SELECT '========================================' AS '';
