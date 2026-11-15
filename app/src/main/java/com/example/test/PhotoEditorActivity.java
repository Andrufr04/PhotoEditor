package com.example.test;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PhotoEditorActivity extends AppCompatActivity {

    ImageView photoView;

    Bitmap originalBitmap;
    Bitmap currentBitmap;

    ActivityResultLauncher<String> photoPicker;

    // Змінні для малювання
    boolean isDrawingMode = false;
    float lastX = -1, lastY = -1;
    int[] brushColors = {
            Color.parseColor("#E63946"), // Червоний
            Color.parseColor("#2A9D8F"), // Зелений
            Color.parseColor("#1D3557"), // Синій
            Color.WHITE,                     // Білий
            Color.BLACK                      // Чорний
    };
    int currentColorIndex = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_editor);

        photoView = findViewById(R.id.photoView);

        Button selectButton = findViewById(R.id.selectPhotoButton);

        // Кнопки фільтрів
        Button originalButton = findViewById(R.id.originalButton);
        Button grayButton = findViewById(R.id.grayButton);
        Button sepiaButton = findViewById(R.id.sepiaButton);
        Button brightButton = findViewById(R.id.brightButton);
        Button coolButton = findViewById(R.id.coolButton);
        Button invertButton = findViewById(R.id.invertButton);
        Button warmButton = findViewById(R.id.warmButton);
        Button contrastButton = findViewById(R.id.contrastButton);

        // Кнопки інструментів та малювання
        Button drawModeButton = findViewById(R.id.drawModeButton);
        Button brushColorButton = findViewById(R.id.brushColorButton);
        Button circleButton = findViewById(R.id.circleButton);
        Button rectButton = findViewById(R.id.rectButton);
        Button splitButton = findViewById(R.id.splitButton);
        Button rotateButton = findViewById(R.id.rotateButton);
        Button watermarkButton = findViewById(R.id.watermarkButton);

        // Кнопки експорту
        Button saveButton = findViewById(R.id.saveButton);
        Button shareButton = findViewById(R.id.shareButton);

        photoPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        loadPhoto(uri);
                    }
                }
        );

        selectButton.setOnClickListener(v -> photoPicker.launch("image/*"));

        // Обробка фільтрів
        originalButton.setOnClickListener(v -> applyFilter(0));
        grayButton.setOnClickListener(v -> applyFilter(1));
        sepiaButton.setOnClickListener(v -> applyFilter(2));
        brightButton.setOnClickListener(v -> applyFilter(3));
        coolButton.setOnClickListener(v -> applyFilter(4));
        invertButton.setOnClickListener(v -> applyFilter(5));
        warmButton.setOnClickListener(v -> applyFilter(6));
        contrastButton.setOnClickListener(v -> applyFilter(7));

        // Перемикач режиму малювання
        drawModeButton.setOnClickListener(v -> {
            if (currentBitmap == null) {
                Toast.makeText(this, "Спочатку завантажте фото", Toast.LENGTH_SHORT).show();
                return;
            }
            isDrawingMode = !isDrawingMode;
            if (isDrawingMode) {
                drawModeButton.setText("Пензель: УВІМК");
                drawModeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E76F51")));
                Toast.makeText(this, "Малюйте пальцем по фото", Toast.LENGTH_SHORT).show();
            } else {
                drawModeButton.setText("Пензель: ВИМК");
                drawModeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#52796F")));
            }
        });

        // Перемикач кольору пензля
        brushColorButton.setOnClickListener(v -> {
            currentColorIndex = (currentColorIndex + 1) % brushColors.length;
            int selectedColor = brushColors[currentColorIndex];
            brushColorButton.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
            // Робимо текст кнопки чорним, якщо колір пензля білий
            brushColorButton.setTextColor(selectedColor == Color.WHITE ? Color.BLACK : Color.WHITE);
        });

        // Обробка дотиків до фото для малювання
        photoView.setOnTouchListener((v, event) -> {
            if (!isDrawingMode || currentBitmap == null) {
                return false;
            }

            // Перетворюємо координати екрана у реальні пікселі зображення
            float[] touchPoint = new float[]{event.getX(), event.getY()};
            Matrix inverseMatrix = new Matrix();
            photoView.getImageMatrix().invert(inverseMatrix);
            inverseMatrix.mapPoints(touchPoint);

            float x = touchPoint[0];
            float y = touchPoint[1];

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = x;
                    lastY = y;
                    // Переконуємось, що currentBitmap можна редагувати
                    if (!currentBitmap.isMutable()) {
                        currentBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    drawPointOrLine(x, y, x, y);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (lastX != -1 && lastY != -1) {
                        drawPointOrLine(lastX, lastY, x, y);
                    }
                    lastX = x;
                    lastY = y;
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    lastX = -1;
                    lastY = -1;
                    break;
            }
            return true;
        });

        // Обробка інших інструментів
        circleButton.setOnClickListener(v -> drawShape(true));
        rectButton.setOnClickListener(v -> drawShape(false));
        splitButton.setOnClickListener(v -> splitIntoFour());
        rotateButton.setOnClickListener(v -> rotatePhoto());
        watermarkButton.setOnClickListener(v -> addWatermark());

        saveButton.setOnClickListener(v -> saveBitmapToGallery(currentBitmap, "EditedPhoto_" + System.currentTimeMillis() + ".jpg", true));
        shareButton.setOnClickListener(v -> sharePhoto());
    }

    // Метод для малювання плавної лінії на currentBitmap
    private void drawPointOrLine(float startX, float startY, float endX, float endY) {
        Canvas canvas = new Canvas(currentBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(brushColors[currentColorIndex]);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND); // Заокруглені краї лінії
        paint.setStrokeJoin(Paint.Join.ROUND);

        // Адаптивна товщина пензля залежно від розміру фото
        float brushSize = Math.min(currentBitmap.getWidth(), currentBitmap.getHeight()) * 0.015f;
        paint.setStrokeWidth(Math.max(brushSize, 8f));

        if (startX == endX && startY == endY) {
            canvas.drawPoint(startX, startY, paint);
        } else {
            canvas.drawLine(startX, startY, endX, endY, paint);
        }

        photoView.setImageBitmap(currentBitmap);
    }

    private void loadPhoto(Uri uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            originalBitmap = BitmapFactory.decodeStream(stream);
            if (stream != null) {
                stream.close();
            }
            applyFilter(0);
        } catch (Exception e) {
            Toast.makeText(this, "Помилка завантаження", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyFilter(int filter) {
        if (originalBitmap == null) {
            Toast.makeText(this, "Спочатку завантажте фото", Toast.LENGTH_SHORT).show();
            return;
        }

        ColorMatrix matrix = new ColorMatrix();

        switch (filter) {
            case 0: // Original
                matrix.setSaturation(1);
                break;
            case 1: // BW
                matrix.setSaturation(0);
                break;
            case 2: // Sepia
                matrix.set(new float[]{
                        0.393f, 0.769f, 0.189f, 0, 0,
                        0.349f, 0.686f, 0.168f, 0, 0,
                        0.272f, 0.534f, 0.131f, 0, 0,
                        0, 0, 0, 1, 0
                });
                break;
            case 3: // Bright
                matrix.set(new float[]{
                        1, 0, 0, 0, 40,
                        0, 1, 0, 0, 40,
                        0, 0, 1, 0, 40,
                        0, 0, 0, 1, 0
                });
                break;
            case 4: // Cool
                matrix.set(new float[]{
                        0.9f, 0, 0, 0, 0,
                        0, 1, 0, 0, 0,
                        0, 0, 1.2f, 0, 15,
                        0, 0, 0, 1, 0
                });
                break;
            case 5: // Invert
                matrix.set(new float[]{
                        -1, 0, 0, 0, 255,
                        0, -1, 0, 0, 255,
                        0, 0, -1, 0, 255,
                        0, 0, 0, 1, 0
                });
                break;
            case 6: // Warm
                matrix.set(new float[]{
                        1.2f, 0, 0, 0, 15,
                        0, 1.05f, 0, 0, 5,
                        0, 0, 0.85f, 0, 0,
                        0, 0, 0, 1, 0
                });
                break;
            case 7: // Contrast
                float c = 1.5f;
                float t = 128f * (1f - c);
                matrix.set(new float[]{
                        c, 0, 0, 0, t,
                        0, c, 0, 0, t,
                        0, 0, c, 0, t,
                        0, 0, 0, 1, 0
                });
                break;
        }

        Bitmap result = Bitmap.createBitmap(
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));

        canvas.drawBitmap(originalBitmap, 0, 0, paint);

        currentBitmap = result;
        photoView.setImageBitmap(currentBitmap);
    }

    private void drawShape(boolean isCircle) {
        if (currentBitmap == null) {
            Toast.makeText(this, "Спочатку завантажте фото", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);

        int w = mutableBitmap.getWidth();
        int h = mutableBitmap.getHeight();
        float strokeWidth = Math.min(w, h) * 0.025f;
        paint.setStrokeWidth(strokeWidth);

        if (isCircle) {
            paint.setColor(Color.parseColor("#FF5722"));
            float radius = Math.min(w, h) / 3f;
            canvas.drawCircle(w / 2f, h / 2f, radius, paint);
        } else {
            paint.setColor(Color.WHITE);
            float margin = Math.min(w, h) * 0.05f;
            canvas.drawRect(margin, margin, w - margin, h - margin, paint);
        }

        currentBitmap = mutableBitmap;
        photoView.setImageBitmap(currentBitmap);
    }

    private void splitIntoFour() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Спочатку завантажте фото", Toast.LENGTH_SHORT).show();
            return;
        }

        int w = currentBitmap.getWidth();
        int h = currentBitmap.getHeight();
        int halfW = w / 2;
        int halfH = h / 2;

        // 1. Одразу оновлюємо інтерфейс у головному потоці
        Bitmap preview = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(preview);
        Paint linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(Math.min(w, h) * 0.01f);

        canvas.drawLine(halfW, 0, halfW, h, linePaint);
        canvas.drawLine(0, halfH, w, halfH, linePaint);

        // Зберігаємо копію для нарізки до того, як намалювали лінії
        Bitmap sourceForSplit = currentBitmap;

        currentBitmap = preview;
        photoView.setImageBitmap(currentBitmap);

        // 2. Збереження 4 файлів виконуємо у фоновому потоці
        new Thread(() -> {
            Bitmap topLeft = Bitmap.createBitmap(sourceForSplit, 0, 0, halfW, halfH);
            Bitmap topRight = Bitmap.createBitmap(sourceForSplit, halfW, 0, halfW, halfH);
            Bitmap bottomLeft = Bitmap.createBitmap(sourceForSplit, 0, halfH, halfW, halfH);
            Bitmap bottomRight = Bitmap.createBitmap(sourceForSplit, halfW, halfH, halfW, halfH);

            long timestamp = System.currentTimeMillis();
            saveBitmapToGallery(topLeft, "Part1_TL_" + timestamp + ".jpg", false);
            saveBitmapToGallery(topRight, "Part2_TR_" + timestamp + ".jpg", false);
            saveBitmapToGallery(bottomLeft, "Part3_BL_" + timestamp + ".jpg", false);
            saveBitmapToGallery(bottomRight, "Part4_BR_" + timestamp + ".jpg", false);

            // Повідомлення показуємо знову в головному потоці
            runOnUiThread(() ->
                    Toast.makeText(this, "4 частини збережено в галерею!", Toast.LENGTH_LONG).show()
            );
        }).start();
    }

    private void rotatePhoto() {
        if (originalBitmap == null || currentBitmap == null) {
            Toast.makeText(this, "Спочатку завантажте фото", Toast.LENGTH_SHORT).show();
            return;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
        currentBitmap = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.getWidth(), currentBitmap.getHeight(), matrix, true);

        photoView.setImageBitmap(currentBitmap);
    }

    private void addWatermark() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Спочатку завантажте фото", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        int w = mutableBitmap.getWidth();
        int h = mutableBitmap.getHeight();

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setAlpha(180);
        textPaint.setTextSize(Math.min(w, h) * 0.05f);
        textPaint.setShadowLayer(5f, 2f, 2f, Color.BLACK);
        textPaint.setTextAlign(Paint.Align.RIGHT);

        float margin = Math.min(w, h) * 0.04f;
        canvas.drawText("© MyPhotoEditor", w - margin, h - margin, textPaint);

        currentBitmap = mutableBitmap;
        photoView.setImageBitmap(currentBitmap);
    }

    private void saveBitmapToGallery(Bitmap bitmapToSave, String fileName, boolean showToast) {
        if (bitmapToSave == null) {
            if (showToast) Toast.makeText(this, "Завантажте фото", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyPhotoEditor");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("Не вдалося створити файл");

            OutputStream stream = getContentResolver().openOutputStream(uri);
            if (stream == null) throw new Exception("Не вдалося відкрити файл");

            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();

            if (showToast) {
                Toast.makeText(this, "Фото збережено!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            if (showToast) {
                Toast.makeText(this, "Помилка збереження", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sharePhoto() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Завантажте фото", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = new File(getCacheDir(), "shared_photo.jpg");
            FileOutputStream stream = new FileOutputStream(file);
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Поділитися фото"));
        } catch (Exception e) {
            Toast.makeText(this, "Помилка поширення", Toast.LENGTH_SHORT).show();
        }
    }
}