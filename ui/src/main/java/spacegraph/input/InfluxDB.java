package spacegraph.input;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jcog.Util;
import org.apache.commons.compress.utils.IOUtils;
import org.mockito.internal.util.io.IOUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * https://docs.influxdata.com/influxdb/v1.2/guides/querying_data/
 *
 * curl -G 'http://localhost:8086/query?pretty=true' --data-urlencode "db=mydb" --data-urlencode "q=SELECT \"value\" FROM \"cpu_load_short\" WHERE \"region\"='us-west'"

 */
public class InfluxDB {

    private final String host;
    private final int port;
    private final String db;

    public InfluxDB(String db) {
        this(db, "localhost", 8086);
    }

    public InfluxDB(String db, String host, int port) {
        this.host = host;
        this.port = port;
        this.db = db;
    }

    public float[] get(String measurement, long from, long to) {
        return get(measurement, "*", from, to);
    }

    static final DateFormat RFCTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    public float[] get(String measurement, String value, long from, long to) {
        char c = value.charAt(0);
        if ((c !='*') && (c!='\"'))
            value = '"' + value + '"';

        String query = "SELECT " + value + " FROM \"" + measurement + "\" WHERE \"time\" >= \"" + rfc(from) + "\" AND \"time\" <= \"" + rfc(to) + "\"";

        String epoch = "ms";

        URL u = null;
        try {
            u = new URL("http://" + host + ":" + port + "/query?db=" + db + "&epoch=" + epoch + "&q=" + UrlEscapers.urlFragmentEscaper().escape( query) );
            System.out.println(u);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return Util.EmptyFloatArray;
        }

        try {

            JsonElement x = new JsonParser().parse(new InputStreamReader(u.openStream()));
            System.out.println(x);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new float[] { };
    }

    public String rfc(long from) {
        return RFCTime.format(new Date(from));
    }

    public static void main(String[] args) {

        System.out.println(Arrays.toString(
            new InfluxDB("nar1").get("cpu", "*",
                System.currentTimeMillis() - 16 * 24 * 60 * 60 * 1000, System.currentTimeMillis()))
        );
    }
}
