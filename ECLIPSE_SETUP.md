# Hướng dẫn chạy Chat Application trong Eclipse

## Bước 1: Import Projects

1. Mở Eclipse
2. File → Import → Maven → Existing Maven Projects
3. Browse đến thư mục `chat-app-v1`
4. Chọn cả 2 projects: **ChatServer** và **ChatClient**
5. Click Finish

## Bước 2: Cấu hình Database

1. Đảm bảo MySQL đang chạy
2. Mở MySQL Workbench hoặc terminal:
```sql
mysql -u root -p
source /path/to/chat-app-v1/database/schema.sql
```

3. Cập nhật thông tin kết nối trong file:
`ChatServer/src/main/java/com/chatapp/server/util/DatabaseManager.java`

Sửa dòng:
```java
private static final String DB_PASSWORD = "your_password"; // Thay password của bạn
```

## Bước 3: Chạy Server

### Cách 1: Run as Java Application (Simple)
1. Mở file `ChatServer/src/main/java/com/chatapp/server/ChatServer.java`
2. Right-click → Run As → Java Application
3. Server sẽ chạy trên port 8888

### Cách 2: Maven Build (Recommended)
1. Right-click vào project **ChatServer**
2. Run As → Maven build...
3. Goals: `clean compile exec:java`
4. Add parameter:
   - Name: `exec.mainClass`
   - Value: `com.chatapp.server.ChatServer`
5. Click Run

## Bước 4: Chạy Client

⚠️ **QUAN TRỌNG**: JavaFX cần cấu hình đặc biệt!

### Cách 1: Sử dụng Maven (KHUYẾN NGHỊ - Không bị lỗi JavaFX)

1. Right-click vào project **ChatClient**
2. Run As → Maven build...
3. Goals: `clean javafx:run`
4. Click Run
5. ✅ Client sẽ chạy không lỗi!

### Cách 2: Run as Java Application (Cần cấu hình VM Arguments)

Nếu chạy trực tiếp `ChatClientApp.java` sẽ bị lỗi "JavaFX runtime components are missing".

**Fix bằng cách thêm VM Arguments:**

1. Right-click `ChatClientApp.java` → Run As → Run Configurations...
2. Chọn Java Application → ChatClientApp
3. Tab "Arguments"
4. Trong VM arguments, thêm:

**Cho Windows:**
```
--module-path "C:\Path\To\javafx-sdk-19\lib" --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web
```

**Cho Linux/Mac:**
```
--module-path /path/to/javafx-sdk-19/lib --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web
```

5. Thay `/path/to/javafx-sdk-19` bằng đường dẫn thực tế đến JavaFX SDK của bạn
6. Click Apply → Run

### Cách 3: Tải JavaFX SDK và cấu hình Eclipse

Nếu chưa có JavaFX SDK:

1. **Tải JavaFX SDK:**
   - Truy cập: https://gluonhq.com/products/javafx/
   - Tải version 19 (hoặc 17, 11 tùy JDK của bạn)
   - Extract vào thư mục (ví dụ: `C:\javafx-sdk-19`)

2. **Cấu hình User Library trong Eclipse:**
   - Window → Preferences → Java → Build Path → User Libraries
   - Click New, tên: `JavaFX19`
   - Click Add External JARs
   - Browse đến `javafx-sdk-19/lib`
   - Chọn TẤT CẢ file .jar
   - Click OK

3. **Add Library vào ChatClient project:**
   - Right-click ChatClient project → Build Path → Configure Build Path
   - Tab Libraries → Add Library → User Library
   - Chọn JavaFX19 → Finish

4. **Thêm VM Arguments như Cách 2**

## Bước 5: Test Application

1. **Chạy Server trước**
2. **Chạy Client** (có thể chạy nhiều Client cùng lúc để test chat)
3. **Login với tài khoản test:**
   - Username: `admin` / Password: `admin123`
   - Username: `user1` / Password: `test123`

## Xử lý lỗi thường gặp

### ❌ Lỗi: "JavaFX runtime components are missing"
**Nguyên nhân:** Chạy trực tiếp ChatClientApp.java mà không có JavaFX modules

**Giải pháp:**
- ✅ **KHUYẾN NGHỊ**: Dùng Maven → `mvn javafx:run` (Cách 1)
- Hoặc thêm VM arguments (Cách 2)

### ❌ Lỗi: "Error: Could not find or load main class"
**Giải pháp:**
```bash
cd ChatClient
mvn clean install
```

### ❌ Lỗi: "Failed to connect to database"
**Giải pháp:**
- Kiểm tra MySQL đang chạy
- Kiểm tra username/password trong `DatabaseManager.java`
- Kiểm tra database `chat_app_db` đã được tạo

### ❌ Lỗi: "Cannot connect to server"
**Giải pháp:**
- Đảm bảo Server đã chạy trước
- Kiểm tra port 8888 chưa bị chiếm
- Tắt firewall tạm thời để test

## Script chạy nhanh (Terminal)

Nếu không muốn dùng Eclipse, dùng scripts có sẵn:

### Linux/Mac:
```bash
# Terminal 1 - Server
./run-server.sh

# Terminal 2 - Client
./run-client.sh
```

### Windows:
```cmd
REM Terminal 1 - Server
run-server.bat

REM Terminal 2 - Client
run-client.bat
```

## Lưu ý

- **Server phải chạy trước, Client chạy sau**
- Có thể chạy nhiều Client cùng lúc
- Port mặc định: 8888 (có thể thay đổi trong code)
- Database mặc định: `chat_app_db` trên localhost:3306

---

Chúc bạn thành công! 🚀
