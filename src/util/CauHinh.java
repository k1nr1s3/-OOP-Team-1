package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

// cấu hình chung: đường dẫn file dữ liệu + các hàm nhập từ bàn phím (nhập sai thì bắt nhập lại)
public class CauHinh {

    public static final Scanner SCANNER = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);

    public static final String DUONG_DAN_SAN_PHAM = "data/sanpham.txt";
    public static final String DUONG_DAN_NHAN_VIEN = "data/nhanvien.txt";
    public static final String DUONG_DAN_KHACH_HANG = "data/khachhang.txt";
    public static final String DUONG_DAN_KHUYEN_MAI = "data/khuyenmai.txt";
    public static final String DUONG_DAN_HOA_DON = "data/hoadon.txt";
    public static final String DUONG_DAN_NGUYEN_LIEU = "data/nguyenlieu.txt";

    public static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String nhapChuoi(String thongBao) {
        System.out.print(thongBao);
        if (!SCANNER.hasNextLine()) {   // hết dữ liệu nhập (Ctrl+D / Ctrl+Z) thì thoát hẳn, tránh lặp vô hạn
            System.out.println();
            System.exit(0);
        }
        return SCANNER.nextLine().trim();
    }

    public static int nhapSoNguyen(String thongBao) {
        while (true) {
            try {
                return Integer.parseInt(nhapChuoi(thongBao));
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập một số nguyên!");
            }
        }
    }

    public static double nhapSoThuc(String thongBao) {
        while (true) {
            try {
                double value = Double.parseDouble(nhapChuoi(thongBao).replace(",", "."));
                if (!Double.isFinite(value)) {
                    throw new NumberFormatException("not finite");
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập một số!");
            }
        }
    }

    // ngày dạng dd/MM/yyyy, bỏ trống thì trả về macDinh
    public static LocalDate nhapNgay(String thongBao, LocalDate macDinh) {
        while (true) {
            String s = nhapChuoi(thongBao);
            if (s.isEmpty() && macDinh != null) {
                return macDinh;
            }
            try {
                return LocalDate.parse(s, DINH_DANG_NGAY);
            } catch (DateTimeParseException e) {
                System.out.println("Ngày k hợp lệ, nhập dạng dd/MM/yyyy (ví dụ 25/10/2026)!");
            }
        }
    }

    public static boolean xacNhan(String thongBao) {
        String s = nhapChuoi(thongBao + " (c/k): ");
        return s.equalsIgnoreCase("c") || s.equalsIgnoreCase("y");
    }

    // in đường kẻ tiêu đề cho menu
    public static void tieuDe(String ten) {
        System.out.println();
        System.out.println("================ " + ten + " ================");
    }
}
