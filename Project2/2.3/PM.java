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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeletePolicyRequest;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.Statistic;
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
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.AddTagsRequest;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckResult;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;
import com.amazonaws.services.elasticloadbalancing.model.Listener;


public class PM {
	public static void main(String[] args) throws Exception {
		
		String testID = "pangdahaichanguo";
		String warmupID = "qingliangkekou";
		String snsARN = "arn:aws:sns:us-east-1:930143863167:dingzproject2scalealarm";
		
		//Load the Properties File with AWS Credentials
		Properties properties = new Properties();
		properties.load(PM.class.getResourceAsStream("/AwsCredentials.properties"));
		BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
		
		//Create the AmazonEC2Client object so we can call various APIs
		AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);
		
		//Create Load Generator Request
		RunInstancesRequest runLoadGeneratorRequest = new RunInstancesRequest();
		runLoadGeneratorRequest.withImageId("ami-7aba0c12")
		.withInstanceType("m3.medium")
		.withMinCount(1)
		.withMaxCount(1)
		.withKeyName("15619Project2")
		.withSecurityGroups("launch-wizard-2");
		
		//Launch Load Generator
		RunInstancesResult runLoadGeneratorResult = ec2.runInstances(runLoadGeneratorRequest);  
		Instance loadGenerator=runLoadGeneratorResult.getReservation().getInstances().get(0);
		String loadGeneratorID = loadGenerator.getInstanceId();
		
		//Create a Tag to the Instance 
		CreateTagsRequest createTagsRequest = new CreateTagsRequest();
		createTagsRequest.withResources(loadGeneratorID)
							.withTags(new Tag("Project", "2.3"));
		ec2.createTags(createTagsRequest);

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
						}
	        	
				}
			}
		}
		//sleep for one minute, ensuring LG web console is accessible via HTTP
		Thread.sleep(60*1000);
		//visit the load generator web interface and authenticate 
		URL loadGeneratorURL;
		try {
		
			String loadGeneratorAddr = "http://" + loadGeneratorDns + "/username?username=dingz";									            

			System.out.println(loadGeneratorAddr);
			loadGeneratorURL = new URL(loadGeneratorAddr);
            URLConnection conn = loadGeneratorURL.openConnection();

            // open the stream and put it into BufferedReader
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

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
		
		//Create ELB
		AmazonElasticLoadBalancingClient elb = new AmazonElasticLoadBalancingClient(bawsc);
		CreateLoadBalancerRequest elbRequest = new CreateLoadBalancerRequest();
		elbRequest.setLoadBalancerName("project2elb");
		//ELB redirects HTTP:80 requests to HTTP:80 on the instance
        List<Listener> listeners = new ArrayList<Listener>(1);
        listeners.add(new Listener("HTTP", 80, 80));
        elbRequest.withAvailabilityZones("us-east-1d");
        elbRequest.setListeners(listeners);
        CreateLoadBalancerResult elbResult=new CreateLoadBalancerResult();
        elbResult = elb.createLoadBalancer(elbRequest);
        //Configure the health check to activate the elb
        /* 15 secs between health checks of an individual instance
 		   5 secs during which no response means a failed health probe. 
		   3: Specifies the number of consecutive health probe failures required 
		   		before moving the instance to the Unhealthy state.
		   5: Specifies the number of consecutive health probe successes required 
		   		before moving the instance to the Healthy state. default: 10 
		*/
        HealthCheck healthCheck = new HealthCheck("HTTP:80/heartbeat?username=dingz", 15, 5, 3, 5);
        ConfigureHealthCheckRequest configureHealthCheckRequest = new ConfigureHealthCheckRequest("project2elb", healthCheck);
        ConfigureHealthCheckResult configureHealthCheckResult = new ConfigureHealthCheckResult();
        configureHealthCheckResult = elb.configureHealthCheck(configureHealthCheckRequest);
        //Add tag to the elb
        AddTagsRequest addTagsRequest = new AddTagsRequest();
        com.amazonaws.services.elasticloadbalancing.model.Tag elbTag = new com.amazonaws.services.elasticloadbalancing.model.Tag();
        elbTag.withKey("Project")
        .withValue("2.3");
        addTagsRequest.withLoadBalancerNames("project2elb").withTags(elbTag);
        elb.addTags(addTagsRequest);
        System.out.println("ELB created");
        //sleep for one minute, ensuring elb DNS can be read
        Thread.sleep(60*1000);
        String elbDNS = elbResult.getDNSName();
        
        //Create ASG with launch configuration, scaling policies and CloudWatch alarm
        AmazonAutoScalingClient asgClient = new AmazonAutoScalingClient(bawsc);
        AmazonCloudWatchClient cwClient = new AmazonCloudWatchClient(bawsc);
        //Create launch configuration 
        CreateLaunchConfigurationRequest lcReq = new CreateLaunchConfigurationRequest();
        //enable detailed monitoring
        InstanceMonitoring instanceMonitoring = new InstanceMonitoring();
        instanceMonitoring.setEnabled(true);
        lcReq.withImageId("ami-3c8f3a54")
        .withInstanceType("m1.small")
        .withInstanceMonitoring(instanceMonitoring)
        .withLaunchConfigurationName("project2lc")
        .withSecurityGroups("launch-wizard-2");
        asgClient.createLaunchConfiguration(lcReq);
        System.out.println("LC created");
        
        //Configure ASG
        CreateAutoScalingGroupRequest asgReq = new CreateAutoScalingGroupRequest();
        com.amazonaws.services.autoscaling.model.Tag asgTag = new com.amazonaws.services.autoscaling.model.Tag();
        asgTag.withKey("Project")
        .withValue("2.3"); 
        asgReq.withMinSize(1)
        .withMaxSize(7)
        .withDesiredCapacity(6)
        .withLaunchConfigurationName("project2lc")
        .withAutoScalingGroupName("project2asg")
        .withAvailabilityZones("us-east-1d")
        .withDefaultCooldown(60)
        .withLoadBalancerNames("project2elb")
        .withTags(asgTag);
        asgClient.createAutoScalingGroup(asgReq);
        System.out.println("ASG created");
        //Create Auto Scale Policies
        //scale out
        PutScalingPolicyRequest spOutReq = new PutScalingPolicyRequest();
        spOutReq.withAutoScalingGroupName("project2asg")
        .withAdjustmentType("ChangeInCapacity")
        .withCooldown(60)
        .withPolicyName("my-scale-out-policy")
        .withScalingAdjustment(2);//automatically adds a single instance to the auto scaling group
        //scale in
        PutScalingPolicyRequest spInReq = new PutScalingPolicyRequest();
        spInReq.withAutoScalingGroupName("project2asg")
        .withAdjustmentType("ChangeInCapacity")
        .withCooldown(60)
        .withPolicyName("my-scale-in-policy")
        .withScalingAdjustment(-1);//automatically removes a single instance from the auto scaling group
        
        PutScalingPolicyResult scaleOutRes = asgClient.putScalingPolicy(spOutReq);
        String addArn = scaleOutRes.getPolicyARN();//get the ARN of scale out policy
        //System.out.println("ARN of ScalingOutPolicy: " + addArn);
        PutScalingPolicyResult scaleInRes = asgClient.putScalingPolicy(spInReq);
        String rmArn = scaleInRes.getPolicyARN();//get the ARN of scale in policy
        //System.out.println("ARN of ScalingInPolicy: " + rmArn);
        System.out.println("Policies created");
        //Create CouldWatch Alarm
        //specify alarm dimension 
        Dimension dimension = new Dimension();
        dimension.withName("AutoScalingGroupName")
        		 .withValue("project2asg");
        //specify alarm actions 
        List addActions = new ArrayList();
        addActions.add(addArn);
        addActions.add(snsARN);//add SNS ARN to action list 
        List rmActions = new ArrayList();
        rmActions.add(rmArn);
        rmActions.add(snsARN);//add SNS ARN to action list 
        
        PutMetricAlarmRequest addReq = new PutMetricAlarmRequest();
        addReq.withAlarmName("AddCapacity")
        .withMetricName("CPUUtilization")
        .withNamespace("AWS/EC2")
        .withStatistic(Statistic.Average)
        .withPeriod(60)
        .withThreshold(80d)
        .withUnit(StandardUnit.Percent)
        .withComparisonOperator(ComparisonOperator.GreaterThanThreshold)
        .withDimensions(dimension)
        .withEvaluationPeriods(2)
        .withAlarmActions(addActions);
        
        PutMetricAlarmRequest rmReq = new PutMetricAlarmRequest();
        rmReq.withAlarmName("RemoveCapacity")
        .withMetricName("CPUUtilization")
        .withNamespace("AWS/EC2")
        .withStatistic(Statistic.Average)
        .withPeriod(60)
        .withThreshold(40d)
        .withUnit(StandardUnit.Percent)
        .withComparisonOperator(ComparisonOperator.LessThanThreshold)
        .withDimensions(dimension)
        .withEvaluationPeriods(2)
        .withAlarmActions(rmActions);
        
        cwClient.putMetricAlarm(addReq);
        cwClient.putMetricAlarm(rmReq);
        System.out.println("Alarm created");
        //ensure sufficient time for instances in ELB to get registered
        Thread.sleep(300*1000);
		//warm up the ELB 
		URL warmupURL;
		try {
			System.out.println("begin warm up the ELB");
			String warmupAddr = "http://" + loadGeneratorDns + "/warmup?dns=" +
					elbDNS+ "&testId=" + warmupID;
			System.out.println(warmupAddr);
			warmupURL = new URL(warmupAddr);
	        URLConnection conn = warmupURL.openConnection();

	        // open the stream and put it into BufferedReader
	        BufferedReader in = new BufferedReader(
	                  new InputStreamReader(conn.getInputStream()));

	        String inputLine;
	        while ((inputLine = in.readLine()) != null) {
	        	System.out.println(inputLine);
	        }
	        in.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
	        e.printStackTrace();
	    }
		//give plenty of time for the elb to warmup before entering testing 
		Thread.sleep(360*1000);
		System.out.println("warm up the ELB done");
		//visit the test page and initiate the test 
		URL testURL;
		try {
			System.out.println("begin phase 3");
			String testAddr = "http://" + loadGeneratorDns +"/begin-phase-3?dns=" + elbDNS + "&testId=" + testID;
			System.out.println(testAddr);
			testURL = new URL(testAddr);
	        URLConnection conn = testURL.openConnection();
	        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String inputLine;
	        while ((inputLine = in.readLine()) != null) {
		        	System.out.println(inputLine);
		    }
		    in.close();
	       
	        System.out.println("Phase 3 Done");

		} catch (MalformedURLException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
		Thread.sleep(60*1000);
		Boolean testRunning = true;
		//check the result page 
		do{
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
					if (inputLine.equals("Test completed")){
						testRunning = false;
					}
	            }
	            in.close();

	        } catch (MalformedURLException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}while(testRunning);
		
		System.out.println("Test Completed");
		System.out.printf("\nTerminate all resources? (y/N): ");
		BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
		String userResponse = bufferRead.readLine();
		if(userResponse.toLowerCase().equals("y"))
		{
			System.out.println("Termination begins");
			//re-edit the ASG properties to terminate all ASG instances
			System.out.println("Terminating ASG instances");
			UpdateAutoScalingGroupRequest updateAsgReq = new UpdateAutoScalingGroupRequest();
			updateAsgReq.withAutoScalingGroupName("project2asg")
						.withMinSize(0)
						.withMaxSize(0)
	        			.withDesiredCapacity(0);
			asgClient.updateAutoScalingGroup(updateAsgReq);
			Thread.sleep(300*1000);//give enough time for all ASG instances to shut down
			
			//terminate LG
			System.out.println("Terminating LG");
			TerminateInstancesRequest terminateLGReq = new TerminateInstancesRequest();
			List<String> instances = new ArrayList<String>();
			instances.add(loadGeneratorID);
			terminateLGReq.setInstanceIds(instances);
			ec2.terminateInstances(terminateLGReq);
			Thread.sleep(60*1000);
			
			//terminate ELB
			System.out.println("Terminating ELB");
			DeleteLoadBalancerRequest deleteElbReq = new DeleteLoadBalancerRequest("project2elb");
			elb.deleteLoadBalancer(deleteElbReq);
			Thread.sleep(60*1000);
			
			//terminate cloud watch alarms
			System.out.println("Terminating Alarms");
			DeleteAlarmsRequest deleteAlarmReq = new DeleteAlarmsRequest();
			deleteAlarmReq.withAlarmNames("AddCapacity","RemoveCapacity");
			cwClient.deleteAlarms(deleteAlarmReq);
			Thread.sleep(60*1000);
			
			//terminate ASG
			System.out.println("Terminating ASG");
			DeleteAutoScalingGroupRequest deleteAsgReq = new DeleteAutoScalingGroupRequest();
			deleteAsgReq.withAutoScalingGroupName("project2asg");
			asgClient.deleteAutoScalingGroup(deleteAsgReq);
			Thread.sleep(60*1000);
			
			//terminate LC
			System.out.println("Terminating LC");
			DeleteLaunchConfigurationRequest deleteLCReq = new DeleteLaunchConfigurationRequest();
			deleteLCReq.withLaunchConfigurationName("project2lc");
			asgClient.deleteLaunchConfiguration(deleteLCReq);
			Thread.sleep(60*1000);
			
			
			System.out.println("Termination done");

		}
		
		
		
	}
}