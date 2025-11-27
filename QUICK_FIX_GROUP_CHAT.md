# Quick Fix - Group Chat Issues 🚀

## ⚡ TL;DR - Cách nhanh nhất để fix

Nếu group chat không hoạt động, chọn **1 trong 2 cách** dưới đây:

### 🔧 Cách 1: Fix Database Hiện Tại (Giữ dữ liệu cũ)

```bash
cd /home/user/chat-app-v1
mysql -u root -p < database/fix_group_chat.sql
```

**Khi nào dùng:** Bạn muốn giữ lại users và messages hiện tại

**Làm gì:**
- Fix table structure nếu thiếu columns
- Thêm indexes để tăng tốc
- Tạo test data nếu cần
- Verify database

---

### 🔄 Cách 2: Reset Database Hoàn Toàn (Xóa hết, làm lại)

```bash
cd /home/user/chat-app-v1
mysql -u root -p < database/reset_database.sql
```

**Khi nào dùng:** Database bị lỗi nặng, muốn bắt đầu từ đầu

**Làm gì:**
- XÓA database cũ hoàn toàn
- Tạo database mới từ đầu
- Tạo 4 test users: admin, alice, bob, charlie (password: admin123)
- Tạo 2 groups với messages sẵn
- Tạo friendships giữa các users

**⚠️ CẢNH BÁO:** Sẽ XÓA TẤT CẢ DỮ LIỆU hiện tại!

---

## 📋 Sau khi chạy SQL - Rebuild Application

```bash
# 1. Rebuild Server
cd /home/user/chat-app-v1/ChatServer
mvn clean install

# 2. Rebuild Client
cd /home/user/chat-app-v1/ChatClient
mvn clean install

# 3. Run Server (Terminal 1)
cd /home/user/chat-app-v1/ChatServer
mvn exec:java

# 4. Run Client (Terminal 2)
cd /home/user/chat-app-v1/ChatClient
mvn javafx:run
```

---

## 🧪 Test Group Chat

### Nếu dùng Cách 1 (fix_group_chat.sql):

1. Login với user hiện tại
2. Vào **Groups tab**
3. Tìm group "Test Group Chat"
4. Click vào group để mở chat
5. Xem có messages không

### Nếu dùng Cách 2 (reset_database.sql):

**Test với 2 groups có sẵn:**

#### Group 1: "Team Project"
- **Members:** admin, alice, bob
- **6 messages** về project discussion
- **Login as:** admin (hoặc alice/bob)
- **Password:** admin123

#### Group 2: "Friends Hangout"
- **Members:** alice, bob, charlie
- **6 messages** về weekend plans
- **Login as:** alice (hoặc bob/charlie)
- **Password:** admin123

**Các bước test:**

1. **Login:**
   ```
   Username: admin
   Password: admin123
   ```

2. **Vào Groups tab** → Thấy "Team Project"

3. **Click vào group** → Mở chat window

4. **Kiểm tra:**
   - ✅ Thấy 6 messages cũ load từ database
   - ✅ Messages có tên người gửi
   - ✅ Messages hiển thị đúng thứ tự (cũ nhất trên, mới nhất dưới)
   - ✅ Có thể gửi tin nhắn mới
   - ✅ Tin nhắn mới hiển thị ngay

5. **Test với user khác:**
   - Logout admin
   - Login as alice (password: admin123)
   - Vào Groups → thấy cả "Team Project" và "Friends Hangout"
   - Test cả 2 groups

---

## 🐛 Nếu vẫn lỗi - Debug

### Bước 1: Verify Database

```sql
mysql -u root -p

USE chat_app;

-- Check groups
SELECT * FROM chat_groups;

-- Check group members
SELECT g.group_name, u.username, gm.member_role
FROM group_members gm
JOIN chat_groups g ON gm.group_id = g.group_id
JOIN users u ON gm.user_id = u.user_id
ORDER BY g.group_id, gm.member_role DESC;

-- Check group messages
SELECT
    g.group_name,
    u.username AS sender,
    m.message_content,
    m.sent_at
FROM messages m
JOIN chat_groups g ON m.group_id = g.group_id
JOIN users u ON m.sender_id = u.user_id
WHERE m.group_id IS NOT NULL
ORDER BY g.group_id, m.sent_at;
```

**Kết quả mong đợi:**
- Có ít nhất 1 group
- Có ít nhất 2 users là members của group
- Có ít nhất 1 message trong group

### Bước 2: Check Server Logs

Khi mở group chat, server logs phải có:

```
INFO  - Getting groups list for user 1
INFO  - Successfully retrieved 2 groups for user 1
DEBUG - Added group: Team Project (1) with 3 members
DEBUG - Added group: Friends Hangout (2) with 3 members
```

Khi click vào group:

```
INFO  - Getting group messages for group 1 (user: 1)
INFO  - Fetching group messages for groupId=1, limit=100
INFO  - Successfully retrieved 6 messages for group 1
```

**Nếu thấy 0 messages:**
- Database không có messages cho group đó
- Chạy lại `reset_database.sql`

### Bước 3: Check Client Logs

Console phải hiển thị:

```
Loading group messages for group ID: 1
Received 6 group messages
```

**Nếu không thấy:**
- Client không gửi request
- Check network connection
- Restart cả server và client

---

## 📊 So sánh 2 cách

| Feature | fix_group_chat.sql | reset_database.sql |
|---------|-------------------|-------------------|
| Giữ dữ liệu cũ | ✅ Có | ❌ Không (xóa hết) |
| Tạo test users | ❌ Không | ✅ Có (4 users) |
| Tạo test groups | ✅ 1 group | ✅ 2 groups |
| Tạo test messages | ✅ 3 messages | ✅ 12 messages |
| Fix table structure | ✅ Có | ✅ Có |
| Add indexes | ✅ Có | ✅ Có |
| Độ an toàn | ✅ Safe | ⚠️ Xóa data |
| Khi nào dùng | Production | Development/Testing |

---

## 🎯 Recommended Flow

### Lần đầu setup / Development:

```bash
# 1. Reset database với test data
mysql -u root -p < database/reset_database.sql

# 2. Rebuild
cd ChatServer && mvn clean install
cd ../ChatClient && mvn clean install

# 3. Run và test
cd ../ChatServer && mvn exec:java  # Terminal 1
cd ChatClient && mvn javafx:run     # Terminal 2

# 4. Login: admin / admin123
# 5. Test groups
```

### Production / Có data quan trọng:

```bash
# 1. Backup trước
mysqldump -u root -p chat_app > backup_$(date +%Y%m%d).sql

# 2. Fix database
mysql -u root -p < database/fix_group_chat.sql

# 3. Rebuild
cd ChatServer && mvn clean install
cd ../ChatClient && mvn clean install

# 4. Test
```

---

## 💡 Tips

1. **Luôn backup trước khi reset:**
   ```bash
   mysqldump -u root -p chat_app > backup.sql
   ```

2. **Nếu quên password test users:**
   - Password cho tất cả: `admin123`
   - Usernames: admin, alice, bob, charlie

3. **Nếu cần thêm users:**
   ```sql
   INSERT INTO users (username, email, password_hash, full_name, user_status)
   VALUES ('newuser', 'new@email.com',
           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
           'New User', 'ONLINE');
   -- Password: admin123
   ```

4. **Nếu cần thêm user vào group:**
   ```sql
   INSERT INTO group_members (group_id, user_id, member_role)
   VALUES (1, 5, 'MEMBER');  -- Add user_id=5 to group_id=1
   ```

5. **Nếu cần xem debug output chi tiết:**
   - Đọc file `DEBUG_GROUP_MESSAGES.md`
   - Follow từng bước để tìm vấn đề

---

## 🆘 Vẫn không hoạt động?

1. **Check MySQL service:**
   ```bash
   sudo systemctl status mysql
   # Nếu không chạy:
   sudo systemctl start mysql
   ```

2. **Check database connection:**
   ```bash
   mysql -u root -p -e "SHOW DATABASES;"
   # Phải thấy chat_app trong list
   ```

3. **Check Java version:**
   ```bash
   java -version
   # Cần Java 11+
   ```

4. **Check Maven:**
   ```bash
   mvn -version
   ```

5. **Collect logs và gửi để được hỗ trợ:**
   ```bash
   # Server logs
   cd ChatServer
   mvn exec:java > server.log 2>&1 &

   # Client logs
   cd ChatClient
   mvn javafx:run > client.log 2>&1

   # Sau khi test, gửi:
   # - server.log
   # - client.log
   # - Output của: SELECT * FROM messages WHERE group_id IS NOT NULL;
   ```

---

## ✅ Success Checklist

- [ ] Database script chạy thành công (không có lỗi)
- [ ] Server build thành công
- [ ] Client build thành công
- [ ] Server chạy không có ERROR logs
- [ ] Client mở được
- [ ] Login thành công
- [ ] Thấy groups trong Groups tab
- [ ] Click vào group mở được chat window
- [ ] Thấy messages cũ load từ database
- [ ] Gửi tin nhắn mới được
- [ ] Tin nhắn mới hiển thị ngay

Nếu tất cả ✅ → Group chat đã hoạt động! 🎉

---

**Made with ❤️ for debugging group chat issues**
