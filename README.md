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

5. Una vez terminada la importación (que tardará un rato), debería aparecer la máquina virtual. Vamos a configurar *VirtualBox* para que se cree una red de "solo-anfitrión". `Archivo->Preferencias->Red->Redes solo-anfitrión`. 

## Referencias
Este tutorial se ha realizado basándonos en gran medida en los siguientes tutoriales:

1. [Introducción a la programación MapReduce en Hadoop](http://laurel.datsi.fi.upm.es/docencia/asignaturas/ppd). Universidad Politécnica de Madrid (UPM).

2. [Hadoop Tutorial](http://web.stanford.edu/class/cs246/homeworks/tutorial.pdf) Stanford University.

