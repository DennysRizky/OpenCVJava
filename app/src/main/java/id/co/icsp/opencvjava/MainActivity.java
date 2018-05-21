package id.co.icsp.opencvjava;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_64F;
import static org.opencv.core.CvType.CV_64FC1;
import static org.opencv.core.CvType.CV_64FC3;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.core.CvType.CV_8UC4;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {
    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // Used in Camera selection from menu (when implemented)
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    Mat mRgba, imgHsv, mRgb;
    Mat mask;
    Mat mBgr;
    Mat lms, coreLms;
    Mat a, b, c, d, e, f, g;
    private int intState = 0;
    private static final int stateProtanopia = 1;
    private static final int stateDeuteranopia = 2;
    private static final int stateHighlight = 3;
    private static final int stateContrast = 4;

    private String [] modes = new String[]{"HIGHLIGHT", "CONSTRAST", "OUTLINING", "DALTON"};

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //set content view AFTER ABOVE sequence (to avoid crash)
        setContentView(R.layout.show_camera);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

        mRgba = new Mat(height, width, CV_8UC4);
        mRgb = new Mat(height, width, CV_8UC3);
        mask = new Mat(height, width, CV_8UC3);
        mBgr = new Mat(height, width, CV_8UC3);
        imgHsv = new Mat(height, width, CV_8UC3);
        lms = new Mat (height,width,CV_8UC3);
        a = new Mat(height, width, CV_8UC3);
        b = new Mat(height, width, CV_8UC3);
        c = new Mat(height, width, CV_8UC3);
        d = new Mat(height, width, CV_8UC3);
        e = new Mat(height, width, CV_8UC3);
        f = new Mat(height, width, CV_8UC3);
        g = new Mat(height, width, CV_8UC3);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        // PROSES MANIPULASI CITRA
        // TODO Auto-generated method stub
        mRgba = inputFrame.rgba();

        //Merubah tangkapan kamera dari mode rgba ke mode hsv
        Imgproc.cvtColor(mRgba, mBgr, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(mBgr, imgHsv, Imgproc.COLOR_BGR2HSV);
        Imgproc.cvtColor(mRgba, mRgb, Imgproc.COLOR_RGBA2RGB);


        //Mask warna biru
        Core.inRange(imgHsv, new Scalar(90, 100, 100), new Scalar(135, 255, 255), mask);

        //Ganti warna biru dengan warna merah
        switch (intState) {
            case stateProtanopia:
                lmsTrans(mRgb,lms);
                Imgproc.cvtColor(mRgb,mRgba, Imgproc.COLOR_RGB2RGBA);
                break;
            case stateDeuteranopia:
                deuDalton(mRgb,lms);
                Imgproc.cvtColor(mRgb,mRgba, Imgproc.COLOR_RGB2RGBA);
                break;
            case stateHighlight:
                mRgba.setTo(new Scalar(0, 0, 255, 100), mask);
                break;
            case stateContrast:
                mRgba.setTo(new Scalar(255, 255, 255, 100), mask);
                break;
        }

        return mRgba;
    }
    public void doPotranopia(View v){
        ((LinearLayout) findViewById(R.id.displayTest)).setVisibility(View.GONE);
        intState = stateProtanopia;
        ((ImageView) findViewById(R.id.btnPotranopia)).setBackgroundColor(Color.GRAY);
        ((ImageView) findViewById(R.id.btnDeuteranopi)).setBackgroundColor(Color.BLACK);
        ((ImageView) findViewById(R.id.btnHighlight)).setBackgroundColor(Color.BLACK);
        ((ImageView) findViewById(R.id.btnContrast)).setBackgroundColor(Color.BLACK);
    }
    public void doDeuteranopia(View v){
        ((LinearLayout) findViewById(R.id.displayTest)).setVisibility(View.GONE);
        intState = stateDeuteranopia;
        ((ImageView) findViewById(R.id.btnPotranopia)).setBackgroundColor(Color.BLACK);
        ((ImageView) findViewById(R.id.btnDeuteranopi)).setBackgroundColor(Color.GRAY);
        ((ImageView) findViewById(R.id.btnHighlight)).setBackgroundColor(Color.BLACK);
        ((ImageView) findViewById(R.id.btnContrast)).setBackgroundColor(Color.BLACK);
    }
    public void doHighlight(View v){
        ((LinearLayout) findViewById(R.id.displayTest)).setVisibility(View.GONE);
        intState = stateHighlight;
        ((ImageView) findViewById(R.id.btnPotranopia)).setBackgroundColor(Color.BLACK);
        ((ImageView) findViewById(R.id.btnDeuteranopi)).setBackgroundColor(Color.BLACK);
        ((ImageView) findViewById(R.id.btnHighlight)).setBackgroundColor(Color.GRAY);
        ((ImageView) findViewById(R.id.btnContrast)).setBackgroundColor(Color.BLACK);
    }
    public void doContrast(View v){
        ((LinearLayout) findViewById(R.id.displayTest)).setVisibility(View.GONE);
        intState = stateContrast;
        ((ImageView) findViewById(R.id.btnPotranopia)).setBackgroundColor(Color.BLACK);
        ((ImageView) findViewById(R.id.btnDeuteranopi)).setBackgroundColor(Color.BLACK);
        ((ImageView) findViewById(R.id.btnHighlight)).setBackgroundColor(Color.BLACK);
        ((ImageView) findViewById(R.id.btnContrast)).setBackgroundColor(Color.GRAY);
    }
    public void doTest(View v){
        Intent main = new Intent(getApplicationContext(),Test.class);
        startActivity(main);
    }
    public void doDisplayTest(View v){
        ((LinearLayout) findViewById(R.id.displayTest)).setVisibility(View.VISIBLE);
    }

    public void lmsTrans(Mat a, Mat b){
        for (int i = 0; i < a.rows(); i++) {
            for (int j = 0; j < a.cols(); j++) {

                double[] rgbValue = a.get(i, j);
                double[] lmsValue = b.get(i, j);

                //RGB into LMS
                lmsValue[0] =   rgbValue[0] * 1.4671 +
                                rgbValue[1] * 0.1843 +
                                rgbValue[2] * 0.03;   //B-->S

                lmsValue[1] =   rgbValue[0] * 3.8671 +
                                rgbValue[1] * 27.1554 +
                                rgbValue[2] * 3.4557;   //G-->M

                lmsValue[2] =   rgbValue[0] * 4.1194 +
                                rgbValue[1] * 43.5161 +
                                rgbValue[2] * 17.8824;  //R-->L

                //simulated CVD
                double[] lmsSim = c.get(i,j);

                lmsSim [0] = lmsValue [0];
                lmsSim [1] = lmsValue [1];
                lmsSim [2] = lmsValue [0] * -2.53 +
                            lmsValue [1] * 2.023;


                //LMS simulation into RGB correction
                double [] rgbCorr = d.get(i,j);
                rgbCorr[0] =   lmsSim[0] * 0.69;   //B-->S

                rgbCorr[1] =   lmsSim[0] * (-0.1136) +
                                lmsSim[1] * 0.0540 +
                                lmsSim[2] * (-0.0102);   //G-->M

                rgbCorr[2] =   lmsSim[0] * 0.1167 +
                                lmsSim[1] * (-0.1305) +
                                lmsSim[2] * 0.0809;  //R-->L

                //RGB errorness
                double [] rgbErr = e.get(i,j);
                rgbErr [0] = rgbValue[0] - rgbCorr[0];
                rgbErr [1] = rgbValue[1] - rgbCorr[1];
                rgbErr [2] = rgbValue[2] - rgbCorr[2];

                //Shift RGB for CVD
                double [] rgbShift = f.get(i,j);
                rgbShift [0] = rgbErr[0] * 1 +
                                rgbErr [2] * 0.7;
                rgbShift [1] = rgbErr[1] * 1 +
                        rgbErr [2] * 0.7;
                rgbShift [2] = 0;

                //Daltonize
                double [] rgbDalton = g.get(i,j);
                rgbDalton [0] = rgbValue [0] + rgbShift [0];
                rgbDalton [1] = rgbValue [1] + rgbShift [1];
                rgbDalton [2] = rgbValue [2] + rgbShift [2];

                a.put(i,j,rgbDalton);

            }
        }
    }

    public void deuDalton(Mat a, Mat b){
        for (int i = 0; i < a.rows(); i++) {
            for (int j = 0; j < a.cols(); j++) {

                double[] rgbValue = a.get(i, j);
                double[] lmsValue = b.get(i, j);

                //RGB into LMS
                lmsValue[0] =   rgbValue[0] * 1.4671 +
                        rgbValue[1] * 0.1843 +
                        rgbValue[2] * 0.03;   //B-->S

                lmsValue[1] =   rgbValue[0] * 3.8671 +
                        rgbValue[1] * 27.1554 +
                        rgbValue[2] * 3.4557;   //G-->M

                lmsValue[2] =   rgbValue[0] * 4.1194 +
                        rgbValue[1] * 43.5161 +
                        rgbValue[2] * 17.8824;  //R-->L

                //simulated CVD
                double[] lmsSim = c.get(i,j);

                lmsSim [0] = lmsValue [0];
                lmsSim [1] = lmsValue [0]*1.25 +
                                lmsValue[2]*0.49;
                lmsSim [2] = lmsValue [2];


                //LMS simulation into RGB correction
                double [] rgbCorr = d.get(i,j);
                rgbCorr[0] =   lmsSim[0] * 0.69;   //B-->S

                rgbCorr[1] =   lmsSim[0] * (-0.11) +
                        lmsSim[1] * 0.05 +
                        lmsSim[2] * (-0.01);   //G-->M

                rgbCorr[2] =   lmsSim[0] * 0.12 +
                        lmsSim[1] * (-0.13) +
                        lmsSim[2] * 0.08;  //R-->L

                //RGB errorness
                double [] rgbErr = e.get(i,j);
                rgbErr [0] = rgbValue[0] - rgbCorr[0];
                rgbErr [1] = rgbValue[1] - rgbCorr[1];
                rgbErr [2] = rgbValue[2] - rgbCorr[2];

                //Shift RGB for CVD
                double [] rgbShift = f.get(i,j);
                rgbShift [0] = rgbErr[0] * 1 +
                        rgbErr [2] * 0.7;
                rgbShift [1] = rgbErr[1] * 1 +
                        rgbErr [2] * 0.7;
                rgbShift [2] = 0;

                //Daltonize
                double [] rgbDalton = g.get(i,j);
                rgbDalton [0] = rgbValue [0] + rgbShift [0];
                rgbDalton [1] = rgbValue [1] + rgbShift [1];
                rgbDalton [2] = rgbValue [2] + rgbShift [2];

                a.put(i,j,rgbDalton);

            }
        }
    }


}
