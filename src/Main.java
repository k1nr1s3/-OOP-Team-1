import iface.IGiamGia;
import model.HoaDon;
import model.HoaDon.PhuongThuc;
import model.HoaDon.TrangThai;
import model.KhachHang;
import model.CongThuc;
import model.KhuyenMai;
import model.NguyenLieu;
import model.NhanVien;
import model.PhieuNhapKho;
import model.PhieuXuatKho;
import model.PhaChe;
import model.QuanLy;
import model.SanPham;
import model.ThuNgan;
import service.DanhSachHoaDon;
import service.DanhSachKhachHang;
import service.DanhSachKhuyenMai;
import service.DanhSachNguyenLieu;
import service.DanhSachNhanVien;
import service.DanhSachSanPham;
import util.CauHinh;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class Main {

    private static final DanhSachSanPham dsSP = new DanhSachSanPham();
    private static final DanhSachNhanVien dsNV = new DanhSachNhanVien();
    private static final DanhSachKhachHang dsKH = new DanhSachKhachHang();
    private static final DanhSachKhuyenMai dsKM = new DanhSachKhuyenMai();
    private static final DanhSachHoaDon dsHD = new DanhSachHoaDon();
    private static final DanhSachNguyenLieu dsNL = new DanhSachNguyenLieu();

    private static final String DUONG_DAN_TAI_KHOAN = "data/taikhoan.txt";
    private static final String MAT_KHAU_MAC_DINH = "abc@123";
    private static String maDangNhap = "";
    private static int capDo = 0;   // 1 = thu ngân, 2 = pha chế, 3 = quản lý

    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));

        System.out.println("=== HỆ THỐNG QUẢN LÝ CỬA HÀNG " + HoaDon.TEN_CUA_HANG + " ===");
        docTatCa();

        if (!dangNhap()) {
            System.out.println("Đăng nhập sai quá 3 lần. Thoát chương trình!");
            CauHinh.SCANNER.close();
            return;
        }

        switch (capDo) {
            case 1: menuThuNgan(); break;
            case 2: menuPhaChe(); break;
            default: menuQuanLy(); break;
        }

        if (CauHinh.xacNhan("Lưu dữ liệu trước khi thoát")) {
            ghiTatCa();
        }
        System.out.println("Tạm biệt!");
        CauHinh.SCANNER.close();
    }

    //Đăng nhập & tài khoản
    private static boolean dangNhap() {
        CauHinh.tieuDe("ĐĂNG NHẬP");
        for (int lan = 1; lan <= 3; lan++) {
            String tk = CauHinh.nhapChuoi("Tài khoản (mã nhân viên): ");
            String mk = CauHinh.nhapChuoi("Mật khẩu: ");
            int cd = kiemTraTaiKhoan(tk, mk);
            if (cd >= 1 && cd <= 3) {
                maDangNhap = tk.trim().toUpperCase();
                capDo = cd;
                System.out.println("Đăng nhập thành công! Mã: " + maDangNhap + " - Cấp truy cập: " + capDo);
                return true;
            }
            System.out.println("Sai tài khoản hoặc mật khẩu (lần " + lan + "/3).");
        }
        return false;
    }

    // Trả về cấp độ (1-3) nếu đúng, 0 nếu sai. Định dạng mỗi dòng: MaNV|MatKhau|CapDo
    private static int kiemTraTaiKhoan(String tk, String mk) {
        Path path = Paths.get(DUONG_DAN_TAI_KHOAN);
        if (!Files.exists(path)) {
            System.out.println("Không tìm thấy " + DUONG_DAN_TAI_KHOAN);
            return 0;
        }
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|");
                if (p.length < 3) continue;
                if (p[0].trim().equalsIgnoreCase(tk.trim()) && p[1].trim().equals(mk)) {
                    try {
                        return Integer.parseInt(p[2].trim());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Không đọc được " + DUONG_DAN_TAI_KHOAN + ": " + e.getMessage());
        }
        return 0;
    }

    private static boolean tonTaiTaiKhoan(String ma) {
        Path path = Paths.get(DUONG_DAN_TAI_KHOAN);
        if (!Files.exists(path)) return false;
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|");
                if (p.length >= 1 && p[0].trim().equalsIgnoreCase(ma.trim())) return true;
            }
        } catch (IOException e) {
            System.out.println("Không đọc được " + DUONG_DAN_TAI_KHOAN + ": " + e.getMessage());
        }
        return false;
    }

    // Thêm 1 dòng MaNV|MatKhau|CapDo vào cuối taikhoan.txt. Trả về false nếu lỗi ghi file.
    // Chỉ được gọi sau khi mã NV đã thêm vào danh sách thành công, nên tài khoản trùng mã (nếu có) là tài khoản mồ côi -> xoá để tạo lại
    private static boolean themTaiKhoan(String ma, String mk, int cap) {
        if (tonTaiTaiKhoan(ma)) xoaTaiKhoan(ma);
        try {
            Path path = Paths.get(DUONG_DAN_TAI_KHOAN);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            String dong = ma.trim().toUpperCase() + "|" + mk + "|" + cap + System.lineSeparator();
            // nếu file đang có dữ liệu mà không kết thúc bằng xuống dòng thì thêm xuống dòng trước
            if (Files.exists(path) && Files.size(path) > 0) {
                byte[] data = Files.readAllBytes(path);
                byte last = data[data.length - 1];
                if (last != '\n' && last != '\r') {
                    dong = System.lineSeparator() + dong;
                }
            }
            Files.write(path, dong.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            System.out.println("Không ghi được " + DUONG_DAN_TAI_KHOAN + ": " + e.getMessage());
            return false;
        }
    }

    // taikhoan.txt được ghi ngay, nên nhanvien.txt cũng ghi ngay sau khi thêm/xoá NV để 2 file luôn khớp nhau
    private static void luuDanhSachNhanVien() {
        try {
            dsNV.ghiFile(CauHinh.DUONG_DAN_NHAN_VIEN);
        } catch (IOException e) {
            System.out.println("Lỗi khi ghi " + CauHinh.DUONG_DAN_NHAN_VIEN + ": " + e.getMessage());
        }
    }

    // Xoá tài khoản của nhân viên đã bị xoá (tránh tài khoản "mồ côi")
    private static void xoaTaiKhoan(String ma) {
        Path path = Paths.get(DUONG_DAN_TAI_KHOAN);
        if (!Files.exists(path)) return;
        try {
            List<String> con = new ArrayList<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String s = line.replace("\uFEFF", "").trim();
                String[] p = s.split("\\|");
                boolean trung = !s.isEmpty() && !s.startsWith("#") && p.length >= 1
                        && p[0].trim().equalsIgnoreCase(ma.trim());
                if (!trung) con.add(line);
            }
            Files.write(path, con, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Không cập nhật được " + DUONG_DAN_TAI_KHOAN + ": " + e.getMessage());
        }
    }

    // Đổi mật khẩu: CHỈ đổi được tài khoản đang đăng nhập (kể cả quản lý), không có cách đổi mật khẩu của người khác
    private static void doiMatKhau() {
        CauHinh.tieuDe("ĐỔI MẬT KHẨU - " + maDangNhap);
        String cu = CauHinh.nhapChuoi("Mật khẩu hiện tại: ");
        if (kiemTraTaiKhoan(maDangNhap, cu) != capDo) {
            System.out.println("Mật khẩu hiện tại không đúng.");
            return;
        }
        String moi = CauHinh.nhapChuoi("Mật khẩu mới: ");
        if (moi.length() < 6) {
            System.out.println("Mật khẩu mới phải có ít nhất 6 ký tự.");
            return;
        }
        if (moi.contains("|")) {
            System.out.println("Mật khẩu không được chứa ký tự '|'.");
            return;
        }
        if (moi.equals(cu)) {
            System.out.println("Mật khẩu mới phải khác mật khẩu hiện tại.");
            return;
        }
        if (moi.equals(MAT_KHAU_MAC_DINH)) {
            System.out.println("Không được dùng lại mật khẩu mặc định.");
            return;
        }
        if (!moi.equals(CauHinh.nhapChuoi("Nhập lại mật khẩu mới: "))) {
            System.out.println("Hai lần nhập mật khẩu mới không khớp.");
            return;
        }
        System.out.println(capNhatMatKhau(maDangNhap, moi)
                ? "Đã đổi mật khẩu thành công. Lần đăng nhập sau dùng mật khẩu mới."
                : "Không đổi được mật khẩu (k tìm thấy tài khoản hoặc lỗi ghi file).");
    }

    // Ghi mật khẩu mới vào đúng dòng MaNV|MatKhau|CapDo, giữ nguyên cấp độ và các dòng khác
    private static boolean capNhatMatKhau(String ma, String mkMoi) {
        Path path = Paths.get(DUONG_DAN_TAI_KHOAN);
        if (!Files.exists(path)) return false;
        try {
            List<String> dong = new ArrayList<>();
            boolean daDoi = false;
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String s = line.replace("\uFEFF", "").trim();
                String[] p = s.split("\\|");
                if (!daDoi && !s.isEmpty() && !s.startsWith("#") && p.length >= 3
                        && p[0].trim().equalsIgnoreCase(ma.trim())) {
                    p[1] = mkMoi;
                    dong.add(String.join("|", p));
                    daDoi = true;
                } else {
                    dong.add(line);
                }
            }
            if (!daDoi) return false;
            Files.write(path, dong, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.out.println("Không cập nhật được " + DUONG_DAN_TAI_KHOAN + ": " + e.getMessage());
            return false;
        }
    }

    //Menu cấp 1: Thu ngân
    private static void menuThuNgan() {
        int chon;
        do {
            CauHinh.tieuDe("MENU THU NGÂN");
            System.out.println("1. Bán hàng");
            System.out.println("2. Chấm công");
            System.out.println("3. Quản lý khách hàng");
            System.out.println("4. Lưu các thay đổi");
            System.out.println("5. Đổi mật khẩu");
            System.out.println("0. Thoát");
            chon = CauHinh.nhapSoNguyen("Chọn chức năng: ");
            try {
                switch (chon) {
                    case 1: menuBanHang(); break;
                    case 2: menuChamCong(); break;
                    case 3: menuKhachHang(); break;
                    case 4: ghiTatCa(); break;
                    case 5: doiMatKhau(); break;
                    case 0: break;
                    default: System.out.println("Lựa chọn k hợp lệ!");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } while (chon != 0);
    }

    //Menu cấp 2: Pha chế
    private static void menuPhaChe() {
        int chon;
        do {
            CauHinh.tieuDe("MENU PHA CHẾ");
            System.out.println("1. Chấm công");
            System.out.println("2. Kho nguyên liệu (nhập / xuất / khoá món)");
            System.out.println("3. Lưu các thay đổi");
            System.out.println("4. Đổi mật khẩu");
            System.out.println("0. Thoát");
            chon = CauHinh.nhapSoNguyen("Chọn chức năng: ");
            try {
                switch (chon) {
                    case 1: menuChamCong(); break;
                    case 2: menuKho(); break;
                    case 3: ghiTatCa(); break;
                    case 4: doiMatKhau(); break;
                    case 0: break;
                    default: System.out.println("Lựa chọn k hợp lệ!");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } while (chon != 0);
    }

    //Menu cấp 3: Quản lý
    private static void menuQuanLy() {
        int chon;
        do {
            CauHinh.tieuDe("MENU CHÍNH (QUẢN LÝ)");
            System.out.println("1. Bán hàng (lập hoá đơn, gọi món, thanh toán)");
            System.out.println("2. Hoá đơn & thống kê doanh thu");
            System.out.println("3. Quản lý sản phẩm");
            System.out.println("4. Quản lý khách hàng");
            System.out.println("5. Quản lý khuyến mãi");
            System.out.println("6. Quản lý nhân viên");
            System.out.println("7. Quản lý kho nguyên liệu");
            System.out.println("8. Lưu tất cả dữ liệu");
            System.out.println("9. Đổi mật khẩu của tôi");
            System.out.println("0. Thoát");
            chon = CauHinh.nhapSoNguyen("Chọn chức năng: ");
            chayAnToan(chon);
        } while (chon != 0);
    }

    private static void chayAnToan(int chon) {
        try {
            switch (chon) {
                case 1: menuBanHang(); break;
                case 2: menuHoaDon(); break;
                case 3: menuSanPham(); break;
                case 4: menuKhachHang(); break;
                case 5: menuKhuyenMai(); break;
                case 6: menuNhanVien(); break;
                case 7: menuKho(); break;
                case 8: ghiTatCa(); break;
                case 9: doiMatKhau(); break;
                case 0: break;
                default: System.out.println("Lựa chọn k hợp lệ!");
            }
        } catch (RuntimeException e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }

    //Chấm công (dùng cho thu ngân & pha chế, chỉ chấm / xem cho tài khoản đang đăng nhập)
    private static void menuChamCong() {
        int chon;
        do {
            CauHinh.tieuDe("CHẤM CÔNG - " + maDangNhap);
            System.out.println(dsNV.trangThaiChamCong(maDangNhap));
            System.out.println("1. Check-in (vào ca)");
            System.out.println("2. Check-out (ra ca, tự tính số giờ làm)");
            System.out.println("3. Bảng chấm công của tôi (theo tháng)");
            if (capDo == 2) {
                System.out.println("4. Ghi nhận số ly pha chế");
            }
            System.out.println("0. Quay lại");
            chon = CauHinh.nhapSoNguyen("Chọn: ");
            try {
                switch (chon) {
                    case 1:
                        LocalDateTime vao = dsNV.checkIn(maDangNhap);
                        System.out.println("Đã check-in lúc " + vao.toLocalTime().format(NhanVien.DINH_DANG_GIO)
                                + " ngày " + vao.toLocalDate().format(NhanVien.DINH_DANG_NGAY) + ".");
                        break;
                    case 2:
                        LocalDateTime cv = LocalDateTime.now().withNano(0);
                        double gio = dsNV.checkOut(maDangNhap);
                        System.out.println("Đã check-out lúc " + cv.toLocalTime().format(NhanVien.DINH_DANG_GIO)
                                + ". Số giờ làm: " + String.format("%.2f", gio) + " giờ.");
                        break;
                    case 3:
                        LocalDate homNay = LocalDate.now();
                        String t = CauHinh.nhapChuoi("Tháng (bỏ trống = tháng này): ");
                        int thang = t.isEmpty() ? homNay.getMonthValue() : parseSoNguyen(t);
                        String n = CauHinh.nhapChuoi("Năm (bỏ trống = năm nay): ");
                        int nam = n.isEmpty() ? homNay.getYear() : parseSoNguyen(n);
                        dsNV.inBangChamCongCaNhan(maDangNhap, thang, nam);   // chỉ của bản thân
                        break;
                    case 4:
                        if (capDo != 2) {
                            System.out.println("Lựa chọn k hợp lệ!");
                            break;
                        }
                        int thangLy = CauHinh.nhapSoNguyen("Tháng: ");
                        int namLy = CauHinh.nhapSoNguyen("Năm: ");
                        int soLy = CauHinh.nhapSoNguyen("Số ly: ");
                        System.out.println(dsNV.ghiNhanSoLy(maDangNhap, thangLy, namLy, soLy)
                                ? "Đã ghi nhận số ly." : "Không ghi nhận được (sai mã hoặc số ly).");
                        break;
                    case 0: break;
                    default: System.out.println("Lựa chọn k hợp lệ!");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } while (chon != 0);
    }

    private static int parseSoNguyen(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Phải nhập số nguyên");
        }
    }

    //Đọc/ghi file
    private static void docTatCa() {
        try {
            System.out.println("Đã đọc " + dsSP.docFile(CauHinh.DUONG_DAN_SAN_PHAM) + " sản phẩm");
            System.out.println("Đã đọc " + dsNV.docFile(CauHinh.DUONG_DAN_NHAN_VIEN) + " nhân viên");
            System.out.println("Đã đọc " + dsKH.docFile(CauHinh.DUONG_DAN_KHACH_HANG) + " khách hàng");
            System.out.println("Đã đọc " + dsKM.docFile(CauHinh.DUONG_DAN_KHUYEN_MAI) + " khuyến mãi");
            System.out.println("Đã đọc " + dsHD.docFile(CauHinh.DUONG_DAN_HOA_DON) + " hoá đơn");
            System.out.println("Đã liên kết " + dsHD.ganNhanVien(dsNV) + " hoá đơn với nhân viên");
            System.out.println("Đã đọc " + dsNL.docFile(CauHinh.DUONG_DAN_NGUYEN_LIEU) + " nguyên liệu");
        } catch (IOException | RuntimeException e) {
            System.out.println("Lỗi khi đọc dữ liệu: " + e.getMessage());
        }
    }

    private static void ghiTatCa() {
        try {
            dsSP.ghiFile(CauHinh.DUONG_DAN_SAN_PHAM);
            dsNV.ghiFile(CauHinh.DUONG_DAN_NHAN_VIEN);
            dsKH.ghiFile(CauHinh.DUONG_DAN_KHACH_HANG);
            dsKM.ghiFile(CauHinh.DUONG_DAN_KHUYEN_MAI);
            dsHD.ghiFile(CauHinh.DUONG_DAN_HOA_DON);
            dsNL.ghiFile(CauHinh.DUONG_DAN_NGUYEN_LIEU);
            System.out.println("Đã lưu tất cả dữ liệu vào thư mục data/");
        } catch (IOException e) {
            System.out.println("Lỗi khi ghi file: " + e.getMessage());
        }
    }

    //Kho nguyên liệu (bếp: nhập / xuất / khoá; quản lý: thêm cả sửa công thức, thêm nguyên liệu, báo cáo)
    private static void menuKho() {
        boolean quanLy = capDo == 3;
        int chon;
        do {
            CauHinh.tieuDe(quanLy ? "KHO NGUYÊN LIỆU (QUẢN LÝ)" : "KHO NGUYÊN LIỆU (BẾP)");
            System.out.println("1. Bảng nguyên liệu (tồn kho)");
            System.out.println("2. Xem công thức món ăn");
            System.out.println("3. Nhập nguyên liệu");
            System.out.println("4. Xuất nguyên liệu (lập phiếu xuất)");
            System.out.println("5. Lịch sử phiếu xuất / phiếu nhập");
            System.out.println("6. Khoá món theo nguyên liệu");
            System.out.println("7. Mở khoá món theo nguyên liệu");
            System.out.println("8. Cảnh báo hết hàng / sắp hết hàng");
            if (quanLy) {
                System.out.println("9. Sửa công thức món ăn");
                System.out.println("10. Thêm nguyên liệu mới");
                System.out.println("11. Tìm nguyên liệu");
                System.out.println("12. Nhà cung cấp");
                System.out.println("13. Báo cáo tổng hợp kho");
            }
            System.out.println("0. Quay lại");
            chon = CauHinh.nhapSoNguyen("Chọn: ");
            try {
                if (!quanLy && chon > 8) {
                    System.out.println("Chức năng này chỉ dành cho quản lý!");
                    continue;
                }
                switch (chon) {
                    case 1: dsNL.hienThiDanhSach(); break;
                    case 2: xemCongThuc(); break;
                    case 3: nhapNguyenLieu(); break;
                    case 4: xuatNguyenLieu(); break;
                    case 5: dsNL.hienThiPhieuXuatKho(); dsNL.hienThiPhieuNhapKho(); break;
                    case 6: khoaNguyenLieu(true); break;
                    case 7: khoaNguyenLieu(false); break;
                    case 8: dsNL.canhBaoHetHang(); break;
                    case 9: suaCongThuc(); break;
                    case 10:
                        NguyenLieu moi = new NguyenLieu();
                        moi.nhap(CauHinh.SCANNER);
                        System.out.println(dsNL.themNguyenLieu(moi) ? "Them thanh cong!" : "Ma NL da ton tai!");
                        break;
                    case 11:
                        String tk = CauHinh.nhapChuoi("Nhập mã hoặc tên cần tìm: ");
                        NguyenLieu theoMa = dsNL.timTheoMa(tk);
                        List<NguyenLieu> kq = theoMa != null ? java.util.Collections.singletonList(theoMa) : dsNL.timTheoTen(tk);
                        if (kq.isEmpty()) {
                            System.out.println("Không tìm thấy!");
                        } else {
                            kq.forEach(NguyenLieu::xuat);
                        }
                        break;
                    case 12: dsNL.hienThiNhaCungCap(); break;
                    case 13: dsNL.baoCaoTongHop(); break;
                    case 0: break;
                    default: System.out.println("Lựa chọn k hợp lệ!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: phải nhập số hợp lệ");
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } while (chon != 0);
    }

    private static void xemCongThuc() {
        String ma = CauHinh.nhapChuoi("Mã món (bỏ trống = tất cả): ");
        if (ma.isEmpty()) {
            dsSP.inTatCaCongThuc(dsNL);
        } else if (!dsSP.inCongThuc(ma, dsNL)) {
            System.out.println("Không có sản phẩm " + ma);
        }
    }

    // Nhập nguyên liệu: lập phiếu nhập, mã phiếu / ngày / người nhập tự điền
    private static void nhapNguyenLieu() {
        dsNL.hienThiDanhSach();
        String ma = CauHinh.nhapChuoi("Mã nguyên liệu cần nhập: ");
        NguyenLieu nl = dsNL.timTheoMa(ma);
        if (nl == null) {
            System.out.println("Không tìm thấy nguyên liệu " + ma);
            return;
        }
        double sl = CauHinh.nhapSoThuc("Số lượng nhập (" + nl.getDonViTinh() + "): ");
        double gia = CauHinh.nhapSoThuc("Đơn giá nhập (VND, 0 = giữ giá cũ " + Math.round(nl.getDonGiaNhap()) + "): ");
        String ncc = CauHinh.nhapChuoi("Mã nhà cung cấp (bỏ trống = " + nl.getMaNCC() + "): ");
        PhieuNhapKho pn = dsNL.nhapKho(maDangNhap, ma, sl, gia, ncc);
        System.out.printf("Đã lập phiếu nhập %s: %s +%s %s. Tồn kho mới: %s %s%n", pn.getMaPhieu(), nl.getTenNL(),
                CongThuc.dinhDangSo(sl), nl.getDonViTinh(), CongThuc.dinhDangSo(nl.getSoLuongTon()), nl.getDonViTinh());
    }

    // Xuất nguyên liệu: phiếu có mã tự sinh, nội dung (lý do) và 1 hoặc nhiều dòng nguyên liệu - số lượng
    private static void xuatNguyenLieu() {
        dsNL.hienThiDanhSach();
        System.out.println("Mã phiếu xuất (tự động): " + dsNL.maPhieuXuatMoi());
        String noiDung = CauHinh.nhapChuoi("Nội dung xuất (ghi lý do): ");
        if (noiDung.isEmpty()) {
            System.out.println("Phải ghi nội dung (lý do) xuất kho.");
            return;
        }
        Map<String, Double> dong = new LinkedHashMap<>();
        do {
            String ma = CauHinh.nhapChuoi("Mã nguyên liệu xuất: ");
            NguyenLieu nl = dsNL.timTheoMa(ma);
            if (nl == null) {
                System.out.println("Không tìm thấy nguyên liệu " + ma);
                continue;
            }
            double sl = CauHinh.nhapSoThuc("Số lượng xuất (" + nl.getDonViTinh() + ", tồn "
                    + CongThuc.dinhDangSo(nl.getSoLuongTon()) + "): ");
            if (sl <= 0) {
                System.out.println("Số lượng phải lớn hơn 0.");
                continue;
            }
            dong.merge(nl.getMaNL(), sl, Double::sum);
            System.out.println("Đã thêm vào phiếu: " + nl.getTenNL() + " x " + CongThuc.dinhDangSo(dong.get(nl.getMaNL())));
        } while (CauHinh.xacNhan("Xuất thêm nguyên liệu khác"));
        if (dong.isEmpty()) {
            System.out.println("Phiếu k có nguyên liệu nào, đã bỏ.");
            return;
        }
        if (!CauHinh.xacNhan("Xác nhận lập phiếu xuất")) {
            System.out.println("Đã bỏ phiếu xuất.");
            return;
        }
        PhieuXuatKho px = dsNL.lapPhieuXuat(maDangNhap, noiDung, dong);
        System.out.print(px.taoPhieu(dsNL::timTheoMa));
        for (String ma : px.getChiTiet().keySet()) {
            NguyenLieu nl = dsNL.timTheoMa(ma);
            if (nl != null && nl.isCanhBaoHetHang()) {
                System.out.println("Cảnh báo: " + nl.getTenNL() + " " + nl.getTrangThaiTonKho().toLowerCase()
                        + " (còn " + CongThuc.dinhDangSo(nl.getSoLuongTon()) + " " + nl.getDonViTinh() + ")");
            }
        }
    }

    // Khoá / mở khoá nguyên liệu: mọi món có nguyên liệu đó (kể cả combo) bị khoá / mở khoá theo, nhân viên k order được
    private static void khoaNguyenLieu(boolean khoa) {
        List<NguyenLieu> dangKhoa = dsNL.layDanhSachDangKhoa();
        if (!dangKhoa.isEmpty()) {
            System.out.println("Nguyên liệu đang khoá:");
            dangKhoa.forEach(NguyenLieu::xuat);
        } else if (!khoa) {
            System.out.println("Hiện k có nguyên liệu nào bị khoá.");
            return;
        }
        String ma = CauHinh.nhapChuoi((khoa ? "Mã nguyên liệu cần KHOÁ: " : "Mã nguyên liệu cần MỞ KHOÁ: "));
        NguyenLieu nl = dsNL.timTheoMa(ma);
        if (nl == null) {
            System.out.println("Không tìm thấy nguyên liệu " + ma);
            return;
        }
        if (nl.isKhoa() == khoa) {
            System.out.println(nl.getTenNL() + (khoa ? " đã bị khoá từ trước." : " chưa bị khoá."));
            return;
        }
        SanPham[] anhHuong = dsSP.timTheoNguyenLieu(nl.getMaNL());
        System.out.println("Các món chứa " + nl.getTenNL() + ": " + (anhHuong.length == 0 ? "(chưa có món nào)" : ""));
        for (SanPham sp : anhHuong) {
            System.out.println("  - " + sp.getMaSP() + " " + sp.getTenSP());
        }
        if (!CauHinh.xacNhan(khoa ? "Khoá các món này" : "Mở khoá các món này")) {
            System.out.println("Đã huỷ.");
            return;
        }
        dsNL.datKhoa(nl.getMaNL(), khoa);
        System.out.println(khoa ? "Đã khoá. Thu ngân k order được các món trên."
                : "Đã mở khoá. (Món vẫn bị khoá nếu còn nguyên liệu khác đang khoá.)");
    }

    // Quản lý sửa công thức món ăn (đồ ăn / thức uống). Combo lấy công thức từ các món thành phần.
    private static void suaCongThuc() {
        dsSP.hienThi();
        String maSP = CauHinh.nhapChuoi("Mã món cần sửa công thức: ");
        SanPham sp = dsSP.timTheoMa(maSP);
        if (sp == null) {
            System.out.println("Không có sản phẩm " + maSP);
            return;
        }
        if (sp instanceof model.Combo) {
            System.out.println("Combo k có công thức riêng (lấy từ các món thành phần), hãy sửa công thức từng món:");
            dsSP.inCongThuc(sp.getMaSP(), dsNL);
            return;
        }
        int chon;
        do {
            dsSP.inCongThuc(sp.getMaSP(), dsNL);
            System.out.println("1. Thêm / sửa liều lượng 1 nguyên liệu");
            System.out.println("2. Bỏ 1 nguyên liệu khỏi công thức");
            System.out.println("3. Xoá toàn bộ công thức");
            System.out.println("0. Xong");
            chon = CauHinh.nhapSoNguyen("Chọn: ");
            switch (chon) {
                case 1:
                    dsNL.hienThiDanhSach();
                    String ma = CauHinh.nhapChuoi("Mã nguyên liệu: ");
                    NguyenLieu nl = dsNL.timTheoMa(ma);
                    if (nl == null) {
                        System.out.println("Không tìm thấy nguyên liệu " + ma);
                        break;
                    }
                    double lieu = CauHinh.nhapSoThuc("Liều lượng cho 1 phần (" + nl.getDonViTinh() + "): ");
                    System.out.println(dsSP.datLieuLuong(sp.getMaSP(), nl.getMaNL(), lieu, dsNL) ? "Đã cập nhật." : "K cập nhật được.");
                    break;
                case 2:
                    System.out.println(dsSP.xoaNguyenLieuKhoiCongThuc(sp.getMaSP(), CauHinh.nhapChuoi("Mã nguyên liệu: "))
                            ? "Đã bỏ." : "Công thức k có nguyên liệu này.");
                    break;
                case 3:
                    if (CauHinh.xacNhan("Xoá toàn bộ công thức")) {
                        dsSP.xoaCongThuc(sp.getMaSP());
                        System.out.println("Đã xoá công thức.");
                    }
                    break;
                case 0: break;
                default: System.out.println("Lựa chọn k hợp lệ!");
            }
        } while (chon != 0);
    }

    //1.Bán hàng
    private static void menuBanHang() {
        int chon;
        do {
            CauHinh.tieuDe("BÁN HÀNG");
            System.out.println("1. Lập hoá đơn mới");
            System.out.println("2. Gọi thêm món");
            System.out.println("3. Bớt / xoá món");
            System.out.println("4. Xem hoá đơn (bill)");
            System.out.println("5. Thanh toán");
            System.out.println("6. Huỷ hoá đơn");
            System.out.println("7. Danh sách hoá đơn đang chờ thanh toán");
            System.out.println("8. Xem thực đơn");
            System.out.println("0. Quay lại");
            chon = CauHinh.nhapSoNguyen("Chọn: ");
            try {
                switch (chon) {
                    case 1: lapHoaDon(); break;
                    case 2: goiMon(); break;
                    case 3: botMon(); break;
                    case 4: xemBill(); break;
                    case 5: thanhToan(); break;
                    case 6:
                        dsHD.huyHoaDon(CauHinh.nhapChuoi("Mã hoá đơn: "), CauHinh.nhapChuoi("Lý do huỷ: "));
                        System.out.println("Đã huỷ hoá đơn.");
                        break;
                    case 7: DanhSachHoaDon.inMang(dsHD.locTheoTrangThai(TrangThai.CHO_THANH_TOAN)); break;
                    case 8: dsSP.hienThiThucDon(dsNL); break;
                    case 0: break;
                    default: System.out.println("Lựa chọn k hợp lệ!");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } while (chon != 0);
    }

    private static void lapHoaDon() {
        String maNV;
        if (capDo == 1) {
            maNV = maDangNhap;   // thu ngân tự lập hoá đơn bằng mã của mình
        } else {
            dsNV.hienThi();
            maNV = CauHinh.nhapChuoi("Mã nhân viên lập hoá đơn (thu ngân / quản lý): ");
        }
        String maKH = CauHinh.nhapChuoi("Mã khách hàng (bỏ trống = khách vãng lai): ");
        HoaDon hd = dsHD.lapHoaDon(dsNV, maNV, dsKH, maKH);
        System.out.println("Đã lập hoá đơn " + hd.getMaHD() + ". Hãy gọi món!");
        if (CauHinh.xacNhan("Gọi món ngay")) {
            do {
                System.out.println("----- THỰC ĐƠN -----");
                dsSP.hienThiThucDon(dsNL);
                themMonVaoHoaDon(hd.getMaHD());
            } while (CauHinh.xacNhan("Gọi thêm món"));
        }
    }

    private static void themMonVaoHoaDon(String maHD) {
        String maSP = CauHinh.nhapChuoi("Mã sản phẩm: ");
        SanPham sp = dsSP.timTheoMa(maSP);
        if (sp == null) {
            System.out.println("Không có sản phẩm " + maSP);
            return;
        }
        List<NguyenLieu> khoa = sp.nguyenLieuDangKhoa(dsNL);
        if (!khoa.isEmpty()) {
            StringBuilder ds = new StringBuilder();
            for (NguyenLieu nl : khoa) {
                ds.append(ds.length() > 0 ? ", " : "").append(nl.getTenNL());
            }
            System.out.println("Món \"" + sp.getTenSP() + "\" đang bị KHOÁ (nguyên liệu: " + ds + "), k order được.");
            return;
        }
        int coThe = sp.soLuongCoTheTao(dsNL);
        if (coThe != CongThuc.KHONG_GIOI_HAN) {
            System.out.println("Dự kiến làm được tối đa: " + coThe + " phần");
        }
        int sl = CauHinh.nhapSoNguyen("Số lượng: ");
        HoaDon hd = dsHD.timTheoMa(maHD);
        if (hd != null && sl > 0) {
            List<String> thieu = nguyenLieuThieuKhiThem(hd, sp, sl);
            if (!thieu.isEmpty()) {
                System.out.println("K đủ nguyên liệu để thêm " + sl + " x " + sp.getTenSP() + ": " + String.join("; ", thieu));
                return;
            }
        }
        dsHD.themMon(maHD, sp, sl);
        System.out.println("Đã thêm: " + sp.getTenSP() + " x" + sl);
    }

    // Tổng nguyên liệu cần cho cả hoá đơn (các món đã gọi + sl phần của sp) so với tồn kho; trả về các nguyên liệu thiếu
    private static List<String> nguyenLieuThieuKhiThem(HoaDon hd, SanPham sp, int sl) {
        CongThuc can = new CongThuc();
        for (model.ChiTietHoaDon ct : hd.getDanhSachChiTiet()) {
            SanPham daGoi = dsSP.timTheoMa(ct.getMaSP());
            if (daGoi != null) {
                can.congThem(daGoi.getCongThuc(), ct.getSoLuong());
            }
        }
        can.congThem(sp.getCongThuc(), sl);
        return can.nguyenLieuKhongDu(dsNL);
    }

    private static void goiMon() {
        themMonVaoHoaDon(CauHinh.nhapChuoi("Mã hoá đơn: "));
    }

    private static void botMon() {
        String maHD = CauHinh.nhapChuoi("Mã hoá đơn: ");
        String maSP = CauHinh.nhapChuoi("Mã sản phẩm: ");
        int sl = CauHinh.nhapSoNguyen("Số lượng bớt (0 = xoá hẳn món): ");
        boolean ok = sl <= 0 ? dsHD.xoaMon(maHD, maSP) : dsHD.giamMon(maHD, maSP, sl);
        System.out.println(ok ? "Đã cập nhật." : "Hoá đơn k có món này.");
    }

    private static void xemBill() {
        HoaDon hd = dsHD.timTheoMa(CauHinh.nhapChuoi("Mã hoá đơn: "));
        System.out.println(hd == null ? "Không tìm thấy hoá đơn." : hd.taoBill());
    }

    private static void thanhToan() {
        String maHD = CauHinh.nhapChuoi("Mã hoá đơn: ");
        HoaDon hd = dsHD.timTheoMa(maHD);
        if (hd == null) {
            System.out.println("Không tìm thấy hoá đơn.");
            return;
        }
        System.out.println(hd.taoBill());
        KhachHang kh = hd.getMaKH().isEmpty() ? null : dsKH.timTheoMa(hd.getMaKH());

        //gợi ý khuyến mãi tốt nhất
        KhuyenMai km = null;
        KhuyenMai tot = dsKM.layTotNhat(hd.tongTienHang(), LocalDate.now());
        if (tot != null) {
            System.out.println("Khuyến mãi tốt nhất: " + tot.getMoTa());
            if (CauHinh.xacNhan("Áp dụng khuyến mãi này")) {
                km = tot;
            }
        }
        if (km == null) {
            String maKM = CauHinh.nhapChuoi("Mã khuyến mãi khác (bỏ trống = không dùng): ");
            if (!maKM.isEmpty()) {
                km = dsKM.timTheoMa(maKM);
                if (km == null) {
                    System.out.println("Không có khuyến mãi " + maKM);
                    return;
                }
            }
        }
        int diem = 0;
        if (kh != null && kh.getDiemTichLuy() > 0) {
            diem = CauHinh.nhapSoNguyen("Khách có " + kh.getDiemTichLuy() + " điểm, muốn đổi bao nhiêu (0 = k đổi): ");
        }
        System.out.println("Số tiền khách phải trả: "
                + IGiamGia.dinhDangTien(hd.xemTruocThanhToan(kh, km, diem)));

        System.out.println("Phương thức: 1. Tiền mặt  2. Chuyển khoản  3. Thẻ  4. Ví điện tử");
        PhuongThuc[] pts = PhuongThuc.values();
        int p = CauHinh.nhapSoNguyen("Chọn: ");
        if (p < 1 || p > pts.length) {
            System.out.println("Phương thức k hợp lệ.");
            return;
        }
        double dua = pts[p - 1] == PhuongThuc.TIEN_MAT ? CauHinh.nhapSoThuc("Tiền khách đưa (VND): ") : 0;
        double thoi = dsHD.thanhToan(maHD, dsKH, km, diem, pts[p - 1], dua);
        System.out.println("Thanh toán thành công! Tiền thối: " + IGiamGia.dinhDangTien(thoi));
        System.out.println(hd.taoBill());
    }

    //2.Hoá đơn & thống kê
    private static void menuHoaDon() {
        int chon;
        do {
            CauHinh.tieuDe("HOÁ ĐƠN & THỐNG KÊ");
            System.out.println("1. Danh sách tất cả hoá đơn");
            System.out.println("2. Xem 1 hoá đơn theo mã");
            System.out.println("3. Lọc theo trạng thái");
            System.out.println("4. Lọc theo nhân viên");
            System.out.println("5. Lọc theo khách hàng");
            System.out.println("6. Lọc theo khoảng ngày lập");
            System.out.println("7. Báo cáo doanh thu theo ngày");
            System.out.println("8. Báo cáo doanh thu theo tháng");
            System.out.println("9. Báo cáo doanh thu theo năm");
            System.out.println("10. Báo cáo tổng hợp theo khoảng ngày");
            System.out.println("11. Thống kê theo sản phẩm");
            System.out.println("12. Thống kê theo nhân viên");
            System.out.println("0. Quay lại");
            chon = CauHinh.nhapSoNguyen("Chọn: ");
            try {
                switch (chon) {
                    case 1: dsHD.hienThi(); break;
                    case 2: xemBill(); break;
                    case 3:
                        System.out.println("1. Chờ thanh toán  2. Đã thanh toán  3. Đã huỷ");
                        int t = CauHinh.nhapSoNguyen("Chọn: ");
                        if (t >= 1 && t <= 3) {
                            DanhSachHoaDon.inMang(dsHD.locTheoTrangThai(TrangThai.values()[t - 1]));
                        }
                        break;
                    case 4: DanhSachHoaDon.inMang(dsHD.locTheoNhanVien(CauHinh.nhapChuoi("Mã nhân viên: "))); break;
                    case 5: DanhSachHoaDon.inMang(dsHD.locTheoKhachHang(CauHinh.nhapChuoi("Mã khách hàng: "))); break;
                    case 6:
                        LocalDate tu = CauHinh.nhapNgay("Từ ngày (dd/MM/yyyy): ", null);
                        LocalDate den = CauHinh.nhapNgay("Đến ngày (dd/MM/yyyy): ", null);
                        DanhSachHoaDon.inMang(dsHD.locTheoNgayLap(tu, den));
                        break;
                    case 7: dsHD.inBaoCaoNgay(CauHinh.nhapNgay("Ngày (bỏ trống = hôm nay): ", LocalDate.now())); break;
                    case 8:
                        dsHD.inBaoCaoThang(CauHinh.nhapSoNguyen("Tháng: "), CauHinh.nhapSoNguyen("Năm: "));
                        break;
                    case 9: dsHD.inBaoCaoNam(CauHinh.nhapSoNguyen("Năm: ")); break;
                    case 10:
                        dsHD.inBaoCaoTongHop(CauHinh.nhapNgay("Từ ngày: ", null), CauHinh.nhapNgay("Đến ngày: ", null));
                        break;
                    case 11:
                        dsHD.inThongKeSanPham(CauHinh.nhapNgay("Từ ngày: ", null), CauHinh.nhapNgay("Đến ngày: ", null));
                        break;
                    case 12:
                        dsHD.inThongKeNhanVien(CauHinh.nhapNgay("Từ ngày: ", null), CauHinh.nhapNgay("Đến ngày: ", null));
                        break;
                    case 0: break;
                    default: System.out.println("Lựa chọn k hợp lệ!");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } while (chon != 0);
    }

    //3.Sản phẩm
    private static void menuSanPham() {
        int chon;
        do {
            CauHinh.tieuDe("QUẢN LÝ SẢN PHẨM");
            System.out.println("1. Hiển thị danh sách");
            System.out.println("2. Nhập n sản phẩm mới");
            System.out.println("3. Thêm 1 sản phẩm");
            System.out.println("4. Sửa sản phẩm (tên, giá)");
            System.out.println("5. Xoá sản phẩm");
            System.out.println("6. Tìm theo mã");
            System.out.println("7. Tìm gần đúng theo mã / tên");
            System.out.println("8. Tìm theo loại");
            System.out.println("9. Tìm theo khoảng giá");
            System.out.println("10. Tìm nâng cao (tên + loại + giá)");
            System.out.println("11. Sắp xếp theo giá");
            System.out.println("12. Thống kê");
            System.out.println("13. Combo tiết kiệm bao nhiêu so với mua lẻ");
            System.out.println("0. Quay lại");
            chon = CauHinh.nhapSoNguyen("Chọn: ");
            try {
                switch (chon) {
                    case 1: dsSP.hienThi(); break;
                    case 2: dsSP.nhapN(CauHinh.SCANNER); break;
                    case 3:
                        SanPham sp = DanhSachSanPham.nhapMotSanPham(CauHinh.SCANNER);
                        System.out.println(dsSP.them(sp) ? "Đã thêm " + sp.getMaSP() : "Mã đã tồn tại!");
                        break;
                    case 4:
                        String ma = CauHinh.nhapChuoi("Mã sản phẩm: ");
                        String ten = CauHinh.nhapChuoi("Tên mới (bỏ trống = giữ nguyên): ");
                        String gia = CauHinh.nhapChuoi("Giá mới (bỏ trống = giữ nguyên): ");
                        Double giaMoi = gia.isEmpty() ? null : Double.valueOf(gia);
                        System.out.println(dsSP.sua(ma, ten, giaMoi) ? "Đã sửa." : "K sửa được (sai mã hoặc sai dữ liệu).");
                        break;
                    case 5:
                        System.out.println(dsSP.xoa(CauHinh.nhapChuoi("Mã sản phẩm: ")) ? "Đã xoá." : "Không có mã này.");
                        break;
                    case 6:
                        SanPham t = dsSP.timTheoMa(CauHinh.nhapChuoi("Mã sản phẩm: "));
                        if (t == null) {
                            System.out.println("Không tìm thấy.");
                        } else {
                            t.hienThiThongTin();   // đa hình: mỗi loại in theo cách riêng
                        }
                        break;
                    case 7: DanhSachSanPham.inMang(dsSP.timGanDung(CauHinh.nhapChuoi("Từ khoá: "))); break;
                    case 8: DanhSachSanPham.inMang(dsSP.timTheoLoai(CauHinh.nhapChuoi("Loại (Đồ ăn / Thức uống / Combo): "))); break;
                    case 9:
                        DanhSachSanPham.inMang(dsSP.timTheoKhoangGia(
                                CauHinh.nhapSoThuc("Giá từ: "), CauHinh.nhapSoThuc("Giá đến: ")));
                        break;
                    case 10:
                        String kTen = CauHinh.nhapChuoi("Tên chứa (bỏ trống = bỏ qua): ");
                        String kLoai = CauHinh.nhapChuoi("Loại (bỏ trống = bỏ qua): ");
                        String g1 = CauHinh.nhapChuoi("Giá từ (bỏ trống = bỏ qua): ");
                        String g2 = CauHinh.nhapChuoi("Giá đến (bỏ trống = bỏ qua): ");
                        DanhSachSanPham.inMang(dsSP.timNangCao(kTen, kLoai,
                                g1.isEmpty() ? null : Double.valueOf(g1), g2.isEmpty() ? null : Double.valueOf(g2)));
                        break;
                    case 11:
                        DanhSachSanPham.inMang(dsSP.sapXepTheoGia(CauHinh.xacNhan("Tăng dần")));
                        break;
                    case 12: dsSP.inThongKe(); break;
                    case 13: dsSP.inTietKiemCombo(); break;
                    case 0: break;
                    default: System.out.println("Lựa chọn k hợp lệ!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: phải nhập số hợp lệ");
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } while (chon != 0);
    }

    //4.Khách hàng
    private static void menuKhachHang() {
        int chon;
        do {
            CauHinh.tieuDe("QUẢN LÝ KHÁCH HÀNG");
            System.out.println("1. Hiển thị danh sách");
            System.out.println("2. Nhập n khách hàng mới");
            System.out.println("3. Đăng ký nhanh 1 khách");
            System.out.println("4. Sửa thông tin (tên, sđt)");
            System.out.println("5. Xoá khách hàng");
            System.out.println("6. Tìm theo mã");
            System.out.println("7. Tìm theo số điện thoại");
            System.out.println("8. Tìm gần đúng (mã / tên / sđt)");
            System.out.println("9. Tìm nâng cao (tên + loại + chi tiêu)");
            System.out.println("10. Thống kê khách hàng");
            System.out.println("11. Top khách chi tiêu nhiều nhất");
            System.out.println("12. Xem ưu đãi theo hạng cho 1 mức tiền");
            System.out.println("0. Quay lại");
            chon = CauHinh.nhapSoNguyen("Chọn: ");
            try {
                switch (chon) {
                    case 1: dsKH.hienThi(); break;
                    case 2: dsKH.nhapN(CauHinh.SCANNER); break;
                    case 3:
                        KhachHang moi = dsKH.dangKy(CauHinh.nhapChuoi("Họ tên: "), CauHinh.nhapChuoi("Số điện thoại: "),
                                CauHinh.xacNhan("Khách thân thiết"));
                        System.out.println(moi == null ? "Số điện thoại đã tồn tại!" : "Đã đăng ký " + moi.getMaKH());
                        break;
                    case 4:
                        boolean ok = dsKH.sua(CauHinh.nhapChuoi("Mã khách: "),
                                CauHinh.nhapChuoi("Tên mới (bỏ trống = giữ nguyên): "),
                                CauHinh.nhapChuoi("SĐT mới (bỏ trống = giữ nguyên): "));
                        System.out.println(ok ? "Đã sửa." : "K sửa được (sai mã, sđt k hợp lệ hoặc trùng).");
                        break;
                    case 5: System.out.println(dsKH.xoa(CauHinh.nhapChuoi("Mã khách: ")) ? "Đã xoá." : "Không có mã này."); break;
                    case 6:
                        KhachHang kh = dsKH.timTheoMa(CauHinh.nhapChuoi("Mã khách: "));
                        System.out.println(kh == null ? "Không tìm thấy." : KhachHang.tieuDeBang() + "\n" + kh);
                        break;
                    case 7:
                        KhachHang k2 = dsKH.timTheoSDT(CauHinh.nhapChuoi("Số điện thoại: "));
                        System.out.println(k2 == null ? "Không tìm thấy." : KhachHang.tieuDeBang() + "\n" + k2);
                        break;
                    case 8: DanhSachKhachHang.inMang(dsKH.timGanDung(CauHinh.nhapChuoi("Từ khoá: "))); break;
                    case 9:
                        String ten = CauHinh.nhapChuoi("Tên chứa (bỏ trống = bỏ qua): ");
                        String loai = CauHinh.nhapChuoi("Loại THUONG / THANTHIET / DONG / BAC / VANG / KIM_CUONG (bỏ trống = bỏ qua): ");
                        String c1 = CauHinh.nhapChuoi("Chi tiêu từ (bỏ trống = bỏ qua): ");
                        String c2 = CauHinh.nhapChuoi("Chi tiêu đến (bỏ trống = bỏ qua): ");
                        DanhSachKhachHang.inMang(dsKH.timNangCao(ten, loai,
                                c1.isEmpty() ? null : Double.valueOf(c1), c2.isEmpty() ? null : Double.valueOf(c2)));
                        break;
                    case 10: dsKH.inThongKe(); break;
                    case 11: DanhSachKhachHang.inMang(dsKH.topKhach(CauHinh.nhapSoNguyen("Lấy top mấy: "))); break;
                    case 12: dsKH.inUuDaiTheoKhach(CauHinh.nhapSoThuc("Tổng tiền hàng (VND): ")); break;
                    case 0: break;
                    default: System.out.println("Lựa chọn k hợp lệ!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: phải nhập số hợp lệ");
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } while (chon != 0);
    }

    //5.Khuyến mãi
    private static void menuKhuyenMai() {
        int chon;
        do {
            CauHinh.tieuDe("QUẢN LÝ KHUYẾN MÃI");
            System.out.println("1. Hiển thị danh sách");
            System.out.println("2. Thêm chương trình khuyến mãi");
            System.out.println("3. Sửa khuyến mãi");
            System.out.println("4. Xoá khuyến mãi");
            System.out.println("5. Tìm theo tên");
            System.out.println("6. Xem khuyến mãi còn hiệu lực hôm nay");
            System.out.println("7. Gợi ý khuyến mãi tốt nhất cho 1 mức tiền");
            System.out.println("0. Quay lại");
            chon = CauHinh.nhapSoNguyen("Chọn: ");
            try {
                switch (chon) {
                    case 1: dsKM.hienThi(); break;
                    case 2:
                        KhuyenMai km = new KhuyenMai(CauHinh.nhapChuoi("Tên chương trình: "),
                                CauHinh.nhapSoThuc("Phần trăm giảm (0-100]: "),
                                CauHinh.nhapSoThuc("Giảm tối đa (VND, 0 = k giới hạn): "),
                                CauHinh.nhapSoThuc("Đơn tối thiểu (VND): "),
                                CauHinh.nhapNgay("Từ ngày (dd/MM/yyyy): ", null),
                                CauHinh.nhapNgay("Đến ngày (dd/MM/yyyy): ", null));
                        System.out.println(dsKM.them(km) ? "Đã thêm " + km.getMaKM() : "Mã đã tồn tại!");
                        break;
                    case 3:
                        String ma = CauHinh.nhapChuoi("Mã khuyến mãi: ");
                        String ten = CauHinh.nhapChuoi("Tên mới (bỏ trống = giữ nguyên): ");
                        String pt = CauHinh.nhapChuoi("Phần trăm mới (bỏ trống = giữ nguyên): ");
                        String max = CauHinh.nhapChuoi("Giảm tối đa mới (bỏ trống = giữ nguyên): ");
                        String min = CauHinh.nhapChuoi("Đơn tối thiểu mới (bỏ trống = giữ nguyên): ");
                        boolean ok = dsKM.sua(ma, ten, pt.isEmpty() ? null : Double.valueOf(pt),
                                max.isEmpty() ? null : Double.valueOf(max), min.isEmpty() ? null : Double.valueOf(min));
                        System.out.println(ok ? "Đã sửa." : "K sửa được.");
                        break;
                    case 4: System.out.println(dsKM.xoa(CauHinh.nhapChuoi("Mã khuyến mãi: ")) ? "Đã xoá." : "Không có mã này."); break;
                    case 5:
                        for (KhuyenMai x : dsKM.timTheoTen(CauHinh.nhapChuoi("Từ khoá: "))) {
                            System.out.println(x);
                        }
                        break;
                    case 6:
                        KhuyenMai[] conHan = dsKM.locConHieuLuc(LocalDate.now());
                        for (KhuyenMai x : conHan) {
                            System.out.println(x);
                        }
                        System.out.println("Đang chạy: " + conHan.length + " chương trình");
                        break;
                    case 7:
                        double tien = CauHinh.nhapSoThuc("Tổng tiền hàng (VND): ");
                        KhuyenMai tot = dsKM.layTotNhat(tien, LocalDate.now());
                        System.out.println(tot == null ? "Không có khuyến mãi nào áp dụng được."
                                : tot.getMoTa() + " -> giảm " + IGiamGia.dinhDangTien(tot.tinhGiamGia(tien, LocalDate.now())));
                        break;
                    case 0: break;
                    default: System.out.println("Lựa chọn k hợp lệ!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: phải nhập số hợp lệ");
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } while (chon != 0);
    }

    //6. Nhân viên
    private static void menuNhanVien() {
        int chon;
        do {
            CauHinh.tieuDe("QUẢN LÝ NHÂN VIÊN");
            System.out.println("1. Hiển thị danh sách");
            System.out.println("2. Thêm nhân viên (tự tạo tài khoản, mật khẩu mặc định " + MAT_KHAU_MAC_DINH + ")");
            System.out.println("3. Xoá nhân viên");
            System.out.println("4. Tìm kiếm (mã / tên / sđt / địa chỉ)");
            System.out.println("5. Tìm kiếm nâng cao");
            System.out.println("6. Chấm công");
            System.out.println("7. Sửa chấm công");
            System.out.println("8. Xoá chấm công");
            System.out.println("9. Ghi nhận số ly pha chế");
            System.out.println("10. Bảng chấm công theo tháng");
            System.out.println("11. Phiếu lương 1 nhân viên");
            System.out.println("12. Bảng lương toàn bộ theo tháng");
            System.out.println("13. Thống kê nhân viên theo tháng");
            System.out.println("0. Quay lại");
            chon = CauHinh.nhapSoNguyen("Chọn: ");
            try {
                switch (chon) {
                    case 1: dsNV.hienThi(); break;
                    case 2: themNhanVien(); break;
                    case 3:
                        String maXoa = CauHinh.nhapChuoi("Mã nhân viên: ");
                        if (maXoa.equalsIgnoreCase(maDangNhap)) {
                            System.out.println("Không thể xoá chính tài khoản đang đăng nhập.");
                        } else if (dsNV.xoa(maXoa)) {
                            xoaTaiKhoan(maXoa);
                            luuDanhSachNhanVien();
                            System.out.println("Đã xoá (kèm tài khoản đăng nhập).");
                        } else {
                            System.out.println("Không có mã này.");
                        }
                        break;
                    case 4:
                        List<NhanVien> kq = dsNV.timKiem(CauHinh.nhapChuoi("Từ khoá: "));
                        kq.forEach(System.out::println);
                        System.out.println("Tìm thấy: " + kq.size() + " nhân viên");
                        break;
                    case 5: timKiemNhanVienNangCao(); break;
                    case 6:
                        String ma = CauHinh.nhapChuoi("Mã nhân viên: ");
                        LocalDate ngay = CauHinh.nhapNgay("Ngày (bỏ trống = hôm nay): ", LocalDate.now());
                        double gio = CauHinh.nhapSoThuc("Số giờ làm: ");
                        System.out.println(dsNV.chamCong(ma, ngay, gio) ? "Đã chấm công." : "K chấm được (sai mã, sai số giờ hoặc ngày đã chấm).");
                        break;
                    case 7:
                        ma = CauHinh.nhapChuoi("Mã nhân viên: ");
                        ngay = CauHinh.nhapNgay("Ngày chấm công: ", null);
                        gio = CauHinh.nhapSoThuc("Số giờ làm mới: ");
                        System.out.println(dsNV.suaChamCong(ma, ngay, gio) ? "Đã sửa chấm công." : "Không sửa được.");
                        break;
                    case 8:
                        ma = CauHinh.nhapChuoi("Mã nhân viên: ");
                        ngay = CauHinh.nhapNgay("Ngày chấm công: ", null);
                        System.out.println(dsNV.xoaChamCong(ma, ngay) ? "Đã xoá chấm công." : "Không xoá được.");
                        break;
                    case 9:
                        ma = CauHinh.nhapChuoi("Mã pha chế: ");
                        int thangPhaChe = CauHinh.nhapSoNguyen("Tháng: ");
                        int namPhaChe = CauHinh.nhapSoNguyen("Năm: ");
                        int soLy = CauHinh.nhapSoNguyen("Số ly: ");
                        System.out.println(dsNV.ghiNhanSoLy(ma, thangPhaChe, namPhaChe, soLy)
                                ? "Đã ghi nhận số ly." : "Không ghi nhận được (sai mã hoặc số ly).");
                        break;
                    case 10: dsNV.inBangChamCong(CauHinh.nhapSoNguyen("Tháng: "), CauHinh.nhapSoNguyen("Năm: ")); break;
                    case 11:
                        NhanVien nv = dsNV.timTheoMa(CauHinh.nhapChuoi("Mã nhân viên: "));
                        if (nv == null) {
                            System.out.println("Không tìm thấy.");
                        } else {
                            System.out.println(nv.phieuLuong(CauHinh.nhapSoNguyen("Tháng: "), CauHinh.nhapSoNguyen("Năm: ")));
                        }
                        break;
                    case 12: dsNV.inBangLuong(CauHinh.nhapSoNguyen("Tháng: "), CauHinh.nhapSoNguyen("Năm: ")); break;
                    case 13: dsNV.inThongKe(CauHinh.nhapSoNguyen("Tháng: "), CauHinh.nhapSoNguyen("Năm: ")); break;
                    case 0: break;
                    default: System.out.println("Lựa chọn k hợp lệ!");
                }
            } catch (RuntimeException e) {
                System.out.println("Lỗi: " + e.getMessage());
            }
        } while (chon != 0);
    }

    private static void timKiemNhanVienNangCao() {
        System.out.println("1. Theo tên");
        System.out.println("2. Theo vai trò");
        System.out.println("3. Theo giới tính");
        System.out.println("4. Theo lương cơ bản");
        System.out.println("5. Theo lương thực lĩnh");
        int loai = CauHinh.nhapSoNguyen("Chọn kiểu tìm: ");
        List<NhanVien> kq;
        switch (loai) {
            case 1:
                kq = dsNV.timTheoTen(CauHinh.nhapChuoi("Tên: "));
                break;
            case 2:
                String vaiTro = CauHinh.nhapChuoi("Vai trò (TN/PC/QL): ").trim().toUpperCase();
                Class<? extends NhanVien> lop = vaiTro.equals("TN") ? ThuNgan.class
                        : vaiTro.equals("PC") ? PhaChe.class
                        : vaiTro.equals("QL") ? QuanLy.class : null;
                kq = dsNV.timTheoVaiTro(lop);
                break;
            case 3:
                kq = dsNV.timTheoGioiTinh(CauHinh.nhapChuoi("Giới tính: "));
                break;
            case 4:
                kq = dsNV.timTheoKhoangLuongCoBan(CauHinh.nhapSoThuc("Lương từ: "),
                        CauHinh.nhapSoThuc("Lương đến: "));
                break;
            case 5:
                int thang = CauHinh.nhapSoNguyen("Tháng: ");
                int nam = CauHinh.nhapSoNguyen("Năm: ");
                kq = dsNV.timTheoKhoangLuong(CauHinh.nhapSoThuc("Lương từ: "),
                        CauHinh.nhapSoThuc("Lương đến: "), thang, nam);
                break;
            default:
                System.out.println("Kiểu tìm không hợp lệ.");
                return;
        }
        kq.forEach(System.out::println);
        System.out.println("Tìm thấy: " + kq.size() + " nhân viên");
    }

    private static void themNhanVien() {
        System.out.println("Loại: 1. Thu ngân  2. Pha chế  3. Quản lý");
        int loai = CauHinh.nhapSoNguyen("Chọn: ");
        String ma = dsNV.maMoiTuDong();
        String ten = CauHinh.nhapChuoi("Họ tên: ");
        LocalDate ns = CauHinh.nhapNgay("Ngày sinh (dd/MM/yyyy): ", null);
        String gt = CauHinh.nhapChuoi("Giới tính (Nam/Nữ): ");
        String sdt = CauHinh.nhapChuoi("Số điện thoại: ");
        String dc = CauHinh.nhapChuoi("Địa chỉ: ");
        LocalDate vao = CauHinh.nhapNgay("Ngày vào làm (bỏ trống = hôm nay): ", LocalDate.now());
        double luong = CauHinh.nhapSoThuc("Lương cơ bản (VND): ");
        NhanVien nv;
        switch (loai) {
            case 1:
                ThuNgan.CaLam ca = ThuNgan.CaLam.valueOf(CauHinh.nhapChuoi("Ca làm (SANG/CHIEU/TOI): ").toUpperCase());
                nv = new ThuNgan(ma, ten, ns, gt, sdt, dc, vao, luong, ca, CauHinh.nhapSoNguyen("Số quầy: "), 0);
                break;
            case 2:
                PhaChe.CapBac cb = PhaChe.CapBac.valueOf(CauHinh.nhapChuoi("Cấp bậc (HOC_VIEC/CHINH_THUC/TRUONG_BAR): ").toUpperCase());
                nv = new PhaChe(ma, ten, ns, gt, sdt, dc, vao, luong, cb, 0);
                break;
            case 3:
                nv = new QuanLy(ma, ten, ns, gt, sdt, dc, vao, luong, CauHinh.nhapChuoi("Chức vụ: "),
                        CauHinh.nhapSoThuc("Phụ cấp chức vụ (VND): "), CauHinh.nhapSoThuc("Tỷ lệ thưởng KPI (0 - 1): "));
                break;
            default:
                System.out.println("Loại k hợp lệ.");
                return;
        }
        if (!dsNV.them(nv)) {
            System.out.println("Mã nhân viên đã tồn tại!");
            return;
        }
        System.out.println("Đã thêm " + nv.getMaNV());
        luuDanhSachNhanVien();

        // Tạo tài khoản đăng nhập: cấp độ trùng với loại nhân viên (1 thu ngân, 2 pha chế, 3 quản lý)
        if (themTaiKhoan(nv.getMaNV(), MAT_KHAU_MAC_DINH, loai)) {
            System.out.println("Đã tạo tài khoản: " + nv.getMaNV() + " / " + MAT_KHAU_MAC_DINH
                    + " (cấp " + loai + "). Nhắc nhân viên đổi mật khẩu.");
        } else {
            System.out.println("Không tạo được tài khoản (đã tồn tại hoặc lỗi ghi file " + DUONG_DAN_TAI_KHOAN + ").");
        }
    }
}