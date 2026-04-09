package es.codeurjc.service.loan;

import es.codeurjc.model.LoanEvaluationResult;
import es.codeurjc.service.EuriborService;

public class LoanApprovalAlgorithm {

    private final EuriborService euriborService;

    public LoanApprovalAlgorithm() {
        this.euriborService = new EuriborService();
    }

    public LoanApprovalAlgorithm(EuriborService euriborService) {
        this.euriborService = euriborService;
    }

    private static final int LIMITE_INFERIOR = 1000;
    private static final int LIMITE_SUPEIOR = 50000;

    private static final int PLAZO_MESES_MIN = 6;
    private static final int PLAZO_MESES_MAX = 120;

    private static final double PORCENTAJE_SALDO_MINIMO = 0.20;

    public LoanEvaluationResult evaluate(LoanRequest request) {

        if (request.getAmount() < LIMITE_INFERIOR || request.getAmount() > LIMITE_SUPEIOR){
            return new LoanEvaluationResult(false, "Cantidad fuera de rango");
        }

        if (request.getTermMonths() < PLAZO_MESES_MIN || request.getTermMonths() > PLAZO_MESES_MAX){
            return  new LoanEvaluationResult(false, "Plazo no válido");
        }

        if (request.getCustomerBalance() < (request.getAmount() * PORCENTAJE_SALDO_MINIMO)) {
            return new LoanEvaluationResult(false, "Saldo insuficiente");
        }

        double interestRate = 2.0 + euriborService.getEuribor();
        double totalAmount = request.getAmount() + (request.getAmount() * (interestRate / 100));
        double monthlyPayment = totalAmount / request.getTermMonths();
        return new LoanEvaluationResult(true, "Aprobado", request.getAmount(), interestRate, monthlyPayment);
    }
}
