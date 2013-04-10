package com.lynxeyed.cameracoder;

import java.lang.Math;
import java.util.List;
import android.content.Context;  
import android.graphics.Bitmap;  
import android.graphics.Canvas;  
import android.hardware.Camera.*;  
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.view.MotionEvent;
import android.view.SurfaceView;  
import android.view.SurfaceHolder; 
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.util.Log;


public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;  
    private Bitmap bitmap;  
    private int[] rgb;  
    private int defaultFrontCameraId = -1, defaultBackCameraId = -1;
    private int width, height, counter;  
    private final String TAG = "CameraPreview"; 
    private TextView tv,tv_main;
    private ScrollView scrollView;
    private static double tHold_S, tHold_V, dh_ave;
    private static double dh_old = 0;
	private static byte findChar;
	byte[] previewData;
	long currentTime = System.currentTimeMillis();
	long passedTimeMillis;
	final int SECONDS_BETWEEN_PHOTOS = 60;	// Period: 60msec
	boolean alreadyRead = false;			// 検出した色はすでにデコード済みか
	 	
    private final PreviewCallback _previewCallback = new PreviewCallback() {
    	
    	public void onPreviewFrame(final byte[] data, Camera camera) {
    	
    		previewData = data;
    		decodeYUV420SP(rgb, previewData, width, height); 	
			bitmap.setPixels(rgb, 0, width, 0, 0, width, height);
			//tv_main.setText(String.valueOf(dh_ave)+"..."); // デバッグ用：色相表示

			// 描画  		
    		Canvas canv = mHolder.lockCanvas();  
    		canv.drawBitmap(bitmap, 0, 0, null);
    		mHolder.unlockCanvasAndPost(canv);

			byte bytes = stringGenerate(dh_ave);
			byte[] byarray = {bytes};
			if(bytes != 0){	
				tv_main.setText(tv_main.getText().toString() + new String(byarray));	// 文字データ表示
				tv_main.requestFocus();	
				scrollView.scrollTo(0, tv_main.getBottom());
			}

    	}
	};

	
	
	// constructor
	public CameraPreview(Context context, SurfaceView sv,SeekBar sb_S,SeekBar sb_V,TextView tv_main,TextView tv,final ScrollView scrollView) {  
		super(context);
		this.tv = tv;
		this.tv_main = tv_main;
		this.scrollView = scrollView;
		// Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
		mHolder=sv.getHolder();
		mHolder.addCallback(this);
		
		 // deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		sb_S.setMax(100);
		sb_V.setMax(100);
		sb_S.setProgress(50);
		sb_V.setProgress(50);
		tHold_V = sb_V.getProgress()/100.0;
		tHold_S = sb_S.getProgress()/100.0;
		
		scrollView.post(new Runnable() {
	        public void run() {
	            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
	        }
	    });

		sb_S.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}

			public void onProgressChanged(SeekBar sb_S, int progress,
					boolean fromUser) {
				tHold_S = progress / 100.0;
				Log.d(TAG,"S:onProgressChanged");
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
		});
		sb_V.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
			
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
			
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				tHold_V = progress / 100.0;
				Log.d(TAG,"V:onProgressChanged");
			}
		});
		
	}  

    public void surfaceCreated(SurfaceHolder holder) {

		// 利用可能なカメラの個数を取得
	    int numberOfCameras = Camera.getNumberOfCameras();
	 
	    // CameraInfoからカメラのidを取得
	    CameraInfo cameraInfo = new CameraInfo();
	    Log.d("NumOfCameras", cameraInfo.toString());
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
            	defaultBackCameraId = i;	//背面のカメラ
            }
            else if(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            	defaultFrontCameraId = i;	//前面のカメラ
            } 
        }
        
        if(defaultBackCameraId != -1){		// 背面カメラ優先
        	try{
        		mCamera = Camera.open(defaultBackCameraId);
        		mCamera.setPreviewCallback(_previewCallback); 
        	} catch (Exception e) {
                  Log.d(TAG, "Error setting camera preview: " + e.getMessage());
                  return;
        	}
        	return;
    	}else 
    		if(defaultFrontCameraId != -1){
    		try{
        		mCamera = Camera.open(defaultFrontCameraId);
        		mCamera.setPreviewCallback(_previewCallback); 
        	} catch (Exception e) {
                  Log.d(TAG, "Error setting camera preview: " + e.getMessage());
                  return;
        	}
        	return;
    	}
        return;
        
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
//    	mLooper = null;
    	mCamera.stopPreview();  
    	mCamera.setPreviewCallback(null);  
    	mCamera.release();  
    	mCamera = null;
    }
    
    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;
 
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
 
        int targetHeight = h;
 
        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
 
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

		//w = w / 2;
		//h = h / 2; // 演算量を減らすのに単純に(縦)1/2倍x(横)1/2倍 = (面積)1/4のサイズにする

		// カメラのプレビュー開始  
		Camera.Parameters parameters = mCamera.getParameters();
		
		List<Size> sizes = parameters.getSupportedPreviewSizes();
		Log.d("getSupportedPreviewSizes", sizes.toString());
        Size optimalSize = getOptimalPreviewSize(sizes, w, h);
 		
		w = optimalSize.width;
		h = optimalSize.height;
		width = w;  
		height = h; 
    		        
		bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);  
		rgb = new int[w * h];  
		
		
		try{
			parameters.setPictureSize(w, h);
			mCamera.setParameters(parameters);
		}catch (Exception e) {
		    parameters = mCamera.getParameters();
		}
		
		try{
			parameters.setPreviewSize(w, h); 
			mCamera.setParameters(parameters);
		}catch (Exception e) {
		    parameters = mCamera.getParameters();
		}
		
		try{
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
			mCamera.setParameters(parameters);
		}catch (Exception e) {
		    parameters = mCamera.getParameters();
		}
		mCamera.startPreview();
        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event){
    	Log.i("onTouchEvent()","STOP");
    	return true;
    }
       
   public void onPictureTaken(byte[] data, Camera c){
    	c.startPreview();
    }
   
   // 色データから文字を生成
   // vals:判定データ, vals_old:前回のデータ
   private byte stringGenerate(double mHue){
	   
	   if((280.0 <= mHue)||( 55.0 > mHue))      mHue = 0.0;		// red
	   else if(( 55.0 <= mHue)&&(125.0 > mHue)) mHue = 60.0;	    // yellow
	   else if((125.0 <= mHue)&&(185.0 > mHue)) mHue = 120.0;	// green
	   else if((185.0 <= mHue)&&(215.0 > mHue)) mHue = 180.0;	// cyan
	   else if((215.0 <= mHue)&&(227.0 > mHue)) mHue = 240.0;	// blue
	   else if((227.0 <= mHue)&&(280.0 > mHue)) mHue = 300.0;	// purple
	   else mHue = dh_old;
	   
	   
	   if(mHue != dh_old){
		   currentTime = System.currentTimeMillis();	//前回のサンプルデータと異なるならばカウンタをリセットし
		   alreadyRead = false;
		   Log.d(TAG,"mHueChanged mHue ="+mHue+" dh_old="+dh_old);
		   dh_old = mHue;
	   }
	   
	   passedTimeMillis = System.currentTimeMillis() - currentTime; // 検出した色が何msec継続しているか測定
	   Log.d(TAG,"passedTime = "+passedTimeMillis);
	   
	   if((passedTimeMillis >= SECONDS_BETWEEN_PHOTOS) && (alreadyRead == false)){	
		   if(mHue == 300.0){	//purpleなら無効データ
			   counter  = 0;
			   findChar = 0;
			   dh_old = mHue;				
		   }else if(mHue <= 180.0){			// red,yellow,green,cyan
			   findChar = (byte)((findChar & 0x3F) | (color2bin(mHue)<<6));
			   counter = counter + 2;
			   dh_old = mHue;
		   }else if(mHue == 240.0){			// blue = same as before 2bit data
			   findChar =  (byte) (((findChar & 0x30) << 2) | (findChar & 0x3F));
			   counter = counter + 2;
			   dh_old = mHue;
		   }
		   alreadyRead = true;
		   tv.setText("色相="+String.valueOf(mHue)+"..."+String.valueOf(counter));
	   }else{
		   return 0;
	   }
		      
	   if(counter == 8){
		   //dh_old = 300.0;	// 過去色データ無効化
		   counter  = 0;
		   if((0x20 <= findChar)&&(0x7f >= findChar)) // 表示可能なアルファベット+記号か？
		   {
			   return findChar;
		   }else
			   return 0;
	   }
	   findChar = (byte)(findChar >> 2);
	   return 0;
	}
   
   private byte color2bin(double color){
	   byte mChar = 0;
   
	   if(color == 0.0){			// red = 00b
		   mChar = 0x00; 
	   }
	   else if(color == 60.0){		// yellow = 01b
		   mChar = 0x01;
	   }	   
	   else if(color == 120.0){		// green = 10b
		   mChar = 0x02;
	   }
	   else if(color == 180.0){		// cyan = 11b
		   mChar = 0x03;
	   }
	   return mChar;
   }
   
	// YUV420 to BMP  
	static public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height){  
		double dh = 0.0,ds,dv;
	    double c;
	    double cmax,cmin;
	    
		final int frameSize = width * height;  
		for (int j = 0, yp = 0; j < height; j++) 
		{  
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;  
			for (int i = 0; i < width; i++, yp++) 
			{  
				int y = (0xff & ((int) yuv420sp[yp])) - 16;  
				if (y < 0) y = 0;  
				if ((i & 1) == 0) 
				{  
					v = (0xff & yuv420sp[uvp++]) - 128;  
					u = (0xff & yuv420sp[uvp++]) - 128; 
				}  
				int y1192 = 1192 * y;  
				int r = (y1192 + 1634 * v);  
				int g = (y1192 - 833 * v - 400 * u);  
				int b = (y1192 + 2066 * u);  
				if (r < 0) r = 0; else if (r > 262143) r = 262143;  
				if (g < 0) g = 0; else if (g > 262143) g = 262143;  
				if (b < 0) b = 0; else if (b > 262143) b = 262143;  
				
				//HSV変換
			    //RGB->HSV変換時のR,G,Bは 0.0～1.0
                double dr = r / 262143.0;
                double dg = g / 262143.0;
                double db = b / 262143.0;
            
                //hsvに変換
                if (dr >= dg)  cmax = dr; else cmax = dg;
                if (db >= cmax) cmax = db;
                if (dr <= dg) cmin = dr; else cmin = dg;
                if ( db <= cmin) cmin = db;
            
                dv  = cmax;
                c = cmax - cmin;
                if (cmax == 0.0)ds = 0.0; else ds = c/cmax;
                if (ds != 0.0){
                    if (dr == cmax)
                       dh  = 60.0 * (dg - db)/c;
                    else if (dg == cmax) dh  = 60.0 * (db - dr)/c + 120.0;
                    else if (db == cmax)
                        dh  = 60.0 * (dr - dg)/c + 240.0;
                    if (dh < 0.0) dh  = dh + 360.0;
                }
                if(ds < tHold_S){
            	   if(dv < tHold_V){
            		   dh = 0.0;
            		   ds = 0.0;
            		   dv = 0.0;
            	   }else{
            		   // 飽和(画像が白飛び)する場合の対策
            		   ds = tHold_S;
            		   if(dv < 1.0){
            			   	dh = 0;
            			   	dv = 0;
            		   }
            	   }   
               }
               
               if(dv > 0.65) dh_ave = (7.0 * dh_ave + 3.0 * dh) / 10.0;   

				//rgb空間に再変換            
                int inn = (int)Math.floor( dh  / 60.0 );   
                if(inn < 0)
                    inn *= -1;
                
                double fl = ( dh  / 60.0 ) - inn;
                //if((inn & 1)!=0) fl = 1 - fl; // if i is even
                if(inn == 2) fl = 1 - fl; // if i is even
                
                double p = dv * ( 1 - ds );
                double q = dv * ( 1 - ds  * fl );
                double t = dv * (1 - (1 - fl) * ds);
                
                ////計算結果のR,G,Bは0.0～1.0なので255倍
                dv = dv * 255.0;
                p = p * 255.0;
                q = q * 255.0;
                t = t * 255.0;
                
                switch( inn ) 
                {
                    case 0: r = (int)dv; g = (int) t;  b = (int)p;	break;
                    case 1: r = (int)q;  g = (int) dv; b = (int)p;	break;
                    case 2: r = (int)p;  g = (int) dv; b = (int)q;	break;
                    case 3: r = (int)p;  g = (int) q;  b = (int)dv;	break;
                    case 4: r = (int)t;  g = (int) p;  b = (int)dv;	break;
                    case 5: r = (int)dv; g = (int) p;  b = (int)q;	break;
                }
				rgb[yp] = 0xff000000 | ((r << 16) & 0xff0000) | ((g << 8) & 0xff00) | ((b) & 0xff);
			}
		}
	}
}
