package model;

import iface.IGiamGia;


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
        switch (f[0].trim().toUpperCase()) {
            case "DOAN":     return new DoAn(ma, ten, gia, Boolean.parseBoolean(rieng));
            case "THUCUONG": return new ThucUong(ma, ten, gia, rieng.isEmpty() ? "M" : rieng);
            case "COMBO":    return new Combo(ma, ten, gia, rieng);
            default: throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + f[0]);
        }
    }

    public static String tieuDeBang() {
        return String.format("%-6s %-10s %-28s %12s  %s", "Mã", "Loại", "Tên", "Đơn giá", "Chi tiết");
    }

      protected String maSP;
    protected String tenSP;
    protected double donGia;
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
        return maLoai() + "|" + maSP + "|" + tenSP + "|" + Math.round(donGia) + "|" + chuoiRieng();
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
