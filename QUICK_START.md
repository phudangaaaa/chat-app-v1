# 🚀 Quick Start - Chạy ứng dụng ngay trong 5 phút

## ✅ Cách NHANH NHẤT (Command Line)

### Bước 1: Cài đặt Database (1 phút)
```bash
# Tạo database
mysql -u root -p < database/schema.sql
# Nhập password MySQL của bạn
```

### Bước 2: Cấu hình Database Password (30 giây)
Mở file: `ChatServer/src/main/java/com/chatapp/server/util/DatabaseManager.java`

Sửa dòng 14:
```java
private static final String DB_PASSWORD = "your_password"; // <-- Thay password MySQL của bạn
```

### Bước 3: Chạy Server (1 phút)
**Terminal 1:**
```bash
./run-server.sh          # Linux/Mac
# hoặc
run-server.bat           # Windows
```

Hoặc:
```bash
cd ChatServer
mvn clean compile exec:java
```

**Đợi đến khi thấy**: `Chat Server started on port 8888`

### Bước 4: Chạy Client (1 phút)
**Terminal 2:**
```bash
./run-client.sh          # Linux/Mac
# hoặc
run-client.bat           # Windows
```

Hoặc:
```bash
cd ChatClient
mvn clean javafx:run
```

**✨ DONE!** Cửa sổ login sẽ hiện ra!

### Bước 5: Login và Test
- **Username**: `admin`
- **Password**: `admin123`

---

## 🏃 Nếu dùng Eclipse/IntelliJ

### Eclipse:
1. Import Maven Projects (ChatServer và ChatClient)
2. **Chạy Server**: Right-click `ChatServer.java` → Run As → Java Application
3. **Chạy Client**:
   - Right-click project **ChatClient**
   - Run As → Maven build...
   - Goals: `clean javafx:run`
   - Click Run

### IntelliJ IDEA:
1. Open project folder `chat-app-v1`
2. **Chạy Server**: Click ▶️ bên cạnh `ChatServer.main()`
3. **Chạy Client**:
   - Maven tab (bên phải)
   - ChatClient → Plugins → javafx → **Double-click `javafx:run`**

---

## ❌ Gặp lỗi "JavaFX runtime components are missing"?

**Nguyên nhân**: Bạn đang chạy trực tiếp `ChatClientApp.java` thay vì dùng Maven.

**Giải pháp**:
```bash
cd ChatClient
mvn clean javafx:run    # ✅ ĐÚNG
```

**KHÔNG dùng**:
- ❌ Run `ChatClientApp.java` trực tiếp trong IDE
- ❌ `mvn exec:java`

**Muốn run trực tiếp trong IDE?**
→ Xem hướng dẫn chi tiết:
- Eclipse: [ECLIPSE_SETUP.md](ECLIPSE_SETUP.md)
- IntelliJ: [INTELLIJ_SETUP.md](INTELLIJ_SETUP.md)

---

## 📱 Test Chat giữa 2 người

### Cách 1: Chạy 2 Clients trên cùng máy
```bash
# Terminal 2
cd ChatClient && mvn javafx:run

# Terminal 3 (Client thứ 2)
cd ChatClient && mvn javafx:run
```

- Client 1: Login với `admin` / `admin123`
- Client 2: Login với `user1` / `test123`

### Test các tính năng:
1. **Search users**: Tìm kiếm `user1` hoặc `admin`
2. **Add friend**: Click "Add Friend"
3. **Accept request**: Client nhận được thông báo, chấp nhận
4. **Chat 1:1**: Double-click vào tên bạn bè → Chat window mở
5. **Send message**: Gõ tin nhắn, nhấn Enter
6. **Send file**: Click "📎 Send File"
7. **Emoji**: Click "😀 Emoji"
8. **Create group**: Click "Create Group"
9. **Group chat**: Double-click group → Chat

---

## 🎯 Tài khoản test có sẵn

| Username | Password | Full Name      |
|----------|----------|----------------|
| admin    | admin123 | Administrator  |
| user1    | test123  | User One       |
| user2    | test123  | User Two       |

---

## 🔧 Lỗi thường gặp

### ❌ "Cannot connect to server"
→ Chưa chạy Server. Chạy Server trước, Client sau!

### ❌ "Failed to connect to database"
→ MySQL chưa chạy hoặc password sai. Check `DatabaseManager.java`

### ❌ "Port 8888 already in use"
→ Server đã chạy rồi. Kill process hoặc đổi port trong code.

### ❌ Maven build failed
```bash
mvn clean install -U
```

---

## 📚 Xem thêm

- [README.md](README.md) - Hướng dẫn đầy đủ
- [ECLIPSE_SETUP.md](ECLIPSE_SETUP.md) - Setup Eclipse chi tiết
- [INTELLIJ_SETUP.md](INTELLIJ_SETUP.md) - Setup IntelliJ chi tiết

---

**Chúc bạn thành công! 🎉**

Có vấn đề? Tạo issue trên GitHub!
