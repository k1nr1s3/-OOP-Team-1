# Quản lý cửa hàng thức ăn nhanh & giải khát (FAST FOOD & DRINK)

Đồ án môn **Lập trình hướng đối tượng** (Java). Ứng dụng console quản lý một cửa hàng đồ ăn nhanh + giải khát: đăng nhập phân quyền, bán hàng, hoá đơn, sản phẩm và combo, khách hàng thân thiết, khuyến mãi, nhân viên (chấm công, lương), kho nguyên liệu (công thức món, nhập / xuất kho, khoá món) và thống kê doanh thu.
> **Giảng viên:** Nguyễn Thanh Sang

> **Nhóm thực hiện: Nhóm 1**

| STT | Họ và tên | MSSV | Nội dung thực hiện |
|---|---|---|---|
| 29 | Huỳnh Nguyễn Ngọc Như | 3122410285 | HoaDon, ChiTietHoaDon, DanhSachHoaDon, lập hoá đơn, thanh toán, thống kê doanh thu theo ngày, tháng, sản phẩm, nhân viên |
| 32 | Lê Văn Phong | 3125560051 | NguyenLieu, NhaCungCap, PhieuNhapKho, DanhSachNguyenLieu, interface IFile, FileHelper, cảnh báo hết hàng, báo cáo tổng hợp |
| 36 | Trần Võ Đại Thắng | 3125560061 | SanPham, DoAn, ThucUong, Combo, DanhSachSanPham, interface IQuanLy, menu Main, vẽ sơ đồ class, ghép code |
| 40 | Thạch Thanh Triển | 3125720030 | NhanVien, ThuNgan, PhaCheCheBien, QuanLy, DanhSachNhanVien, tính lương, chấm công, tìm kiếm và thống kê nhân viên |
| 41 | Lê Tấn Trung | 3125720031 | KhachHang, KhachHangThanThiet, KhuyenMai, interface IGiamGia, DanhSachKhachHang, tích điểm, phân hạng |



---

## 1. Yêu cầu & cách chạy

- JDK 17 trở lên (đã kiểm tra với JDK 21).

**Windows:**  `run.bat` (biên dịch `Main.java` rồi chạy, cần đứng trong thư mục `src`)
**Biên dịch lại và chạy thủ công:**

```bash
cd src
javac -encoding UTF-8 -d out $(find . -name "*.java")   # PowerShell: dùng Get-ChildItem thay cho find
java -Dfile.encoding=UTF-8 -cp out Main
```

Phải chạy từ thư mục `src` vì chương trình đọc dữ liệu theo đường dẫn tương đối `data/...`.

Khi mở chương trình, toàn bộ dữ liệu trong `data/` được đọc tự động, sau đó **phải đăng nhập** (tối đa 3 lần, sai quá 3 lần thì thoát). Tài khoản là **mã nhân viên** (ví dụ `NV01`), mật khẩu lưu trong `data/taikhoan.txt`. Sau khi đăng nhập, menu hiện ra theo cấp truy cập của tài khoản (xem mục 2). Chọn **Lưu các thay đổi** (hoặc **8. Lưu tất cả dữ liệu** với Quản lý), hoặc trả lời `c` khi thoát để ghi lại dữ liệu.

---

## 2. Chức năng

### Đăng nhập & phân quyền

Mỗi tài khoản có một cấp truy cập, và mỗi cấp có menu riêng:

| Cấp | Vai trò | Menu |
|---|---|---|
| 1 | Thu ngân | Bán hàng, Chấm công, Quản lý khách hàng, Lưu các thay đổi, Đổi mật khẩu |
| 2 | Pha chế | Chấm công (kèm ghi nhận số ly), Kho nguyên liệu (bếp), Lưu các thay đổi, Đổi mật khẩu |
| 3 | Quản lý | Toàn bộ chức năng (bảng bên dưới) và Đổi mật khẩu |

Mọi tài khoản chỉ **đổi được mật khẩu của chính mình**. Mật khẩu mới phải từ 6 ký tự trở lên, khác mật khẩu hiện tại, khác mật khẩu mặc định và không chứa ký tự `|`.

### Menu của Quản lý

| Menu | Chức năng |
|---|---|
| 1. Bán hàng | Lập hoá đơn, gọi thêm món, bớt/xoá món, xem bill, thanh toán (tiền mặt / chuyển khoản / thẻ / ví), huỷ hoá đơn, danh sách hoá đơn chờ thanh toán, xem thực đơn (kèm số phần còn làm được và tình trạng món) |
| 2. Hoá đơn & thống kê | Lọc hoá đơn theo trạng thái / nhân viên / khách / ngày; báo cáo doanh thu theo ngày, tháng, năm; thống kê theo sản phẩm, nhân viên, phương thức |
| 3. Sản phẩm | Nhập n sản phẩm, thêm, sửa (tên, giá), xoá; tìm theo mã, gần đúng (không dấu), loại, khoảng giá, tìm nâng cao nhiều khoá; sắp xếp; thống kê; combo tiết kiệm bao nhiêu so với mua lẻ. **Tạo combo:** thêm sản phẩm loại `3 = Combo`, nhập mã các món thành phần cách nhau bằng `;` |
| 4. Khách hàng | Nhập n khách, đăng ký nhanh, sửa, xoá; tìm theo mã / SĐT / gần đúng / nâng cao; thống kê theo hạng; top khách chi tiêu; xem ưu đãi theo mức tiền |
| 5. Khuyến mãi | Thêm, sửa, xoá, tìm; xem chương trình còn hiệu lực; gợi ý khuyến mãi tốt nhất cho một mức tiền |
| 6. Nhân viên | Thêm (tự tạo tài khoản), xoá (xoá luôn tài khoản), tìm; chấm công; bảng chấm công; phiếu lương; bảng lương; thống kê theo tháng |
| 7. Kho nguyên liệu | Bảng tồn kho, xem công thức món, nhập nguyên liệu (lập phiếu nhập), xuất nguyên liệu (lập phiếu xuất), lịch sử phiếu xuất / nhập, khoá / mở khoá món theo nguyên liệu, cảnh báo hết / sắp hết hàng; riêng Quản lý có thêm: sửa công thức món, thêm nguyên liệu mới, tìm nguyên liệu, nhà cung cấp, báo cáo tổng hợp kho |
| 8. Lưu dữ liệu | Ghi toàn bộ danh sách ra file text |
| 9. Đổi mật khẩu | Đổi mật khẩu của tài khoản đang đăng nhập |

### Quy tắc nghiệp vụ chính

- **Phân quyền:** Thu ngân và Quản lý được lập hoá đơn, Pha chế thì không. Menu **Kho nguyên liệu** có hai chế độ: Pha chế dùng chức năng 1–8 (xem, nhập, xuất, khoá món, cảnh báo), các chức năng còn lại chỉ dành cho Quản lý.
- **Hoá đơn:** hoá đơn đã thanh toán / đã huỷ không sửa được. Mỗi dòng món tối đa 99 phần.
- **Tính tiền:** tổng hàng − giảm theo hạng thành viên − khuyến mãi − đổi điểm, rồi cộng **VAT** (mặc định 8%, đổi bằng `HoaDon.datTyLeVAT`). Doanh thu = tiền sau giảm, **chưa gồm VAT**, chỉ tính hoá đơn đã thanh toán.
- **Khách hàng:** khách thường không giảm giá; khách thân thiết có hạng (Đồng / Bạc / Vàng / Kim cương) giảm theo hạng và tự nâng hạng theo tổng chi tiêu. Có tích điểm và đổi điểm (1 điểm = 1.000 VND).
- **Khuyến mãi:** giảm theo %, có trần giảm tối đa, đơn tối thiểu và khoảng ngày hiệu lực.
- **Combo:** `Combo` là một loại `SanPham` (có mã, tên, đơn giá riêng) và đồng thời chứa danh sách các `SanPham` thành phần. Mã lặp lại nghĩa là combo có nhiều phần của món đó. Combo **không chứa combo khác**, và mã thành phần không tồn tại hoặc là combo sẽ bị bỏ qua kèm cảnh báo. Combo không có công thức riêng, công thức của combo là tổng công thức các món thành phần.
- **Công thức & kho nguyên liệu:** mỗi đồ ăn / thức uống có công thức (nguyên liệu và liều lượng cho 1 phần). Khi gọi món, hệ thống kiểm tra đủ nguyên liệu rồi **trừ kho ngay**; bớt món hoặc huỷ hoá đơn thì **hoàn kho** phần tương ứng. Thực đơn hiển thị số phần còn làm được, món hết nguyên liệu không order được. Món chưa có công thức không bị giới hạn.
- **Khoá món theo nguyên liệu:** khi một nguyên liệu bị khoá, mọi món dùng nguyên liệu đó (kể cả combo) bị khoá theo và không order được, cho đến khi mở khoá nguyên liệu.
- **Phiếu nhập / xuất kho:** mã phiếu, ngày và người lập tự điền theo tài khoản đang đăng nhập. Phiếu xuất bắt buộc ghi nội dung (lý do), gồm một hoặc nhiều dòng nguyên liệu – số lượng. Nguyên liệu hết hoặc sắp hết (dưới mức tối thiểu) được cảnh báo.
- **Lương:**
  `lương = lương cơ bản / 26 × số ngày công + tiền làm thêm (hệ số 1,5) + thưởng/phụ cấp theo vai trò − bảo hiểm (10,5% lương cơ bản)`
  Làm từ 8 giờ trở lên tính 1 công, giờ vượt 8 là làm thêm. Thưởng/phụ cấp: Thu ngân (hoa hồng + phụ cấp ca tối), Pha chế (theo cấp bậc + số ly), Quản lý (phụ cấp chức vụ + KPI).

---

## 3. Cấu trúc thư mục

```
doan/
└── src/
    ├── Main.java                  # đăng nhập, menu theo cấp truy cập, điều phối các chức năng
    ├── run.bat                    # chạy trên Windows
    ├── data/                      # dữ liệu dạng text (UTF-8)
    │   ├── sanpham.txt   nhanvien.txt   khachhang.txt
    │   ├── khuyenmai.txt hoadon.txt     nguyenlieu.txt
    │   └── taikhoan.txt           # tài khoản đăng nhập
    ├── iface/                     # interface
    │   ├── IFile  IQuanLy<T>  IGiamGia  IThongKe
    ├── model/                     # lớp đối tượng
    │   ├── SanPham (abstract) ← DoAn, ThucUong, Combo
    │   ├── NhanVien (abstract) ← ThuNgan, PhaCheCheBien, QuanLy
    │   ├── KhachHang ← KhachHangThanThiet
    │   ├── KhuyenMai, HoaDon, ChiTietHoaDon, DongThongKe
    │   ├── CongThuc               # công thức món: nguyên liệu -> liều lượng
    │   └── NguyenLieu, NhaCungCap, PhieuNhapKho, PhieuXuatKho
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
| ≥ 15 class | 19 class model, 6 class service, 2 class util, `Main` (cùng 4 interface, 6 enum) |
| Constructor | Mọi lớp đều có; dữ liệu được kiểm tra trong setter, sai thì ném `IllegalArgumentException` |
| Kế thừa | `SanPham` → `DoAn`/`ThucUong`/`Combo`; `NhanVien` → `ThuNgan`/`PhaCheCheBien`/`QuanLy`; `KhachHang` → `KhachHangThanThiet` |
| Đa hình | Mảng `SanPham[]`, `KhachHang[]` chứa nhiều loại; `getLoai()`, `tinhThuongPhuCap()`, `tinhGiamGia()` được ghi đè; `Combo` ghi đè `getCongThuc()` (cộng công thức các món con); `fromFileString` tạo đúng loại theo dữ liệu file |
| Quan hệ has-a | `Combo` chứa danh sách `SanPham` thành phần (aggregation, 1..*); `SanPham` chứa `CongThuc`; `HoaDon` chứa `ChiTietHoaDon[]`; các lớp `DanhSach...` chứa mảng đối tượng tương ứng |
| Lớp mảng đối tượng | `DanhSachSanPham`, `DanhSachKhachHang`, `DanhSachKhuyenMai`, `DanhSachHoaDon` (mảng tự mở rộng), `HoaDon` chứa `ChiTietHoaDon[]` |
| Đọc/ghi file text | Interface `IFile`; 6 file dữ liệu trong `data/` (cộng `taikhoan.txt` do `Main` đọc/ghi) |
| static | Bộ đếm sinh mã tự động (`SP01`, `KH001`, `HD00001`...), `fromFileString`, `taoMaTuDong`, VAT mặc định, static/default method trong interface |
| Abstract | `SanPham`, `NhanVien` cùng các phương thức trừu tượng |
| Interface | `IFile`, `IQuanLy`, `IGiamGia`, `IThongKe` (có default và static method) |

---

## 5. Định dạng file dữ liệu

Các trường ngăn cách bằng `|`. Dòng bắt đầu bằng `#` và dòng trống bị bỏ qua. Dòng sai định dạng bị bỏ qua và có thông báo, chương trình không dừng.

| File | Định dạng một dòng |
|---|---|
| `sanpham.txt` | `LOAI\|maSP\|tenSP\|donGia\|trườngRiêng\|côngThức` — `DOAN`: chay (true/false); `THUCUONG`: size S/M/L; `COMBO`: mã thành phần cách nhau `;` (mã lặp lại = nhiều phần). Công thức dạng `maNL:liềuLượng,maNL:liềuLượng`; combo không có trường công thức |
| `khachhang.txt` | `THUONG\|THANTHIET\|maKH\|hoTen\|sđt\|điểm\|tổngChiTiêu` |
| `khuyenmai.txt` | `maKM\|tên\|%giảm\|giảmTốiĐa\|đơnTốiThiểu\|ngàyBĐ\|ngàyKT` |
| `nhanvien.txt` | `TN/NB/QL\|maNV\|hoTen\|ngàySinh\|giớiTính\|sđt\|địaChỉ\|ngàyVàoLàm\|lươngCB\|trườngRiêng...\|chấmCông (ngày:giờ, cách nhau dấu phẩy)` — `TN`: Thu ngân, `NB`: Pha chế / chế biến, `QL`: Quản lý |
| `hoadon.txt` | `maHD\|ngàyLập\|maNV\|tênNV\|maKH\|tênKH\|trạngThái\|ngàyTT\|phươngThức\|maKM\|VAT\|giảmHạng\|giảmKM\|giảmĐiểm\|điểmĐổi\|điểmTích\|khách đưa\|ghiChú\|dòngMón` (món: `maSP~tên~loại~đơnGiá~SL`, cách nhau `;`) |
| `nguyenlieu.txt` | Các dòng ngăn cách bằng `;` cho nguyên liệu (`maNL;tenNL;donViTinh;soLuongTon;mucToiThieu;donGiaNhap;maNCC;khoa`), nhà cung cấp (`NCC\|...`), phiếu nhập kho (`PN\|...`) và phiếu xuất kho (`PX\|maPhieu;thoiGian;maNV;noiDung;maNL:soLuong,...`) |
| `taikhoan.txt` | `maNV\|matKhau\|capDo` — `capDo`: 1 = Thu ngân, 2 = Pha chế, 3 = Quản lý. Tạo khi thêm nhân viên (mật khẩu mặc định), xoá khi xoá nhân viên, ghi lại khi đổi mật khẩu |

---