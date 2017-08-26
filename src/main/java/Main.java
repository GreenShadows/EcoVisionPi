import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.io.FileOutputStream;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.util.Iterator;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


//The code will not run as it is not on a Pi. TODO:Add validation to check if running on Pi
import com.hopding.jrpicam.RPiCamera;
import com.hopding.jrpicam.exceptions.FailedToRunRaspistillException;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;

public class Main {
	public static void main(String[] args) {
	    String res = "";
	    final GpioController gpio = GpioFactory.getInstance();
	    final GpioPinDigitalOutput bioLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);
            final GpioPinDigitalOutput recyLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02);
    	    final GpioPinDigitalOutput toxicLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03);
	    try{
	        RPiCamera piCamera = new RPiCamera("./Pictures");
	        piCamera.setToDefaults();
		res = JSONParse(pushToWatson(compress(piCamera.takeStill(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ").format(new Date())))));
	    }
	    catch(Exception e){e.printStackTrace();}
		System.out.println(res);
		if(res=="Biodegradable"){bioLed.high();}
		else if(res=="Recyclable"){recyLed.high();}
		else if(res=="Toxic"){toxicLed.high();}
	}
	 private static String pushToWatson (File data){
	        VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
	        service.setApiKey("145b047be11f5059687578f4ca85325d23e0cdf8");

	        ClassifyImagesOptions options = new ClassifyImagesOptions
	                .Builder()
	                .images(data)//modified implementation as per SDK documentation.
	                .threshold(0.0001)
	                .classifierIds("WasteType_909361399")//This is required for our classifier to refect in its current state.
	                .build();
	        VisualClassification result = service.classify(options).execute();
	        return result.toString();
	   }
	 private static String JSONParse (String JSON) throws JSONException {
	        JSONObject watson=null;
	        double highestScore=0.0;
	        String wasteType=null;
	        try {
	            watson = new JSONObject(JSON);
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }
	        JSONArray images = watson.getJSONArray("images");
	        JSONObject image_1 = images.getJSONObject(0);
	        JSONArray classifiers = image_1.getJSONArray("classifiers");
	        JSONObject classifier_wasteType = classifiers.getJSONObject(0);
	        JSONArray classes = classifier_wasteType.getJSONArray("classes");
	        for (int i = 0; i < classes.length(); i+=1){
	            String class_name= classes.getJSONObject(i).getString("class");
	            double score= classes.getJSONObject(i).getDouble("score");
	            if (i==0){
	                highestScore=score;
	                wasteType=class_name;
	            }
	            if (score>highestScore){
	                highestScore=score;
	                wasteType=class_name;
	            }
	        };
	    return wasteType;    
	 }
	 private static File compress(File uncon) throws IOException{
          //File input = new File("digital_image_processing.jpg");
          BufferedImage image = ImageIO.read(uncon);

          File compressedImage = null; 
          OutputStream os = new FileOutputStream(compressedImage);

          Iterator<ImageWriter>writers = ImageIO.getImageWritersByFormatName("jpg");
          ImageWriter writer = (ImageWriter) writers.next();

          ImageOutputStream ios = ImageIO.createImageOutputStream(os);
          writer.setOutput(ios);

          ImageWriteParam param = writer.getDefaultWriteParam();
          // Check if canWriteCompressed is true
          if(param.canWriteCompressed()) {
              param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
              param.setCompressionQuality(0.05f);
         }
        writer.write(null, new IIOImage(image, null, null), param);
        return compressedImage;
    }

}
