package model;

import iface.IGiamGia;
import java.time.LocalDate;

// chương trình khuyến mãi: giảm theo %, có mức giảm tối đa, đơn tối thiểu với thời hạn áp dụng
public class KhuyenMai implements IGiamGia {

    private static int demKM = 0;   // đếm để tự sinh mã KM001, KM002...

    private String maKM;
    private String tenKM;
    private double phanTramGiam;    // trong khoảng (0, 100]
    private double giamToiDa;       // 0 = k giới hạn
    private double donToiThieu;     // đơn từ bao nhiêu mới áp dụng được
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;

    // ===================== Constructor =====================
    public KhuyenMai(String tenKM, double phanTramGiam, double giamToiDa, double donToiThieu,
                     LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        this(taoMaTuDong(), tenKM, phanTramGiam, giamToiDa, donToiThieu, ngayBatDau, ngayKetThuc);
    }

    public KhuyenMai(String maKM, String tenKM, double phanTramGiam, double giamToiDa, double donToiThieu,
                     LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        if (maKM == null || maKM.trim().isEmpty() || tenKM == null || tenKM.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã và tên khuyến mãi không được rỗng");
        }
        if (phanTramGiam <= 0 || phanTramGiam > 100) {
            throw new IllegalArgumentException("Phần trăm giảm phải trong khoảng (0, 100]");
        }
        if (ngayBatDau == null || ngayKetThuc == null || ngayBatDau.isAfter(ngayKetThuc)) {
            throw new IllegalArgumentException("Thời gian khuyến mãi không hợp lệ");
        }
        this.maKM = maKM.trim();
        this.tenKM = tenKM.trim();
        this.phanTramGiam = phanTramGiam;
        this.giamToiDa = Math.max(giamToiDa, 0);
        this.donToiThieu = Math.max(donToiThieu, 0);
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        capNhatBoDem(this.maKM);
    }

    // ===================== Static =====================
    public static String taoMaTuDong() {
        return String.format("KM%03d", ++demKM);
    }

    public static void capNhatBoDem(String ma) {
        try {
            int so = Integer.parseInt(ma.substring(2));
            if (so > demKM) {
                demKM = so;
            }
        } catch (RuntimeException e) {
            // mã k đúng dạng KMxxx thì bỏ qua
        }
    }

    // ===================== Nghiệp vụ =====================
    public boolean conHieuLuc(LocalDate ngay) {
        return !ngay.isBefore(ngayBatDau) && !ngay.isAfter(ngayKetThuc);
    }

    public boolean conHieuLuc() {
        return conHieuLuc(LocalDate.now());
    }

    // tính giảm giá vào 1 ngày cụ thể (để test cho dễ)
    public double tinhGiamGia(double tongTien, LocalDate ngay) {
        if (!conHieuLuc(ngay) || tongTien < donToiThieu) {
            return 0;
        }
        double giam = tongTien * phanTramGiam / 100;
        if (giamToiDa > 0 && giam > giamToiDa) {
            giam = giamToiDa;
        }
        return giam;
    }

    @Override
    public double tinhGiamGia(double tongTien) {
        return tinhGiamGia(tongTien, LocalDate.now());
    }

    @Override
    public String getMoTa() {
        String s = tenKM + ": giảm " + phanTramGiam + "%";
        if (giamToiDa > 0) {
            s += " (tối đa " + IGiamGia.dinhDangTien(giamToiDa) + ")";
        }
        if (donToiThieu > 0) {
            s += ", đơn từ " + IGiamGia.dinhDangTien(donToiThieu);
        }
        return s;
    }

    // ===================== Đọc - ghi file =====================
    public String toFileString() {
        return maKM + "|" + tenKM + "|" + phanTramGiam + "|" + String.format("%.0f", giamToiDa) + "|"
                + String.format("%.0f", donToiThieu) + "|" + ngayBatDau + "|" + ngayKetThuc;
    }

    public static KhuyenMai fromFileString(String dong) {
        String[] p = dong.split("\\|");
        if (p.length < 7) {
            throw new IllegalArgumentException("Thiếu dữ liệu: " + dong);
        }
        return new KhuyenMai(p[0].trim(), p[1].trim(), Double.parseDouble(p[2].trim()),
                Double.parseDouble(p[3].trim()), Double.parseDouble(p[4].trim()),
                LocalDate.parse(p[5].trim()), LocalDate.parse(p[6].trim()));
    }

    // ===================== Get / Set =====================
    public String getMaKM() { return maKM; }
    public String getTenKM() { return tenKM; }
    public double getPhanTramGiam() { return phanTramGiam; }
    public double getGiamToiDa() { return giamToiDa; }
    public double getDonToiThieu() { return donToiThieu; }
    public LocalDate getNgayBatDau() { return ngayBatDau; }
    public LocalDate getNgayKetThuc() { return ngayKetThuc; }

    public void setTenKM(String tenKM) {
        if (tenKM == null || tenKM.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khuyến mãi không được rỗng");
        }
        this.tenKM = tenKM.trim();
    }

    public void setPhanTramGiam(double phanTramGiam) {
        if (phanTramGiam <= 0 || phanTramGiam > 100) {
            throw new IllegalArgumentException("Phần trăm giảm phải trong khoảng (0, 100]");
        }
        this.phanTramGiam = phanTramGiam;
    }

    public void setGiamToiDa(double giamToiDa) { this.giamToiDa = Math.max(giamToiDa, 0); }
    public void setDonToiThieu(double donToiThieu) { this.donToiThieu = Math.max(donToiThieu, 0); }

    public void setThoiGian(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        if (ngayBatDau == null || ngayKetThuc == null || ngayBatDau.isAfter(ngayKetThuc)) {
            throw new IllegalArgumentException("Thời gian khuyến mãi không hợp lệ");
        }
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
    }

    @Override
    public String toString() {
        return String.format("%-7s %-26s %5.1f%% %14s %12s %s -> %s %s",
                maKM, tenKM, phanTramGiam,
                giamToiDa > 0 ? IGiamGia.dinhDangTien(giamToiDa) : "không giới hạn",
                IGiamGia.dinhDangTien(donToiThieu), ngayBatDau, ngayKetThuc,
                conHieuLuc() ? "[Đang chạy]" : "[Hết hạn/Chưa tới]");
    }
}
