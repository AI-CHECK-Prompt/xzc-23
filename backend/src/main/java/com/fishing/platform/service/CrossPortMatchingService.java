package com.fishing.platform.service;

import com.fishing.platform.common.BusinessException;
import com.fishing.platform.entity.*;
import com.fishing.platform.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 跨渔港撮合服务
 * <p>
 * 业务流程：预挂牌 → 采购商报价 → 船东接受 → 生成电子交易确认单 → 双方签署
 * 状态机：预挂牌 OPEN→DEAL/CANCEL/EXPIRED；报价 PENDING→ACCEPTED/REJECTED/WITHDRAWN
 */
@Service
public class CrossPortMatchingService {

    private static final Logger log = LoggerFactory.getLogger(CrossPortMatchingService.class);

    @Autowired private PreListingRepository listingRepo;
    @Autowired private BidRepository bidRepo;
    @Autowired private TradeConfirmationRepository confRepo;
    @Autowired private VesselRepository vesselRepo;

    private static final String LISTING_OPEN = "OPEN";
    private static final String LISTING_DEAL = "DEAL";
    private static final String LISTING_CANCEL = "CANCEL";
    private static final String LISTING_EXPIRED = "EXPIRED";
    private static final String BID_PENDING = "PENDING";
    private static final String BID_ACCEPTED = "ACCEPTED";
    private static final String BID_REJECTED = "REJECTED";
    private static final String BID_WITHDRAWN = "WITHDRAWN";
    private static final String CONF_DRAFT = "DRAFT";
    private static final String CONF_SIGNED = "SIGNED";

    // ============ 预挂牌 ============

    public PreListing createListing(PreListing listing) {
        if (listing.getVesselId() == null) {
            throw new BusinessException("缺少 vesselId");
        }
        Vessel vessel = vesselRepo.findById(listing.getVesselId())
                .orElseThrow(() -> new BusinessException("船舶不存在"));
        listing.setVesselNo(vessel.getVesselNo());
        listing.setVesselName(vessel.getVesselName());
        listing.setOwnerName(vessel.getOwnerName());
        listing.setPortName(vessel.getPortName());
        listing.setSeaAreaName(vessel.getSeaAreaName());
        listing.setId(UUID.randomUUID().toString().replace("-", ""));
        listing.setStatus(LISTING_OPEN);
        listing.setCreatedAt(LocalDateTime.now());
        listing.setUpdatedAt(LocalDateTime.now());
        log.info("【撮合-预挂牌】船 {} 发布 {} 期望价 {} 元/kg", vessel.getVesselNo(),
                listing.getSpecies(), listing.getExpectedPrice());
        return listingRepo.save(listing);
    }

    public List<PreListing> searchListings(String seaArea, String species, String portName, String status) {
        return listingRepo.search(seaArea, species, portName, status);
    }

    public List<PreListing> findListingsByVessel(String vesselId) {
        return listingRepo.findByVessel(vesselId);
    }

    public PreListing cancelListing(String id) {
        PreListing listing = mustListing(id);
        if (!LISTING_OPEN.equals(listing.getStatus())) {
            throw new BusinessException("当前状态 " + listing.getStatus() + " 不可下架");
        }
        listing.setStatus(LISTING_CANCEL);
        listing.setUpdatedAt(LocalDateTime.now());
        log.info("【撮合-预挂牌】{} 已下架", id);
        return listingRepo.save(listing);
    }

    // ============ 报价 ============

    public Bid createBid(Bid bid) {
        PreListing listing = mustListing(bid.getListingId());
        if (!LISTING_OPEN.equals(listing.getStatus())) {
            throw new BusinessException("该预挂牌当前状态 " + listing.getStatus() + "，不接受新报价");
        }
        if (bid.getBidPrice() == null || bid.getBidPrice().signum() <= 0) {
            throw new BusinessException("报价必须大于 0");
        }
        bid.setId(UUID.randomUUID().toString().replace("-", ""));
        bid.setStatus(BID_PENDING);
        bid.setCreatedAt(LocalDateTime.now());
        log.info("【撮合-报价】{} 对挂牌 {} 报价 {} 元/kg", bid.getBuyerName(),
                listing.getId(), bid.getBidPrice());
        return bidRepo.save(bid);
    }

    public List<Bid> findBidsByListing(String listingId) {
        return bidRepo.findByListing(listingId);
    }

    public Bid withdrawBid(String id) {
        Bid bid = mustBid(id);
        if (!BID_PENDING.equals(bid.getStatus())) {
            throw new BusinessException("当前状态 " + bid.getStatus() + " 不可撤销");
        }
        bid.setStatus(BID_WITHDRAWN);
        log.info("【撮合-报价】{} 已撤销", id);
        return bidRepo.save(bid);
    }

    // ============ 撮合成交 + 生成确认单 ============

    /**
     * 船东接受某报价：原子化操作
     * 1) 预挂牌 → DEAL
     * 2) 该报价 → ACCEPTED
     * 3) 同挂牌其他 PENDING 报价 → REJECTED
     * 4) 生成电子交易确认单（DRAFT 状态）
     */
    @Transactional
    public TradeConfirmation accept(String listingId, String bidId) {
        PreListing listing = mustListing(listingId);
        if (!LISTING_OPEN.equals(listing.getStatus())) {
            throw new BusinessException("当前挂牌状态 " + listing.getStatus() + "，不可成交");
        }
        Bid accepted = mustBid(bidId);
        if (!accepted.getListingId().equals(listingId)) {
            throw new BusinessException("报价与挂牌不匹配");
        }
        if (!BID_PENDING.equals(accepted.getStatus())) {
            throw new BusinessException("该报价状态 " + accepted.getStatus() + "，不可接受");
        }

        listing.setStatus(LISTING_DEAL);
        listing.setUpdatedAt(LocalDateTime.now());
        listingRepo.save(listing);

        accepted.setStatus(BID_ACCEPTED);
        bidRepo.save(accepted);

        for (Bid other : bidRepo.findByListing(listingId)) {
            if (other == accepted) continue;
            if (BID_PENDING.equals(other.getStatus())) {
                other.setStatus(BID_REJECTED);
                bidRepo.save(other);
            }
        }

        TradeConfirmation conf = new TradeConfirmation();
        conf.setId(UUID.randomUUID().toString().replace("-", ""));
        conf.setConfirmationNo(generateConfirmationNo(listing));
        conf.setListingId(listingId);
        conf.setBidId(bidId);
        conf.setVesselId(listing.getVesselId());
        conf.setVesselNo(listing.getVesselNo());
        conf.setPortName(listing.getPortName());
        conf.setSeaAreaName(listing.getSeaAreaName());
        conf.setSpecies(listing.getSpecies());
        conf.setWeight(accepted.getBidWeight() != null ? accepted.getBidWeight() : listing.getExpectedWeight());
        conf.setUnitPrice(accepted.getBidPrice());
        BigDecimal amount = conf.getWeight() == null ? BigDecimal.ZERO
                : conf.getWeight().multiply(conf.getUnitPrice());
        conf.setTotalAmount(amount);
        conf.setBuyerName(accepted.getBuyerName());
        conf.setSellerName(listing.getOwnerName());
        conf.setDestination(accepted.getDestination());
        conf.setStatus(CONF_DRAFT);
        conf.setCreatedAt(LocalDateTime.now());
        conf.setUpdatedAt(LocalDateTime.now());
        confRepo.save(conf);
        log.info("【撮合-成交】挂牌 {} 被接受，确认单 {} 金额 {}", listingId, conf.getConfirmationNo(), amount);
        return conf;
    }

    // ============ 电子签署 ============

    public TradeConfirmation sign(String confirmationId, String party, String signer, boolean buyerSide) {
        TradeConfirmation conf = confRepo.findById(confirmationId)
                .orElseThrow(() -> new BusinessException("确认单不存在"));
        if (!CONF_DRAFT.equals(conf.getStatus())) {
            throw new BusinessException("当前状态 " + conf.getStatus() + "，不可签署");
        }
        LocalDateTime now = LocalDateTime.now();
        if (buyerSide) {
            conf.setSignedByBuyer(signer);
            conf.setSignedAtByBuyer(now);
        } else {
            conf.setSignedBySeller(signer);
            conf.setSignedAtBySeller(now);
        }
        if (conf.getSignedByBuyer() != null && conf.getSignedBySeller() != null) {
            conf.setStatus(CONF_SIGNED);
            log.info("【撮合-确认单】{} 双方已签署，交易成立", conf.getConfirmationNo());
        } else {
            log.info("【撮合-确认单】{} {} 签署完成", conf.getConfirmationNo(), party);
        }
        conf.setUpdatedAt(now);
        return confRepo.save(conf);
    }

    // ============ 查询 ============

    public TradeConfirmation findConfirmation(String id) {
        return confRepo.findById(id).orElse(null);
    }

    public List<TradeConfirmation> findTransactions(String vesselId, String buyerName) {
        return confRepo.findTransactions(vesselId, buyerName);
    }

    // ============ 工具方法 ============

    private PreListing mustListing(String id) {
        return listingRepo.findById(id)
                .orElseThrow(() -> new BusinessException("预挂牌不存在: " + id));
    }

    private Bid mustBid(String id) {
        return bidRepo.findById(id)
                .orElseThrow(() -> new BusinessException("报价不存在: " + id));
    }

    private String generateConfirmationNo(PreListing listing) {
        // 渔港名-船号-YYYYMMDD-序号
        String port = listing.getPortName() == null ? "未知" : listing.getPortName();
        String vesselNo = listing.getVesselNo() == null ? "未知" : listing.getVesselNo();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 按 vesselNo + 当日 范围查实际已生成的确认单（避免 v.id=null 等导致全表扫描不到的问题）
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long sameCount = confRepo.findByVesselNoAndDate(vesselNo, start, end).size();
        return port + "-" + vesselNo + "-" + date + "-" + String.format("%03d", sameCount + 1);
    }
}
