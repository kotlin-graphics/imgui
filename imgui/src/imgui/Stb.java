/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

/**
 *
 * @author GBarbieri
 */
public class Stb {

    private static int barrier, barrier2, barrier3, barrier4;
private static int dout;
    
    public static int decompressLength(char[] input) {
        return (input[8] << 24) + (input[9] << 16) + (input[10] << 8) + input[11];
    }

    public static int decompress(char[] output, char[] input) {

        int len, i = 0, o = 0;
        if (in4(0, input) != 0x57bC0000) {
            return 0;
        }
        if (in4(4, input) != 0) {
            return 0;    // error! stream is > 4GB
        }

        len = decompressLength(input);
        int barrier2 = 0;
        int barrier3 = 0 + len;
        int barrier = 0 + len;
        int barrier4 = 0;
        i += 16;

        for (;;) {

            int oldI = i;
            
        }
    }
    
    private char [] decompressToken(char [] i) {
        
        if (i[0] >= 0x20) { // use fewer if's for cases that expand small
//            if (i[0] >= 0x80)       match(stb__dout-i[1]-1, i[0] - 0x80 + 1), i += 2;
//            else if (*i >= 0x40)  stb__match(stb__dout-(stb__in2(0) - 0x4000 + 1), i[2]+1), i += 3;
//            else /* *i >= 0x20 */ stb__lit(i+1, i[0] - 0x20 + 1), i += 1 + (i[0] - 0x20 + 1);
        } else { // more ifs for cases that expand large, since overhead is amortized
//            if (*i >= 0x18)       stb__match(stb__dout-(stb__in3(0) - 0x180000 + 1), i[3]+1), i += 4;
//            else if (*i >= 0x10)  stb__match(stb__dout-(stb__in3(0) - 0x100000 + 1), stb__in2(3)+1), i += 5;
//            else if (*i >= 0x08)  stb__lit(i+2, stb__in2(0) - 0x0800 + 1), i += 2 + (stb__in2(0) - 0x0800 + 1);
//            else if (*i == 0x07)  stb__lit(i+3, stb__in2(1) + 1), i += 3 + (stb__in2(1) + 1);
//            else if (*i == 0x06)  stb__match(stb__dout-(stb__in3(1)+1), i[4]+1), i += 5;
//            else if (*i == 0x04)  stb__match(stb__dout-(stb__in3(1)+1), stb__in2(4)+1), i += 6;
        }
        return i;
    }
    
    private void match(char [] data, int length) {
        
        assert (dout + length <= barrier);
        
        if (dout + length > barrier) { dout += length; return; }
    if (data[0] < barrier4) { dout = barrier+1; return; }
    while ((length--) != 0) *stb__dout++ = *data++;
    }

    private static int in4(int x, char[] i) {
        return (i[x] << 24) + in3(x + 1, i);
    }

    private static int in3(int x, char[] i) {
        return (i[x] << 16) + in2(x + 1, i);
    }

    private static int in2(int x, char[] i) {
        return (i[x] << 8) + i[x + 1];
    }
}
