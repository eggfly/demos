
package tk.eggfly;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Main {
    private static final byte[] PRINTABLE_CHARS = ("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "!\"#$%&\'()*+,-./:;<=>?@[\\]^_`{|}~ ").getBytes();
    private static final Map<Byte, Integer> CHAR_TO_INDEX_MAP = createCharToIndexMap();
    private static final MessageDigest sMD5 = safeCreateDigestInstance("MD5");
    private static final MessageDigest sSHA1 = safeCreateDigestInstance("SHA1");

    private static MessageDigest safeCreateDigestInstance(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    private static Map<Byte, Integer> createCharToIndexMap() {
        Map<Byte, Integer> map = new HashMap<Byte, Integer>();
        for (int i = 0; i < PRINTABLE_CHARS.length; i++) {
            map.put(PRINTABLE_CHARS[i], i);
        }
        return map;
    }

    /**
     * @param args
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws ClassNotFoundException {
        String start = "";
        if (args.length > 3) {
            System.err.println(String.format("usage: %s [start] [print=false]", args[0]));
            System.exit(1);
            return;
        }
        test();
        // calc(start);
    }

    private static void calc(String start) {
        byte[] current = start.getBytes();
        while (true) {
            final String md5 = md5(current);
            final String sha1 = sha1(current);
            System.out.println(String.format(Locale.US, "%s,%s,%s",
                    new String(current, StandardCharsets.UTF_8), md5, sha1));
            current = next(current);
        }
    }

    private static byte[] next(byte[] current) {
        for (int i = current.length - 1; i >= 0; i--) {
            final byte currentByte = current[i];
            final int currentIndex = CHAR_TO_INDEX_MAP.get(currentByte);
            if (currentIndex < PRINTABLE_CHARS.length - 1) {
                // 不需要进位,下一个char即可返回
                final byte nextByte = PRINTABLE_CHARS[currentIndex + 1];
                current[i] = nextByte;
                return current;
            } else {
                // 需要进位,进位一定是第一个字符
                current[i] = PRINTABLE_CHARS[0];
                // 不break而是continue让i--,让更高位的byte自增(i越小越是更高位)
            }
        }
        // 到这里没return就意味着最高位也需要进位了,类似于999 + 1 = 1000,也就是长度这时候需要增加1
        // 初始化条件,也就是空字符串"",就是length = 0时,没经过for到这里
        final byte[] ret = new byte[current.length + 1];
        System.arraycopy(current, 0, ret, 1, current.length);
        ret[0] = PRINTABLE_CHARS[0];
        return ret;
    }

    private static String sha1(byte[] bytes) {
        byte[] digest = sSHA1.digest(bytes);
        return byte2HexStr(digest);
    }

    private static String md5(byte[] bytes) {
        byte[] digest = sMD5.digest(bytes);
        return byte2HexStr(digest);
    }

    private static String byte2HexStr(byte[] b) {
        String stmp = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < b.length; n++)
        {
            stmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
        }
        return sb.toString();
    }

    private static void test() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:db.db");
            System.out.println(connection);
            Statement statement = connection.createStatement();
            statement.executeUpdate("drop table if exists rainbow");
            statement.executeUpdate("create table rainbow (id integer, origin string, hash string)");
            statement.executeUpdate("insert into rainbow (origin, hash) values('1', 'leo')");
            statement.executeUpdate("insert into rainbow (origin, hash) values('2', 'yui')");
            ResultSet rs = statement.executeQuery("select * from rainbow");
            while (rs.next()) {
                // read the result set
                System.out.println("id = " + rs.getInt("id"));
                System.out.println("origin = " + rs.getString("origin"));
                System.out.println("hash = " + rs.getString("hash"));
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }
}
