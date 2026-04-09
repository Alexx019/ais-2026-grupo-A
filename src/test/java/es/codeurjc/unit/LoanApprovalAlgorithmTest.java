package es.codeurjc.unit;

import es.codeurjc.model.LoanEvaluationResult;
import es.codeurjc.service.loan.LoanApprovalAlgorithm;
import es.codeurjc.service.loan.LoanRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoanApprovalAlgorithmTest {

    private final LoanApprovalAlgorithm algorithm = new LoanApprovalAlgorithm();

    @Test
    void shouldRejectWhenAmountIsTooLow() {
        LoanRequest request = new LoanRequest();
        request.setAmount(500);
        LoanEvaluationResult result = algorithm.evaluate(request);

        assertFalse(result.isApproved());
        assertEquals("Cantidad fuera de rango", result.getReason());
    }

    @Test
    void shouldRejectWhenAmountIsTooHigh() {
        LoanRequest request = new LoanRequest();
        request.setAmount(50001);
        LoanEvaluationResult result = algorithm.evaluate(request);

        assertFalse(result.isApproved());
        assertEquals("Cantidad fuera de rango", result.getReason());
    }

    @Test
    void shouldRejectWhenTermMonthsAreTooLow() {
        LoanRequest request = new LoanRequest();
        request.setAmount(1001);
        request.setTermMonths(5);
        LoanEvaluationResult result = algorithm.evaluate(request);

        assertFalse(result.isApproved());
        assertEquals("Plazo no válido", result.getReason());
    }

    @Test
    void shouldRejectWhenTermMonthsAreTooHigh() {
        LoanRequest request = new LoanRequest();
        request.setAmount(1001);
        request.setTermMonths(200);
        LoanEvaluationResult result = algorithm.evaluate(request);

        assertFalse(result.isApproved());
        assertEquals("Plazo no válido", result.getReason());
    }

    @Test
    void shouldRejectWhenBalanceIsInsufficient() {
        LoanRequest request = new LoanRequest();
        request.setAmount(20000);
        request.setTermMonths(24);
        request.setCustomerBalance(3000);
        LoanEvaluationResult result = algorithm.evaluate(request);

        assertFalse(result.isApproved());
        assertEquals("Saldo insuficiente", result.getReason());
    }

    @Test
    void shouldApproveValidBasicLoan() {
        LoanRequest request = new LoanRequest();
        request.setAmount(20000);
        request.setTermMonths(24);
        request.setCustomerBalance(5000);
        LoanEvaluationResult result = algorithm.evaluate(request);

        assertTrue(result.isApproved(), "El préstamo cumple todo y debería ser aprobado");
        assertEquals("Aprobado", result.getReason());
    }

}