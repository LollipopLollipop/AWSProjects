import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;



public class LanguageModel {
	public static class Map extends Mapper<LongWritable, Text, Text, Text> {
		//private final static IntWritable one = new IntWritable(1);
		private Text word_count = new Text();
		private Text phrase = new Text();
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			String compound_phrase = line.split("\t")[0].trim();
			String count = line.split("\t")[1].trim();
			String last_word="";
			String leading_phrase = compound_phrase;
			word_count.set(last_word+":"+count);
			phrase.set(leading_phrase);
			context.write(phrase, word_count);
			if(compound_phrase.lastIndexOf(" ")>0){
				last_word = compound_phrase.substring(compound_phrase.lastIndexOf(" ")+1);
				leading_phrase = compound_phrase.substring(0,compound_phrase.lastIndexOf(" "));
				word_count.set(last_word+":"+count);
				phrase.set(leading_phrase);
				context.write(phrase, word_count);
			}
			
		}
	}
	
	public static class Reduce extends TableReducer<Text, Text, ImmutableBytesWritable> {
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException{
			//used to store word-count mapping in descending order
			TreeMap<Integer,String> mySortedMap = new TreeMap<Integer,String>(Collections.reverseOrder());
			for (Text val: values){
				String cur_val = val.toString().trim();
				String word = cur_val.split(":")[0].trim();
				int count = Integer.parseInt(cur_val.split(":")[1].trim());
				//discard words with few counts
				//store all word and count combination to treemap
				if (count > 5)//t value set to 5
					mySortedMap.put(count,word);
			}
			if(mySortedMap.size()>1){
				Set keys = mySortedMap.keySet();
				int j=0;
				float denominator = 0;
				float prob = 0;
				String word_prob = "";
				
				for (Iterator i = keys.iterator(); i.hasNext() && j<6; j++) {//n value set to 5
					Integer count_key = (Integer) i.next();
					String word_value = (String) mySortedMap.get(count_key);
					//first key-value pair is the phrase and its count 
					//use it as denominator to calc prob
					if(j==0)
						denominator=(float)count_key;
					else{
						prob = (float)(count_key)/denominator;
						//concatenate word-count pairs for each leading phrase 
						word_prob+=(word_value+":"+Float.toString(prob)+";");
					}
				}
				//context.write(key, new Text(word_prob));
				Put row = new Put(Bytes.toBytes(key.toString()));
				row.add(Bytes.toBytes("cf"),Bytes.toBytes("attr"),Bytes.toBytes(word_prob));
				context.write(new ImmutableBytesWritable(Bytes.toBytes(key.toString())), row);
			}
		}
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		// TODO Auto-generated method stub
		Configuration conf = HBaseConfiguration.create();
		//conf.set("hbase.mapred.outputtable", args[1]);
	    
	    Job job = new Job(conf, "LanguageModel");
		job.setJarByClass(LanguageModel.class);
		
		job.setMapperClass(Map.class);
		job.setMapOutputKeyClass(Text.class);
	    job.setMapOutputValueClass(Text.class);
		//job.setReducerClass(Reduce.class);
		//job.setInputFormatClass(TextInputFormat.class);
		//job.setOutputFormatClass(TableOutputFormat.class);
		TableMapReduceUtil.initTableReducerJob(
				"languagemodelgene",      // output table
				Reduce.class,             // reducer class
				job);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		//FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.setReducerClass(Reduce.class);
		job.waitForCompletion(true);
		
	}

}
