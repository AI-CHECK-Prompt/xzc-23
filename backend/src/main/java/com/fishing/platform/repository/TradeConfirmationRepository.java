package com.fishing.platform.repository;

import com.fishing.platform.entity.TradeConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TradeConfirmationRepository extends JpaRepository<TradeConfirmation, String> {

    Optional<TradeConfirmation> findByListingId(String listingId);

    Optional<TradeConfirmation> findByConfirmationNo(String confirmationNo);

    @Query("select t from TradeConfirmation t where t.vesselId = :vesselId or t.buyerName = :buyerName order by t.createdAt desc")
    List<TradeConfirmation> findTransactions(@Param("vesselId") String vesselId,
                                             @Param("buyerName") String buyerName);

    /**
     * 按 vesselNo 与日期查询当日确认单，用于生成每日序号。
     * 用 vesselNo 而非 vesselId 是因为该字段在确认单上有冗余，便于按业务键统计。
     */
    @Query("select t from TradeConfirmation t " +
            "where t.vesselNo = :vesselNo and t.createdAt >= :start and t.createdAt < :end " +
            "order by t.createdAt desc")
    List<TradeConfirmation> findByVesselNoAndDate(@Param("vesselNo") String vesselNo,
                                                  @Param("start") java.time.LocalDateTime start,
                                                  @Param("end") java.time.LocalDateTime end);
}
