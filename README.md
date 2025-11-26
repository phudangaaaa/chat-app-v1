# Chat Application - Ứng dụng Chat Desktop giống Zalo

Ứng dụng chat desktop được phát triển bằng Java, JavaFX, MySQL với đầy đủ các tính năng:

## 🚀 Tính năng

### 👤 Quản lý người dùng
- ✅ Đăng ký tài khoản (username, email, password, full name)
- ✅ Đăng nhập/Đăng xuất
- ✅ Cập nhật profile (full name, status message)
- ✅ Cập nhật trạng thái (ONLINE, OFFLINE, AWAY, BUSY)

### 👥 Quản lý bạn bè
- ✅ Tìm kiếm người dùng theo keyword
- ✅ Gửi/Nhận lời mời kết bạn
- ✅ Chấp nhận/Từ chối lời mời kết bạn
- ✅ Xem danh sách bạn bè
- ✅ Xem profile người dùng khác
- ✅ Thông báo online/offline của bạn bè

### 💬 Tin nhắn
- ✅ Chat riêng tư (1:1) với bạn bè
- ✅ Chat nhóm với nhiều thành viên
- ✅ Nhiều loại tin nhắn: TEXT, IMAGE, FILE, VIDEO, AUDIO
- ✅ Gửi file và hình ảnh
- ✅ Hỗ trợ emoji
- ✅ Hiển thị lịch sử tin nhắn

### 👥 Quản lý nhóm
- ✅ Tạo nhóm chat mới
- ✅ Tham gia nhóm chat
- ✅ Xem danh sách nhóm
- ✅ Chat trong nhóm

### 📞 Cuộc gọi
- ✅ Khởi tạo cuộc gọi (voice/video)
- ✅ Nhận cuộc gọi đến
- ✅ Chấp nhận/Từ chối cuộc gọi
- ⚠️ **Lưu ý**: Tính năng gọi voice/video chỉ ở dạng cơ bản, cần tích hợp WebRTC để hoàn thiện

## 🛠️ Công nghệ sử dụng

- **Backend (Server):**
  - Java 11+
  - Socket Programming
  - MySQL Database
  - BCrypt (password hashing)
  - Gson (JSON processing)
  - SLF4J (logging)

- **Frontend (Client):**
  - JavaFX 19
  - Maven
  - Gson
  - Emoji-java library

## 📋 Yêu cầu hệ thống

- JDK 11 hoặc cao hơn
- MySQL 5.7 hoặc cao hơn
- Maven 3.6+
- Eclipse IDE hoặc IntelliJ IDEA (khuyến nghị)

## 📦 Cài đặt

### 1. Cài đặt Database

```bash
# Đăng nhập MySQL
mysql -u root -p

# Chạy script tạo database
source database/schema.sql

# Hoặc import trực tiếp
mysql -u root -p < database/schema.sql
```

**Lưu ý:** Cập nhật thông tin kết nối database trong file:
```
ChatServer/src/main/java/com/chatapp/server/util/DatabaseManager.java
```

Sửa các dòng:
```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "your_password_here"; // Thay đổi password của bạn
```

### 2. Build Project với Maven

#### Build Server:
```bash
cd ChatServer
mvn clean install
```

#### Build Client:
```bash
cd ChatClient
mvn clean install
```

## ▶️ Chạy ứng dụng

### Cách 1: Chạy từ Eclipse/IntelliJ

#### 1. Import Projects vào Eclipse/IntelliJ:
- File → Import → Existing Maven Projects
- Chọn thư mục `ChatServer` và `ChatClient`

#### 2. Chạy Server:
- Mở class `ChatServer.java`
- Right-click → Run As → Java Application
- Server sẽ chạy trên port 8888

#### 3. Chạy Client:
- Mở class `ChatClientApp.java`
- Right-click → Run As → Java Application
- Có thể chạy nhiều Client cùng lúc để test

### Cách 2: Chạy từ Command Line

#### 1. Chạy Server:
```bash
cd ChatServer
mvn exec:java -Dexec.mainClass="com.chatapp.server.ChatServer"
```

#### 2. Chạy Client:
```bash
cd ChatClient
mvn javafx:run
```

## 👥 Tài khoản test có sẵn

Database đã có sẵn một số tài khoản để test:

| Username | Password | Full Name      |
|----------|----------|----------------|
| admin    | admin123 | Administrator  |
| user1    | test123  | User One       |
| user2    | test123  | User Two       |

## 📖 Hướng dẫn sử dụng

### 1. Đăng ký tài khoản mới
- Mở ứng dụng Client
- Click "Register here"
- Điền đầy đủ thông tin
- Click "Register"

### 2. Đăng nhập
- Nhập username và password
- Click "Login"

### 3. Tìm kiếm và kết bạn
- Trong Main window, nhập keyword vào ô "Search"
- Click "Search"
- Chọn người dùng và click "Add Friend"
- Người nhận sẽ nhận được thông báo và có thể chấp nhận/từ chối

### 4. Chat 1:1
- Trong tab "Friends", double-click vào tên bạn bè
- Cửa sổ chat sẽ mở
- Gõ tin nhắn và nhấn Enter hoặc click "Send"

### 5. Tạo nhóm chat
- Click "Create Group"
- Nhập tên nhóm
- Mời bạn bè vào nhóm

### 6. Chat nhóm
- Trong tab "Groups", double-click vào tên nhóm
- Chat như bình thường

### 7. Gửi file/hình ảnh
- Trong cửa sổ chat, click "Send File"
- Chọn file cần gửi
- File sẽ được upload lên server

### 8. Gọi voice/video
- Trong chat 1:1, click "Voice Call" hoặc "Video Call"
- Người nhận sẽ được thông báo

## 🔧 Cấu hình

### Thay đổi Server Host/Port

Trong file `ChatClient/src/main/java/com/chatapp/client/service/NetworkManager.java`:

```java
private static final String SERVER_HOST = "localhost"; // Thay đổi IP server
private static final int SERVER_PORT = 8888;          // Thay đổi port
```

Trong file `ChatServer/src/main/java/com/chatapp/server/ChatServer.java`:

```java
private static final int PORT = 8888; // Thay đổi port
```

## 🏗️ Cấu trúc dự án

```
chat-app-v1/
├── ChatServer/                 # Server application
│   ├── src/main/java/
│   │   └── com/chatapp/server/
│   │       ├── ChatServer.java        # Main server class
│   │       ├── handler/               # Client request handlers
│   │       ├── model/                 # Data models
│   │       ├── service/               # Business logic services
│   │       └── util/                  # Utilities (DB, Password, File)
│   ├── pom.xml
│   └── uploads/                # Uploaded files directory
│
├── ChatClient/                # Client application
│   ├── src/main/java/
│   │   └── com/chatapp/client/
│   │       ├── ChatClientApp.java    # Main client class
│   │       ├── controller/           # JavaFX controllers
│   │       ├── model/                # Data models
│   │       ├── service/              # Network services
│   │       └── util/                 # Utilities (Session)
│   ├── src/main/resources/
│   │   ├── fxml/                     # FXML UI files
│   │   └── css/                      # Stylesheets
│   └── pom.xml
│
├── database/
│   └── schema.sql             # Database schema
└── README.md                  # This file
```

## 🐛 Xử lý lỗi thường gặp

### 1. Không kết nối được Server
- Kiểm tra Server đã chạy chưa
- Kiểm tra firewall
- Kiểm tra HOST và PORT trong NetworkManager

### 2. Lỗi Database
- Kiểm tra MySQL đã chạy chưa
- Kiểm tra username/password trong DatabaseManager
- Kiểm tra database đã được tạo chưa

### 3. Lỗi JavaFX
- Đảm bảo đã cài đặt JavaFX SDK
- Kiểm tra version JavaFX trong pom.xml

### 4. Lỗi build Maven
```bash
# Clean và build lại
mvn clean install -U
```

## 📝 Tính năng có thể mở rộng

- [ ] Mã hóa end-to-end cho tin nhắn
- [ ] Tích hợp WebRTC cho video call thực sự
- [ ] Gửi location, stickers
- [ ] Typing indicator
- [ ] Message read receipts (seen)
- [ ] Push notifications
- [ ] Dark mode
- [ ] Multi-language support

## 📄 License

Dự án được phát triển cho mục đích học tập.

## 👨‍💻 Liên hệ

Nếu có vấn đề hoặc câu hỏi, vui lòng tạo issue trên GitHub.

---

**Chúc bạn sử dụng ứng dụng vui vẻ! 🎉**
