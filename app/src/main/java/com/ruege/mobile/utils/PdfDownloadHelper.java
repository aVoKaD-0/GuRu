package com.ruege.mobile.utils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import timber.log.Timber;

public class PdfDownloadHelper {

    private final Activity context;
    private File pendingSourceFile;
    private String pendingFileName;
    private String pendingDescription;
    public static final int REQUEST_WRITE_STORAGE = 1001;

    public PdfDownloadHelper(Activity context) {
        this.context = context;
    }

    public void downloadPdfToDownloads(File sourceFile, String fileName, String description) {
        if (sourceFile == null || !sourceFile.exists() || fileName == null || fileName.isEmpty()) {
            Timber.e("downloadPdfToDownloads: sourceFile is null, не существует или fileName пуст.");
            return;
        }

        if (hasWritePermission()) {
            copyPdfToDownloads(sourceFile, fileName, description);
        } else {
            this.pendingSourceFile = sourceFile;
            this.pendingFileName = fileName;
            this.pendingDescription = description;
            requestWritePermission();
        }
    }
    
    public boolean hasWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestWritePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(context, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
    }
    
    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingSourceFile != null && pendingFileName != null) {
                    copyPdfToDownloads(pendingSourceFile, pendingFileName, pendingDescription);
                }
            } else {
                Toast.makeText(context, "Разрешение на запись не предоставлено.", Toast.LENGTH_SHORT).show();
            }
            pendingSourceFile = null;
            pendingFileName = null;
            pendingDescription = null;
        }
    }

    public void copyPdfToDownloads(File sourceFile, String fileName, String description) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            saveUsingMediaStore(sourceFile, fileName, description);
        } else {
            saveUsingDirectFile(sourceFile, fileName, description);
        }
    }

    @android.annotation.TargetApi(android.os.Build.VERSION_CODES.Q)
    private void saveUsingMediaStore(File sourceFile, String fileName, String description) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        contentValues.put(MediaStore.Downloads.TITLE, fileName);
        contentValues.put(MediaStore.Downloads.IS_PENDING, 1);

        android.content.ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL), contentValues);

        if (uri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(uri);
                 FileInputStream inputStream = new FileInputStream(sourceFile)) {
                if (outputStream == null) {
                    Timber.e("Не удалось открыть выходной поток для URI: " + uri);
                    Toast.makeText(context, "Ошибка сохранения файла (output stream null)", Toast.LENGTH_LONG).show();
                    return;
                }
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                contentValues.clear();
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(uri, contentValues, null, null);
                Timber.d("Файл успешно сохранен через MediaStore: " + uri);
                Toast.makeText(context, description != null && !description.isEmpty() ? description + " сохранен в Загрузки" : "Файл сохранен в Загрузки", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Timber.e(e, "Ошибка при сохранении файла через MediaStore");
                Toast.makeText(context, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
                try {
                    resolver.delete(uri, null, null);
                } catch (Exception ex) {
                    Timber.e(ex, "Ошибка при удалении частично созданного файла");
                }
            }
        } else {
            Timber.e("Не удалось получить URI для сохранения файла через MediaStore");
            Toast.makeText(context, "Ошибка: Не удалось получить URI для сохранения", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUsingDirectFile(File sourceFile, String fileName, String description) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            if (!downloadsDir.mkdirs()) {
                Timber.e("Не удалось создать директорию Загрузки");
                Toast.makeText(context, "Ошибка: не удалось создать директорию Загрузки", Toast.LENGTH_LONG).show();
                return;
            }
        }
        File destinationFile = new File(downloadsDir, fileName);
        int count = 0;
        String tempFileName = fileName;
        String extension = "";
        String baseName = tempFileName;
        int dotIndex = tempFileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < tempFileName.length() - 1) {
            baseName = tempFileName.substring(0, dotIndex);
            extension = tempFileName.substring(dotIndex);
        }
        while (destinationFile.exists()) {
            count++;
            tempFileName = baseName + " (" + count + ")" + extension;
            destinationFile = new File(downloadsDir, tempFileName);
        }

        try (FileInputStream inputStream = new FileInputStream(sourceFile);
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            Timber.d("Файл успешно скопирован в загрузки: " + destinationFile.getAbsolutePath());
            Toast.makeText(context, description != null && !description.isEmpty() ? description + " сохранен в Загрузки" : "Файл сохранен в Загрузки", Toast.LENGTH_SHORT).show();

            MediaScannerConnection.scanFile(
                context,
                new String[]{destinationFile.getAbsolutePath()},
                new String[]{"application/pdf"},
                (path, uri) -> Timber.d("Файл отсканирован и доступен в системе: " + (uri != null ? uri.toString() : "uri is null"))
            );
        } catch (Exception e) {
            Timber.e(e, "Ошибка при сохранении файла напрямую");
            Toast.makeText(context, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
} 