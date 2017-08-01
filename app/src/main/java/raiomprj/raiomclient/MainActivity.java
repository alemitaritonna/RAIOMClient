package raiomprj.raiomclient;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    //Socket
    private Socket mSocket;
    //Matriz
    private Mat img = null;
    //Timer
    // Timer
    private long startTime = 0;

    //Constantes
    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOGIN = 0;
    private static final int TYPING_TIMER_LENGTH = 600;
    private static final int READ_REQUEST_CODE = 42;

    //Camara
    private CameraBridgeViewBase mOpenCvCameraView;

    int TAKE_PHOTO_CODE = 0;
    public static int count = 0;
    private Mat mRgba;

    //URL y nombe de imagen en dónde se guarda
    String fileName=null;



    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //inicializamos la Cámara
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);


        //creo el Socket con la dirección  url del Servidor
        try {
            mSocket = IO.socket(Constants.SERVER_URL);
        }catch (URISyntaxException e) {
            Log.v("AvisActivity", "error connecting to socket");
        }

        // activo los Listener... en modo escucha.. si se produce algún evento, se dispara el método
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on("fast response", onFASTResponse);
        mSocket.on("orb response", onORBResponse);

        mSocket.connect();


        //Presiono el botón y voila!
        Button fileButton = (Button) findViewById(R.id.button);
        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentDateandTime = "";
                fileName = Environment.getExternalStorageDirectory().getPath() +
                        "/sample_picture_" + currentDateandTime + ".png";

                Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGBA2BGR);
                Imgcodecs.imwrite(fileName, mRgba);
                Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_BGR2RGBA);

                try {
                    // show image in view
                    showImage(Uri.parse(fileName));
                    // send image to server
                    sendImage(Uri.parse("file:///"+fileName));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

  }




    //*********** MÉTODOS VARIOS *****************************************************
    private void showImage(Uri uri) throws IOException {
        ImageView imageView = (ImageView) findViewById(R.id.fileImageView);
        imageView.setImageURI(uri);

        //una vez que tengo la imagen la cargo en una Matriz para trabajarla
        img = new Mat(1080, 1920, CvType.CV_8UC3);
        img = Imgcodecs.imread(uri.toString());
        if(img.empty()) {
            Log.i(TAG, "OJOOOOO IMAGEN VACIA!!!!!");
        }

    }


    private void sendImage(Uri uri) throws JSONException, FileNotFoundException {
        JSONObject data = new JSONObject();
        data.put("image", encodeImage(uri));

        // Envía la imagen al Servidor para ser Procesada!!!
        //mSocket.emit("fast", data);
        mSocket.emit("fast", data);
    }


    private String encodeImage(Uri uri) throws FileNotFoundException {

        InputStream fis = getContentResolver().openInputStream(uri);
        Bitmap bm = BitmapFactory.decodeStream(fis);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, output);

        return Base64.encodeToString(output.toByteArray(), Base64.DEFAULT);
    }






//*********** LISTERNER DEL SOCKET *****************************************************
    private Emitter.Listener onFASTResponse = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // TODO: Timer
                    long currentTime = System.nanoTime();
                    long diffTime = currentTime - startTime;

                    Log.i(TAG, "Time in nanoTime:");
                    Log.i(TAG, "start time: " + String.valueOf(startTime));
                    Log.i(TAG, "current time: " + String.valueOf(currentTime));
                    Log.i(TAG, "difference time (en nanoseg): " + String.valueOf(diffTime));
                    Log.i(TAG, "difference time (en segundos): " + String.valueOf( (double)(diffTime) / 1000000000.0)); // seconds

                    //
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    String message;
                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                    } catch (JSONException e) {
                        return;
                    }
                    Log.i("onFASTResponse", message.toString());
                    String[] sCoors = message.replaceAll("\\[\\[", "")
                            .replaceAll("\\]\\]", "")
                            .replaceAll("\\[", "")
                            //.replaceAll("\\s", "")
                            .split("\\],");
                    Log.i("onFASTResponse", sCoors[0].toString());
                    Log.i("onFASTResponse", sCoors[1].toString());

                    // procesar la respuesta de los keypoints
                    // se crea la lista en donde se pondrán los keypoints
                    List<KeyPoint> lkeypoints = new ArrayList<KeyPoint>();

                    for (int i = 0; i < sCoors.length; i++) {
                        try {
                            ArrayList coor = new ArrayList(2);
                            String[] xy = sCoors[i].split(",");

                            // Con el string que viene del Servidor como reesultado y luego de haberlo parseado
                            // debemos ir armando la lista de los keypoints en el lado del cliente (app Android)
                            KeyPoint kp = new KeyPoint(Float.parseFloat(xy[0]), Float.parseFloat(xy[1]), 1);
                            lkeypoints.add(kp);

                        } catch (NumberFormatException nfe) {
                            //NOTE: write something here if you need to recover from formatting errors
                        };
                    }
                    Log.d(TAG, "Total Keypoints: " + String.valueOf(lkeypoints.size()));

                    // de la lista de keypoints se arma la Matriz de keypoints
                    // esta Matriz es la se usará para dibujar los kp en la imágen de salida
                    MatOfKeyPoint keypoints = new MatOfKeyPoint();
                    keypoints.fromList(lkeypoints);

                    // se crea la Matriz de respuesta (imágen de salida) de la imagen que se está tratando
                    Mat outimg = new Mat(img.rows(), img.cols(), img.type());


                    //https://groups.google.com/forum/#!topic/android-opencv/co9Zv9pon30
                    //The problem is that unfortunately drawKeypoints() can't work with RGBA Mats, it accepts 8UC3 and 8UC1 only.
                    //So if you'd like to call drawKeypoints(), you need convert the picture to RGB and then back
                    // to RGBA to display.
                    Mat rgb = new Mat();
                    Imgproc.cvtColor(img, rgb, Imgproc.COLOR_RGBA2RGB);
                    Features2d.drawKeypoints(rgb, keypoints, rgb, new Scalar(0, 255, 0), 0);
                    //Imgproc.cvtColor(rgb, outimg, Imgproc.COLOR_RGB2RGBA);
                    Imgproc.cvtColor(rgb, outimg, Imgproc.COLOR_RGB2BGR);
                    // Fin FIX

                    Log.i(TAG, "HASTA ACA LLEGO 2");

                    //Creo una imagen BMP tomando eel resultado que es una MATRIZ y la muestro
                    ImageView imageView = (ImageView) findViewById(R.id.fileImageView);
                    Bitmap bm = Bitmap.createBitmap(outimg.cols(), outimg.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(outimg, bm);
                    imageView.setImageBitmap(bm);

                    Log.i("onFASTResponse", "Image set on imageView");
                }
            });
        }
    };


    private Emitter.Listener onORBResponse = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // TODO: Timer
                    long currentTime = System.nanoTime();
                    long diffTime = currentTime - startTime;

                    Log.i(TAG, "Time in nanoTime:");
                    Log.i(TAG, "start time: " + String.valueOf(startTime));
                    Log.i(TAG, "current time: " + String.valueOf(currentTime));
                    Log.i(TAG, "difference time (en nanoseg): " + String.valueOf(diffTime));
                    Log.i(TAG, "difference time (en segundos): " + String.valueOf( (double)(diffTime) / 1000000000.0)); // seconds

                    //
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    String message;
                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                    } catch (JSONException e) {
                        return;
                    }
                    Log.i("onORBResponse", message.toString());
                    String[] sCoors = message.replaceAll("\\[\\[", "")
                            .replaceAll("\\]\\]", "")
                            .replaceAll("\\[", "")
                            //.replaceAll("\\s", "")
                            .split("\\],");
                    Log.i("onORBResponse", sCoors[0].toString());
                    Log.i("onORBResponse", sCoors[1].toString());

                    // procesar la respuesta de los keypoints
                    // se crea la lista en donde se pondrán los keypoints
                    List<KeyPoint> lkeypoints = new ArrayList<KeyPoint>();

                    for (int i = 0; i < sCoors.length; i++) {
                        try {
                            ArrayList coor = new ArrayList(2);
                            String[] xy = sCoors[i].split(",");

                            // Con el string que viene del Servidor como reesultado y luego de haberlo parseado
                            // debemos ir armando la lista de los keypoints en el lado del cliente (app Android)
                            KeyPoint kp = new KeyPoint(Float.parseFloat(xy[0]), Float.parseFloat(xy[1]), 1);
                            lkeypoints.add(kp);

                        } catch (NumberFormatException nfe) {
                            //NOTE: write something here if you need to recover from formatting errors
                        };
                    }
                    Log.d(TAG, "Total Keypoints: " + String.valueOf(lkeypoints.size()));

                    // de la lista de keypoints se arma la Matriz de keypoints
                    // esta Matriz es la se usará para dibujar los kp en la imágen de salida
                    MatOfKeyPoint keypoints = new MatOfKeyPoint();
                    keypoints.fromList(lkeypoints);

                    // se crea la Matriz de respuesta (imágen de salida) de la imagen que se está tratando
                    Mat outimg = new Mat(img.rows(), img.cols(), img.type());


                    //https://groups.google.com/forum/#!topic/android-opencv/co9Zv9pon30
                    //The problem is that unfortunately drawKeypoints() can't work with RGBA Mats, it accepts 8UC3 and 8UC1 only.
                    //So if you'd like to call drawKeypoints(), you need convert the picture to RGB and then back
                    // to RGBA to display.
                    Mat rgb = new Mat();
                    Imgproc.cvtColor(img, rgb, Imgproc.COLOR_RGBA2RGB);
                    Features2d.drawKeypoints(rgb, keypoints, rgb, new Scalar(0, 255, 0), 0);
                    //Imgproc.cvtColor(rgb, outimg, Imgproc.COLOR_RGB2RGBA);
                    Imgproc.cvtColor(rgb, outimg, Imgproc.COLOR_RGB2BGR);
                    // Fin FIX

                    Log.i(TAG, "HASTA ACA LLEGO 2");

                    //Creo una imagen BMP tomando eel resultado que es una MATRIZ y la muestro
                    ImageView imageView = (ImageView) findViewById(R.id.fileImageView);
                    Bitmap bm = Bitmap.createBitmap(outimg.cols(), outimg.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(outimg, bm);
                    imageView.setImageBitmap(bm);

                    Log.i("onORBResponse", "Image set on imageView");
                }
            });
        }
    };


    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSocket.emit("add user", "ALE");
                    Toast.makeText(getApplicationContext(), "Nueva Conexión…", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Desconectado…", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };




    //*********** LIFECYCLE EVENTS *****************************************************
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off("fast response", onFASTResponse);
        mSocket.off("orb response", onORBResponse);

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

    }


    @Override
    public void onResume()
    {
        super.onResume();

        //cargo librería OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC3);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        //Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGBA2BGR);
        return mRgba;
    }

}
