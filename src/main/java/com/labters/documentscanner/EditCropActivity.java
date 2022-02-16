/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:10 PM
 *
 */

package com.labters.documentscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.labters.documentscanner.helpers.ScannerConstants;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class EditCropActivity extends AppCompatActivity {

    protected CompositeDisposable disposable = new CompositeDisposable();
    private ImageView imgBitmap;
    private LinearLayout btnClose;
    private TextView btnDone;
    private boolean isInverted = false;

    private OnClickListener btnCloseClick = v -> finish();
    private OnClickListener btnDoneClick = v -> {
        Intent intent = new Intent();
        setResult(999, intent);
        finish();
    };

    private OnClickListener onRotateClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            disposable.add(
                    Observable.fromCallable(() -> {
                        ScannerConstants.selectedImageBitmap = rotateBitmap(ScannerConstants.selectedImageBitmap, 90);
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                imgBitmap.setImageBitmap(ScannerConstants.selectedImageBitmap);
                            })
            );
        }
    };

    private OnClickListener btnInvertColor = new OnClickListener() {
        @Override
        public void onClick(View v) {
            disposable.add(
                    Observable.fromCallable(() -> {
                        invertColor();
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
//                                Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                                imgBitmap.setImageBitmap(ScannerConstants.selectedImageBitmap);
                            })
            );
        }
    };

    private void invertColor() {
        if (!isInverted) {
            Bitmap bmpMonochrome = Bitmap.createBitmap(ScannerConstants.selectedImageBitmap.getWidth(), ScannerConstants.selectedImageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmpMonochrome);
            ColorMatrix ma = new ColorMatrix();
            ma.setSaturation(0);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(ma));
            canvas.drawBitmap(ScannerConstants.selectedImageBitmap, 0, 0, paint);
            ScannerConstants.selectedImageBitmap = bmpMonochrome.copy(bmpMonochrome.getConfig(), true);
        } else {
            ScannerConstants.selectedImageBitmap = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
        }
        isInverted = !isInverted;
    }

    protected Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit_crop);

        imgBitmap = findViewById(R.id.imgBitmap);
        btnClose = findViewById(R.id.btnBack);
        btnDone = findViewById(R.id.btnDone);

        btnClose.setOnClickListener(btnCloseClick);
        btnDone.setOnClickListener(btnDoneClick);

        if (ScannerConstants.selectedImageBitmap != null) {
            imgBitmap.setImageBitmap(ScannerConstants.selectedImageBitmap);
        }

        ImageView ivRotate = findViewById(R.id.ivRotate);
        ivRotate.setOnClickListener(onRotateClick);

        ImageView ivInvert = findViewById(R.id.ivFilter);
        ivInvert.setOnClickListener(btnInvertColor);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposable.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }
}
