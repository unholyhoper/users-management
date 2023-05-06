package com.microservices.usersmanagement.controller;

import com.microservices.usersmanagement.dto.UpdatePasswordDTO;
import com.microservices.usersmanagement.dto.UserDTO;
import com.microservices.usersmanagement.dto.UserRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    @PostMapping
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    @Consumes(MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createUser(@RequestBody UserRequest userRequest) {
        String userName = userRequest.getUserName();
        String password = userRequest.getPassword();
        String email = userRequest.getEmail();
        String name = userRequest.getName();
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) || StringUtils.isEmpty(email)) {
            return ResponseEntity.badRequest().body("Empty username or password");
        }

        List<UserRepresentation> userRepresentations = keycloak.realm("SELECTION-ENGINE").users().list();
        for (UserRepresentation userRepresentation : userRepresentations) {
            if (userRepresentation.getEmail().equalsIgnoreCase(email) || userRepresentation.getUsername().equalsIgnoreCase(userName)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User with same email or username already exists");
            }
        }

        CredentialRepresentation credentials = new CredentialRepresentation();
        credentials.setType(CredentialRepresentation.PASSWORD);
        credentials.setValue(password);
        credentials.setTemporary(false);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userName);
        userRepresentation.setFirstName(name);
        userRepresentation.setLastName(name);
        userRepresentation.setEmail(email);
        userRepresentation.setEnabled(true);
        userRepresentation.setCredentials(Arrays.asList(credentials));
        userRepresentation.setEmailVerified(false);
        userRepresentation.setRequiredActions(Collections.singletonList(UserModel.RequiredAction.VERIFY_EMAIL.name()));
        Response result = keycloak.realm("microservices").users().create(userRepresentation);

        int status = result.getStatus();
        assignRoles(userRepresentation.getId(), Arrays.asList("admin"));

        if (status == 201 || status == 200 || status == 204) {
            userRepresentation = keycloak.realm("microservices").users().searchByEmail(email, true).get(0);
            keycloak.realm("microservices").users().get(userRepresentation.getId()).sendVerifyEmail();
            return ResponseEntity.status(status).body("User created");
        }

        //Roles management :

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while creating user");
    }

    private void assignRoles(String userId, List<String> roles) {

        List<RoleRepresentation> roleList = rolesToRealmRoleRepresentation(roles);
        keycloak.realm("microservices").users().get(userId).roles().realmLevel().add(roleList);

    }

    private List<RoleRepresentation> rolesToRealmRoleRepresentation(List<String> roles) {

        List<RoleRepresentation> existingRoles = keycloak.realm("microservices").roles().list();

        List<String> serverRoles = existingRoles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
        List<RoleRepresentation> resultRoles = new ArrayList<>();

        for (String role : roles) {
            int index = serverRoles.indexOf(role);
            if (index != -1) {
                resultRoles.add(existingRoles.get(index));
            } else {
                System.out.println("Role does not exist");
            }
        }
        return resultRoles;
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


    @DeleteMapping("/{id}")
    @RolesAllowed("admin")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        keycloak.realm("microservices").users().get(id).remove();
        return ResponseEntity.ok().build();
    }


}
