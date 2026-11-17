package model;

import java.time.LocalDate;

// dùng set chức vụ, phụ cấp  với thưởng trong file này hoặc kéo qua file cấu hình cũng đc --triển
public class QuanLy extends NhanVien {

    private String chucVu;           // Quản lý cửa hàng, Quản lý ca...
    private double phuCapChucVu;     // VND / tháng
    private double tyLeThuongKPI;    // 0.0 -> 1.0 (0.1 = 10% lương)

    public QuanLy(String maNV, String hoTen, LocalDate ngaySinh, String gioiTinh,
                  String soDienThoai, String diaChi, LocalDate ngayVaoLam, double luongCoBan,
                  String chucVu, double phuCapChucVu, double tyLeThuongKPI) {
        super(maNV, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, ngayVaoLam, luongCoBan);
        setChucVu(chucVu);
        setPhuCapChucVu(phuCapChucVu);
        setTyLeThuongKPI(tyLeThuongKPI);
    }

    @Override
    public String getVaiTro() { return "Quản lý"; }

    @Override
    public double tinhThuongPhuCap(int thang, int nam) {
        return phuCapChucVu + luongCoBan * tyLeThuongKPI;
    }

    public String getChucVu() { return chucVu; }
    public double getPhuCapChucVu() { return phuCapChucVu; }
    public double getTyLeThuongKPI() { return tyLeThuongKPI; }

    public void setChucVu(String chucVu) {
        String s = lamSach(chucVu);
        if (s.isEmpty()) throw new IllegalArgumentException("Chức vụ không được để trống");
        this.chucVu = s;
    }

    public void setPhuCapChucVu(double phuCapChucVu) {
        if (!Double.isFinite(phuCapChucVu) || phuCapChucVu < 0) {
            throw new IllegalArgumentException("Phụ cấp phải là số hữu hạn, không âm");
        }
        this.phuCapChucVu = phuCapChucVu;
    }

    public void setTyLeThuongKPI(double tyLeThuongKPI) {
        if (!Double.isFinite(tyLeThuongKPI) || tyLeThuongKPI < 0 || tyLeThuongKPI > 1) {
            throw new IllegalArgumentException("Tỷ lệ thưởng KPI phải trong khoảng 0 - 1");
        }
        this.tyLeThuongKPI = tyLeThuongKPI;
    }

    @Override
    protected String maLoai() { return "QL"; }

    @Override
    protected String chuoiRieng() {
        return String.join("|", chucVu, String.valueOf(phuCapChucVu), String.valueOf(tyLeThuongKPI));
    }

    //giống cái pha chế trên nhưng mà do nhiều trường hơn nên đẩy xuống 12 
    public static QuanLy taoTuChuoi(String[] f) {
        kiemTraSoTruong(f, 12);
        QuanLy ql = new QuanLy(f[1], f[2], LocalDate.parse(f[3]), f[4], f[5], f[6],
                LocalDate.parse(f[7]), Double.parseDouble(f[8]),
                f[9], Double.parseDouble(f[10].trim()), Double.parseDouble(f[11].trim()));
        ql.napChamCong(truongTai(f, 12));
        return ql;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Chức vụ: %s", chucVu);
    }
}
