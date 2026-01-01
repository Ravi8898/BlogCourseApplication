package org.project.mapper;

import org.project.dto.requestDto.AddressRequest;
import org.project.model.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressRequestMapper
        implements Mapper<AddressRequest, Address> {

    /**
     * Maps AddressRequest DTO to a new Address entity
     * Used during user registration or address creation
     */
    @Override
    public Address map(AddressRequest request) {

        // Return null if request is null to avoid NullPointerException
        if (request == null) return null;

        // Build and return new Address entity from request data
        return Address.builder()
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .landmark(request.getLandmark())
                .district(request.getDistrict())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .build();
    }

    /**
     * Updates an existing Address entity with non-null fields from AddressRequest
     * Used for partial address updates (PATCH-like behavior)
     */
    public void updateEntity(AddressRequest request, Address address) {

        // If request or entity is null, do nothing
        if (request == null || address == null) return;

        // Update only fields that are provided in the request
        if (request.getAddressLine1() != null)
            address.setAddressLine1(request.getAddressLine1());

        if (request.getAddressLine2() != null)
            address.setAddressLine2(request.getAddressLine2());

        if (request.getLandmark() != null)
            address.setLandmark(request.getLandmark());

        if (request.getDistrict() != null)
            address.setDistrict(request.getDistrict());

        if (request.getCity() != null)
            address.setCity(request.getCity());

        if (request.getState() != null)
            address.setState(request.getState());

        if (request.getCountry() != null)
            address.setCountry(request.getCountry());

        if (request.getPostalCode() != null)
            address.setPostalCode(request.getPostalCode());
    }
}
