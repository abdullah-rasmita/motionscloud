package com.motionscloud.trycamera2.support;

import java.io.FileDescriptor;
import java.io.IOException;

import android.media.MediaRecorder;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

public class MotionsCloudMediaRecorder extends MediaRecorder {
	
	public final static String RECORDERTAG = "com.motionscloud.trycamera2.support.MotionsCloudMediaRecorder";
	
	private LocalSocket localClient1 =  null;
	
	public MotionsCloudMediaRecorder(String serverName)
	{
		//create MediaRecorder
		super();
		//create localclient and connect it to server
		//Set  destination (a localsocket)
	    localClient1 = new LocalSocket();
	    try
	    {
	    	localClient1.connect(new LocalSocketAddress(serverName));
		}
	    catch(IOException e)
	    {
	    	Log.e(RECORDERTAG, "Error connecting to local server:" + e);
	    	localClient1 = null;
	    }
	}
	
	//disconnect localclient from server
	@Override
	public void release()
	{
		//release localClient
		releaseSocket();
		super.release();
	}
	
	@Override
	public void setOutputFile(FileDescriptor fd) throws UnsupportedOperationException
	{}
	
	public void setOutputFile()
	{
		super.setOutputFile(localClient1.getFileDescriptor());
	}
	
	private void releaseSocket()
	{
    	if(localClient1 != null)
    	{
    		try
            {
                localClient1.close();
                localClient1 = null;
            }
            catch(IOException e)
            {
            	Log.e(RECORDERTAG, "Client error in closing: " + e);
            }
    	}
    }
}
