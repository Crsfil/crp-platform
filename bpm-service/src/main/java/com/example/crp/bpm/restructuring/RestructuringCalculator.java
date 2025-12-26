package com.example.crp.bpm.restructuring;

import org.springframework.stereotype.Component;

@Component
public class RestructuringCalculator {

    public Result evaluate(double principal,
                           double annualRatePct,
                           int remainingTermMonths,
                           Double desiredPayment,
                           int graceMonths,
                           Double discountRateAnnualPct) {
        double monthlyRate = annualRatePct / 12.0 / 100.0;
        double discountedRate = (discountRateAnnualPct == null ? annualRatePct + 2.0 : discountRateAnnualPct) / 12.0 / 100.0;

        double adjustedPrincipal = principal;
        if (graceMonths > 0) {
            adjustedPrincipal *= Math.pow(1.0 + monthlyRate, graceMonths);
        }

        int termMonths = remainingTermMonths;
        double payment;
        if (desiredPayment != null && desiredPayment > 0) {
            termMonths = (int) Math.ceil(Math.log(desiredPayment / (desiredPayment - monthlyRate * adjustedPrincipal))
                    / Math.log(1.0 + monthlyRate));
            payment = desiredPayment;
        } else {
            payment = annuityPayment(adjustedPrincipal, monthlyRate, termMonths);
        }

        double npv = 0.0;
        for (int i = 1; i <= termMonths; i++) {
            npv += payment / Math.pow(1.0 + discountedRate, i);
        }
        npv -= principal;

        double marginPct = principal == 0.0 ? 0.0 : (npv / principal) * 100.0;
        return new Result(npv, marginPct, termMonths, payment);
    }

    private double annuityPayment(double principal, double monthlyRate, int termMonths) {
        if (termMonths <= 0) {
            return 0.0;
        }
        if (monthlyRate == 0.0) {
            return principal / termMonths;
        }
        double factor = Math.pow(1.0 + monthlyRate, termMonths);
        return principal * (monthlyRate * factor) / (factor - 1.0);
    }

    public record Result(double npv, double marginPct, int termMonths, double payment) { }
}
