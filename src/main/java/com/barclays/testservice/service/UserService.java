package com.barclays.testservice.service;

import com.barclays.testservice.exception.UserHasAccountsException;
import com.barclays.testservice.exception.UserNotAllowedException;
import com.barclays.testservice.exception.UserNotFoundException;
import com.barclays.testservice.model.User;
import com.barclays.testservice.repository.AddressRepository;
import com.barclays.testservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

    private static final String USER_ID_PREFIX = "usr-";
    private static final String ADDRESS_ID_PREFIX = "adr-";

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;


    public User createUser(User newUser, String password) {
        newUser.setId(getNextUserId());
        newUser.getAddress().setId(getNextAddressId());
        newUser.setPassword(passwordEncoder.encode(password));
        return userRepository.save(newUser);
    }

    public User getUserByUserId(String userId, String authUserId) {
        checkUserIdAllowed(userId, authUserId);

        return  userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }


    public User updateUserByUserId(String userId, User updateUser, String authUserId) {
        checkUserIdAllowed(userId, authUserId);

        var fetchedUser = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // Copy potentially updated fields to the fetched object
        fetchedUser.setName(updateUser.getName());
        fetchedUser.getAddress().setLine1(updateUser.getAddress().getLine1());
        fetchedUser.getAddress().setLine2(updateUser.getAddress().getLine2());
        fetchedUser.getAddress().setLine3(updateUser.getAddress().getLine3());
        fetchedUser.getAddress().setTown(updateUser.getAddress().getTown());
        fetchedUser.getAddress().setCounty(updateUser.getAddress().getCounty());
        fetchedUser.getAddress().setPostcode(updateUser.getAddress().getPostcode());
        fetchedUser.setPhoneNumber(updateUser.getPhoneNumber());
        fetchedUser.setEmail(updateUser.getEmail());

        return userRepository.save(fetchedUser);
    }

    public void deleteUserByUserId(String userId, String authUserId) {
        checkUserIdAllowed(userId, authUserId);

        if(!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }

        // If a user has bank accounts, we can't delete
        if(accountService.checkUserHasBankAccounts(userId)) {
            throw new UserHasAccountsException();
        }

        userRepository.deleteById(userId);
    }

    private void checkUserIdAllowed(String userId, String authUserId) {
        if(!authUserId.equals(userId)) {
            throw new UserNotAllowedException();
        }
    }

    private String getNextUserId() {
        return USER_ID_PREFIX + userRepository.getNextSequenceValue();
    }

    private String getNextAddressId() {
        return ADDRESS_ID_PREFIX + addressRepository.getNextSequenceValue();
    }
}
