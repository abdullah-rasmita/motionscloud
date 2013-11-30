package com.motionscloud.trycamera2;

import java.io.IOException;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.motionscloud.trycamera2.R.id;
import com.motionscloud.trycamera2.support.MotionsCloudMediaRecorder;

public class CameraActivity extends Activity {
	
	public final static String CAMERATAG = "com.example.TryCamera2.CameraActivity";

    static boolean isRecording = false;
    
    public final int currentAPI = android.os.Build.VERSION.SDK_INT;
    
	private Camera mCamera;
    private CameraPreview mPreview;
    private FrameLayout preview;
    
    private MotionsCloudMediaRecorder mMediaRecorder;
    
    private LocalSocket localclient1 = null;
    private String serverName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	//has not been tested
	@Override
	protected void onStart()
	{
		super.onStart();
		Intent intent = this.getIntent();
		if(intent.hasExtra("serverName"))
		{
			serverName = intent.getStringExtra("serverName");
			localclient1 = new LocalSocket();
		    try
		    {
		    	localclient1.connect(new LocalSocketAddress(serverName));
				localclient1.close();
				localclient1 = null;
			}
		    catch(IOException e)
		    {
		    	//notify user -- No server
		    	Log.e(CAMERATAG, "Error connecting to local server:" + e);
		    	finish();
		    	return;
		    }
		}
		else
		{
			//notify user -- wrong intent
	    	Log.e(CAMERATAG, "No server information in intent");
	    	finish();
	    	return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		setContentView(R.layout.activity_camera);
		
		//API level 11
		if(currentAPI >= 11)
		{
			ActionBar actionBar = getActionBar();
			actionBar.hide();
		}
		else
		{
			//ActionBar actionBar = getSupportActionBar();
			//actionBar.hide();
		}
		
		// Create an instance of Camera
        mCamera = getCameraInstance();
        
        if(mCamera != null)
        {
        	// Create our Preview view and set it as the content of our activity.
        	preview = (FrameLayout) findViewById(R.id.camera_preview);
        	mPreview = this.new CameraPreview(this, mCamera, preview);
        	preview.addView(mPreview);
        	processButton();
        }
	}
	
	@Override
    protected void onPause() 
	{
        super.onPause();
        if(isRecording)
        {
        	recordStop();
        }
        releaseMediaRecorder();
        Button buttonRecord = (Button) findViewById(id.button_record);
        buttonRecord.setText("Capture");
        releaseCamera();              // release the camera immediately on pause event
    }
	
	//still has some problems 
	@Override
	protected void onStop()
	{
		super.onStop();
		//send intent to stop WriterService
		Intent intent = new Intent(this, WriterService.class);
		intent.putExtra("action", "stop");
		startService(intent);
	}

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
        	mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
    
    private void recordStop()
    {	
		// stop recording and release camera
        mMediaRecorder.stop();  // stop the recording
        
        //Set that we are done recording
        isRecording = false;
    }
    
    private void processButton()
    {
    	final Button buttonRecord = (Button) findViewById(id.button_record);
    	buttonRecord.setOnClickListener(
    		    new View.OnClickListener() {
    		        @Override
    		        public void onClick(View v) {
    		            if (isRecording) 
    		            {
    		                recordStop();
    		                releaseMediaRecorder();
	            			buttonRecord.setText("Capture"); 
    		            }
    		            else 
    		            {
    		                // initialize video camera
    		            		if (prepareVideoRecorder(mCamera))
    		            		{
    		            			// Camera is available and unlocked, MediaRecorder is prepared,
    		            			// now you can start recording
    		            			mMediaRecorder.start();

    		            			// inform the user that recording has started
    		            			buttonRecord.setText("Stop");
    		            			isRecording = true;
    		            		}
    		            		else 
    		            		{
    		            			// prepare didn't work, release the camera
    		            			releaseMediaRecorder();
    		            			isRecording = false;
    		                    // inform user
    		            		}
    		            }
    		        }
    		    }
    		);
    }
    
	public static Camera getCameraInstance(){
	    Camera c = null;
	    try {
	        c = Camera.open(); // attempt to get a Camera instance
	    }
	    catch (Exception e)
	    {
	        // Camera is not available (in use or does not exist)
            Log.d(CAMERATAG, "Error starting camera: " + e.getMessage());
	    }
	    return c; // returns null if camera is unavailable
	}
	
	private boolean prepareVideoRecorder(Camera mCamera){

	    mMediaRecorder = new MotionsCloudMediaRecorder(serverName);

	    // Step 1: Unlock and set camera to MediaRecorder
	    mCamera.unlock();
	    mMediaRecorder.setCamera(mCamera);

	    // Step 2: Set sources
	    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

	    // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
	    //mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
	    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
	    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
	    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
	    
	    
	    // Step 4: Set  destination (a localsocket that has been defined in the constructor)
	    mMediaRecorder.setOutputFile();

	    // Step 5: Set the preview output
	    mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

	    // Step 6: Prepare configured MediaRecorder
	    try {
	        mMediaRecorder.prepare();
	    } catch (IllegalStateException e) {
	        Log.d(CAMERATAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
	        releaseMediaRecorder();
	        return false;
	    } catch (IOException e) {
	        Log.d(CAMERATAG, "IOException preparing MediaRecorder: " + e.getMessage());
	        releaseMediaRecorder();
	        return false;
	    }
	    	    
	    return true;
	}
	
	/** A basic Camera preview class */
	public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	    private SurfaceHolder mHolder;
	    private Camera mCamera;
	    private FrameLayout preview;

	    @SuppressWarnings("deprecation")
		public CameraPreview(Context context, Camera camera, FrameLayout framelayout) {
	        super(context);
	        mCamera = camera;
	        preview = framelayout;

	        // Install a SurfaceHolder.Callback so we get notified when the
	        // underlying surface is created and destroyed.
	        mHolder = getHolder();
	        mHolder.addCallback(this);
	        // deprecated setting, but required on Android versions prior to 3.0
	        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	    }

	    public void surfaceCreated(SurfaceHolder holder) {
	        // The Surface has been created, now tell the camera where to draw the preview.
	        try {
	            mCamera.setPreviewDisplay(holder);
	            mCamera.startPreview();
	        } catch (IOException e) {
	            Log.d(CAMERATAG, "Error setting camera preview: " + e.getMessage());
	        }
	    }

	    public void surfaceDestroyed(SurfaceHolder holder) {
	        // empty. Take care of releasing the Camera preview in your activity.
	    }

	    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
	        // If your preview can change or rotate, take care of those events here.
	        // Make sure to stop the preview before resizing or reformatting it.

	        //if (mHolder.getSurface() == null)
	        if (holder.getSurface() == null)
	        {
	          // preview surface does not exist
	          return;
	        }

	        // stop preview before making changes
	        try {
	            mCamera.stopPreview();
	        } catch (Exception e){
	          // ignore: tried to stop a non-existent preview
	        }

	        // set preview size and make any resize, rotate or
	        // reformatting changes here

	        // start preview with new settings
	        try {


	            /**Set preview size**/
        		Camera.Size size = getBestPreviewSize(mPreview.getWidth(),mPreview.getHeight());
        		//set camera preview size
	        	Camera.Parameters parameters = mCamera.getParameters();
        		parameters.setPreviewSize(size.width, size.height);
        		mCamera.setParameters(parameters);
        		
        		//set surfaceview size
        		float mult_factor = 1;
        		/* optional -- change magnification factor*/
	        	mult_factor = Math.min((float)preview.getWidth()/size.width, (float)preview.getHeight()/size.height);
        	    
            	FrameLayout.LayoutParams mPreviewParams = (FrameLayout.LayoutParams) mPreview.getLayoutParams();
        		mPreviewParams.width = (int)(mult_factor * size.width);
        		mPreviewParams.height = (int)(mult_factor * size.height);
            	mPreviewParams.gravity = 0x11;
            	mPreview.setLayoutParams(mPreviewParams);
            	/**end set preview size**/
            	
	            mCamera.setPreviewDisplay(holder);
        	    mCamera.startPreview();

	        } catch (Exception e){
	            Log.d(CAMERATAG, "Error starting camera preview: " + e.getMessage());
	        }
	    }

		
		private Camera.Size getBestPreviewSize(int width, int height)
		{
			Camera.Size result=null;
			Camera.Parameters p = mCamera.getParameters();
			for (Camera.Size size : p.getSupportedPreviewSizes()) 
			{	
				if (size.width <= width && size.height <= height)
				{
					if (result == null) 
					{
						result=size;
					} 
					else
					{
						int resultArea = result.width*result.height;
						int newArea = size.width*size.height;
						if (newArea > resultArea) 
						{
							result=size;
						}
					}
				}
			}
		    return result;
		}
	}
}