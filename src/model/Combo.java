package model;

import iface.IGiamGia;
import iface.IQuanLy;


public class Combo extends SanPham {

    private String[] maThanhPhan;

    public Combo(String maSP, String tenSP, double donGia, String dsMa) {
        super(maSP, tenSP, donGia);
        setThanhPhan(dsMa);
    }

    @Override
    public String getLoai() { return "Combo"; }

    @Override
    protected String maLoai() { return "COMBO"; }

    @Override
    public String moTaThem() { return "Gồm: " + String.join(", ", maThanhPhan); }

    @Override
    protected String chuoiRieng() { return String.join(";", maThanhPhan); }

    @Override
    public void hienThiThongTin() {
        System.out.println("Combo: " + tenSP + " (" + maSP + ") - Giá: " + IGiamGia.dinhDangTien(donGia)
                + " - " + moTaThem());
    }

    // tổng giá nếu mua lẻ từng món (mã k có trong danh sách thì bỏ qua)
    public double tinhGiaLe(IQuanLy<SanPham> ds) {
        double tong = 0;
        for (String ma : maThanhPhan) {
            SanPham sp = ds.timTheoMa(ma);
            if (sp != null) {
                tong += sp.getDonGia();
            }
        }
        return tong;
    }

    // số tiền khách tiết kiệm khi mua combo so với mua lẻ
    public double tienTietKiem(IQuanLy<SanPham> ds) {
        return Math.max(tinhGiaLe(ds) - donGia, 0);
    }

    public String[] getMaThanhPhan() { return maThanhPhan.clone(); }

    public void setThanhPhan(String dsMa) {
        if (dsMa == null || dsMa.trim().isEmpty()) {
            throw new IllegalArgumentException("Combo phải có ít nhất 1 sản phẩm thành phần");
        }
        String[] p = dsMa.split("[;,]");
        for (int i = 0; i < p.length; i++) {
            p[i] = p[i].trim();
            if (p[i].isEmpty()) {
                throw new IllegalArgumentException("Danh sách mã thành phần k hợp lệ: " + dsMa);
            }
        }
        this.maThanhPhan = p;
    }
}
