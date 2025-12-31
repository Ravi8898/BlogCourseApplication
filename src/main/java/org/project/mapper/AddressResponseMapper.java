package org.project.mapper;

import org.project.dto.responseDto.AddressResponse;
import org.project.model.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressResponseMapper implements Mapper<Address, AddressResponse> {

    /**
     * Maps Address entity to AddressResponse DTO
     * Used while sending address data in API responses
     */
    @Override
    public AddressResponse map(Address address) {

        // Return null if address entity is null to avoid NullPointerException
        if (address == null) {
            return null;
        }

        // Create response DTO object
        AddressResponse response = new AddressResponse();

        // Map entity fields to response DTO
        response.setAddressId(address.getId());
        response.setAddressLine1(address.getAddressLine1());
        response.setAddressLine2(address.getAddressLine2());
        response.setLandmark(address.getLandmark());
        response.setDistrict(address.getDistrict());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPostalCode(address.getPostalCode());

        // Return populated response DTO
        return response;
    }
}
