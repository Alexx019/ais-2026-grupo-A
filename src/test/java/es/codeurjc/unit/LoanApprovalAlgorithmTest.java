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
}