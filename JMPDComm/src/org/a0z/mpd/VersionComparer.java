package org.a0z.mpd;

import java.util.Comparator;

import android.text.TextUtils;

public class VersionComparer implements Comparator<String> {
    /* states: S_N: normal, S_I: comparing integral part, S_F: comparing
               fractional parts, S_Z: idem but with leading Zeroes only */
    static final byte S_Normal = 0x0;
    static final byte S_Integral = 0x3;
    static final byte S_Fractional = 0x6;
    static final byte S_Zeros = 0x9;

    /* result_type: CMP: return diff; LEN: compare using len_diff/diff */
    static final byte R_BFR = -1;
    static final byte R_AFT = +1;
    static final byte R_CMP = 2;
    static final byte R_LEN = 3;

    /* Symbol(s)    0       [1-9]   others
       Transition   (10) 0  (01) d  (00) x   */
    static final byte[] next_state =
    {
        /* state          x         d             0  */
        /* Normal */      S_Normal, S_Integral,   S_Zeros,
        /* Integral */    S_Normal, S_Integral,   S_Integral,
        /* Fractional */  S_Normal, S_Fractional, S_Fractional,
        /* Zeros */       S_Normal, S_Fractional, S_Zeros,
    };

    static final byte[] result_type =
    {
        /* state          x/x    x/d    x/0    d/x    d/d    d/0    0/x    0/d    0/0  */

        /* Normal */      R_CMP, R_CMP, R_CMP, R_CMP, R_LEN, R_CMP, R_CMP, R_CMP, R_CMP,
        /* Integral */    R_CMP, R_BFR, R_BFR, R_AFT, R_LEN, R_LEN, R_AFT, R_LEN, R_LEN,
        /* Fractional */  R_CMP, R_CMP, R_CMP, R_CMP, R_CMP, R_CMP, R_CMP, R_CMP, R_CMP,
        /* Zeros */       R_CMP, R_AFT, R_AFT, R_BFR, R_CMP, R_CMP, R_BFR, R_CMP, R_CMP,
    };

    /* Compare S1 and S2 as strings holding indices/version numbers,
       returning less than, equal to or greater than zero if S1 is less than,
       equal to or greater than S2 (for more info, see the texinfo doc).
    */

    public static int strverscmp(String s1, String s2)
    {
        if (s1 == s2)
            return 0;

        final byte O = (byte)1;
        final byte Z = (byte)0;
        
        final int l1 = s1.length();
        final int l2 = s2.length();

        int p1 = 0, p2 = 0;

        Character c1 = s1.charAt(p1++);
        Character c2 = s2.charAt(p2++);
        /* Hint: '0' is a digit too.  */
        int state = (byte)S_Normal + (c1 == '0' ? O : Z) + (Character.isDigit(c1) ? O : Z);

        int diff;
        while ((diff = c1 - c2) == 0)
        {
            if (c1 == '\0')
                return diff;

            state = next_state[state];
            c1 = p1 < l1 ? s1.charAt(p1++) : '\0';
            c2 = p2 < l2 ? s2.charAt(p2++) : '\0';
            state += (c1 == '0' ? O : Z) + (Character.isDigit(c1) ? O : Z);
        }

        switch (state = result_type[state * 3 + (((c2 == '0' ? O : Z) + (Character.isDigit(c2) ? O : Z)))])
        {
            case R_CMP:
                return diff;

            case R_LEN:
                while (p1 < l1 && Character.isDigit(s1.charAt(p1++)))
                    if (!(p2 < l2 && Character.isDigit(s2.charAt(p2++))))
                        return R_AFT;

                return p2 < l2 && Character.isDigit(s2.charAt(p2)) ? R_BFR : diff;

            default:
                return state;
        }
    }

    public int compare(String x, String y)
    {
        return TextUtils.isEmpty(x) ? TextUtils.isEmpty(y) ? 0 : -1 : TextUtils.isEmpty(y) ? +1 : strverscmp(x == null? "" : x, y == null? "" : y);
    }

    public static final VersionComparer Instance = new VersionComparer();
}
