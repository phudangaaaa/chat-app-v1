# Debug Group Messages - Hướng dẫn kiểm tra tại sao tin nhắn group không hiển thị

## 🔍 Tổng quan vấn đề

**Triệu chứng:** Khi mở group chat, không thấy tin nhắn cũ từ database

**Code đã được kiểm tra:** ✅ Code load tin nhắn group là ĐÚNG
- Client: `ChatController.loadGroupMessages()` (line 217-245)
- Server: `MessageService.getGroupMessages()` (line 138-176)
- Server: `ClientHandler.handleGetMessages()` (line 355-379)

## 📋 Các bước debug

### Bước 1: Kiểm tra database có tin nhắn không?

```bash
# Kết nối MySQL
mysql -u root -p

# Chọn database
USE chat_app;

# Kiểm tra tin nhắn group
SELECT
  m.message_id,
  m.sender_id,
  m.group_id,
  m.message_type,
  m.message_content,
  m.sent_at,
  s.username as sender_name
FROM messages m
JOIN users s ON m.sender_id = s.user_id
WHERE m.group_id IS NOT NULL
ORDER BY m.sent_at DESC;
```

**Kết quả mong đợi:**
- Nếu có dữ liệu → Vấn đề ở code
- Nếu KHÔNG có dữ liệu → Cần tạo test data (xem Bước 2)

---

### Bước 2: Tạo test data cho group messages

```bash
# Chạy script tạo test data
mysql -u root -p chat_app < /home/user/chat-app-v1/database/insert_test_group_messages.sql
```

**Script này sẽ:**
1. Tạo một test group nếu chưa có
2. Thêm users vào group
3. Insert 5 tin nhắn test vào group
4. Verify data

**Sau khi chạy, check lại:**
```sql
SELECT COUNT(*) FROM messages WHERE group_id IS NOT NULL;
-- Phải thấy > 0
```

---

### Bước 3: Debug từ phía Server

**Mở terminal và chạy server với logs chi tiết:**

```bash
cd /home/user/chat-app-v1/ChatServer
mvn exec:java
```

**Khi mở group chat, xem server logs phải có:**

```
INFO  - Getting group messages for group X (user: Y)
INFO  - Fetching group messages for groupId=X, limit=100
DEBUG - Executing SQL: SELECT m.*, s.username as sender_name ...
DEBUG - Parameters: groupId=X, limit=100
INFO  - Successfully retrieved N messages for group X
```

**Các trường hợp:**

#### Case 1: Không thấy log "Getting group messages"
→ Client không gửi request
→ Kiểm tra ChatController.loadGroupMessages() có được gọi không

#### Case 2: Thấy log nhưng "Successfully retrieved 0 messages"
→ Database không có messages hoặc groupId sai
→ Chạy query SQL manual để check

#### Case 3: Thấy log "Successfully retrieved N messages" (N > 0)
→ Server trả về đúng
→ Vấn đề ở client side (xem Bước 4)

---

### Bước 4: Debug từ phía Client

**Trong ChatController.loadGroupMessages() có print statements:**

```java
System.out.println("Loading group messages for group ID: " + group.getGroupId());
System.out.println("Received " + messageList.size() + " group messages");
```

**Chạy client và xem console output:**

```bash
cd /home/user/chat-app-v1/ChatClient
mvn javafx:run
```

**Mở một group chat và check console:**

```
Loading group messages for group ID: 1
Received 5 group messages
```

**Các trường hợp:**

#### Case 1: Không thấy "Loading group messages"
→ loadGroupMessages() không được gọi
→ Kiểm tra initialize() của ChatController
→ Kiểm tra isGroupChat flag

#### Case 2: Thấy "Loading" nhưng không thấy "Received"
→ Request thất bại hoặc timeout
→ Check network connection
→ Check server có running không

#### Case 3: Thấy "Received 0 group messages"
→ Server trả về empty list
→ Back to Bước 3 - check server

#### Case 4: Thấy "Received N group messages" (N > 0) nhưng UI trống
→ Messages không add vào ListView
→ Check `messages.setAll(messageList)` có chạy không
→ Check UI rendering (xem Bước 5)

---

### Bước 5: Debug UI Rendering

**File:** `ChatController.setupMessageListView()` (line 91-155)

**Kiểm tra:**

1. **ListView có bind đúng không?**
   ```java
   messageListView.setItems(messages);
   ```

2. **Cell factory có render đúng không?**
   - Sender detection: `message.getSenderId() == SessionManager.getInstance().getCurrentUserId()`
   - Group sender name: `message.getSenderName()`

3. **Test bằng cách thêm debug message:**
   ```java
   messages.setAll(messageList);
   System.out.println("Messages added to ListView: " + messages.size());
   messageListView.refresh(); // Force refresh
   ```

---

## 🔧 Quick Fix Script

**Chạy tất cả các bước debug trong 1 command:**

```bash
#!/bin/bash
echo "=== GROUP MESSAGES DEBUG ==="

# Step 1: Check database
echo -e "\n1. Checking database for group messages..."
mysql -u root -p chat_app -e "SELECT COUNT(*) as total_group_messages FROM messages WHERE group_id IS NOT NULL;"

# Step 2: If empty, create test data
read -p "Do you want to create test data? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
    echo "Creating test data..."
    mysql -u root -p chat_app < /home/user/chat-app-v1/database/insert_test_group_messages.sql
fi

# Step 3: Check which groups exist
echo -e "\n2. Available groups:"
mysql -u root -p chat_app -e "SELECT group_id, group_name, created_at FROM chat_groups;"

# Step 4: Check group members
read -p "Enter group_id to check: " group_id
echo -e "\n3. Members of group $group_id:"
mysql -u root -p chat_app -e "SELECT u.user_id, u.username, u.full_name FROM users u JOIN group_members gm ON u.user_id = gm.user_id WHERE gm.group_id = $group_id;"

# Step 5: Check messages for that group
echo -e "\n4. Messages in group $group_id:"
mysql -u root -p chat_app -e "SELECT m.message_id, s.username as sender, m.message_content, m.sent_at FROM messages m JOIN users s ON m.sender_id = s.user_id WHERE m.group_id = $group_id ORDER BY m.sent_at DESC LIMIT 10;"

echo -e "\n=== DEBUG COMPLETE ==="
```

**Lưu script này vào file:**
```bash
chmod +x /home/user/chat-app-v1/debug_group_messages.sh
./debug_group_messages.sh
```

---

## 🎯 Các vấn đề thường gặp và giải pháp

### Vấn đề 1: Database không có tin nhắn
**Nguyên nhân:** Chưa có ai gửi tin nhắn trong group

**Giải pháp:**
```sql
-- Manual insert test message
INSERT INTO messages (sender_id, group_id, message_type, message_content)
VALUES (1, 1, 'TEXT', 'Test message from SQL');
```

---

### Vấn đề 2: GroupId không khớp
**Nguyên nhân:** Client gửi sai groupId

**Debug:**
```java
// Trong ChatController.loadGroupMessages()
System.out.println("Loading messages for group: " + group.getGroupId() + " - " + group.getGroupName());
```

**Kiểm tra groupId có tồn tại trong database:**
```sql
SELECT * FROM chat_groups WHERE group_id = X;
```

---

### Vấn đề 3: JOIN query thất bại
**Nguyên nhân:** User đã bị xóa nhưng messages vẫn còn

**Giải pháp:** Dùng LEFT JOIN thay vì INNER JOIN
```java
// Trong MessageService.getGroupMessages()
String sql = "SELECT m.*, s.username as sender_name " +
             "FROM messages m " +
             "LEFT JOIN users s ON m.sender_id = s.user_id " +  // Changed to LEFT JOIN
             "WHERE m.group_id = ? " +
             "ORDER BY m.sent_at DESC LIMIT ?";
```

---

### Vấn đề 4: JSON parsing error
**Nguyên nhân:** Response format không đúng

**Debug:**
```java
// Trong ChatController.loadGroupMessages()
System.out.println("Response: " + response);
System.out.println("Messages data: " + response.getData().get("messages"));
```

---

### Vấn đề 5: UI không update
**Nguyên nhân:** Không chạy trên JavaFX thread

**Giải pháp:**
```java
// Đảm bảo UI update chạy trên Platform.runLater()
Platform.runLater(() -> {
    messages.setAll(messageList);
    scrollToBottom();
    messageListView.refresh(); // Force refresh
});
```

---

## ✅ Checklist để verify tin nhắn group hoạt động

- [ ] Database có ít nhất 1 group
- [ ] Database có ít nhất 1 user là member của group đó
- [ ] Database có ít nhất 1 message với group_id = groupId
- [ ] Server log hiện "Successfully retrieved N messages" (N > 0)
- [ ] Client log hiện "Received N group messages" (N > 0)
- [ ] ListView có messages (check `messages.size()`)
- [ ] UI hiển thị messages với sender name
- [ ] Có thể scroll xem các messages

---

## 📞 Nếu vẫn không hoạt động

**Thu thập logs đầy đủ:**

```bash
# Server logs
cd ChatServer
mvn exec:java > server_debug.log 2>&1 &

# Client logs
cd ChatClient
mvn javafx:run > client_debug.log 2>&1

# Sau khi test, check logs:
grep -i "group.*message" server_debug.log
grep -i "group.*message" client_debug.log
```

**Gửi cho developer:**
1. `server_debug.log`
2. `client_debug.log`
3. Output của `SELECT * FROM messages WHERE group_id = X;`
4. Screenshot của UI (nếu có)

---

## 💡 Lưu ý quan trọng

1. **Always check database first** - Nếu không có data thì code đúng cũng không hiển thị được gì
2. **Read the logs** - Logs sẽ chỉ ra chính xác vấn đề ở đâu
3. **Test step by step** - Đừng skip bước nào
4. **Use test data** - Tạo test data để dễ debug hơn

---

## 🚀 Expected Working Flow

1. User clicks vào group trong Groups tab
2. `MainController.openChatWindow()` được gọi với `group` parameter
3. `ChatController` được khởi tạo với `isGroupChat = true`
4. `ChatController.initialize()` gọi `loadGroupMessages()`
5. Client gửi request `ACTION_GET_MESSAGES` với `groupId`
6. Server nhận request, gọi `handleGetMessages()`
7. Server kiểm tra `data.has("groupId")` → true
8. Server gọi `messageService.getGroupMessages(groupId, limit)`
9. SQL query chạy và trả về list messages
10. Server gửi response với `messages` array
11. Client nhận response, parse thành `List<Message>`
12. Client reverse list (oldest first)
13. Client update UI: `messages.setAll(messageList)`
14. ListView render messages với sender names
15. User thấy tin nhắn! 🎉

Nếu bất kỳ bước nào fail → debug bước đó!
