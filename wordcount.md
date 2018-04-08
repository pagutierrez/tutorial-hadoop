# Ejemplo WordCount y primeros ejercicios

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Ejemplo WordCount y primeros ejercicios](#ejemplo-wordcount-y-primeros-ejercicios)
	- [¡Hola mundo! en Hadoop](#hola-mundo-en-hadoop)
	- [Ejercicio 1](#ejercicio-1)
	- [Escribiendo nuestros propios *mappers* y *reducers*](#escribiendo-nuestros-propios-mappers-y-reducers)
	- [Ejercicio 2](#ejercicio-2)
	- [Referencias](#referencias)

<!-- /TOC -->

## ¡Hola mundo! en Hadoop

El **WordCount** (contador de palabras) es el "¡Hola Mundo!" de Hadoop. Por su sencillez y su idoneidad para ser resuelto con el paradigma *MapReduce*, se utiliza en multitud de tutoriales de iniciación.

En primer lugar, descarga el código de [WordCount.java](code/ejemplo3/WordCount.java). Cópialo en una carpeta `ejemplo3` del `$HOME` de tu usuario `cloudera`.

Utiliza un paquete apropiado (y genera la carpeta correspondiente) o mantén el genérico (elimina la línea `package`). Las únicas clases estándar de Java que vamos a utilizar son `IOException` y `regex.Pattern`, que las emplearemos para extraer las palabras de los ficheros:
```java
package master.sd;
import java.io.IOException;
import java.util.regex.Pattern;
```

La clase `WordCount` extenderá a la clase `Configured` e implementará la clase de utilidades `Tool`. Haciendo esto, le dices a Hadoop lo que necesita saber para ejecutar tu programa en un objeto de configuración. Luego empleas el `ToolRunner` para ejecutar la aplicación *MapReduce*. Es por ello que vamos a necesitar los siguientes `import`:
```java
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
```

La clase `Logger` manda mensajes de depuración desde las clases **map** y **reduce**. Cuando ejecutas la aplicación, uno de los mensajes estándar de información proporciona la URL que permite rastrear la ejecución del trabajo. Cualquier mensaje pasado al `Logger` se muestra los *logs* del **map** o del **reduce** de tu servidor Hadoop:
```java
import org.apache.log4j.Logger;
```

Necesitas la clase `Job` para crear, configurar y ejecutar una instancia de tu aplicación *MapReduce*. Debes extender la clase `Mapper`, especificando tu propia clase para la acción **map**, y añadir las instrucciones específicas de procesado. Lo mismo sucede con el `Reducer`, lo extiendes para crear y personalizar las acciones de tu **reduce**:
```java
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
```

Utiliza la clase `Path` para acceder a tus archivos en el HDFS. En las instrucciones de configuración de tu `Job`, puedes especificar las rutas requeridas utilizando las clases `FileInputFormat` y `FileOutputFormat`:
```java
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
```

Como ya se comentó, los objetos `Writable` tienen métodos para escribir, leer y comparar valores durante el procesamiento de **map** y **reduce**. La clase `Text` es como un `StringWritable`, porque realiza esencialmente las mismas funciones que hacen las clases `IntWritable` para enteros y `LongWritable` para `long`:
```java
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
```

[WordCount.java](code/ejemplo3/WordCount.java) incluye los métodos `main` y `run` y las clases internas `MiMap` y `MiReduce`.
```java
public class WordCount extends Configured implements Tool {

  private static final Logger LOG = Logger.getLogger(WordCount.class);
```

El método `main` invoca al `ToolRunner`, que crea y ejecuta una nueva instancia de `WordCount`, pasándole los argumentos de la línea de comandos. Cuando la aplicación ya ha terminado, devuelve un valor entero de estado de terminación, que se pasa al objeto `System` al salir:
```java
  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new WordCount(), args);
    System.exit(res);
  }
```

El método `run` configura el trabajo (lo que incluye establecer las rutas pasadas por la línea de comandos), comienza el trabajo, espera a que el trabajo termine y devuelve un valor booleano de éxito:
```java
  public int run(String[] args) throws Exception {
```

Creamos una nueva instancia del objeto `Job`. En este ejemplo utilizamos el método `Configured.getConf()` que devuelve el objeto con la configuración para esta instancia de `WordCount`, y nombramos el objeto del trabajo 'miwordcount':
```java
    Job job = Job.getInstance(getConf(), "miwordcount");
```

Establecemos el `jar`, basándonos en la clase en uso:
```java
    job.setJarByClass(this.getClass());
```

Establecemos las rutas de entrada y salida para la aplicación. Los ficheros de entrada se guardan en el HDFS y sus rutas se pasan por línea de comandos en tiempo de ejecución:
```java
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
```

Establecemos la clase para el **map** y para el **reduce**. En este caso, utilizaremos las clases internas `MiMap` y `MiReduce` definidas en la clase:
```java
    job.setMapperClass(MiMap.class);
    job.setReducerClass(MiReduce.class);
```
Utilizamos un objeto `Text` para crear la salida de la clave (palabra que estamos contando) y un `IntWritable` para el valor (número de veces que aparece la palabra):
```java
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
```

Lanzamos el trabajo y esperar a que termine. La sintaxis del método es `waitForCompletion(boolean verbose)`. Si pasamos un `true`, el método muestra el progreso de los **map** y **reduce** durante su ejecución. Si pasamos `false`, muestra el progreso hasta que se ejecutan los **map** y **reduce**, pero no después.

En Unix, 0 indica éxito y cualquier otra cosa un fallo. Cuando el trabajo termina correctamente, el método devuelve un 0. Cuando falla devuelve un 1:
```java
    return job.waitForCompletion(true) ? 0 : 1;
```

La clase `MiMap` (que es una extensión de `Mapper`) transforma la entrada `<clave,valor>` en pares `<clave,valor>` intermedios que serán enviados al **reducer**. La clase define varias variables globales, empezando con un `IntWritable` con valor 1 y un objeto de texto `Text` utilizado para almacenar cada palabra tal y como se procesa a partir de la cadena de entrada:
```java
  public static class MiMap extends Mapper<LongWritable, Text, Text, IntWritable> {
    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();
```

Creamos un patrón de expresión regular que utilizaremos para transformar cada línea de entrada. El patrón es `\s*\b\s*`, dónde '\b' significa *boundary* de palabra, es decir, espacios, tabuladores y signos de puntuación y los '\s*' son cero o más espacios:
```java
    private static final Pattern WORD_BOUNDARY = Pattern.compile("\\s*\\b\\s*");
```

Hadoop invoca al método **map** una vez por cada par `<clave,valor>` de tu entrada de datos (en nuestro caso, hay uo por línea). Esto no tiene porque corresponderse necesariamente con los pares `<clave,valor>` intermedios que se pasan al **reducer** (lo normal es que haya muchos más pares intermedios). En el caso que nos ocupa, el método **map** recibe el *offset* del primer carácter de la línea actual como clave, y el objeto `Text` representando a la línea completa como valor. Divide la línea en palabras para crear los pares intermedios, usando para ello el patrón de expresión regular:
```java
    public void map(LongWritable offset, Text lineText, Context context)
        throws IOException, InterruptedException {
```
En primer lugar, convierte el objeto `Text` a un `String`. Crea la variable `currentWord`, que utilizará para capturar las palabras individuales de cada línea de entrada:
```java
      String line = lineText.toString();
      Text currentWord = new Text();
```
Utiliza el patrón de expresión regular para dividir la línea actual en palabras individuales, basándose en los delimitadores de palabra. Si la palabra es la cadena vacía, pasamos a la siguiente. En caso contrario, escribimos un par `<clave,valor>` en el objeto que actúa como contexto para el trabajo (recuerda que esto se escribirá en ficheros locales intermedios del **map** cuyo contenido será luego transferido como `<clave,valor>` al **reduce**):
```java
      for (String word : WORD_BOUNDARY.split(line)) {
            if (word.isEmpty()) {
                continue;
            }
            currentWord = new Text(word);
            context.write(currentWord,one);
        }
      }
```

El *mapper* va a crear un par `<clave,valor>` para cada palabra, compuesto de la palabra y el valor `IntWritable` 1. El *reducer* procesa cada par, añadiendo una unidad a un contador para la palabra actual, sabiendo que a él le han pasado todos los valores para la clave que se corresponde a esa palabra. Después, escribirá el resultado de esa palabra en ficheros locales temporales, utilizando de nuevo el objeto de contexto del *reducer*. El mismo *reducer* procesará la siguiente palabra. Cuando todos los pares `<clave,valor>` intermedios se han procesado, el trabajo *MapReduce* ha terminado. La aplicación salva entonces los resultados a la ruta de salida correspondiente en el HDFS.
```java
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
```

Para probar el programa, vamos a bajarnos [todas las obras de Shakespeare](http://www.gutenberg.org/files/100/100-0.txt) del proyecto Gutenberg. Podemos hacer esto mediante `cURL` o `wget` ([diferencias](http://daniel.haxx.se/docs/curl-vs-wget.html)), pero hay que tener cuidado de eliminar el carácter de marca de orden de *bytes* ([BOM](http://es.wikipedia.org/wiki/Marca_de_orden_de_bytes_%28BOM%29)). Utilizaremos el siguiente comando:
```bash
curl http://www.gutenberg.org/files/100/100-0.txt | sed -e 's/^\xEF\xBB\xBF//' > pg100.txt
```

Como podrás observar, el fichero pesa unos 5MB. Ahora, borramos los ficheros de entrada anteriores y copiamos el fichero descargado a nuestra carpeta de entrada en el HDFS:
```bash
[cloudera@quickstart ejemplo3]$ ^C
[cloudera@quickstart ejemplo3]$ hadoop fs -rm input/f*.txt
18/04/08 10:32:10 INFO fs.TrashPolicyDefault: Namenode trash configuration: Deletion interval = 0 minutes, Emptier interval = 0 minutes.
Deleted input/f1.txt
18/04/08 10:32:10 INFO fs.TrashPolicyDefault: Namenode trash configuration: Deletion interval = 0 minutes, Emptier interval = 0 minutes.
Deleted input/f2.txt
[cloudera@quickstart ejemplo3]$ hadoop fs -put pg100.txt input
```

Compilamos los ficheros y creamos el `jar`:
```bash
[cloudera@quickstart ejemplo3]$ javac  -cp `hadoop classpath` master/sd/*.java
[cloudera@quickstart ejemplo3]$ jar cvf wordcount.jar master/sd/*.class
added manifest
adding: master/sd/WordCount.class(in = 1995) (out= 997)(deflated 50%)
adding: master/sd/WordCount$MiMap.class(in = 2213) (out= 990)(deflated 55%)
adding: master/sd/WordCount$MiReduce.class(in = 1651) (out= 694)(deflated 57%)
```

Ejecutamos el ejemplo:
```bash
[cloudera@quickstart ejemplo3]$ hadoop jar wordcount.jar master.sd.WordCount input output
18/04/08 10:33:31 INFO client.RMProxy: Connecting to ResourceManager at /0.0.0.0:8032
18/04/08 10:33:32 WARN security.UserGroupInformation: PriviledgedActionException as:cloudera (auth:SIMPLE) cause:org.apache.hadoop.mapred.FileAlreadyExistsException: Output directory hdfs://quickstart.cloudera:8020/user/cloudera/output already exists
Exception in thread "main" org.apache.hadoop.mapred.FileAlreadyExistsException: Output directory hdfs://quickstart.cloudera:8020/user/cloudera/output already exists
	at org.apache.hadoop.mapreduce.lib.output.FileOutputFormat.checkOutputSpecs(FileOutputFormat.java:146)
	at org.apache.hadoop.mapreduce.JobSubmitter.checkSpecs(JobSubmitter.java:562)
	at org.apache.hadoop.mapreduce.JobSubmitter.submitJobInternal(JobSubmitter.java:432)
	at org.apache.hadoop.mapreduce.Job$10.run(Job.java:1306)
	at org.apache.hadoop.mapreduce.Job$10.run(Job.java:1303)
	at java.security.AccessController.doPrivileged(Native Method)
	at javax.security.auth.Subject.doAs(Subject.java:415)
	at org.apache.hadoop.security.UserGroupInformation.doAs(UserGroupInformation.java:1671)
	at org.apache.hadoop.mapreduce.Job.submit(Job.java:1303)
	at org.apache.hadoop.mapreduce.Job.waitForCompletion(Job.java:1324)
	at master.sd.WordCount.run(WordCount.java:39)
	at org.apache.hadoop.util.ToolRunner.run(ToolRunner.java:70)
	at org.apache.hadoop.util.ToolRunner.run(ToolRunner.java:84)
	at master.sd.WordCount.main(WordCount.java:25)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.RunJar.run(RunJar.java:221)
	at org.apache.hadoop.util.RunJar.main(RunJar.java:136)
```

El error viene provocado por que el directorio de salida ya existía. Es necesario eliminarlo para que *Hadoop* nos permita lanzar el trabajo:
```bash
[cloudera@quickstart ejemplo3]$ hadoop fs -rm -r output
18/04/08 10:34:09 INFO fs.TrashPolicyDefault: Namenode trash configuration: Deletion interval = 0 minutes, Emptier interval = 0 minutes.
Deleted output
[cloudera@quickstart ejemplo3]$ hadoop jar wordcount.jar master.sd.WordCount input output
18/04/08 10:34:15 INFO client.RMProxy: Connecting to ResourceManager at /0.0.0.0:8032
18/04/08 10:34:16 INFO input.FileInputFormat: Total input paths to process : 1
18/04/08 10:34:16 INFO mapreduce.JobSubmitter: number of splits:1
18/04/08 10:34:16 INFO mapreduce.JobSubmitter: Submitting tokens for job: job_1523207149655_0003
18/04/08 10:34:16 INFO impl.YarnClientImpl: Submitted application application_1523207149655_0003
18/04/08 10:34:16 INFO mapreduce.Job: The url to track the job: http://quickstart.cloudera:8088/proxy/application_1523207149655_0003/
18/04/08 10:34:16 INFO mapreduce.Job: Running job: job_1523207149655_0003
18/04/08 10:34:25 INFO mapreduce.Job: Job job_1523207149655_0003 running in uber mode : false
18/04/08 10:34:25 INFO mapreduce.Job:  map 0% reduce 0%
18/04/08 10:34:42 INFO mapreduce.Job:  map 67% reduce 0%
18/04/08 10:34:46 INFO mapreduce.Job:  map 100% reduce 0%
18/04/08 10:35:37 INFO mapreduce.Job:  map 100% reduce 100%
18/04/08 10:35:38 INFO mapreduce.Job: Job job_1523207149655_0003 completed successfully
18/04/08 10:35:38 INFO mapreduce.Job: Counters: 49
	File System Counters
		FILE: Number of bytes read=13085898
		FILE: Number of bytes written=26392423
		FILE: Number of read operations=0
		FILE: Number of large read operations=0
		FILE: Number of write operations=0
		HDFS: Number of bytes read=5858918
		HDFS: Number of bytes written=341329
		HDFS: Number of read operations=6
		HDFS: Number of large read operations=0
		HDFS: Number of write operations=2
	Job Counters
		Launched map tasks=1
		Launched reduce tasks=1
		Data-local map tasks=1
		Total time spent by all maps in occupied slots (ms)=20147
		Total time spent by all reduces in occupied slots (ms)=46114
		Total time spent by all map tasks (ms)=20147
		Total time spent by all reduce tasks (ms)=46114
		Total vcore-seconds taken by all map tasks=20147
		Total vcore-seconds taken by all reduce tasks=46114
		Total megabyte-seconds taken by all map tasks=20630528
		Total megabyte-seconds taken by all reduce tasks=47220736
	Map-Reduce Framework
		Map input records=147929
		Map output records=1248490
		Map output bytes=10588912
		Map output materialized bytes=13085898
		Input split bytes=126
		Combine input records=0
		Combine output records=0
		Reduce input groups=33428
		Reduce shuffle bytes=13085898
		Reduce input records=1248490
		Reduce output records=33428
		Spilled Records=2496980
		Shuffled Maps =1
		Failed Shuffles=0
		Merged Map outputs=1
		GC time elapsed (ms)=107
		CPU time spent (ms)=8050
		Physical memory (bytes) snapshot=563924992
		Virtual memory (bytes) snapshot=3137871872
		Total committed heap usage (bytes)=399507456
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=0
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	File Input Format Counters
		Bytes Read=5858792
	File Output Format Counters
		Bytes Written=341329

```

Ahora debemos comprobar la salida generada, para ver el resultado de los **reduce**. Como siempre, este lo podremos comprobar mirando el contenido del fichero(s) `part*` en la carpeta de salida:
```bash
hadoop fs -cat output/part*
```

Si hubiese habido otros ficheros en la carpeta de entrada, también se hubieran procesado.

Si abrimos la dirección <http://localhost:8088/cluster>, podremos acceder al *manager* de Hadoop. Desde aquí podemos consultar todos los *logs*, lo cuál es especialmente importante cuando nuestros trabajos no se completan con éxito. Puede que algunos enlaces no se abran correctamente (al estar accediendo por `localhost`). Si esto sucede, sustituye `quickstart.cloudera` en la barra de navegación por `192.168.56.101` y no deberías tener problema. También puedes añadir una línea a tu `/etc/hosts` para que `quickstart.cloudera` se resuelva como `192.168.56.101` o simplemente abrir el enlace desde el navegador web de la máquina virtual.

## Ejercicio 1

Ejecuta el ejemplo anterior y guarda el fichero de salida generado.

Debes entregar el fichero de salida (`part-*`).


## Escribiendo nuestros propios *mappers* y *reducers*

A la hora de escribir nuestras propias aplicaciones *MapReduce*, tenemos dos opciones:

1. Utilizar el entorno de desarrollo de la máquina virtual. Como ya hemos comentado, contiene Eclipse. Prueba a abrirlo y verás que viene un programa de ejemplo *MapReduce*, similar al que ya hemos visto, pero donde se han generado las clases en cuatro ficheros:
    * el *Driver* (o programa principal que ejecuta el trabajo),
    * el *Map*,
    * el *Reduce*,
    * y un *Test* para realizar pruebas unitarias utilizando `JUnit`.

2. Utilizar el anfitrión como entorno de desarrollo, haciendo uso de la conexión `sftp://` de o las carpetas compartidas de VirtualBox para luego subir el fichero `.jar`. Si así lo hacemos, podemos utilizar cualquier entorno de desarrollo para Java que tengamos instalado en nuestro anfitrión (Eclipse, NetBeans o el que prefieras).

Si utilizamos Eclipse, podemos generar el fichero `.jar` pulsando el botón derecho del ratón sobre el nombre del proyecto y luego `Java->JAR file`.

## Ejercicio 2

* El segundo ejercicio a realizar es escribir un programa *MapReduce* para Hadoop que cuente el número de palabras que empiezan por cada letra. Para la implementación, ignora la capitalización, es decir, considera todas las letras en minúscula. Ignora todos los caracteres que no sean alfabéticos, pero incluye los dígitos.
* Ejecuta el programa desarrollado sobre la misma entrada (conjunto de obras de Shakespeare).

Debes entregar el fichero de salida generado (`part-*`) y el código fuente (solo los `.java`).

## Referencias

Este tutorial se ha realizado basándonos en gran medida en los siguientes tutoriales:

1. [Introducción a la programación MapReduce en Hadoop](http://laurel.datsi.fi.upm.es/docencia/asignaturas/ppd). Universidad Politécnica de Madrid (UPM).
2. [Hadoop Tutorial](http://snap.stanford.edu/class/cs246-2017/homeworks/hw0/tutorialv3.pdf) Stanford University.
