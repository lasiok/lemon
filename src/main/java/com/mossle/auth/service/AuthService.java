package com.mossle.auth.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import com.mossle.auth.domain.Role;
import com.mossle.auth.domain.UserStatus;
import com.mossle.auth.manager.RoleManager;
import com.mossle.auth.manager.UserStatusManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class AuthService {
    private static Logger logger = LoggerFactory.getLogger(AuthService.class);
    private UserStatusManager userStatusManager;
    private RoleManager roleManager;

    public UserStatus createOrGetUserStatus(String username, String reference,
            String userRepoRef, String scopeId) {
        UserStatus userStatus = userStatusManager.findUnique(
                "from UserStatus where username=? and userRepoRef=?", username,
                userRepoRef);

        if (userStatus == null) {
            userStatus = new UserStatus();
            userStatus.setUsername(username);
            userStatus.setReference(reference);
            userStatus.setUserRepoRef(userRepoRef);
            userStatus.setScopeId(scopeId);
            // TODO: 考虑status同步的策略，目前是默认都设置成了有效
            userStatus.setStatus(1);
            userStatusManager.save(userStatus);
        }

        return userStatus;
    }

    public void configUserRole(Long userId, List<Long> roleIds,
            String userRepoRef, String scopeId, boolean clearRoles) {
        logger.debug("userId: {}, roleIds: {}", userId, roleIds);

        UserStatus userStatus = userStatusManager.get(userId);

        if (userStatus == null) {
            logger.warn("cannot find UserStatus : {}", userId);

            return;
        }

        if (clearRoles) {
            List<Role> roles = new ArrayList<Role>();

            roles.addAll(userStatus.getRoles());

            for (Role role : roles) {
                userStatus.getRoles().remove(role);
            }
        }

        for (Long roleId : roleIds) {
            Role role = roleManager.get(roleId);
            boolean skip = false;

            if (role == null) {
                logger.warn("role is null, roleId : {}", roleId);

                continue;
            }

            for (Role r : userStatus.getRoles()) {
                logger.debug("r.getId() : {}, role.getId() : {}", r.getId(),
                        role.getId());

                if (r.getId().equals(role.getId())) {
                    skip = true;

                    break;
                }
            }

            if (skip) {
                continue;
            }

            userStatus.getRoles().add(role);
        }

        userStatusManager.save(userStatus);
    }

    public List<Role> findRoles(String scopeId) {
        return roleManager.find("from Role where scopeId=?", scopeId);
    }

    @Resource
    public void setUserStatusManager(UserStatusManager userStatusManager) {
        this.userStatusManager = userStatusManager;
    }

    @Resource
    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }
}
