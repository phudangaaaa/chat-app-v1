# Hướng dẫn chạy Chat Application trong IntelliJ IDEA

## Bước 1: Import Projects

1. Mở IntelliJ IDEA
2. File → Open
3. Browse đến thư mục `chat-app-v1`
4. Click OK
5. IntelliJ sẽ tự động detect Maven projects và import cả 2 projects

Hoặc:

1. File → New → Project from Existing Sources
2. Chọn `chat-app-v1`
3. Chọn "Import project from external model" → Maven
4. Next → Finish

## Bước 2: Cấu hình Database

1. Đảm bảo MySQL đang chạy
2. Chạy script tạo database:
```bash
mysql -u root -p < database/schema.sql
```

3. Cập nhật password trong:
`ChatServer/src/main/java/com/chatapp/server/util/DatabaseManager.java`

```java
private static final String DB_PASSWORD = "your_password"; // Thay password
```

## Bước 3: Chạy Server

### Cách 1: Run Main Class (Simple)
1. Mở file `ChatServer/src/main/java/com/chatapp/server/ChatServer.java`
2. Click vào nút Run (▶️) bên cạnh class `ChatServer`
3. Hoặc Right-click → Run 'ChatServer.main()'
4. Server sẽ chạy trên port 8888

### Cách 2: Maven Run Configuration
1. View → Tool Windows → Maven
2. Expand ChatServer → Plugins → exec
3. Double-click `exec:java`

## Bước 4: Chạy Client

⚠️ **QUAN TRỌNG**: JavaFX cần cấu hình đặc biệt trong IntelliJ!

### Cách 1: Sử dụng Maven (KHUYẾN NGHỊ - Không lỗi JavaFX)

1. View → Tool Windows → Maven
2. Expand ChatClient → Plugins → javafx
3. Double-click `javafx:run`
4. ✅ Client sẽ chạy không lỗi!

### Cách 2: Run Main Class với VM Options

1. Mở file `ChatClientApp.java`
2. Right-click → Run 'ChatClientApp.main()'
3. Nếu gặp lỗi "JavaFX runtime components are missing":

**Cấu hình VM Options:**

1. Run → Edit Configurations...
2. Chọn Application → ChatClientApp
3. Trong "VM options", thêm:

```
--module-path /path/to/javafx-sdk-19/lib --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web
```

4. Thay `/path/to/javafx-sdk-19` bằng đường dẫn thực tế
5. Hoặc nếu dùng Maven dependencies (IntelliJ tự động):

```
--add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web
```

6. Click OK → Run

### Cách 3: Cấu hình JavaFX SDK trong IntelliJ

Nếu muốn dùng external JavaFX SDK:

1. **Tải JavaFX SDK:**
   - https://gluonhq.com/products/javafx/
   - Tải version 19
   - Extract vào thư mục

2. **Thêm Library:**
   - File → Project Structure → Libraries
   - Click + → Java
   - Browse đến `javafx-sdk-19/lib`
   - Chọn tất cả .jar files
   - Click OK

3. **Add to Module:**
   - File → Project Structure → Modules
   - Chọn ChatClient
   - Tab Dependencies
   - Click + → Library → JavaFX-19
   - Click OK

4. **Thêm VM Options như Cách 2**

## Bước 5: Tạo Run Configurations (One-click run)

### Run Configuration cho Server:

1. Run → Edit Configurations...
2. Click + → Application
3. Name: `ChatServer`
4. Main class: `com.chatapp.server.ChatServer`
5. Use classpath of module: `ChatServer`
6. Click OK

### Run Configuration cho Client:

**Option A: Maven (Recommended)**
1. Run → Edit Configurations...
2. Click + → Maven
3. Name: `ChatClient (Maven)`
4. Working directory: `$ProjectFileDir$/ChatClient`
5. Command line: `clean javafx:run`
6. Click OK

**Option B: Application (với VM options)**
1. Run → Edit Configurations...
2. Click + → Application
3. Name: `ChatClient (JavaFX)`
4. Main class: `com.chatapp.client.ChatClientApp`
5. Use classpath of module: `ChatClient`
6. VM options: `--add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web`
7. Click OK

## Bước 6: Chạy và Test

1. **Chạy Server:**
   - Chọn "ChatServer" configuration
   - Click Run (▶️)

2. **Chạy Client:**
   - Chọn "ChatClient (Maven)" configuration
   - Click Run (▶️)
   - Có thể chạy nhiều instances: Run → Edit Configurations → Allow multiple instances

3. **Login:**
   - Username: `admin` / Password: `admin123`
   - Username: `user1` / Password: `test123`

## Xử lý lỗi trong IntelliJ

### ❌ Lỗi: "JavaFX runtime components are missing"

**Giải pháp 1 (KHUYẾN NGHỊ):**
- Dùng Maven run: `mvn javafx:run`

**Giải pháp 2:**
- Thêm VM options: `--add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web`

**Giải pháp 3:**
- File → Invalidate Caches → Invalidate and Restart
- Reimport Maven project

### ❌ Lỗi: "Module not found" hoặc Import errors

**Giải pháp:**
```bash
cd ChatClient
mvn clean install

cd ../ChatServer
mvn clean install
```

Sau đó trong IntelliJ:
- Right-click project root → Maven → Reload Project

### ❌ Lỗi: "Cannot resolve symbol" cho JavaFX classes

**Giải pháp:**
- File → Project Structure → Modules → ChatClient
- Tab Dependencies
- Đảm bảo có JavaFX dependencies
- Click Apply

### ❌ Lỗi Database Connection

**Giải pháp:**
- Kiểm tra MySQL service đang chạy
- Verify database name: `chat_app_db`
- Check username/password trong `DatabaseManager.java`

## Tips & Tricks

### Chạy nhanh với Shortcuts:
- **Shift + F10**: Run selected configuration
- **Ctrl + Shift + F10**: Run current file
- **Alt + Shift + F10**: Choose configuration to run

### Debug Mode:
- Click Debug (🐞) thay vì Run
- Đặt breakpoints bằng cách click vào line numbers

### Multiple Clients:
1. Run → Edit Configurations
2. Chọn ChatClient configuration
3. Check "Allow multiple instances"
4. Có thể chạy nhiều clients để test chat

### Maven Commands nhanh:

Trong Terminal (View → Tool Windows → Terminal):

```bash
# Server
cd ChatServer && mvn clean compile exec:java

# Client
cd ChatClient && mvn clean javafx:run
```

## Recommended IntelliJ Settings

### 1. Enable Auto-import:
- Settings → Editor → General → Auto Import
- Check "Add unambiguous imports on the fly"

### 2. JDK Configuration:
- File → Project Structure → Project
- Project SDK: 11 or higher
- Project language level: 11

### 3. Maven Settings:
- Settings → Build, Execution, Deployment → Build Tools → Maven
- Check "Always update snapshots"
- Maven home directory: Use bundled

---

**Chúc bạn code vui vẻ với IntelliJ IDEA! 🚀**
