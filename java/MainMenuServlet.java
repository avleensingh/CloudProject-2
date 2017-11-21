/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.server;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.db.FileInformationOfDatabase;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.io.IOUtils;

@MultipartConfig
public class MainMenuServlet extends HttpServlet {


    private static final String SUFFIX = "/";   
    private static boolean flagForNewFolder=true;
    String bucketName = "";
    
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException 
    {
        ArrayList<String> arrayListOfFile;
        arrayListOfFile = new ArrayList<>();
        
      String userName= request.getSession(false).getAttribute("UserName").toString();
      
        System.out.println("username from session is:"+userName);
      
      AWSCredentials credentials = new BasicAWSCredentials(
				"", 
				"");
           
            AmazonS3 s3client = new AmazonS3Client(credentials);
            s3client.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1));
            s3client.setEndpoint("s3.ap-south-1.amazonaws.com");
        
            System.out.println("Inside MainMenuServlet::::::::::::");
//        CloudClass cs=new CloudClass();
            
        List<Part> fileParts; // Retrieves <input type="file" name="file" multiple="true">
        
        
        fileParts = request.getParts().stream().filter(part -> "file".equals(part.getName())).collect(Collectors.toList());

        
        ObjectListing listofFolders;
        listofFolders = s3client.listObjects(bucketName);

      
       List<S3ObjectSummary> summaries = listofFolders.getObjectSummaries();
       listofFolders = s3client.listNextBatchOfObjects (listofFolders);
       summaries.addAll (listofFolders.getObjectSummaries());
       
        for (S3ObjectSummary objectSummary : summaries) 
       {
	    String uniqueId = objectSummary.getKey();
            if(uniqueId.equals(userName))
            {   
                flagForNewFolder=false;
                break;
                
            }
            else
            {
                System.out.println("check2");
                flagForNewFolder=true;
            }
       }  
       
       if(flagForNewFolder)
        createFolder(bucketName, userName, s3client);
       
          for(Part filePart : fileParts) 
         { 
          String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
         
          InputStream fileContent = filePart.getInputStream();
                
          StringWriter writer = new StringWriter();
          IOUtils.copy(fileContent,writer,"UTF-8");
          String theFileContent= writer.toString();
        
          File file = new File(fileName);         
          file.createNewFile();

          FileWriter fwriter = new FileWriter(file);
          fwriter.write(theFileContent);
          fwriter.close();
          
          String completeFilePath = userName + SUFFIX + fileName;
          
          s3client.putObject(new PutObjectRequest(bucketName,completeFilePath,file));
          
          arrayListOfFile.add(fileName);
          
        } 
        
        FileInformationOfDatabase info=new FileInformationOfDatabase();
        info.addDataIntoDatabase(userName,arrayListOfFile);
        
        
   
    }   
     
   
 public static void createFolder(String bucketName, String folderName, AmazonS3 client) {

	ObjectMetadata metadata = new ObjectMetadata();
	metadata.setContentLength(0);
	
	InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
	
	PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
				folderName + SUFFIX, emptyContent, metadata);
	
	client.putObject(putObjectRequest);
}
     

}
         
        
    
    
    


