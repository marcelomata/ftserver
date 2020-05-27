package ftserver;

import iBoxDB.LocalServer.NotColumn;
import java.util.Date;
import java.util.Random;

public class Page {

    public final static int MAX_URL_LENGTH = 150;

    public long id;
    public String url;

    public String html;
    public String text;

    public Date createTime = new Date();

    private static final Random RAN = new Random();

    @NotColumn
    public String getRandomContent() {
        int len = text.length() - 100;
        if (len <= 20) {
            return text;
        }
        int s = RAN.nextInt(len);
        if (s < 0) {
            s = 0;
        }
        if (s > len) {
            s = len;
        }

        int end = s + 200;
        if (end > text.length()) {
            end = text.length();
        }

        return text.substring(s, end);
    }

}
