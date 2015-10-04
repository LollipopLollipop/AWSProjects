import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;


public class InvertedIndex {
	public static class Map extends Mapper<LongWritable, Text, Text, Text> {
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			//to fetch the name of the current file being processed
			FileSplit fs = (FileSplit) context.getInputSplit();
			String location = fs.getPath().getName(); 
			//use DistributedCache to collect stop words
			Path[] localCacheUrl = DistributedCache.getLocalCacheFiles(context.getConfiguration());
			//use HashSet to store stopWords for fast search 
			HashSet stopWords = new HashSet();
			BufferedReader readBuffer = new BufferedReader(new FileReader(localCacheUrl[0].toString()));
            String wordLine;
            while ((wordLine=readBuffer.readLine())!=null){
            	stopWords.add(wordLine);
            }
            readBuffer.close(); 
			//All punctuation should be stripped and replaced with a space
			String line = value.toString().replaceAll("\\p{Punct}"," ");
			StringTokenizer tokenizer = new StringTokenizer(line);			
			while (tokenizer.hasMoreElements()) {
				//Treat all words as case-insensitive
				String target = tokenizer.nextToken().toLowerCase();
				//remove stop words
				if (!stopWords.contains(target)){
					Text word = new Text(target);
					context.write(word, new Text(location));
				}
			}
		}

	}
	public static class Reduce extends Reducer<Text, Text, Text, Text> {
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException{
			String fileList = new String();
			String prevVal = new String();
			for (Text val: values){
				String curVal = val.toString();
				//remove duplicate file names
				if (!curVal.equals(prevVal) || prevVal.equals(""))
					fileList += (curVal + " ");
				prevVal = curVal;
			}
			context.write(key, new Text(fileList.trim()));
		}
		
	}
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException, URISyntaxException {
		// TODO Auto-generated method stub
		Configuration conf = new Configuration();
		
		Job job = new Job(conf, "invertedindex");
		job.setJarByClass(InvertedIndex.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		//download online stop words file to local 
		downloadFile("http://jmlr.csail.mit.edu/papers/volume5/lewis04a/a11-smart-stop-list/english.stop", "stopWords.txt");
		//copy local file to HDFS
		FileSystem hdfs =FileSystem.get(new URI("hdfs://localhost:9000"), conf);
		Path localFilePath = new Path("/home/hadoop/stopWords.txt");
		Path hdfsFilePath=new Path("hdfs://localhost:9000/stopWords.txt");
		hdfs.copyFromLocalFile(localFilePath, hdfsFilePath);
		//add file to DistributedCache
		DistributedCache.addCacheFile(new URI("hdfs://localhost:9000/stopWords.txt"),job.getConfiguration());
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.waitForCompletion(true);	
	}
	public static void downloadFile(String fileURL, String filename) throws IOException{
		URL link = new URL(fileURL); 
		
		InputStream in = new BufferedInputStream(link.openStream());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int n = 0;
		while (-1!=(n=in.read(buf)))
		{
			out.write(buf, 0, n);
		}
		out.close();
		in.close();
		byte[] response = out.toByteArray();

		FileOutputStream fos = new FileOutputStream(filename);
		fos.write(response);
		fos.close();
	}

}
