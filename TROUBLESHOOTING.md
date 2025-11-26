# Troubleshooting Guide - Hướng dẫn xử lý lỗi

## 🐛 Group Chat không hiển thị tin nhắn cũ

### Nguyên nhân:
1. Code cũ chưa được rebuild
2. Database chưa có messages hoặc messages không có `group_id`
3. Lỗi network/server

### Cách kiểm tra:

#### 1. Rebuild cả server và client
```bash
# Stop server và client nếu đang chạy

# Rebuild server
cd ChatServer
mvn clean install

# Rebuild client
cd ../ChatClient
mvn clean install
```

#### 2. Kiểm tra logs

**Client logs** (khi mở group chat):
```
Loading group messages for group ID: 1
Received X group messages
```

**Server logs** (khi client request messages):
```
Getting group messages for group 1 (user: 2)
Retrieved X group messages for group 1
```

#### 3. Kiểm tra database
```sql
-- Xem messages trong group
SELECT * FROM messages WHERE group_id = 1;

-- Đếm số messages
SELECT COUNT(*) FROM messages WHERE group_id = 1;

-- Kiểm tra messages có groupId NULL
SELECT COUNT(*) FROM messages WHERE group_id IS NULL AND receiver_id IS NULL;
```

#### 4. Test case:
1. Đăng nhập 2 user
2. Tạo group chat mới
3. Gửi vài tin nhắn
4. Thoát group chat
5. Vào lại → Tin nhắn phải hiển thị

### Fix:

**Nếu messages cũ không có groupId**, chạy SQL để fix:
```sql
-- Update old messages (nếu cần)
-- CẢNH BÁO: Chỉ chạy nếu bạn biết mình đang làm gì!
-- UPDATE messages SET group_id = X WHERE ...;
```

**Nếu vẫn lỗi**, xóa messages cũ và test lại:
```sql
DELETE FROM messages WHERE group_id = X;
```

---

## 🎥 Video Call không hoạt động

### Nguyên nhân:
1. Webcam library chưa được download (network issues)
2. Không có webcam trên máy
3. Webcam đang được sử dụng bởi app khác
4. Module configuration sai
5. JavaFX runtime issues

### Cách kiểm tra:

#### 1. Kiểm tra webcam có hoạt động không
- Mở Camera app trên Windows/Mac/Linux
- Đảm bảo webcam không bị app khác chiếm

#### 2. Rebuild với dependencies
```bash
cd ChatClient

# Clean và force update dependencies
mvn clean install -U

# Nếu network issues, enable offline mode SAU KHI đã download dependencies lần đầu
mvn compile -o
```

#### 3. Kiểm tra logs

**Client logs khi mở call window**:
```
WebcamManager initialized successfully
Webcam capture started
```

**Nếu thấy lỗi**:
```
Failed to initialize WebcamManager: ...
No webcam available or WebcamManager not initialized!
```

#### 4. Test webcam riêng

Tạo test file `TestWebcam.java`:
```java
import com.github.sarxos.webcam.Webcam;

public class TestWebcam {
    public static void main(String[] args) {
        System.out.println("Detecting webcams...");
        for (Webcam webcam : Webcam.getWebcams()) {
            System.out.println("Found: " + webcam.getName());
        }

        Webcam webcam = Webcam.getDefault();
        if (webcam != null) {
            System.out.println("Default webcam: " + webcam.getName());
            webcam.open();
            System.out.println("Webcam opened successfully!");
            webcam.close();
        } else {
            System.out.println("No webcam found!");
        }
    }
}
```

Run:
```bash
javac -cp "target/classes:target/dependency/*" TestWebcam.java
java -cp ".:target/classes:target/dependency/*" TestWebcam
```

#### 5. Kiểm tra module-info.java
Đảm bảo có các modules sau:
```java
requires javafx.swing;
requires webcam.capture;
requires java.desktop;
```

### Fix:

**Fix 1: Download dependencies lại**
```bash
cd ChatClient
rm -rf ~/.m2/repository/com/github/sarxos
mvn dependency:purge-local-repository
mvn clean install
```

**Fix 2: Nếu không có webcam**
- Video call sẽ mở nhưng không hiển thị local preview
- Đây là behavior bình thường
- Log sẽ hiển thị: "No webcam available"

**Fix 3: Module issues**
```bash
# Chạy với JavaFX plugin
cd ChatClient
mvn javafx:run
```

**Fix 4: Nếu call window không mở**
- Check logs trong console
- Kiểm tra `Call.fxml` có load được không
- Verify `CallController.java` không có compile errors

---

## 🔧 Build Issues

### Maven dependency download fails

**Lỗi**: `Could not transfer artifact ... Temporary failure in name resolution`

**Fix**:
```bash
# 1. Kiểm tra internet connection
ping repo.maven.apache.org

# 2. Retry với timeout lớn hơn
mvn clean install -Dmaven.wagon.http.retryHandler.count=3

# 3. Nếu đã download dependencies, dùng offline mode
mvn compile -o
```

### JavaFX runtime missing

**Lỗi**: `Error: JavaFX runtime components are missing`

**Fix**:
```bash
# ĐÚNG: Chạy với maven
mvn javafx:run

# SAI: Đừng run main class trực tiếp
java com.chatapp.client.ChatClientApp  # ❌
```

---

## 📝 How to get full logs

### Server logs:
```bash
cd ChatServer
mvn exec:java > server.log 2>&1
```

### Client logs:
```bash
cd ChatClient
mvn javafx:run > client.log 2>&1
```

---

## 🆘 Vẫn lỗi?

1. **Xóa build cache và rebuild**:
```bash
cd ChatServer && mvn clean
cd ../ChatClient && mvn clean
cd ..
```

2. **Check Java version**:
```bash
java -version  # Phải >= Java 17
mvn -version
```

3. **Restart MySQL**:
```bash
# Linux/Mac
sudo systemctl restart mysql

# Windows
net stop MySQL
net start MySQL
```

4. **Check MySQL connection**:
```bash
mysql -u root -p
> USE chat_app;
> SHOW TABLES;
> SELECT COUNT(*) FROM users;
```

5. **Collect logs**:
- Server logs
- Client logs
- MySQL error logs
- Stack traces

6. **Report issue** với đầy đủ thông tin:
- OS version
- Java version
- Maven version
- Full error logs
- Steps to reproduce
