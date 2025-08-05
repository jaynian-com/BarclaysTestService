package com.barclays.testservice.repository;

import com.barclays.testservice.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, String> {
    @Query(value = "SELECT NEXTVAL('userdetail_seq')", nativeQuery = true)
    Long getNextSequenceValue();
}
