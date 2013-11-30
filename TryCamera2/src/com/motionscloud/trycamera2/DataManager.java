package com.motionscloud.trycamera2;

import java.io.File;

import android.app.IntentService;
import android.content.Intent;

//-- videoNum should be changed by WriterService
//-- partNum is controlled by DataManager by firing signal if it has to be increased
//-- DataManager should be stopped by itself, WriterService should be stopped by signal from CameraActivity (onStop)
//-- SenderService is launched to send file written by WriterService
//-- There is a possibility that connection lost so SenderService fails. -- use SenderService(clear)
//-- For broadcasting, WriterService should write directly to internet

public class DataManager extends IntentService 
{
	private int videoNum = -1;
	private int partNum = 0;
	private String serverSign;
	
	public DataManager()
	{
		super("DataManager");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		String videoName;
		File file0;
		
		//get server Signature
		serverSign = intent.getStringExtra("serverSign");
		
		//get videoNum
		videoNum = intent.getIntExtra("videoNum", -1);
		
		partNum = 0;
		
		if(videoNum == -1)
			return;
		else
			videoName = WriterService.createFileName(serverSign, videoNum, partNum);
		
		file0 = new File(videoName);
		
		//check network 
		//decide file size (only in the beginning) -- if no connection file size = -1
		
		//set the destination -- for now only toFile is allowed
		WriterService.toFile  = true;
		
		//set that it is ready to write to destination
		WriterService.isReadySave = true;
		
		//Activites to be done with timer -> use Thread.sleep(10)
		while(true)
		{
			//wait for 100 ms
			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException e)
			{}
			
			//check connection

			//if all data has been saved
				//change the name of the last file to <name>_FINAL 
				//start SenderService(clear)--> SenderService will be in foreground, it will send all remaining file until _FINAL and do the checking etc
				//set isReadyRecording  =  true -- no need anymore
				//break from loop
			if(WriterService.isSaved)
			{
				WriterService.isSaved = false;
				break;
			}
			
			//Notify WriterService if there are changes in data destination
			
			//for testing --  write to new files every 10 KB
			if(file0.length() >= 10000)
			{
				WriterService.newPart = true;
				partNum = partNum + 1;
				videoName = WriterService.createFileName(serverSign, videoNum, partNum);
				file0 = new File(videoName);
			}
			
			//else
				//Start SenderService if there is file to be sent
		}
	}
}
