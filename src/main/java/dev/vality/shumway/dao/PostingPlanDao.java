package dev.vality.shumway.dao;

import dev.vality.shumway.domain.PostingLog;
import dev.vality.shumway.domain.PostingOperation;
import dev.vality.shumway.domain.PostingPlanLog;

import java.util.List;
import java.util.Map;

public interface PostingPlanDao {

    /**
     * @return Created or updated plan (if already exists). Return null if last plan operation is not overridable.
     */
    PostingPlanLog addOrUpdatePlanLog(PostingPlanLog planLog) throws DaoException;

    /**
     * @return Updated plan if found and overridable, null if plan not found or last plan operation is not overridable.
     */
    PostingPlanLog updatePlanLog(PostingPlanLog planLog, PostingOperation postingOperation) throws DaoException;

    /**
     * @return Current plan with exclusive low level write lock, null if not found
     */
    PostingPlanLog getExclusivePlanLog(String planId) throws DaoException;

    /**
     * @return Current plan with shared lock, null if not found
     */
    PostingPlanLog getSharedPlanLog(String planId) throws DaoException;

    /**
     * @return Posting log records containing referred posting operation
     */
    Map<Long, List<PostingLog>> getPostingLogs(String planId, PostingOperation operation) throws DaoException;

    /**
     * Add new posting logs.
     * No duplication or data integrity checks're performed here. Referred data expected to be consistent.
     */
    void addPostingLogs(List<PostingLog> postingLogs) throws DaoException;

}
