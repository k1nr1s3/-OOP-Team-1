package model;

import iface.IGiamGia;

public class ThucUong extends SanPham {

    public static final String[] SIZE_HOP_LE = {"S", "M", "L"};

    private String size;

    public ThucUong(String maSP, String tenSP, double donGia, String size) {
        super(maSP, tenSP, donGia);
        setSize(size);
    }

    @Override
    public String getLoai() { return "Thức uống"; }

    @Override
    protected String maLoai() { return "THUCUONG"; }

    @Override
    public String moTaThem() { return "Size " + size; }

    @Override
    protected String chuoiRieng() { return size; }

    @Override
    public void hienThiThongTin() {
        System.out.println("Thức uống: " + tenSP + " (" + maSP + ") - Giá: " + IGiamGia.dinhDangTien(donGia)
                + " - " + moTaThem());
    }

    public String getSize() { return size; }

    public void setSize(String size) {
        String s = size == null ? "" : size.trim().toUpperCase();
        for (String hopLe : SIZE_HOP_LE) {
            if (hopLe.equals(s)) {
                this.size = s;
                return;
            }
        }
        throw new IllegalArgumentException("Size phải là S, M hoặc L");
    }
}
