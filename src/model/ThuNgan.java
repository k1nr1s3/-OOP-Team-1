package model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;


// dùng set tl hh, phụ cấp trong file này hoặc kéo qua file cấu hình cũng đc --triển
public class ThuNgan extends NhanVien {

    public enum CaLam { SANG, CHIEU, TOI }
    public enum CapBac { HOC_VIEC, CHINH_THUC }

    public static final double TY_LE_HOA_HONG = 0.005;
    public static final double PHU_CAP_CA_TOI_MOI_NGAY = 20_000;

    // Lịch sử ca: ngày bắt đầu áp dụng -> ca. Mốc đầu tiên là ngày vào làm.
    private final TreeMap<LocalDate, CaLam> lichSuCa = new TreeMap<>();
    private CapBac capBac;
    private int soQuay;
    private double doanhThuKy;   // doanh thu quầy trong kỳ lương đang tính
    private final Map<YearMonth, Double> doanhThuTheoKy = new TreeMap<>();

    public ThuNgan(String maNV, String hoTen, LocalDate ngaySinh, String gioiTinh,
                   String soDienThoai, String diaChi, LocalDate ngayVaoLam, double luongCoBan,
                   CaLam caLam, int soQuay, double doanhThuKy) {
        this(maNV, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, ngayVaoLam,
                luongCoBan, CapBac.CHINH_THUC, caLam, soQuay, doanhThuKy);
    }

    public ThuNgan(String maNV, String hoTen, LocalDate ngaySinh, String gioiTinh,
                   String soDienThoai, String diaChi, LocalDate ngayVaoLam, double luongCoBan,
                   CapBac capBac, CaLam caLam, int soQuay, double doanhThuKy) {
        super(maNV, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, ngayVaoLam, luongCoBan);
        setCapBac(capBac);
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
        YearMonth ky = kiemTraKyLuong(thang, nam);
        double tien = doanhThuTheoKy.getOrDefault(ky, 0.0) * TY_LE_HOA_HONG;
        // phụ cấp ca tối tính theo ca đang áp dụng đúng vào từng ngày đi làm
        long soNgayToi = getBangChamCong().keySet().stream()
                .filter(ngay -> YearMonth.from(ngay).equals(ky) && caTai(ngay) == CaLam.TOI)
                .count();
        return tien + PHU_CAP_CA_TOI_MOI_NGAY * soNgayToi;
    }

    @Override
    protected double gioChuanLamThemTrongNgay() {
        return capBac == CapBac.HOC_VIEC ? 6.0 : GIO_CHUAN_MOI_NGAY;
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

    // Ca đang áp dụng hôm nay
    public CaLam getCaLam() { return caTai(LocalDate.now()); }

    // Ca áp dụng vào một ngày bất kỳ (trước mốc đầu tiên thì lấy ca đầu tiên)
    public CaLam caTai(LocalDate ngay) {
        Map.Entry<LocalDate, CaLam> e = lichSuCa.floorEntry(ngay);
        return e != null ? e.getValue() : lichSuCa.firstEntry().getValue();
    }

    public Map<LocalDate, CaLam> getLichSuCa() { return java.util.Collections.unmodifiableMap(lichSuCa); }

    // Đổi ca kể từ ngày tuNgay (không trước ngày vào làm, không trước lần đổi gần nhất).
    // Các ngày trước đó vẫn tính theo ca cũ. False nếu ca mới trùng ca đang áp dụng tại ngày đó.
    public boolean doiCaTu(CaLam caMoi, LocalDate tuNgay) {
        if (caMoi == null) throw new IllegalArgumentException("Ca làm việc không được để trống");
        if (tuNgay == null || tuNgay.isBefore(getNgayVaoLam())) {
            throw new IllegalArgumentException("Ngày đổi ca không được trước ngày vào làm (" + getNgayVaoLam().format(DINH_DANG_NGAY) + ")");
        }
        LocalDate lanDoiCuoi = lichSuCa.lastKey();
        if (tuNgay.isBefore(lanDoiCuoi)) {
            throw new IllegalArgumentException("Ngày đổi ca không được trước lần đổi gần nhất (" + lanDoiCuoi.format(DINH_DANG_NGAY) + ")");
        }
        if (lichSuCa.lastEntry().getValue() == caMoi) return false;
        lichSuCa.put(tuNgay, caMoi);
        return true;
    }
    public CapBac getCapBac() { return capBac; }
    public int getSoQuay() { return soQuay; }
    public double getDoanhThuKy() { return doanhThuKy; }

    // Gán ca ban đầu (lúc tạo). Về sau muốn đổi ca thì dùng doiCaTu để giữ lịch sử.
    public void setCaLam(CaLam caLam) {
        if (caLam == null) throw new IllegalArgumentException("Ca làm việc không được để trống");
        if (lichSuCa.isEmpty()) lichSuCa.put(getNgayVaoLam(), caLam);
        else doiCaTu(caLam, LocalDate.now());
    }

    public void setCapBac(CapBac capBac) {
        if (capBac == null) throw new IllegalArgumentException("Cấp bậc không được để trống");
        this.capBac = capBac;
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
        return String.join("|", capBac.name(), getCaLam().name(), String.valueOf(soQuay), String.valueOf(doanhThuKy));
    }

    // Lịch sử ca ghi ở cuối dòng: yyyy-MM-dd:CA,yyyy-MM-dd:CA
    @Override
    protected String chuoiCuoiDong() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<LocalDate, CaLam> e : lichSuCa.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(':').append(e.getValue().name());
        }
        return sb.toString();
    }

    private void napLichSuCa(String chuoi) {
        if (chuoi == null || chuoi.isBlank()) return;   // file cũ chưa có lịch sử: giữ ca ban đầu
        TreeMap<LocalDate, CaLam> moi = new TreeMap<>();
        for (String muc : chuoi.split(",")) {
            String[] kv = muc.trim().split(":", -1);
            if (kv.length != 2) throw new IllegalArgumentException("Bản ghi lịch sử ca không hợp lệ: " + muc);
            try {
                moi.put(LocalDate.parse(kv[0].trim()), CaLam.valueOf(kv[1].trim()));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Bản ghi lịch sử ca không hợp lệ: " + muc, e);
            }
        }
        lichSuCa.clear();
        lichSuCa.putAll(moi);
    }

    // giống quản lý  
    public static ThuNgan taoTuChuoi(String[] f) {
        kiemTraSoTruong(f, 12);
        ThuNgan tn;
        if (f.length >= 13 && ("HOC_VIEC".equals(f[9].trim()) || "CHINH_THUC".equals(f[9].trim()))) {
            tn = new ThuNgan(f[1], f[2], LocalDate.parse(f[3]), f[4], f[5], f[6],
                    LocalDate.parse(f[7]), Double.parseDouble(f[8]),
                    CapBac.valueOf(f[9].trim()), CaLam.valueOf(f[10].trim()),
                    Integer.parseInt(f[11].trim()), 0);
            tn.setDoanhThuKy(Double.parseDouble(f[12].trim()));
            tn.napChamCong(truongTai(f, 13));
            tn.napLichSuCa(truongTai(f, 14));
        } else {
            tn = new ThuNgan(f[1], f[2], LocalDate.parse(f[3]), f[4], f[5], f[6],
                    LocalDate.parse(f[7]), Double.parseDouble(f[8]),
                    CaLam.valueOf(f[9].trim()), Integer.parseInt(f[10].trim()), 0);
            tn.setDoanhThuKy(Double.parseDouble(f[11].trim()));
            tn.napChamCong(truongTai(f, 12));
        }
        tn.ganDoanhThuKyCu(tn.getKyChamCongCuoi());
        return tn;
    }

    private YearMonth getKyChamCongCuoi() {
        return getBangChamCong().keySet().stream().reduce((a, b) -> b)
                .map(YearMonth::from).orElse(null);
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Cấp bậc: %s | Ca: %s | Quầy: %d",
                capBac == CapBac.HOC_VIEC ? "Học việc" : "Chính thức", getCaLam(), soQuay);
    }
}
