# Tutorial de introducción a la programación MapReduce en Hadoop
## Introducción
Este tutorial pretende presentar varios ejemplos sencillos que permitan familiarizarse con los conceptos fundamentales del desarrollo de programas en el entorno *MapReduce* de Java, concretamente, en la implementación de Hadoop. Se asume que ya se conocen los aspectos básicos del modelo *MapReduce*. En caso contrario, se recomienda consultar los apuntes de clase y el artículo original que propone este modelo de programación paralela ([MapReduce: Simplified Data Processing on Large Clusters](http://static.googleusercontent.com/media/research.google.com/es//archive/mapreduce-osdi04.pdf) de *Jeffrey Dean* y *Sanjay Ghemawat*), en cuyas ideas se basa la implementación de *MapReduce* de libre distribución incluida en Hadoop.

Este tutorial supone una pequeña introducción al mundo de Hadoop, pero deberías consultar en Internet si deseas disponer de más información.

El tutorial describe como instalar Hadoop, como escribir una primera aplicación, como compilarla, ejecutarla y comprobar la salida.

## Instalación de Hadoop

La forma más fácil de instalar Hadoop es utilizar una de las máquinas virtuales que proporciona Cloudera en su [página web](http://www.cloudera.com/content/cloudera/en/documentation/core/latest/topics/cloudera_quickstart_vm.html). Las máquinas virtuales (*Cloudera QuickStart VMs*) traen todo el entorno ya configurado, ahorrando mucho tiempo. Están disponibles para VMWare, KVM y VirtualBox.

El problema fundamental de dichas máquinas virtuales es que requieren bastante memoria RAM (recomendado un mínimo de 4GB dedicados al *guest* según Cloudera). Funcionan con menos memoria, pero el desempeño se reduce (más esperas).

La otra opción es, si disponéis de un sistema operativo GNU/Linux, instalaros Hadoop. Una forma fácil de realizar la instalación es utilizar *Cloudera manager installer*. El [instalador de Cloudera](http://archive.cloudera.com/cm5/installer/latest/cloudera-manager-installer.bin) debería poder configurar tu equipo de forma fácil.

### Instalación utilizando VirtualBox

1. [Descarga](https://www.virtualbox.org/wiki/Downloads) e instala *VirtualBox* en tu equipo.
2. [Descarga](http://www.cloudera.com/content/cloudera/en/documentation/core/latest/topics/cloudera_quickstart_vm.html) la última versión de la máquina virtual de Cloudera.
3. Descomprime la máquina virtual. Está comprimida con *7zip* (puede que necesites [instalarlo](http://www.7-zip.org/)).
4. Arranca *VirtualBox* y selecciona "Importar servicio virtualizado". Selecciona el archivo OVF ya descomprimido.
5. Una vez terminada la importación (que tardará un rato), debería aparecer la máquina virtual. Vamos a configurar *VirtualBox* para que se cree una red de "solo-anfitrión". `Archivo->Preferencias->Red->Redes solo-anfitrión`. Añádela con los parámetros por defecto. Después, configuramos la máquina virtual para que la use `Botón derecho->Configuración->Red->Adaptador2->Habilitar->Conectado a -> Adaptador solo anfitrión`. De esta forma, podremos acceder por `ssh` a nuestro máquina virtual (utilizando `ssh` o `putty`) a través de la dirección `192.168.56.101`. 
6. Finalmente, arranca la máquina virtual (*paciencia*). Una vez arrancada, deberíamos poder acceder desde el anfitrión a la dirección [http://localhost:8088](http://localhost:8088), dónde podremos ver la interfaz del administrador de recursos. Podrás ver varios que éste y varios puertos está redirigidos por NAT en el Adaptador 1 de tu máquina virtual.
7. El usuario y contraseña por defecto para Cloudera es:
    - User: `cloudera`
    - Password: `cloudera`
8. Puedes utilizar una conexión `sftp://` desde el navegador para facilitar la interacción con Cloudera. Si tu Sistema Operativo anfitrión es GNU/Linux, pulsa `Ctrl+L` y escribe `sftp://cloudera@192.168.56.101/home/cloudera`.

La máquina virtual instada incluye el siguiente *software* (`cloudera-quickstart-vm-5.4.0-0-virtualbox`):

- CentOS 6.4
- JDK (1.7.0_67).
- Hadoop 2.6.0.
- Eclise 4.2.6 (Juno).

## Manejo del HDFS

El sistema de ficheros de Hadoop (HDFS) se puede manejar a través de tres interfaces:

1. Interfaz de línea de comandos, mediante el comando `hadoop fs [opciones]`.
2. Interfaz web (puerto 50070 del *NameNode*). Puedes acceder a través de [http://localhost:50070/](http://localhost:50070/). Ahí podrás ver los ficheros creados y 
3. API de programación.

La interfaz de línea de comandos incluye, por ejemplo, los siguientes comandos:

| Comando | Acción
| --------|---------
| `hadoop fs -ls <path>` | Lista ficheros |
| `hadoop fs -cp <src> <dst>` | Copia ficheros HDFS a HDFS
| `hadoop fs -mv <src> <dst>` | Mueve ficheros HDFS a HDFS |
| `hadoop fs -rm <path>` | Borra ficheros en HDFS |
| `hadoop fs -rmr <path>` | Borra recursivamente |
| `hadoop fs -cat <path>` | Muestra fichero en HDFS |
| `hadoop fs -mkdir <path>` | Crea directorio en HDFS |
| `hadoop fs -chmod ...` | Cambia permisos de fichero |
| `hadoop fs -chown ...` | Cambia propietario/grupo de fichero |
| `hadoop fs -put <local> <dst>` | Copia de local a HDFS |
| `hadoop fs -get <src> <local>` | Copia de HDFS a local |


## El MapReduce nulo

Para entender mejor el modo de operación de MapReduce, comenzamos desarrollando un programa [`Null.java`](code/ejemplo1/Null.java) que, en principio, no hace nada, dejando, por tanto, que se ejecute un trabajo *MapReduce* con todos sus parámetros por defecto:
```java
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
```

Lo primero que debes de hacer es crear un directorio, utilizando el CLI de HDFS. Abre una terminal en la máquina virtual (o conéctate por SSH) y escribe:
```bash
    hadoop fs -mkdir input
```
El directorio de trabajo por defecto para este usuario es `\users\cloudera`. Ahora añade algunos ficheros. Para ello, crea con `gedit` los ficheros en local (por ejemplo, `f1.txt` y `f2.txt`), añade el texto que quieras y luego cópialos al HDFS con:
```bash
    hadoop fs -put f*.txt input/
```
Comprueba como ha quedado el sistema de ficheros a través de [http://localhost:50070/](http://localhost:50070/).

Ahora debes crear el fichero [`Null.java`](code/ejemplo1/Null.java) (en local) y compilar y ejecutar este programa especificando como primer parámetro el nombre de ese directorio y como segundo el nombre de un directorio, que no debe existir previamente, donde quedará la salida del trabajo:
```bash
javac  -cp `hadoop classpath` *.java  # compilar
jar cvf Null.jar *.class # crear el JAR
hadoop jar Null.jar Null input output # nombre del JAR, de la clase principal y args del programa
```
Echa un vistazo al contenido del directorio de salida, donde, entre otros, habrá un fichero denominado `part-r-00000`. 
```bash
[cloudera@quickstart ejemplo1]$ hadoop fs -ls output
Found 2 items
-rw-r--r--   1 cloudera cloudera          0 2015-06-05 04:15 output/_SUCCESS
-rw-r--r--   1 cloudera cloudera         77 2015-06-05 04:15 output/part-r-00000
[cloudera@quickstart ejemplo1]$ hadoop fs -ls output/part-r-00000
-rw-r--r--   1 cloudera cloudera         77 2015-06-05 04:15 output/part-r-00000
[cloudera@quickstart ejemplo1]$ hadoop fs -cat output/part-r-00000
0	sadfsadf
0	asdfasfd
9	asdfasdf
9	qwerqwer
18	qwerwqer
18	qwer qwer
27	
28	
```

¿Qué relación ves entre el contenido de este fichero y los ficheros de texto usados en la prueba? Pronto volveremos con ello.

Analicemos mejor el código de [`Null.java`](code/ejemplo1/Null.java). Al especificar un trabajo *MapReduce* tenemos que incluir los siguientes elementos:

> job.setInputFormatClass(TextInputFormat.class);

Esto especifica el formato de entrada. En este caso, hemos usado `TextInputFormat` que es una clase que representa datos de tipo texto y que considera cada línea del fichero como un registro invocando, por tanto, la función **map** del programa por cada línea. Al invocar a **map**, le pasaremos como clave el *offset* (desplazamiento) dentro del fichero correspondiente al principio de la línea. El tipo de la clave será `LongWritable`: `Writable` es el tipo *serializable* que usa *MapReduce* para gestionar todos los datos, que en este caso son de tipo `long`. Como valor, al invocar a **map** pasaremos el contenido de la línea (de tipo `Text`, la versión `Writable` de un `String`).

> job.setMapperClass(Mapper.class);

Esto especifica cuál es la clase utilizada para el **map**. En este caso, utilizamos el Map identidad, que simplemente copia lo que llega a la salida (sin modificarlo).

> job.setMapOutputKeyClass(LongWritable.class);

Este es el tipo de datos de la clave generada por **map**. Dado que la función **map** usada copia la clave recibida, es de tipo `LongWritable`.

> job.setMapOutputValueClass(Text.class);

El tipo de datos del valor generado por **map**. Dado que la función map usada copia el valor recibido, es de tipo `Text`.

> job.setPartitionerClass(HashPartitioner.class);

Esta clase es la que vamos a utilizar para realizar las particiones (decidir que *reduce* se le asigna a cada clave. Por defecto, utilizamos el basado en hash (`hash(key) mod R`).

> job.setNumReduceTasks(1);

Sólo vamos a utilizar un reducer, por eso generamos un solo fichero de salida.

> job.setReducerClass(Reducer.class);

Con esto se especifica la clase del reducer. En este caso, utilizamos el Reducer identidad, que copia los pares `<clave,valor>` que llegan al fichero de salida.

> job.setOutputKeyClass(LongWritable.class);

El tipo de datos de la clave generada por **reduce** y por **map**, excepto si se ha especificado uno distinto para **map** usando `setMapOutputKeyClass`. Dado que la función reduce usada copia la clave recibida, es de tipo `LongWritable`.

> job.setOutputValueClass(Text.class);

El tipo de datos del valor generado por **reduce** y por **map** excepto si se ha especificado uno distinto para **map** usando `setMapValueKeyClass`. Dado que la función reduce usada copia el valor recibido, es de tipo Text.

> job.setOutputFormatClass(TextOutputFormat.class);

Este formato de salida es de tipo texto y consiste en la clave y el valor separados, por defecto, por un tabulador (para pasar a texto los valores generados por reduce, el entorno de ejecución invoca el método `toString` de las respectivas clases `Writable`).

Modifica el código de `Null.java` para especificar dos *reducers* y ejecútalo analizando la salida producida por el programa.

Para terminar esta primera toma de contacto, hay que explicar que el mandato hadoop gestiona sus propios argumentos de la línea de comandos (veremos un ejemplo en la siguiente sección). Es necesario separar dentro de los argumentos de la línea de comandos aquellos que corresponden a Hadoop y los que van destinados a la aplicación. La clase `Tool` facilita este trabajo. A continuación, se presenta la nueva versión de la clase [`Null.java`](code/ejemplo2/Null.java) usando este mecanismo.

```java
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
```

## ¡Hola mundo! en Hadoop

El WordCount (contador de palabras) es el "¡Hola Mundo!" de Hadoop. Por su sencillez y su idoneidad para ser resuelto con el paradigma *MapReduce*, se utiliza en multitud de tutoriales de iniciación. Ahora vamos a seguir el [tutorial de iniciación a Hadoop de Cloudera](http://www.cloudera.com/content/cloudera/en/documentation/hadoop-tutorial/CDH5/Hadoop-Tutorial.html) que podemos encontrar en su documentación.

En primer lugar, descarga el código de [WordCount.java](code/ejemplo3/WordCount.java). Cópialo en una carpeta `ejemplo3` del `$HOME` de tu usuario `cloudera`.    

Utiliza un paquete apropiado (y genera la carpeta correspondiente) o mantén el genérico (elimina la línea `package`). Las únicas clases estándar de Java que vamos a utilizar son `IOException` y `regex.Pattern`, que las emplearemos para extraer las palabras de los ficheros:
```java
package master.sd;
import java.io.IOException;
import java.util.regex.Pattern;
```

Esta clase extenderá a la clase `Configured` e implementa la clase de utilidades `Tool`. Haciendo esto, le dices a Hadoop lo que necesita saber para ejecutar tu programa en un objeto de configuración. Luego empleas el `ToolRunner` para ejecutar la aplicación MapReduce:
```java
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
```

La clase `Logger` manda mensajes de depuración desde las clases **map** y **reduce**. Cuando ejecutas la aplicación, uno de los mensajes estándar de información proporciona la URL que permite rastrear la ejecución del trabajo. Cualquier mensaje pasado al `Logger` se muestra los logs del map o del reduce de tu servidor Hadoop.
```java
import org.apache.log4j.Logger;
```

Necesitas la clase `Job` para crear, configurar y ejecutar una instancia de tu aplicación MapReduce. Debes extender la clase `Mapper` utilizando tu propia clase para la acción **map** y añadir las instrucciones específicas de procesado. Lo mismo sucede con el `Reducer`: lo extiendes para crear y personalizar las acciones de tu **reduce**:
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

El método `main` invoca al `ToolRunner`, que crea y ejecuta una nueva instancia de `WordCount`, pasándole los argumentos de la línea de comandos. Cuando la aplicación ya ha terminado, devuelve un valor entero de estado de terminación, que se pasa al objeto `System` al salir.
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

Creamos una nueva instancia del objeto `Job`. En este ejemplo utilizamos el método `Configured.getConf()` el objeto de configuración para esta instancia de `WordCount`, y nombramos el objeto del trabajo 'miwordcount':
```java
    Job job = Job.getInstance(getConf(), "miwordcount");
```

Establecer el `jar`, basándonos en la clase en uso:
```java
    job.setJarByClass(this.getClass());
```

Establecer las rutas de entrada y salida para la aplicación. Los ficheros de entrada se guardan en el HDFS y sus rutas se pasan por línea de comandos en tiempo de ejecución:
```java
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
```

Establecer la clase para el **map** y para el **reduce**. En este caso, utilizaremos las clases internas MiMap y MiReduce definidas en la clase:
```java
    job.setMapperClass(MiMap.class);
    job.setReducerClass(MiReduce.class);
```
Utilizamos un objeto `Text` para crear la salida de la clave (palabra que estamos contando) y un `IntWritable` para el valor (número de veces que aparece la palabra):
```java
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
```

Lanzar el trabajo y esperar a que termine. La sintaxis del método es `waitForCompletion(boolean verbose)`. Si pasamos un `true`, el método muestra el progreso de los **map** y **reduce** durante su ejecución. Si pasamos `false`, muestra el progreso hasta que se ejecutan los **map** y **reduce**, pero no después.

En Unix, 0 indica éxito y cualquier otra cosa un fallo. Cuando el trabajo termina correctamente, el método devuelve un 0. Cuando falla devuelve un 1:
```java
    return job.waitForCompletion(true) ? 0 : 1;
```

La clase MiMap (que es una extensión de `Mapper`) transforma la entrada `<clave,valor>` en pares `<clave,valor>` intermedios que serán enviados al **reducer**. La clase define varias variables globales, empezando con un `IntWritable` con valor 1 y un objeto de texto `Text` utilizado para almacenar cada palabra tal y como se procesa a partir de la cadena de entrada:
```java
  public static class MiMap extends Mapper<LongWritable, Text, Text, IntWritable> {
    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();
```

Creamos un patrón de expresión regular que utilizaremos para transformar cada línea de entrada. El patrón es `\b`, que significa *boundary* de palabra, es decir, espacios, tabuladores y signos de puntuación:
```java
    private static final Pattern WORD_BOUNDARY = Pattern.compile("\\s*\\b\\s*");
```

Hadoop invoca al método **map** una vez por cada par `<clave,valor>` de tu entrada de datos. Esto no tiene porque corresponderse necesariamente con los pares `<clave,valor>` intermedios que se pasan al **reducer** (lo normal es que haya muchos más pares intermedios). En el caso que nos ocupa, el método **map** recibe el *offset* del primer carácter de la línea actual como clave, y el objeto `Text` representando a la línea completa como valor. Divide la línea en palabras para crear los pares intermedios, usando para ello el patrón de expresión regular:
```java
    public void map(LongWritable offset, Text lineText, Context context)
        throws IOException, InterruptedException {
```
Después, convierte el objeto `Text` a un `String`. Crea la variable `currentWord`, que utilizará para capturar las palabras individuales de cada línea de entrada:
```java
      String line = lineText.toString();
      Text currentWord = new Text();
```
Utiliza el patrón de expresión regular para dividir la línea actual en palabras individuales, basándose en los delimitadores de palabra. Si la palabra es la cadena vacía, pasamos a la siguiente. En caso contrario, escribimos un par `<clave,valor>` al objeto que actúa como contexto para el trabajo:
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

El *mapper* va a crear un par `<clave,valor>` para cada palabra, compuesto de la palabra y el valor `IntWritable` 1. El *reducer* procesa cada par, añadiendo una unidad al contador de la palabra actual correspondiente a todos los *mappers*. Después, escribe el resultado de esa palabra el objeto de contexto del *reducer* y pasa a la siguiente. Cuanto todos los pares `<clave,valor>` intermedios se han procesado, el trabajo MapReduce ha terminado. La aplicación salva entonces los resultados a la ruta de salida correspondiente en el HDFS.
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

Para probar el programa, vamos a utilizar vamos a bajarnos [todas las obras](http://www.gutenberg.org/cache/epub/100/pg100.txt) de Shakespeare del proyecto Gutenberg. Podemos hacer esto mediante cURL o wget, pero hay que tener cuidado de eliminar el carácter de marca de orden de *bytes* ([BOM](http://es.wikipedia.org/wiki/Marca_de_orden_de_bytes_%28BOM%29)). Utilizaremos el siguiente comando:
```bash
curl http://www.gutenberg.org/cache/epub/100/pg100.txt | sed -e 's/^\xEF\xBB\xBF//' > pg100.txt
```

Como podrás observar, el fichero pesa unos 5MB. Ahora, borramos los ficheros de entrada anteriores y copiamos el fichero descargado con `curl` a nuestra carpeta de entrada en el HDFS:
```bash
[cloudera@quickstart ejemplo3]$ hadoop fs -rm input/f*.txt
15/06/05 07:54:34 INFO fs.TrashPolicyDefault: Namenode trash configuration: Deletion interval = 0 minutes, Emptier interval = 0 minutes.
Deleted input/f1.txt
15/06/05 07:54:35 INFO fs.TrashPolicyDefault: Namenode trash configuration: Deletion interval = 0 minutes, Emptier interval = 0 minutes.
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
15/06/05 07:59:41 INFO client.RMProxy: Connecting to ResourceManager at /0.0.0.0:8032
15/06/05 07:59:41 WARN security.UserGroupInformation: PriviledgedActionException as:cloudera (auth:SIMPLE) cause:org.apache.hadoop.mapred.FileAlreadyExistsException: Output directory hdfs://quickstart.cloudera:8020/user/cloudera/output already exists
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

El error viene provocado por que el directorio de salida ya existía. Es necesario eliminarlo, para que se nos permita lanzar el trabajo:
```bash
[cloudera@quickstart ejemplo3]$ hadoop fs -rm -r output
15/06/05 08:00:40 INFO fs.TrashPolicyDefault: Namenode trash configuration: Deletion interval = 0 minutes, Emptier interval = 0 minutes.
Deleted output
```
Ahora ya podemos lanzar el trabajo. Este paso puede tardar bastante dependiendo de la cantidad de memoria RAM de la que disponga tu ordenador. Se nos suministra un resumen de las operaciones realizadas:
```bash
[cloudera@quickstart ejemplo3]$ hadoop jar wordcount.jar master.sd.WordCount input output
15/06/05 08:01:25 INFO client.RMProxy: Connecting to ResourceManager at /0.0.0.0:8032
15/06/05 08:01:26 INFO input.FileInputFormat: Total input paths to process : 1
15/06/05 08:01:26 INFO mapreduce.JobSubmitter: number of splits:1
15/06/05 08:01:26 INFO mapreduce.JobSubmitter: Submitting tokens for job: job_1433500865548_0004
15/06/05 08:01:26 INFO impl.YarnClientImpl: Submitted application application_1433500865548_0004
15/06/05 08:01:27 INFO mapreduce.Job: The url to track the job: http://quickstart.cloudera:8088/proxy/application_1433500865548_0004/
15/06/05 08:01:27 INFO mapreduce.Job: Running job: job_1433500865548_0004
15/06/05 08:01:35 INFO mapreduce.Job: Job job_1433500865548_0004 running in uber mode : false
15/06/05 08:01:35 INFO mapreduce.Job:  map 0% reduce 0%
15/06/05 08:01:44 INFO mapreduce.Job:  map 100% reduce 0%
15/06/05 08:01:52 INFO mapreduce.Job:  map 100% reduce 100%
15/06/05 08:01:53 INFO mapreduce.Job: Job job_1433500865548_0004 completed successfully
15/06/05 08:01:53 INFO mapreduce.Job: Counters: 49
	File System Counters
		FILE: Number of bytes read=12291899
		FILE: Number of bytes written=24804425
		FILE: Number of read operations=0
		FILE: Number of large read operations=0
		FILE: Number of write operations=0
		HDFS: Number of bytes read=5590012
		HDFS: Number of bytes written=311091
		HDFS: Number of read operations=6
		HDFS: Number of large read operations=0
		HDFS: Number of write operations=2
	Job Counters 
		Launched map tasks=1
		Launched reduce tasks=1
		Data-local map tasks=1
		Total time spent by all maps in occupied slots (ms)=6382
		Total time spent by all reduces in occupied slots (ms)=5858
		Total time spent by all map tasks (ms)=6382
		Total time spent by all reduce tasks (ms)=5858
		Total vcore-seconds taken by all map tasks=6382
		Total vcore-seconds taken by all reduce tasks=5858
		Total megabyte-seconds taken by all map tasks=6535168
		Total megabyte-seconds taken by all reduce tasks=5998592
	Map-Reduce Framework
		Map input records=124787
		Map output records=1172174
		Map output bytes=9947545
		Map output materialized bytes=12291899
		Input split bytes=126
		Combine input records=0
		Combine output records=0
		Reduce input groups=30254
		Reduce shuffle bytes=12291899
		Reduce input records=1172174
		Reduce output records=30254
		Spilled Records=2344348
		Shuffled Maps =1
		Failed Shuffles=0
		Merged Map outputs=1
		GC time elapsed (ms)=101
		CPU time spent (ms)=7250
		Physical memory (bytes) snapshot=606945280
		Virtual memory (bytes) snapshot=3138928640
		Total committed heap usage (bytes)=522715136
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=0
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	File Input Format Counters 
		Bytes Read=5589886
	File Output Format Counters 
		Bytes Written=311091

```

Ahora debemos comprobar la salida generada, para ver el resultado de los **reduce**. Como siempre, este lo podremos comprobar mirando el contenido del fichero(s) `part*` en la carpeta de salida:
```bash
hadoop fs -cat output/part*
```

Si hubiese habido otros ficheros en la carpeta de entrada, también se hubieran procesado.

Si abrimos la dirección [http://localhost:8088/cluster](http://localhost:8088/cluster) podremos acceder al *manager* de Hadoop. Desde aquí podemos consultar todos los *logs*, lo cuál es especialmente importante cuando nuestros trabajos no se completan con éxito. Puede que algunos enlaces no se abran correctamente (al estar accediendo por `localhost`). Si esto sucede, sustituye `quickstart.cloudera` en la barra de navegación por `192.168.56.101` y no deberías tener problema.


## Escribiendo nuestros propios *mappers* y *reducers*

A la hora de escribir nuestras propias aplicaciones MapReduce, tenemos dos opciones:

1. Utilizar el entorno de desarrollo de la máquina virtual. Como ya hemos comentado, contiene Eclipse. Prueba a abrirlo y verás que viene un programa de ejemplo MapReduce, similar al que ya hemos visto, pero donde se han generado cuatros clases: 
    * el Driver (o programa principal que ejecuta el trabajo),
    * el Map,
    * el Reduce,
    * y un Test para realizar pruebas unitarias utilizando JUnit.
    
2. Utilizar el anfitrión como entorno de desarrollo, haciendo uso de la conexión `sftp://` de o las carpetas compartidas de VirtualBox para luego subir el fichero `.jar`. Si así lo hacemos, podemos utilizar cualquier entorno de desarrollo para Java (Eclipse, NetBeans o el que prefieras).

Si utilizamos Eclipse, el fichero `.jar` lo podemos generar pulsando el botón derecho del ratón sobre el nombre del proyecto y luego `Java->JAR file`.

## Ejercicio 1

Ejecuta el ejemplo anterior y guarda el fichero de salida generado.

## Ejercicio 2

* El primer ejercicio que debes realizar es escribir un programa MapReduce para Hadoop que muestre el número de palabras que empiezan por cada letra. Esto significa que, para cada letra, queremos contar el número de palabras que empiezan por esa letra. Para la implementación, ignora la capitalización, es decir, considera todas las letras en minúscula. Ignora todos los caracteres que no sean alfabéticos, pero incluye los dígitos.
* Ejecuta el programa desarrollado sobre la misma entrada (conjunto de obras de Shakespeare).

Debes entregar el fichero de salida y el código fuente generado (solo los `.java`).

## Algunos aspectos adicionales

Como se comentó en las diapositivas de clase, existen algunos aspectos adicionales (uso de combinadores, particiones, etc...), que nos pueden servir para mejorar las prestaciones de los procesos MapReduce. Tienes una guía bastante breve en el [tutorial Hadoop de Cloudera](http://www.cloudera.com/content/cloudera/en/documentation/hadoop-tutorial/CDH5/Hadoop-Tutorial/ht_mapreduce_if.html). Lee tranquilamente dicha guía para entender mejor estos aspectos.

### WordCount V2: configuraciones específicas

Este primer ejemplo está extraído del [tutorial Hadoop de Cloudera](http://www.cloudera.com/content/cloudera/en/documentation/hadoop-tutorial/CDH5/Hadoop-Tutorial/ht_mapreduce_if.html), mencionado anteriormente.
Vamos a utilizar las opciones de configuración de Hadoop para incorporar un parámetro que nos permita elegir si el conteo de palabras va a ser sensible (o no) a mayúsculas. Además, haremos uso de un **combiner** que va a combinar los pares `<clave,valor>` que sean locales al **map** utilizado (incremento de prestaciones).

Consulta el fichero [WordCount.java](code/ejemplo4/WordCount.java). Se han realizado los siguientes cambios:

* Importamos la clase `Configuration`. Se puede utilizar esta clase para acceder a argumentos de la línea de comandos en tiempo de ejecución:
```java
import org.apache.hadoop.conf.Configuration;
```
* Creamos una variable para establecer (o no) si el *map* va a ser sensible a mayúsculas:
```java
private boolean sensibleMayusculas = false;
```
* Añadimos un método `setup`. Hadoop llama a este método automáticamente al mandar un trabajo. Este código instancia un objeto de tipo `Configuration`, y después establece el valor de la variable `sensibleMayusculas` al valor de la variable de sistema  `wordcount.case.sensitive` que se supone que ha sido especificada por línea de comandos (valor por defecto `false`).
```java
    protected void setup(Mapper.Context context)
      throws IOException,
        InterruptedException {
      Configuration config = context.getConfiguration();
      this.sensibleMayusculas = config.getBoolean("wordcount.mayusculas.sensible", false);
    }
```
* Desactivamos la sensibilidad a mayúsculas aquí. Si `sensibleMayusculas` es `false`, la línea completa se convierte a minúsculas antes de que sea procesado por el `StringTokenizer`:
```java
      if (!sensibleMayusculas) {
        line = line.toLowerCase();
      }
```
* Además, puedes observar como, en este caso, se ha utilizado el mismo *reducer* para servir de combinador de pares locales:
```java
    job.setReducerClass(MiReduce.class);
```
Compila el ejemplo como hiciste anteriormente. Ahora podrás ejecutar el ejemplo de dos formas. Por defecto, el programa no será sensible a mayúsculas:
```bash
hadoop jar wordcount.jar master.sd.WordCount input output
```

Si queremos que sea sensible a mayúsculas podemos utilizar:
```bash
hadoop jar wordcount.jar master.sd.WordCount -Dwordcount.mayusculas.sensible=true input output
```

### MostFrequentNextWord

El fichero [MostFrequentNextWord.java](code/ejemplo5/MostFrequentNextWord.java)

## Ejercicio 2

* El primer ejercicio que debes realizar es escribir un programa MapReduce para Hadoop que muestre el número de palabras que empiezan por cada letra. Esto significa que, para cada letra, queremos contar el número de palabras que empiezan por esa letra. Para la implementación, ignora la capitalización, es decir, considera todas las letras en minúscula. Ignora todos los caracteres que no sean alfabéticos, pero incluye los dígitos.
* Ejecuta el programa desarrollado sobre la misma entrada (conjunto de obras de Shakespeare).

Debes entregar el fichero de salida y el código fuente generado (solo los `.java`).

## Referencias
Este tutorial se ha realizado basándonos en gran medida en los siguientes tutoriales:

1. [Introducción a la programación MapReduce en Hadoop](http://laurel.datsi.fi.upm.es/docencia/asignaturas/ppd). Universidad Politécnica de Madrid (UPM).

2. [Hadoop Tutorial](http://web.stanford.edu/class/cs246/homeworks/tutorial.pdf) Stanford University.

