// Copyright (c) 2018, Yubico AB
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.kedacom.u2f.users;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kedacom.u2f.common.UserInfo;
import com.kedacom.u2f.common.UserListNode;
import com.kedacom.u2f.consts.U2fConsts;
import com.kedacom.u2f.data.CredentialRegistration;
import com.yubico.internal.util.CollectionUtil;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author wx
 */
@Slf4j
public class InMemoryRegistrationStorage implements RegistrationStorage, CredentialRepository {

    private final Cache<String, Map<ByteArray,CredentialRegistration>> storage = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build();
    private final Cache<String, User> userStorage = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build();
    @Override
    public boolean addUser(String username, String password) {
        userStorage.put(username, User.builder().username(username).password(password).build());
        return true;
    }

    @Override
    public boolean removeUser(String username) {
        userStorage.invalidate(username);
        storage.invalidate(username);
        return true;
    }

    @Override
    public boolean modifyPassword(String username, String pwd) {
        userStorage.getIfPresent(username).withPassword(pwd);
        return true;
    }

    @Override
    public boolean checkUser(String username, String pwd) {
        return pwd.equals(userStorage.getIfPresent(username).getPassword());
    }

    @Override
    public boolean delUserRegInfo(String username, ByteArray credentialId) {
        try {
            storage.get(username, HashMap::new).remove(credentialId);
            return true;
        } catch (ExecutionException e) {
            log.error("Failed to remove registration", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<UserListNode> getUserList(String username) {

        CopyOnWriteArrayList<UserListNode> userNodeList = new CopyOnWriteArrayList<UserListNode>();
        if (!U2fConsts.ADMINNAME.equals(username)) {
            if ((null != username) && (!"".equals(username))) {
                Map<String, RegisteredCredential> reginfo = new HashMap<>();
                UserListNode node = new UserListNode();
                node.setUserinfo(new UserInfo.Builder().withUser(userStorage.getIfPresent(username)).build());
                storage.getIfPresent(username).forEach((k,v) -> reginfo.put(k.toJsonString(),v.getCredential()));
                node.setUserreginfo(reginfo);
                userNodeList.add(node);
            }
        } else {
            userStorage.asMap().forEach((k,v)->{
                Map<String, RegisteredCredential> reginfo = new HashMap<>();
                UserListNode node = new UserListNode();
                node.setUserinfo(new UserInfo.Builder().withUser(v).build());
                storage.getIfPresent(k).forEach((kk,vv) -> reginfo.put(kk.toJsonString(),vv.getCredential()));
                node.setUserreginfo(reginfo);
                userNodeList.add(node);
            });
        }
        return userNodeList;
    }

    @Override
    public boolean addRegistrationByUsername(String username, CredentialRegistration reg) {
        try {
            storage.get(username, HashMap::new).put(reg.getCredential().getCredentialId(),reg);
            return true;
        } catch (ExecutionException e) {
            log.error("Failed to add registration", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return getRegistrationsByUsername(username).stream()
                .map(registration -> PublicKeyCredentialDescriptor.builder()
                        .id(registration.getCredential().getCredentialId())
                        .build())
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<CredentialRegistration> getRegistrationsByUsername(String username) {
        try {
            return storage.get(username, HashMap::new).values();
        } catch (ExecutionException e) {
            log.error("Registration lookup failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<CredentialRegistration> getRegistrationsByUserHandle(ByteArray userHandle) {
        return storage.asMap().values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .filter(credentialRegistration ->
                        userHandle.equals(credentialRegistration.getUserIdentity().getId())
                )
                .collect(Collectors.toList());
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return getRegistrationsByUserHandle(userHandle).stream()
                .findAny()
                .map(CredentialRegistration::getUsername);
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return getRegistrationsByUsername(username).stream()
                .findAny()
                .map(reg -> reg.getUserIdentity().getId());
    }

    @Override
    public void updateSignatureCount(AssertionResult result) {
        CredentialRegistration registration = getRegistrationByUsernameAndCredentialId(result.getUsername(), result.getCredentialId())
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        "Credential \"%s\" is not registered to user \"%s\"",
                        result.getCredentialId(), result.getUsername()
                )));

        Map<ByteArray,CredentialRegistration> regs = storage.getIfPresent(result.getUsername());
        regs.remove(result.getCredentialId());
        regs.put(result.getCredentialId(),registration.withSignatureCount(result.getSignatureCount()));
    }


    @Override
    public Optional<CredentialRegistration> getRegistrationByUsernameAndCredentialId(String username, ByteArray id) {
        try {
            return storage.get(username, HashMap::new).values().stream()
                    .filter(credReg -> id.equals(credReg.getCredential().getCredentialId()))
                    .findFirst();
        } catch (ExecutionException e) {
            log.error("Registration lookup failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean removeRegistrationByUsername(String username, CredentialRegistration credentialRegistration) {
        try {
            storage.get(username, HashMap::new).remove(credentialRegistration.getCredential().getCredentialId());
            return true;
        } catch (ExecutionException e) {
            log.error("Failed to remove registration", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean removeAllRegistrations(String username) {
        storage.invalidate(username);
        return true;
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        Optional<CredentialRegistration> registrationMaybe = storage.asMap().values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .filter(credReg -> credentialId.equals(credReg.getCredential().getCredentialId()))
                .findAny();

        log.debug("lookup credential ID: {}, user handle: {}; result: {}", credentialId, userHandle, registrationMaybe);
        return registrationMaybe.flatMap(registration ->
                Optional.of(
                        RegisteredCredential.builder()
                                .credentialId(registration.getCredential().getCredentialId())
                                .userHandle(registration.getUserIdentity().getId())
                                .publicKeyCose(registration.getCredential().getPublicKeyCose())
                                .signatureCount(registration.getSignatureCount())
                                .build()
                )
        );
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return CollectionUtil.immutableSet(
                storage.asMap().values().stream()
                        .map(Map::values)
                        .flatMap(Collection::stream)
                        .filter(reg -> reg.getCredential().getCredentialId().equals(credentialId))
                        .map(reg -> RegisteredCredential.builder()
                                .credentialId(reg.getCredential().getCredentialId())
                                .userHandle(reg.getUserIdentity().getId())
                                .publicKeyCose(reg.getCredential().getPublicKeyCose())
                                .signatureCount(reg.getSignatureCount())
                                .build()
                        )
                        .collect(Collectors.toSet()));
    }

}
