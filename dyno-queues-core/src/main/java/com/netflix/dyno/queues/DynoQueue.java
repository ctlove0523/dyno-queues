/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *
 */
package com.netflix.dyno.queues;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Viren
 * Abstraction of a dyno queue.
 */
public interface DynoQueue extends Closeable {

    /**
     *
     * @return Returns the name of the queue
     */
	String getName();

    /**
     *
     * @return Time in milliseconds before the messages that are popped and not acknowledge are pushed back into the queue.
     * @see #ack(String)
     */
	int getUnackTime();

    /**
     *
     * @param messages messages to be pushed onto the queue
     * @return Returns the list of message ids
     */
	List<String> push(List<Message> messages);

    /**
     *
     * @param messageCount number of messages to be popped out of the queue.
     * @param wait Amount of time to wait if there are no messages in queue
     * @param unit Time unit for the wait period
     * @return messages.  Can be less than the messageCount if there are fewer messages available than the message count.
	 *                    If the popped messages are not acknowledge in a timely manner, they are pushed back into the queue.
     * @see #peek(int)
     * @see #ack(String)
     * @see #getUnackTime()
     *
     */
	List<Message> pop(int messageCount, int wait, TimeUnit unit);

    /**
     * Pops "messageId" from the local shard if it exists.
     * Note that if "messageId" is present in a different shard, we will be unable to pop it.
     *
     * @param messageId ID of message to pop
     * @return Returns a "Message" object if pop was successful. 'null' otherwise.
     */
	Message popWithMsgId(String messageId);

    /**
     * Provides a peek into the queue without taking messages out.
	 *
	 * Note: This peeks only into the 'local' shard.
	 *
     * @param messageCount number of messages to be peeked.
     * @return List of peeked messages.
     * @see #pop(int, int, TimeUnit)
     */
	List<Message> peek(int messageCount);

    /**
     * Provides an acknowledgement for the message.  Once ack'ed the message is removed from the queue forever.
     * @param messageId ID of the message to be acknowledged
     * @return true if the message was found pending acknowledgement and is now ack'ed.  false if the message id is invalid or message is no longer present in the queue.
     */
	boolean ack(String messageId);


    /**
     * Bulk version for {@link #ack(String)}
     * @param messages Messages to be acknowledged.  Each message MUST be populated with id and shard information.
     */
	void ack(List<Message> messages);

    /**
     * Sets the unack timeout on the message (changes the default timeout to the new value).  Useful when extended lease is required for a message by consumer before sending ack.
     * @param messageId ID of the message to be acknowledged
     * @param timeout time in milliseconds for which the message will remain in un-ack state.  If no ack is received after the timeout period has expired, the message is put back into the queue
     * @return true if the message id was found and updated with new timeout.  false otherwise.
     */
	boolean setUnackTimeout(String messageId, long timeout);


    /**
     * Updates the timeout for the message.
     * @param messageId ID of the message to be acknowledged
     * @param timeout time in milliseconds for which the message will remain invisible and not popped out of the queue.
     * @return true if the message id was found and updated with new timeout.  false otherwise.
     */
	boolean setTimeout(String messageId, long timeout);

    /**
     *
     * @param messageId  Remove the message from the queue
     * @return true if the message id was found and removed.  False otherwise.
     */
	boolean remove(String messageId);
	boolean atomicRemove(String messageId);

    /**
     * Enqueues 'message' if it doesn't exist in any of the shards or unack sets.
     *
     * @param message Message to enqueue if it doesn't exist.
     * @return true if message was enqueued. False if messageId already exists.
     */
	boolean ensure(Message message);

    /**
     * Checks the message bodies (i.e. the data in the hash map), and returns true on the first match with
     * 'predicate'.
     *
     * Matching is done based on 'lua pattern' matching.
     * http://lua-users.org/wiki/PatternsTutorial
     *
     * Disclaimer: This is a potentially expensive call, since we will iterate over the entire hash map in the
     * worst case. Use mindfully.
     *
     * @param predicate The predicate to check against.
     * @return 'true' if any of the messages contain 'predicate'; 'false' otherwise.
     */
	boolean containsPredicate(String predicate);

	/**
	 * Checks the message bodies (i.e. the data in the hash map), and returns true on the first match with
	 * 'predicate'.
	 *
	 * Matching is done based on 'lua pattern' matching.
	 * http://lua-users.org/wiki/PatternsTutorial
	 *
	 * Disclaimer: This is a potentially expensive call, since we will iterate over the entire hash map in the
	 * worst case. Use mindfully.
	 *
	 * @param predicate The predicate to check against.
	 * @param localShardOnly If this is true, it will only check if the message exists in the local shard as opposed to
	 *                       all shards. Note that this will only work if the Dynomite cluster ring size is 1 (i.e. one
	 *                       instance per AZ).
	 * @return 'true' if any of the messages contain 'predicate'; 'false' otherwise.
	 */
	boolean containsPredicate(String predicate, boolean localShardOnly);

    /**
     * Checks the message bodies (i.e. the data in the hash map), and returns the ID of the first message to match with
     * 'predicate'.
     *
     * Matching is done based on 'lua pattern' matching.
     * http://lua-users.org/wiki/PatternsTutorial
     *
     * Disclaimer: This is a potentially expensive call, since we will iterate over the entire hash map in the
     * worst case. Use mindfully.
     *
     * @param predicate The predicate to check against.
     * @return Message ID as string if any of the messages contain 'predicate'; 'null' otherwise.
     */
	String getMsgWithPredicate(String predicate);

	/**
	 * Checks the message bodies (i.e. the data in the hash map), and returns the ID of the first message to match with
	 * 'predicate'.
	 *
	 * Matching is done based on 'lua pattern' matching.
	 * http://lua-users.org/wiki/PatternsTutorial
	 *
	 * Disclaimer: This is a potentially expensive call, since we will iterate over the entire hash map in the
	 * worst case. Use mindfully.
	 *
	 * @param predicate The predicate to check against.
	 * @param localShardOnly If this is true, it will only check if the message exists in the local shard as opposed to
	 *                       all shards. Note that this will only work if the Dynomite cluster ring size is 1 (i.e. one
	 *                       instance per AZ).
	 * @return Message ID as string if any of the messages contain 'predicate'; 'null' otherwise.
	 */
	String getMsgWithPredicate(String predicate, boolean localShardOnly);

	/**
	 * Pops the message with the highest priority that matches 'predicate'.
	 *
	 * Note: Can be slow for large queues.
	 *
	 * @param predicate The predicate to check against.
	 * @param localShardOnly If this is true, it will only check if the message exists in the local shard as opposed to
	 *                       all shards. Note that this will only work if the Dynomite cluster ring size is 1 (i.e. one
	 *                       instance per AZ).
	 * @return
	 */
	Message popMsgWithPredicate(String predicate, boolean localShardOnly);

    /**
     *
     * @param messageId message to be retrieved.
     * @return Retrieves the message stored in the queue by the messageId.  Null if not found.
     */
	Message get(String messageId);

	/**
	 *
	 * Attempts to return all the messages found in the hashmap. It's a best-effort return of all payloads, i.e. it may
	 * not 100% match with what's in the queue metadata at any given time and is read with a non-quorum connection.
	 *
	 * @return Returns a list of all messages found in the message hashmap.
	 */
	List<Message> getAllMessages();

	/**
	 *
	 * Same as get(), but uses the non quorum connection.
	 * @param messageId message to be retrieved.
	 * @return Retrieves the message stored in the queue by the messageId.  Null if not found.
	 */
	Message localGet(String messageId);

    List<Message> bulkPop(int messageCount, int wait, TimeUnit unit);
	List<Message> unsafeBulkPop(int messageCount, int wait, TimeUnit unit);

    /**
     *
     * @return Size of the queue.
     * @see #shardSizes()
     */
	long size();

    /**
     *
     * @return Map of shard name to the # of messages in the shard.
     * @see #size()
     */
	Map<String, Map<String, Long>> shardSizes();

    /**
     * Truncates the entire queue.  Use with caution!
     */
	void clear();

    /**
     * Process un-acknowledged messages.  The messages which are polled by the client but not ack'ed are moved back to queue
     */
	void processUnacks();
	void atomicProcessUnacks();

	/**
	 *
	 * Attempts to return the items present in the local queue shard but not in the hashmap, if any.
	 * (Ideally, we would not require this function, however, in some configurations, especially with multi-region write
	 * traffic sharing the same queue, we may find ourselves with stale items in the queue shards)
	 *
	 * @return List of stale messages IDs.
	 */
	List<Message> findStaleMessages();

	/*
	 * <=== Begin unsafe* functions. ===>
	 *
	 *     The unsafe functions listed below are not advisable to use.
	 *     The reason they are listed as unsafe is that they operate over all shards of a queue which means that
	 *     due to the eventually consistent nature of Dynomite, the calling application may see duplicate item(s) that
	 *     may have already been popped in a different rack, by another instance of the same application.
	 *
	 *     Why are these functions made available then?
	 *     There are some users of dyno-queues who have use-cases that are completely okay with dealing with duplicate
	 *     items.
	 */

	/**
	 * Provides a peek into all shards of the queue without taking messages out.
	 * Note: This function does not guarantee ordering of items based on shards like unsafePopAllShards().
	 *
	 * @param messageCount The number of messages to peek.
	 * @return A list of up to 'count' messages.
	 */
	List<Message> unsafePeekAllShards(final int messageCount);


	/**
	 * Allows popping from all shards of the queue.
	 *
	 * Note: The local shard will always be looked into first and other shards will be filled behind it (if 'messageCount' is
	 * greater than the number of elements in the local shard). This way we ensure the chances of duplicates are less.
	 *
	 * @param messageCount number of messages to be popped out of the queue.
	 * @param wait Amount of time to wait for each shard if there are no messages in shard.
	 * @param unit Time unit for the wait period
	 * @return messages. Can be less than the messageCount if there are fewer messages available than the message count.
	 * 					 If the popped messages are not acknowledge in a timely manner, they are pushed back into
	 * 					 the queue.
	 * @see #peek(int)
	 * @see #ack(String)
	 * @see #getUnackTime()
	 *
	 */
	List<Message> unsafePopAllShards(int messageCount, int wait, TimeUnit unit);


	/**
	 * Same as popWithMsgId(), but allows popping from any shard.
	 *
	 * @param messageId ID of message to pop
	 * @return Returns a "Message" object if pop was successful. 'null' otherwise.
	 */
	Message unsafePopWithMsgIdAllShards(String messageId);

}
