# CHANGELOG

## Version 1.1.0 - Latest Updates

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

- [ ] WebRTC integration cho audio/video streaming thực sự
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
