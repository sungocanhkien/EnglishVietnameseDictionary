package anhkien.myproject.englishvietnamesedictionary;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static String DB_PATH = "";
    public static final String DATABASE_NAME = "mydatabase.db"; // Tên file database trong assets
    public static final int DATABASE_VERSION = 1; // Tăng version nếu bạn thay đổi schema VÀ file .db trong assets

    private final Context myContext;

    // Tên bảng - PHẢI KHỚP VỚI TÊN BẠN ĐẶT KHI IMPORT CSV
    public static final String TABLE_WORDS = "databaseme";

    // Tên các cột trong bảng Words - PHẢI KHỚP VỚI TÊN CỘT TRONG FILE CSV/EXCEL CỦA BẠN
    // Nếu khi import vào DB Browser, tên cột bị thay đổi (ví dụ: có khoảng trắng bị thay bằng '_')
    // thì bạn cần dùng tên cột CHÍNH XÁC trong file .db
    public static final String COLUMN_ID = "ID"; // Từ file Excel của bạn
    public static final String COLUMN_WORD = "word";
    public static final String COLUMN_TRANSLATION = "translation";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_EXAMPLE = "example";
    public static final String COLUMN_PRONUNCIATION = "pronunciation";
    public static final String COLUMN_LANGUAGE = "language"; // ví dụ 'en' hoặc 'vi'
    public static final String COLUMN_IS_FAVORITE = "isFavorite";

    // Câu lệnh tạo bảng (CHỦ YẾU ĐỂ THAM KHẢO, VÌ DB BROWSER SẼ TẠO BẢNG KHI IMPORT CSV)
    // Đảm bảo khi import, cột ID được đặt là PRIMARY KEY.
    private static final String TABLE_CREATE_WORDS_REFERENCE =
            "CREATE TABLE " + TABLE_WORDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY, " + // ID từ CSV, không cần AUTOINCREMENT nếu đã có sẵn và duy nhất
                    COLUMN_WORD + " TEXT NOT NULL, " +
                    COLUMN_TRANSLATION + " TEXT NOT NULL, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_EXAMPLE + " TEXT, " +
                    COLUMN_PRONUNCIATION + " TEXT, " +
                    COLUMN_LANGUAGE + " TEXT, " +
                    COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.myContext = context;

        if (android.os.Build.VERSION.SDK_INT >= 17) {
            DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
        } else {
            DB_PATH = "/data/data/" + context.getPackageName() + "/databases/";
        }
        Log.d(TAG, "DB_PATH: " + DB_PATH);

        File dbDir = new File(DB_PATH);
        if (!dbDir.exists()){
            boolean dirCreated = dbDir.mkdirs();
            Log.d(TAG, "Database directory created: " + dirCreated);
        }

        try {
            createDataBase();
        } catch (IOException e) {
            Log.e(TAG, "Error creating database", e);
            throw new RuntimeException("Error creating database", e);
        }
    }

    public void createDataBase() throws IOException {
        boolean dbExist = checkDataBase();
        if (dbExist) {
            Log.d(TAG, "Database already exists.");
            // Kiểm tra phiên bản ở đây nếu cần
            // SQLiteDatabase db = SQLiteDatabase.openDatabase(DB_PATH + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
            // if (db.getVersion() < DATABASE_VERSION) {
            //     Log.d(TAG, "Database version mismatch. Needs upgrade. Deleting old and copying new.");
            //     File dbFile = new File(DB_PATH + DATABASE_NAME);
            //     dbFile.delete();
            //     this.getReadableDatabase().close(); // Tạo lại file trống
            //     copyDataBase();
            // }
            // db.close();
        } else {
            this.getReadableDatabase(); // Gọi để Android tạo file DB trống và thư mục
            this.close(); // Đóng file trống
            try {
                copyDataBase();
                Log.d(TAG, "Database copied successfully from assets.");
            } catch (IOException e) {
                Log.e(TAG, "Error copying database from assets", e);
                throw new Error("Error copying database");
            }
        }
    }

    private boolean checkDataBase() {
        File dbFile = new File(DB_PATH + DATABASE_NAME);
        return dbFile.exists();
    }

    private void copyDataBase() throws IOException {
        InputStream myInput = myContext.getAssets().open(DATABASE_NAME);
        String outFileName = DB_PATH + DATABASE_NAME;
        OutputStream myOutput = new FileOutputStream(outFileName);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        myOutput.flush();
        myOutput.close();
        myInput.close();
        Log.d(TAG, "Database copied to: " + outFileName);

        // Set version cho database vừa copy (QUAN TRỌNG cho onUpgrade sau này)
        SQLiteDatabase copiedDb = SQLiteDatabase.openDatabase(outFileName, null, SQLiteDatabase.OPEN_READWRITE);
        copiedDb.setVersion(DATABASE_VERSION);
        copiedDb.close();
    }

    @Override
    public synchronized void close() {
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Phương thức này sẽ không được gọi nhiều nếu copyDataBase() thành công
        // vì database đã tồn tại với schema từ file assets.
        // Nó chỉ chạy nếu copyDataBase() thất bại và getReadableDatabase()
        // phải tạo một DB mới hoàn toàn.
        Log.d(TAG, "onCreate called - This should ideally not happen if assets copy is successful.");
        // db.execSQL(TABLE_CREATE_WORDS_REFERENCE); // Có thể giữ lại làm dự phòng
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        // Nếu phiên bản database trong assets mới hơn phiên bản hiện tại trên thiết bị
        if (newVersion > oldVersion) {
            try {
                Log.d(TAG, "Newer database version found in assets. Deleting old and copying new.");
                // Xóa file DB cũ trước khi copy file mới từ assets
                File dbFile = new File(DB_PATH + DATABASE_NAME);
                if (dbFile.exists()) {
                    boolean deleted = dbFile.delete();
                    Log.d(TAG, "Old database deleted: " + deleted);
                }
                // Cần gọi getReadableDatabase().close() để SQLiteOpenHelper biết là cần tạo lại
                // Tuy nhiên, vì chúng ta sẽ copy trực tiếp, chỉ cần đảm bảo file không bị khóa.
                // this.getReadableDatabase().close(); // Có thể gây lỗi nếu db đang được tham chiếu
                copyDataBase(); // Copy lại file mới từ assets
            } catch (IOException e) {
                Log.e(TAG, "Error upgrading database by copying from assets", e);
                // Xử lý lỗi, ví dụ thông báo cho người dùng
            }
        }
    }

}
