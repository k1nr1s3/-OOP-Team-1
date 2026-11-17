package model;

import iface.IGiamGia;

public class DoAn extends SanPham {

    private boolean laDoChay;

    public DoAn(String maSP, String tenSP, double donGia, boolean laDoChay) {
        super(maSP, tenSP, donGia);
        this.laDoChay = laDoChay;
    }

    @Override
    public String getLoai() { return "Đồ ăn"; }

    @Override
    protected String maLoai() { return "DOAN"; }

    @Override
    public String moTaThem() { return laDoChay ? "Món chay" : "Món mặn"; }

    @Override
    protected String chuoiRieng() { return String.valueOf(laDoChay); }

    @Override
    public void hienThiThongTin() {
        System.out.println("Đồ ăn: " + tenSP + " (" + maSP + ") - Giá: " + IGiamGia.dinhDangTien(donGia)
                + " - " + moTaThem());
    }

    public boolean isLaDoChay() { return laDoChay; }

    public void setLaDoChay(boolean laDoChay) { this.laDoChay = laDoChay; }
}
