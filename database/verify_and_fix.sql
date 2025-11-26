-- Comprehensive verification and fix script
-- Run this to check and fix both friends list and group chat issues

USE chat_app;

-- ===== SECTION 1: VERIFY FRIENDS =====
SELECT '===== CHECKING FRIENDS TABLE =====' as '';

-- Check total friends relationships
SELECT 'Total friend relationships:' as info, COUNT(*) as count FROM friends;

-- Check friends per user
SELECT
    u.user_id,
    u.username,
    u.full_name,
    COUNT(f.friend_id) as friend_count
FROM users u
LEFT JOIN friends f ON u.user_id = f.user_id
GROUP BY u.user_id, u.username, u.full_name
ORDER BY friend_count DESC;

-- Show all friend relationships
SELECT
    f.user_id,
    u1.username as user_name,
    f.friend_id,
    u2.username as friend_name
FROM friends f
JOIN users u1 ON f.user_id = u1.user_id
JOIN users u2 ON f.friend_id = u2.user_id
ORDER BY f.user_id, f.friend_id;

-- Check for missing reciprocal relationships (should be bidirectional)
SELECT 'Missing reciprocal relationships:' as issue;
SELECT
    f1.user_id,
    u1.username as user_name,
    f1.friend_id,
    u2.username as friend_name
FROM friends f1
JOIN users u1 ON f1.user_id = u1.user_id
JOIN users u2 ON f1.friend_id = u2.user_id
LEFT JOIN friends f2 ON f1.friend_id = f2.user_id AND f1.user_id = f2.friend_id
WHERE f2.user_id IS NULL;

-- ===== SECTION 2: VERIFY GROUP CHAT =====
SELECT '===== CHECKING GROUP CHAT =====' as '';

-- Check total groups
SELECT 'Total groups:' as info, COUNT(*) as count FROM chat_groups;

-- Check group messages
SELECT 'Total group messages:' as info, COUNT(*) as count FROM messages WHERE group_id IS NOT NULL;

-- Show all groups with message counts
SELECT
    g.group_id,
    g.group_name,
    g.created_by,
    u.username as creator,
    COUNT(m.message_id) as message_count,
    COUNT(DISTINCT gm.user_id) as member_count
FROM chat_groups g
JOIN users u ON g.created_by = u.user_id
LEFT JOIN messages m ON g.group_id = m.group_id
LEFT JOIN group_members gm ON g.group_id = gm.group_id
GROUP BY g.group_id, g.group_name, g.created_by, u.username
ORDER BY g.created_at DESC;

-- Show recent group messages
SELECT
    m.message_id,
    m.group_id,
    g.group_name,
    m.sender_id,
    u.username as sender,
    m.message_content,
    m.sent_at
FROM messages m
JOIN users u ON m.sender_id = u.user_id
JOIN chat_groups g ON m.group_id = g.group_id
WHERE m.group_id IS NOT NULL
ORDER BY m.sent_at DESC
LIMIT 20;

-- ===== SECTION 3: CHECK FOR DATA ISSUES =====
SELECT '===== CHECKING FOR DATA ISSUES =====' as '';

-- Check for orphaned messages
SELECT 'Orphaned messages (no group, no receiver):' as issue, COUNT(*) as count
FROM messages
WHERE group_id IS NULL AND receiver_id IS NULL;

-- Check for invalid user references in friends table
SELECT 'Invalid friends (user not exists):' as issue, COUNT(*) as count
FROM friends f
LEFT JOIN users u ON f.friend_id = u.user_id
WHERE u.user_id IS NULL;

-- Check for messages with invalid sender
SELECT 'Messages with invalid sender:' as issue, COUNT(*) as count
FROM messages m
LEFT JOIN users u ON m.sender_id = u.user_id
WHERE u.user_id IS NULL;

-- Check for messages with invalid group
SELECT 'Messages with invalid group:' as issue, COUNT(*) as count
FROM messages m
LEFT JOIN chat_groups g ON m.group_id = g.group_id
WHERE m.group_id IS NOT NULL AND g.group_id IS NULL;

-- ===== SECTION 4: TEST QUERIES (Same as Java code) =====
SELECT '===== TESTING ACTUAL QUERIES =====' as '';

-- Test getFriends query for user 1 (change userId as needed)
SET @test_user_id = (SELECT user_id FROM users ORDER BY user_id LIMIT 1);

SELECT 'Testing getFriends for user:' as test, @test_user_id as user_id;
SELECT
    u.*
FROM users u
JOIN friends f ON u.user_id = f.friend_id
WHERE f.user_id = @test_user_id
ORDER BY u.user_status DESC, u.full_name;

-- Test getGroupMessages query for first group (change groupId as needed)
SET @test_group_id = (SELECT group_id FROM chat_groups ORDER BY group_id LIMIT 1);

SELECT 'Testing getGroupMessages for group:' as test, @test_group_id as group_id;
SELECT
    m.*,
    s.username as sender_name
FROM messages m
JOIN users s ON m.sender_id = s.user_id
WHERE m.group_id = @test_group_id
ORDER BY m.sent_at DESC
LIMIT 50;

-- ===== SECTION 5: RECOMMENDATIONS =====
SELECT '===== RECOMMENDATIONS =====' as '';

-- Recommend creating test data if no groups/messages
SELECT
    CASE
        WHEN (SELECT COUNT(*) FROM chat_groups) = 0 THEN 'Run insert_test_group_messages.sql to create test data'
        WHEN (SELECT COUNT(*) FROM messages WHERE group_id IS NOT NULL) = 0 THEN 'Groups exist but no messages. Send test messages in the app.'
        ELSE 'Data looks okay. Check server logs for detailed debugging.'
    END as recommendation;
