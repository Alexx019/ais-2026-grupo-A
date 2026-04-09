package es.codeurjc.unit;

import es.codeurjc.model.LoanEvaluationResult;
import es.codeurjc.service.EuriborService;
import es.codeurjc.service.loan.LoanApprovalAlgorithm;
import es.codeurjc.service.loan.LoanRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class LoanApprovalAlgorithmTest {

    private EuriborService euriborServiceMock;
    private LoanApprovalAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        euriborServiceMock = Mockito.mock(EuriborService.class);
        algorithm = new LoanApprovalAlgorithm(euriborServiceMock);
    }

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
        Mockito.when(euriborServiceMock.getEuribor()).thenReturn(3.0);
        LoanRequest request = new LoanRequest();
        request.setAmount(20000);
        request.setTermMonths(24);
        request.setCustomerBalance(5000);
        request.setMonthlyIncome(3000);

        LoanEvaluationResult result = algorithm.evaluate(request);

        assertTrue(result.isApproved(), "El préstamo cumple todo y debería ser aprobado");
        assertEquals("Aprobado", result.getReason());
    }

    @Test
    void shouldCalculateInterestRateCorrectly() {
        Mockito.when(euriborServiceMock.getEuribor()).thenReturn(3.14);
        LoanRequest request = new LoanRequest();
        request.setAmount(20000);
        request.setTermMonths(24);
        request.setCustomerBalance(5000);
        request.setMonthlyIncome(3000);

        LoanEvaluationResult result = algorithm.evaluate(request);

        assertTrue(result.isApproved());
        assertEquals(5.14, result.getInterestRate(), 0.001);
    }

    @Test
    void shouldCalculateMonthlyPaymentCorrectly() {
        Mockito.when(euriborServiceMock.getEuribor()).thenReturn(3.0);
        LoanRequest request = new LoanRequest();
        request.setAmount(20000);
        request.setTermMonths(24);
        request.setCustomerBalance(5000);
        request.setMonthlyIncome(3000);

        LoanEvaluationResult result = algorithm.evaluate(request);

        assertTrue(result.isApproved());
        assertEquals(875.0, result.getMonthlyPayment(), 0.001);
    }
    @Test
    void shouldRejectWhenPaymentExceedsIncomeLimit() {
        Mockito.when(euriborServiceMock.getEuribor()).thenReturn(3.0);
        LoanRequest request = new LoanRequest();
        request.setAmount(20000);
        request.setTermMonths(24);
        request.setCustomerBalance(5000);
        request.setMonthlyIncome(2000);

        LoanEvaluationResult result = algorithm.evaluate(request);

        assertFalse(result.isApproved());
        assertEquals("Cuota demasiado alta", result.getReason());
    }
    @Test
    void shouldApproveWhenPaymentIsWithinIncomeLimit() {

        Mockito.when(euriborServiceMock.getEuribor()).thenReturn(3.0);

        LoanRequest request = new LoanRequest();
        request.setAmount(20000);
        request.setTermMonths(24);
        request.setCustomerBalance(5000);
        request.setMonthlyIncome(2200);

        LoanEvaluationResult result = algorithm.evaluate(request);

        assertTrue(result.isApproved());
        assertEquals("Aprobado", result.getReason());
    }
}
