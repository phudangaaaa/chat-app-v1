-- Debug script for group chat messages
-- Run this to check if group messages exist and are being queried correctly

USE chat_app;

-- 1. Check if there are any groups
SELECT 'Total groups:' as info, COUNT(*) as count FROM chat_groups;
SELECT * FROM chat_groups ORDER BY created_at DESC LIMIT 5;

-- 2. Check if there are any group messages
SELECT 'Total group messages:' as info, COUNT(*) as count FROM messages WHERE group_id IS NOT NULL;

-- 3. Show all group messages with details
SELECT
    m.message_id,
    m.sender_id,
    s.username as sender_username,
    s.full_name as sender_name,
    m.group_id,
    g.group_name,
    m.message_type,
    m.message_content,
    m.sent_at
FROM messages m
JOIN users s ON m.sender_id = s.user_id
LEFT JOIN chat_groups g ON m.group_id = g.group_id
WHERE m.group_id IS NOT NULL
ORDER BY m.sent_at DESC
LIMIT 20;

-- 4. Check messages per group
SELECT
    g.group_id,
    g.group_name,
    COUNT(m.message_id) as message_count
FROM chat_groups g
LEFT JOIN messages m ON g.group_id = m.group_id
GROUP BY g.group_id, g.group_name
ORDER BY message_count DESC;

-- 5. Test the exact query used by MessageService.getGroupMessages()
-- Replace '1' with your actual group_id
SET @test_group_id = 1;
SET @test_limit = 50;

SELECT
    m.*,
    s.username as sender_name
FROM messages m
JOIN users s ON m.sender_id = s.user_id
WHERE m.group_id = @test_group_id
ORDER BY m.sent_at DESC
LIMIT @test_limit;

-- 6. Check for any orphaned messages (messages without valid group or user)
SELECT 'Orphaned messages (no group, no receiver):' as issue;
SELECT * FROM messages
WHERE group_id IS NULL AND receiver_id IS NULL
LIMIT 10;

-- 7. Check group_members table
SELECT 'Group members:' as info;
SELECT
    gm.group_id,
    g.group_name,
    gm.user_id,
    u.username,
    u.full_name
FROM group_members gm
JOIN chat_groups g ON gm.group_id = g.group_id
JOIN users u ON gm.user_id = u.user_id
ORDER BY gm.group_id, u.username;
