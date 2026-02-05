package redis.replication;

public final class RdbPayloads {
    private RdbPayloads() {
    }

    public static byte[] emptyRdb() {
        return new byte[] {
                'R','E','D','I','S','0','0','0','6',
                (byte) 0xFE, 0x00,
                (byte) 0xFB, 0x00, 0x00,
                (byte) 0xFF
        };
    }
}
