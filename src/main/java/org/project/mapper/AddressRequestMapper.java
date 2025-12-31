package org.project.mapper;

import org.project.dto.requestDto.AddressRequest;
import org.project.model.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressRequestMapper
        implements Mapper<AddressRequest, Address> {

    @Override
    public Address map(AddressRequest request) {

        if (request == null) return null;

        return Address.builder()
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .landmark(request.getLandmark())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .build();
    }
    public void updateEntity(AddressRequest request, Address address) {

        if (request == null || address == null) return;

        if (request.getAddressLine1() != null)
            address.setAddressLine1(request.getAddressLine1());

        if (request.getAddressLine2() != null)
            address.setAddressLine2(request.getAddressLine2());

        if (request.getLandmark() != null)
            address.setLandmark(request.getLandmark());

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

