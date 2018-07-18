package org.reactnative.camera.tasks;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.BarcodeFormat;
// import com.google.zxing.common.HybridBinarizer;
// import com.google.zxing.common.GlobalHistogramBinarizer;
import org.reactnative.camera.utils.GlobalHistogramBinarizer;
import org.reactnative.camera.utils.HybridBinarizer;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.zbar.lib.ZbarManager;

import android.util.Log;

public class BarCodeScannerAsyncTask extends android.os.AsyncTask<Void, Void, Result> {
  private byte[] mImageData;
  private int mWidth;
  private int mHeight;
  private BarCodeScannerAsyncTaskDelegate mDelegate;
  private final MultiFormatReader mMultiFormatReader;

  private final ZbarManager mZarBarManager;

  //  note(sjchmiela): From my short research it's ok to ignore rotation of the image.
  public BarCodeScannerAsyncTask(
      BarCodeScannerAsyncTaskDelegate delegate,
      MultiFormatReader multiFormatReader,
      byte[] imageData,
      int width,
      int height
  ) {
    mImageData = imageData;
    mWidth = width;
    mHeight = height;
    mDelegate = delegate;
    mMultiFormatReader = multiFormatReader;
    mZarBarManager = new ZbarManager();
  }

  @Override
  protected Result doInBackground(Void... ignored) {
    if (isCancelled() || mDelegate == null) {
      return null;
    }

    Result result = null;

    try {
      BinaryBitmap bitmap = generateBitmapFromImageData(mImageData, mWidth, mHeight);
      result = mMultiFormatReader.decodeWithState(bitmap);
      Log.d("codingpapi", "recognize with zxing result:" + result);
      if (result == null) {
        result = getQRByZbar(mImageData, mWidth, mHeight);
      }
    } catch (NotFoundException e) {
      result = getQRByZbar(mImageData, mWidth, mHeight);
      // No barcode found, result is already null.
    } catch (Throwable t) {
      t.printStackTrace();
    }
    if (result != null) {
      Log.d("codingpapi", "recognize with zxing result:" + result.getText());
    }

    return result;
  }
  @Override
  protected void onPostExecute(Result result) {
    super.onPostExecute(result);
    if (result != null) {
      mDelegate.onBarCodeRead(result);
    }
    mDelegate.onBarCodeScanningTaskCompleted();
  }

  private Result getQRByZbar(byte[] imageData, int width, int height) {
    int tX = width / 5;
    int tY = height / 4;
    int tXW = tX * 3;
    int tYW = tY * 2;

    Result result = null;
    PlanarYUVLuminanceSource source = generateLuminanceSource(imageData, width, height);
    String stringResult = mZarBarManager.decode(source.getMatrix(), tXW, tYW, false, 0, 0, tXW, tYW);
    if (stringResult != null) {
      byte[] fakeYuvData = new byte[10];
      ResultPoint[] fakePoints = new ResultPoint[10];
      result = new Result(stringResult, fakeYuvData, fakePoints, BarcodeFormat.QR_CODE);
    }
    Log.d("codingpapi", "recognize with zbar result:" + result);
    return result;
  }

  private PlanarYUVLuminanceSource generateLuminanceSource(byte[] imageData, int width, int height) {
    int tX = width / 5;
    int tY = height / 4;
    int tXW = tX * 3;
    int tYW = tY * 2;
    PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
        imageData, // byte[] yuvData
        width, // int dataWidth
        height, // int dataHeight
        tX, // int left
        tY, // int top
        tXW, // int width
        tYW, // int height
        false // boolean reverseHorizontal
    );
    return source;
  }

  private BinaryBitmap generateBitmapFromImageData(byte[] imageData, int width, int height) {
    PlanarYUVLuminanceSource source = generateLuminanceSource(imageData, width, height);
    return new BinaryBitmap(new GlobalHistogramBinarizer(source));
  }
}
