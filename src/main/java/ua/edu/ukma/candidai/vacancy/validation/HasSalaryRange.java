package ua.edu.ukma.candidai.vacancy.validation;

import java.math.BigDecimal;

public interface HasSalaryRange {

    BigDecimal salaryMin();

    BigDecimal salaryMax();
}
