package org.project.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponse {
    private Long addressId;
    private String addressLine1;
    private String addressLine2;
    private String landmark;
    private String district;
    private String city;
    private String state;
    private String country;
    private String postalCode;
}
