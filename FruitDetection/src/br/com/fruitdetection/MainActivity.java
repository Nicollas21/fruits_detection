package br.com.fruitdetection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

public class MainActivity extends Activity implements CvCameraViewListener2 {
	
	private static final String TAG = "FruitDetect::Activity";
	private static final Scalar FRUIT_RECT_COLOR = new Scalar(0, 255, 0, 255);
	public static final int JAVA_DETECTOR = 0; 
	
	private MenuItem               mItemFruit50;
	private MenuItem               mItemFruit40;
	private MenuItem               mItemFruit30;
	private MenuItem               mItemFruit20; 
	
	private Mat                    mRgba; 
	private Mat                    mGray; 
	private File                   mCascadeFile;
	private CascadeClassifier      mJavaDetector;
	
	private int mDetectorType = JAVA_DETECTOR;
	private float mRelativeFruitSize  = 0.2f;
	private int mAbsoluteFruitSize   = 0; 
	
	private CameraBridgeViewBase mOpenCvCameraView;
	
	private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.banana_classifier);
                    	
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        						mCascadeFile = new File(cascadeDir, "banana_classifier.xml");                        
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                       
                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fruit_detect_surface_view);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null){
			  mOpenCvCameraView.disableView();			
		}          
	}
	
	@Override
	protected void onResume() {		
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mOpenCvCameraView.disableView();
	}



	@Override
	public void onCameraViewStarted(int width, int height) {
		 mGray = new Mat();
	     mRgba = new Mat();
	}

	@Override
	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFruitSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFruitSize) > 0) {
                mAbsoluteFruitSize = Math.round(height * mRelativeFruitSize);
            }           
        }

        MatOfRect fruits = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, fruits, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFruitSize, mAbsoluteFruitSize), new Size());
        }
        
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] fruitsArray = fruits.toArray();
        for (int i = 0; i < fruitsArray.length; i++){
        	Core.rectangle(mRgba, fruitsArray[i].tl(), fruitsArray[i].br(), FRUIT_RECT_COLOR, 3);
        	Core.putText(mRgba, "banana"+i, new Point(fruitsArray[i].x, fruitsArray[i].y), 3, 1, new Scalar(255, 0, 0, 255), 1);       	
        }
        return mRgba;
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mItemFruit50 = menu.add("Fruit size 50%");
        mItemFruit40 = menu.add("Fruit size 40%");
        mItemFruit30 = menu.add("Fruit size 30%");
        mItemFruit20 = menu.add("Fruit size 20%");
        return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		 if (item == mItemFruit50){
			 setMinFruitSize(0.5f);			 
		 } 
		 else if (item == mItemFruit40){
			 setMinFruitSize(0.4f);
		 }
	     else if (item == mItemFruit30){
	    	 setMinFruitSize(0.3f);
	     }
		 else if (item == mItemFruit20){
			 setMinFruitSize(0.2f); 
		 }
		 return true;  
	}
	
	private void setMinFruitSize(float fruitSize) {
        mRelativeFruitSize = fruitSize;
        mAbsoluteFruitSize = 0;
    }
       
	

	
	
}
