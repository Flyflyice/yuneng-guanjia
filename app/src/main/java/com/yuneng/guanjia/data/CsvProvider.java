package com.yuneng.guanjia.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 只读文件提供者：把导出 CSV / 导入模板以 content:// 形式分享给微信等应用。
 * 文件根目录固定为 getExternalFilesDir(null)，并校验路径不能越界。
 */
public class CsvProvider extends ContentProvider {
    public static final String AUTHORITY = "com.yuneng.guanjia.files";

    @Override
    public boolean onCreate() {
        return true;
    }

    private File root() {
        File dir = getContext() == null ? null : getContext().getExternalFilesDir(null);
        if (dir == null && getContext() != null) dir = getContext().getFilesDir();
        return dir;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File base = root();
        if (base == null) throw new FileNotFoundException("no root");
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (path.startsWith("/")) path = path.substring(1);
        if (path.isEmpty()) throw new FileNotFoundException("empty path");
        File file = new File(base, path);
        try {
            String canonicalBase = base.getCanonicalPath();
            String canonicalFile = file.getCanonicalPath();
            if (!canonicalFile.startsWith(canonicalBase + File.separator)) {
                throw new FileNotFoundException("out of bounds");
            }
        } catch (IOException e) {
            throw new FileNotFoundException("bad path");
        }
        if (!file.isFile()) throw new FileNotFoundException("not found");
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        return "text/csv";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
