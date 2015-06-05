import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.Configured;

public class Null extends Configured implements Tool {
	public int run(String[] args) throws Exception {
		if (args.length != 2) {
  			System.err.println("Usage: null in out");
			System.exit(2);
		}
		Job job = Job.getInstance(getConf()); // le pasa la config.

		job.setJarByClass(getClass()); // pequeño cambio

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		int resultado = ToolRunner.run(new Null(), args);
		System.exit(resultado);
	}
}
