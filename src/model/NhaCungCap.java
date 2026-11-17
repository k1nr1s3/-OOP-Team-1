package model;

import java.util.Scanner;

public class NhaCungCap {
    private String maNCC;
    private String tenNCC;
    private String soDienThoai;
    private String diaChi;

    public NhaCungCap() {
    }

    public NhaCungCap(String maNCC, String tenNCC, String soDienThoai, String diaChi) {
        this.maNCC = maNCC;
        this.tenNCC = tenNCC;
        this.soDienThoai = soDienThoai;
        this.diaChi = diaChi;
    }

    public String getMaNCC() {
        return maNCC;
    }

    public void setMaNCC(String maNCC) {
        this.maNCC = maNCC;
    }

    public String getTenNCC() {
        return tenNCC;
    }

    public void setTenNCC(String tenNCC) {
        this.tenNCC = tenNCC;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public void nhap(Scanner sc) {
        System.out.print("Nhap ma nha cung cap: ");
        this.maNCC = sc.nextLine().trim();
        System.out.print("Nhap ten nha cung cap: ");
        this.tenNCC = sc.nextLine().trim();
        System.out.print("Nhap so dien thoai: ");
        this.soDienThoai = sc.nextLine().trim();
        System.out.print("Nhap dia chi: ");
        this.diaChi = sc.nextLine().trim();
    }

    public void xuat() {
        System.out.printf("| %-8s | %-25s | %-12s | %-30s |%n",
                maNCC, tenNCC, soDienThoai, diaChi);
    }

    public String toFileString() {
        return String.join(";", maNCC, tenNCC, soDienThoai, diaChi);
    }

    public static NhaCungCap fromFileString(String line) {
        String[] parts = line.split(";");
        if (parts.length < 4) {
            return null;
        }
        return new NhaCungCap(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim()
        );
    }

    @Override
    public String toString() {
        return String.format("NhaCungCap[maNCC=%s, tenNCC=%s, sdt=%s, diaChi=%s]",
                maNCC, tenNCC, soDienThoai, diaChi);
    }
}

