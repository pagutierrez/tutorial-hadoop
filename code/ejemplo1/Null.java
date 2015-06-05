import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Null {
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
  			System.err.println("Uso: null in out");
			System.exit(2);
		}
		// Crea un trabajo MapReduce
		Job job = Job.getInstance(); 
		// Especifica el JAR del mismo
		job.setJarByClass(Null.class);

		// Especifica directorio de entrada y de salida
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		// Arranca el trabajo y espera que termine
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
