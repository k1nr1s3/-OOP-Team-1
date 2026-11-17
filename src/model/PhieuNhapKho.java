package model;

import java.util.Scanner;

public class PhieuNhapKho {
    private String maPhieu;
    private String ngayNhap;
    private String maNV;
    private String maNCC;
    private String maNL;
    private double soLuongNhap;
    private double donGiaNhap;

    public PhieuNhapKho() {
    }

    public PhieuNhapKho(String maPhieu, String ngayNhap, String maNV,
                        String maNCC, String maNL, double soLuongNhap, double donGiaNhap) {
        this.maPhieu = maPhieu;
        this.ngayNhap = ngayNhap;
        this.maNV = maNV;
        this.maNCC = maNCC;
        this.maNL = maNL;
        this.soLuongNhap = soLuongNhap;
        this.donGiaNhap = donGiaNhap;
    }

    public String getMaPhieu() {
        return maPhieu;
    }

    public void setMaPhieu(String maPhieu) {
        this.maPhieu = maPhieu;
    }

    public String getNgayNhap() {
        return ngayNhap;
    }

    public void setNgayNhap(String ngayNhap) {
        this.ngayNhap = ngayNhap;
    }

    public String getMaNV() {
        return maNV;
    }

    public void setMaNV(String maNV) {
        this.maNV = maNV;
    }

    public String getMaNCC() {
        return maNCC;
    }

    public void setMaNCC(String maNCC) {
        this.maNCC = maNCC;
    }

    public String getMaNL() {
        return maNL;
    }

    public void setMaNL(String maNL) {
        this.maNL = maNL;
    }

    public double getSoLuongNhap() {
        return soLuongNhap;
    }

    public void setSoLuongNhap(double soLuongNhap) {
        this.soLuongNhap = Math.max(0, soLuongNhap);
    }

    public double getDonGiaNhap() {
        return donGiaNhap;
    }

    public void setDonGiaNhap(double donGiaNhap) {
        this.donGiaNhap = Math.max(0, donGiaNhap);
    }

    public double tinhThanhTien() {
        return soLuongNhap * donGiaNhap;
    }

    public void nhap(Scanner sc) {
        System.out.print("Nhap ngay nhap (dd/MM/yyyy): ");
        this.ngayNhap = sc.nextLine().trim();
        System.out.print("Nhap ma nhan vien phu trach: ");
        this.maNV = sc.nextLine().trim();
        System.out.print("Nhap ma nha cung cap (chi nhap so, vd 01): ");
        this.maNCC = NhaCungCap.chuanHoaMaNCC(sc.nextLine());
        System.out.print("Nhap ma nguyen lieu: ");
        this.maNL = sc.nextLine().trim();
        System.out.print("Nhap so luong nhap: ");
        this.soLuongNhap = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Nhap don gia nhap: ");
        this.donGiaNhap = Double.parseDouble(sc.nextLine().trim());
    }

    public void xuat() {
        System.out.printf("| %-8s | %-12s | %-8s | %-8s | %-8s | %10.2f | %12.0f | %14.0f |%n",
                maPhieu, ngayNhap, maNV, maNCC, maNL, soLuongNhap, donGiaNhap, tinhThanhTien());
    }

    public String toFileString() {
        return String.format("%s;%s;%s;%s;%s;%.2f;%.0f",
                maPhieu, ngayNhap, maNV, maNCC, maNL, soLuongNhap, donGiaNhap);
    }

    public static PhieuNhapKho fromFileString(String line) {
        String[] parts = line.split(";");
        if (parts.length < 7) {
            return null;
        }
        try {
            return new PhieuNhapKho(
                    parts[0].trim(),
                    parts[1].trim(),
                    parts[2].trim(),
                    parts[3].trim(),
                    parts[4].trim(),
                    Double.parseDouble(parts[5].trim().replace(",", ".")),
                    Double.parseDouble(parts[6].trim().replace(",", "."))
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("PhieuNhapKho[maPhieu=%s, ngay=%s, maNL=%s, sl=%.2f, thanhTien=%.0f]",
                maPhieu, ngayNhap, maNL, soLuongNhap, tinhThanhTien());
    }
}

