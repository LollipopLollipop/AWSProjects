import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
public class NGram {
	public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text();
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			//limit the words in the phrase to be purely alphabetical [A-Za-z]
			String line = value.toString().replaceAll("[^a-zA-Z]+"," ");
			//store only lower-case words
			//remove leading or tailing white space
			String[] words = line.trim().toLowerCase().split("\\s+");
			//print 1-gram
			printNGram(word, context, words, 0);
			//print 2-gram
			printNGram(word, context, words, 1);
			//print 3-gram
			printNGram(word, context, words, 2);
			//print 4-gram
			printNGram(word, context, words, 3);
			//print 5-gram
			printNGram(word, context, words, 4);
		}
		public static void printNGram(Text printWord, Context context, String[] words, int n) throws IOException, InterruptedException{
			for (int x=0; (x+n)<words.length; x++){
				String word=words[x];
				switch(n){
				case 1: word += " " + words[x+1]; break;
				case 2: word += " " + words[x+1]+ " " + words[x+2]; break;
				case 3: word += " " + words[x+1]+ " " + words[x+2] + " " + words[x+3]; break;
				case 4: word += " " + words[x+1]+ " " + words[x+2] + " " + words[x+3] + " " + words[x+4]; break;
				}
				//only print non-empty phrase
				if (!word.equals("")){
					printWord.set(word);
					context.write(printWord, one);
				}
			}
		}
	}
	public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException{
			int sum=0;
			for (IntWritable val : values){
				sum += val.get();
			}
			context.write(key, new IntWritable(sum));
		}
		
	}
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		// TODO Auto-generated method stub
		Configuration conf = new Configuration();
		
		Job job = new Job(conf, "NGram");
		job.setJarByClass(NGram.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.waitForCompletion(true);
	}

}
