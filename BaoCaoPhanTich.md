# BÁO CÁO PHÂN TÍCH LỖI: CACHE KHÔNG HOẠT ĐỘNG (THIẾU @EnableCaching)

## 1. Xác định nguyên nhân cache không hoạt động

### Annotation bị thiếu
Trong file cấu hình chính `Application.java`, hệ thống đang thiếu annotation **`@EnableCaching`**.

### Cơ chế AOP Proxy và lý do `@Cacheable` là chưa đủ
Spring Cache hoạt động dựa trên cơ chế **AOP (Aspect-Oriented Programming)** thông qua các Proxy.
Khi bạn thêm `@Cacheable` vào một phương thức (ví dụ `getUserById`), Spring sẽ không trực tiếp gọi vào object `UserService` gốc. Thay vào đó, nó tạo ra một **Proxy object** bọc bên ngoài `UserService`.
- Proxy này chứa một `CacheInterceptor`.
- Khi client gọi `getUserById`, request đi qua Proxy trước. `CacheInterceptor` sẽ kiểm tra xem trong Cache đã có dữ liệu cho key này chưa (Cache hit).
- Nếu có, nó trả về dữ liệu ngay lập tức mà không gọi vào method gốc.
- Nếu không (Cache miss), nó mới gọi method gốc, lấy kết quả từ Database, lưu vào Cache, rồi mới trả về.

Tuy nhiên, **Proxy này chỉ được Spring tạo ra khi tính năng caching được kích hoạt rõ ràng**. Việc chỉ khai báo `@Cacheable` giống như bạn dán nhãn "cần được cache", nhưng nếu không có `@EnableCaching` thì Spring Boot sẽ không bật `CacheInterceptor` và không tạo Proxy cho các method đó.

### Kết quả
Do thiếu `@EnableCaching`, method `getUserById` được thực thi như một method Java bình thường, không có bất kỳ bước kiểm tra cache nào chặn trước. Hậu quả là **mỗi lần gọi API hệ thống đều gọi trực tiếp xuống Database**, khiến thời gian phản hồi luôn ở mức 200ms.

---

## 2. Cách sửa mã nguồn

- **Kích hoạt Caching**: Đã thêm `@EnableCaching` vào file `Application.java` (hoặc có thể thêm vào một class `@Configuration` riêng).
- **Cấu hình CacheManager**: Tạo file `CacheConfig.java` để khai báo `ConcurrentMapCacheManager` làm backend lưu trữ cho bộ đệm "users".
- **Sửa method getUserById**: Cập nhật logic ở `UserService` để an toàn hơn:
  - Ném lỗi (fail-fast) nếu `userId` truyền vào là null hoặc rỗng.

---

## 3. Xử lý các tình huống đặc biệt

### a) Tham số userId là null hoặc rỗng
- **Nguy cơ**: Sinh ra các key rác (null, "") trong cache, làm lãng phí bộ nhớ và có thể gây lỗi hệ thống.
- **Giải pháp**: 
  - Khai báo điều kiện trong annotation: `@Cacheable(..., condition = "#userId != null and !#userId.trim().isEmpty()")`. Spring sẽ bỏ qua việc cache nếu điều kiện này false.
  - Kết hợp với việc validate (fail-fast) ngay trong method: `if (userId == null || userId.trim().isEmpty()) throw new IllegalArgumentException(...);` để chặn lỗi ngay từ đầu.

### b) Phương thức trả về null (Ví dụ không tìm thấy user)
- **Có nên lưu kết quả null vào cache không?**
  - *Nhược điểm*: Gây lãng phí dung lượng bộ nhớ vô ích.
  - *Ưu điểm (Nên dùng)*: Nếu hệ thống hay bị tấn công vét cạn bằng các ID ảo (Cache Penetration), việc cache giá trị null (với TTL ngắn) sẽ giúp chặn các request ảo này đập xuống Database liên tục.
- **Cách xử lý**: 
  - Nếu **không muốn** cache kết quả null: Sử dụng thuộc tính `unless` của Spring Cache: `@Cacheable(..., unless = "#result == null")`. Lệnh này báo cho Spring biết: "Hãy cache kết quả này, TRỪ KHI kết quả trả về là null".
  - Nếu **muốn** cache giá trị null để tránh Cache Penetration: Có thể cho phép cache bình thường nhưng set cấu hình TTL ngắn lại, hoặc sử dụng `RedisCacheConfiguration.defaultCacheConfig()` nếu dùng Redis (mặc định Redis cho phép cache null, tuy nhiên Spring Data Redis version mới thường khuyến cáo dùng `.disableCachingNullValues()` hoặc cân nhắc kỹ). Ở bài này, với `ConcurrentMapCacheManager`, ta chọn phương án an toàn là **không cache kết quả null** bằng `unless = "#result == null"`.

---

## 4. Test Case
Một test case `UserServiceTest` đã được viết sử dụng `@SpringBootTest` kết hợp với `@MockBean` (Mockito) để verify hành vi:
1. Mock phương thức `findById` của `UserRepository`.
2. Gọi `userService.getUserById("U001")` lần 1: Bộ đệm chưa có -> Phải gọi DB.
3. Gọi `userService.getUserById("U001")` lần 2: Bộ đệm đã có -> Trả về kết quả ngay, không gọi DB.
4. Xác minh (verify) bằng Mockito rằng `userRepository.findById` chỉ được kích hoạt đúng 1 lần duy nhất trong toàn bộ chu kỳ test.
