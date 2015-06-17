package com.codamasters.thehipsterizer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageHazeFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSketchFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageToonFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;


public class CameraActivity extends ActionBarActivity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private PictureCallback mPicture;
    private ImageButton capture;
    private LinearLayout filters;
    private ImageView capturedImage;
    private Uri fileUri;
    private String filePath;
    private Context myContext;
    private LinearLayout cameraPreview, menuFiltersLayout;
    private RelativeLayout buttonsLayout;
    private boolean cameraFront;
    private File pictureFile;
    private int cameraId = -1;
    private Drawable autoflashicon, flashicon, noflashicon;
    private final int FLASH_AUTO = 0;
    private final int FLASH_ON = 1;
    private final int FLASH_OFF = 2;
    private int flashState = FLASH_OFF;
    private GPUImageFilter actualFilter;
    private String currentFilter = "";
    private GPUImageView view;
    private Matrix matrix;
    private Bitmap auxImage;
    private ProgressBar progBar;
    private Handler mHandler;
    private int _cameraId;
    private Toolbar toolbar;
    private ImageView button_filters_image;
    private TextView button_filters_text;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        myContext = this;

        // Inicializamos todos los objetos
        initialize();

        mPreview.setWillNotDraw(false);

        // Guardamos el handler porque nos hará falta más adelante para usar hebras secundarias
        // pero aún así poder acceder a la interfz de usuario
        mHandler = new Handler(Looper.getMainLooper());

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object

            // Asignamos la toolbar como nueva ActionBar y la configuramos con el botón de volver hacia atrás
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CameraActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.animator.animation3, R.animator.animation4);
                }
            });
        }
        else{
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    // Generamos el menú apartir del xml correspondiente y
    // asignamos los estados diferentes que tiene el flash en el Menú
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        switch (flashState){
            case FLASH_OFF:
                menu.findItem(R.id.action_flash).setIcon(noflashicon);
                return true;
            case FLASH_ON:
                menu.findItem(R.id.action_flash).setIcon(flashicon);
                return true;
            case FLASH_AUTO:
                menu.findItem(R.id.action_flash).setIcon(autoflashicon);
                return true;
            default:
                menu.findItem(R.id.action_flash).setIcon(noflashicon);
        }
        return true;
    }

    // Esta función se encarga de manejar los eventos que se producen al hacer clic
    // sobre los items de la barra de menú
    // Tendremos el botón de cambiar la cámara de trasera a delantera y viceversa
    // Y el botón para modificar los íconos del flash cuando hacemos clic sobre el

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_switch:
                int camerasNumber = Camera.getNumberOfCameras();
                if (camerasNumber > 1) {

                    try {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                releaseCamera();
                                chooseCamera();
                                mPreview.refreshCamera(mCamera);

                            }
                        });
                    } catch (Exception e) {
                    e.printStackTrace();
                    }


                } else {
                    Toast toast = Toast.makeText(myContext, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
                    toast.show();
                }
                return true;

            case R.id.action_flash:

                if (item.getIcon() == autoflashicon) {
                    mPreview.setCurrentFlash(Camera.Parameters.FLASH_MODE_ON);
                    mPreview.refreshCamera(mCamera);
                    flashState = FLASH_ON;
                }
                else if (item.getIcon() == flashicon) {
                    mPreview.setCurrentFlash(Camera.Parameters.FLASH_MODE_OFF);
                    mPreview.refreshCamera(mCamera);
                    flashState = FLASH_OFF;

                }
                else if(item.getIcon() == noflashicon) {
                    mPreview.setCurrentFlash(Camera.Parameters.FLASH_MODE_AUTO);
                    mPreview.refreshCamera(mCamera);
                    flashState = FLASH_AUTO;
                }
                invalidateOptionsMenu();
                return true;
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Esta función se encarga de buscar la cámara delantera
    private int findFrontFacingCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }
    // Esta función se encarga de buscar la cámara trasera

    private int findBackFacingCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    // onResume se llamra cuando vuelva de un estado de pause la aplicación, como cuando la minimizamos
    // y la volvimos a abrir, tendrá las mismas acciones que onCreate

    public void onResume() {
        super.onResume();

        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            if (findFrontFacingCamera() < 0) {
                Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
            }

            setContentView(R.layout.activity_camera);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            myContext = this;

            //actionBar.hide();
            //RelativeLayout buttonsLayout = (RelativeLayout)findViewById(R.id.buttonsLayout);
            //buttonsLayout.bringToFront();
            initialize();
            mPreview.setWillNotDraw(false);
            mHandler = new Handler(Looper.getMainLooper());

            mCamera = Camera.open(findBackFacingCamera());
            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object

                // Asignamos la toolbar como nueva ActionBar y la configuramos con el botón de volver hacia atrás
                setSupportActionBar(toolbar);
                toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(CameraActivity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.animator.animation3, R.animator.animation4);
                    }
                });
            }
            else{
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayShowHomeEnabled(false);
                actionBar.setDisplayShowTitleEnabled(false);
            }
        }
    }

    // Inicializamos todos los objetos que vayamos a usar
    public void initialize() {
        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
        view = (GPUImageView) findViewById(R.id.live_filter_view);

        mPreview = new CameraPreview(myContext, mCamera, view);

        cameraPreview.addView(mPreview);

        capture = (ImageButton) findViewById(R.id.button_capture);
        capture.setOnClickListener(captureListener);
        capture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        capture.getBackground().setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                        capture.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        capture.getBackground().clearColorFilter();
                        capture.invalidate();
                        break;
                    }
                }
                return false;
            }
        });


        filters = (LinearLayout) findViewById(R.id.button_filters);
        filters.setOnClickListener(filtersListener);

        capturedImage = (ImageView) findViewById(R.id.capturedImageView);
        capturedImage.setOnClickListener(capturedImageListener);

        //filtersScroll = (ScrollView) findViewById(R.id.filtersScrollView);
        menuFiltersLayout = (LinearLayout) findViewById(R.id.menuFiltersLayout);
        buttonsLayout = (RelativeLayout) findViewById(R.id.buttonsLayout);


        menuFiltersLayout.setVisibility(View.GONE);

        autoflashicon = getResources().getDrawable(R.drawable.autoflash);
        flashicon = getResources().getDrawable(R.drawable.flash);
        noflashicon = getResources().getDrawable(R.drawable.noflash);

        matrix = new Matrix();
        progBar = (ProgressBar) findViewById(R.id.loadingPanel);

        button_filters_image = (ImageView) findViewById(R.id.button_filters_image);
        button_filters_text = (TextView) findViewById(R.id.button_filters_text);
    }


    // Conjunto de onClickListeners correspondientes a cada filtro de la lista de filtros posibles
    // Al hacer clic sobre uno desaparece la barra de la lista
    // y actualizamos el filtro activo en la imagen de seleccion de filtros

    public void filterNormal(View v) {
        currentFilter = "normal";
        mPreview.setCurrentFilter(new IFNormalFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_normal));
        button_filters_text.setText("None");

    }
    public void filterNashville(View v) {
        currentFilter = "nashville";
        mPreview.setCurrentFilter( new IFNashvilleFilter(this) );
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_nashville));
        button_filters_text.setText("Nashville");
    }

    public void filter1977(View v) {
        currentFilter = "1977";
        mPreview.setCurrentFilter( new IF1977Filter(this) );
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_1977));
        button_filters_text.setText("1977");
    }

    public void filterValencia(View v) {
        currentFilter = "valencia";
        mPreview.setCurrentFilter(new IFValenciaFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_valencia));
        button_filters_text.setText("Valencia");
    }

    public void filterAmaro(View v) {
        currentFilter = "amaro";
        mPreview.setCurrentFilter (new IFAmaroFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_amaro));
        button_filters_text.setText("Amaro");
    }

    public void filterBrannan(View v) {
        currentFilter = "brannan";
        mPreview.setCurrentFilter (new IFBrannanFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_brannan));
        button_filters_text.setText("Brannan");
    }

    public void filterEarlyBird(View v) {
        currentFilter = "earlybird";
        mPreview.setCurrentFilter (new IFEarlybirdFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_earlybird));
        button_filters_text.setText("EarlyBird");
    }

    public void filterHefe(View v) {
        currentFilter = "hefe";
        mPreview.setCurrentFilter (new IFHefeFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_hefe));
        button_filters_text.setText("Hefe");
    }

    public void filterHudson(View v) {
        currentFilter = "hudson";
        mPreview.setCurrentFilter (new IFHudsonFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_hudson));
        button_filters_text.setText("Hudson");
    }

    public void filterInkwell(View v) {
        currentFilter = "inkwell";
        mPreview.setCurrentFilter (new IFInkwellFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_inkwell));
        button_filters_text.setText("Inkwell");
    }

    public void filterLomofi(View v) {
        currentFilter = "lomofi";
        mPreview.setCurrentFilter (new IFLomofiFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_lomofi));
        button_filters_text.setText("Lomofi");
    }

    public void filterLordKelvin(View v) {
        currentFilter = "lordkelvin";
        mPreview.setCurrentFilter(new IFLordKelvinFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_lordkelvin));
        button_filters_text.setText("LordKelvin");
    }


    public void filterRise(View v) {
        currentFilter = "rise";
        mPreview.setCurrentFilter(new IFRiseFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_rise));
        button_filters_text.setText("Rise");
    }

    public void filterSierra(View v) {
        currentFilter = "sierra";
        mPreview.setCurrentFilter(new IFSierraFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_sierra));
        button_filters_text.setText("Sierra");
    }

    public void filterSutro(View v) {
        currentFilter = "sutro";
        mPreview.setCurrentFilter(new IFSutroFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_sutro));
        button_filters_text.setText("Sutro");
    }

    public void filterToaster(View v) {
        currentFilter = "toaster";
        mPreview.setCurrentFilter(new IFToasterFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_toaster));
        button_filters_text.setText("Toaster");
    }

    public void filterWalden(View v) {
        currentFilter = "walden";
        mPreview.setCurrentFilter(new IFWaldenFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_walden));
        button_filters_text.setText("Walden");
    }

    public void filterXproll(View v) {
        currentFilter = "xproll";
        mPreview.setCurrentFilter(new IFXproIIFilter(this));
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_xproll));
        button_filters_text.setText("Xproll");
    }

    public void filterHaze(View v) {
        currentFilter = "haze";
        mPreview.setCurrentFilter(new GPUImageHazeFilter());
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_haze));
        button_filters_text.setText("Haze");
    }

    public void filterSketch(View v) {
        currentFilter = "sketch";
        mPreview.setCurrentFilter(new GPUImageSketchFilter());
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_sketch));
        button_filters_text.setText("Sketch");
    }

    public void filterToon(View v) {
        currentFilter = "sketch";
        mPreview.setCurrentFilter(new GPUImageToonFilter());
        menuFiltersLayout.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);

        button_filters_image.setImageDrawable(getResources().getDrawable(R.drawable.filt_toon));
        button_filters_text.setText("Toon");
    }


    // Listener que se encarga de desplegar la lista de filtros

    OnClickListener filtersListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            buttonsLayout.setVisibility(View.GONE);
            menuFiltersLayout.setVisibility(View.VISIBLE);
        }

    };

    // Listener que se encarga de mostrar la imagen tomada en pantalla completa

    OnClickListener capturedImageListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(pictureFile);
                File file = new File(uri.getPath());
                intent.setDataAndType(Uri.fromFile(file), "image/*");
                startActivity(intent);
            } catch(Exception e) {
            }
        }
    };


    // Función para elegir la cámara

    public void chooseCamera() {

        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {

                mCamera = Camera.open(cameraId);
                _cameraId = 0;
                mPicture = getPictureCallback();
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {

                mCamera = Camera.open(cameraId);
                _cameraId = 1;
                mPicture = getPictureCallback();
            }
        }

        mPreview.setCameraCode(_cameraId);

    }

    // Al minimzar la aplicación se libera la cámara

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    // Función que comprueba si tiene una cámara el dispositivo que estemos usando

    private boolean hasCamera(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    // Una vez que se toma una foto es necesario capturar los datos que se han generado
    // En esta función se realiza el tratamiento de dichos datos para guardarlos en memoria
    // Además se generará una miniatura que se mostrará una vez almacenada la imagen
    // con su correspondiente animacion de carga


    private PictureCallback getPictureCallback() {
        PictureCallback picture = new PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                try {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                File pictureFileOut = getOutputMediaFile();
                                pictureFile = pictureFileOut;

                                FileOutputStream out = null;

                                out = new FileOutputStream(pictureFileOut);

                                auxImage = view.capture();
                                auxImage.compress(Bitmap.CompressFormat.PNG, 100, out);

                                try {
                                    if (out != null) {
                                        out.close();
                                        try {
                                            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                            Uri uri = Uri.fromFile(pictureFileOut);
                                            mediaScanIntent.setData(uri);
                                            sendBroadcast(mediaScanIntent);
                                        } catch (Exception e) {
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Bitmap aux2 = getResizedBitmap(auxImage, capturedImage.getWidth(), capturedImage.getHeight());
                                aux2 = addBlackBorder(aux2, 2);
                                capturedImage.setImageBitmap(aux2);
                                progBar.setVisibility(View.GONE);

                            } catch (Exception e) {
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {

                }

                mPreview.refreshCamera(mCamera);
            }
        };
        return picture;
    }

    // Listener para el evento de hacer una imagen.

    OnClickListener captureListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            progBar.setVisibility(View.VISIBLE);
            mCamera.takePicture(null, null, mPicture);

        }
    };

    // Esta función maneja la animación de cambiarle el color al boton de tomar la imagen cuando se presiona

    public static void buttonEffect(View button){
        button.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        v.getBackground().setColorFilter(0xe0f47521, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        v.getBackground().clearColorFilter();
                        v.invalidate();
                        break;
                    }
                }
                return false;
            }
        });
    }

    // Generamos el fichero, con la ruta adecuada, el cual va almacenar la imagen una vez tomada

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory().getPath() , "DCIM/Camera");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }

    // Función que libera todos los recursos asociados a la cámara

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview); // Alabado sea StackOverflow
            mCamera.release();
            mCamera = null;
        }
    }

    // Función que redimensiona una imagen

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }


    // Función que añade un borde negro a una imagen

    private Bitmap addBlackBorder(Bitmap bmp, int borderSize) {
        Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(bmp, borderSize, borderSize, null);
        return bmpWithBorder;
    }

}
