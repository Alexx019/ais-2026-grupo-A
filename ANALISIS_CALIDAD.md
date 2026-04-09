# Tarea 1 y 3: Análisis de Calidad del Código y Refactorización

## Captura de Pantalla del Overview de SonarQube

![Overview SonarQube](img/ejemplo.png)

## Análisis de Calidad - Issues 

A continuación se muestra un resumen de los issues encontrados en el análisis de calidad realizado con SonarQube y mediante el análisis manual del código:

### Issue 1: Strings and Boxed types should be compared using "equals()".

**Reporte de la issue**

En el análisis realizado con SonarQube se ha detectado un problema de calidad que indica:  
“Los Strings deben ser comparados usando la funcion equals()”.  
A continuación se muestra la captura de pantalla de la issue reportada por SonarQube, donde
se observa la comparación incorrecta de dos Strings diferentes en la clase AccountService:

![issue1](img/capturas/Captura_Issue5.png)

**Explicación de los alumnos del mal olor detectado:** 
El operador `==` en Java compara si dos variables apuntan al mismo espacio en memoria, independientemente de su contenido. Esto significa que el código no valida realmente si los números de cuenta (String) provienen de objetos distintos.

Consideramos que se trata de un **issue real** ya que este defecto permite que un usuario realice transferencias hacia su propia cuenta, vulnerando las reglas del sistema. Su corrección es crítica para mantener la integridad de la lógica de negocio y evitar transacciones inválidas en la aplicación.

**Refactorización**

### Issue 2: Logic and code should not be duplicated (DRY Principle)

Reporte de la issue:

Al analizar el código a mano hemos comprobado un mal olor bastante notable en ciertas partes: existen bloques de código idénticos repartidos en diferentes métodos de la clase AccountService (específicamente entre las líneas 102-116 y 151-165). 
SonarQube y otros analizadores estáticos marcan esto como una violación del principio DRY (Don't Repeat Yourself).
Adjunto capturas de las lineas de código:

![issue1](img/capturas/CapturaIssue2A.png)
![issue1](img/capturas/CapturaIssue2A.png)

**Explicación de los alumnos del mal olor detectado:**
Duplicación de Código (Bloque de Depósito): Los métodos deposit(String, double, String) y deposit(String, double) son prácticamente idénticos.

No es un falso positivo porque la duplicación no es accidental ni necesaria por restricciones técnicas. 
Es el resultado de un "Copy-Paste" que introduce rigidez en el diseño y aumenta el riesgo de errores de sincronización de lógica en futuras modificaciones.


### Issue 3: Define a constant instead of duplicating this literal "Deposit Confirmation" 4 times.

**Reporte de la issue**
En el análisis realizado con SonarQube se ha detectado un problema de calidad que indica:
“Define a constant instead of duplicating this literal "Deposit Confirmation" 4 times” (Define una constante en lugar de duplicar este literal "Deposit Confirmation" 4 veces).
A continuación se muestra la captura de pantalla de la issue reportada por SonarQube, donde se observa la repetición de la misma cadena de texto en diferentes llamadas a métodos (como emailService y smsService) dentro de la clase AccountService:

![issue3](img/capturas/Captura_Issue3.png)

**Explicación de los alumnos del mal olor detectado:** 
El uso repetido de cadenas de texto literales a lo largo del código viola el principio DRY (Don't Repeat Yourself). Si en el futuro los requisitos cambian y es necesario modificar el asunto del mensaje, el desarrollador tendría que buscar y actualizar manualmente cada una de las apariciones de ese texto en la clase, lo que es propenso a errores u olvidos.

Consideramos que se trata de un issue real, específicamente un code smell que afecta negativamente a la mantenibilidad del software (marcada como "High" en SonarQube).

**Refactorización**

### Issue 4: Remove this unused "seccondAccount" local variable.

*Reporte de la issue*
En el análisis realizado con SonarQube se ha detectado un problema de calidad que indica:
“Remove this unused "secondAccount" local variable” (Elimina esta variable local "secondAccount" no utilizada)
A continuación se muestra la captura de pantalla de la issue reportada por SonarQube, donde se observa la declaración de la variable local Account secondAccount:

![issue4](img/capturas/Captura_Issue4.png)

*Explicación de los alumnos del mal olor detectado:* 
El error se encuentra en el método withdraw de la clase AccountService. Se declara la variable local Account seccondAccount pero esta nunca se inicializa, asigna ni usa en ningún punto del método.

Consideramos que se trata de un issue real, específicamente un code smell que afecta negativamente a la mantenibilidad del software (marcada como "High" en SonarQube).

*Refactorización*


