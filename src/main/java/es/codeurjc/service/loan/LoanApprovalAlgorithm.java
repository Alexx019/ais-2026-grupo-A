package es.codeurjc.service.loan;

import es.codeurjc.model.LoanEvaluationResult;

public class LoanApprovalAlgorithm {

    private static final int LIMITE_INFERIOR = 1000;

    public LoanEvaluationResult evaluate(LoanRequest request) {

        if (request.getAmount() < LIMITE_INFERIOR){
            return new LoanEvaluationResult(false, "Cantidad fuera de rango");
        }

        return new LoanEvaluationResult(true, "Aprobado");
    }
}
