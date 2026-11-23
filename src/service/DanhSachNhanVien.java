package service;

import iface.IFile;
import iface.IQuanLy;
import model.NhanVien;
import model.PhaCheCheBien;
import model.QuanLy;
import model.ThuNgan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BẢNG VÍ DỤ 
 *
 * Định dạng mỗi dòng trong data/nhanvien.txt (ngăn cách bằng '|'):
 *   LOAI|ma|hoTen|ngaySinh|gioiTinh|sdt|diaChi|ngayVaoLam|luongCoBan|<trường riêng>|<chấm công>
 *   TN: capBac|caLam|soQuay|doanhThuKy (+ cuối dòng: lịch sử ca yyyy-MM-dd:CA,...)          NB: capBac|soLyPhaChe|soMonCheBien          QL: chucVu|phuCap|tyLeKPI
 *   Chấm công: yyyy-MM-dd:soGio,yyyy-MM-dd:soGio,...
 */
public class DanhSachNhanVien implements IQuanLy<NhanVien>, IFile {

    private final List<NhanVien> dsNV = new ArrayList<>();
    
    public String maMoiTuDong() {
        int soLonNhat = 0;
        for (NhanVien nv : dsNV) {
            String ma = nv.getMaNV();
            if (ma.matches("NV\\d+")) {
                soLonNhat = Math.max(soLonNhat, Integer.parseInt(ma.substring(2)));
            }
        }
        return String.format("NV%02d", soLonNhat + 1);
    }

    // ===================== Quản lý danh sách =====================

    @Override
    public int soLuong() { return dsNV.size(); }

    public List<NhanVien> getDanhSach() {
        return Collections.unmodifiableList(dsNV);
    }

    //Trả về false nếu null hoặc trùng mã
    @Override
    public boolean them(NhanVien nv) {
        if (nv == null || timTheoMa(nv.getMaNV()) != null) return false;
        return dsNV.add(nv);
    }

    @Override
    public boolean xoa(String maNV) {
        NhanVien nv = timTheoMa(maNV);
        return nv != null && dsNV.remove(nv);
    }

    @Override
    public void hienThi() {
        if (dsNV.isEmpty()) {
            System.out.println("Danh sách nhân viên trống.");
            return;
        }
        dsNV.forEach(System.out::println);
    }

    // ===================== Chấm công =====================

    public boolean chamCong(String maNV, LocalDate ngay, double soGio) {
        NhanVien nv = timTheoMa(maNV);
        return nv != null && nv.chamCong(ngay, soGio);
    }

    public boolean chamCong(String maNV, LocalDate ngay, LocalTime gioVao, LocalTime gioRa) {
        NhanVien nv = timTheoMa(maNV);
        return nv != null && nv.chamCong(ngay, gioVao, gioRa);
    }

    public boolean suaChamCong(String maNV, LocalDate ngay, double soGio) {
        NhanVien nv = timTheoMa(maNV);
        return nv != null && nv.suaChamCong(ngay, soGio);
    }

    public boolean xoaChamCong(String maNV, LocalDate ngay) {
        NhanVien nv = timTheoMa(maNV);
        return nv != null && nv.xoaChamCong(ngay);
    }

    public boolean ghiNhanSoLy(String maNV, int thang, int nam, int soLy) {
        NhanVien nv = timTheoMa(maNV);
        if (!(nv instanceof PhaCheCheBien) || soLy <= 0) return false;
        try {
            ((PhaCheCheBien) nv).themSoLy(YearMonth.of(nam, thang), soLy);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    public boolean ghiNhanSoMon(String maNV, int thang, int nam, int soMon) {
        NhanVien nv = timTheoMa(maNV);
        if (!(nv instanceof PhaCheCheBien) || soMon <= 0) return false;
        try {
            ((PhaCheCheBien) nv).themSoMon(YearMonth.of(nam, thang), soMon);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    // ----- Check-in / check-out (thu ngân, nhân viên bếp tự chấm cho mình) -----

    // Ghi nhận giờ vào ca = giờ hiện tại. Trả về thời điểm đã ghi; lỗi thì ném ngoại lệ kèm lý do.
    public LocalDateTime checkIn(String maNV) {
        NhanVien nv = timTheoMa(maNV);
        if (nv == null) throw new NoSuchElementException("Không tìm thấy nhân viên " + maNV);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        nv.checkIn(now);
        return now;
    }

    // Ghi nhận giờ ra ca = giờ hiện tại, tự tính số giờ làm. Trả về số giờ làm của ca vừa kết thúc.
    public double checkOut(String maNV) {
        NhanVien nv = timTheoMa(maNV);
        if (nv == null) throw new NoSuchElementException("Không tìm thấy nhân viên " + maNV);
        return nv.checkOut(LocalDateTime.now().withNano(0));
    }

    // Trạng thái chấm công hôm nay của 1 nhân viên, để in lên đầu menu chấm công.
    public String trangThaiChamCong(String maNV) {
        NhanVien nv = timTheoMa(maNV);
        if (nv == null) return "Không tìm thấy nhân viên " + maNV;
        LocalDateTime mo = nv.getCheckInDangMo();
        if (mo != null) {
            return "Đang trong ca: check-in lúc " + mo.toLocalTime().format(NhanVien.DINH_DANG_GIO)
                    + " ngày " + mo.toLocalDate().format(NhanVien.DINH_DANG_NGAY);
        }
        LocalDate homNay = LocalDate.now();
        Double gio = nv.getBangChamCong().get(homNay);
        if (gio == null) return "Hôm nay chưa chấm công.";
        LocalTime vao = nv.getGioVao(homNay);
        LocalTime ra = nv.getGioRa(homNay);
        return "Hôm nay đã chấm công: " + (vao == null ? "" : vao.format(NhanVien.DINH_DANG_GIO) + " - "
                + ra.format(NhanVien.DINH_DANG_GIO) + ", ") + String.format("%.2f giờ", gio);
    }

    // Bảng chấm công CỦA RIÊNG 1 nhân viên trong tháng: từng ngày (giờ vào, giờ ra, số giờ) và tổng kết.
    public void inBangChamCongCaNhan(String maNV, int thang, int nam) {
        NhanVien nv = timTheoMa(maNV);
        if (nv == null) throw new NoSuchElementException("Không tìm thấy nhân viên " + maNV);
        YearMonth ky = YearMonth.of(nam, thang);
        System.out.printf("===== BẢNG CHẤM CÔNG THÁNG %02d/%d - %s (%s) =====%n", thang, nam, nv.getHoTen(), nv.getMaNV());
        System.out.printf("%-12s %-10s %-10s %10s %10s%n", "Ngày", "Giờ vào", "Giờ ra", "Số giờ", "Ngày công");
        for (Map.Entry<LocalDate, Double> e : nv.getBangChamCong().entrySet()) {
            if (!YearMonth.from(e.getKey()).equals(ky)) continue;
            LocalTime vao = nv.getGioVao(e.getKey());
            LocalTime ra = nv.getGioRa(e.getKey());
            System.out.printf("%-12s %-10s %-10s %10.2f %10.2f%n", e.getKey().format(NhanVien.DINH_DANG_NGAY),
                    vao == null ? "-" : vao.format(NhanVien.DINH_DANG_GIO),
                    ra == null ? "-" : ra.format(NhanVien.DINH_DANG_GIO),
                    e.getValue(), Math.min(e.getValue(), NhanVien.GIO_CHUAN_MOI_NGAY) / NhanVien.GIO_CHUAN_MOI_NGAY);
        }
        LocalDateTime mo = nv.getCheckInDangMo();
        if (mo != null && YearMonth.from(mo).equals(ky)) {
            System.out.printf("%-12s %-10s %-10s %10s %10s%n", mo.toLocalDate().format(NhanVien.DINH_DANG_NGAY),
                    mo.toLocalTime().format(NhanVien.DINH_DANG_GIO), "(đang làm)", "-", "-");
        }
        System.out.printf("Tổng: %d ngày làm | %.2f ngày công | %.2f giờ làm thêm%n",
                nv.tinhSoNgayDiLam(thang, nam), nv.tinhSoNgayCong(thang, nam), nv.tinhGioLamThem(thang, nam));
    }

    // Nhân viên chưa có ngày chấm công nào trong tháng.
    public List<NhanVien> nhanVienChuaChamCong(int thang, int nam) {
        return dsNV.stream()
                .filter(nv -> nv.tinhSoNgayDiLam(thang, nam) == 0)
                .collect(Collectors.toList());
    }

    public void inBangChamCong(int thang, int nam) {
        System.out.printf("===== BẢNG CHẤM CÔNG THÁNG %02d/%d =====%n", thang, nam);
        System.out.printf("%-6s %-22s %-13s %10s %10s %10s%n",
                "Mã", "Họ tên", "Vai trò", "Ngày làm", "Ngày công", "Giờ OT");
        for (NhanVien nv : dsNV) {
            System.out.printf("%-6s %-22s %-13s %10d %10.2f %10.2f%n",
                    nv.getMaNV(), nv.getHoTen(), nv.getVaiTro(),
                    nv.tinhSoNgayDiLam(thang, nam), nv.tinhSoNgayCong(thang, nam), nv.tinhGioLamThem(thang, nam));
        }
    }

    // ===================== Tính lương =====================

    public double tinhLuong(String maNV, int thang, int nam) {
        NhanVien nv = timTheoMa(maNV);
        if (nv == null) throw new NoSuchElementException("Không tìm thấy nhân viên " + maNV);
        return nv.tinhLuong(thang, nam);
    }

    public double tinhTongLuong(int thang, int nam) {
        return dsNV.stream().mapToDouble(nv -> nv.tinhLuong(thang, nam)).sum();
    }

    // tính lương trung bình của mấy đứa trên 1 ngày công trong tháng 
    public double tinhLuongTrungBinh(int thang, int nam) {
        return dsNV.stream()
                .filter(nv -> nv.tinhSoNgayDiLam(thang, nam) > 0)
                .mapToDouble(nv -> nv.tinhLuong(thang, nam))
                .average().orElse(0);
    }

    public void inBangLuong(int thang, int nam) {
        System.out.printf("===== BẢNG LƯƠNG THÁNG %02d/%d =====%n", thang, nam);
        System.out.printf("%-6s %-22s %-13s %10s %18s%n", "Mã", "Họ tên", "Vai trò", "Ngày công", "Thực lĩnh");
        for (NhanVien nv : dsNV) {
            System.out.printf("%-6s %-22s %-13s %10.2f %18s%n",
                    nv.getMaNV(), nv.getHoTen(), nv.getVaiTro(),
                    nv.tinhSoNgayCong(thang, nam), NhanVien.dinhDangTien(nv.tinhLuong(thang, nam)));
        }
        System.out.println("Tổng chi lương: " + NhanVien.dinhDangTien(tinhTongLuong(thang, nam)));
    }

    // ===================== Đổi ca / cấp bậc / chức vụ =====================

    // Đổi ca của thu ngân kể từ ngày tuNgay, các ngày trước đó vẫn tính theo ca cũ.
    // False nếu không có mã, không phải thu ngân hoặc ca mới trùng ca đang áp dụng.
    // Ném IllegalArgumentException nếu ngày đổi không hợp lệ.
    public boolean doiCaLam(String maNV, ThuNgan.CaLam caMoi, LocalDate tuNgay) {
        NhanVien nv = timTheoMa(maNV);
        if (!(nv instanceof ThuNgan) || caMoi == null) return false;
        return ((ThuNgan) nv).doiCaTu(caMoi, tuNgay);
    }

    public boolean doiCaLam(String maNV, ThuNgan.CaLam caMoi) {
        return doiCaLam(maNV, caMoi, LocalDate.now());
    }

    // Đổi cấp bậc (học việc / chính thức) của thu ngân. False nếu không phải thu ngân hoặc trùng cấp bậc cũ.
    public boolean doiCapBac(String maNV, ThuNgan.CapBac capBacMoi) {
        NhanVien nv = timTheoMa(maNV);
        if (!(nv instanceof ThuNgan) || capBacMoi == null) return false;
        ThuNgan tn = (ThuNgan) nv;
        if (tn.getCapBac() == capBacMoi) return false;
        tn.setCapBac(capBacMoi);
        return true;
    }

    // Đổi cấp bậc của nhân viên bếp. False nếu không phải nhân viên bếp hoặc trùng cấp bậc cũ.
    public boolean doiCapBac(String maNV, PhaCheCheBien.CapBac capBacMoi) {
        NhanVien nv = timTheoMa(maNV);
        if (!(nv instanceof PhaCheCheBien) || capBacMoi == null) return false;
        PhaCheCheBien pc = (PhaCheCheBien) nv;
        if (pc.getCapBac() == capBacMoi) return false;
        pc.setCapBac(capBacMoi);
        return true;
    }

    // Đổi chức vụ + phụ cấp chức vụ của quản lý. Dữ liệu sai thì ném IllegalArgumentException và giữ nguyên giá trị cũ.
    // False nếu không phải quản lý hoặc không có gì thay đổi.
    public boolean doiChucVu(String maNV, String chucVuMoi, double phuCapMoi) {
        NhanVien nv = timTheoMa(maNV);
        if (!(nv instanceof QuanLy)) return false;
        QuanLy ql = (QuanLy) nv;
        String chucCu = ql.getChucVu();
        double phuCapCu = ql.getPhuCapChucVu();
        try {
            ql.setChucVu(chucVuMoi);
            ql.setPhuCapChucVu(phuCapMoi);
        } catch (IllegalArgumentException e) {
            ql.setChucVu(chucCu);
            ql.setPhuCapChucVu(phuCapCu);
            throw e;
        }
        return !chucCu.equals(ql.getChucVu()) || phuCapCu != ql.getPhuCapChucVu();
    }

    // ===================== Tìm kiếm =====================

    // tìm theo mã nv, k có thì NULL
    @Override
    public NhanVien timTheoMa(String maNV) {
        if (maNV == null) return null;
        String ma = maNV.trim();
        for (NhanVien nv : dsNV) {
            if (nv.getMaNV().equalsIgnoreCase(ma)) return nv;
        }
        return null;
    }

    // tìm theo tên ( bỏ dáu)
    public List<NhanVien> timTheoTen(String tuKhoa) {
        String k = boDau(tuKhoa);
        if (k.isEmpty()) return Collections.emptyList();
        return dsNV.stream()
                .filter(nv -> boDau(nv.getHoTen()).contains(k))
                .collect(Collectors.toList());
    }

    // tìm theo vai trò  
    public List<NhanVien> timTheoVaiTro(Class<? extends NhanVien> loai) {
        if (loai == null) return Collections.emptyList();
        return dsNV.stream().filter(loai::isInstance).collect(Collectors.toList());
    }

    public List<NhanVien> timTheoGioiTinh(String gioiTinh) {
        String g = boDau(gioiTinh);
        return dsNV.stream()
                .filter(nv -> boDau(nv.getGioiTinh()).equals(g))
                .collect(Collectors.toList());
    }

    public List<NhanVien> timTheoKhoangLuongCoBan(double min, double max) {
        kiemTraKhoangSo(min, max, "lương cơ bản");
        return dsNV.stream()
                .filter(nv -> nv.getLuongCoBan() >= min && nv.getLuongCoBan() <= max)
                .collect(Collectors.toList());
    }

    // tìm theo lương thực lĩnh trong tháng 
    public List<NhanVien> timTheoKhoangLuong(double min, double max, int thang, int nam) {
        kiemTraKhoangSo(min, max, "lương thực lĩnh");
        return dsNV.stream()
                .filter(nv -> {
                    double luong = nv.tinhLuong(thang, nam);
                    return luong >= min && luong <= max;
                })
                .collect(Collectors.toList());
    }

    // Tìm theo mã, tên, số điện thoại hoặc địa chỉ
    public List<NhanVien> timKiem(String tuKhoa) {
        String k = boDau(tuKhoa);
        if (k.isEmpty()) return Collections.emptyList();
        return dsNV.stream()
                .filter(nv -> boDau(nv.getMaNV()).contains(k)
                        || boDau(nv.getHoTen()).contains(k)
                        || boDau(nv.getSoDienThoai()).contains(k)
                        || boDau(nv.getDiaChi()).contains(k))
                .collect(Collectors.toList());
    }

    // ===================== Thống kê =====================

    // Số lượng nhân viên theo vai trò: {Thu ngân=2, Nhân viên bếp=3, Quản lý=1}.
    public Map<String, Long> demTheoVaiTro() {
        return dsNV.stream().collect(Collectors.groupingBy(
                NhanVien::getVaiTro, LinkedHashMap::new, Collectors.counting()));
    }

    // Số lượng nhân viên theo giới tính: {Nam=3, Nữ=2}.
    public Map<String, Long> demTheoGioiTinh() {
        return dsNV.stream().collect(Collectors.groupingBy(
                NhanVien::getGioiTinh, LinkedHashMap::new, Collectors.counting()));
    }

    // Tổng lương thực lĩnh từng vai trò trong tháng.
    public Map<String, Double> tongLuongTheoVaiTro(int thang, int nam) {
        return dsNV.stream().collect(Collectors.groupingBy(
                NhanVien::getVaiTro, LinkedHashMap::new,
                Collectors.summingDouble(nv -> nv.tinhLuong(thang, nam))));
    }

    public Optional<NhanVien> nhanVienLuongCaoNhat(int thang, int nam) {
        return dsNV.stream()
                .filter(nv -> nv.tinhSoNgayDiLam(thang, nam) > 0)
                .max(Comparator.comparingDouble(nv -> nv.tinhLuong(thang, nam)));
    }

    public Optional<NhanVien> nhanVienLuongThapNhat(int thang, int nam) {
        return dsNV.stream()
                .filter(nv -> nv.tinhSoNgayDiLam(thang, nam) > 0)
                .min(Comparator.comparingDouble(nv -> nv.tinhLuong(thang, nam)));
    }

    public Optional<NhanVien> nhanVienNhieuGioLamThemNhat(int thang, int nam) {
        return dsNV.stream()
                .filter(nv -> nv.tinhGioLamThem(thang, nam) > 0)
                .max(Comparator.comparingDouble(nv -> nv.tinhGioLamThem(thang, nam)));
    }

    // In báo cáo thống kê tổng hợp của một tháng
    public void inThongKe(int thang, int nam) {
        System.out.printf("===== THỐNG KÊ NHÂN VIÊN THÁNG %02d/%d =====%n", thang, nam);
        System.out.println("Tổng số nhân viên : " + soLuong());

        System.out.println("Theo vai trò      : " + demTheoVaiTro());
        System.out.println("Theo giới tính    : " + demTheoGioiTinh());

        System.out.println("Tổng chi lương    : " + NhanVien.dinhDangTien(tinhTongLuong(thang, nam)));
        System.out.println("Lương trung bình  : " + NhanVien.dinhDangTien(tinhLuongTrungBinh(thang, nam)));
        tongLuongTheoVaiTro(thang, nam).forEach((vaiTro, tien) ->
                System.out.println("  - " + vaiTro + ": " + NhanVien.dinhDangTien(tien)));

        nhanVienLuongCaoNhat(thang, nam).ifPresent(nv ->
                System.out.println("Lương cao nhất    : " + nv.getHoTen() + " (" + NhanVien.dinhDangTien(nv.tinhLuong(thang, nam)) + ")"));
        nhanVienLuongThapNhat(thang, nam).ifPresent(nv ->
                System.out.println("Lương thấp nhất   : " + nv.getHoTen() + " (" + NhanVien.dinhDangTien(nv.tinhLuong(thang, nam)) + ")"));
        nhanVienNhieuGioLamThemNhat(thang, nam).ifPresent(nv ->
                System.out.printf("Làm thêm nhiều nhất: %s (%.2f giờ)%n", nv.getHoTen(), nv.tinhGioLamThem(thang, nam)));

        List<NhanVien> chuaCham = nhanVienChuaChamCong(thang, nam);
        System.out.println("Chưa chấm công    : " + chuaCham.size()
                + (chuaCham.isEmpty() ? "" : " -> " + chuaCham.stream().map(NhanVien::getHoTen).collect(Collectors.joining(", "))));
    }

    // ===================== Đọc / ghi file =====================

    /**
     * Đọc danh sách từ file (thay thế dữ liệu hiện tại). Dòng trống hoặc bắt đầu bằng '#' bị bỏ qua,
     * dòng sai định dạng bị báo lỗi ra System.err và bỏ qua.
     * @return số nhân viên đọc được (file không tồn tại -> 0, giữ nguyên danh sách)
     */
    @Override
    public int docFile(String duongDan) throws IOException {
        Path path = Paths.get(duongDan);
        if (!Files.exists(path)) return 0;

        List<NhanVien> docDuoc = new ArrayList<>();
        int soDong = 0;
        for (String dong : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            soDong++;
            dong = dong.trim();
            if (dong.isEmpty() || dong.startsWith("#")) continue;
            try {
                NhanVien nv = taoTuDong(dong);
                if (docDuoc.stream().anyMatch(x -> x.getMaNV().equalsIgnoreCase(nv.getMaNV()))) {
                    System.err.println("Dòng " + soDong + ": trùng mã nhân viên, bỏ qua");
                } else {
                    docDuoc.add(nv);
                }
            } catch (RuntimeException e) {
                System.err.println("Dòng " + soDong + " sai định dạng (" + e.getMessage() + "), bỏ qua");
            }
        }
        dsNV.clear();
        dsNV.addAll(docDuoc);
        return dsNV.size();
    }

    @Override
    public void ghiFile(String duongDan) throws IOException {
        Path path = Paths.get(duongDan);
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        List<String> dong = dsNV.stream().map(NhanVien::toFileString).collect(Collectors.toList());
        Files.write(path, dong, StandardCharsets.UTF_8);
    }

    private static NhanVien taoTuDong(String dong) {
        String[] f = dong.split("\\|", -1);
        switch (f[0].trim()) {
            case "TN": return ThuNgan.taoTuChuoi(f);
            case "PC":
            case "NB": return PhaCheCheBien.taoTuChuoi(f);
            case "QL": return QuanLy.taoTuChuoi(f);
            default: throw new IllegalArgumentException("Loại nhân viên không hợp lệ: " + f[0]);
        }
    }

    // ===================== TOOLS =====================

    /** "Nguyễn Đình" -> "nguyen dinh" */
    private static String boDau(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return t.toLowerCase();
    }

    private static void kiemTraKhoangSo(double min, double max, String tenKhoang) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new IllegalArgumentException("Khoảng " + tenKhoang + " không hợp lệ");
        }
    }
}
