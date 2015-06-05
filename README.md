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

La máquina virtual instada incluye el siguiente *software* (`cloudera-quickstart-vm-5.4.0-0-virtualbox`):

- CentOS 6.4
- JDK (1.7.0_67).
- Hadoop 2.6.0.
- Eclise 4.2.6 (Juno).

## El MapReduce nulo

Para entender mejor el modo de operación de MapReduce, comenzamos desarrollando un programa [Null.java](Null.java) que, en principio, no hace nada, dejando, por tanto, que se ejecute un trabajo *MapReduce* con todos sus parámetros por defecto:
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
Crea un directorio (por ejemplo, `input`) con varios ficheros de texto y pruebe a compilar y ejecutar este programa especificando como primer parámetro el nombre de ese directorio y como segundo el nombre de un directorio, que no debe existir previamente, donde quedará la salida del trabajo:
    javac  -cp `hadoop classpath` *.java  # compilar
    jar cvf Null.jar *.class # crear el JAR
    hadoop jar Null.jar Null input output # nombre del JAR, de la clase principal y args del programa
Echa un vistazo al contenido del directorio de salida, donde, entre otros, habrá un fichero denominado `part-r-00000`. ¿Qué relación ves entre el contenido de este fichero y los ficheros de texto usados en la prueba? Pronto volveremos con ello.

Al especificar un trabajo *MapReduce* tenemos que incluir los siguientes elementos:
- `job.setInputFormatClass(TextInputFormat.class);`

Esto especifica el formato de entrada. En este caso, hemos usado `TextInputFormat` que es una clase que representa datos de tipo texto y que considera cada línea del fichero como un registro invocando, por tanto, la función **map** del programa por cada línea. Al invocar a **map**, le pasaremos como clave el *offset* (desplazamiento) dentro del fichero correspondiente al principio de la línea. El tipo de la clave será `LongWritable`: `Writable` es el tipo *serializable* que usa *MapReduce* para gestionar todos los datos, que en este caso son de tipo `long`. Como valor, al invocar a **map** pasaremos el contenido de la línea (de tipo `Text`, la versión `Writable` de un `String`).
- `job.setMapperClass(Mapper.class);`
Es un Mapper identidad, que simplemente copia la clave y el valor recibido.
job.setMapOutputKeyClass(LongWritable.class);
El tipo de datos de la clave generada por map. Dado que la función map usada copia la clave recibida, es de tipo LongWritable.
job.setMapOutputValueClass(Text.class);
El tipo de datos del valor generado por map. Dado que la función map usada copia el valor recibido, es de tipo Text.
job.setPartitionerClass(HashPartitioner.class);
Es la clase que realiza la asignación de claves a reducers, usando una función hash para ello.
job.setNumReduceTasks(1);
Sólo usa un Reducer; por eso, hay un único fichero en el directorio de salida.
job.setReducerClass(Reducer.class);
Es un Reducer identidad, que simplemente copia la clave y el valor recibido.
job.setOutputKeyClass(LongWritable.class);
El tipo de datos de la clave generada por reduce y por map excepto si se ha especificado uno distinto para map usando setMapOutputKeyClass. Dado que la función reduce usada copia la clave recibida, es de tipo LongWritable.
job.setOutputValueClass(Text.class);
El tipo de datos del valor generado por reduce y por map excepto si se ha especificado uno distinto para map usando setMapValueKeyClass. Dado que la función reduce usada copia el valor recibido, es de tipo Text.
job.setOutputFormatClass(TextOutputFormat.class);
Este formato de salida es de tipo texto y consiste en la clave y el valor separados, por defecto, por un tabulador (para pasar a texto los valores generados por reduce, el entorno de ejecución invoca el método toString de los respectivas clases Writables).
Se sugiere al lector que modifique el código de Null.java para especificar dos reducers y lo ejecute analizando la salida producida por el programa.

Para terminar esta primera toma de contacto, hay que explicar que el mandato hadoop gestiona sus propios argumentos de línea (veremos un ejemplo en la siguiente sección) y, por tanto, es necesario separar dentro de los argumentos de línea aquellos que corresponden a Hadoop y los que van destinados a la aplicación. La clase Tool facilita este trabajo. A continuación, se presenta la nueva versión de la clase Null.java usando este mecanismo.

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


## Referencias
Este tutorial se ha realizado basándonos en gran medida en los siguientes tutoriales:

1. [Introducción a la programación MapReduce en Hadoop](http://laurel.datsi.fi.upm.es/docencia/asignaturas/ppd). Universidad Politécnica de Madrid (UPM).

2. [Hadoop Tutorial](http://web.stanford.edu/class/cs246/homeworks/tutorial.pdf) Stanford University.

