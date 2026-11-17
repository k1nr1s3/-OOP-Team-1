package model;

import java.util.Locale;
import java.util.Scanner;

public class NguyenLieu {
    private String maNL;
    private String tenNL;
    private String donViTinh;
    private double soLuongTon;
    private double mucToiThieu;
    private double donGiaNhap;
    private String maNCC;

    public NguyenLieu() {
    }

    public NguyenLieu(String maNL, String tenNL, String donViTinh,
                      double soLuongTon, double mucToiThieu, double donGiaNhap, String maNCC) {
        this.maNL = maNL;
        this.tenNL = tenNL;
        this.donViTinh = donViTinh;
        this.soLuongTon = soLuongTon;
        this.mucToiThieu = mucToiThieu;
        this.donGiaNhap = donGiaNhap;
        this.maNCC = maNCC;
    }

    public String getMaNL() {
        return maNL;
    }

    public void setMaNL(String maNL) {
        this.maNL = maNL;
    }

    public String getTenNL() {
        return tenNL;
    }

    public void setTenNL(String tenNL) {
        this.tenNL = tenNL;
    }

    public String getDonViTinh() {
        return donViTinh;
    }

    public void setDonViTinh(String donViTinh) {
        this.donViTinh = donViTinh;
    }

    public double getSoLuongTon() {
        return soLuongTon;
    }

    public void setSoLuongTon(double soLuongTon) {
        this.soLuongTon = Math.max(0, soLuongTon);
    }

    public double getMucToiThieu() {
        return mucToiThieu;
    }

    public void setMucToiThieu(double mucToiThieu) {
        this.mucToiThieu = Math.max(0, mucToiThieu);
    }

    public double getDonGiaNhap() {
        return donGiaNhap;
    }

    public void setDonGiaNhap(double donGiaNhap) {
        this.donGiaNhap = Math.max(0, donGiaNhap);
    }

    public String getMaNCC() {
        return maNCC;
    }

    public void setMaNCC(String maNCC) {
        this.maNCC = maNCC;
    }

    public boolean isHetHang() {
        return soLuongTon <= 0;
    }

    public boolean isCanhBaoHetHang() {
        return soLuongTon <= mucToiThieu;
    }

    public String getTrangThaiTonKho() {
        if (isHetHang()) {
            return "HET HANG";
        }
        if (isCanhBaoHetHang()) {
            return "SAP HET HANG";
        }
        return "CON HANG";
    }

    public double tinhGiaTriTonKho() {
        return soLuongTon * donGiaNhap;
    }

    public void nhapThem(double soLuong) {
        if (soLuong > 0) {
            this.soLuongTon += soLuong;
        }
    }

    public boolean xuatKho(double soLuong) {
        if (soLuong <= 0 || soLuong > this.soLuongTon) {
            return false;
        }
        this.soLuongTon -= soLuong;
        return true;
    }

    public void nhap(Scanner sc) {
        System.out.print("Nhap ten nguyen lieu: ");
        this.tenNL = sc.nextLine().trim();
        System.out.print("Nhap don vi tinh (kg/lit/hop/goi): ");
        this.donViTinh = sc.nextLine().trim();
        System.out.print("Nhap so luong ton: ");
        this.soLuongTon = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Nhap muc ton toi thieu (canh bao): ");
        this.mucToiThieu = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Nhap don gia nhap: ");
        this.donGiaNhap = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Nhap ma nha cung cap (chi nhap so, vd 01): ");
        this.maNCC = NhaCungCap.chuanHoaMaNCC(sc.nextLine());
    }

    public void xuat() {
        System.out.printf("| %-6s | %-22s | %-8s | %10.2f | %10.2f | %12.0f | %-8s | %-13s |%n",
                maNL, tenNL, donViTinh, soLuongTon, mucToiThieu, donGiaNhap, maNCC, getTrangThaiTonKho());
    }

    public String toFileString() {
        return String.format(Locale.US, "%s;%s;%s;%.2f;%.2f;%.0f;%s",
                maNL, tenNL, donViTinh, soLuongTon, mucToiThieu, donGiaNhap, maNCC);
    }

    public static NguyenLieu fromFileString(String line) {
        String[] parts = line.split(";");
        if (parts.length < 7) {
            return null;
        }
        try {
            String maNL = parts[0].trim();
            String tenNL = parts[1].trim();
            String donViTinh = parts[2].trim();
            double soLuongTon = Double.parseDouble(parts[3].trim().replace(",", "."));
            double mucToiThieu = Double.parseDouble(parts[4].trim().replace(",", "."));
            double donGiaNhap = Double.parseDouble(parts[5].trim().replace(",", "."));
            String maNCC = parts[6].trim();
            return new NguyenLieu(maNL, tenNL, donViTinh, soLuongTon, mucToiThieu, donGiaNhap, maNCC);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("NguyenLieu[maNL=%s, tenNL=%s, ton=%.2f %s, trangThai=%s]",
                maNL, tenNL, soLuongTon, donViTinh, getTrangThaiTonKho());
    }
}

