package com.kedacom.u2f.users;

import lombok.Builder;
import lombok.Value;
import lombok.With;

/**
 * @author : wx
 * @version : 1
 * @date : 2020/10/14 11:45
 */
@With
@Value
@Builder
public class User {
    private String username;
    private String password;
}
