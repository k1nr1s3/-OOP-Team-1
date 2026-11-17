package model;

import iface.IGiamGia;

// 1 dòng trong bảng thống kê doanh thu (theo ngày, tháng, sản phẩm, nhân viên...)
// khoa: dùng để gom nhóm + sắp xếp (vd 2026-10-05, SP01, NV02), ten: nhãn hiển thị
public class DongThongKe {

    private final String khoa;
    private final String ten;
    private int soHoaDon;
    private int soLuong;       // tổng số món bán ra
    private double doanhThu;   // doanh thu thuần (sau giảm giá, chưa VAT)

    public DongThongKe(String khoa, String ten) {
        this.khoa = khoa;
        this.ten = ten;
    }

    // cộng dồn thêm số liệu của 1 hoá đơn (hoặc 1 dòng món)
    public void cong(int soHoaDon, int soLuong, double doanhThu) {
        this.soHoaDon += soHoaDon;
        this.soLuong += soLuong;
        this.doanhThu += doanhThu;
    }

    public String getKhoa() { return khoa; }
    public String getTen() { return ten; }
    public int getSoHoaDon() { return soHoaDon; }
    public int getSoLuong() { return soLuong; }
    public double getDoanhThu() { return doanhThu; }

    @Override
    public String toString() {
        return String.format("%-10s %-26s %6d %7d %16s", khoa, ten, soHoaDon, soLuong,
                IGiamGia.dinhDangTien(doanhThu));
    }
}
