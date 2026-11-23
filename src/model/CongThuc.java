package model;

import iface.IQuanLy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Công thức làm 1 phần món: danh sách (mã nguyên liệu -> liều lượng cho 1 phần).
 * Quan hệ has-a: SanPham HAS-A CongThuc; Combo HAS-A các SanPham thành phần nên công thức của Combo
 * là tổng công thức của các món bên trong.
 * Định dạng file: NL01:0.25,NL02:0.05
 */
public class CongThuc {

    /** Giá trị trả về của soLuongToiDaTao khi công thức rỗng (món chưa gắn nguyên liệu nên k bị giới hạn). */
    public static final int KHONG_GIOI_HAN = Integer.MAX_VALUE;

    private final Map<String, Double> thanhPhan = new LinkedHashMap<>();

    public CongThuc() {
    }

    // ===================== Sửa công thức =====================

    // thêm nguyên liệu vào công thức, đã có rồi thì ghi đè liều lượng mới
    public void datLieuLuong(String maNL, double lieuLuong) {
        String ma = chuanHoaMa(maNL);
        if (!Double.isFinite(lieuLuong) || lieuLuong <= 0) {
            throw new IllegalArgumentException("Liều lượng phải là số dương");
        }
        thanhPhan.put(ma, lieuLuong);
    }

    public boolean xoaNguyenLieu(String maNL) {
        return thanhPhan.remove(chuanHoaMa(maNL)) != null;
    }

    public void xoaHet() {
        thanhPhan.clear();
    }

    // cộng công thức khác vào công thức này (nhân với heSo phần) - dùng để gộp công thức của Combo / hoá đơn
    public void congThem(CongThuc khac, int heSo) {
        if (khac == null || heSo <= 0) {
            return;
        }
        for (Map.Entry<String, Double> e : khac.thanhPhan.entrySet()) {
            thanhPhan.merge(e.getKey(), e.getValue() * heSo, Double::sum);
        }
    }

    // ===================== Truy vấn =====================

    public boolean isRong() {
        return thanhPhan.isEmpty();
    }

    public int soThanhPhan() {
        return thanhPhan.size();
    }

    public boolean coNguyenLieu(String maNL) {
        return thanhPhan.containsKey(chuanHoaMa(maNL));
    }

    public double getLieuLuong(String maNL) {
        return thanhPhan.getOrDefault(chuanHoaMa(maNL), 0.0);
    }

    public Map<String, Double> getThanhPhan() {
        return Collections.unmodifiableMap(thanhPhan);
    }

    // ===================== Liên quan kho =====================

    // số phần tối đa làm được với tồn kho hiện tại (nguyên liệu k có trong kho = 0). Công thức rỗng -> KHONG_GIOI_HAN
    public int soLuongToiDaTao(IQuanLy<NguyenLieu> kho) {
        if (thanhPhan.isEmpty()) {
            return KHONG_GIOI_HAN;
        }
        long toiDa = Long.MAX_VALUE;
        for (Map.Entry<String, Double> e : thanhPhan.entrySet()) {
            NguyenLieu nl = kho.timTheoMa(e.getKey());
            double ton = nl == null ? 0 : nl.getSoLuongTon();
            toiDa = Math.min(toiDa, (long) Math.floor(ton / e.getValue() + 1e-9));
        }
        return (int) Math.max(0, Math.min(toiDa, Integer.MAX_VALUE - 1));
    }

    // các nguyên liệu trong công thức đang bị khoá
    public List<NguyenLieu> nguyenLieuBiKhoa(IQuanLy<NguyenLieu> kho) {
        List<NguyenLieu> kq = new ArrayList<>();
        for (String ma : thanhPhan.keySet()) {
            NguyenLieu nl = kho.timTheoMa(ma);
            if (nl != null && nl.isKhoa()) {
                kq.add(nl);
            }
        }
        return kq;
    }

    // mô tả các nguyên liệu k đủ để làm công thức này (rỗng = đủ)
    public List<String> nguyenLieuKhongDu(IQuanLy<NguyenLieu> kho) {
        List<String> kq = new ArrayList<>();
        for (Map.Entry<String, Double> e : thanhPhan.entrySet()) {
            NguyenLieu nl = kho.timTheoMa(e.getKey());
            double ton = nl == null ? 0 : nl.getSoLuongTon();
            if (ton + 1e-9 < e.getValue()) {
                String ten = nl == null ? e.getKey() : nl.getTenNL();
                String dv = nl == null ? "" : " " + nl.getDonViTinh();
                kq.add(String.format(Locale.US, "%s (cần %s%s, còn %s%s)",
                        ten, dinhDangSo(e.getValue()), dv, dinhDangSo(ton), dv));
            }
        }
        return kq;
    }

    // mô tả dễ đọc: "Thit ga 0.25 kg, Dau an 0.03 lit"
    public String moTa(IQuanLy<NguyenLieu> kho) {
        if (thanhPhan.isEmpty()) {
            return "(chưa có công thức)";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> e : thanhPhan.entrySet()) {
            NguyenLieu nl = kho == null ? null : kho.timTheoMa(e.getKey());
            if (sb.length() > 0) sb.append(", ");
            sb.append(nl == null ? e.getKey() : nl.getTenNL() + " (" + nl.getMaNL() + ")")
              .append(' ').append(dinhDangSo(e.getValue()))
              .append(nl == null ? "" : " " + nl.getDonViTinh());
        }
        return sb.toString();
    }

    // ===================== Đọc / ghi file =====================

    public String toFileString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> e : thanhPhan.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(':').append(dinhDangSo(e.getValue()));
        }
        return sb.toString();
    }

    // chuỗi rỗng -> công thức rỗng; sai định dạng -> IllegalArgumentException
    public static CongThuc fromFileString(String chuoi) {
        CongThuc ct = new CongThuc();
        if (chuoi == null || chuoi.trim().isEmpty()) {
            return ct;
        }
        for (String muc : chuoi.split(",")) {
            String[] kv = muc.trim().split(":", -1);
            if (kv.length != 2) {
                throw new IllegalArgumentException("Thành phần công thức không hợp lệ: " + muc);
            }
            try {
                ct.datLieuLuong(kv[0], Double.parseDouble(kv[1].trim().replace(",", ".")));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Liều lượng không hợp lệ: " + muc);
            }
        }
        return ct;
    }

    // ===================== TOOLS =====================

    private static String chuanHoaMa(String maNL) {
        String s = maNL == null ? "" : maNL.trim().toUpperCase();
        if (s.isEmpty() || s.contains(":") || s.contains(",") || s.contains("|") || s.contains(";")) {
            throw new IllegalArgumentException("Mã nguyên liệu không hợp lệ: " + maNL);
        }
        return s;
    }

    // 0.250000 -> "0.25", 3.0 -> "3"
    public static String dinhDangSo(double so) {
        return BigDecimal.valueOf(Math.round(so * 10000) / 10000.0).stripTrailingZeros().toPlainString();
    }
}
