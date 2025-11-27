-- Fix Group Chat - Complete SQL Update Script
-- This script will fix all group chat issues

-- Use correct database name
USE chat_app;

-- ============================================
-- PART 1: FIX TABLE STRUCTURE (if needed)
-- ============================================

-- Check and add missing columns to messages table if not exist
SET @dbname = 'chat_app';
SET @tablename = 'messages';
SET @columnname = 'file_url';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(500) AFTER message_content')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add file_name if not exists
SET @columnname = 'file_name';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(255) AFTER file_url')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add file_size if not exists
SET @columnname = 'file_size';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' BIGINT AFTER file_name')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ============================================
-- PART 2: FIX INDEXES FOR PERFORMANCE
-- ============================================

-- Add index for group messages if not exists
SET @indexname = 'idx_group_messages';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (INDEX_NAME = @indexname)
  ) > 0,
  'SELECT 1',
  CONCAT('CREATE INDEX ', @indexname, ' ON ', @tablename, ' (group_id, sent_at DESC)')
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- Add index for sender in group messages
SET @indexname = 'idx_sender_group';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (INDEX_NAME = @indexname)
  ) > 0,
  'SELECT 1',
  CONCAT('CREATE INDEX ', @indexname, ' ON ', @tablename, ' (sender_id, group_id)')
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- ============================================
-- PART 3: VERIFY CURRENT DATA
-- ============================================

SELECT '========================================' AS '';
SELECT 'DATABASE VERIFICATION' AS '';
SELECT '========================================' AS '';

-- Check users
SELECT 'Total users:' AS Info, COUNT(*) AS Count FROM users;

-- Check groups
SELECT 'Total groups:' AS Info, COUNT(*) AS Count FROM chat_groups;

-- Check group members
SELECT 'Total group members:' AS Info, COUNT(*) AS Count FROM group_members;

-- Check group messages
SELECT 'Total group messages:' AS Info, COUNT(*) AS Count FROM messages WHERE group_id IS NOT NULL;

-- Check private messages
SELECT 'Total private messages:' AS Info, COUNT(*) AS Count FROM messages WHERE receiver_id IS NOT NULL;

-- Show groups with member counts
SELECT '========================================' AS '';
SELECT 'GROUPS AND MEMBER COUNTS' AS '';
SELECT '========================================' AS '';

SELECT
    g.group_id,
    g.group_name,
    g.creator_id,
    c.username AS creator_name,
    COUNT(DISTINCT gm.user_id) AS member_count,
    g.created_at
FROM chat_groups g
LEFT JOIN users c ON g.creator_id = c.user_id
LEFT JOIN group_members gm ON g.group_id = gm.group_id
GROUP BY g.group_id, g.group_name, g.creator_id, c.username, g.created_at
ORDER BY g.created_at DESC;

-- Show group messages by group
SELECT '========================================' AS '';
SELECT 'MESSAGES PER GROUP' AS '';
SELECT '========================================' AS '';

SELECT
    g.group_id,
    g.group_name,
    COUNT(m.message_id) AS message_count,
    MAX(m.sent_at) AS last_message_at
FROM chat_groups g
LEFT JOIN messages m ON g.group_id = m.group_id
GROUP BY g.group_id, g.group_name
ORDER BY g.group_id;

-- ============================================
-- PART 4: CREATE TEST DATA IF NEEDED
-- ============================================

-- Create a test group if no groups exist
INSERT INTO chat_groups (group_name, group_description, creator_id)
SELECT 'Test Group Chat', 'Test group for debugging', user_id
FROM users
WHERE username = 'admin'
AND NOT EXISTS (SELECT 1 FROM chat_groups WHERE group_name = 'Test Group Chat')
LIMIT 1;

-- Get the test group ID
SET @test_group_id = (SELECT group_id FROM chat_groups WHERE group_name = 'Test Group Chat' LIMIT 1);

-- Add admin to the group if not already a member
INSERT IGNORE INTO group_members (group_id, user_id, member_role)
SELECT @test_group_id, user_id, 'ADMIN'
FROM users
WHERE username = 'admin';

-- Add other users to the group if they exist
INSERT IGNORE INTO group_members (group_id, user_id, member_role)
SELECT @test_group_id, user_id, 'MEMBER'
FROM users
WHERE username IN ('user1', 'user2');

-- Insert test messages if no messages exist for this group
INSERT INTO messages (sender_id, group_id, message_type, message_content)
SELECT
    u.user_id,
    @test_group_id,
    'TEXT',
    CONCAT('Test message from ', u.username)
FROM users u
WHERE u.username IN ('admin', 'user1', 'user2')
AND NOT EXISTS (
    SELECT 1 FROM messages
    WHERE group_id = @test_group_id
    AND sender_id = u.user_id
)
LIMIT 3;

-- ============================================
-- PART 5: FINAL VERIFICATION
-- ============================================

SELECT '========================================' AS '';
SELECT 'FINAL VERIFICATION - TEST GROUP' AS '';
SELECT '========================================' AS '';

-- Show test group details
SELECT
    'Group Details:' AS Info,
    group_id,
    group_name,
    group_description,
    creator_id,
    created_at
FROM chat_groups
WHERE group_id = @test_group_id;

-- Show test group members
SELECT '========================================' AS '';
SELECT 'Test Group Members:' AS '';

SELECT
    gm.group_id,
    gm.user_id,
    u.username,
    u.full_name,
    gm.member_role,
    gm.joined_at
FROM group_members gm
JOIN users u ON gm.user_id = u.user_id
WHERE gm.group_id = @test_group_id
ORDER BY gm.member_role DESC, gm.joined_at;

-- Show test group messages
SELECT '========================================' AS '';
SELECT 'Test Group Messages:' AS '';

SELECT
    m.message_id,
    m.sender_id,
    s.username AS sender_name,
    m.message_type,
    m.message_content,
    m.sent_at
FROM messages m
JOIN users s ON m.sender_id = s.user_id
WHERE m.group_id = @test_group_id
ORDER BY m.sent_at DESC;

-- Show the exact query that Java code will execute
SELECT '========================================' AS '';
SELECT 'SIMULATING JAVA QUERY:' AS '';
SELECT '========================================' AS '';

SELECT
    m.*,
    s.username as sender_name
FROM messages m
JOIN users s ON m.sender_id = s.user_id
WHERE m.group_id = @test_group_id
ORDER BY m.sent_at DESC
LIMIT 100;

-- ============================================
-- PART 6: TROUBLESHOOTING QUERIES
-- ============================================

SELECT '========================================' AS '';
SELECT 'TROUBLESHOOTING INFO' AS '';
SELECT '========================================' AS '';

-- Check for orphaned messages (sender doesn't exist)
SELECT 'Orphaned messages (sender deleted):' AS Issue, COUNT(*) AS Count
FROM messages m
LEFT JOIN users u ON m.sender_id = u.user_id
WHERE u.user_id IS NULL;

-- Check for messages with invalid group_id
SELECT 'Messages with invalid group_id:' AS Issue, COUNT(*) AS Count
FROM messages m
LEFT JOIN chat_groups g ON m.group_id = g.group_id
WHERE m.group_id IS NOT NULL AND g.group_id IS NULL;

-- Check for messages violating the CHECK constraint
SELECT 'Messages violating CHECK constraint:' AS Issue, COUNT(*) AS Count
FROM messages
WHERE NOT (
    (receiver_id IS NOT NULL AND group_id IS NULL) OR
    (receiver_id IS NULL AND group_id IS NOT NULL)
);

SELECT '========================================' AS '';
SELECT 'SCRIPT COMPLETED!' AS '';
SELECT '========================================' AS '';
SELECT 'Next steps:' AS '';
SELECT '1. Check the output above for any issues' AS '';
SELECT '2. Rebuild the application: cd ChatServer && mvn clean install' AS '';
SELECT '3. Rebuild the client: cd ChatClient && mvn clean install' AS '';
SELECT '4. Run the server: cd ChatServer && mvn exec:java' AS '';
SELECT '5. Run the client: cd ChatClient && mvn javafx:run' AS '';
SELECT '6. Test group chat with the Test Group Chat' AS '';
