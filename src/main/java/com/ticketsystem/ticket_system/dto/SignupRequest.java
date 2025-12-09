package com.ticketsystem.ticket_system.dto;

import com.ticketsystem.ticket_system.model.User;
import lombok.Data;

import java.util.Set;

@Data
public class SignupRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private Set<User.Role> roles;
}
