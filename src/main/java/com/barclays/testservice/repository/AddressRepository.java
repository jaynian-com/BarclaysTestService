package com.barclays.testservice.repository;

import com.barclays.testservice.model.Address;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface AddressRepository extends CrudRepository<Address, String> {
    @Query(value = "SELECT NEXTVAL('address_seq')", nativeQuery = true)
    Long getNextSequenceValue();
}
