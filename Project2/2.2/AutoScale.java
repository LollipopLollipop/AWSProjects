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
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
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
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;
import com.amazonaws.services.elasticloadbalancing.model.Listener;


public class AutoScale {
	public static void main(String[] args) throws Exception {
		
		String testID = "pianerchuanchaojihaochi";
		String warmupID = "duiyapianerchuanbusuo";
		
		//Load the Properties File with AWS Credentials
		Properties properties = new Properties();
		properties.load(AutoScale.class.getResourceAsStream("/AwsCredentials.properties"));
		BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
		 
		// Create the AmazonEC2Client object so we can call various APIs
		AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);
		
		//Create Load Generator Request
		RunInstancesRequest runLoadGeneratorRequest = new RunInstancesRequest();
		runLoadGeneratorRequest.withImageId("ami-562d853e")
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
							.withTags(new Tag("Project", "2.2"));
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
		//sleep for one minute, ensuring LG web console is accessible
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
        List<Listener> listeners = new ArrayList<Listener>(1);
        listeners.add(new Listener("HTTP", 80, 80));
        elbRequest.withAvailabilityZones("us-east-1a","us-east-1b", "us-east-1d");
        elbRequest.setListeners(listeners);
        CreateLoadBalancerResult elbResult=new CreateLoadBalancerResult();
        elbResult = elb.createLoadBalancer(elbRequest);
        //Configure the health check to activate the elb
        HealthCheck healthCheck = new HealthCheck("HTTP:80/heartbeat?username=dingz", 30, 5, 2, 10);
        ConfigureHealthCheckRequest configureHealthCheckRequest = new ConfigureHealthCheckRequest("project2elb", healthCheck);
        ConfigureHealthCheckResult configureHealthCheckResult = new ConfigureHealthCheckResult();
        configureHealthCheckResult = elb.configureHealthCheck(configureHealthCheckRequest);
        //Add tag to the elb
        AddTagsRequest addTagsRequest = new AddTagsRequest();
        com.amazonaws.services.elasticloadbalancing.model.Tag elbTag = new com.amazonaws.services.elasticloadbalancing.model.Tag();
        elbTag.withKey("Project")
        .withValue("2.2");
        addTagsRequest.withLoadBalancerNames("project2elb")
        				.withTags(elbTag);
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
        InstanceMonitoring instanceMonitoring = new InstanceMonitoring();
        instanceMonitoring.setEnabled(true);
        lcReq.withImageId("ami-ec14ba84")
        .withInstanceType("m3.medium")
        .withInstanceMonitoring(instanceMonitoring)//enable detailed monitoring
        .withLaunchConfigurationName("project2lc")
        .withSecurityGroups("launch-wizard-2");
        asgClient.createLaunchConfiguration(lcReq);
        System.out.println("LC created");
        //Configure ASG
        CreateAutoScalingGroupRequest asgReq = new CreateAutoScalingGroupRequest();
        com.amazonaws.services.autoscaling.model.Tag asgTag = new com.amazonaws.services.autoscaling.model.Tag();
        asgTag.withKey("Project")
        .withValue("2.2");
        asgReq.withMinSize(1)
        .withMaxSize(16)
        .withDesiredCapacity(6)
        .withLaunchConfigurationName("project2lc")
        .withAutoScalingGroupName("project2asg")
        .withAvailabilityZones("us-east-1a","us-east-1b", "us-east-1d")
        .withDefaultCooldown(60)
        .withLoadBalancerNames("project2elb")
        .withTags(asgTag);
        asgClient.createAutoScalingGroup(asgReq);
        System.out.println("ASG created");
        //Create Auto Scale Policies
        //scale out
        PutScalingPolicyRequest spOutReq = new PutScalingPolicyRequest();
        spOutReq.withAutoScalingGroupName("project2asg")
        .withAdjustmentType("PercentChangeInCapacity")
        .withCooldown(60)
        .withPolicyName("my-scale-out-policy")
        .withScalingAdjustment(2);
        //scale in
        PutScalingPolicyRequest spInReq = new PutScalingPolicyRequest();
        spInReq.withAutoScalingGroupName("project2asg")
        .withAdjustmentType("PercentChangeInCapacity")
        .withCooldown(60)
        .withPolicyName("my-scale-in-policy")
        .withScalingAdjustment(-2);
        
        PutScalingPolicyResult scaleOutRes = asgClient.putScalingPolicy(spOutReq);
        String addArn = scaleOutRes.getPolicyARN();
        PutScalingPolicyResult scaleInRes = asgClient.putScalingPolicy(spInReq);
        String rmArn = scaleInRes.getPolicyARN();
        System.out.println("Policies created");
        //Create CouldWatch Alarm
        //specify alarm dimension 
        Dimension dimensionAdd = new Dimension();
        dimensionAdd.setName("project2asgIncrease");
        dimensionAdd.setValue("project2asgAddCapacity");
        Dimension dimensionRm = new Dimension();
        dimensionRm.setName("project2asgDecrease");
        dimensionRm.setValue("project2asgRmCapacity");
        //specify alarm actions 
        List addActions = new ArrayList();
        addActions.add(addArn);
        List rmActions = new ArrayList();
        rmActions.add(rmArn);
        
        PutMetricAlarmRequest addReq = new PutMetricAlarmRequest();
        addReq.withAlarmName("AddCapacity")
        .withMetricName("CPUUtilization")
        .withNamespace("AWS/EC2")
        .withStatistic(Statistic.Average)
        .withPeriod(120)
        .withThreshold(80d)
        .withUnit(StandardUnit.Percent)
        .withComparisonOperator(ComparisonOperator.GreaterThanThreshold)
        .withDimensions(dimensionAdd)
        .withEvaluationPeriods(1)
        .withAlarmActions(addActions);
        
        PutMetricAlarmRequest rmReq = new PutMetricAlarmRequest();
        rmReq.withAlarmName("RemoveCapacity")
        .withMetricName("CPUUtilization")
        .withNamespace("AWS/EC2")
        .withStatistic(Statistic.Average)
        .withPeriod(120)
        .withThreshold(40d)
        .withUnit(StandardUnit.Percent)
        .withComparisonOperator(ComparisonOperator.LessThanThreshold)
        .withDimensions(dimensionRm)
        .withEvaluationPeriods(1)
        .withAlarmActions(rmActions);
        
        cwClient.putMetricAlarm(addReq);
        cwClient.putMetricAlarm(rmReq);
        System.out.println("Alarm created");
        Thread.sleep(120*1000);
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

	        System.out.println("warm up the ELB done");

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
	        e.printStackTrace();
	    }
		//give plenty of time for the elb to warmup before entering testing 
		Thread.sleep(330*1000);
		
		//visit the test page and initiate the test 
		URL testURL;
		try {
			System.out.println("begin phase 2");
			String testAddr = "http://" + loadGeneratorDns +"/begin-phase-2?dns=" + elbDNS + "&testId=" + testID;
			System.out.println(testAddr);
			testURL = new URL(testAddr);
	        URLConnection conn = testURL.openConnection();
	        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String inputLine;
	        while ((inputLine = in.readLine()) != null) {
		        	System.out.println(inputLine);
		    }
		    in.close();
	       
	        System.out.println("Phase 2 Done");

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
		System.out.println("Termination begins");
		asgReq.setMinSize(0);
		asgReq.setMaxSize(0);
        asgReq.setDesiredCapacity(0);
		asgClient.createAutoScalingGroup(asgReq);
		System.out.println("Termination done");
	}
}