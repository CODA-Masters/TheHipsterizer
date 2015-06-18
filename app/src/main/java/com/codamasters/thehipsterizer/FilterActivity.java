package com.codamasters.thehipsterizer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageHazeFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSketchFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageToonFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;


/**
 * Created by julio on 26/02/15.
 */

// Clase para la actividad de poder aplicar filtros a una imagen cargada de galeria

public class FilterActivity extends ActionBarActivity {

    static final int REQ_CODE_PICK_IMAGE = 1;
    private Bitmap galleryImage;

    private Bitmap auxImage, originalImage;
    private Context context;

    private GPUImageView mEffectView;
    private TextureRenderer mTexRenderer = new TextureRenderer();

    private Toolbar toolbar;

    private int position_image;

    private static int ROTATION_0 = 0;
    private static int ROTATION_90 = 1;
    private static int ROTATION_180 = 2;
    private static int ROTATION_270 = 3;


    // Realizamos la configuración de la actividad correspondientes
    // e iniciamos el intent para poder elegir una imagen de galeria

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_filter);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object

            // Asignamos la toolbar como nueva ActionBar y la configuramos con el botón de volver hacia atrás
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                    Intent intent = new Intent(FilterActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.animator.animation3, R.animator.animation4);
                }
            });
        }
        else{
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        mEffectView = (GPUImageView) findViewById(R.id.image_preview);
        mEffectView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);

        pickImage();

    }

    // Función para lanzar el intent de la galeria

    public void pickImage() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQ_CODE_PICK_IMAGE);
        }
        else {
            // Create intent to Open Image applications like Gallery, Google Photos
            Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            // Start the Intent
            startActivityForResult(galleryIntent, REQ_CODE_PICK_IMAGE);
        }
    }

    // Una vez seleccionada la imagen la cargamos en la vista y guardamos una imagen auxiliar
    // para sobre ella aplicar los filtros

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case REQ_CODE_PICK_IMAGE:
                try{
                    if (resultCode == RESULT_OK && imageReturnedIntent !=  null ) {
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Uri selectedImage = imageReturnedIntent.getData();
                            InputStream imageStream = null;
                            try {
                                imageStream = context.getContentResolver().openInputStream(selectedImage);

                                BitmapFactory.Options options = new BitmapFactory.Options();

                                options.inSampleSize = 2;
                                options.inPurgeable = true;
                                options.inInputShareable = true;
                                options.inJustDecodeBounds = false;
                                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                                galleryImage = BitmapFactory.decodeStream(imageStream,null,options);

                                auxImage = galleryImage;
                                originalImage = galleryImage;
                                mEffectView.setImage(galleryImage);

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                        else{
                            // Get the Image from data

                            Uri selectedImage = imageReturnedIntent.getData();
                            String[] filePathColumn = { MediaStore.Images.Media.DATA };

                            // Get the cursor
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            // Move to first row
                            cursor.moveToFirst();

                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            String imgDecodableString = cursor.getString(columnIndex);
                            cursor.close();

                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 2;
                            options.inPurgeable = true;
                            options.inInputShareable = true;
                            options.inJustDecodeBounds = false;
                            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                            galleryImage = BitmapFactory.decodeFile(imgDecodableString,options);

                            auxImage = galleryImage;
                            originalImage = galleryImage;
                            mEffectView.setImage(galleryImage);
                        }
                    }
                    else {
                        Toast.makeText(this, "You haven't picked Image",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                            .show();
                }
            break;
        }
    }

    // Lista de los onClickListeners correspondientes a cada filtro que se encarga de actualizar
    // el filtro activo en la GPUImageView

    public void filterNashville(View v) {
        mEffectView.setFilter(new IFNashvilleFilter(context));
    }

    public void filter1977(View v) {
        mEffectView.setFilter(new IF1977Filter(context));
    }

    public void filterValencia(View v) {
        mEffectView.setFilter(new IFValenciaFilter(context));
    }

    public void filterAmaro(View v) {
        mEffectView.setFilter(new IFAmaroFilter(context));
    }

    public void filterBrannan(View v) {
        mEffectView.setFilter(new IFBrannanFilter(context));
    }

    public void filterEarlyBird(View v) {
        mEffectView.setFilter(new IFEarlybirdFilter(context));
    }

    public void filterHefe(View v) {
        mEffectView.setFilter(new IFHefeFilter(context));
    }

    public void filterHudson(View v) {
        mEffectView.setFilter(new IFHudsonFilter(context));
    }

    public void filterInkwell(View v) {
        mEffectView.setFilter(new IFInkwellFilter(context));
    }

    public void filterLomofi(View v) {
        mEffectView.setFilter(new IFLomofiFilter(context));
    }

    public void filterLordKelvin(View v) {
        mEffectView.setFilter(new IFLordKelvinFilter(context));
    }

    public void filterNormal(View v) {
        mEffectView.setFilter(new IFNormalFilter(context));
    }

    public void filterRise(View v) {
        mEffectView.setFilter(new IFRiseFilter(context));
    }

    public void filterSierra(View v) {
        mEffectView.setFilter(new IFSierraFilter(context));
    }

    public void filterSutro(View v) {
        mEffectView.setFilter(new IFSutroFilter(context));
    }

    public void filterToaster(View v) {
        mEffectView.setFilter(new IFToasterFilter(context));
    }

    public void filterWalden(View v) {
        mEffectView.setFilter(new IFWaldenFilter(context));
    }

    public void filterXproll(View v) {
        mEffectView.setFilter(new IFXproIIFilter(context));
    }

    public void filterHaze(View v) {
        mEffectView.setFilter(new GPUImageHazeFilter());
    }

    public void filterSketch(View v){
        mEffectView.setFilter(new GPUImageSketchFilter());
    }

    public void filterToon(View v){
        mEffectView.setFilter(new GPUImageToonFilter());
    }

    // Creamos el menú
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_filter, menu);

        return true;
    }

    // manejamos las acciones de los botones del menú
    // En concreto la función de guardar la imagen filtrada

    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle presses on the action bar items
        ActionBar actionBar = getSupportActionBar();
        switch (item.getItemId()) {
            case R.id.action_save:
                try {

                    new Thread(new Runnable() {
                        public void run() {
                            try {

                                File pictureFile = getOutputMediaFile();

                                FileOutputStream out = null;

                                out = new FileOutputStream(pictureFile);

                                auxImage =  mEffectView.capture();

                                int center_X = auxImage.getWidth()/2;
                                int center_Y = auxImage.getHeight()/2;

                                Bitmap save_image  = Bitmap.createBitmap(auxImage, center_X-galleryImage.getWidth()/2, center_Y-galleryImage.getHeight()/2, galleryImage.getWidth(), galleryImage.getHeight());

                                save_image.compress(Bitmap.CompressFormat.PNG, 100, out);
                                try {
                                if (out != null) {
                                    out.close();
                                    try {
                                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                        Uri uri = Uri.fromFile(pictureFile);
                                        mediaScanIntent.setData(uri);
                                        sendBroadcast(mediaScanIntent);
                                    } catch (Exception e) {
                                    }
                                }
                                } catch (IOException e) {
                                        e.printStackTrace();
                                }


                            } catch(Exception e) {
                            }
                        }
                    }).start();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Toast.makeText(getApplicationContext(), "Imagen guardada",Toast.LENGTH_LONG).show();
                }

                return true;
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.action_rotar_izquierda:

                position_image--;
                if(position_image<0) position_image+=4;

                rotateImage();

                return true;

            case R.id.action_rotar_derecha:
                position_image++;
                position_image = position_image%4;

                rotateImage();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void rotateImage(){

        if(position_image == ROTATION_0) {
            galleryImage = originalImage;
        }
        else if(position_image == ROTATION_180) {
            galleryImage = RotateBitmap(originalImage, 180);
        }
        else if(position_image == ROTATION_90) {

            deleteViewImage();
            int width = galleryImage.getWidth();
            int height = galleryImage.getHeight();

            galleryImage = RotateBitmap(originalImage, 90);

            galleryImage = getResizedBitmap(galleryImage, height, width);

        }
        else if(position_image == ROTATION_270) {
            deleteViewImage();

            int width = galleryImage.getWidth();
            int height = galleryImage.getHeight();

            galleryImage = RotateBitmap(originalImage, 270);

            galleryImage = getResizedBitmap(galleryImage, height, width);

        }

        auxImage = galleryImage;
        mEffectView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);
        mEffectView.setImage(galleryImage);
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public void deleteViewImage(){
        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Bitmap bmp = Bitmap.createBitmap(originalImage.getWidth(), originalImage.getHeight(), conf);
        mEffectView.setImage(bmp);
    }

    // Función para obtener el fichero en el cual se guardará la imagen

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

    public Bitmap getResizedBitmap(Bitmap b, int newWidth, int newHeight) {
        int width = b.getWidth();
        int height = b.getHeight();

        // calculate the scale
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        // createa matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);

        // recreate the new Bitmap
        Bitmap result = Bitmap.createBitmap(b, 0, 0,
                width, height, matrix, true);

       return result;

    }

}
