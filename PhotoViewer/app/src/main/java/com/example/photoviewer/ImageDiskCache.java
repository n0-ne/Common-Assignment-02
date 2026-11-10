package com.example.photoviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;

public class ImageDiskCache {

    private static final long MAX_CACHE_SIZE_BYTES =
            30L * 1024L * 1024L;

    private final File cacheDirectory;

    public ImageDiskCache(Context context) {
        cacheDirectory = new File(
                context.getCacheDir(),
                "detection_images"
        );
    }

    public synchronized Bitmap get(String imageUrl) {
        if (!cacheDirectory.exists()) {
            return null;
        }

        File cacheFile = getCacheFile(imageUrl);

        if (!cacheFile.isFile()) {
            return null;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(
                cacheFile.getAbsolutePath()
        );

        if (bitmap == null) {
            cacheFile.delete();
            return null;
        }

        // 캐시 적중 시 최근 사용 시각을 갱신한다.
        cacheFile.setLastModified(
                System.currentTimeMillis()
        );

        return bitmap;
    }

    public synchronized void put(
            String imageUrl,
            byte[] imageBytes
    ) throws IOException {

        if (imageBytes == null || imageBytes.length == 0) {
            return;
        }

        if (!cacheDirectory.exists()
                && !cacheDirectory.mkdirs()) {

            throw new IOException(
                    "이미지 캐시 폴더를 만들 수 없습니다."
            );
        }

        File cacheFile = getCacheFile(imageUrl);

        try (
                FileOutputStream output =
                        new FileOutputStream(cacheFile)
        ) {
            output.write(imageBytes);
        }

        cacheFile.setLastModified(
                System.currentTimeMillis()
        );

        trimToSize();
    }

    public synchronized void clear() {
        File[] files = cacheDirectory.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                file.delete();
            }
        }
    }

    public synchronized int getFileCount() {
        File[] files = getCacheFiles();
        return files.length;
    }

    public synchronized long getSizeBytes() {
        long totalSize = 0L;

        for (File file : getCacheFiles()) {
            totalSize += file.length();
        }

        return totalSize;
    }

    private void trimToSize() {
        File[] files = getCacheFiles();

        Arrays.sort(
                files,
                Comparator.comparingLong(
                        File::lastModified
                )
        );

        long totalSize = 0L;

        for (File file : files) {
            totalSize += file.length();
        }

        for (File file : files) {
            if (totalSize <= MAX_CACHE_SIZE_BYTES) {
                break;
            }

            long fileSize = file.length();

            if (file.delete()) {
                totalSize -= fileSize;
            }
        }
    }

    private File[] getCacheFiles() {
        File[] files = cacheDirectory.listFiles(
                File::isFile
        );

        return files == null ? new File[0] : files;
    }

    private File getCacheFile(String imageUrl) {
        return new File(
                cacheDirectory,
                sha256(imageUrl) + ".cache"
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte item : hash) {
                result.append(
                        String.format("%02x", item & 0xff)
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256을 사용할 수 없습니다.",
                    e
            );
        }
    }
}