> Tutorial realizado por:
> * [Aurora Esteban Toscano](https://github.com/i32estoa)
> * [Javier Barbero Gómez](https://github.com/javierbg)

<!-- TOC -->

* [Instalación de Hadoop desde cero](#instalación-de-hadoop-desde-cero)
   * [Preparación](#preparación)
      * [Creación del usuario para Hadoop](#creación-del-usuario-para-hadoop)
      * [Configurar nombres de host](#configurar-nombres-de-host)
      * [Instalación de la JVM](#instalación-de-la-jvm)
      * [Instalación de SSH](#instalación-de-ssh)
   * [Instalación de Apache Hadoop](#instalación-de-apache-hadoop)
      * [Instalar los binarios](#instalar-los-binarios)
      * [Configuración del Nodo Maestro](#configuración-del-nodo-maestro)
      * [Asignación de memoria](#asignación-de-memoria)
      * [Copia de la configuración a los nodos esclavos](#copia-de-la-configuración-a-los-nodos-esclavos)
   * [Formateo y ejecución de HDFS](#formateo-y-ejecución-de-hdfs)
      * [Lanzar y parar el servicio HDFS](#lanzar-y-parar-el-servicio-hdfs)
      * [Monitorización del clúster HDFS](#monitorización-del-clúster-hdfs)
      * [Uso del sistema de ficheros](#uso-del-sistema-de-ficheros)
   * [Ejecución de YARN](#ejecución-de-yarn)
      * [Ejecutar el servicio YARN](#ejecutar-el-servicio-yarn)
      * [Monitorizar el servicio YARN](#monitorizar-el-servicio-yarn)

<!-- /TOC -->

# Instalación de Hadoop desde cero

Si bien es posible [utilizar una instalación "precocinada" de Hadoop como Cloudera](instalacion.md), también se puede instalar Hadoop en una máquina ya existente, ya sea física o virtual.

El tutorial se ha realizado sobre máquinas Ubuntu Server 16.04.4 LTS (64-bit). Las secciones de preparación variarán en función de si se utiliza otro SO (aquellos basados en Debian no deberían cambiar mucho, otros SO requerirán más modificaciones). Para adaptar los comandos a su SO se recomienda leer detenidamente las explicaciones y comentarios.

Además se ha utilizado Apache Hadoop en su versión 2.8.3. Las configuraciones podrían variar de una versión a otra.

## Preparación

### Creación del usuario para Hadoop

En primer lugar es necesario identificar qué máquina adoptará el rol de maestro y cuáles el de esclavo. En todas ellas debemos crear un usuario encargado de realizar las labores de ejecución del clúster. A lo largo de todo este documento se referirá a este usuario como `hadoopd` ("Hadoop Daemon").

```bash
sudo su # Login como usuario root
adduser hadoopd # Crear el usuario hadoopd
# Introducir a continuación la contraseña para este usuario
adduser hadoopd sudo # Añadir hadoopd al grupo sudo
```

A menos que se indique lo contrario, el proceso de configuración del sistema se realizará desde este usuario y se utilizará `sudo` cuando se necesiten premisos elevados.

### Configurar nombres de host

Para facilitar las conexiones entre las máquinas se les dará un nombre significativo a cada uno de ellas. Idealmente esto se realizaría utilizando DNS, pero para esquemas más sencillos puede configurarse manualmente.

El nombre de host local se puede configurar en el fichero `/etc/hostname` (esto es necesario hacerlo en todas las máquinas). Es aconsejable dar nombres identificativos a cada máquina. Este nombre aparecerá por ejemplo en el _prompt_ al conectarse por SSH a cada máquina.

Además es necesario modificar el fichero `/etc/hosts` para indicar las direcciones IP del resto de hosts. Se comentará o eliminará la línea con la IP 127.0.1.1 (con el nombre de host antiguo) y se añadirá una línea por cada uno de los host que contenga su IP y su nombre de host. Por ejemplo, en un caso de direccionamiento estático se añadirían las siguientes líneas:

```
10.0.0.10 hadoop-master
10.0.0.11 hadoop-slave1
10.0.0.12 hadoop-slave2
10.0.0.13 hadoop-slave3
```

Estas serán las máquinas que se referenciarán a lo largo de este tutorial.

### Instalación de la JVM

Hadoop funciona sobre la máquina virtual de Java o JVM, así que será necesario instalar un JDK para poder lanzarlo. Existen dos opciones: [Oracle JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) y su alternativa libre [OpenJDK](http://openjdk.java.net). Cualquiera de estas dos opciones es válida, aunque a continuación se muestran los pasos para instalar OpenJDK 8 (las versiones de Hadoop posteriores a la 2.7 requieren un mínimo de Java 7).

En el caso de que se vaya a desarrollar en estas máquinas podría instalarse el JDK al completo (que incluye el JRE):

```bash
sudo apt-get install openjdk-8-jdk
```

En caso contrario sólo sería necesario instalar el JRE, ahorrando así un poco de espacio:

```bash
sudo apt-get install openjdk-8-jre
```

Además es necesario configurar la variable de entorno de Java para su correcto funcionamiento

```bash
echo "JAVA_HOME=$(which java)" | sudo tee -a /etc/environment
source /etc/environment # Recargar variables
echo $JAVA_HOME # Esto debería mostrar la ruta de instalación
```


### Instalación de SSH

Para la gestión y comunicación de las máquinas durante la instalación será conveniente utilizar el protocolo SSH. Para ello, será primero necesario instalar el servidor SSH.

```bash
sudo apt install openssh-server
```

La configuración del servidor SSH se puede personalizar en el fichero `/etc/ssh/sshd_config`. Si este sistema tuviera que interactuar con redes externas sería conveniente reforzar la seguridad del mismo, por ejemplo, bloqueando el login como usuario `root`, desactivando el login por contraseña o instalando utilidades como Fail2Ban. Este aspecto queda fuera del alcance de este documento.

Para facilitar la comunicación entre las máquina durante la fase de instalación se configurará la autenticación mediante pares de clave pública-privada. Para ello, en el nodo maestro identificados como el usuario `hadoopd`, lanzamos lo siguiente:

```bash
ssh-keygen -b 4096 # Se genera el par de claves
# Distribuimos las claves al resto de nodos
# Es buena práctica hacerlo también con el propio nodo, por si se quiere utilizar también como nodo de datos
# Durante este proceso seguramente se pregunte al usuario si añadir la clave ECDSA a los host conocidos, será necesario aceptar (yes)
# Es necesario repetir este comando tantas veces como máquinas se vayan a configurar (incluyendo el maestro) sustituyendo <nombre-maquina> por el nombre de cada una de las máquinas
ssh-copy-id -i $HOME/.ssh/id_rsa.pub hadoopd@<nombre-maqina>
```
## Instalación de Apache Hadoop

Una vez hechas las preparaciones previas, se pasa a la instalación de Apache Hadoop y la configuración de HDFS y YARN. Inicialmente la configuración se preparará en el nodo maestro, para posteriormente ser duplicada a los nodos esclavos.

> Todos los pasos de esta sección deben realizarse autenticados como `hadoopd`, el usuario creado específicamente para los demonios de Hadoop.

### Instalar los binarios

Desde la máquina `hadoop-master` se descargan y descomprimen los binarios de Hadoop desde su página oficial. Para ello, se ejecutan los siguientes comandos:

```bash
cd # Volver a $HOME
# Descarga del comprimido de la versión elegida
wget http://apache.mindstudios.com/hadoop/common/hadoop-2.8.3/hadoop-2.8.3.tar.gz
# Descompresión de los binarios
tar -xzf hadoop-2.8.3.tar.gz
# Se trabajará con rutas independientes de la versión
mv hadoop-2.8.3 hadoop
```

A continuación se deben establecer las variables de entorno de Hadoop. Esto se hará editando el fichero `/home/hadoopd/.profile` añadiendo la siguiente línea al final:

```bash
# Añadir binarios de Hadoop al PATH del usuario
PATH=/home/hadoopd/hadoop/bin:/home/hadoopd/hadoop/sbin:$PATH
```

### Configuración del Nodo Maestro

Desde `hadoop-master` lo primero es actualizar la ruta de Java que utilizará Hadoop. Para ello, debe actualizarse la variable `JAVA_HOME` del fichero de configuración `~/hadoop/etc/hadoop/hadoop-env.sh`:

```bash
# Debe borrarse el valor por defecto:
# export JAVA_HOME=${JAVA_HOME}
# Y cambiar a la ruta donde se descomprimió Oracle JDK
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64/jre"
```

A continuación se configura dónde se alojará el `NameNode` de HDFS, que al ser el centro del sistema, debe estar en el Nodo Maestro, en este caso en el puerto 9000. Esto se hará mediante la edición del fichero `~/hadoop/etc/hadoop/core-site.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
  <property>
    <name>fs.default.name</name>
    <value>hdfs://hadoop-master:9000</value>
  </property>
</configuration>
```

La rutas de las máquinas dónde se almacenará el `NameNode` o el `DataNode` en su caso serán configuradas en el fichero `~/hadoop/etc/hadoop/hdfs-site.xml`:

```xml
<configuration>
  <property>
    <name>dfs.namenode.name.dir</name>
    <value>/home/hadoopd/data/nameNode</value>
  </property>

  <property>
    <name>dfs.datanode.data.dir</name>
    <value>/home/hadoopd/data/dataNode</value>
  </property>

  <property>
    <name>dfs.replication</name>
    <value>3</value>
  </property>
</configuration>
```

El parámetro `dfs.replication` hace referencia al nivel de redundancia que tendrán los datos, es decir, en cuántos nodos se replicarán éstos. En este caso se va a optar por un nivel máximo de redundancia; tantas copias como nodos de datos.

Por último, se debe configurar qué nodos del sistema serán los esclavos. Esto se hace en el fichero `~/hadoop/etc/hadoop/slaves`, añadiendo los identificadores que se configuraron al principio:

```
hadoop-slave1
hadoop-slave2
hadoop-slave3
```

A continuación se configurará YARN como el planificador de trabajos por defecto. Para ello se editará una copia del fichero `~/hadoop/etc/hadoop/mapred-site.xml.template`.

```bash
cd ~/hadoop/etc/hadoop
cp mapred-site.xml.template mapred-site.xml
```

Se establecerá YARN como el framework por defecto para las operaciones de `MapReduce` en el fichero `~/hadoop/etc/hadoop/mapred-site.xml`:

```xml
<configuration>
  <property>
    <name>mapreduce.framework.name</name>
    <value>yarn</value>
  </property>
</configuration>
```

Por último, algunos parámetros de YARN deben configurarse en el fichero `~/hadoop/etc/hadoop/yarn-site.xml`:

```xml
<configuration>
  <property>
    <name>yarn.acl.enable</name>
    <value>0</value>
  </property>

  <property>
    <name>yarn.resourcemanager.hostname</name>
    <value>hadoop-master</value>
  </property>

  <property>
    <name>yarn.nodemanager.aux-services</name>
    <value>mapreduce_shuffle</value>
  </property>
</configuration>
```

### Asignación de memoria

En este trabajo se van a configurar nodos con 2GB de RAM. Esta memoria debe ser repartida entre los distintos recursos que YARN necesita para la planificación de trabajos y que serán lanzados en cada nodo esclavo. A continuación se muestran los recursos a asignar y un ejemplo de memoria asignada. Como en las configuraciones anteriores, la configuración sólo se realizará en `hadoop-master`, y más adelante se copiará al resto de clústeres.

Las propiedades de YARN a contemplar son:

* Cuánta memoria se podrá asignar a los contenedores YARN en cada nodo esclavo, que debe ser mayor que la memoria asignada al resto de propiedades o el alojamiento de los contenedores sería rechazado.
* Cuánta memoria como mínimo y como máximo puede ocupar un sólo contenedor dentro del nodo esclavo.


Estas propiedades serán configuradas en el fichero `~/hadoop/etc/hadoop/yarn-site.xml`:

```xml
<property>
  <name>yarn.nodemanager.resource.memory-mb</name>
  <value>1536</value>
</property>

<property>
  <name>yarn.scheduler.maximum-allocation-mb</name>
  <value>1536</value>
</property>

<property>
  <name>yarn.scheduler.minimum-allocation-mb</name>
  <value>128</value>
</property>

<property>
  <name>yarn.nodemanager.vmem-check-enabled</name>
  <value>false</value>
</property>
```

La última propiedad desactiva la comprobación de memoria virtual, para asegurar que los contenedores se alojan correctamente en JDK8.

Además, otros recursos no específicos de YARN deben configurarse para cada nodo esclavo:

* Cuánta memoria se utilizará para el `ApplicationManager`, responsable de la coordinación de recursos. Este valor, que será constante y debe encajar en el tamaño máximo del contenedor.
* Cuánta memoria se asignará a cada operación de mapeo o reducción, que debe ser menor que el tamaño máximo.

Estas propiedades serán configuradas en el fichero `~/hadoop/etc/hadoop/mapred-site.xml`:

```xml
<property>
  <name>yarn.app.mapreduce.am.resource.mb</name>
  <value>512</value>
</property>

<property>
  <name>mapreduce.map.memory.mb</name>
  <value>256</value>
</property>

<property>
  <name>mapreduce.reduce.memory.mb</name>
  <value>256</value>
</property>
```

### Copia de la configuración a los nodos esclavos

Una vez finalizada la configuración en el nodo maestro, ésta debe ser duplicada en los nodos esclavos para que todos puedan coordinarse en el clúster correctamente. Esta transferencia se realizará desde `hadoop-master` conectándose vía SSH a los nodos esclavos:

```bash
cd # Volver a $HOME
# Copiar los binarios de Hadoop a los esclavos
for node in hadoop-slave1 hadoop-slave2 hadoop-slave3; do
    scp hadoop-*.tar.gz $node:/home/hadoopd;
done

# Conectarse al primer nodo y descomprimir Hadoop
ssh hadoop-slave1
tar -xzf hadoop-2.8.3.tar.gz
mv hadoop-2.8.3 hadoop
exit

# Repetir la operación anterior para el resto de máquinas esclavo
# Reemplaza en los esclavos las configuraciones por defecto por la del maestro
for node in hadoop-slave1 hadoop-slave2 hadoop-slave3; do
    scp ~/hadoop/etc/hadoop/* $node:/home/hadoopd/hadoop/etc/hadoop/;
done
```

## Formateo y ejecución de HDFS

HDFS requiere ser formateado, como cualquier otro sistema de ficheros. Para ello, en el nodo maestro lanzamos el comando:

```bash
hdfs namenode -format
```

Si todo ha funcionado correctamente deberían aparecer el directorio `~/data` para el usuario `hadoopd`.

### Lanzar y parar el servicio HDFS

Para lanzar el servicio basta con ejecutar el siguiente comando en el nodo maestro:

```bash
start-dfs.sh
```

Esto ejecutará el `NameNode` y el `SecondaryNameNode` en `hadoop-master` y un `DataNode` en cada `hadoop-slave`.

Para comprobar que las JVM se están ejecutando en cada nodo puede utilizarse el comando `jps`.

En `hadoop-master` debe imprimir la siguiente información (con distinto PID):


```
21922 Jps
21603 NameNode
21787 SecondaryNameNode
```


Y en los `hadoop-slave`:

```
19728 DataNode
19819 Jps
```

Para parar el servicio, ejecutar en el nodo maestro el comando:

```bash
stop-dfs.sh
```

### Monitorización del clúster HDFS

Para la monitorización del clúster a través del terminal puede utilizarse el comando `hdfs`:

```bash
hdfs dfsadmin -report # Informe general del clúster
hdfs dfsadmin -help # Descripción de los comandos disponibles
```

Además existe una interfaz web por defecto disponible a través del puerto 50070 (si se ha configurado el nombre de host del maestro como `hadoop-master`: http://hadoop-master:50070).

### Uso del sistema de ficheros

Mientras el sistema está lanzado se puede interactuar con él mediante la línea de comandos. Las órdenes más elementales son:

```bash
# Todas las rutas son por defecto relativas a /user/<nombre usuario>
# Crear carpetas
hdfs dfs -mkdir <ruta remota>

# Enviar archivos (por defecto en /user/<nombre usuario>)
hdfs dfs -put <lista de ficheros locales> <directorio remoto>

# Mostrar el contenido de un directorio
hdfs dfs -ls <directorio remoto>

# Descargar archivos
hdfs dfs -get <archivo remoto> [<ruta local>]

# Imprimir por terminal los contenidos de un archivo
hdfs dfs -cat <archivo remoto>

# Mostrar ayuda de uso (es muy extensa, así que se pasa por el comando `less`)
hdfs dfs -help | less
```

## Ejecución de YARN

HDFS es únicamente el sistema de ficheros utilizado por Apache Hadoop. Para poder lanzar tareas _MapReduce_ en nuestro clúster es necesario hacer uso del servicio YARN. A continuación se explica  cómo activar, monitorizar y hacer uso de este servicio.

### Ejecutar el servicio YARN

De forma similar a HDFS, el servicio YARN se ejecuta con el siguiente comando en el nodo maestro:

```bash
start-yarn.sh
```

Para comprobar que el servicio está lanzado se puede volver a utilizar `jps`. En el nodo maestro debería aparecer un `ResourceManager` y en los nodos esclavos un `NodeManager` en cada uno.

Para detener el servicio en todas las máquinas se utiliza el siguiente comando:

```bash
stop-yarn.sh
```

### Monitorizar el servicio YARN

YARN ofrece los siguientes comandos para monitorizar el clúster:

```bash
yarn node -list # Informe de los nodos en funcionamiento
yarn application -list # Lista de aplicaciones en ejecución
```

De forma similar a HDFS, existe una interfaz web por defecto disponible a través del puerto 8088 (si se ha configurado el nombre de host del maestro como `hadoop-master`: http://hadoop-master:8088).
