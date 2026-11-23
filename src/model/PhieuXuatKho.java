package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Phiếu xuất kho: mã tự sinh, nội dung (lý do xuất), người lập và 1 hoặc nhiều dòng (nguyên liệu - số lượng xuất).
 * Định dạng file: maPhieu;yyyy-MM-ddTHH:mm:ss;maNV;noiDung;maNL:soLuong,maNL:soLuong
 */
public class PhieuXuatKho {

    public static final DateTimeFormatter DINH_DANG_HIEN_THI = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String maPhieu;
    private final LocalDateTime thoiGian;
    private final String maNV;
    private final String noiDung;
    private final Map<String, Double> chiTiet = new LinkedHashMap<>();   // maNL -> số lượng xuất

    public PhieuXuatKho(String maPhieu, LocalDateTime thoiGian, String maNV, String noiDung) {
        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã phiếu xuất không được rỗng");
        }
        String nd = lamSach(noiDung);
        if (nd.isEmpty()) {
            throw new IllegalArgumentException("Phải ghi nội dung (lý do) xuất kho");
        }
        this.maPhieu = maPhieu.trim().toUpperCase();
        this.thoiGian = thoiGian == null ? LocalDateTime.now() : thoiGian;
        this.maNV = lamSach(maNV).toUpperCase();
        this.noiDung = nd;
    }

    // thêm 1 dòng nguyên liệu - số lượng; trùng nguyên liệu thì cộng dồn
    public void themDong(String maNL, double soLuong) {
        String ma = lamSach(maNL).toUpperCase();
        if (ma.isEmpty() || ma.contains(":") || ma.contains(",")) {
            throw new IllegalArgumentException("Mã nguyên liệu không hợp lệ: " + maNL);
        }
        if (!Double.isFinite(soLuong) || soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng xuất phải là số dương (" + ma + ")");
        }
        chiTiet.merge(ma, soLuong, Double::sum);
    }

    public String getMaPhieu() { return maPhieu; }
    public LocalDateTime getThoiGian() { return thoiGian; }
    public String getMaNV() { return maNV; }
    public String getNoiDung() { return noiDung; }
    public Map<String, Double> getChiTiet() { return Collections.unmodifiableMap(chiTiet); }
    public int soDong() { return chiTiet.size(); }

    public String toFileString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> e : chiTiet.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(':').append(CongThuc.dinhDangSo(e.getValue()));
        }
        return String.join(";", maPhieu, thoiGian.withNano(0).toString(), maNV, noiDung, sb.toString());
    }

    // dòng sai định dạng -> trả về null
    public static PhieuXuatKho fromFileString(String line) {
        String[] p = line.split(";", -1);
        if (p.length < 5) {
            return null;
        }
        try {
            PhieuXuatKho px = new PhieuXuatKho(p[0], LocalDateTime.parse(p[1].trim()), p[2], p[3]);
            for (String muc : p[4].split(",")) {
                if (muc.trim().isEmpty()) continue;
                String[] kv = muc.trim().split(":", -1);
                px.themDong(kv[0], Double.parseDouble(kv[1].trim().replace(",", ".")));
            }
            return px.chiTiet.isEmpty() ? null : px;
        } catch (RuntimeException e) {
            return null;
        }
    }

    // in phiếu ra màn hình; tenNL có thể null
    public String taoPhieu(java.util.function.Function<String, NguyenLieu> tra) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("===== PHIẾU XUẤT KHO %s =====%n", maPhieu));
        sb.append(String.format("Thời gian : %s%n", thoiGian.format(DINH_DANG_HIEN_THI)));
        sb.append(String.format("Người lập : %s%n", maNV));
        sb.append(String.format("Nội dung  : %s%n", noiDung));
        for (Map.Entry<String, Double> e : chiTiet.entrySet()) {
            NguyenLieu nl = tra == null ? null : tra.apply(e.getKey());
            sb.append(String.format(Locale.US, "  - %-6s %-24s : %s %s%n", e.getKey(),
                    nl == null ? "" : nl.getTenNL(), CongThuc.dinhDangSo(e.getValue()),
                    nl == null ? "" : nl.getDonViTinh()));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("%-6s | %s | %-6s | %-30s | %d dòng", maPhieu, thoiGian.format(DINH_DANG_HIEN_THI),
                maNV, noiDung, chiTiet.size());
    }

    private static String lamSach(String s) {
        return s == null ? "" : s.replace(';', ',').replace('|', '/').replaceAll("[\\r\\n]+", " ").trim();
    }
}
