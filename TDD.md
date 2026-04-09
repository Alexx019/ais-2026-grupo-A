# Tarea 4: Implementación de una funcionalidad con TDD 


### Test 1: Debe rechazar si la cantidad es menor a 1000

**INPUT y OUTPUT**: 500 -> "Cantidad fuera de rango"

**Código de test**
```java
@Test
void shouldRejectWhenAmountIsTooLow() {
    LoanRequest request = new LoanRequest();
    request.setAmount(500);
    LoanEvaluationResult result = algorithm.evaluate(request);

    assertFalse(result.isApproved());
    assertEquals("Cantidad fuera de rango", result.getReason());
}
```

**Mensaje del test añadido que NO PASA**

```log
org.opentest4j.AssertionFailedError: 
Expected :false
Actual   :true
```

**Código mínimo para que el test pase**

Se añade una condición que verifica la cantidad del préstamo y la compara con el límite inferior (en este caso 1000). Si es inferior se rechaza el préstamo con el mensaje: "Cantidad fuera de rango"

```java
private static final int LIMITE_INFERIOR = 1000;

public LoanEvaluationResult evaluate(LoanRequest request) {

    if (request.getAmount() < LIMITE_INFERIOR){
        return new LoanEvaluationResult(false, "Cantidad fuera de rango");
    }

    return new LoanEvaluationResult(true, "Aprobado");
}
```

**Captura de que TODOS los test PASAN**

![img_TDD_test1.png](img/capturas/img_TDD_test1.png)