package model;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


public abstract class NhanVien {

    // hằng số ví dụ để sau này thay bằng file cấu hình //
    public static final int NGAY_CONG_CHUAN = 26;
    public static final double GIO_CHUAN_MOI_NGAY = 8.0;
    public static final double HE_SO_LAM_THEM = 1.5;
    public static final double TY_LE_BAO_HIEM = 0.105;
    public static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    protected String maNV;
    protected String hoTen;
    protected LocalDate ngaySinh;
    protected String gioiTinh;     
    protected String soDienThoai;
    protected String diaChi;
    protected LocalDate ngayVaoLam;
    protected double luongCoBan; 

    // Bảng chấm công: ngày -> số giờ làm thực tế
    private final Map<LocalDate, Double> bangChamCong = new TreeMap<>();
    // Giờ vào / ra của những ngày chấm bằng check-in / check-out: ngày -> {giờ vào, giờ ra}
    private final Map<LocalDate, LocalTime[]> gioVaoRa = new TreeMap<>();
    // Ca đang làm: đã check-in nhưng chưa check-out (ngày -> giờ vào). Tối đa 1 ca mở.
    private final Map<LocalDate, LocalTime> caDangMo = new TreeMap<>();

    private static final DateTimeFormatter DINH_DANG_GIO_FILE = DateTimeFormatter.ofPattern("HHmmss");
    public static final DateTimeFormatter DINH_DANG_GIO = DateTimeFormatter.ofPattern("HH:mm:ss");

    protected NhanVien(String maNV, String hoTen, LocalDate ngaySinh, String gioiTinh,
                       String soDienThoai, String diaChi, LocalDate ngayVaoLam, double luongCoBan) {
        setMaNV(maNV);
        setHoTen(hoTen);
        setNgaySinh(ngaySinh);
        setGioiTinh(gioiTinh);
        setSoDienThoai(soDienThoai);
        setDiaChi(diaChi);
        setNgayVaoLam(ngayVaoLam);
        setLuongCoBan(luongCoBan);
    }


    // Tên vai trò hiển thị, ví dụ "Thu ngân"
    public abstract String getVaiTro();

    // thưởng + phụ cấp thêm nếu có  
    public abstract double tinhThuongPhuCap(int thang, int nam);

    // Mã loại dùng khi ghi file: TN, NB (cũ PC), QL
    protected abstract String maLoai();

    // Các trường riêng của lớp con
    protected abstract String chuoiRieng();

    // Chấm công

    // Chấm công theo số giờ làm. Trả về false nếu dữ liệu không hợp lệ hoặc ngày đó đã chấm.
    public boolean chamCong(LocalDate ngay, double soGio) {
        if (!hopLeChamCong(ngay, soGio) || bangChamCong.containsKey(ngay)) return false;
        bangChamCong.put(ngay, soGio);
        return true;
    }

    // Chấm công theo giờ vào - giờ ra (số giờ = ra - vào).
    public boolean chamCong(LocalDate ngay, LocalTime gioVao, LocalTime gioRa) {
        if (gioVao == null || gioRa == null || !gioRa.isAfter(gioVao)) return false;
        double soGio = Duration.between(gioVao, gioRa).toMinutes() / 60.0;
        if (!chamCong(ngay, soGio)) return false;
        gioVaoRa.put(ngay, new LocalTime[]{gioVao, gioRa});
        return true;
    }

    // Check-in: ghi nhận giờ vào ca. Lỗi (đã check-in, hôm nay đã chấm đủ...) thì ném IllegalStateException / IllegalArgumentException.
    public void checkIn(LocalDateTime luc) {
        if (luc == null) throw new IllegalArgumentException("Thời điểm check-in không hợp lệ");
        LocalDate ngay = luc.toLocalDate();
        if (!caDangMo.isEmpty()) {
            Map.Entry<LocalDate, LocalTime> mo = caDangMo.entrySet().iterator().next();
            throw new IllegalStateException("Bạn đã check-in lúc " + mo.getValue().format(DINH_DANG_GIO)
                    + " ngày " + mo.getKey().format(DINH_DANG_NGAY) + " mà chưa check-out");
        }
        if (ngay.isBefore(ngayVaoLam)) {
            throw new IllegalArgumentException("Ngày chấm công k được trước ngày vào làm");
        }
        if (bangChamCong.containsKey(ngay)) {
            throw new IllegalStateException("Ngày " + ngay.format(DINH_DANG_NGAY) + " đã chấm công ("
                    + String.format("%.2f", bangChamCong.get(ngay)) + " giờ)");
        }
        caDangMo.put(ngay, luc.toLocalTime().withNano(0));
    }

    // Check-out: ghi nhận giờ ra, tự tính số giờ làm = ra - vào và lưu vào bảng chấm công. Trả về số giờ làm.
    public double checkOut(LocalDateTime luc) {
        if (caDangMo.isEmpty()) {
            throw new IllegalStateException("Bạn chưa check-in nên k check-out được");
        }
        Map.Entry<LocalDate, LocalTime> mo = caDangMo.entrySet().iterator().next();
        LocalDateTime vao = LocalDateTime.of(mo.getKey(), mo.getValue());
        if (luc == null || !luc.isAfter(vao)) {
            throw new IllegalStateException("Giờ ra phải sau giờ vào (" + mo.getValue().format(DINH_DANG_GIO) + ")");
        }
        double soGio = Math.round(Duration.between(vao, luc).getSeconds() / 3600.0 * 10000) / 10000.0;
        if (soGio > 24) {
            throw new IllegalStateException("Quá 24 giờ kể từ lúc check-in, hãy nhờ quản lý sửa / xoá chấm công");
        }
        bangChamCong.put(mo.getKey(), Math.max(soGio, 0.0001));
        gioVaoRa.put(mo.getKey(), new LocalTime[]{mo.getValue(), luc.toLocalTime().withNano(0)});
        caDangMo.remove(mo.getKey());
        return bangChamCong.get(mo.getKey());
    }

    // thời điểm check-in của ca đang mở (null nếu k có)
    public LocalDateTime getCheckInDangMo() {
        if (caDangMo.isEmpty()) return null;
        Map.Entry<LocalDate, LocalTime> mo = caDangMo.entrySet().iterator().next();
        return LocalDateTime.of(mo.getKey(), mo.getValue());
    }

    // giờ vào / giờ ra của 1 ngày (null nếu ngày đó k chấm bằng check-in / check-out)
    public LocalTime getGioVao(LocalDate ngay) {
        LocalTime[] v = gioVaoRa.get(ngay);
        return v == null ? null : v[0];
    }

    public LocalTime getGioRa(LocalDate ngay) {
        LocalTime[] v = gioVaoRa.get(ngay);
        return v == null ? null : v[1];
    }

    // sửa bản chấm công ( false nếu chưa có hoặc k hợp lệ)
    public boolean suaChamCong(LocalDate ngay, double soGio) {
        if (!hopLeChamCong(ngay, soGio) || !bangChamCong.containsKey(ngay)) return false;
        bangChamCong.put(ngay, soGio);
        gioVaoRa.remove(ngay);   // số giờ đã sửa tay thì giờ vào / ra cũ k còn đúng
        return true;
    }

    // xoá bản chấm công của ngày đó (kể cả ca đang mở chưa check-out)
    public boolean xoaChamCong(LocalDate ngay) {
        gioVaoRa.remove(ngay);
        boolean daCoCaMo = caDangMo.remove(ngay) != null;
        return bangChamCong.remove(ngay) != null || daCoCaMo;
    }

    private boolean hopLeChamCong(LocalDate ngay, double soGio) {
        return ngay != null
                && !ngay.isBefore(ngayVaoLam)
                && !ngay.isAfter(LocalDate.now())
                && soGio > 0 && soGio <= 24;
    }

    //Số ngày có đi làm trong tháng (mỗi bản ghi tính 1 ngày)
    public int tinhSoNgayDiLam(int thang, int nam) {
        YearMonth ym = kiemTraKyLuong(thang, nam);
        int dem = 0;
        for (LocalDate ngay : bangChamCong.keySet()) {
            if (YearMonth.from(ngay).equals(ym)) dem++;
        }
        return dem;
    }

    // Số ngày công quy đổi (làm 4h = 0,5 công; từ 8h trở lên = 1 công).
    public double tinhSoNgayCong(int thang, int nam) {
        YearMonth ym = kiemTraKyLuong(thang, nam);
        double tong = 0;
        for (Map.Entry<LocalDate, Double> e : bangChamCong.entrySet()) {
            if (YearMonth.from(e.getKey()).equals(ym)) {
                tong += Math.min(e.getValue(), GIO_CHUAN_MOI_NGAY) / GIO_CHUAN_MOI_NGAY;
            }
        }
        return tong;
    }

    // giờ làm thêm ( dư ra hơn 8h = làm thêm )
    public double tinhGioLamThem(int thang, int nam) {
        YearMonth ym = kiemTraKyLuong(thang, nam);
        double tong = 0;
        for (Map.Entry<LocalDate, Double> e : bangChamCong.entrySet()) {
            if (YearMonth.from(e.getKey()).equals(ym)) {
                tong += Math.max(0, e.getValue() - gioChuanLamThemTrongNgay());
            }
        }
        return tong;
    }

    protected double gioChuanLamThemTrongNgay() {
        return GIO_CHUAN_MOI_NGAY;
    }

    protected double getHeSoLamThem() {
        return HE_SO_LAM_THEM;
    }

    public Map<LocalDate, Double> getBangChamCong() {
        return Collections.unmodifiableMap(bangChamCong);
    }

    // Tính lương 

    public double tinhLuongTheoCong(int thang, int nam) {
        return luongCoBan / NGAY_CONG_CHUAN * tinhSoNgayCong(thang, nam);
    }

    public double tinhTienLamThem(int thang, int nam) {
        double luongMotGio = luongCoBan / NGAY_CONG_CHUAN / GIO_CHUAN_MOI_NGAY;
        return luongMotGio * getHeSoLamThem() * tinhGioLamThem(thang, nam);
    }

    public double tinhBaoHiem() {
        return luongCoBan * TY_LE_BAO_HIEM;
    }

    // lương 1 tháng
    public final double tinhLuong(int thang, int nam) {
        if (tinhSoNgayDiLam(thang, nam) == 0) return 0;
        double luong = tinhLuongTheoCong(thang, nam)
                + tinhTienLamThem(thang, nam)
                + tinhThuongPhuCap(thang, nam)
                - tinhBaoHiem();
        return Math.max(0, luong);
    }

    // này hàm để in ra phiếu lương //
    public String phieuLuong(int thang, int nam) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("===== PHIẾU LƯƠNG THÁNG %02d/%d =====%n", thang, nam));
        sb.append(String.format("Mã NV            : %s%n", maNV));
        sb.append(String.format("Họ tên           : %s (%s)%n", hoTen, getVaiTro()));
        sb.append(String.format("Lương cơ bản     : %s%n", dinhDangTien(luongCoBan)));
        sb.append(String.format("Số ngày đi làm   : %d%n", tinhSoNgayDiLam(thang, nam)));
        sb.append(String.format("Số ngày công     : %.2f / %d%n", tinhSoNgayCong(thang, nam), NGAY_CONG_CHUAN));
        sb.append(String.format("Giờ làm thêm     : %.2f%n", tinhGioLamThem(thang, nam)));
        sb.append(String.format("Lương theo công  : %s%n", dinhDangTien(tinhLuongTheoCong(thang, nam))));
        sb.append(String.format("Tiền làm thêm    : %s%n", dinhDangTien(tinhTienLamThem(thang, nam))));
        sb.append(String.format("Thưởng/Phụ cấp   : %s%n", dinhDangTien(tinhThuongPhuCap(thang, nam))));
        sb.append(String.format("Bảo hiểm (-)     : %s%n", dinhDangTien(tinhBaoHiem())));
        sb.append(String.format("THỰC LĨNH        : %s%n", dinhDangTien(tinhLuong(thang, nam))));
        return sb.toString();
    }

    //** đọc - ghi file 

    // định dạng 1 dòng dữ liệu để lưu 
    public final String toFileString() {
        String dong = String.join("|",
                maLoai(), maNV, hoTen, ngaySinh.toString(), gioiTinh, soDienThoai, diaChi,
                ngayVaoLam.toString(), String.valueOf(luongCoBan),
                chuoiRieng(), chuoiChamCong());
        String them = chuoiCuoiDong();
        return them == null ? dong : dong + "|" + them;
    }

    // Trường ghi thêm sau chấm công (lớp con cần thì ghi đè, mặc định không có)
    protected String chuoiCuoiDong() { return null; }

    // định dạng chầm công theo chuỗi để lưu file 
    protected final String chuoiChamCong() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<LocalDate, Double> e : bangChamCong.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(':').append(java.math.BigDecimal.valueOf(e.getValue()).toPlainString());
            LocalTime[] v = gioVaoRa.get(e.getKey());
            if (v != null) {   // ngày:soGio@HHmmss-HHmmss
                sb.append('@').append(v[0].format(DINH_DANG_GIO_FILE)).append('-').append(v[1].format(DINH_DANG_GIO_FILE));
            }
        }
        for (Map.Entry<LocalDate, LocalTime> e : caDangMo.entrySet()) {   // ca chưa check-out: ngày:0.0@HHmmss-
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(":0.0@").append(e.getValue().format(DINH_DANG_GIO_FILE)).append('-');
        }
        return sb.toString();
    }

    /** Nạp bảng chấm công từ chuỗi đọc file (không kiểm tra lại ngày hiện tại). */
    protected final void napChamCong(String chuoi) {
        bangChamCong.clear();
        gioVaoRa.clear();
        caDangMo.clear();
        if (chuoi == null || chuoi.isBlank()) return;
        for (String muc : chuoi.split(",")) {
            String[] phan = muc.trim().split("@", -1);   // phần sau @ (nếu có) là giờ vào-ra
            String[] kv = phan[0].split(":", -1);
            if (kv.length != 2 || phan.length > 2) {
                throw new IllegalArgumentException("Bản ghi chấm công không hợp lệ: " + muc);
            }
            LocalDate ngay;
            double soGio;
            LocalTime vao = null;
            LocalTime ra = null;
            try {
                ngay = LocalDate.parse(kv[0].trim());
                soGio = Double.parseDouble(kv[1].trim());
                if (phan.length == 2) {
                    String[] vr = phan[1].split("-", -1);
                    if (vr.length != 2) throw new IllegalArgumentException("giờ vào-ra sai");
                    vao = LocalTime.parse(vr[0].trim(), DINH_DANG_GIO_FILE);
                    ra = vr[1].trim().isEmpty() ? null : LocalTime.parse(vr[1].trim(), DINH_DANG_GIO_FILE);
                }
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Bản ghi chấm công không hợp lệ: " + muc, e);
            }
            if (vao != null && ra == null) {   // ca đang mở (chưa check-out)
                if (!caDangMo.isEmpty() || bangChamCong.containsKey(ngay) || ngay.isBefore(ngayVaoLam)) {
                    throw new IllegalArgumentException("Ca đang mở không hợp lệ: " + muc);
                }
                caDangMo.put(ngay, vao);
                continue;
            }
            if (!hopLeChamCong(ngay, soGio) || bangChamCong.containsKey(ngay) || caDangMo.containsKey(ngay)) {
                throw new IllegalArgumentException("Bản ghi chấm công không hợp lệ hoặc trùng ngày: " + muc);
            }
            bangChamCong.put(ngay, soGio);
            if (vao != null) gioVaoRa.put(ngay, new LocalTime[]{vao, ra});
        }
    }

    protected static void kiemTraSoTruong(String[] f, int toiThieu) {
        if (f.length < toiThieu) {
            throw new IllegalArgumentException("Dòng dữ liệu thiếu trường (cần ít nhất " + toiThieu + ")");
        }
    }

    protected static String truongTai(String[] f, int viTri) {
        return viTri < f.length ? f[viTri] : "";
    }

    // hàm get với set

    public String getMaNV() { return maNV; }
    public String getHoTen() { return hoTen; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public String getGioiTinh() { return gioiTinh; }
    public String getSoDienThoai() { return soDienThoai; }
    public String getDiaChi() { return diaChi; }
    public LocalDate getNgayVaoLam() { return ngayVaoLam; }
    public double getLuongCoBan() { return luongCoBan; }

    public final void setMaNV(String maNV) {
        String s = lamSach(maNV);
        if (s.isEmpty()) throw new IllegalArgumentException("Mã nhân viên không được để trống");
        this.maNV = s.toUpperCase();
    }

    public final void setHoTen(String hoTen) {
        String s = lamSach(hoTen);
        if (s.isEmpty()) throw new IllegalArgumentException("Họ tên không được để trống");
        this.hoTen = s;
    }

    public final void setNgaySinh(LocalDate ngaySinh) {
        if (ngaySinh == null || ngaySinh.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ");
        }
        this.ngaySinh = ngaySinh;
    }

    public final void setGioiTinh(String gioiTinh) {
        String s = lamSach(gioiTinh).toLowerCase();
        if (s.equals("nam")) this.gioiTinh = "Nam";
        else if (s.equals("nữ") || s.equals("nu")) this.gioiTinh = "Nữ";
        else throw new IllegalArgumentException("Giới tính phải là Nam hoặc Nữ");
    }

    public final void setSoDienThoai(String soDienThoai) {
        String s = lamSach(soDienThoai);
        if (!s.isEmpty() && !s.matches("0\\d{9}")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0");
        }
        this.soDienThoai = s;
    }

    public final void setDiaChi(String diaChi) {
        this.diaChi = lamSach(diaChi);
    }

    public final void setNgayVaoLam(LocalDate ngayVaoLam) {
        if (ngayVaoLam == null || ngayVaoLam.isAfter(LocalDate.now())
                || (ngaySinh != null && !ngayVaoLam.isAfter(ngaySinh))) {
            throw new IllegalArgumentException("Ngày vào làm phải sau ngày sinh và không được ở tương lai");
        }
        this.ngayVaoLam = ngayVaoLam;
    }

    public final void setLuongCoBan(double luongCoBan) {
        if (!Double.isFinite(luongCoBan) || luongCoBan < 0) {
            throw new IllegalArgumentException("Lương cơ bản phải là số hữu hạn, không âm");
        }
        this.luongCoBan = luongCoBan;
    }

    protected static YearMonth kiemTraKyLuong(int thang, int nam) {
        try {
            return YearMonth.of(nam, thang);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Tháng/năm không hợp lệ: " + thang + "/" + nam, e);
        }
    }

    //tool
    protected static String lamSach(String s) {
        return s == null ? "" : s.replace('|', '/').replaceAll("[\\r\\n]+", " ").trim();
    }

    public static String dinhDangTien(double soTien) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(Math.round(soTien)) + " VND";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NhanVien)) return false;
        return maNV.equals(((NhanVien) o).maNV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maNV);
    }

    @Override
    public String toString() {
        return String.format("%-6s | %-22s | %-13s | %-4s | Vào làm: %s | Lương CB: %s",
                maNV, hoTen, getVaiTro(), gioiTinh, ngayVaoLam.format(DINH_DANG_NGAY), dinhDangTien(luongCoBan));
    }
}
