package service;

import iface.IFile;
import iface.IGiamGia;
import iface.IQuanLy;
import model.Combo;
import model.CongThuc;
import model.DoAn;
import model.NguyenLieu;
import model.SanPham;
import model.ThucUong;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

/**
 * Danh sách sản phẩm (lớp danh sách trung tâm). Dùng MẢNG SanPham[], chứa lẫn DoAn / ThucUong / Combo (đa hình).
 * Định dạng file xem ở comment đầu lớp SanPham.
 */
public class DanhSachSanPham implements IQuanLy<SanPham>, IFile {

    private SanPham[] ds;
    private int n;
    private boolean dangDoc = false;   // đang đọc file: chưa nối combo vì thành phần có thể nằm phía sau

    // ===================== Constructor =====================
    public DanhSachSanPham() {
        this(10);
    }

    public DanhSachSanPham(int sucChua) {
        ds = new SanPham[Math.max(sucChua, 1)];
        n = 0;
    }

    // ===================== TOOLS =====================
    private void moRong() {
        if (n == ds.length) {
            ds = Arrays.copyOf(ds, ds.length * 2);
        }
    }

    private int timViTri(String ma) {
        if (ma == null) {
            return -1;
        }
        for (int i = 0; i < n; i++) {
            if (ds[i].getMaSP().equalsIgnoreCase(ma.trim())) {
                return i;
            }
        }
        return -1;
    }

    private SanPham[] loc(Predicate<SanPham> dieuKien) {
        SanPham[] tam = new SanPham[n];
        int dem = 0;
        for (int i = 0; i < n; i++) {
            if (dieuKien.test(ds[i])) {
                tam[dem++] = ds[i];
            }
        }
        return Arrays.copyOf(tam, dem);
    }

    // lấy sản phẩm theo vị trí, sai vị trí thì NULL
    public SanPham get(int i) {
        return (i >= 0 && i < n) ? ds[i] : null;
    }

    @Override
    public int soLuong() {
        return n;
    }

    // ===================== A. Nhập n phần tử đầu tiên =====================
    public void nhapN(Scanner sc) {
        int soLuongNhap = 0;
        while (soLuongNhap <= 0) {
            System.out.print("Nhập số sản phẩm cần thêm: ");
            try {
                soLuongNhap = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập số nguyên dương!");
            }
        }
        for (int i = 1; i <= soLuongNhap; i++) {
            System.out.println("--- Sản phẩm thứ " + i + " ---");
            boolean xong = false;
            while (!xong) {
                try {
                    SanPham sp = nhapMotSanPham(sc);
                    if (them(sp)) {
                        System.out.println("Đã thêm " + sp.getMaSP());
                        xong = true;
                    } else {
                        System.out.println("Mã sản phẩm đã tồn tại, nhập lại!");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Lỗi: " + e.getMessage() + " - nhập lại!");
                }
            }
        }
    }

    // nhập 1 sản phẩm từ bàn phím, mã tự sinh (đa hình: trả về đúng loại con)
    public static SanPham nhapMotSanPham(Scanner sc) {
        System.out.print("Loại (1 = Đồ ăn, 2 = Thức uống, 3 = Combo): ");
        String loai = sc.nextLine().trim();
        System.out.print("Tên sản phẩm: ");
        String ten = sc.nextLine();
        System.out.print("Đơn giá (VND): ");
        double gia = Double.parseDouble(sc.nextLine().trim());
        String ma = SanPham.taoMaTuDong();
        switch (loai) {
            case "1":
                System.out.print("Món chay? (c/k): ");
                return new DoAn(ma, ten, gia, sc.nextLine().trim().equalsIgnoreCase("c"));
            case "2":
                System.out.print("Size (S/M/L): ");
                return new ThucUong(ma, ten, gia, sc.nextLine());
            case "3":
                System.out.print("Mã các món trong combo (cách nhau bằng ;): ");
                return new Combo(ma, ten, gia, sc.nextLine());
            default:
                throw new IllegalArgumentException("Loại sản phẩm phải là 1, 2 hoặc 3");
        }
    }

    // ===================== B. Xuất danh sách =====================
    @Override
    public void hienThi() {
        System.out.println(SanPham.tieuDeBang());
        for (int i = 0; i < n; i++) {
            System.out.println(ds[i]);
        }
        System.out.println("Tổng: " + n + " sản phẩm");
    }

    public static void inMang(SanPham[] mang) {
        System.out.println(SanPham.tieuDeBang());
        for (SanPham sp : mang) {
            System.out.println(sp);
        }
        System.out.println("Tìm thấy: " + mang.length + " sản phẩm");
    }

    // ===================== C. Thêm 1 / k phần tử =====================
    // trùng mã hoặc null thì k thêm
    @Override
    public boolean them(SanPham sp) {
        if (sp == null || timViTri(sp.getMaSP()) != -1) {
            return false;
        }
        moRong();
        ds[n++] = sp;
        lienKetCombo();
        return true;
    }

    // thêm k sản phẩm cùng lúc, trả về số sản phẩm thêm được
    public int themK(SanPham[] mang) {
        int dem = 0;
        if (mang != null) {
            for (SanPham sp : mang) {
                if (them(sp)) {
                    dem++;
                }
            }
        }
        return dem;
    }

    // ===================== D. Sửa theo mã =====================
    // tham số null / rỗng = giữ nguyên. Trả về false nếu k có mã hoặc dữ liệu sai (khi đó k đổi gì)
    public boolean sua(String ma, String tenMoi, Double giaMoi) {
        SanPham sp = timTheoMa(ma);
        if (sp == null || (giaMoi != null && giaMoi < 0)) {
            return false;
        }
        if (tenMoi != null && !tenMoi.trim().isEmpty()) {
            sp.setTenSP(tenMoi);
        }
        if (giaMoi != null) {
            sp.setDonGia(giaMoi);
        }
        return true;
    }

    // ===================== E. Xoá theo mã =====================
    @Override
    public boolean xoa(String ma) {
        int vt = timViTri(ma);
        if (vt == -1) {
            return false;
        }
        for (int i = vt; i < n - 1; i++) {
            ds[i] = ds[i + 1];
        }
        ds[--n] = null;
        lienKetCombo();
        return true;
    }

    // ===================== F. Tìm kiếm: từ chính xác đến gần đúng, nhiều khoá =====================
    @Override
    public SanPham timTheoMa(String ma) {
        int vt = timViTri(ma);
        return vt == -1 ? null : ds[vt];
    }

    // tìm gần đúng theo mã hoặc tên (bỏ dấu, k phân biệt hoa thường)
    public SanPham[] timGanDung(String tuKhoa) {
        String k = DanhSachKhachHang.boDau(tuKhoa);
        return loc(sp -> DanhSachKhachHang.boDau(sp.getMaSP()).contains(k)
                || DanhSachKhachHang.boDau(sp.getTenSP()).contains(k));
    }

    // lọc theo loại: "Đồ ăn", "Thức uống", "Combo" (bỏ dấu, k phân biệt hoa thường)
    public SanPham[] timTheoLoai(String loai) {
        String k = DanhSachKhachHang.boDau(loai);
        return loc(sp -> DanhSachKhachHang.boDau(sp.getLoai()).equals(k));
    }

    public SanPham[] timTheoKhoangGia(double giaTu, double giaDen) {
        return loc(sp -> sp.getDonGia() >= giaTu && sp.getDonGia() <= giaDen);
    }

    // tìm nâng cao nhiều khoá cùng lúc: khoá nào null / rỗng thì bỏ qua
    public SanPham[] timNangCao(String ten, String loai, Double giaTu, Double giaDen) {
        String kTen = (ten == null || ten.trim().isEmpty()) ? null : DanhSachKhachHang.boDau(ten);
        String kLoai = (loai == null || loai.trim().isEmpty()) ? null : DanhSachKhachHang.boDau(loai);
        return loc(sp -> (kTen == null || DanhSachKhachHang.boDau(sp.getTenSP()).contains(kTen))
                && (kLoai == null || DanhSachKhachHang.boDau(sp.getLoai()).equals(kLoai))
                && (giaTu == null || sp.getDonGia() >= giaTu)
                && (giaDen == null || sp.getDonGia() <= giaDen));
    }

    // ===================== G. Sắp xếp (trả về mảng mới, k đổi thứ tự trong danh sách) =====================
    public SanPham[] sapXepTheoGia(boolean tang) {
        SanPham[] tam = Arrays.copyOf(ds, n);
        Comparator<SanPham> cmp = Comparator.comparingDouble(SanPham::getDonGia);
        Arrays.sort(tam, tang ? cmp : cmp.reversed());
        return tam;
    }

    public SanPham[] sapXepTheoTen() {
        SanPham[] tam = Arrays.copyOf(ds, n);
        Arrays.sort(tam, Comparator.comparing(sp -> DanhSachKhachHang.boDau(sp.getTenSP())));
        return tam;
    }

    // ===================== H. Thống kê =====================
    public int demTheoLoai(String loai) {
        return timTheoLoai(loai).length;
    }

    public double giaTrungBinh() {
        if (n == 0) {
            return 0;
        }
        double tong = 0;
        for (int i = 0; i < n; i++) {
            tong += ds[i].getDonGia();
        }
        return tong / n;
    }

    // sản phẩm giá cao nhất / thấp nhất, danh sách trống thì NULL
    public SanPham sanPhamDatNhat() {
        SanPham max = null;
        for (int i = 0; i < n; i++) {
            if (max == null || ds[i].getDonGia() > max.getDonGia()) {
                max = ds[i];
            }
        }
        return max;
    }

    public SanPham sanPhamReNhat() {
        SanPham min = null;
        for (int i = 0; i < n; i++) {
            if (min == null || ds[i].getDonGia() < min.getDonGia()) {
                min = ds[i];
            }
        }
        return min;
    }

    public void inThongKe() {
        System.out.println("Tổng số sản phẩm : " + n);
        System.out.println("  - Đồ ăn        : " + demTheoLoai("Đồ ăn"));
        System.out.println("  - Thức uống    : " + demTheoLoai("Thức uống"));
        System.out.println("  - Combo        : " + demTheoLoai("Combo"));
        if (n > 0) {
            System.out.println("Giá trung bình   : " + IGiamGia.dinhDangTien(giaTrungBinh()));
            System.out.println("Đắt nhất         : " + sanPhamDatNhat().getTenSP() + " ("
                    + IGiamGia.dinhDangTien(sanPhamDatNhat().getDonGia()) + ")");
            System.out.println("Rẻ nhất          : " + sanPhamReNhat().getTenSP() + " ("
                    + IGiamGia.dinhDangTien(sanPhamReNhat().getDonGia()) + ")");
        }
    }

    // in tiết kiệm của từng combo so với mua lẻ (đa hình + dùng interface IQuanLy)
    public void inTietKiemCombo() {
        boolean coCombo = false;
        for (int i = 0; i < n; i++) {
            if (ds[i] instanceof Combo) {
                Combo c = (Combo) ds[i];
                System.out.println(c.getTenSP() + ": mua lẻ " + IGiamGia.dinhDangTien(c.tinhGiaLe(this))
                        + " -> combo " + IGiamGia.dinhDangTien(c.getDonGia())
                        + " (tiết kiệm " + IGiamGia.dinhDangTien(c.tienTietKiem(this)) + ")");
                coCombo = true;
            }
        }
        if (!coCombo) {
            System.out.println("Chưa có combo nào.");
        }
    }

    // ===================== Công thức, nguyên liệu & thực đơn =====================

    // has-a: nối mã thành phần của mọi Combo với sản phẩm thật (gọi lại khi thêm / xoá / đọc file)
    private void lienKetCombo() {
        if (dangDoc) {
            return;
        }
        for (int i = 0; i < n; i++) {
            if (ds[i] instanceof Combo) {
                for (String ma : ((Combo) ds[i]).lienKet(this)) {
                    System.err.println("Combo " + ds[i].getMaSP() + ": thành phần \"" + ma
                            + "\" k có trong danh sách hoặc là combo, bỏ qua");
                }
            }
        }
    }

    // đặt (thêm / sửa) liều lượng 1 nguyên liệu trong công thức món. Combo k có công thức riêng -> false
    public boolean datLieuLuong(String maSP, String maNL, double lieuLuong, IQuanLy<NguyenLieu> kho) {
        SanPham sp = timTheoMa(maSP);
        if (sp == null || sp instanceof Combo || kho.timTheoMa(maNL) == null) {
            return false;
        }
        sp.getCongThuc().datLieuLuong(maNL, lieuLuong);
        return true;
    }

    public boolean xoaNguyenLieuKhoiCongThuc(String maSP, String maNL) {
        SanPham sp = timTheoMa(maSP);
        return sp != null && !(sp instanceof Combo) && sp.getCongThuc().xoaNguyenLieu(maNL);
    }

    public boolean xoaCongThuc(String maSP) {
        SanPham sp = timTheoMa(maSP);
        if (sp == null || sp instanceof Combo) {
            return false;
        }
        sp.getCongThuc().xoaHet();
        return true;
    }

    // các món (kể cả combo) có dùng nguyên liệu này trong công thức
    public SanPham[] timTheoNguyenLieu(String maNL) {
        return loc(sp -> sp.getCongThuc().coNguyenLieu(maNL));
    }

    // in công thức 1 món; combo thì in công thức từng món thành phần rồi tổng
    public boolean inCongThuc(String maSP, IQuanLy<NguyenLieu> kho) {
        SanPham sp = timTheoMa(maSP);
        if (sp == null) {
            return false;
        }
        System.out.println(sp.getTenSP() + " (" + sp.getMaSP() + ") - " + sp.getLoai());
        if (sp instanceof Combo) {
            Combo c = (Combo) sp;
            for (SanPham tp : c.getMonThanhPhan()) {
                System.out.println("  + " + tp.getTenSP() + " (" + tp.getMaSP() + "): " + tp.getCongThuc().moTa(kho));
            }
            System.out.println("  => Tổng công thức combo: " + c.getCongThuc().moTa(kho));
        } else {
            System.out.println("  Công thức (1 phần): " + sp.getCongThuc().moTa(kho));
        }
        return true;
    }

    public void inTatCaCongThuc(IQuanLy<NguyenLieu> kho) {
        for (int i = 0; i < n; i++) {
            inCongThuc(ds[i].getMaSP(), kho);
        }
    }

    // thực đơn cho thu ngân: kèm số phần dự kiến làm được theo tồn kho và trạng thái khoá
    public void hienThiThucDon(IQuanLy<NguyenLieu> kho) {
        System.out.println(String.format("%-6s %-10s %-26s %12s  %-10s %s",
                "Mã", "Loại", "Tên", "Đơn giá", "Có thể làm", "Tình trạng"));
        int soKhoa = 0;
        for (int i = 0; i < n; i++) {
            SanPham sp = ds[i];
            int coThe = sp.soLuongCoTheTao(kho);
            String soPhan = coThe == CongThuc.KHONG_GIOI_HAN ? "-" : coThe + " phần";
            String tinhTrang;
            java.util.List<NguyenLieu> khoa = sp.nguyenLieuDangKhoa(kho);
            if (!khoa.isEmpty()) {
                StringBuilder sb = new StringBuilder("ĐÃ KHOÁ (nguyên liệu: ");
                for (int k = 0; k < khoa.size(); k++) {
                    sb.append(k > 0 ? ", " : "").append(khoa.get(k).getTenNL());
                }
                tinhTrang = sb.append(")").toString();
                soKhoa++;
            } else if (coThe == 0) {
                tinhTrang = "HẾT NGUYÊN LIỆU";
            } else if (sp.getCongThuc().isRong()) {
                tinhTrang = "Chưa có công thức";
            } else {
                tinhTrang = "Còn";
            }
            System.out.println(String.format("%-6s %-10s %-26s %12s  %-10s %s", sp.getMaSP(), sp.getLoai(),
                    sp.getTenSP(), IGiamGia.dinhDangTien(sp.getDonGia()), soPhan, tinhTrang));
        }
        System.out.println("Tổng: " + n + " sản phẩm" + (soKhoa > 0 ? " (" + soKhoa + " món đã khoá, k order được)" : ""));
    }

    // ===================== Đọc / ghi file =====================
    /**
     * Đọc danh sách từ file (thay thế dữ liệu hiện tại). Dòng trống hoặc bắt đầu bằng '#' bị bỏ qua,
     * dòng sai định dạng bị báo lỗi ra System.err và bỏ qua.
     * @return số sản phẩm đọc được (file không tồn tại -> 0, giữ nguyên danh sách)
     */
    @Override
    public int docFile(String duongDan) throws IOException {
        Path path = Paths.get(duongDan);
        if (!Files.exists(path)) {
            return 0;
        }
        ds = new SanPham[10];
        n = 0;
        dangDoc = true;
        int soDong = 0;
        for (String dong : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            soDong++;
            dong = dong.replace("\uFEFF", "").trim();
            if (dong.isEmpty() || dong.startsWith("#")) {
                continue;
            }
            try {
                if (!them(SanPham.fromFileString(dong))) {
                    System.err.println("Dòng " + soDong + ": trùng mã sản phẩm, bỏ qua");
                }
            } catch (RuntimeException e) {
                System.err.println("Dòng " + soDong + " sai định dạng (" + e.getMessage() + "), bỏ qua");
            }
        }
        dangDoc = false;
        lienKetCombo();
        return n;
    }

    @Override
    public void ghiFile(String duongDan) throws IOException {
        Path path = Paths.get(duongDan);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        List<String> dong = new ArrayList<>();
        dong.add("# LOAI|maSP|tenSP|donGia|truongRieng|congThuc  (DOAN: laDoChay, THUCUONG: size, COMBO: cac ma SP cach nhau bang ; (lap lai = nhieu phan), congThuc: maNL:lieuLuong,maNL:lieuLuong - combo khong co cong thuc rieng)");
        for (int i = 0; i < n; i++) {
            dong.add(ds[i].toFileString());
        }
        Files.write(path, dong, StandardCharsets.UTF_8);
    }
}
