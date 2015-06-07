package master.sd;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class MostFrequentNextWord extends Configured implements Tool {

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new MostFrequentNextWord(), args);
		System.exit(res);
	}

	public int run(String[] args) throws Exception {
		Job job = Job.getInstance(getConf(), "mostFrequentNextWord-MR1");
		job.setJarByClass(this.getClass());
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.setMapperClass(MiMap.class);
		job.setReducerClass(MiReduce.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		if (! job.waitForCompletion(true))
			return 1;
		
		Job job2 = Job.getInstance(getConf(), "mostFrequentNextWord-MR2");
		job2.setJarByClass(this.getClass());
		FileInputFormat.addInputPath(job2, new Path(args[1]));
		FileOutputFormat.setOutputPath(job2, new Path(args[2]));
		job2.setMapperClass(MiMap2.class);
		job2.setReducerClass(MiReduce2.class);
		job2.setOutputKeyClass(Text.class);
		job2.setOutputValueClass(Text.class);
		return job2.waitForCompletion(true) ? 0 : 1;
	}

	public static class MiMap extends Mapper<LongWritable, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text();
		private static final Pattern WORD_BOUNDARY = Pattern.compile("\\s+");
		private static final Pattern NON_ALPHA = Pattern.compile("^.*[^a-zA-Z0-9].*$");

		public void map(LongWritable offset, Text lineText, Context context)
				throws IOException, InterruptedException {
			String line = lineText.toString();
			String[] words = WORD_BOUNDARY.split(line);
			for (int i=0; i<(words.length-1); i++) {
				if( !(words[i].isEmpty()) && !(words[i+1].isEmpty())){
					if (!NON_ALPHA.matcher(words[i]).matches() && !NON_ALPHA.matcher(words[i+1]).matches()) {
						this.word.set(words[i] + "-" + words[i+1]);
						context.write(this.word,one);
					}					
				}
			}
		}
	}

	public static class MiReduce extends Reducer<Text, IntWritable, Text, IntWritable> {
		@Override
		public void reduce(Text word, Iterable<IntWritable> counts, Context context)
				throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable count : counts) {
				sum += count.get();
			}
			context.write(word, new IntWritable(sum));
		}
	}

	public static class MiMap2 extends Mapper<LongWritable, Text, Text, Text> {

		public void map(LongWritable offset, Text lineText, Context context)
				throws IOException, InterruptedException {
			String line = lineText.toString();

			int index, index2;
			if((index = line.indexOf('-')) == -1) 
				return;
			if((index2 = line.indexOf('\t')) == -1)
				return;

			String word = line.substring(0, index);
			String nextWord = line.substring(index+1, index2);
			String repetitions = line.substring(index2+1);
			context.write(new Text(word), new Text(nextWord + '-' + repetitions));
		}
	}

	public static class MiReduce2 extends Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			String mostCommonNextWord = "";
			int maximum = 0;
			// Buscamos la palabra más común
			for (Text val : values) {
				String val_str = val.toString();
				int index;
				if((index = val_str.indexOf('-')) == -1)
					return;

				int occurrences = Integer.parseInt(val_str.substring(index+1));
				if( maximum < occurrences){
					mostCommonNextWord = val_str.substring(0, index);
					maximum = occurrences;
				}
			}
			if(maximum!=0)
				context.write(key, new Text(mostCommonNextWord));
		}  
	}
}
