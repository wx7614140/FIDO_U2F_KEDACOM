package com.kedacom.u2f.users;

import com.kedacom.u2f.common.UserListNode;
import com.kedacom.u2f.data.CredentialRegistration;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.data.ByteArray;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationStorage extends CredentialRepository {

    boolean addRegistrationByUsername(String username, CredentialRegistration reg);

    Collection<CredentialRegistration> getRegistrationsByUsername(String username);

    Optional<CredentialRegistration> getRegistrationByUsernameAndCredentialId(String username, ByteArray credentialId);

    Collection<CredentialRegistration> getRegistrationsByUserHandle(ByteArray userHandle);

    default boolean userExists(String username) {
        return !getRegistrationsByUsername(username).isEmpty();
    }

    boolean removeRegistrationByUsername(String username, CredentialRegistration credentialRegistration);

    boolean removeAllRegistrations(String username);

    void updateSignatureCount(AssertionResult result);

    boolean addUser(String username, String password);

    boolean removeUser(String username);

    boolean modifyPassword(String username, String pwd);

    boolean checkUser(String username, String pwd);


    boolean delUserRegInfo(String username,ByteArray credentialId);

    List<UserListNode> getUserList(String username); //if admin,return all;if not admin,just return himself.
}
