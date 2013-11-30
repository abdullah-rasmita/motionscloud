package com.motionscloud.trycamera2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.IntentService;
import android.content.Intent;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class WriterService extends IntentService
{
	public final static String SENDERTAG = "com.example.TryCamera2.WriterService";
	
	static boolean toFile = false;
	static boolean newPart = true;
	static boolean isSaved = false;
	static boolean isReadySave = false;
	
	private LocalServerSocket localServer = null;
	static String ServerName = "com.motionscloud.trycamera2.localserver";
	private LocalSocket client = null;
	
	private File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM), "MotionsCloud");
	private int videoNumber = 0;
	private int partNumber = -1;
	
	private boolean stop = false;
	
	public WriterService()
	{
		super("WriterService");
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.hasExtra("action")) 
        {
        	// Set the stopping flag
            stop = intent.getStringExtra("action").equals("stop");
            
            //in case server waiting for client
            if(client == null)
            {
            	LocalSocket localClient1 = new LocalSocket();
    		    try
    		    {
    		    	localClient1.connect(new LocalSocketAddress(ServerName));
    				localClient1.close();
    				localClient1 = null;
    			}
    		    catch(IOException e)
    		    {}
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
	
	@Override
	protected void onHandleIntent(Intent intent)
	{
		//check if it is a stop request
		if (intent.hasExtra("action")) 
		{
            stop = intent.getStringExtra("action").equals("stop");
            if (stop) 
            {
                try
                {
                	localServer.close();
                }
                catch(IOException e)
                {
                	Log.e(SENDERTAG, "Server cannot be closed");
                }
                
            	//clean up
            	localServer = null;
            	client = null;
       	 		
            	newPart = true;
       	 		isReadySave = false;
       	 		toFile = false;
       	 		
       	 		videoNumber = 0;
       	 		partNumber = -1;
       	 		
       	 		stop = false;
                return;
            }
        }
		//check if server is running, if not create localserver
		createLocalServer();
	}
	
	public static String createFileName(String serverSign, int vid_num, int part_num)
	{
		String mediaFileName;
	 	mediaFileName = serverSign + "_VID_" + vid_num + "_PART_" + part_num +  ".mp4";
	 	return mediaFileName;
	}
	
	private String createServerSign(File mediaDir)
	{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String serverSign = mediaDir.getPath() + File.separator + "SER_"+ timeStamp;
		return serverSign;
	}
	
	private void createLocalServer()
	{
		int len;
		
		InputStream in;
		FileOutputStream fileOut;
		File mediaFile;
		
		Intent intent;
		
		//create local server --  if it is not been created
		if(localServer == null)
		{
			try
			{
				localServer = new LocalServerSocket(ServerName);
			}
			catch(IOException e)
			{
				Log.e(SENDERTAG, "Server error: " + e);
				return;
			}
		}
		
		//create video directory -- if it is not been created
		if (! mediaStorageDir.exists())
	 	{
	 		if (! mediaStorageDir.mkdirs())
	 		{
	 			Log.d(SENDERTAG, "failed to create directory");
	 			return;
	 		}
	 	}
		
		//create video signature
		String serverSign = createServerSign(mediaStorageDir);
		
		//tell mainactivity that preparation is done
		intent = new Intent("server_ready");
		intent.putExtra("status",1);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		
		//start the server activity
		while(true)
		{
			//prepare inputStream
   	 		len = 0;
   	 		in = null;
   	 		
   	 		//prepare output stream
   	 		fileOut = null;
   	 		client = null;
			
   	 		// Start server and create local socket to connect to client
   	 		try 
   	 		{
   	 			client = localServer.accept();
   	 		}
   	 		catch (IOException e) 
   	 		{
   	 			Log.e(SENDERTAG, "Accept failed: " + e);
   	 			return;
   	 		} 	
   	 		
   	 		//clean up
	 		newPart = true;
	 		isReadySave = false;
	 		toFile = false;
   	 		
   	 		if(stop)
   	 			return;
   	 		else
   	 		{
   	 			//start new service that manage data flow -- DataManager
   	 			intent =  new Intent(this, DataManager.class);
   	 			intent.putExtra("serverSign", serverSign);
   	 			intent.putExtra("videoNum", videoNumber);
   	 			startService(intent);
   			
   	 			//wait until it is ready to save file
   	 			while(!isReadySave)
   	 			{
   	 				try
   	 				{
   	 					Thread.sleep(100);
   	 				}
   	 				catch(InterruptedException e)
   	 				{}
   	 			}
   			
   	 			
   	 			try
   	 			{
   	 				in = client.getInputStream();
   	 				byte[] buffer = new byte[1024];
	 				while ((len = in.read(buffer)) >= 0 && !stop) 
	 				{
	 					if(newPart)
	 					{
	 						partNumber = partNumber + 1;
	 						if(toFile)
	 						{
	 							//create file
	 							mediaFile = new File(createFileName(serverSign, videoNumber, partNumber));
	 							fileOut = new FileOutputStream(mediaFile);
	 						}
	 						newPart = false;
	 					}
	 					fileOut.write(buffer, 0, len);
	 					Log.i(SENDERTAG, "Writing "+len+" bytes");
	 				}
   	 			}
   	 			catch(Exception e)
   	 			{
   	 				Log.e(SENDERTAG, "failed to write" + e);
   	 			}
   	 			finally
   	 			{
   	 				videoNumber = videoNumber + 1;
   	 				partNumber = -1;
   	 				//set isSaved to true
   	 				isSaved = true;
   	 				try 
   	 				{
   	 					if(fileOut != null)
   	 					{
   	 						fileOut.close();
   	 					}
   	 					if (in != null) 
   	 					{
   	 						in.close();
   	 					}
   	 				} 
   	 				catch (Exception e2) 
   	 				{
   	 					Log.e(SENDERTAG, "failed to writexx" + e2);
   	 				}
   	 			}
   	 			if(stop)
   	 				return;
   	 		}
		}
	}
}