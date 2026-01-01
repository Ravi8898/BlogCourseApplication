package org.project.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressRequest {

    @NotBlank(message = "Address line1 is required")
    private String addressLine1;

    private String addressLine2;

    private String landmark;

    private String district;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Postal code is required")
    private String postalCode;
}
