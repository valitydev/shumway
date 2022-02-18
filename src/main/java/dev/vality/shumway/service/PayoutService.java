package dev.vality.shumway.service;

import dev.vality.geck.common.util.TypeUtil;
import dev.vality.shumway.dao.PayoutDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayoutService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PayoutDao payoutDao;

    public PayoutService(PayoutDao payoutDao) {
        this.payoutDao = payoutDao;
    }

    public long getAccountAvailableAmount(long id, String time) {
        log.debug("Get account available amount: {}", id);
        long amount = payoutDao.getStatefulAccountAvailableAmount(id,
                TypeUtil.stringToLocalDateTime(time));
        log.debug("Got account available amount: {}", id);
        return amount;
    }
}
