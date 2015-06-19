package com.codamasters.thehipsterizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

// Esta clase es la encargada de manejar los frames que la cámara va percibiendo
// para mostrarlos en pantalla

// Para poder usar filtros de openGl es necesario usar una view especial, la GPUImageView

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
	private SurfaceHolder mHolder;
	private Camera mCamera;
    private String currentFlash = Parameters.FLASH_MODE_OFF;
    private Context context;
    boolean isPreviewRunning;
    private GPUImageView view;
    private Bitmap bitmap;
    private Matrix matrix;
    private Camera.Size previewSize;
    private YuvImage yuvimage;
    private ByteArrayOutputStream baos;
    private byte[] jdata;
    private Bitmap rotatedBitmap;
    private GPUImageFilter actualFilter;
    private int cameraId;

    // Obtenemos la información de la clase CameraActivity que es donde se inicaliza un objeto
    // de CameraPreview

    public CameraPreview(Context context, Camera camera, GPUImageView view) {
		super(context);
        matrix = new Matrix();
        mCamera = camera;
		mHolder = getHolder();
		mHolder.addCallback(this);
        isPreviewRunning = false;
        this.context = context;
        this.view = view;
        // deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setWillNotDraw(false);

    }

    // Hasta que no se haya creado la superficie para mostrar los frame no podemos iniciar
    // la preview

	public void surfaceCreated(SurfaceHolder holder) {
		try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                isPreviewRunning = true;
            }
		} catch (IOException e) {
			Log.d(VIEW_LOG_TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

    // Función que se encarga de actualizar la cámara

	public void refreshCamera(Camera camera) {

        if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
            isPreviewRunning = false;
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}
		// set preview size and make any resize, rotate or
		// reformatting changes here
		// start preview with new settings

		setCamera(camera);
		try {


            if(this!=null)
                mCamera.setPreviewCallback(this);

            mCamera.setPreviewDisplay(mHolder);
			Camera.Parameters params = mCamera.getParameters();
            params.setPreviewSize(640,480);
            params.setFlashMode(currentFlash);
            mCamera.setParameters(params);


            mCamera.startPreview();

            isPreviewRunning = true;
		} catch (Exception e) {
			Log.d(VIEW_LOG_TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

    // Si la superficie cambia, primero tenemos que detener la preview, realizar los cambios pertenecientes
    // y nuevamente lanzar la preview

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.d("Mensaje", "Superficie cambiada");

        if (isPreviewRunning)
        {
            mCamera.stopPreview();
            isPreviewRunning = false;
    }

        try{

            refreshCamera(mCamera);
            mCamera.startPreview();
            isPreviewRunning = true;

        } catch (Exception e){
            Log.d("Error", "Error starting camera preview: " + e.getMessage());
        }

    }

    // Función que cambia la instancia de la cámara usada

	public void setCamera(Camera camera) {
		mCamera = camera;

	}

    // Función que modifica el flash activado

    public void setCurrentFlash(String currentFlash) {
        this.currentFlash = currentFlash;
    }
    public String getCurrentFlash(){ return this.currentFlash; }

    // Función que modifica el filtro activo y actualiza con ello la GPUImageView

    public void setCurrentFilter(GPUImageFilter actualFilter) {
        this.actualFilter = actualFilter;
        if(actualFilter != null)
            view.setFilter(actualFilter);
    }


    @Override
	public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("Mensaje", "Superficie destruida");
    }


    // Con cada frame capurado tendremos que realizar una serie de tratamientos para poder aplicarle
    // el filro seleccionado y así mostrar el frame resultante

    // Además tendremos que tener en cuenta una serie de transformaciones:
    //      - Cuando la imagen proviene de la cámara trasera es necesario aplicar un giro de 90º
    //      - Cuando la imagen de la cámara delantera tenemos que aplicarle un "espejo" y girarla 90º

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
            Parameters parameters = camera.getParameters();
            int imageFormat = parameters.getPreviewFormat();
            if (imageFormat == ImageFormat.NV21)
            {

                try {

                previewSize = camera.getParameters().getPreviewSize();

                yuvimage=new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);

                baos = new ByteArrayOutputStream();
                yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 80, baos);
                jdata = baos.toByteArray();

                bitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);

                matrix = new Matrix();

                if(cameraId==1) {
                    float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1};
                    Matrix matrixMirrorY = new Matrix();
                    matrixMirrorY.setValues(mirrorY);

                    matrix.postConcat(matrixMirrorY);
                }
                matrix.postRotate(90);


                rotatedBitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap .getWidth(), bitmap .getHeight(), matrix, true);
                bitmap.recycle();

                view.setImage(rotatedBitmap);

                } catch (Error e) {
                    Log.e("Error", e.getLocalizedMessage());
                }

            }
            //camera.addCallbackBuffer(data);
        }

        // Función que cambia el código de la cámara seleccionada

        public void setCameraCode(int cameraId) {
            this.cameraId = cameraId;
        }

        public Camera getmCamera(){
            return mCamera;
        }

}