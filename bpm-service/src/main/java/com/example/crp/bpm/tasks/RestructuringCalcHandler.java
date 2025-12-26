package com.example.crp.bpm.tasks;

import com.example.crp.bpm.restructuring.RestructuringCalculator;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RestructuringCalcHandler implements CamundaTaskHandler {
    private final RestructuringCalculator calculator;
    private final ApplicationStatusPublisher statusPublisher;

    public RestructuringCalcHandler(RestructuringCalculator calculator, ApplicationStatusPublisher statusPublisher) {
        this.calculator = calculator;
        this.statusPublisher = statusPublisher;
    }

    @Override
    public String topic() {
        return "restructure-calc";
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            Double principal = TaskVariables.getDouble(task, "outstandingPrincipal");
            Double rateAnnualPct = TaskVariables.getDouble(task, "rateAnnualPct");
            Integer remainingTerm = TaskVariables.getInt(task, "remainingTermMonths");
            Double desiredPayment = TaskVariables.getDouble(task, "desiredPayment");
            Integer graceMonths = TaskVariables.getInt(task, "graceMonths");
            Double minMarginPct = TaskVariables.getDouble(task, "minMarginPct");
            Double discountRateAnnualPct = TaskVariables.getDouble(task, "discountRateAnnualPct");
            Long applicationId = TaskVariables.getLong(task, "applicationId");

            int grace = graceMonths == null ? 0 : graceMonths;
            double principalValue = principal == null ? 0.0 : principal;
            double rateValue = rateAnnualPct == null ? 0.0 : rateAnnualPct;
            int termValue = remainingTerm == null ? 0 : remainingTerm;

            RestructuringCalculator.Result result = calculator.evaluate(
                    principalValue,
                    rateValue,
                    termValue,
                    desiredPayment,
                    grace,
                    discountRateAnnualPct
            );

            double minMargin = minMarginPct == null ? 8.0 : minMarginPct;
            boolean approved = result.marginPct() >= minMargin;

            Map<String, Object> vars = new HashMap<>();
            vars.put("npv", result.npv());
            vars.put("marginPct", result.marginPct());
            vars.put("newTermMonths", result.termMonths());
            vars.put("newPayment", result.payment());
            vars.put("approved", approved);
            vars.put("status", approved ? "RESTRUCTURE_APPROVED" : "RESTRUCTURE_REVIEW");
            service.complete(task, vars);
            statusPublisher.publishStatus(applicationId, approved ? "RESTRUCTURE_APPROVED" : "RESTRUCTURE_REVIEW");
        } catch (Exception ex) {
            CamundaTaskFailures.handleFailure(service, task, ex);
        }
    }
}
