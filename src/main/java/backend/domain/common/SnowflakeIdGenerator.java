package backend.domain.common;

import java.util.random.RandomGenerator;

public class SnowflakeIdGenerator {

    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long maxNodeId = (1L << NODE_ID_BITS) - 1;
    private static final long maxSequence = (1L << SEQUENCE_BITS) - 1;

    private static final long nodeId = RandomGenerator.getDefault().nextLong(maxNodeId + 1);
    // UTC = 2024-01-01T00:00:00Z
    private static final long startTimeMillis = 1704067200000L;

    private static long lastTimeMillis = startTimeMillis;
    private static long sequence = 0L;

    public static synchronized long nextId() {
        long currentTimeMillis = System.currentTimeMillis();

        if (currentTimeMillis < lastTimeMillis) {
            throw new IllegalStateException("Invalid Time");
        }

        if (currentTimeMillis == lastTimeMillis) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) {
                currentTimeMillis = waitNextMillis(currentTimeMillis);
            }
        } else {
            sequence = 0;
        }

        lastTimeMillis = currentTimeMillis;

        return ((currentTimeMillis - startTimeMillis) << (NODE_ID_BITS + SEQUENCE_BITS))
                | (nodeId << SEQUENCE_BITS)
                | sequence;
    }

    private static long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp <= lastTimeMillis) {
            currentTimestamp = System.currentTimeMillis();
        }
        return currentTimestamp;
    }
}
