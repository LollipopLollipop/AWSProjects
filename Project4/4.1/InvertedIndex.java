import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
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
			//All punctuation should be stripped and replaced with a space
			String line = value.toString().replaceAll("\\p{Punct}"," ");
			StringTokenizer tokenizer = new StringTokenizer(line);			
			while (tokenizer.hasMoreElements()) {
				//Treat all words as case-insensitive
				String target = tokenizer.nextToken().toLowerCase();
				Text word = new Text(target);
				context.write(word, new Text(location));
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
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.waitForCompletion(true);	
	}

}
