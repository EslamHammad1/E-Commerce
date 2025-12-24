package com.example.E_commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============= Address Request DTO =============
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequestDTO {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Street address is required")
    @Size(min = 5, max = 200)
    private String streetAddress;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 100)
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(min = 3, max = 20)
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 100)
    private String country;

    private Boolean isDefault = false;
}
