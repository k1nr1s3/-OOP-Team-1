package model;

import iface.IGiamGia;

// khách thường (chưa phải thành viên thân thiết), tích điểm hệ số 1.0, k được giảm giá theo hạng
// là lớp cơ sở của KhachHangThanThiet --trung
public class KhachHang implements IGiamGia {

    // ===================== Static =====================
    public static final int TIEN_MOI_DIEM = 10000;   // 10.000 VND = 1 điểm
    public static final int GIA_TRI_DIEM = 1000;     // đổi điểm: 1 điểm = 1.000 VND
    private static int demKH = 0;                    // đếm để tự sinh mã KH001, KH002...

    // ===================== Thuộc tính =====================
    private String maKH;
    private String hoTen;
    private String soDienThoai;
    protected int diemTichLuy;        // điểm đang có, đem đi đổi được
    protected double tongChiTieu;     // tổng tiền đã mua từ trước tới giờ

    // ===================== Constructor =====================
    // tạo khách mới, mã tự sinh (KH001, KH002...)
    public KhachHang(String hoTen, String soDienThoai) {
        this(taoMaTuDong(), hoTen, soDienThoai);
    }

    public KhachHang(String maKH, String hoTen, String soDienThoai) {
        this(maKH, hoTen, soDienThoai, 0, 0);
    }

    // constructor đầy đủ, dùng khi đọc từ file
    public KhachHang(String maKH, String hoTen, String soDienThoai, int diemTichLuy, double tongChiTieu) {
        setMaKH(maKH);
        setHoTen(hoTen);
        setSoDienThoai(soDienThoai);
        this.diemTichLuy = Math.max(diemTichLuy, 0);
        this.tongChiTieu = Math.max(tongChiTieu, 0);
        capNhatBoDem(this.maKH);
    }

    // ===================== Phương thức static =====================
    public static String taoMaTuDong() {
        return String.format("KH%03d", ++demKH);
    }

    // cập nhật bộ đếm để mã tự sinh sau này k trùng với mã đọc từ file
    public static void capNhatBoDem(String ma) {
        try {
            int so = Integer.parseInt(ma.substring(2));
            if (so > demKH) {
                demKH = so;
            }
        } catch (RuntimeException e) {
            // mã k đúng dạng KHxxx thì bỏ qua
        }
    }

    public static boolean hopLeSDT(String sdt) {
        return sdt != null && sdt.matches("0\\d{9}");
    }

    // ===================== Nghiệp vụ =====================
    // hệ số nhân điểm, lớp con ghi đè lại (đa hình)
    protected double heSoTichDiem() {
        return 1.0;
    }

    // mua hàng: cộng vào tổng chi tiêu rồi tích điểm
    // soTien = số tiền khách thực trả, trả về số điểm được cộng thêm
    public int tichDiem(double soTien) {
        if (soTien <= 0) {
            return 0;
        }
        tongChiTieu += soTien;
        int diem = (int) (soTien / TIEN_MOI_DIEM * heSoTichDiem());
        diemTichLuy += diem;
        return diem;
    }

    // đổi điểm lấy tiền giảm, trả về số tiền được giảm (k đủ điểm thì ném lỗi)
    public double doiDiem(int soDiem) {
        if (soDiem <= 0 || soDiem > diemTichLuy) {
            throw new IllegalArgumentException("Số điểm đổi không hợp lệ (đang có " + diemTichLuy + " điểm)");
        }
        diemTichLuy -= soDiem;
        return (double) soDiem * GIA_TRI_DIEM;
    }

    // ===================== IGiamGia =====================
    @Override
    public double tinhGiamGia(double tongTien) {
        return 0;
    }

    @Override
    public String getMoTa() {
        return "Khách thường - không giảm giá";
    }

    // ===================== Thông tin loại khách (lớp con ghi đè) =====================
    public String getLoai() {
        return "Thường";
    }

    protected String maLoai() {
        return "THUONG";
    }

    // ===================== Đọc - ghi file =====================
    public String toFileString() {
        return maLoai() + "|" + maKH + "|" + hoTen + "|" + soDienThoai + "|"
                + diemTichLuy + "|" + String.format("%.0f", tongChiTieu);
    }

    // dòng file -> đúng loại khách (xem trường đầu: THUONG / THANTHIET)
    public static KhachHang fromFileString(String dong) {
        String[] p = dong.split("\\|");
        if (p.length < 6) {
            throw new IllegalArgumentException("Thiếu dữ liệu: " + dong);
        }
        int diem = Integer.parseInt(p[4].trim());
        double chiTieu = Double.parseDouble(p[5].trim());
        if (p[0].trim().equals("THANTHIET")) {
            return new KhachHangThanThiet(p[1].trim(), p[2].trim(), p[3].trim(), diem, chiTieu);
        }
        return new KhachHang(p[1].trim(), p[2].trim(), p[3].trim(), diem, chiTieu);
    }

    // ===================== Get / Set =====================
    public String getMaKH() { return maKH; }
    public String getHoTen() { return hoTen; }
    public String getSoDienThoai() { return soDienThoai; }
    public int getDiemTichLuy() { return diemTichLuy; }
    public double getTongChiTieu() { return tongChiTieu; }

    public void setMaKH(String maKH) {
        if (maKH == null || maKH.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã khách hàng không được rỗng");
        }
        this.maKH = maKH.trim();
    }

    public void setHoTen(String hoTen) {
        if (hoTen == null || hoTen.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ tên không được rỗng");
        }
        this.hoTen = hoTen.trim();
    }

    public void setSoDienThoai(String soDienThoai) {
        if (!hopLeSDT(soDienThoai)) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0");
        }
        this.soDienThoai = soDienThoai;
    }

    // ===================== Hiển thị =====================
    public static String tieuDeBang() {
        return String.format("%-7s %-22s %-12s %-18s %8s %16s",
                "Mã", "Họ tên", "SĐT", "Loại", "Điểm", "Tổng chi tiêu");
    }

    @Override
    public String toString() {
        return String.format("%-7s %-22s %-12s %-18s %8d %16s",
                maKH, hoTen, soDienThoai, getLoai(), diemTichLuy, IGiamGia.dinhDangTien(tongChiTieu));
    }
}
