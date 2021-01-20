package eassen;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author eassen
 * @Create 2021/1/16 1:22 下午
 */
public class HuffmanEncode {

    // 长度 头占位长度
    private static int PRE_LENGTH;

    // 编码长度
    private static long BIT_CODES_LENGTH;

    //解码方式
    private static final Charset DEFAULT_DECODE_FORMAT = StandardCharsets.UTF_8;


    /**
     * <原编码, 替代值>
     */
    static Map<Byte, String> huffmanCodes = new HashMap<>();


    static StringBuilder stringBuilder = new StringBuilder();

    /**
     * 获取文件
     *
     * @param source
     * @param target
     */
    public static void zipFile(String source, String target) throws IOException {
        ObjectOutputStream oos = null;
        FileInputStream fis = null;

        try {
            //获取文件源
            fis = new FileInputStream(source);
            byte[] b = new byte[fis.available()];
            fis.read(b);

            byte[] huffmanBytes = huffmanZip(b);

            oos = new ObjectOutputStream(new FileOutputStream(target));
            oos.writeObject(huffmanBytes);
            oos.writeObject(huffmanCodes);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fis.close();
            oos.close();
        }
    }

    /**
     * 解压缩方法
     *
     * @param source
     * @param target
     * @throws IOException
     */
    public static void unzipFile(String source, String target) throws IOException {
        InputStream is = null;
        ObjectInputStream ois = null;
        FileOutputStream fos = null;

        try {
            is = new FileInputStream(source);
            ois = new ObjectInputStream(is);
            // 读信息
            byte[] huffmanBytes = (byte[]) ois.readObject();
            // 读码表
            Map<Byte, String> huffmanCodes = (Map<Byte, String>) ois.readObject();
            // 解码
            byte[] bytes = decode(huffmanBytes, huffmanCodes);
            // byte to String

            fos = new FileOutputStream(target);
            fos.write(bytes);


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            is.close();
            ois.close();
            fos.close();
        }
    }

    /**
     * 解码
     *
     * @param huffmanCodes
     * @param huffmanBytes
     * @return
     */
    private static byte[] decode(byte[] huffmanBytes, Map<Byte, String> huffmanCodes) {

        // 解析头部信息
        parsingHeader(huffmanBytes);

        // 构建 二进制串
        buildBitString(huffmanBytes);

        // 反转 K, V
        Map<String, Byte> huffmanCodesMap = invertMap(huffmanCodes);

        // 还原原串
        byte[] bytes = reduction(huffmanCodesMap);
        return bytes;
    }

    /**
     * 解析头部信息
     *
     * @param huffmanBytes
     */
    private static void parsingHeader(byte[] huffmanBytes) {

        StringBuilder byte_len = new StringBuilder();
        byte byte_len_len = huffmanBytes[0];

        for (int i = 1; i <= byte_len_len; i++) {
            byte_len.append((char)huffmanBytes[i]);
        }
        PRE_LENGTH = byte_len_len + 1;
        BIT_CODES_LENGTH = Long.parseLong(byte_len.toString());
    }

    /**
     * 还原原串
     *
     * @param huffmanCodesMap 编码表
     * @return
     */
    private static byte[] reduction(Map<String, Byte> huffmanCodesMap) {

        int i = 0, count;
        Byte b;
        StringBuilder unCodeStringBuilder = new StringBuilder();

        while (i < BIT_CODES_LENGTH) {
            count = 1;

            while (true) {
                // 根据哈夫曼编码特性，任意编码替代不重复
                // 取首个哈夫曼
                String key = stringBuilder.substring(i, i + count);
                b = huffmanCodesMap.get(key);

                if (b != null) {
                    // 二进制 转Ascii 转值
                    char c = (char) Integer.parseInt(b.toString());
                    unCodeStringBuilder.append(c);
                    break;
                } else {
                    count++;
                }
            }
            i += count;
        }
        byte[] bytes = unCodeStringBuilder.toString().getBytes(DEFAULT_DECODE_FORMAT);
        return bytes;
    }

    /**
     * 构建 二进制串
     *
     * @param huffmanBytes
     */
    private static void buildBitString(byte[] huffmanBytes) {
        // 截取头部以后
        for (int i = PRE_LENGTH; i < huffmanBytes.length; i++) {
            stringBuilder.append(byteToBitString(huffmanBytes[i]));
        }
    }

    /**
     * Byte转Bit(option2)
     */
    private static String byteToBitString(byte b) {
        return "" + (byte) ((b >> 7) & 0x1) +
                (byte) ((b >> 6) & 0x1) +
                (byte) ((b >> 5) & 0x1) +
                (byte) ((b >> 4) & 0x1) +
                (byte) ((b >> 3) & 0x1) +
                (byte) ((b >> 2) & 0x1) +
                (byte) ((b >> 1) & 0x1) +
                (byte) ((b >> 0) & 0x1);
    }

    /**
     * 压缩（哈夫曼压缩）
     *
     * @param bytes
     * @return
     */
    private static byte[] huffmanZip(byte[] bytes) throws Exception {

        // 统计结点出现次数，计算权值
        List<Node> nodes = getNodes(bytes);
        // 根据权值构建哈夫曼树
        Node huffmanTreeRoot = createHuffmanTree(nodes);
        //遍历树 （可选功能，求效率注释掉）
        preOrder(huffmanTreeRoot);
        //封装成编码表
        Map<Byte, String> huffmanCodes = putCodes(huffmanTreeRoot);

        byte[] huffmanCodeBytes = zip(bytes, huffmanCodes);
        // 封装编码表头
        byte[] huffmanCodeBytesWithHeader = withHeader(huffmanCodeBytes);

        return huffmanCodeBytesWithHeader;
    }

    /**
     * 封装头部信息
     *
     * @param bytes
     * @return
     */
    private static byte[] withHeader(byte[] bytes) {

        byte[] byte_len = String.valueOf(BIT_CODES_LENGTH).getBytes();

        byte[] huffmanCodeBytes = new byte[bytes.length + byte_len.length + 1];

        huffmanCodeBytes[0] = (byte) byte_len.length;
        System.arraycopy(byte_len, 0, huffmanCodeBytes, 1, byte_len.length);
        System.arraycopy(bytes, 0, huffmanCodeBytes, byte_len.length + 1, +bytes.length);

        return huffmanCodeBytes;
    }

    /**
     * 压缩
     *
     * @param bytes
     * @param huffmanCodes
     * @return
     */
    private static byte[] zip(byte[] bytes, Map<Byte, String> huffmanCodes) throws Exception {

        StringBuilder stringBuilder = new StringBuilder();

        // 压缩替换
        for (byte data : bytes) {
            if (huffmanCodes.get(data) == null) {
                throw new Exception("不存在对应的哈夫曼编码");
            } else {
                stringBuilder.append(huffmanCodes.get(data));
            }
        }
        BIT_CODES_LENGTH = stringBuilder.length();
        // 二进制bit转byte
        return bitToByte(stringBuilder);
    }

    /**
     * bit -> byte
     *
     * @param stringBuilder
     * @return
     */
    private static byte[] bitToByte(StringBuilder stringBuilder) {
        int len;

        /**
         * 八位二进制为一组
         */
        if (stringBuilder.length() % 8 == 0) {
            len = stringBuilder.length() / 8;
        } else {
            len = stringBuilder.length() / 8 + 1;
        }

        byte[] huffmanBytes = new byte[len];
        int index = 0;

        for (int i = 0; i < stringBuilder.length(); i += 8) {
            StringBuilder strByte;
            if (i + 8 > stringBuilder.length()) {
                strByte = new StringBuilder(stringBuilder.substring(i));
                // 最后一组数 的补码
                while (strByte.length() < 8) {
                    strByte.append("0");
                }
            } else {
                strByte = new StringBuilder(stringBuilder.substring(i, i + 8));
            }

            huffmanBytes[index] = (byte) Integer.parseInt(strByte.toString(), 2);
            index++;
        }
        return huffmanBytes;
    }

    /**
     * Bit转Byte
     */
    private static byte bitToByte(String byteStr) {
        int re, len;
        if (null == byteStr) {
            return 0;
        }
        len = byteStr.length();
        if (len != 4 && len != 8) {
            return 0;
        }
        if (len == 8) {
            if (byteStr.charAt(0) == '0') {
                re = Integer.parseInt(byteStr, 2);
            } else {
                // 负数
                re = Integer.parseInt(byteStr, 2) - 256;
            }
        } else {
            re = Integer.parseInt(byteStr, 2);
        }
        return (byte) re;
    }


    /**
     * dfs入口
     *
     * @param rootNode
     * @return
     */
    private static Map<Byte, String> putCodes(Node rootNode) {
        if (rootNode == null) {
            return null;
        } else {
            putCodes(rootNode.left, "0", stringBuilder);
            //清空字符串
            stringBuilder = new StringBuilder();
            putCodes(rootNode.right, "1", stringBuilder);
            return huffmanCodes;
        }
    }


    /**
     * 哈夫曼编码
     *
     * @param node
     * @param code
     * @param stringBuilder
     */
    private static void putCodes(Node node, String code, StringBuilder stringBuilder) {

        if (node == null) {
            return;
        }

        //深克隆， dfs流里记录 当前坐标 01011
        StringBuilder currentStringBuilder = new StringBuilder(stringBuilder);
        currentStringBuilder.append(code);

        if (node.data != null) {
            huffmanCodes.put(node.data, currentStringBuilder.toString());
        } else {
            putCodes(node.left, "0", currentStringBuilder);
            putCodes(node.right, "1", currentStringBuilder);
        }
    }

    /**
     * 创建哈夫曼树
     *
     * @param nodes
     * @return
     */
    private static Node createHuffmanTree(List<Node> nodes) {

        // 集合只存在一个结点时，构建完毕
        while (nodes.size() > 1) {

            //先找出权值最小的两个结点
            nodes = nodes.stream().sorted(Node::compareTo).collect(Collectors.toList());

            Node leftNode = nodes.get(0);
            Node rightNode = nodes.get(1);

            //生成父亲结点
            Node parent = new Node(null, leftNode.weight + rightNode.weight);

            parent.left = leftNode;
            parent.right = rightNode;

            nodes.remove(leftNode);
            nodes.remove(rightNode);
            // 将权值之和插入到原集合中
            nodes.add(parent);
        }

        return nodes.get(0);
    }

    /**
     * 获取所有结点，统计每个byte出现的次数
     *
     * @param bytes
     * @return
     */
    private static List<Node> getNodes(byte[] bytes) {
        ArrayList<Node> nodes = new ArrayList<>();
        //统计每个byte出现的次数
        Map<Byte, Integer> countsMap = new HashMap<>();
        for (byte data : bytes) {
            Integer count = countsMap.get(data);
            if (count == null) {
                countsMap.put(data, 1);
            } else {
                countsMap.put(data, count + 1);
            }
        }

        for (Map.Entry<Byte, Integer> entry : countsMap.entrySet()) {
            // <data, weight>
            nodes.add(new Node(entry.getKey(), entry.getValue()));
        }

        return nodes;
    }

    /**
     * map反转
     *
     * @param map
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<V, K> invertMap(final Map<K, V> map) {
        final Map<V, K> out = new HashMap<V, K>(map.size());
        for (final Map.Entry<K, V> entry : map.entrySet()) {
            out.put(entry.getValue(), entry.getKey());
        }
        return out;
    }

    /**
     * 前序遍历
     *
     * @param root
     */
    private static void preOrder(Node root) {
        if (root != null) {
            root.preOrder();
        } else {
            System.out.println("赫夫曼树为空");
        }
    }
}
