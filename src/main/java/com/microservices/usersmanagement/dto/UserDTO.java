package com.microservices.usersmanagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.UserRepresentation;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UserDTO {

//    @JsonProperty("id")
//    String id;
    String username;
    String email;
    String firstName;
    String lastName;
    String role;


    public UserDTO(UserRepresentation userRepresentation){
        UserDTO.builder()
                .email(userRepresentation.getEmail())
                .firstName(userRepresentation.getFirstName())
                .lastName(userRepresentation.getLastName()).build();
    }
}
