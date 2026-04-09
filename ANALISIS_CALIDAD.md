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

