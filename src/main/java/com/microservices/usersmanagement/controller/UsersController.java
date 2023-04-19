package com.microservices.usersmanagement.controller;

import com.microservices.usersmanagement.dto.UpdatePasswordDTO;
import com.microservices.usersmanagement.dto.UserDTO;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    @Autowired
    Keycloak keycloak;


    @ConfigProperty(name = "quarkus.keycloak.admin-client.realm")
    String realm;


    @GetMapping("/me")
    public ResponseEntity<UserDTO> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = keycloak.realm("microservices").users().get(username).toRepresentation();

        UserDTO userDTO = new UserDTO(user);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(userDTO);
    }

@PutMapping
public ResponseEntity<String> updatePassword(@RequestBody UpdatePasswordDTO updatePasswordRequest) {
    String password = updatePasswordRequest.getPassword();
    String repeatedPassword = updatePasswordRequest.getRepeatedPassword();
    if (!password.equals(repeatedPassword)) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Passwords don't match");
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    UserRepresentation userRepresentation = keycloak.realm("microservices")
            .users().get(username).toRepresentation();

    CredentialRepresentation credentials = new CredentialRepresentation();
    credentials.setType(CredentialRepresentation.PASSWORD);
    credentials.setValue(password);
    credentials.setTemporary(false);
    userRepresentation.setCredentials(Arrays.asList(credentials));

    UserResource userResource = keycloak.realm("microservices")
            .users().get(userRepresentation.getId());
    userResource.update(userRepresentation);

    return ResponseEntity.ok("Password updated successfully");
}


}
