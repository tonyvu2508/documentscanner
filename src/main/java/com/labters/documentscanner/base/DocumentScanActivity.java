package com.labters.documentscanner.base;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.labters.documentscanner.R;
import com.labters.documentscanner.helpers.ScannerConstants;
import com.labters.documentscanner.libraries.NativeClass;
import com.labters.documentscanner.libraries.PolygonView;
import com.labters.documentscanner.tflite.Classifier;
import com.labters.documentscanner.tflite.TensorFlowImageClassifier;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public abstract class DocumentScanActivity extends AppCompatActivity {

    protected CompositeDisposable disposable = new CompositeDisposable();
    private Bitmap selectedImage;
    private NativeClass nativeClass = new NativeClass();

    protected abstract FrameLayout getHolderImageCrop();

    protected abstract ImageView getImageView();

    protected abstract PolygonView getPolygonView();

    protected abstract void showProgressBar();

    protected abstract void hideProgressBar();

    protected abstract void showError(CropperErrorType errorType);

    protected abstract Bitmap getBitmapImage();

    private Classifier classifier;

    private static final String MODEL_PATH = "tflite_model_060821_90.tflite";
    private static final boolean QUANT = false;
    private static final String LABEL_PATH = "labels.txt";
    private static final int INPUT_SIZE = 224;
    private static final int INPUT_SIZE_WIDTH = 320;
    private static final int INPUT_SIZE_HEIGHT = 240;


    private void setImageRotation() {
        Bitmap tempBitmap = ScannerConstants.selectedImageBitmap;
        Log.d("xxxx", tempBitmap.getWidth() + "---" + tempBitmap.getHeight());
        if (tempBitmap.getWidth() > tempBitmap.getHeight()) {
             rotateBitmap(tempBitmap, 90);
        }
    }

    protected Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void setProgressBar(boolean isShow) {
        if (isShow)
            showProgressBar();
        else
            hideProgressBar();
    }

    protected void startCropping() {
        selectedImage = getBitmapImage();
        setProgressBar(true);
        disposable.add(Observable.fromCallable(() -> {
                    Bitmap tempBitmap = ScannerConstants.selectedImageBitmap;
                      Log.d("xxxx tempBitmap", tempBitmap.getWidth() + "---" + tempBitmap.getHeight());
//                    if (tempBitmap.getWidth() > tempBitmap.getHeight()) {
//                        rotateBitmap(tempBitmap, 90);
//                    }
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE,
                            QUANT);
//                    makeButtonVisible();
                    Bitmap bitmap = Bitmap.createScaledBitmap(tempBitmap, INPUT_SIZE_WIDTH, INPUT_SIZE_HEIGHT, false);
                    return classifier.recognizeImage(bitmap);
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((result) -> {
                            initializeCropping(result);
                            setProgressBar(false);
                        })
        );
    }


    public void initializeCropping(float[][][] labelProbArray) {
        Log.d("xxxx result", Arrays.deepToString(labelProbArray[0]));

        Bitmap scaledBitmap = scaledBitmap(selectedImage, getHolderImageCrop().getWidth() ,  getHolderImageCrop().getHeight());
        getImageView().setImageBitmap(scaledBitmap);

        Bitmap tempBitmap = ((BitmapDrawable) getImageView().getDrawable()).getBitmap();
        Log.d("xxx tempBitmap 123", tempBitmap.getWidth() + "xxx" + tempBitmap.getHeight());
        try {
//            pointFs = getEdgePoints(tempBitmap);

            float ratioX = (float) tempBitmap.getWidth() / 320;
            float ratioY = (float) tempBitmap.getHeight() / 240;

            Log.d("xxxx", ratioX + "--" + ratioY);

            float point1X = (labelProbArray[0][0][0]) * ratioX;
            float point1Y = (labelProbArray[0][0][1]) * ratioY;

            float point2X = (labelProbArray[0][1][0]) * ratioX;
            float point2Y = (labelProbArray[0][1][1]) * ratioY;

            float point4X = (labelProbArray[0][3][0]) * ratioX;
            float point4Y = (labelProbArray[0][3][1]) * ratioY;

            float point3X = (labelProbArray[0][2][0]) * ratioX;
            float point3Y = (labelProbArray[0][2][1]) * ratioY;

            Map<Integer, PointF> orderedPoints = new HashMap<>();
            orderedPoints.put(0, new PointF(point1X, point1Y));
            orderedPoints.put(1, new PointF(point2X, point2Y));
            orderedPoints.put(2, new PointF(point4X, point4Y));
            orderedPoints.put(3, new PointF(point3X, point3Y));

            Log.d("xxxx origin image", orderedPoints.toString());
            getPolygonView().setPoints(orderedPoints);
            getPolygonView().setVisibility(View.VISIBLE);

            int padding = (int) getResources().getDimension(R.dimen.scanPadding);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
            layoutParams.gravity = Gravity.CENTER;

            getPolygonView().setLayoutParams(layoutParams);
            getPolygonView().setPointColor(getResources().getColor(R.color.blue));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected ArrayList<Float> getCropCoords() {
        Map<Integer, PointF> points = getPolygonView().getPoints();

        float xRatio = (float) selectedImage.getWidth() / getImageView().getWidth();
        float yRatio = (float) selectedImage.getHeight() / getImageView().getHeight();

        float x1 = (Objects.requireNonNull(points.get(0)).x) * xRatio;
        float x2 = (Objects.requireNonNull(points.get(1)).x) * xRatio;
        float x3 = (Objects.requireNonNull(points.get(2)).x) * xRatio;
        float x4 = (Objects.requireNonNull(points.get(3)).x) * xRatio;
        float y1 = (Objects.requireNonNull(points.get(0)).y) * yRatio;
        float y2 = (Objects.requireNonNull(points.get(1)).y) * yRatio;
        float y3 = (Objects.requireNonNull(points.get(2)).y) * yRatio;
        float y4 = (Objects.requireNonNull(points.get(3)).y) * yRatio;

        ArrayList<Float> array = new ArrayList<>();
        array.add(x1);
        array.add(y1);
        array.add(x2);
        array.add(y2);
        array.add(x3);
        array.add(y3);
        array.add(x4);
        array.add(y4);

        return array;
    }

    protected Bitmap getCroppedImage() {
        try {
            Map<Integer, PointF> points = getPolygonView().getPoints();

            float xRatio = (float) selectedImage.getWidth() / getImageView().getWidth();
            float yRatio = (float) selectedImage.getHeight() / getImageView().getHeight();

            float x1 = (Objects.requireNonNull(points.get(0)).x) * xRatio;
            float x2 = (Objects.requireNonNull(points.get(1)).x) * xRatio;
            float x3 = (Objects.requireNonNull(points.get(2)).x) * xRatio;
            float x4 = (Objects.requireNonNull(points.get(3)).x) * xRatio;
            float y1 = (Objects.requireNonNull(points.get(0)).y) * yRatio;
            float y2 = (Objects.requireNonNull(points.get(1)).y) * yRatio;
            float y3 = (Objects.requireNonNull(points.get(2)).y) * yRatio;
            float y4 = (Objects.requireNonNull(points.get(3)).y) * yRatio;
            Bitmap finalBitmap = getBitmapImage();
//            if(finalBitmap.getWidth() < finalBitmap.getHeight()){
//                Bitmap rotateImage = rotateBitmap(finalBitmap, 90);
//                return nativeClass.getScannedBitmap(rotateImage, x1, y1, x2, y2, x3, y3, x4, y4);
//            }
            return nativeClass.getScannedBitmap(finalBitmap, x1, y1, x2, y2, x3, y3, x4, y4);
        } catch (Exception e) {
            showError(CropperErrorType.CROP_ERROR);
            return null;
        }
    }

    protected Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) throws Exception {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        return orderedPoints;
    }

    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
        if (point2f == null)
            point2f = new MatOfPoint2f();
        List<Point> points = Arrays.asList(point2f.toArray());
        List<PointF> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            result.add(new PointF(((float) points.get(i).x), ((float) points.get(i).y)));
        }

        return result;

    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = getPolygonView().getOrderedPoints(pointFs);
        if (!getPolygonView().isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
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
        classifier.close();
    }
}
