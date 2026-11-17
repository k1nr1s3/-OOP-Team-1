# Quản lý cửa hàng thức ăn nhanh & giải khát (FAST FOOD & DRINK)

Đồ án môn **Lập trình hướng đối tượng** (Java). Ứng dụng console quản lý một cửa hàng đồ ăn nhanh + giải khát: bán hàng, hoá đơn, sản phẩm, khách hàng thân thiết, khuyến mãi, nhân viên (chấm công, lương), kho nguyên liệu và thống kê doanh thu.
> **Giảng viên:** Nguyễn Thanh Sang

> **Nhóm thực hiện: Nhóm 1**

| STT | Họ và tên | MSSV | Nội dung thực hiện |
|---|---|---|---|
| 29 | Huỳnh Nguyễn Ngọc Như | 3122410285 | HoaDon, ChiTietHoaDon, DanhSachHoaDon, lập hoá đơn, thanh toán, thống kê doanh thu theo ngày, tháng, sản phẩm, nhân viên |
| 32 | Lê Văn Phong | 3125560051 | NguyenLieu, NhaCungCap, PhieuNhapKho, DanhSachNguyenLieu, interface IFile, FileHelper, cảnh báo hết hàng, báo cáo tổng hợp |
| 36 | Trần Võ Đại Thắng | 3125560061 | SanPham, DoAn, ThucUong, Combo, DanhSachSanPham, interface IQuanLy, menu Main, vẽ sơ đồ class, ghép code |
| 40 | Thạch Thanh Triển | 3125720030 | NhanVien, ThuNgan, PhaChe, QuanLy, DanhSachNhanVien, tính lương, chấm công, tìm kiếm và thống kê nhân viên |
| 41 | Lê Tấn Trung | 3125720031 | KhachHang, KhachHangThanThiet, KhuyenMai, interface IGiamGia, DanhSachKhachHang, tích điểm, phân hạng |



---

## 1. Yêu cầu & cách chạy

- JDK 17 trở lên (đã kiểm tra với JDK 21).

**Windows:**  `run.bat` 
**Biên dịch lại và chạy thủ công:**

```bash
cd src
javac -encoding UTF-8 -d out $(find . -name "*.java")   # PowerShell: dùng Get-ChildItem thay cho find
java -Dfile.encoding=UTF-8 -cp out Main
```

Khi mở chương trình, toàn bộ dữ liệu trong `data/` được đọc tự động. Chọn **8. Lưu tất cả dữ liệu** hoặc trả lời `c` khi thoát để ghi lại.

---

## 2. Chức năng

| Menu | Chức năng |
|---|---|
| 1. Bán hàng | Lập hoá đơn, gọi thêm món, bớt/xoá món, xem bill, thanh toán (tiền mặt / chuyển khoản / thẻ / ví), huỷ hoá đơn, xem thực đơn |
| 2. Hoá đơn & thống kê | Lọc hoá đơn theo trạng thái / nhân viên / khách / ngày; báo cáo doanh thu theo ngày, tháng, năm; thống kê theo sản phẩm, nhân viên, phương thức |
| 3. Sản phẩm | Nhập n sản phẩm, thêm, sửa, xoá; tìm theo mã, gần đúng (không dấu), loại, khoảng giá, tìm nâng cao nhiều khoá; sắp xếp; thống kê; combo tiết kiệm bao nhiêu so với mua lẻ |
| 4. Khách hàng | Nhập n khách, đăng ký nhanh, sửa, xoá; tìm theo mã / SĐT / gần đúng / nâng cao; thống kê theo hạng; top khách chi tiêu; xem ưu đãi theo mức tiền |
| 5. Khuyến mãi | Thêm, sửa, xoá, tìm; xem chương trình còn hiệu lực; gợi ý khuyến mãi tốt nhất cho một mức tiền |
| 6. Nhân viên | Thêm, xoá, tìm; chấm công; bảng chấm công; phiếu lương; bảng lương; thống kê theo tháng |
| 7. Kho nguyên liệu | Danh sách, thêm, tìm; lập phiếu nhập kho; xuất kho; nhà cung cấp & lịch sử nhập; cảnh báo hết/sắp hết hàng; báo cáo tổng hợp |
| 8. Lưu dữ liệu | Ghi toàn bộ danh sách ra file text |

### Quy tắc nghiệp vụ chính

- **Hoá đơn:** chỉ **Thu ngân** và **Quản lý** được lập hoá đơn (Pha chế thì không). Hoá đơn đã thanh toán / đã huỷ không sửa được. Mỗi dòng món tối đa 99 phần.
- **Tính tiền:** tổng hàng − giảm theo hạng thành viên − khuyến mãi − đổi điểm, rồi cộng **VAT** (mặc định 8%, đổi bằng `HoaDon.datTyLeVAT`). Doanh thu = tiền sau giảm, **chưa gồm VAT**, chỉ tính hoá đơn đã thanh toán.
- **Khách hàng:** khách thường không giảm giá; khách thân thiết có hạng (Đồng / Bạc / Vàng / Kim cương) giảm theo hạng và tự nâng hạng theo tổng chi tiêu. Có tích điểm và đổi điểm (1 điểm = 1.000 VND).
- **Khuyến mãi:** giảm theo %, có trần giảm tối đa, đơn tối thiểu và khoảng ngày hiệu lực.
- **Lương:**
  `lương = lương cơ bản / 26 × số ngày công + tiền làm thêm (hệ số 1,5) + thưởng/phụ cấp theo vai trò − bảo hiểm (10,5% lương cơ bản)`
  Làm từ 8 giờ trở lên tính 1 công, giờ vượt 8 là làm thêm. Thưởng/phụ cấp: Thu ngân (hoa hồng + phụ cấp ca tối), Pha chế (theo cấp bậc + số ly), Quản lý (phụ cấp chức vụ + KPI).

---

## 3. Cấu trúc thư mục

```
doan/
└── src/
    ├── Main.java                  # menu chính, điều phối các chức năng
    ├── run.bat                    # chạy trên Windows
    ├── data/                      # dữ liệu dạng text (UTF-8)
    │   ├── sanpham.txt   nhanvien.txt   khachhang.txt
    │   ├── khuyenmai.txt hoadon.txt     nguyenlieu.txt
    ├── iface/                     # interface
    │   ├── IFile  IQuanLy<T>  IGiamGia  IThongKe
    ├── model/                     # lớp đối tượng
    │   ├── SanPham (abstract) ← DoAn, ThucUong, Combo
    │   ├── NhanVien (abstract) ← ThuNgan, PhaChe, QuanLy
    │   ├── KhachHang ← KhachHangThanThiet
    │   ├── KhuyenMai, HoaDon, ChiTietHoaDon, DongThongKe
    │   └── NguyenLieu, NhaCungCap, PhieuNhapKho
    ├── service/                   # lớp danh sách (mảng đối tượng) + nghiệp vụ
    │   ├── DanhSachSanPham  DanhSachKhachHang  DanhSachKhuyenMai
    │   ├── DanhSachHoaDon   DanhSachNhanVien   DanhSachNguyenLieu
    └── util/
        ├── CauHinh            # đường dẫn file, nhập liệu an toàn từ bàn phím
        └── FileHelper         # đọc/ghi file text UTF-8
```

---

## 4. Đối chiếu yêu cầu đồ án (OOP)

| Yêu cầu | Thể hiện trong đồ án |
|---|---|
| ≥ 15 class | 17 class model, 6 class service, 2 class util, `Main` (cùng 4 interface, 4 enum) |
| Constructor | Mọi lớp đều có; dữ liệu được kiểm tra trong setter, sai thì ném `IllegalArgumentException` |
| Kế thừa | `SanPham` → `DoAn`/`ThucUong`/`Combo`; `NhanVien` → `ThuNgan`/`PhaChe`/`QuanLy`; `KhachHang` → `KhachHangThanThiet` |
| Đa hình | Mảng `SanPham[]`, `KhachHang[]` chứa nhiều loại; `getLoai()`, `tinhThuongPhuCap()`, `tinhGiamGia()` được ghi đè; `fromFileString` tạo đúng loại theo dữ liệu file |
| Lớp mảng đối tượng | `DanhSachSanPham`, `DanhSachKhachHang`, `DanhSachKhuyenMai`, `DanhSachHoaDon` (mảng tự mở rộng), `HoaDon` chứa `ChiTietHoaDon[]` |
| Đọc/ghi file text | Interface `IFile`; 6 file trong `data/` |
| static | Bộ đếm sinh mã tự động (`SP01`, `KH001`, `HD00001`...), `fromFileString`, `taoMaTuDong`, VAT mặc định, static/default method trong interface |
| Abstract | `SanPham`, `NhanVien` cùng các phương thức trừu tượng |
| Interface | `IFile`, `IQuanLy`, `IGiamGia`, `IThongKe` (có default và static method) |

---

## 5. Định dạng file dữ liệu

Các trường ngăn cách bằng `|`. Dòng bắt đầu bằng `#` và dòng trống bị bỏ qua. Dòng sai định dạng bị bỏ qua và có thông báo, chương trình không dừng.

| File | Định dạng một dòng |
|---|---|
| `sanpham.txt` | `LOAI\|maSP\|tenSP\|donGia\|trườngRiêng` — `DOAN`: chay (true/false); `THUCUONG`: size S/M/L; `COMBO`: mã thành phần cách nhau `;` |
| `khachhang.txt` | `THUONG\|THANTHIET\|maKH\|hoTen\|sđt\|điểm\|tổngChiTiêu` |
| `khuyenmai.txt` | `maKM\|tên\|%giảm\|giảmTốiĐa\|đơnTốiThiểu\|ngàyBĐ\|ngàyKT` |
| `nhanvien.txt` | `TN/PC/QL\|maNV\|hoTen\|ngàySinh\|giớiTính\|sđt\|địaChỉ\|ngàyVàoLàm\|lươngCB\|trườngRiêng...\|chấmCông (ngày:giờ, cách nhau dấu phẩy)` |
| `hoadon.txt` | `maHD\|ngàyLập\|maNV\|tênNV\|maKH\|tênKH\|trạngThái\|ngàyTT\|phươngThức\|maKM\|VAT\|giảmHạng\|giảmKM\|giảmĐiểm\|điểmĐổi\|điểmTích\|khách đưa\|ghiChú\|dòngMón` (món: `maSP~tên~loại~đơnGiá~SL`, cách nhau `;`) |
| `nguyenlieu.txt` | Các dòng ngăn cách bằng `;` cho nguyên liệu, nhà cung cấp, phiếu nhập kho |

---
