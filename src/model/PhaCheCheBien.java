package model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;

public class PhaCheCheBien extends NhanVien {
// hệ số sau thay xong chỉnh qua cấu hình cũng được
    public enum CapBac {
        HOC_VIEC(0.0, "Học việc"),
        CHINH_THUC(0.05, "Chính thức");

        private final double heSoPhuCap;
        private final String ten;

        CapBac(double heSoPhuCap, String ten) {
            this.heSoPhuCap = heSoPhuCap;
            this.ten = ten;
        }

        public double getHeSoPhuCap() { return heSoPhuCap; }
        public String getTen() { return ten; }
    }
    private CapBac capBac;
    private int soLyPhaChe;   // số ly đã pha trong kỳ lương đang tính
    private final Map<YearMonth, Integer> soLyTheoKy = new TreeMap<>();
    private int soMonCheBien; // số món đã chế biến trong kỳ lương đang tính
    private final Map<YearMonth, Integer> soMonTheoKy = new TreeMap<>();

    public PhaCheCheBien(String maNV, String hoTen, LocalDate ngaySinh, String gioiTinh,
                  String soDienThoai, String diaChi, LocalDate ngayVaoLam, double luongCoBan,
                  CapBac capBac, int soLyPhaChe) {
        this(maNV, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, ngayVaoLam,
                luongCoBan, capBac, soLyPhaChe, 0);
    }

    public PhaCheCheBien(String maNV, String hoTen, LocalDate ngaySinh, String gioiTinh,
                  String soDienThoai, String diaChi, LocalDate ngayVaoLam, double luongCoBan,
                  CapBac capBac, int soLyPhaChe, int soMonCheBien) {
        super(maNV, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, ngayVaoLam, luongCoBan);
        setCapBac(capBac);
        setSoLyPhaChe(soLyPhaChe);
        setSoMonCheBien(soMonCheBien);
        if (soLyPhaChe > 0) {
            soLyTheoKy.put(YearMonth.now(), soLyPhaChe);
        }
        if (soMonCheBien > 0) {
            soMonTheoKy.put(YearMonth.now(), soMonCheBien);
        }
    }

    @Override
    public String getVaiTro() { return "Nhân viên bếp"; }

    @Override
    public double tinhThuongPhuCap(int thang, int nam) {
        return luongCoBan * capBac.getHeSoPhuCap();
    }

    @Override
    protected double gioChuanLamThemTrongNgay() {
        return capBac == CapBac.HOC_VIEC ? 6.0 : GIO_CHUAN_MOI_NGAY;
    }

    public void themSoLy(int soLy) {
        themSoLy(YearMonth.now(), soLy);
    }

    public void themSoLy(YearMonth ky, int soLy) {
        if (ky == null || soLy <= 0) return;
        soLyTheoKy.merge(ky, soLy, Integer::sum);
        soLyPhaChe = soLyTheoKy.get(ky);
    }

    public void themSoMon(int soMon) {
        themSoMon(YearMonth.now(), soMon);
    }

    public void themSoMon(YearMonth ky, int soMon) {
        if (ky == null || soMon <= 0) return;
        soMonTheoKy.merge(ky, soMon, Integer::sum);
        soMonCheBien = soMonTheoKy.get(ky);
    }

    public CapBac getCapBac() { return capBac; }
    public int getSoLyPhaChe() { return soLyPhaChe; }
    public int getSoMonCheBien() { return soMonCheBien; }

    public void setCapBac(CapBac capBac) {
        if (capBac == null) throw new IllegalArgumentException("Cấp bậc không được để trống");
        this.capBac = capBac;
    }

    public void setSoLyPhaChe(int soLyPhaChe) {
        if (soLyPhaChe < 0) throw new IllegalArgumentException("Số ly không được âm");
        this.soLyPhaChe = soLyPhaChe;
    }

    public void setSoMonCheBien(int soMonCheBien) {
        if (soMonCheBien < 0) throw new IllegalArgumentException("Số món không được âm");
        this.soMonCheBien = soMonCheBien;
    }

    @Override
    protected String maLoai() { return "NB"; }

    @Override
    protected String chuoiRieng() {
        return String.join("|", capBac.name(), String.valueOf(soLyPhaChe), String.valueOf(soMonCheBien));
    }

    // do mấy cái chức vụ có mấy cái trường riêng nên phải parse riêng, trường riêng ở vị trí 9..10, chấm công ở 11
    public static PhaCheCheBien taoTuChuoi(String[] f) {
        kiemTraSoTruong(f, 11);
        PhaCheCheBien pc = new PhaCheCheBien(f[1], f[2], LocalDate.parse(f[3]), f[4], f[5], f[6],
                LocalDate.parse(f[7]), Double.parseDouble(f[8]),
                CapBac.valueOf(f[9].trim()), 0, 0);
        pc.setSoLyPhaChe(Integer.parseInt(f[10].trim()));
        if (f.length >= 13) {
            pc.setSoMonCheBien(Integer.parseInt(f[11].trim()));
            pc.napChamCong(truongTai(f, 12));
        } else {
            pc.napChamCong(truongTai(f, 11));
        }
        pc.ganSoLyKyCu(pc.getKyChamCongCuoi());
        pc.ganSoMonKyCu(pc.getKyChamCongCuoi());
        return pc;
    }

    private void ganSoLyKyCu(YearMonth ky) {
        if (ky != null && soLyPhaChe > 0) soLyTheoKy.put(ky, soLyPhaChe);
    }

    private void ganSoMonKyCu(YearMonth ky) {
        if (ky != null && soMonCheBien > 0) soMonTheoKy.put(ky, soMonCheBien);
    }

    private YearMonth getKyChamCongCuoi() {
        return getBangChamCong().keySet().stream().reduce((a, b) -> b)
                .map(YearMonth::from).orElse(null);
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Cấp bậc: %s | Số ly: %d | Số món: %d",
                capBac.getTen(), soLyPhaChe, soMonCheBien);
    }
}
