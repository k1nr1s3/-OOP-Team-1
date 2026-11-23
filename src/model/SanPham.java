package model;

import iface.IGiamGia;
import iface.IQuanLy;


public abstract class SanPham {

    //Tạo mã SP
    private static int demSP = 0;   
    public static String taoMaTuDong() {
        return String.format("SP%02d", ++demSP);
    }

    // cập nhật bộ đếm để mã tự sinh sau này k trùng với mã đọc từ file
    public static void capNhatBoDem(String ma) {
        try {
            int so = Integer.parseInt(ma.substring(2));
            if (so > demSP) {
                demSP = so;
            }
        } catch (RuntimeException e) {
            // mã k đúng dạng SPxx thì bỏ qua
        }
    }

    // tạo đúng loại sản phẩm từ 1 dòng trong file (đa hình theo trường LOAI)
    public static SanPham fromFileString(String dong) {
        String[] f = dong.split("\\|", -1);
        if (f.length < 4) {
            throw new IllegalArgumentException("Thiếu dữ liệu: " + dong);
        }
        String ma = f[1].trim();
        String ten = f[2].trim();
        double gia = Double.parseDouble(f[3].trim());
        String rieng = f.length > 4 ? f[4].trim() : "";
        SanPham sp;
        switch (f[0].trim().toUpperCase()) {
            case "DOAN":     sp = new DoAn(ma, ten, gia, Boolean.parseBoolean(rieng)); break;
            case "THUCUONG": sp = new ThucUong(ma, ten, gia, rieng.isEmpty() ? "M" : rieng); break;
            case "COMBO":    sp = new Combo(ma, ten, gia, rieng); break;
            default: throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + f[0]);
        }
        // trường thứ 6 (nếu có) là công thức; Combo k có công thức riêng (lấy từ các món thành phần)
        if (f.length > 5 && !(sp instanceof Combo)) {
            sp.setCongThuc(CongThuc.fromFileString(f[5]));
        }
        return sp;
    }

    public static String tieuDeBang() {
        return String.format("%-6s %-10s %-28s %12s  %s", "Mã", "Loại", "Tên", "Đơn giá", "Chi tiết");
    }

      protected String maSP;
    protected String tenSP;
    protected double donGia;
    private CongThuc congThuc = new CongThuc();   // has-a: mỗi sản phẩm có 1 công thức
    protected SanPham(String maSP, String tenSP, double donGia) {
        setMaSP(maSP);
        setTenSP(tenSP);
        setDonGia(donGia);
        capNhatBoDem(this.maSP);
    }

    public abstract String getLoai();

    protected abstract String maLoai();

    public abstract String moTaThem();

    protected abstract String chuoiRieng();

    public abstract void hienThiThongTin();

    //Đọc/ghi file
    public final String toFileString() {
        String dong = maLoai() + "|" + maSP + "|" + tenSP + "|" + Math.round(donGia) + "|" + chuoiRieng();
        String ct = chuoiCongThucFile();
        return ct.isEmpty() ? dong : dong + "|" + ct;
    }

    // chuỗi công thức ghi file (Combo ghi đè thành rỗng vì công thức lấy từ món thành phần)
    protected String chuoiCongThucFile() {
        return congThuc.toFileString();
    }

    //Công thức & nguyên liệu
    public CongThuc getCongThuc() { return congThuc; }

    public void setCongThuc(CongThuc congThuc) {
        this.congThuc = congThuc == null ? new CongThuc() : congThuc;
    }

    // số phần dự kiến làm được theo tồn kho (CongThuc.KHONG_GIOI_HAN nếu chưa có công thức)
    public int soLuongCoTheTao(IQuanLy<NguyenLieu> kho) {
        return getCongThuc().soLuongToiDaTao(kho);
    }

    // các nguyên liệu đang bị khoá trong công thức; khác rỗng = món bị khoá, k cho order
    public java.util.List<NguyenLieu> nguyenLieuDangKhoa(IQuanLy<NguyenLieu> kho) {
        return getCongThuc().nguyenLieuBiKhoa(kho);
    }

    public boolean isBiKhoa(IQuanLy<NguyenLieu> kho) {
        return !nguyenLieuDangKhoa(kho).isEmpty();
    }

    public String getMaSP() { return maSP; }
    public String getTenSP() { return tenSP; }
    public double getDonGia() { return donGia; }

    public void setMaSP(String maSP) {
        String s = ChiTietHoaDon.lamSach(maSP);
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Mã sản phẩm không được rỗng");
        }
        this.maSP = s;
    }

    public void setTenSP(String tenSP) {
        String s = ChiTietHoaDon.lamSach(tenSP);
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được rỗng");
        }
        this.tenSP = s;
    }

    public void setDonGia(double donGia) {
        if (donGia < 0) {
            throw new IllegalArgumentException("Đơn giá không được âm");
        }
        this.donGia = donGia;
    }

    //Hiển thị
    @Override
    public String toString() {
        return String.format("%-6s %-10s %-28s %12s  %s", maSP, getLoai(), tenSP,
                IGiamGia.dinhDangTien(donGia), moTaThem());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SanPham && ((SanPham) o).maSP.equalsIgnoreCase(maSP);
    }

    @Override
    public int hashCode() {
        return maSP.toLowerCase().hashCode();
    }
}
