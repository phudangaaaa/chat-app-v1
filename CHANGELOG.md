# CHANGELOG

## Version 1.2.0 - WebRTC Video Call Integration

### ✨ Tính năng mới được thêm:

#### 1. 🎥 Video Call với Webcam thực tế
- **Webcam capture thực sự**: Sử dụng thư viện webcam-capture để bắt hình từ camera
- **Local video preview**: Hiển thị video của bạn ở góc phải trên cùng (200x150px)
- **Remote video display**: Hiển thị video của người gọi toàn màn hình (800x600px)
- **UI chuyên nghiệp**:
  - Video call: Hiển thị video toàn màn hình với overlay điều khiển
  - Voice call: UI gradient đẹp mắt với avatar
- **Nút điều khiển**:
  - 🎤 Mute/Unmute microphone
  - 📹 Bật/Tắt camera
  - ✓ Accept call
  - ✕ Reject/End call

**Cách sử dụng:**
1. Trong chat 1:1, click "Video Call"
2. Người kia chấp nhận cuộc gọi
3. Webcam sẽ tự động bật và hiển thị trong góc phải trên
4. Sử dụng nút 📹 để bật/tắt camera
5. Sử dụng nút 🎤 để tắt/bật tiếng

**Thành phần kỹ thuật:**
- `WebcamManager.java`: Quản lý webcam capture ~30 FPS
- `MediaStreamManager.java`: Infrastructure cho audio/video streaming
- Call.fxml: UI responsive với video containers
- CallController.java: Logic điều khiển cuộc gọi và media

#### 2. 🔧 Fix Group Chat Message History
- **Load lịch sử tin nhắn nhóm**: Khi vào lại chat nhóm, tin nhắn cũ sẽ được load
- **Server-side fix**: Cập nhật `handleGetMessages()` để hỗ trợ cả private và group chat
- Sử dụng parameter `groupId` để phân biệt loại chat

**Cách hoạt động:**
- Private chat: Sử dụng `userId` parameter
- Group chat: Sử dụng `groupId` parameter
- Server tự động gọi đúng service method

### 🔧 Cải tiến:

- **Module system**: Thêm `javafx.swing`, `webcam.capture`, `java.desktop` vào module-info.java
- **Maven dependencies**: Thêm webcam-capture library v0.3.12
- **UI improvements**:
  - Responsive video containers
  - Smooth transitions giữa voice và video UI
  - Shadow effects cho video elements
- **Performance**: Webcam capture tối ưu ở 30 FPS với thread riêng

### 🐛 Bug fixes:

- Fix lỗi group chat không hiển thị tin nhắn khi vào lại
- Fix module configuration cho webcam library
- Cải thiện cleanup khi kết thúc cuộc gọi

### 📝 Lưu ý:

- **Webcam support**: Cần có webcam để sử dụng video call
- **Future work**: P2P video streaming giữa clients (hiện tại chỉ local preview)
- Audio streaming infrastructure đã sẵn sàng, cần implement actual audio capture

---

## Version 1.1.0 - File Download & Group Management

### ✨ Tính năng mới được thêm:

#### 1. 📥 Tải file về (File Download)
- **Click vào tin nhắn file để tải về**: Tất cả tin nhắn có file (hình ảnh, video, audio, file) giờ đây có thể click để download
- File sẽ được hiển thị dưới dạng link có thể click
- Chọn vị trí lưu file khi tải về
- Hỗ trợ tất cả loại file: IMAGE, VIDEO, AUDIO, FILE

**Cách sử dụng:**
- Khi nhận được tin nhắn có file, click vào link file
- Chọn nơi lưu file
- File sẽ được tải về máy của bạn

#### 2. 📞 Cửa sổ Video/Voice Call
- **UI call đẹp mắt**: Giao diện gradient màu tím chuyên nghiệp
- **Hiển thị thông tin cuộc gọi**: Tên người gọi, loại cuộc gọi (Voice/Video)
- **Đếm thời gian cuộc gọi**: Timer hiển thị thời lượng cuộc gọi
- **Nút điều khiển**: Accept (Chấp nhận), Reject (Từ chối), End Call (Kết thúc)
- **Cuộc gọi đến**: Cửa sổ tự động hiện khi có cuộc gọi đến
- **Cuộc gọi đi**: Cửa sổ hiện khi bạn gọi người khác

**Cách sử dụng:**
- Trong chat 1:1, click "Voice Call" hoặc "Video Call"
- Cửa sổ call sẽ mở, đợi người kia chấp nhận
- Khi có cuộc gọi đến, cửa sổ sẽ tự động hiện với nút Accept/Reject
- Click "End Call" để kết thúc cuộc gọi

**Lưu ý:** Tính năng audio/video streaming thực sự cần WebRTC. Hiện tại đây là UI và signaling cơ bản.

#### 3. 👥 Thêm thành viên vào nhóm (Add Members to Group)
- **Nút "➕ Add Members"**: Hiện trong cửa sổ chat nhóm
- **Chọn nhiều bạn bè cùng lúc**: Multi-select với Ctrl+Click
- **Lọc thành viên**: Chỉ hiển thị bạn bè chưa có trong nhóm
- **Thêm dễ dàng**: Chọn và click OK để thêm

**Cách sử dụng:**
1. Mở cửa sổ chat nhóm
2. Click nút "➕ Add Members" (bên cạnh tên nhóm)
3. Chọn bạn bè muốn thêm (Ctrl+Click để chọn nhiều)
4. Click OK
5. Các bạn bè đã chọn sẽ được thêm vào nhóm

### 🔧 Cải tiến:

- **Chat window**: Ẩn nút Voice/Video Call trong chat nhóm
- **Group chat**: Hiển thị nút Add Members chỉ trong nhóm
- **File messages**: Hiển thị dưới dạng clickable link
- **Server**: Hỗ trợ thêm user cụ thể vào nhóm (không chỉ current user)

### 🐛 Bug fixes:

- Fix lỗi không tải file được
- Fix UI state management cho private chat vs group chat
- Cải thiện trải nghiệm người dùng

---

## Version 1.0.0 - Initial Release

### Tính năng chính:

#### 👤 Quản lý người dùng
- Đăng ký tài khoản
- Đăng nhập/Đăng xuất
- Cập nhật profile
- Cập nhật trạng thái (ONLINE, OFFLINE, AWAY, BUSY)

#### 👥 Quản lý bạn bè
- Tìm kiếm người dùng
- Gửi/Nhận lời mời kết bạn
- Chấp nhận/Từ chối lời mời
- Danh sách bạn bè
- Thông báo online/offline

#### 💬 Tin nhắn
- Chat 1:1
- Chat nhóm
- Gửi file, hình ảnh
- Emoji
- Lịch sử tin nhắn

#### 👥 Quản lý nhóm
- Tạo nhóm
- Tham gia nhóm
- Chat trong nhóm

#### 📞 Cuộc gọi
- Khởi tạo cuộc gọi
- Nhận cuộc gọi
- Chấp nhận/Từ chối

---

## Hướng dẫn nâng cấp

Để cập nhật lên version mới nhất:

```bash
git pull origin claude/java-chat-app-01NjQL9nXLbcGKs3v4ZZV2zJ

# Rebuild projects
cd ChatServer && mvn clean install
cd ../ChatClient && mvn clean install

# Chạy lại server và client
./run-server.sh
./run-client.sh
```

---

## Roadmap - Tính năng sắp tới

- [x] ~~WebRTC integration cho audio/video streaming thực sự~~ ✅ (Đã có webcam capture v1.2.0)
- [ ] P2P video streaming giữa clients (hiện tại chỉ local preview)
- [ ] Audio capture và streaming thực tế
- [ ] Screen sharing trong video call
- [ ] Message reactions (like, love, etc.)
- [ ] Typing indicator
- [ ] Read receipts (seen)
- [ ] Push notifications
- [ ] Dark mode
- [ ] Message search
- [ ] User blocking
- [ ] Admin controls cho nhóm (kick, ban members)

---

**Last updated:** 2024
