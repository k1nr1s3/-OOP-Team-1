package iface;

import java.text.NumberFormat;
import java.util.Locale;

// interface giảm giá: cái nào giảm giá được cho hoá đơn (khách hàng theo hạng, khuyến mãi...) thì implements cái này
public interface IGiamGia {

    // số tiền được giảm trên tổng tiền
    double tinhGiamGia(double tongTien);

    // mô tả ngắn ưu đãi, để in ra hoá đơn / màn hình
    String getMoTa();

    // số tiền còn phải trả sau khi giảm (default method)
    default double tinhThanhTien(double tongTien) {
        return tongTien - tinhGiamGia(tongTien);
    }

    // cộng dồn nhiều ưu đãi (đa hình qua interface), k vượt quá tổng tiền
    static double tongGiam(double tongTien, IGiamGia... uuDai) {
        double tong = 0;
        for (IGiamGia u : uuDai) {
            if (u != null) {
                tong += u.tinhGiamGia(tongTien);
            }
        }
        return Math.min(tong, tongTien);
    }

    // định dạng tiền giống NhanVien.dinhDangTien: 1.234.567 VND
    static String dinhDangTien(double soTien) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(Math.round(soTien)) + " VND";
    }
}
