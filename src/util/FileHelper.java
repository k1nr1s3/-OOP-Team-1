package util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileHelper {

    public static List<String> docFile(String duongDan) {
        List<String> danhSachDong = new ArrayList<>();
        File file = new File(duongDan);
        if (!file.exists()) {
            return danhSachDong;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    danhSachDong.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Loi khi doc file (" + duongDan + "): " + e.getMessage());
        }
        return danhSachDong;
    }

    public static boolean ghiFile(String duongDan, List<String> danhSachDong) {
        File file = new File(duongDan);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
            for (String dong : danhSachDong) {
                bw.write(dong);
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            System.out.println("Loi khi ghi file (" + duongDan + "): " + e.getMessage());
            return false;
        }
    }

    public static boolean ghiThemFile(String duongDan, String dong) {
        File file = new File(duongDan);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            bw.write(dong);
            bw.newLine();
            return true;
        } catch (IOException e) {
            System.out.println("Loi khi ghi them vao file (" + duongDan + "): " + e.getMessage());
            return false;
        }
    }

    public static boolean kiemTraFileTonTai(String duongDan) {
        return new File(duongDan).exists();
    }
}
