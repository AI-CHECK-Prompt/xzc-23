package com.fishing.platform.repository;

import com.fishing.platform.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, String> {

    @Query("select b from Bid b where b.listingId = :listingId order by b.bidPrice desc, b.createdAt asc")
    List<Bid> findByListing(@Param("listingId") String listingId);
}
