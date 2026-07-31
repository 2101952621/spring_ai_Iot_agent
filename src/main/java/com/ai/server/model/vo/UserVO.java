package com.ai.server.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String gender;
    private String country;
    private Boolean enabled;
    private Boolean activated;
}
