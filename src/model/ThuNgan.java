package model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;


// dùng set tl hh, phụ cấp trong file này hoặc kéo qua file cấu hình cũng đc --triển
public class ThuNgan extends NhanVien {

    public enum CaLam { SANG, CHIEU, TOI }

    public static final double TY_LE_HOA_HONG = 0.005;
    public static final double PHU_CAP_CA_TOI_MOI_NGAY = 20_000;

    private CaLam caLam;
    private int soQuay;
    private double doanhThuKy;   // doanh thu quầy trong kỳ lương đang tính
    private final Map<YearMonth, Double> doanhThuTheoKy = new TreeMap<>();

    public ThuNgan(String maNV, String hoTen, LocalDate ngaySinh, String gioiTinh,
                   String soDienThoai, String diaChi, LocalDate ngayVaoLam, double luongCoBan,
                   CaLam caLam, int soQuay, double doanhThuKy) {
        super(maNV, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, ngayVaoLam, luongCoBan);
        setCaLam(caLam);
        setSoQuay(soQuay);
        setDoanhThuKy(doanhThuKy);
        if (doanhThuKy > 0) {
            doanhThuTheoKy.put(YearMonth.now(), doanhThuKy);
        }
    }

    @Override
    public String getVaiTro() { return "Thu ngân"; }

    @Override
    public double tinhThuongPhuCap(int thang, int nam) {
        double tien = doanhThuTheoKy.getOrDefault(YearMonth.of(nam, thang), 0.0) * TY_LE_HOA_HONG;
        if (caLam == CaLam.TOI) {
            tien += PHU_CAP_CA_TOI_MOI_NGAY * tinhSoNgayDiLam(thang, nam);
        }
        return tien;
    }

    // Cộng dồn doanh thu từ một hóa đơn (gọi từ DanhSachHoaDon sau này)
    public void themDoanhThu(double soTien) {
        themDoanhThu(YearMonth.now(), soTien);
    }

    public void themDoanhThu(YearMonth ky, double soTien) {
        if (ky == null || !Double.isFinite(soTien) || soTien <= 0) return;
        doanhThuTheoKy.merge(ky, soTien, Double::sum);
        doanhThuKy = doanhThuTheoKy.get(ky);
    }

    public void ganDoanhThuKyCu(YearMonth ky) {
        if (ky != null && doanhThuKy > 0) doanhThuTheoKy.put(ky, doanhThuKy);
    }

    public CaLam getCaLam() { return caLam; }
    public int getSoQuay() { return soQuay; }
    public double getDoanhThuKy() { return doanhThuKy; }

    public void setCaLam(CaLam caLam) {
        if (caLam == null) throw new IllegalArgumentException("Ca làm việc không được để trống");
        this.caLam = caLam;
    }

    public void setSoQuay(int soQuay) {
        if (soQuay < 1) throw new IllegalArgumentException("Số quầy phải >= 1");
        this.soQuay = soQuay;
    }

    public void setDoanhThuKy(double doanhThuKy) {
        if (!Double.isFinite(doanhThuKy) || doanhThuKy < 0) {
            throw new IllegalArgumentException("Doanh thu phải là số hữu hạn, không âm");
        }
        this.doanhThuKy = doanhThuKy;
    }

    @Override
    protected String maLoai() { return "TN"; }

    @Override
    protected String chuoiRieng() {
        return String.join("|", caLam.name(), String.valueOf(soQuay), String.valueOf(doanhThuKy));
    }

    // giống quản lý  
    public static ThuNgan taoTuChuoi(String[] f) {
        kiemTraSoTruong(f, 12);
        ThuNgan tn = new ThuNgan(f[1], f[2], LocalDate.parse(f[3]), f[4], f[5], f[6],
                LocalDate.parse(f[7]), Double.parseDouble(f[8]),
                CaLam.valueOf(f[9].trim()), Integer.parseInt(f[10].trim()), 0);
        tn.setDoanhThuKy(Double.parseDouble(f[11].trim()));
        tn.napChamCong(truongTai(f, 12));
        tn.ganDoanhThuKyCu(tn.getKyChamCongCuoi());
        return tn;
    }

    private YearMonth getKyChamCongCuoi() {
        return getBangChamCong().keySet().stream().reduce((a, b) -> b)
                .map(YearMonth::from).orElse(null);
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Ca: %s | Quầy: %d", caLam, soQuay);
    }
}
