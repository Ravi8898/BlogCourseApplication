package org.project.mapper;

import org.project.dto.responseDto.AddressResponse;
import org.project.model.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressResponseMapper implements Mapper<Address, AddressResponse> {

    @Override
    public AddressResponse map(Address address) {

        if (address == null) {
            return null;
        }

        AddressResponse response = new AddressResponse();
        response.setAddressId(address.getId());
        response.setAddressLine1(address.getAddressLine1());
        response.setAddressLine2(address.getAddressLine2());
        response.setLandmark(address.getLandmark());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPostalCode(address.getPostalCode());

        return response;
    }
}

