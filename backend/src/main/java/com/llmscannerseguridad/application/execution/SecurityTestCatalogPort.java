package com.llmscannerseguridad.application.execution;

import java.util.List;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;

public interface SecurityTestCatalogPort {
    List<SecurityTest> findAllOrdered();
}
