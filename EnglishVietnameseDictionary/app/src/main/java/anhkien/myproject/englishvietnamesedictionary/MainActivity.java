package anhkien.myproject.englishvietnamesedictionary;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "DatabaseTest";
    private TextView tvTestResult; // TextView để hiển thị thông báo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Hoặc layout của TestDatabaseActivity
        // Đảm bảo layout này có một TextView với id là tv_test_result

        tvTestResult = findViewById(R.id.tv_test_result); // GIẢ SỬ BẠN CÓ TEXTVIEW NÀY TRONG LAYOUT

        Log.d(TAG, "onCreate: Starting database test...");
        tvTestResult.setText("Đang kiểm tra database...\n");

        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            // Việc khởi tạo dbHelper sẽ tự động gọi createDataBase()
            // và cố gắng copy file từ assets nếu cần.

            // Nếu không có lỗi nào xảy ra ở trên, nghĩa là quá trình copy (nếu có) đã thành công.
            // Bây giờ thử mở database để đọc.
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            if (db != null && db.isOpen()) {
                Log.i(TAG, "Database opened successfully!");
                tvTestResult.append("Database đã được mở thành công!\n");
                tvTestResult.append("Đường dẫn database: " + db.getPath() + "\n");
                tvTestResult.append("Phiên bản database: " + db.getVersion() + "\n");

                // (TÙY CHỌN) Thử truy vấn đơn giản để đếm số dòng trong bảng "words"
                android.database.Cursor cursor = null;
                try {
                    cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_WORDS, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int count = cursor.getInt(0);
                        Log.i(TAG, "Số lượng dòng trong bảng '" + DatabaseHelper.TABLE_WORDS + "': " + count);
                        tvTestResult.append("Số lượng dòng trong bảng '" + DatabaseHelper.TABLE_WORDS + "': " + count + "\n");
                        if (count > 0) {
                            tvTestResult.append("=> KIỂM TRA THÀNH CÔNG! Database có vẻ đã được copy và chứa dữ liệu.\n");
                        } else {
                            tvTestResult.append("=> CẢNH BÁO: Bảng '" + DatabaseHelper.TABLE_WORDS + "' trống! Kiểm tra lại file .db trong assets và quá trình import CSV.\n");
                        }
                    } else {
                        Log.e(TAG, "Không thể đếm số dòng hoặc bảng không tồn tại.");
                        tvTestResult.append("=> LỖI: Không thể đếm số dòng hoặc bảng '" + DatabaseHelper.TABLE_WORDS + "' không tồn tại.\n");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi khi truy vấn đếm dòng: ", e);
                    tvTestResult.append("=> LỖI KHI TRUY VẤN: " + e.getMessage() + "\n");
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

                db.close(); // Đóng database sau khi kiểm tra
            } else {
                Log.e(TAG, "Không thể mở database!");
                tvTestResult.append("=> LỖI: Không thể mở database sau khi khởi tạo Helper!\n");
            }

        } catch (Exception e) {
            Log.e(TAG, "Lỗi nghiêm trọng khi khởi tạo hoặc mở DatabaseHelper: ", e);
            tvTestResult.append("=> LỖI NGHIÊM TRỌNG: " + e.getMessage() + "\n");
            // In chi tiết lỗi ra Logcat để dễ debug
            e.printStackTrace();
        }
    }
}