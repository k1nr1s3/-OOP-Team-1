package service;

import iface.IFile;
import iface.IQuanLy;
import model.KhuyenMai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Định dạng mỗi dòng trong data/khuyenmai.txt (ngăn cách bằng '|'):
 *   maKM|tenKM|phanTramGiam|giamToiDa|donToiThieu|ngayBatDau|ngayKetThuc   (ngày dạng yyyy-MM-dd)
 * Dùng MẢNG KhuyenMai[]. Lớp này k có trong cấu trúc ban đầu của nhóm trưởng, nhớ báo thêm vào service/
 */
public class DanhSachKhuyenMai implements IQuanLy<KhuyenMai>, IFile {

    private KhuyenMai[] ds;
    private int n;

    public DanhSachKhuyenMai() {
        this(10);
    }

    public DanhSachKhuyenMai(int sucChua) {
        ds = new KhuyenMai[Math.max(sucChua, 1)];
        n = 0;
    }

    // trả về vị trí trong mảng, k thấy thì -1
    private int timViTri(String ma) {
        if (ma == null) {
            return -1;
        }
        for (int i = 0; i < n; i++) {
            if (ds[i].getMaKM().equalsIgnoreCase(ma.trim())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int soLuong() {
        return n;
    }

    // ===================== Thêm / sửa / xoá =====================
    @Override
    public boolean them(KhuyenMai km) {
        if (km == null || timViTri(km.getMaKM()) != -1) {
            return false;
        }
        if (n == ds.length) {
            ds = Arrays.copyOf(ds, ds.length * 2);
        }
        ds[n++] = km;
        return true;
    }

    // thêm k khuyến mãi cùng lúc, trả về số cái thêm thành công
    public int themK(KhuyenMai[] mang) {
        int dem = 0;
        for (KhuyenMai km : mang) {
            if (them(km)) {
                dem++;
            }
        }
        return dem;
    }

    // tham số null = giữ nguyên
    public boolean sua(String ma, String tenMoi, Double phanTramMoi, Double giamToiDaMoi, Double donToiThieuMoi) {
        KhuyenMai km = timTheoMa(ma);
        if (km == null) {
            return false;
        }
        try {
            if (tenMoi != null && !tenMoi.trim().isEmpty()) {
                km.setTenKM(tenMoi);
            }
            if (phanTramMoi != null) {
                km.setPhanTramGiam(phanTramMoi);
            }
            if (giamToiDaMoi != null) {
                km.setGiamToiDa(giamToiDaMoi);
            }
            if (donToiThieuMoi != null) {
                km.setDonToiThieu(donToiThieuMoi);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

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
        return true;
    }

    // ===================== Tìm kiếm =====================
    @Override
    public KhuyenMai timTheoMa(String ma) {
        int vt = timViTri(ma);
        return vt == -1 ? null : ds[vt];
    }

    // tìm gần đúng theo tên (bỏ dấu, k phân biệt hoa thường)
    public KhuyenMai[] timTheoTen(String tuKhoa) {
        String k = DanhSachKhachHang.boDau(tuKhoa);
        KhuyenMai[] tam = new KhuyenMai[n];
        int dem = 0;
        for (int i = 0; i < n; i++) {
            if (DanhSachKhachHang.boDau(ds[i].getTenKM()).contains(k)) {
                tam[dem++] = ds[i];
            }
        }
        return Arrays.copyOf(tam, dem);
    }

    // lọc ra các khuyến mãi còn hiệu lực vào ngày này
    public KhuyenMai[] locConHieuLuc(LocalDate ngay) {
        KhuyenMai[] tam = new KhuyenMai[n];
        int dem = 0;
        for (int i = 0; i < n; i++) {
            if (ds[i].conHieuLuc(ngay)) {
                tam[dem++] = ds[i];
            }
        }
        return Arrays.copyOf(tam, dem);
    }

    // ===================== Nghiệp vụ =====================
    // lấy khuyến mãi giảm nhiều nhất với tổng tiền này (k có cái nào áp dụng được thì NULL)
    public KhuyenMai layTotNhat(double tongTien, LocalDate ngay) {
        KhuyenMai tot = null;
        double giamMax = 0;
        for (int i = 0; i < n; i++) {
            double giam = ds[i].tinhGiamGia(tongTien, ngay);
            if (giam > giamMax) {
                giamMax = giam;
                tot = ds[i];
            }
        }
        return tot;
    }

    // ===================== Hiển thị / thống kê =====================
    @Override
    public void hienThi() {
        for (int i = 0; i < n; i++) {
            System.out.println(ds[i]);
        }
        System.out.println("Tổng: " + n + " chương trình (đang chạy: " + locConHieuLuc(LocalDate.now()).length + ")");
    }

    // ===================== Đọc / ghi file =====================

    /**
     * Đọc danh sách từ file (thay thế dữ liệu hiện tại). Dòng trống hoặc bắt đầu bằng '#' bị bỏ qua,
     * dòng sai định dạng bị báo lỗi ra System.err và bỏ qua.
     * @return số phần tử đọc được (file không tồn tại -> 0, giữ nguyên danh sách)
     */
    @Override
    public int docFile(String duongDan) throws IOException {
        Path path = Paths.get(duongDan);
        if (!Files.exists(path)) {
            return 0;
        }
        ds = new KhuyenMai[10];
        n = 0;
        int soDong = 0;
        for (String dong : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            soDong++;
            dong = dong.replace("\uFEFF", "").trim();
            if (dong.isEmpty() || dong.startsWith("#")) {
                continue;
            }
            try {
                if (!them(KhuyenMai.fromFileString(dong))) {
                    System.err.println("Dòng " + soDong + ": trùng mã, bỏ qua");
                }
            } catch (RuntimeException e) {
                System.err.println("Dòng " + soDong + " sai định dạng (" + e.getMessage() + "), bỏ qua");
            }
        }
        return n;
    }

    @Override
    public void ghiFile(String duongDan) throws IOException {
        Path path = Paths.get(duongDan);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        List<String> dong = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            dong.add(ds[i].toFileString());
        }
        Files.write(path, dong, StandardCharsets.UTF_8);
    }
}
