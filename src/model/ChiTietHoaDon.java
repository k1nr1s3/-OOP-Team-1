package model;

import iface.IGiamGia;

// 1 dòng món trong hoá đơn: mã, tên, loại, đơn giá, số lượng
// LƯU BẢN SAO (snapshot) tên + giá + loại tại lúc bán, để sau này SanPham đổi giá / bị xoá thì hoá đơn cũ và thống kê vẫn đúng
public class ChiTietHoaDon {

    // ===================== Static =====================
    public static final int SO_LUONG_TOI_DA = 99;   // 1 dòng k bán quá số này (chống nhập nhầm)

    // ===================== Thuộc tính =====================
    private String maSP;
    private String tenSP;
    private String loai;
    private double donGia;
    private int soLuong;

    // ===================== Constructor =====================
    // tạo từ sản phẩm đang bán (lấy giá hiện tại)
    public ChiTietHoaDon(SanPham sp, int soLuong) {
        this(kiemTraSanPham(sp).getMaSP(), sp.getTenSP(), sp.getLoai(), sp.getDonGia(), soLuong);
    }

    // constructor đầy đủ, dùng khi đọc từ file
    public ChiTietHoaDon(String maSP, String tenSP, String loai, double donGia, int soLuong) {
        setMaSP(maSP);
        setTenSP(tenSP);
        setLoai(loai);
        setDonGia(donGia);
        setSoLuong(soLuong);
    }

    private static SanPham kiemTraSanPham(SanPham sp) {
        if (sp == null) {
            throw new IllegalArgumentException("Sản phẩm không được rỗng");
        }
        return sp;
    }

    // ===================== Phương thức static =====================
    // bỏ các ký tự dùng làm dấu ngăn cách trong file (| ; ~) và xuống dòng, để k làm hỏng file
    public static String lamSach(String s) {
        return s == null ? "" : s.replaceAll("[|;~\\r\\n]+", " ").trim();
    }

    // ===================== Nghiệp vụ =====================
    public double thanhTien() {
        return donGia * soLuong;
    }

    // cộng thêm số lượng (khách gọi thêm cùng 1 món), vượt mức tối đa thì ném lỗi
    public void tangSoLuong(int them) {
        if (them <= 0) {
            throw new IllegalArgumentException("Số lượng thêm phải > 0");
        }
        setSoLuong(soLuong + them);
    }

    // ===================== Đọc - ghi file =====================
    // dạng: maSP~tenSP~loai~donGia~soLuong
    public String toFileString() {
        return maSP + "~" + tenSP + "~" + loai + "~" + Math.round(donGia) + "~" + soLuong;
    }

    public static ChiTietHoaDon fromFileString(String chuoi) {
        String[] p = chuoi.split("~", -1);
        if (p.length < 5) {
            throw new IllegalArgumentException("Thiếu dữ liệu chi tiết: " + chuoi);
        }
        return new ChiTietHoaDon(p[0], p[1], p[2], Double.parseDouble(p[3].trim()), Integer.parseInt(p[4].trim()));
    }

    // ===================== Get / Set =====================
    public String getMaSP() { return maSP; }
    public String getTenSP() { return tenSP; }
    public String getLoai() { return loai; }
    public double getDonGia() { return donGia; }
    public int getSoLuong() { return soLuong; }

    public void setMaSP(String maSP) {
        String s = lamSach(maSP);
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Mã sản phẩm không được rỗng");
        }
        this.maSP = s;
    }

    public void setTenSP(String tenSP) {
        String s = lamSach(tenSP);
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được rỗng");
        }
        this.tenSP = s;
    }

    public void setLoai(String loai) {
        String s = lamSach(loai);
        this.loai = s.isEmpty() ? "Khác" : s;
    }

    public void setDonGia(double donGia) {
        if (donGia < 0) {
            throw new IllegalArgumentException("Đơn giá không được âm");
        }
        this.donGia = donGia;
    }

    public void setSoLuong(int soLuong) {
        if (soLuong <= 0 || soLuong > SO_LUONG_TOI_DA) {
            throw new IllegalArgumentException("Số lượng phải từ 1 đến " + SO_LUONG_TOI_DA);
        }
        this.soLuong = soLuong;
    }

    // ===================== Hiển thị =====================
    @Override
    public String toString() {
        return String.format("%-8s %-26s %4d %12s %14s",
                maSP, tenSP, soLuong, IGiamGia.dinhDangTien(donGia), IGiamGia.dinhDangTien(thanhTien()));
    }
}
