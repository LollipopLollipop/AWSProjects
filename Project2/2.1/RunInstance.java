import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;


public class RunInstance {
	public static void main(String[] args) throws Exception {
		String testID = "pailingluhaha";
		float cRps = 0;
		int instanceCount = 0;
		System.out.println("program started at " + System.currentTimeMillis());
		//Load the Properties File with AWS Credentials
		Properties properties = new Properties();
		properties.load(RunInstance.class.getResourceAsStream("/AwsCredentials.properties"));
		 
		BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
		 
		// Create the AmazonEC2Client object so we can call various APIs.
		AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);
		 
		//Create Instance Request
		RunInstancesRequest runLoadGeneratorRequest = new RunInstancesRequest();

		//Configure Instance Request
		runLoadGeneratorRequest.withImageId("ami-1810b270")
		.withInstanceType("m3.medium")
		.withMinCount(1)
		.withMaxCount(1)
		.withKeyName("15619Project2")
		.withSecurityGroups("launch-wizard-2");
		
		//Launch Instance
		RunInstancesResult runLoadGeneratorResult = ec2.runInstances(runLoadGeneratorRequest);  
		
		//Return the Object Reference of the Instance just Launched
		Instance loadGenerator=runLoadGeneratorResult.getReservation().getInstances().get(0);
		System.out.println("load genertor launched at " + System.currentTimeMillis());
		String loadGeneratorID = loadGenerator.getInstanceId();
		//Create a Tag to the Instance 
		CreateTagsRequest createTagsRequest = new CreateTagsRequest();
		createTagsRequest.withResources(loadGeneratorID)
							.withTags(new Tag("Project", "2.1"));
		ec2.createTags(createTagsRequest);
		
		//Print Instance ID
		System.out.println("Just launched the load generator with ID: " + loadGeneratorID);
		
		//Record the DNS name of the load generator once the instance is running 
		String loadGeneratorDns = null;
		
		Boolean isPending = true;
		while(isPending){
			DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
			List<Reservation> reservations = describeInstancesRequest.getReservations();
			for (Reservation reservation : reservations) {
				for (Instance instance : reservation.getInstances()) {
					if (instance.getInstanceId().equals(loadGeneratorID))
						if(instance.getState().getName().equals("running")){
							isPending = false;//exit the loop once the instance is successfully running
							loadGeneratorDns = instance.getPublicDnsName();
							System.out.println("load generator dns: " + loadGeneratorDns);//for debugging purpose
							System.out.println("load genertor got running at " + System.currentTimeMillis());
						}
	        	
				}
			}
		}
        
        do{
        	//launch data center instance 
        	RunInstancesRequest runDataCenterInstanceRequest = new RunInstancesRequest();
			runDataCenterInstanceRequest.withImageId("ami-324ae85a")
			.withInstanceType("m3.medium")
			.withMinCount(1)
			.withMaxCount(1)
			.withKeyName("15619Project2")
			.withSecurityGroups("launch-wizard-2");
			RunInstancesResult runDataCenterInstancResult = ec2.runInstances(runDataCenterInstanceRequest);
			Instance dataCenterInstance=runDataCenterInstancResult.getReservation().getInstances().get(0);
			instanceCount++;
			System.out.println("data center instance " + instanceCount +" launched at " + System.currentTimeMillis());
			//create a tag
			String dataCenterInstanceID = dataCenterInstance.getInstanceId();
			createTagsRequest.withResources(dataCenterInstanceID)
								.withTags(new Tag("Project", "2.1"));
			ec2.createTags(createTagsRequest);
			
			System.out.println("Just launched data center instance " + instanceCount + " with ID: " + dataCenterInstanceID);
			
			//Record the DNS name of the data center instance once the instance is running 
			String dataCenterDns = null;
			isPending = true;
			while(isPending){
				DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
				List<Reservation> reservations = describeInstancesRequest.getReservations();
				for (Reservation reservation : reservations) {
					for (Instance instance : reservation.getInstances()) {
						if (instance.getInstanceId().equals(dataCenterInstanceID))
							if(instance.getState().getName().equals("running")){
								isPending = false;
								dataCenterDns = instance.getPublicDnsName();
								System.out.println("instance dns: " + dataCenterDns);//for debugging purpose
								System.out.println("data center instance " + instanceCount +" got running at " + System.currentTimeMillis());
							}
		        	
					}
				}
			}
			Thread.sleep(30*1000);
			//visit the load generator web interface and authenticate 
			URL loadGeneratorURL;
			try {
				String loadGeneratorAddr = "http://" + loadGeneratorDns + "/username?username=dingz";
				System.out.println(loadGeneratorAddr);
				loadGeneratorURL = new URL(loadGeneratorAddr);
	            URLConnection conn = loadGeneratorURL.openConnection();

	            // open the stream and put it into BufferedReader
	            BufferedReader in = new BufferedReader(
	                               new InputStreamReader(conn.getInputStream()));

	            String inputLine;
	            while ((inputLine = in.readLine()) != null) {
	                    System.out.println(inputLine);
	            }
	            in.close();

	            System.out.println("Load Generator Authen Done");

	        } catch (MalformedURLException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			//visit the data center instance web interface and authenticate 
			URL dataCenterInstanceURL;
			try {
				System.out.println("Open data center instance " + instanceCount + " web interface");
				String dataCenterInstanceAddr = "http://" + dataCenterDns + "/username?username=dingz";
				System.out.println(dataCenterInstanceAddr);
				dataCenterInstanceURL = new URL(dataCenterInstanceAddr);
	            URLConnection conn = dataCenterInstanceURL.openConnection();

	            // open the stream and put it into BufferedReader
	            BufferedReader in = new BufferedReader(
	                               new InputStreamReader(conn.getInputStream()));

	            String inputLine;
	            while ((inputLine = in.readLine()) != null) {
	                    System.out.println(inputLine);
	            }
	            in.close();

	            System.out.println("Data Center Instance " + instanceCount + " Authen Done");

	        } catch (MalformedURLException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			Thread.sleep(30*1000);
			//visit the test page and initiate the test 
			URL testURL;
			try {
				System.out.println("Fill data center instance test form");
				String testAddr = "http://" + loadGeneratorDns +"/part/one/i/want/more?dns=" + dataCenterDns + "&testId=" + testID;
				System.out.println(testAddr);
				testURL = new URL(testAddr);
	            URLConnection conn = testURL.openConnection();
	            conn.setConnectTimeout(30*1000);
	            try {
	            	BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	            	String inputLine;
		            while ((inputLine = in.readLine()) != null) {
		                    System.out.println(inputLine);
		            }
		            in.close();
	            } catch (SocketTimeoutException e) {
	            	e.printStackTrace();
	            }
	            System.out.println(" Test Form Done");

	        } catch (MalformedURLException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			
			//wait for one minute 
			Thread.sleep(60*1000);
			//check the result page and parse output 
			URL resultURL; 
			try {
				System.out.println("Open results page");
				String resultAddr = "http://"+loadGeneratorDns+"/view-logs?name=result_dingz_"+testID+".txt";
				System.out.println(resultAddr);
				resultURL = new URL(resultAddr);
	            URLConnection conn = resultURL.openConnection();
	            // open the stream and put it into BufferedReader
	            BufferedReader in = new BufferedReader(
	                               new InputStreamReader(conn.getInputStream()));
	            String inputLine = null;
	            while ((inputLine = in.readLine()) != null) {
	            	System.out.println(inputLine);
					inputLine = inputLine.trim();//remove leading and trailing whitespace
					int strLength = inputLine.split(" ").length;
					//System.out.println("how many elements after splitting tab: " + strLength);
					String[] elements = new String[strLength];
					elements = inputLine.split(" ");
					if(elements[strLength-1].equals("(mean)")) {
						cRps += Float.parseFloat(elements[strLength-3]);
						System.out.println("counting");
					}
					else if(elements[0].equals("minute")) {
						cRps = 0;
						System.out.println("resetting");
					}
	            }
	            System.out.println("cRps is " + cRps);
	            in.close();

	            System.out.println("Done");

	        } catch (MalformedURLException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}while(cRps < 3600);
        System.out.println("program ended at " + System.currentTimeMillis());
        System.out.println(instanceCount + " instances were created");
	}
}