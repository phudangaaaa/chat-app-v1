# Debug Group Chat Messages - Hướng dẫn chi tiết

## ⚠️ Vấn đề: Group chat không hiển thị tin nhắn từ database

### 📋 Checklist - Làm theo thứ tự:

---

## Bước 1: Kiểm tra Database

### 1.1. Kết nối MySQL và check database
```bash
mysql -u root -p
```

### 1.2. Chạy debug script
```sql
source /home/user/chat-app-v1/database/debug_group_messages.sql
```

**Xem kết quả:**
- Có bao nhiêu groups?
- Có bao nhiêu group messages?
- Messages có `group_id` đúng không?

### 1.3. Nếu KHÔNG có messages trong group
```sql
-- Chạy script này để tạo test data
source /home/user/chat-app-v1/database/insert_test_group_messages.sql
```

---

## Bước 2: Kiểm tra Server Logs

### 2.1. Rebuild server với logging mới
```bash
cd ChatServer
mvn clean install
```

### 2.2. Chạy server và xem logs
```bash
mvn exec:java
```

### 2.3. Khi client request group messages, server sẽ log:

**ĐÚNG (có messages):**
```
INFO  - Getting group messages for group 1 (user: 2)
INFO  - Fetching group messages for groupId=1, limit=100
DEBUG - Executing SQL: SELECT m.*, s.username as sender_name FROM messages m JOIN users s ON m.sender_id = s.user_id WHERE m.group_id = ? ORDER BY m.sent_at DESC LIMIT ?
DEBUG - Parameters: groupId=1, limit=100
INFO  - Successfully retrieved 5 messages for group 1
INFO  - Retrieved 5 group messages for group 1
```

**SAI (không có messages):**
```
INFO  - Getting group messages for group 1 (user: 2)
INFO  - Fetching group messages for groupId=1, limit=100
INFO  - Successfully retrieved 0 messages for group 1
WARN  - No messages found for group 1. Check if group has messages in database.
INFO  - Retrieved 0 group messages for group 1
```

---

## Bước 3: Kiểm tra Client Logs

### 3.1. Rebuild client
```bash
cd ChatClient
mvn clean install
```

### 3.2. Chạy client
```bash
mvn javafx:run
```

### 3.3. Mở group chat, xem console logs:

**ĐÚNG:**
```
Loading group messages for group ID: 1
Received 5 group messages
```

**SAI:**
```
Loading group messages for group ID: 1
Received 0 group messages
```

---

## Bước 4: Test Manual Query

### 4.1. Chạy query trực tiếp trong MySQL
```sql
USE chat_app;

-- Replace 1 with your actual group_id
SET @group_id = 1;

SELECT
  m.*,
  s.username as sender_name
FROM messages m
JOIN users s ON m.sender_id = s.user_id
WHERE m.group_id = @group_id
ORDER BY m.sent_at DESC
LIMIT 50;
```

**Nếu query này trả về 0 rows** → Database không có messages cho group này

---

## Bước 5: Common Issues & Fixes

### Issue 1: Database không có messages
**Nguyên nhân:** Chưa gửi messages hoặc messages không được save đúng

**Fix:**
```sql
-- Option 1: Tạo test data
source /home/user/chat-app-v1/database/insert_test_group_messages.sql

-- Option 2: Manual insert
INSERT INTO messages (sender_id, group_id, message_type, message_content)
VALUES (1, 1, 'TEXT', 'Test message');
```

### Issue 2: Messages có receiver_id thay vì group_id
**Nguyên nhân:** Tin nhắn được lưu nhầm là private message

**Check:**
```sql
SELECT * FROM messages WHERE receiver_id IS NOT NULL AND group_id IS NULL;
```

**Fix:** Không có cách fix tự động. Phải gửi lại messages mới.

### Issue 3: group_id sai
**Nguyên nhân:** Client gửi sai groupId

**Check client logs:**
```
Loading group messages for group ID: 999  <-- Sai, group này không tồn tại
```

**Fix:** Check trong MainController.java xem group object có đúng không

### Issue 4: Database connection failed
**Check server logs:**
```
ERROR - Error getting group messages for group 1
java.sql.SQLException: Communications link failure
```

**Fix:**
```bash
# Restart MySQL
sudo systemctl restart mysql

# Check MySQL is running
sudo systemctl status mysql

# Verify connection
mysql -u root -p -e "SELECT 1"
```

---

## Bước 6: End-to-End Test

### 6.1. Tạo scenario hoàn chỉnh:

1. **Đăng nhập 2 users** (User A và User B)

2. **User A tạo group:**
   - Click "Create Group"
   - Nhập tên: "Test Group"

3. **User A add User B vào group:**
   - Mở group chat
   - Click "Add Members"
   - Chọn User B
   - Click OK

4. **User A gửi message:**
   - Type: "Hello from User A"
   - Click Send

5. **Check server logs:**
   ```
   Group message sent from X to group Y
   ```

6. **Check database:**
   ```sql
   SELECT * FROM messages WHERE group_id = Y ORDER BY sent_at DESC LIMIT 5;
   ```
   → Phải thấy message "Hello from User A"

7. **User A close và open lại group chat:**
   - Client logs: "Loading group messages for group ID: Y"
   - Client logs: "Received 1 group messages"
   - Message phải hiển thị

8. **User B open group chat:**
   - Phải thấy message từ User A

---

## Bước 7: SQL Queries hữu ích

```sql
-- 1. Xem tất cả groups
SELECT * FROM chat_groups;

-- 2. Xem members của group 1
SELECT
  gm.*,
  u.username,
  u.full_name
FROM group_members gm
JOIN users u ON gm.user_id = u.user_id
WHERE gm.group_id = 1;

-- 3. Xem tất cả messages của group 1
SELECT
  m.message_id,
  m.sender_id,
  s.username as sender,
  m.message_content,
  m.sent_at
FROM messages m
JOIN users s ON m.sender_id = s.user_id
WHERE m.group_id = 1
ORDER BY m.sent_at DESC;

-- 4. Count messages per group
SELECT
  g.group_id,
  g.group_name,
  COUNT(m.message_id) as message_count
FROM chat_groups g
LEFT JOIN messages m ON g.group_id = m.group_id
GROUP BY g.group_id, g.group_name;

-- 5. Xóa tất cả messages trong group 1 (để test lại)
DELETE FROM messages WHERE group_id = 1;

-- 6. Xóa group và tạo lại
DELETE FROM chat_groups WHERE group_id = 1;
-- Tạo lại group trong app
```

---

## ✅ Expected Behavior (Đúng)

### Khi gửi message:
1. Client gọi `ACTION_SEND_GROUP_MESSAGE`
2. Server log: "Group message sent from X to group Y"
3. Database có record mới trong `messages` với `group_id = Y`
4. Các members khác receive notification

### Khi load messages:
1. Client gọi `ACTION_GET_MESSAGES` với `groupId`
2. Server log: "Getting group messages for group Y"
3. Server log: "Successfully retrieved N messages"
4. Client log: "Received N group messages"
5. Messages hiển thị trong chat window

---

## 🆘 Vẫn không hoạt động?

### Collect đầy đủ thông tin:

1. **Database state:**
   ```bash
   mysql -u root -p chat_app -e "
     SELECT * FROM chat_groups;
     SELECT * FROM group_members;
     SELECT * FROM messages WHERE group_id IS NOT NULL;
   " > db_state.txt
   ```

2. **Server logs:**
   ```bash
   cd ChatServer
   mvn exec:java > server.log 2>&1
   ```

3. **Client logs:**
   ```bash
   cd ChatClient
   mvn javafx:run > client.log 2>&1
   ```

4. **Check TROUBLESHOOTING.md** section "Group Chat không hiển thị tin nhắn cũ"

---

## Quick Fix Script

Nếu muốn fix nhanh, chạy tất cả lệnh này:

```bash
#!/bin/bash

# 1. Check database
mysql -u root -p chat_app < database/debug_group_messages.sql

# 2. Insert test data if needed
mysql -u root -p chat_app < database/insert_test_group_messages.sql

# 3. Rebuild everything
cd ChatServer && mvn clean install
cd ../ChatClient && mvn clean install

# 4. Run server (in background)
cd ../ChatServer && mvn exec:java > server.log 2>&1 &

# 5. Run client
cd ../ChatClient && mvn javafx:run
```

Sau đó test:
1. Mở group chat
2. Xem console logs
3. Check server.log
4. Verify messages hiển thị
