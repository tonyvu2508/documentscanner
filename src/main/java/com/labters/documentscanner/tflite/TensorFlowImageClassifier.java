package com.labters.documentscanner.tflite;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import com.labters.documentscanner.helpers.ScannerConstants;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Created by amitshekhar on 17/03/18.
 */

public class TensorFlowImageClassifier implements Classifier {

    private static final int MAX_RESULTS = 3;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final float THRESHOLD = 0.1f;

    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 255;

    private Interpreter interpreter;
    private int inputSize;
    private List<String> labelList;
    private boolean quant;

    private TensorFlowImageClassifier() {

    }

    public static Classifier create(AssetManager assetManager,
                                    String modelPath,
                                    String labelPath,
                                    int inputSize,
                                    boolean quant) throws IOException {

        TensorFlowImageClassifier classifier = new TensorFlowImageClassifier();
        classifier.interpreter = new Interpreter(classifier.loadModelFile(assetManager, modelPath), new Interpreter.Options());
        classifier.labelList = classifier.loadLabelList(assetManager, labelPath);
        classifier.inputSize = inputSize;
        classifier.quant = quant;

        return classifier;
    }

    @Override
    public float[][][] recognizeImage(Bitmap bitmap) {
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);
        float[][][] result = new float[1][4][2];
        interpreter.run(byteBuffer, result);
        return result;
    }

    @Override
    public void close() {
        interpreter.close();
        interpreter = null;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int IMAGE_WIDTH = 320;
        int IMAGE_HEIGHT = 240;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_WIDTH * IMAGE_HEIGHT * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[320 * 240];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int pixel : pixels) {
            float rChannel = ((pixel >> 16) & 0xFF) / IMAGE_STD;
            float gChannel = ((pixel >> 8) & 0xFF) / IMAGE_STD;
            float bChannel = ((pixel) & 0xFF) / IMAGE_STD;
            byteBuffer.putFloat(rChannel);
            byteBuffer.putFloat(gChannel);
            byteBuffer.putFloat(bChannel);
        }
        return byteBuffer;
    }

    @SuppressLint("DefaultLocale")
    private List<Recognition> getSortedResultByte(byte[][][] labelProbArray) {
        Log.d("xxxx", Arrays.deepToString(labelProbArray[0]));
//        PriorityQueue<Recognition> pq =
//                new PriorityQueue<>(
//                        MAX_RESULTS,
//                        new Comparator<Recognition>() {
//                            @Override
//                            public int compare(Recognition lhs, Recognition rhs) {
//                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
//                            }
//                        });

//        for (int i = 0; i < labelList.size(); ++i) {
//            float confidence = (labelProbArray[0][i] & 0xff) / 255.0f;
//            if (confidence > THRESHOLD) {
//                pq.add(new Recognition("" + i,
//                        labelList.size() > i ? labelList.get(i) : "unknown",
//                        confidence, quant));
//            }
//        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
//        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
//        for (int i = 0; i < recognitionsSize; ++i) {
//            recognitions.add(pq.poll());
//        }

        return recognitions;
    }

    @SuppressLint("DefaultLocale")
    private Map<Integer, PointF> getSortedResultFloat(float[][][] labelProbArray) {
        Log.d("xxxx", Arrays.deepToString(labelProbArray[0]));

        float ratioX = (float) 1008 / 320;
        float ratioY = (float) 756 / 240;

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

        return orderedPoints;
    }

}
