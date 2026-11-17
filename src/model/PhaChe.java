package model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;

public class PhaChe extends NhanVien {
// hệ số sau thay xong chỉnh qua cấu hình cũng được
    public enum CapBac {
        HOC_VIEC(0.0, "Học việc"),
        CHINH_THUC(0.05, "Chính thức"),
        TRUONG_BAR(0.12, "Trưởng bar");

        private final double heSoPhuCap;
        private final String ten;

        CapBac(double heSoPhuCap, String ten) {
            this.heSoPhuCap = heSoPhuCap;
            this.ten = ten;
        }

        public double getHeSoPhuCap() { return heSoPhuCap; }
        public String getTen() { return ten; }
    }
    // ví dụ thưởng ( sau này chỉnh bằng cấu hình)
    public static final double THUONG_MOI_LY = 500;

    private CapBac capBac;
    private int soLyPhaChe;   // số ly đã pha trong kỳ lương đang tính
    private final Map<YearMonth, Integer> soLyTheoKy = new TreeMap<>();

    public PhaChe(String maNV, String hoTen, LocalDate ngaySinh, String gioiTinh,
                  String soDienThoai, String diaChi, LocalDate ngayVaoLam, double luongCoBan,
                  CapBac capBac, int soLyPhaChe) {
        super(maNV, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, ngayVaoLam, luongCoBan);
        setCapBac(capBac);
        setSoLyPhaChe(soLyPhaChe);
        if (soLyPhaChe > 0) {
            soLyTheoKy.put(YearMonth.now(), soLyPhaChe);
        }
    }

    @Override
    public String getVaiTro() { return "Pha chế"; }

    @Override
    public double tinhThuongPhuCap(int thang, int nam) {
        return luongCoBan * capBac.getHeSoPhuCap()
                + soLyTheoKy.getOrDefault(YearMonth.of(nam, thang), 0) * THUONG_MOI_LY;
    }

    public void themSoLy(int soLy) {
        themSoLy(YearMonth.now(), soLy);
    }

    public void themSoLy(YearMonth ky, int soLy) {
        if (ky == null || soLy <= 0) return;
        soLyTheoKy.merge(ky, soLy, Integer::sum);
        soLyPhaChe = soLyTheoKy.get(ky);
    }

    public CapBac getCapBac() { return capBac; }
    public int getSoLyPhaChe() { return soLyPhaChe; }

    public void setCapBac(CapBac capBac) {
        if (capBac == null) throw new IllegalArgumentException("Cấp bậc không được để trống");
        this.capBac = capBac;
    }

    public void setSoLyPhaChe(int soLyPhaChe) {
        if (soLyPhaChe < 0) throw new IllegalArgumentException("Số ly không được âm");
        this.soLyPhaChe = soLyPhaChe;
    }

    @Override
    protected String maLoai() { return "PC"; }

    @Override
    protected String chuoiRieng() {
        return String.join("|", capBac.name(), String.valueOf(soLyPhaChe));
    }

    // do mấy cái chức vụ có mấy cái trường riêng nên phải parse riêng, trường riêng ở vị trí 9..10, chấm công ở 11
    public static PhaChe taoTuChuoi(String[] f) {
        kiemTraSoTruong(f, 11);
        PhaChe pc = new PhaChe(f[1], f[2], LocalDate.parse(f[3]), f[4], f[5], f[6],
                LocalDate.parse(f[7]), Double.parseDouble(f[8]),
                CapBac.valueOf(f[9].trim()), 0);
        pc.setSoLyPhaChe(Integer.parseInt(f[10].trim()));
        pc.napChamCong(truongTai(f, 11));
        pc.ganSoLyKyCu(pc.getKyChamCongCuoi());
        return pc;
    }

    private void ganSoLyKyCu(YearMonth ky) {
        if (ky != null && soLyPhaChe > 0) soLyTheoKy.put(ky, soLyPhaChe);
    }

    private YearMonth getKyChamCongCuoi() {
        return getBangChamCong().keySet().stream().reduce((a, b) -> b)
                .map(YearMonth::from).orElse(null);
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Cấp bậc: %s | Số ly: %d", capBac.getTen(), soLyPhaChe);
    }
}
