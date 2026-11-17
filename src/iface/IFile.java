package iface;

import java.io.IOException;

// lớp danh sách nào đọc / ghi được file text thì implements interface này
public interface IFile {

    // đọc file, thay thế dữ liệu hiện tại. Trả về số phần tử đọc được (file k tồn tại -> 0)
    int docFile(String duongDan) throws IOException;

    // ghi toàn bộ danh sách ra file (ghi đè)
    void ghiFile(String duongDan) throws IOException;
}
