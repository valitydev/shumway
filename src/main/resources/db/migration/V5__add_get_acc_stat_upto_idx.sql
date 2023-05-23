CREATE INDEX IF NOT EXISTS get_acc_stat_upto_idx
    ON shm.account_log
    USING btree
    (account_id, plan_id, batch_id);
