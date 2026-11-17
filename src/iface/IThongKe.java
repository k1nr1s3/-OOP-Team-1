package iface;

import model.DongThongKe;

import java.time.LocalDate;
import java.time.YearMonth;

// interface thống kê doanh thu: lớp nào quản lý hoá đơn và thống kê được thì implements cái này (hiện có DanhSachHoaDon)
// doanh thu = tiền sau giảm giá, chưa gồm VAT, chỉ tính hoá đơn đã thanh toán
public interface IThongKe {

    // ---- phải cài đặt ----
    double doanhThu(LocalDate tu, LocalDate den);

    DongThongKe[] thongKeTheoNgay(LocalDate tu, LocalDate den);

    DongThongKe[] thongKeTheoThang(int nam);

    DongThongKe[] thongKeTheoSanPham(LocalDate tu, LocalDate den);

    DongThongKe[] topSanPham(LocalDate tu, LocalDate den, int k);

    DongThongKe[] thongKeTheoNhanVien(LocalDate tu, LocalDate den);

    void inBaoCaoNgay(LocalDate ngay);

    void inBaoCaoThang(int thang, int nam);

    void inBaoCaoNam(int nam);

    // ---- default method: dựa trên doanhThu(tu, den) nên lớp cài đặt k phải viết lại ----
    default double doanhThuNgay(LocalDate ngay) {
        return doanhThu(ngay, ngay);
    }

    default double doanhThuThang(int thang, int nam) {
        YearMonth ym = YearMonth.of(nam, thang);
        return doanhThu(ym.atDay(1), ym.atEndOfMonth());
    }

    default double doanhThuNam(int nam) {
        return doanhThu(LocalDate.of(nam, 1, 1), LocalDate.of(nam, 12, 31));
    }
}
