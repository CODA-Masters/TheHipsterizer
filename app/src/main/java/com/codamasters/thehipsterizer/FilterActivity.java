package com.codamasters.thehipsterizer;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.WindowManager;

/**
 * Created by julio on 26/02/15.
 */
public class FilterActivity extends ActionBarActivity {

    static final int REQ_CODE_PICK_IMAGE = 1;
    private Bitmap galleryImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
    }

    public static Bitmap applyGaussianBlur(Bitmap src) {
        //set gaussian blur configuration
        double[][] GaussianBlurConfig = new double[][] {
                { 1, 2, 1 },
                { 2, 4, 2 },
                { 1, 2, 1 }
        };
        // create instance of Convolution matrix
        ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
        // Apply Configuration
        convMatrix.applyConfig(GaussianBlurConfig);
        convMatrix.Factor = 16;
        convMatrix.Offset = 0;
        //return out put bitmap
        return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
    }

    public void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQ_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case REQ_CODE_PICK_IMAGE:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();


                    galleryImage = BitmapFactory.decodeFile(filePath);
                }
        }
    }

}