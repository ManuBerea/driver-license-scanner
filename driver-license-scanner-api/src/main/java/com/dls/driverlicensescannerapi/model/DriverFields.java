package com.dls.driverlicensescannerapi.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverFields {
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String addressLine;
    private String postcode;
    private String licenceNumber;
    private String expiryDate;
    private List<String> categories;
}
