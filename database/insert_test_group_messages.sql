-- Insert test data for group chat messages
-- This will help verify that group chat is working correctly

USE chat_app;

-- First, let's check what we have
SELECT 'Current state:' as '=====';
SELECT 'Users:' as info, COUNT(*) as count FROM users;
SELECT 'Groups:' as info, COUNT(*) as count FROM chat_groups;
SELECT 'Group messages:' as info, COUNT(*) as count FROM messages WHERE group_id IS NOT NULL;

-- If you don't have test users, create them (skip if you already have users)
-- INSERT INTO users (username, email, password_hash, full_name, user_status)
-- VALUES
--   ('testuser1', 'test1@example.com', '$2a$10$...', 'Test User 1', 'ONLINE'),
--   ('testuser2', 'test2@example.com', '$2a$10$...', 'Test User 2', 'ONLINE');

-- Create a test group if it doesn't exist
INSERT INTO chat_groups (group_name, created_by)
SELECT 'Test Group Chat', user_id
FROM users
LIMIT 1
ON DUPLICATE KEY UPDATE group_name = group_name;

-- Get the group_id and user_ids for testing
SET @group_id = (SELECT group_id FROM chat_groups ORDER BY created_at DESC LIMIT 1);
SET @user1_id = (SELECT user_id FROM users ORDER BY user_id LIMIT 1);
SET @user2_id = (SELECT user_id FROM users ORDER BY user_id LIMIT 1 OFFSET 1);

-- Add users to the group if not already members
INSERT IGNORE INTO group_members (group_id, user_id)
VALUES
  (@group_id, @user1_id),
  (@group_id, COALESCE(@user2_id, @user1_id));

-- Insert test messages for the group
INSERT INTO messages (sender_id, group_id, message_type, message_content)
VALUES
  (@user1_id, @group_id, 'TEXT', 'Hello everyone in the group! 👋'),
  (@user1_id, @group_id, 'TEXT', 'This is a test message for group chat'),
  (COALESCE(@user2_id, @user1_id), @group_id, 'TEXT', 'Hi! I can see the messages now!'),
  (@user1_id, @group_id, 'TEXT', 'Great! The group chat is working 🎉'),
  (COALESCE(@user2_id, @user1_id), @group_id, 'TEXT', 'Testing 1, 2, 3...');

-- Verify the data was inserted
SELECT '===== VERIFICATION =====' as '';
SELECT 'Group created:' as info;
SELECT group_id, group_name, created_at FROM chat_groups WHERE group_id = @group_id;

SELECT 'Group members:' as info;
SELECT
  gm.group_id,
  gm.user_id,
  u.username,
  u.full_name
FROM group_members gm
JOIN users u ON gm.user_id = u.user_id
WHERE gm.group_id = @group_id;

SELECT 'Group messages:' as info;
SELECT
  m.message_id,
  m.sender_id,
  s.username as sender,
  m.message_content,
  m.sent_at
FROM messages m
JOIN users s ON m.sender_id = s.user_id
WHERE m.group_id = @group_id
ORDER BY m.sent_at DESC;

SELECT 'Total messages in group:' as info, COUNT(*) as count
FROM messages
WHERE group_id = @group_id;

-- Show the SQL query that the Java code will execute
SELECT '===== JAVA QUERY SIMULATION =====' as '';
SELECT
  m.*,
  s.username as sender_name
FROM messages m
JOIN users s ON m.sender_id = s.user_id
WHERE m.group_id = @group_id
ORDER BY m.sent_at DESC
LIMIT 50;
