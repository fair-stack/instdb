package cn.cnic.instdb.utils;

public class Convert {



    /**
     * Convert hex string to byte[]
     *
     * @param hexString the hex string
     * @return byte[]
     */
    public static byte[] hexStringToBytes(String hexString) {
        if(hexString == null || hexString.equals("")) {
            return null;
        }

        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for(int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    public static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }



    /**
     * Convert byte array to hexadecimal string
     *
     * @param b byte[] Byte array to be converted
     * @return String Hexadecimal string
     */
    public static String byteToHex(byte b[]) {
        if(b == null) {
            throw new IllegalArgumentException(
                    "Argument b ( byte array ) is null! ");
        }
        String hs = "";
        String stmp = "";
        for(int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0xff);
            if(stmp.length() == 1) {
                hs = hs + "0" + stmp;
            }
            else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }


}
