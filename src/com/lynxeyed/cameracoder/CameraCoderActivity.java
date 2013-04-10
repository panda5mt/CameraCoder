package com.lynxeyed.cameracoder;

import com.lynxeyed.cameracoder.R.id;
//import android.content.Context; 
import android.app.Activity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.SurfaceView;


public class CameraCoderActivity extends Activity {
    /** Called when the activity is first created. */
    SeekBar    seek_S,seek_V;
	private SurfaceView   mSurfaceView;
    private TextView mTextViewMain,mTextViewSide;
    private ScrollView mScrollView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);  
        setContentView(R.layout.main);
        
        mSurfaceView = (SurfaceView) findViewById(id.surfaceView1);
        seek_S = (SeekBar) findViewById(id.seekBar1);
        seek_V = (SeekBar) findViewById(id.seekBar2);
        mScrollView = (ScrollView)findViewById(id.ScrollView01);
        mTextViewMain = (TextView)findViewById(id.textView_main);
        mTextViewSide = (TextView)findViewById(id.textView);
        new CameraPreview(this, mSurfaceView, seek_S, seek_V, mTextViewMain, mTextViewSide, mScrollView);
    }
}

