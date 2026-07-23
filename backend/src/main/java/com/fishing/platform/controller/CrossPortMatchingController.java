package com.fishing.platform.controller;

import com.fishing.platform.common.ApiResult;
import com.fishing.platform.entity.Bid;
import com.fishing.platform.entity.PreListing;
import com.fishing.platform.entity.TradeConfirmation;
import com.fishing.platform.service.CrossPortMatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 跨渔港撮合
 */
@RestController
@RequestMapping("/api/matching")
public class CrossPortMatchingController {

    @Autowired private CrossPortMatchingService service;

    // ---- 预挂牌 ----
    @PostMapping("/listing/create")
    public ApiResult<PreListing> createListing(@RequestBody PreListing listing) {
        return ApiResult.ok(service.createListing(listing));
    }

    @GetMapping("/listing/search")
    public ApiResult<List<PreListing>> searchListings(@RequestParam(required = false) String seaArea,
                                                      @RequestParam(required = false) String species,
                                                      @RequestParam(required = false) String portName,
                                                      @RequestParam(required = false) String status) {
        return ApiResult.ok(service.searchListings(seaArea, species, portName, status));
    }

    @GetMapping("/listing/byVessel/{vesselId}")
    public ApiResult<List<PreListing>> byVessel(@PathVariable String vesselId) {
        return ApiResult.ok(service.findListingsByVessel(vesselId));
    }

    @PostMapping("/listing/cancel/{id}")
    public ApiResult<PreListing> cancelListing(@PathVariable String id) {
        return ApiResult.ok(service.cancelListing(id));
    }

    // ---- 报价 ----
    @PostMapping("/bid/create")
    public ApiResult<Bid> createBid(@RequestBody Bid bid) {
        return ApiResult.ok(service.createBid(bid));
    }

    @GetMapping("/bid/byListing/{listingId}")
    public ApiResult<List<Bid>> bidsByListing(@PathVariable String listingId) {
        return ApiResult.ok(service.findBidsByListing(listingId));
    }

    @PostMapping("/bid/withdraw/{id}")
    public ApiResult<Bid> withdrawBid(@PathVariable String id) {
        return ApiResult.ok(service.withdrawBid(id));
    }

    // ---- 撮合成交 ----
    @PostMapping("/accept")
    public ApiResult<TradeConfirmation> accept(@RequestParam String listingId,
                                              @RequestParam String bidId) {
        return ApiResult.ok(service.accept(listingId, bidId));
    }

    // ---- 电子签署 ----
    @PostMapping("/confirmation/sign")
    public ApiResult<TradeConfirmation> sign(@RequestParam String confirmationId,
                                             @RequestParam String party,
                                             @RequestParam String signer,
                                             @RequestParam boolean buyerSide) {
        return ApiResult.ok(service.sign(confirmationId, party, signer, buyerSide));
    }

    @GetMapping("/confirmation/{id}")
    public ApiResult<TradeConfirmation> getConfirmation(@PathVariable String id) {
        return ApiResult.ok(service.findConfirmation(id));
    }

    // ---- 交易流水 ----
    @GetMapping("/transactions")
    public ApiResult<List<TradeConfirmation>> transactions(@RequestParam(required = false) String vesselId,
                                                            @RequestParam(required = false) String buyerName) {
        return ApiResult.ok(service.findTransactions(vesselId, buyerName));
    }
}
