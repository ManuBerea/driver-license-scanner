package com.dls.driverlicensescannerapi.model;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    @Builder.Default
    private List<ValidationError> blockingErrors = Collections.emptyList();

    @Builder.Default
    private List<String> warnings = Collections.emptyList();
}
